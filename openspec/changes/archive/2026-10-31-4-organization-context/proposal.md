# Proposal

## Why

Бизнес-модулям ForgeFlow нужен единый способ получить организацию текущего запроса без передачи `OrganizationId` через каждый метод. Предыдущий вариант размещал `OrganizationContext` в `shared.api`, но оставлял `OrganizationId` в `organizations.api`; при объявлении `shared` без зависимостей это создавало недопустимую зависимость `shared -> organizations`.

## What Changes

- Добавить в `shared.api` тип `OrganizationId` как record с компонентом `UUID value` и фабриками `generate()`/`of()`, интерфейс `OrganizationContext` и специализированное исключение отсутствующего контекста.
- Сохранить `shared` без зависимостей; `OrganizationContext` SHALL возвращать `shared.api.OrganizationId`.
- Перенести использование `OrganizationId` в `organizations` на `shared.api.OrganizationId`; `UserId` и `OwnerId` останутся в `organizations.api`.
- Разрешить зависимость `organizations -> shared` и доступ к ней только через `shared.api`; сохранить зависимости `security -> organizations + shared` и `catalog -> directories + shared`.
- Зафиксировать, что `shared` и `directories` не имеют зависимостей, а полный граф остаётся ацикличным.
- Добавить в публичный `OrganizationService` метод `getAffiliation(UserId)`, сохранив существующий `getAffiliation()` как делегирование к явному subject.
- Добавить в `security` модель аутентифицированного пользователя, resolver принадлежности и lifecycle-обёртку с очисткой контекста в `finally`.
- Зафиксировать realm-правило: `forgeflow-users` получают организационный контекст через `organizations`, а `forgeflow-admins` автоматически его не получают.
- Добавить минимальный пример использования `OrganizationContext` в application-сервисе `catalog` и тесты контекста, resolver/lifecycle и архитектурных ограничений.

## Capabilities

### New Capabilities

- `organization-context`: предоставление текущей организации через read-only API `shared.api`, разрешение организации по authenticated subject и безопасный lifecycle контекста.

### Modified Capabilities

- `organizations`: публичное получение affiliation по явному `UserId`, перенос типа `OrganizationId` в `shared.api` и зависимость `organizations -> shared` без изменения остальной логики организаций.
- `module-boundaries`: разрешённые зависимости включают `organizations -> shared`, `security -> organizations + shared` и `catalog -> directories + shared`; `shared` и `directories` остаются независимыми.

## Impact

- Затрагиваются `shared.api`, все места использования `OrganizationId` в `organizations`, `organizations/package-info.java`, `security`, `catalog` и архитектурные тесты.
- Публичный тип `OrganizationId` меняет пакет с `organizations.api` на `shared.api`; `UserId` и `OwnerId` остаются в `organizations.api`.
- Публичный контракт `OrganizationService` расширяется методом `getAffiliation(UserId)`; существующий метод без аргумента сохраняется.
- Межмодульное обращение `organizations` к общему идентификатору выполняется через `shared.api`, поэтому `shared` не получает зависимость от `organizations` и цикл не возникает.
- В MVP не добавляются новые зависимости `spring-web` или `spring-security`; HTTP и Keycloak моделируются отдельными портами и тестовой lifecycle-обёрткой.

## Non-goals

- Реальный HTTP-фильтр/interceptor и интеграция со Spring Web.
- Реальная интеграция с Keycloak, получение realm из токена и настройка Spring Security.
- Автоматическое наследование контекста асинхронными или фоновыми задачами.
- Передача контекста через `ThreadLocal` в бизнес-модули; такие задачи SHALL передавать `OrganizationId` явно.
- Изменение остальной бизнес-логики `organizations` или расширение примера `catalog` за пределы фиксации паттерна.
