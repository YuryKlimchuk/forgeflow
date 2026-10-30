# Design

## Context

Текущий проект — Java 25 модульный монолит без `spring-web` и `spring-security`. `shared.api` предназначен для общих публичных типов и модуль `shared` объявлен без зависимостей. Организационный контекст должен возвращать `OrganizationId`, поэтому этот тип также должен находиться в `shared.api`: размещение `OrganizationId` в `organizations.api` заставило бы `shared` зависеть от `organizations`, что противоречит границе общего модуля.

`organizations` публикует `UserId`, `OwnerId`, `Affiliation` и `OrganizationService`; `security` использует его для разрешения принадлежности. `catalog` использует `shared.api` и `directories.api`.

## Goals / Non-Goals

**Goals:**

- Дать бизнес-модулям стабильный read-only контракт для получения текущего `shared.api.OrganizationId`.
- Разместить `OrganizationId` и `OrganizationContext` в `shared.api`, сохранив `shared` независимым.
- Перенести импорты `OrganizationId` в `organizations` на `shared.api` без изменения бизнес-правил организаций.
- Скрыть механизм хранения контекста в `security` и обеспечить изоляцию последовательных запросов.
- Разрешать организацию по серверному subject и realm через публичный контракт `organizations`.
- Зафиксировать безопасный паттерн явной передачи `OrganizationId` в repository и фоновые операции.

**Non-Goals:**

- Реальный HTTP-фильтр, Spring Security и Keycloak.
- Автоматическое распространение контекста в async/thread pools.
- Поддержка нескольких организаций одновременно в одном текущем контексте.

## Decisions

### D1. Общие идентификатор и контракт в `shared.api`

`OrganizationId` переносится из `organizations.api` в `shared.api` и остаётся record с компонентом `UUID value` и фабриками `generate()`/`of()`. В том же API размещаются `OrganizationContext` и специализированное исключение отсутствующего контекста; `OrganizationContext` возвращает `shared.api.OrganizationId`.

Это решение позволяет `shared` оставаться без зависимостей: общий модуль не импортирует `organizations`. `UserId` и `OwnerId` остаются в `organizations.api`, поскольку они являются идентификаторами предметного модуля организаций. Альтернатива — оставить `OrganizationId` в `organizations.api` — отвергнута из-за недопустимой зависимости `shared -> organizations`.

### D2. Thread-local реализация только в `security`

`ThreadLocalOrganizationContext` реализует публичный контракт и содержит package/infrastructure-доступные операции установки и очистки. Бизнес-модули получают только `OrganizationContext` и не импортируют реализацию или `ThreadLocal`.

### D3. Authenticated user и resolver

В `security` вводятся модель `AuthenticatedUser(userId, realm)` и порт получения текущего пользователя. `OrganizationContextResolver` применяет realm-политику: для `forgeflow-users` вызывает `OrganizationService.getAffiliation(userId)` и извлекает `OrganizationId` из `OwnerOf`/`MemberOf`; для `forgeflow-admins` возвращает отсутствие контекста.

Публичный `OrganizationService.getAffiliation(UserId)` добавляется как явный запрос принадлежности; существующий overload без аргумента делегирует ему через `SubjectProvider`, чтобы не дублировать бизнес-логику.

### D4. Lifecycle через try/finally

Обёртка уровня запроса получает authenticated user, резолвит организацию, устанавливает её перед handler и всегда вызывает clear в `finally`. Она не принимает `organizationId` клиента как источник данных.

### D5. Явная передача идентификатора в бизнес-операции

Application service `catalog` инжектирует `OrganizationContext`, вызывает `requiredOrganizationId()` и передаёт полученный `shared.api.OrganizationId` отдельным аргументом в repository/порт. Для async и background jobs этот идентификатор передаётся параметром; `ThreadLocal` не считается переносимым контекстом.

### D6. Модульные границы и отсутствие цикла

Разрешённый граф фиксируется так: `organizations -> shared`, `security -> organizations + shared`, `catalog -> directories + shared`; `shared` и `directories` не имеют зависимостей. Межмодульный доступ осуществляется только через `api`-пакеты, поэтому `organizations` обращается к общему идентификатору через `shared.api`, а `shared` не знает об `organizations`. Граф остаётся ацикличным.

## Risks / Trade-offs

- **[Риск]** Неочищенный `ThreadLocal` может передать организацию следующему запросу → очистка выполняется безусловно в `finally`.
- **[Риск]** Перенос публичного типа требует согласованного обновления импортов → все использования `OrganizationId` и тесты `organizations` обновляются одновременно, а `organizations/package-info.java` разрешает `shared`.
- **[Риск]** Ошибочная декларация зависимости может создать цикл → архитектурный тест проверяет полный направленный граф и доступ только через API.
- **[Риск]** Realm-заглушка может отличаться от будущих claims Keycloak → граница `AuthenticatedUser`/порта изолирует замену адаптера.
- **[Компромисс]** До появления HTTP слой lifecycle тестовый/локальный, а не framework filter → поведение запроса проверяется через исполняемую try/finally-обёртку.

## Migration Plan

Изменение не требует миграции данных. При реализации сначала `OrganizationId` и контекст размещаются в `shared.api`, затем обновляются импорты и декларация `organizations`, после чего добавляются security lifecycle и пример catalog. Откат требует возврата пакета типа и удаления разрешения `organizations -> shared`, без миграций БД.

## Open Questions

Нет: пакет `OrganizationId`, граф зависимостей и правила realm зафиксированы.
