# Proposal

## Why

ForgeFlow принимает запросы через заглушку `StubAuthenticatedUserProvider`, а фронт не передаёт удостоверение пользователя. В результате приложение не проверяет JWT и не может использовать реальную идентичность Keycloak; change заменяет этот механизм на OAuth2 Resource Server и вход через Keycloak.

## What Changes

- Добавить Spring Security OAuth2 Resource Server с проверкой JWT для realm `forgeflow-users` и `forgeflow-admins`.
- Определять realm по `iss`, преобразовывать JWT `sub` в `UserId` и предоставлять `AuthenticatedUser` через текущий security-контракт.
- Сохранить существующую single-org политику: пользовательская организация разрешается через `OrganizationService.getAffiliation`, административный realm не получает организационный контекст.
- Добавить ленивую регистрацию `Owner` без организации при первом входе пользователя без существующего `Owner`.
- Удалить runtime-зависимость от stub-аутентификации, `MutableSubjectProvider` и демо-пользователя в `DemoDataBootstrap`.
- Добавить React-вход через `keycloak-js` с Authorization Code + PKCE для двух realm'ов и прикреплением Bearer-токена к `/api` запросам.
- Добавить конфигурацию `docker-compose.yml` и JSON-импорт двух realm'ов с password login, Google IdP и клиентами.
- Добавить интеграционные тесты с Testcontainers Keycloak, реальными realm'ами и JWT, а также обновить frontend-тесты.

## Capabilities

### New Capabilities

- `keycloak-authentication`: аутентификация backend JWT, сопоставление issuer/realm, frontend-вход и тестовая инфраструктура Keycloak.

### Modified Capabilities

- `organization-context`: источник `AuthenticatedUser` изменяется со stub-провайдера на Spring Security, а первый вход пользователя без `Owner` приводит к ленивой регистрации `Owner` без организации.

## Impact

Изменения затронут модуль `security`, конфигурацию Spring Boot и Maven-зависимости, request lifecycle и интеграцию с `organizations`, `DemoDataBootstrap`, web security, frontend `api.ts` и компоненты входа, Docker Compose и realm-import JSON. Потребуются Testcontainers Keycloak и интеграционные тесты с реальными токенами. Бизнес-правила организации и single-org модель не изменяются.

## Non-goals

- Создание adminpanel и реализация бизнес-логики `SUPER_ADMIN`.
- Изменение модели multi-org или добавление организации в JWT claim.
- Перенос бизнес-проверок прав организаций в Keycloak.
- Изменение `docs/roles.md`.
