# Spec Delta

## Purpose

Capability обеспечивает аутентификацию ForgeFlow через Keycloak для пользовательского и административного входа, проверку JWT backend и передачу удостоверения из React SPA.

## ADDED Requirements

### Requirement: JWT resource server
Backend SHALL принимать защищённые API-запросы только с валидным JWT Bearer-токеном, выпущенным одним из настроенных issuer URL ForgeFlow.

#### Scenario: Валидный пользовательский токен
- **WHEN** клиент отправляет запрос к `/api` с валидным Bearer-токеном realm `forgeflow-users`
- **THEN** backend выполняет запрос как аутентифицированный пользователь

#### Scenario: Отсутствующий или недействительный токен
- **WHEN** клиент отправляет защищённый запрос без Bearer-токена или с токеном, не прошедшим проверку
- **THEN** backend возвращает HTTP 401 и не выполняет обработчик

### Requirement: Realm mapping by issuer
Security integration SHALL map `forgeflow-users` and `forgeflow-admins` exclusively from the JWT `iss` value and SHALL reject a token whose issuer is not configured.

#### Scenario: User issuer
- **WHEN** валидный JWT содержит issuer URL realm `forgeflow-users`
- **THEN** authenticated user получает realm `forgeflow-users`

#### Scenario: Admin issuer
- **WHEN** валидный JWT содержит issuer URL realm `forgeflow-admins`
- **THEN** authenticated user получает realm `forgeflow-admins`

### Requirement: Subject mapping
Backend SHALL map the JWT `sub` value to `organizations.api.UserId` and SHALL expose it through the authenticated-user contract.

#### Scenario: Subject is propagated
- **WHEN** валидный JWT содержит `sub = user-123`
- **THEN** текущий authenticated user содержит `UserId("user-123")`

### Requirement: Lazy owner registration
ForgeFlow SHALL create an `Owner` without an organization on the first authenticated request from `forgeflow-users` when no `Owner` exists for the JWT `sub`.

#### Scenario: First user login
- **WHEN** пользователь realm `forgeflow-users` с новым `sub` выполняет аутентифицированный запрос
- **THEN** ForgeFlow создаёт для него `Owner` без организации, а запрос не получает организацию автоматически

#### Scenario: Existing owner
- **WHEN** пользователь realm `forgeflow-users` уже имеет `Owner`
- **THEN** ForgeFlow не создаёт второго `Owner`

### Requirement: Frontend user login
Frontend SHALL provide a user login entry point that authenticates against realm `forgeflow-users` using Authorization Code with PKCE.

#### Scenario: User login
- **WHEN** пользователь выбирает вход как пользователь
- **THEN** frontend запускает Keycloak login для `forgeflow-users` и сохраняет полученный access token для API-запросов

### Requirement: Frontend admin login
Frontend SHALL provide a separate admin login entry point that authenticates against realm `forgeflow-admins` using Authorization Code with PKCE.

#### Scenario: Admin login
- **WHEN** пользователь выбирает вход в админку
- **THEN** frontend запускает Keycloak login для `forgeflow-admins` и сохраняет полученный access token для API-запросов

### Requirement: Bearer propagation
Frontend SHALL attach the current access token as `Authorization: Bearer <token>` to every `/api` request made while the user is authenticated.

#### Scenario: Authenticated API request
- **WHEN** frontend выполняет `GET /api/v1/parts` при наличии access token
- **THEN** запрос содержит заголовок `Authorization` с Bearer-токеном

### Requirement: Keycloak realm configuration
Development infrastructure SHALL initialize separate realms `forgeflow-users` and `forgeflow-admins`, each with password login, Google social IdP configuration, and a frontend client.

#### Scenario: Realm import
- **WHEN** Keycloak запускается через project Docker Compose configuration
- **THEN** оба realm'а и их clients доступны для OAuth2 login, а каждый realm имеет password и Google IdP configuration

### Requirement: Real Keycloak integration tests
The authentication test suite SHALL obtain and validate tokens from a real Keycloak instance for both configured realms.

#### Scenario: Integration token validation
- **WHEN** integration tests start Keycloak with imported realms and send requests with tokens issued by each realm
- **THEN** tests verify successful authentication for both realms and rejection of an invalid issuer
