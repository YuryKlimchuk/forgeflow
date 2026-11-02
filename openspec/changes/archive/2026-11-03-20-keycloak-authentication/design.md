# Design

## Context

Текущий backend не подключает Spring Security: `AuthenticatedUserProvider` реализован stub-классом, а `MutableSubjectProvider` используется для тестового/демо-субъекта. `OrganizationContextResolver` уже разделяет пользовательский и административный realm, а `organizations` предоставляет `UserId`, `Owner` и `getAffiliation`. Frontend выполняет прямые `fetch` без заголовка авторизации. Change затрагивает security, web lifecycle, organizations, frontend и локальную Keycloak-инфраструктуру.

## Goals / Non-Goals

**Goals:**

- Ввести единый проверенный JWT-контур для обоих issuer URL.
- Сохранить доменную чистоту: Spring Security и JWT остаются в infrastructure/adapters, а домен получает только `AuthenticatedUser` через существующий порт.
- Сохранить текущую политику realm и single-org резолвинга.
- Сделать локальный запуск и интеграционные тесты воспроизводимыми через импорт realm-конфигурации.

**Non-Goals:**

- Реализация adminpanel или ролевой модели `SUPER_ADMIN`.
- Изменение хранилища организации или переход к schema-per-tenant.
- Создание backend login endpoints вместо стандартного OAuth2 redirect flow.

## Decisions

### D1. Spring Security Resource Server с двумя доверенными issuer

Backend будет использовать JWT Resource Server и resolver, выбирающий JWT decoder по полю `iss`. Доверенными будут только issuer URL realm `forgeflow-users` и `forgeflow-admins`; неизвестный issuer отвергается. Альтернатива — один decoder или выбор realm по custom claim — отклонена: два realm имеют разные issuer, а решение по claim не соответствует зафиксированному контракту.

### D2. Адаптер authenticated user поверх SecurityContext

Infrastructure-адаптер будет читать `JwtAuthenticationToken`, брать `sub` и issuer, сопоставлять issuer с `Realms` и возвращать `AuthenticatedUser(UserId, realm)` через `AuthenticatedUserProvider`. Application/domain-пакеты не будут импортировать Spring Security. Stub-провайдер и mutable subject останутся только там, где это необходимо для изолированных legacy unit-тестов, но не будут зарегистрированы в runtime-контексте.

### D3. Ленивое создание Owner в request lifecycle

После успешной проверки пользовательского JWT application-адаптер вызовет существующую операцию регистрации текущего пользователя перед разрешением организации. Регистрация должна быть идемпотентной: существующий `Owner` не дублируется. Для нового пользователя результатом остаётся отсутствие организации, поскольку организация создаётся отдельно через UI.

### D4. Frontend-адаптер keycloak-js

Frontend будет иметь конфигурацию двух Keycloak clients и отдельные действия user/admin login. Инициализация использует Authorization Code + PKCE. API-клиент получит общий wrapper над `fetch`, который обновляет/берёт access token и добавляет `Authorization` ко всем `/api` запросам; публичные запросы без активной сессии не маскируются под аутентифицированные.

### D5. Realm import как источник локальной конфигурации

Docker Compose будет запускать Keycloak с импортом двух JSON realm-файлов. В каждом файле будут realm, client, password flow и Google Identity Provider с параметрами, задаваемыми через переменные окружения. Секреты Google и client secrets не будут встраиваться в репозиторий.

### D6. Testcontainers с теми же realm-файлами

Интеграционные тесты поднимут настоящий Keycloak Testcontainer, импортируют оба realm и получат токены через token endpoint для проверки backend. Тестовые client credentials и пользователи будут отдельными от production secrets; проверки охватят оба issuer, subject mapping, 401 и отсутствие организации для admin realm.

## Risks / Trade-offs

- **[Разные issuer URL в окружениях]** → вынести issuer в конфигурацию профиля/переменные окружения и использовать те же значения при создании frontend Keycloak clients.
- **[Google IdP требует внешних credentials и callback URL]** → импортировать IdP с env-подстановкой и обеспечить password flow для полностью автономных тестов.
- **[Изменение порядка lifecycle может вызвать запросы до SecurityContext]** → выполнять регистрацию и разрешение только после успешной аутентификации и покрыть фильтр интеграционными тестами.
- **[Обновление access token во frontend может гонять параллельные refresh-запросы]** → централизовать обновление токена в одном promise/adapter и тестировать повторные API-вызовы.
- **[Legacy unit-тесты зависят от stub-классов]** → заменить их на тестовые doubles порта либо явно ограничить stub тестовыми конфигурациями, не регистрируя его в production application context.

## Migration Plan

1. Добавить зависимости и конфигурацию Resource Server, затем зарегистрировать Spring Security filter chain.
2. Подключить provider из `SecurityContext` и ленивую регистрацию `Owner`, после чего удалить runtime bootstrap демо-субъекта.
3. Добавить realm JSON и Compose-настройки, проверить ручной login обоих realm.
4. Подключить frontend login и Bearer wrapper.
5. Запустить unit, frontend и Testcontainers integration tests.

При rollback конфигурация приложения возвращается к предыдущему релизу вместе с отключением новых Keycloak clients; данные `Owner` без организации сохраняются и не требуют миграции.

## Open Questions

Нет: issuer URLs, два realm, протокол, mapping subject и область изменения зафиксированы задачей.
