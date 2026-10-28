# catalog-domain-model Specification

## Purpose
Фиксирует минимальную доменную модель каталога изделий, чтобы другие части ForgeFlow могли ссылаться на пять различимых типов изделий по типизированным идентификаторам без преждевременного добавления свойств и бизнес-правил.

## Requirements

### Requirement: Сущность Assembly
Домен каталога SHALL предоставлять сущность `Assembly` с jMolecules-аннотациями `@Entity` и `@Identity`. `Assembly` SHALL содержать ровно одно поле — `AssemblyId`.

#### Scenario: Проверка сущности сборочной единицы
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.assembly.Assembly`
- **THEN** класс имеет обе указанные аннотации и единственное поле типа `AssemblyId`

### Requirement: Сущность Part
Домен каталога SHALL предоставлять сущность `Part` с jMolecules-аннотациями `@Entity` и `@Identity`. `Part` SHALL содержать ровно одно поле — `PartId`.

#### Scenario: Проверка сущности детали
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.part.Part`
- **THEN** класс имеет обе указанные аннотации и единственное поле типа `PartId`

### Requirement: Сущность Buy
Домен каталога SHALL предоставлять сущность `Buy` с jMolecules-аннотациями `@Entity` и `@Identity`. `Buy` SHALL содержать ровно одно поле — `BuyId`.

#### Scenario: Проверка сущности покупной детали
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.buy.Buy`
- **THEN** класс имеет обе указанные аннотации и единственное поле типа `BuyId`

### Requirement: Сущность Standart
Домен каталога SHALL предоставлять сущность `Standart` с jMolecules-аннотациями `@Entity` и `@Identity`. `Standart` SHALL содержать ровно одно поле — `StandartId`.

#### Scenario: Проверка сущности стандартного изделия
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.standart.Standart`
- **THEN** класс имеет обе указанные аннотации и единственное поле типа `StandartId`

### Requirement: Сущность Vzk
Домен каталога SHALL предоставлять сущность `Vzk` с jMolecules-аннотациями `@Entity` и `@Identity`. `Vzk` SHALL содержать ровно одно поле — `VzkId`.

#### Scenario: Проверка сущности группы ВЗК
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.vzk.Vzk`
- **THEN** класс имеет обе указанные аннотации и единственное поле типа `VzkId`

### Requirement: Типизированный идентификатор Assembly
Публичный API каталога SHALL предоставлять `AssemblyId` как неизменяемый `record` с компонентом `UUID value`, фабрикой `generate()` и фабрикой `of(UUID)`.

#### Scenario: Генерация идентификатора Assembly
- **WHEN** вызывается `AssemblyId.generate()`
- **THEN** возвращается `AssemblyId` с непустым значением `UUID`

### Requirement: Типизированный идентификатор Part
Публичный API каталога SHALL предоставлять `PartId` как неизменяемый `record` с компонентом `UUID value`, фабрикой `generate()` и фабрикой `of(UUID)`.

#### Scenario: Создание идентификатора Part
- **WHEN** вызывается `PartId.of(uuid)` с заданным UUID
- **THEN** возвращается `PartId`, содержащий тот же UUID

### Requirement: Типизированный идентификатор Buy
Публичный API каталога SHALL предоставлять `BuyId` как неизменяемый `record` с компонентом `UUID value`, фабрикой `generate()` и фабрикой `of(UUID)`.

#### Scenario: Генерация идентификатора Buy
- **WHEN** вызывается `BuyId.generate()`
- **THEN** возвращается `BuyId` с непустым значением `UUID`

### Requirement: Типизированный идентификатор Standart
Публичный API каталога SHALL предоставлять `StandartId` как неизменяемый `record` с компонентом `UUID value`, фабрикой `generate()` и фабрикой `of(UUID)`.

#### Scenario: Создание идентификатора Standart
- **WHEN** вызывается `StandartId.of(uuid)` с заданным UUID
- **THEN** возвращается `StandartId`, содержащий тот же UUID

### Requirement: Типизированный идентификатор Vzk
Публичный API каталога SHALL предоставлять `VzkId` как неизменяемый `record` с компонентом `UUID value`, фабрикой `generate()` и фабрикой `of(UUID)`.

#### Scenario: Генерация идентификатора Vzk
- **WHEN** вызывается `VzkId.generate()`
- **THEN** возвращается `VzkId` с непустым значением `UUID`

### Requirement: Минимальность доменной модели
Домен каталога SHALL содержать только пять перечисленных сущностей и их идентификаторы в рамках этого изменения; репозитории, use case'ы, дополнительные поля, бизнес-операции и инфраструктурные адаптеры SHALL отсутствовать.

#### Scenario: Проверка границ изменения
- **WHEN** проверяется состав добавленных классов модуля `catalog`
- **THEN** обнаруживаются только пять сущностей и пять ID, без репозиториев, use case'ов и дополнительных доменных свойств
