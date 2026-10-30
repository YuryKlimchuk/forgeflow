# Spec Delta

## MODIFIED Requirements

### Requirement: Получение принадлежности
Система SHALL возвращать принадлежность указанного `UserId` к его организации и роль `owner` или `member`, либо результат «без организации». `organizations` SHALL использовать `shared.api.OrganizationId` во всех контрактах и моделях, где требуется идентификатор организации, а `UserId` и `OwnerId` SHALL оставаться в `organizations.api`; `OrganizationId` SHALL быть удалён из `organizations.api`. Существующий вызов без аргумента SHALL получать текущий subject через application port и делегировать вызову с явным `UserId`; остальная логика организаций SHALL оставаться без изменений.

#### Scenario: Владелец связан с организацией
- **WHEN** Owner запрашивает свою принадлежность после создания организации
- **THEN** система возвращает `shared.api.OrganizationId` и роль `owner`

#### Scenario: Владелец без организации
- **WHEN** Owner запрашивает принадлежность с `organizationId = null`
- **THEN** система возвращает результат «без организации»

#### Scenario: Участник получает принадлежность
- **WHEN** пользователь имеет Membership в организации
- **THEN** система возвращает `shared.api.OrganizationId` и роль `member`

#### Scenario: Явный владелец связан с организацией
- **WHEN** публичный сервис получает `getAffiliation(userId)` для Owner, связанного с организацией
- **THEN** сервис возвращает `Affiliation.OwnerOf` с `shared.api.OrganizationId` этого Owner

#### Scenario: Явный участник получает принадлежность
- **WHEN** публичный сервис получает `getAffiliation(userId)` для пользователя с Membership
- **THEN** сервис возвращает `Affiliation.MemberOf` с `shared.api.OrganizationId` Membership

#### Scenario: Явный пользователь без организации
- **WHEN** публичный сервис получает `getAffiliation(userId)` для Owner без организации или неизвестного пользователя
- **THEN** сервис возвращает `Affiliation.None`

#### Scenario: Совместимость вызова текущего subject
- **WHEN** вызывается существующий `getAffiliation()` без аргумента
- **THEN** он использует subject из application port и возвращает тот же результат, что `getAffiliation(subject)`

#### Scenario: Импорт идентификатора организацией
- **WHEN** `Organization`, `Owner`, `OrganizationView`, `Affiliation` или `OrganizationService` объявляет `OrganizationId`
- **THEN** типом является `shared.api.OrganizationId`, а обращение выполняется через `shared.api`

#### Scenario: Идентификаторы subject
- **WHEN** публичный контракт `organizations` объявляет `UserId` или `OwnerId`
- **THEN** соответствующий тип находится в `organizations.api`
