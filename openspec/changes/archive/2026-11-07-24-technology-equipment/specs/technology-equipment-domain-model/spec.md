# Spec Delta

## Purpose

Определяет доменную модель оборудования ForgeFlow в модуле `technology`: агрегат `Equipment` с типизированным идентификатором, обязательными полями `number`/`name`/`type`, неизменяемостью номера, мягким удалением и портом хранения, а также расширение value object `Operation` опциональной ссылкой на оборудование.

## ADDED Requirements

### Requirement: Агрегат Equipment
Домен `technology` SHALL предоставлять агрегат `Equipment` в пакете `technology.domain.equipment` с jMolecules-аннотациями `@Entity` и `@Identity`. Агрегат SHALL содержать типизированный идентификатор `EquipmentId`, поля `number`, `name`, `type`, `organizationId`, временные метки `createdAt`/`updatedAt` и nullable `deletedAt`.

#### Scenario: Структура агрегата Equipment
- **WHEN** анализируется класс `com.hydroyura.forgeflow.technology.domain.equipment.Equipment`
- **THEN** класс имеет аннотации `@Entity` и `@Identity` и поля типов `EquipmentId`, `String`, `String`, `String`, `OrganizationId`, `Instant`, `Instant` и nullable `Instant`

### Requirement: Типизированный идентификатор EquipmentId
Публичный API `technology` SHALL предоставлять `EquipmentId` как неизменяемый `record` с компонентом `UUID value`, фабриками `generate()` и `of(UUID)`; конструктор SHALL отклонять `null` исключением `NullPointerException` (конвенция `PartId`/`TechnologyProcessId`).

#### Scenario: Генерация идентификатора
- **WHEN** вызывается `EquipmentId.generate()`
- **THEN** возвращается идентификатор с непустым `UUID`

#### Scenario: Создание из UUID
- **WHEN** вызывается `EquipmentId.of(uuid)`
- **THEN** возвращается идентификатор, содержащий тот же `UUID`

#### Scenario: Отклонение null
- **WHEN** вызывается `EquipmentId.of(null)`
- **THEN** создание завершается `NullPointerException`

### Requirement: Валидация обязательных атрибутов оборудования
Агрегат `Equipment` SHALL отклонять при создании пустое или пробельное значение `number`, `name` или `type` fail-fast исключением `IllegalArgumentException` с указанием поля; `type` SHALL быть обязательной непустой строкой без enum/классификатора.

#### Scenario: Пустое поле
- **WHEN** создаётся оборудование с пустым или пробельным `number`, `name` или `type`
- **THEN** создание завершается `IllegalArgumentException`, идентифицирующим поле

### Requirement: Организация и аудит
Агрегат `Equipment` SHALL хранить непустой `shared.api.OrganizationId organizationId`, `createdAt` и `updatedAt`; поля аудита SHALL содержать только временные метки и SHALL не включать `createdBy` или `updatedBy`.

#### Scenario: Создание с организацией и аудитом
- **WHEN** оборудование создаётся с заданными `organizationId`, `createdAt` и `updatedAt`
- **THEN** агрегат сохраняет эти значения и не содержит полей идентификатора пользователя аудита

### Requirement: Неизменяемость number
Поле `number` агрегата `Equipment` SHALL быть неизменяемым после создания; операция обновления SHALL изменять только `name` и `type`.

#### Scenario: Отсутствие мутатора number
- **WHEN** анализируются публичные методы агрегата
- **THEN** отсутствует метод, изменяющий `number`, а операция обновления не принимает `number`

### Requirement: Обновление изменяемых полей
Агрегат `Equipment` SHALL предоставлять операцию обновления, которая изменяет `name` и `type`, обновляет `updatedAt` и SHALL отклонять пустое или пробельное `name` либо `type` исключением `IllegalArgumentException`.

#### Scenario: Успешное обновление
- **WHEN** у активного оборудования вызывается обновление с непустыми `name` и `type`
- **THEN** поля изменяются, `updatedAt` обновляется, а `number` остаётся прежним

#### Scenario: Некорректные значения обновления
- **WHEN** обновление получает пустое `name` или `type`
- **THEN** операция завершается `IllegalArgumentException`, а состояние агрегата не изменяется

### Requirement: Логическое удаление
Агрегат `Equipment` SHALL хранить nullable `Instant deletedAt`, где `deletedAt = null` означает «не удалён». Метод `markDeleted()` SHALL устанавливать момент удаления, обновлять `updatedAt` и SHALL быть идемпотентным.

#### Scenario: Удаление активного оборудования
- **WHEN** вызывается `markDeleted()` у активного оборудования
- **THEN** `deletedAt` становится ненулевым `Instant`, а `updatedAt` обновляется

#### Scenario: Повторное удаление
- **WHEN** `markDeleted()` вызывается повторно
- **THEN** исходный `deletedAt` и `updatedAt` не изменяются

### Requirement: Порт репозитория оборудования
Домен `technology` SHALL предоставлять driven-порт `EquipmentRepository` в `technology.domain.equipment`, свободный от аннотаций и независимый от Spring, JPA и хранилища. Порт SHALL предоставлять `save`, чтение по `EquipmentId`, чтение списка записей организации и проверку уникальности `number`; операции чтения SHALL принимать `OrganizationId` явным аргументом и ограничиваться текущей организацией. Отдельная операция физического удаления SHALL отсутствовать.

#### Scenario: Tenant-scoped чтение порта
- **WHEN** вызывается операция чтения порта
- **THEN** она принимает `OrganizationId` и возвращает только активное оборудование этой организации

#### Scenario: Отсутствие физического удаления
- **WHEN** анализируется порт `EquipmentRepository`
- **THEN** операция `delete` отсутствует, а логическое удаление выполняется через `markDeleted()` и `save`

### Requirement: Расширение Operation ссылкой на оборудование
Value object `Operation` домена `technology` SHALL дополнительно нести необязательную ссылку `equipmentId` типа `technology.api.EquipmentId` (nullable); операция SHALL иметь не более одной ссылки на оборудование, а `equipmentId = null` SHALL означать операцию без оборудования. Существующие валидации `number > 0` и непустого `name` SHALL сохраняться.

#### Scenario: Операция без оборудования
- **WHEN** создаётся операция без `equipmentId`
- **THEN** операция сохраняет `number` и `name`, а `equipmentId` равен `null`

#### Scenario: Операция с оборудованием
- **WHEN** создаётся операция с непустым `equipmentId`
- **THEN** операция сохраняет ту же ссылку на оборудование

#### Scenario: Сохранение существующей валидации
- **WHEN** создаётся операция с `number <= 0` или пустым `name`
- **THEN** создание завершается `IllegalArgumentException`

### Requirement: Доменная чистота оборудования
Доменный код оборудования (`Equipment`, порт `EquipmentRepository`, доменные исключения) SHALL оставаться независимым от Spring, JPA, PostgreSQL, Flyway и внешних интеграций; агрегат SHALL использовать только jMolecules-аннотации, а порт репозитория SHALL быть свободным от аннотаций интерфейсом. `Operation` SHALL ссылаться на `EquipmentId` из `technology.api` (та же конвенция, что `PartId`/`TechnologyProcessId`).

#### Scenario: Проверка импортов и аннотаций домена оборудования
- **WHEN** анализируются импорты и аннотации пакета `technology.domain.equipment`
- **THEN** отсутствуют зависимости и аннотации Spring, JPA, PostgreSQL и Flyway

### Requirement: Размещение оборудования в границах модуля technology
Оборудование SHALL размещаться в модуле `technology` (а не в `directories`) и SHALL не вводить новых межмодульных зависимостей: модуль `technology` продолжает зависеть только от `catalog`, `directories` и `shared`.

#### Scenario: Отсутствие новых межмодульных зависимостей
- **WHEN** выполняется `ApplicationModules.verify()`
- **THEN** проверка проходит с ацикличным графом, а у `technology` разрешены зависимости только на `catalog`, `directories` и `shared`
