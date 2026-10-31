# Spec Delta

## MODIFIED Requirements

### Requirement: Сущность Assembly
Домен каталога SHALL предоставлять сущность `Assembly` с jMolecules-аннотациями `@Entity` и `@Identity`. `Assembly` SHALL содержать идентификатор `AssemblyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности сборочной единицы
- **WHEN** анализируется класс `com.hydroyura.forgeflow.catalog.domain.assembly.Assembly`
- **THEN** класс имеет указанные аннотации и поля типов `AssemblyId`, `String`, `String`, `OrganizationId`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Part
Домен каталога SHALL предоставлять сущность `Part` с jMolecules-аннотациями `@Entity` и `@Identity`. `Part` SHALL содержать идентификатор `PartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности детали
- **WHEN** анализируется класс `com.hydroyura.forgeflow.catalog.domain.part.Part`
- **THEN** класс имеет указанные аннотации и поля типов `PartId`, `String`, `String`, `OrganizationId`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Buy
Домен каталога SHALL предоставлять сущность `Buy` с jMolecules-аннотациями `@Entity` и `@Identity`. `Buy` SHALL содержать идентификатор `BuyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности покупной детали
- **WHEN** анализируется класс `com.hydroyura.forgeflow.catalog.domain.buy.Buy`
- **THEN** класс имеет указанные аннотации и поля типов `BuyId`, `String`, `String`, `OrganizationId`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Standart
Домен каталога SHALL предоставлять сущность `Standart` с jMolecules-аннотациями `@Entity` и `@Identity`. `Standart` SHALL содержать идентификатор `StandartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности стандартного изделия
- **WHEN** анализируется класс `com.hydroyura.forgeflow.catalog.domain.standart.Standart`
- **THEN** класс имеет указанные аннотации и поля типов `StandartId`, `String`, `String`, `OrganizationId`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Vzk
Домен каталога SHALL предоставлять сущность `Vzk` с jMolecules-аннотациями `@Entity` и `@Identity`. `Vzk` SHALL содержать идентификатор `VzkId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности группы ВЗК
- **WHEN** анализируется класс `com.hydroyura.forgeflow.catalog.domain.vzk.Vzk`
- **THEN** класс имеет указанные аннотации и поля типов `VzkId`, `String`, `String`, `OrganizationId`, `Instant`, `Instant`, `Instant`
