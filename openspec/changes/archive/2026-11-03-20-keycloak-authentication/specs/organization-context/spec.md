# Spec Delta

## MODIFIED Requirements

### Requirement: Разрешение организации по authenticated subject
Резолвер организации SHALL получать `UserId` и realm из порта текущего аутентифицированного пользователя, предоставленного проверенным JWT, и SHALL использовать принадлежность из `organizations` для определения `shared.api.OrganizationId`.

#### Scenario: Пользователь forgeflow-users с принадлежностью
- **WHEN** проверенный JWT имеет issuer realm `forgeflow-users`, а `organizations` возвращает `OwnerOf` или `MemberOf` с `shared.api.OrganizationId`
- **THEN** lifecycle устанавливает этот идентификатор в контекст

#### Scenario: Пользователь без организации
- **WHEN** проверенный JWT принадлежит пользователю без принадлежности организации
- **THEN** lifecycle не устанавливает организацию, и `currentOrganizationId()` возвращает пустой `Optional`

### Requirement: Realm policy
Пользователь realm `forgeflow-admins` SHALL не получать автоматический контекст организации, даже если для его `UserId` существует принадлежность в `organizations`.

#### Scenario: Администраторский realm
- **WHEN** проверенный JWT имеет issuer realm `forgeflow-admins`
- **THEN** resolver не запрашивает организационный контекст для бизнес-запроса и `currentOrganizationId()` остаётся пустым
