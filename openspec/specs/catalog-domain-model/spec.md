# catalog-domain-model Specification

## Purpose
Фиксирует минимальную доменную модель каталога изделий, чтобы другие части ForgeFlow могли ссылаться на пять различимых типов изделий по типизированным идентификаторам без преждевременного добавления свойств и бизнес-правил.

## Requirements

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
Домен каталога SHALL содержать пять перечисленных сущностей, пять типизированных идентификаторов и по одному доменному repository port для каждой сущности; дополнительные сущности, бизнес-правила, use case'ы и инфраструктурные адаптеры SHALL отсутствовать.

#### Scenario: Проверка границ изменения
- **WHEN** проверяется состав классов модуля `catalog`
- **THEN** обнаруживаются пять сущностей, пять ID и пять repository port'ов без use case'ов и production-инфраструктурных адаптеров

### Requirement: Валидация обязательных атрибутов
Каждая сущность каталога SHALL отклонять при создании значение `number`, являющееся `null`, пустым или состоящим только из пробельных символов. Каждая сущность SHALL аналогично отклонять значение `name`.

#### Scenario: Непустые обязательные атрибуты
- **WHEN** создаётся сущность с непустыми `number` и `name`
- **THEN** сущность создаётся, а переданные значения сохраняются без изменения

#### Scenario: Пустой номер
- **WHEN** создаётся сущность с `number = "   "`
- **THEN** создание завершается fail-fast исключением `IllegalArgumentException` с сообщением, идентифицирующим поле `number`

#### Scenario: Пустое имя
- **WHEN** создаётся сущность с `name = ""`
- **THEN** создание завершается fail-fast исключением `IllegalArgumentException` с сообщением, идентифицирующим поле `name`

### Requirement: Organization и аудит
Каждая сущность каталога SHALL хранить непустой `shared.api.OrganizationId organizationId`, `Instant createdAt` и `Instant updatedAt`; поля аудита SHALL содержать только временные метки и не SHALL включать `createdBy` или `updatedBy`.

#### Scenario: Создание сущности с организацией
- **WHEN** создаётся сущность с заданными `organizationId`, `createdAt` и `updatedAt`
- **THEN** сущность сохраняет эти значения и не содержит полей идентификатора пользователя аудита

### Requirement: Логическое удаление
Каждая сущность каталога SHALL хранить nullable `Instant deletedAt`; `deletedAt = null` SHALL означать, что сущность не удалена. Доменный метод `markDeleted()` SHALL устанавливать момент удаления и SHALL быть идемпотентным.

#### Scenario: Логическое удаление активной сущности
- **WHEN** вызывается `markDeleted()` у сущности с `deletedAt = null`
- **THEN** `deletedAt` получает ненулевой `Instant`, физический объект не уничтожается, а `updatedAt` обновляется

#### Scenario: Повторное логическое удаление
- **WHEN** `markDeleted()` вызывается повторно
- **THEN** сущность остаётся удалённой, а исходный `deletedAt` не заменяется

### Requirement: Порты репозиториев каталога
Домен каталога SHALL предоставлять отдельный driven port для каждой сущности; порты SHALL находиться в доменном слое и не зависеть от Spring, JPA или хранилища. Каждый порт SHALL предоставлять `save(entity)`, `findById(id, organizationId)`, `findAll(organizationId)` и `existsByNumber(number, organizationId)` с явной передачей `OrganizationId`; отдельная операция `delete` SHALL отсутствовать, а логическое удаление SHALL выполняться через `markDeleted()` и `save`.

#### Scenario: Tenant-scoped чтение
- **WHEN** вызывается операция чтения любого repository-порта
- **THEN** она принимает `OrganizationId` и возвращает только активные записи этой организации

#### Scenario: Soft-delete через порт
- **WHEN** сервис удаляет сущность каталога
- **THEN** он вызывает `markDeleted()` и сохраняет сущность через `save`, не вызывая `delete`

### Requirement: Переименование Assembly
Сущность `Assembly` SHALL предоставлять метод `rename(String)`, который отклоняет null, пустое или пробельное имя и обновляет `name` и `updatedAt` при валидном имени.

#### Scenario: Переименование сборки
- **WHEN** `rename` получает непустое имя
- **THEN** имя меняется, а `updatedAt` становится моментом не раньше предыдущего значения

#### Scenario: Некорректное имя сборки
- **WHEN** `rename` получает строку, состоящую только из пробелов
- **THEN** метод выбрасывает `IllegalArgumentException`, а имя не меняется

### Requirement: Переименование Buy
Сущность `Buy` SHALL предоставлять метод `rename(String)`, который отклоняет null, пустое или пробельное имя и обновляет `name` и `updatedAt` при валидном имени.

#### Scenario: Переименование покупного изделия
- **WHEN** `rename` получает непустое имя
- **THEN** имя меняется, а `updatedAt` обновляется

### Requirement: Переименование Standart
Сущность `Standart` SHALL предоставлять метод `rename(String)`, который отклоняет null, пустое или пробельное имя и обновляет `name` и `updatedAt` при валидном имени.

#### Scenario: Переименование стандарта
- **WHEN** `rename` получает непустое имя
- **THEN** имя меняется, а `updatedAt` обновляется

### Requirement: Переименование Vzk
Сущность `Vzk` SHALL предоставлять метод `rename(String)`, который отклоняет null, пустое или пробельное имя и обновляет `name` и `updatedAt` при валидном имени.

#### Scenario: Переименование ВЗК
- **WHEN** `rename` получает непустое имя
- **THEN** имя меняется, а `updatedAt` обновляется
