# Spec Delta

## MODIFIED Requirements

### Requirement: Сущность Assembly
Домен каталога SHALL предоставлять сущность `Assembly` с jMolecules-аннотациями `@Entity` и `@Identity`. `Assembly` SHALL содержать идентификатор `AssemblyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Тип организации Assembly
- **WHEN** анализируется поле `organizationId` сущности `Assembly`
- **THEN** его типом является `shared.api.OrganizationId`, а не `UUID`

### Requirement: Сущность Part
Домен каталога SHALL предоставлять сущность `Part` с jMolecules-аннотациями `@Entity` и `@Identity`. `Part` SHALL содержать идентификатор `PartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Тип организации Part
- **WHEN** создаётся или читается `Part`
- **THEN** `organizationId` имеет тип `shared.api.OrganizationId`

### Requirement: Сущность Buy
Домен каталога SHALL предоставлять сущность `Buy` с jMolecules-аннотациями `@Entity` и `@Identity`. `Buy` SHALL содержать идентификатор `BuyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Тип организации Buy
- **WHEN** анализируется поле `organizationId` сущности `Buy`
- **THEN** его типом является `shared.api.OrganizationId`, а не `UUID`

### Requirement: Сущность Standart
Домен каталога SHALL предоставлять сущность `Standart` с jMolecules-аннотациями `@Entity` и `@Identity`. `Standart` SHALL содержать идентификатор `StandartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Тип организации Standart
- **WHEN** анализируется поле `organizationId` сущности `Standart`
- **THEN** его типом является `shared.api.OrganizationId`, а не `UUID`

### Requirement: Сущность Vzk
Домен каталога SHALL предоставлять сущность `Vzk` с jMolecules-аннотациями `@Entity` и `@Identity`. `Vzk` SHALL содержать идентификатор `VzkId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Тип организации Vzk
- **WHEN** анализируется поле `organizationId` сущности `Vzk`
- **THEN** его типом является `shared.api.OrganizationId`, а не `UUID`

### Requirement: Organization и аудит
Каждая сущность каталога SHALL хранить непустой `shared.api.OrganizationId organizationId`, `Instant createdAt` и `Instant updatedAt`; поля аудита SHALL содержать только временные метки и не SHALL включать `createdBy` или `updatedBy`.

#### Scenario: Создание сущности с типизированной организацией
- **WHEN** вызывается factory-метод любой из пяти сущностей с `OrganizationId`
- **THEN** сущность сохраняет переданный типизированный идентификатор организации
