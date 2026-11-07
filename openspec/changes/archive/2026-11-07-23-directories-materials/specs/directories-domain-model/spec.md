# Spec Delta

## Purpose

Определяет доменную модель справочников ForgeFlow: агрегаты `Material` (материал) и `Sortament` (сортамент) с типизированными идентификаторами, перечислением типа стандарта, валидацией обязательных полей, неизменяемостью номера и мягким удалением, без прикладных сервисов и персистентности.

## ADDED Requirements

### Requirement: Агрегаты Material и Sortament
Домен `directories` SHALL предоставлять два независимых агрегата `Material` в `domain/material` и `Sortament` в `domain/sortament` с jMolecules-аннотациями `@Entity` и `@Identity`. Каждый агрегат SHALL содержать типизированный идентификатор (`MaterialId`/`SortamentId`), поля `number`, `name`, `standardType` (`StandardType`), `standard`, `unit`, `organizationId`, временные метки `createdAt`/`updatedAt` и nullable `deletedAt`.

#### Scenario: Структура агрегата Material
- **WHEN** анализируется класс `com.hydroyura.forgeflow.directories.domain.material.Material`
- **THEN** класс имеет аннотации `@Entity` и `@Identity` и поля типов `MaterialId`, `String`, `String`, `StandardType`, `String`, `String`, `OrganizationId`, `Instant`, `Instant` и nullable `Instant`

#### Scenario: Структура агрегата Sortament
- **WHEN** анализируется класс `com.hydroyura.forgeflow.directories.domain.sortament.Sortament`
- **THEN** класс имеет аннотации `@Entity` и `@Identity` и поля типов `SortamentId`, `String`, `String`, `StandardType`, `String`, `String`, `OrganizationId`, `Instant`, `Instant` и nullable `Instant`

### Requirement: Перечисление StandardType
Публичный API `directories` SHALL предоставлять перечисление `StandardType` с константами `GOST`, `DIN`, `ISO` и `OTHER`, покрывающее типы стандартов ГОСТ, DIN, ISO и прочие.

#### Scenario: Состав перечисления
- **WHEN** анализируется `StandardType`
- **THEN** перечисление содержит ровно константы `GOST`, `DIN`, `ISO` и `OTHER`

### Requirement: Типизированные идентификаторы MaterialId и SortamentId
Публичный API `directories` SHALL предоставлять `MaterialId` и `SortamentId` как неизменяемые `record` с компонентом `UUID value`, фабриками `generate()` и `of(UUID)`; конструктор SHALL отклонять `null` исключением `NullPointerException` (конвенция `PartId`/`OrganizationId`).

#### Scenario: Генерация идентификаторов
- **WHEN** вызываются `MaterialId.generate()` и `SortamentId.generate()`
- **THEN** возвращаются идентификаторы с непустым `UUID`

#### Scenario: Создание из UUID
- **WHEN** вызываются `MaterialId.of(uuid)` и `SortamentId.of(uuid)`
- **THEN** возвращаются идентификаторы, содержащие тот же `UUID`

#### Scenario: Отклонение null
- **WHEN** вызываются `MaterialId.of(null)` или `SortamentId.of(null)`
- **THEN** создание завершается `NullPointerException`

### Requirement: Валидация обязательных атрибутов
Агрегаты `Material` и `Sortament` SHALL отклонять при создании пустое или пробельное значение `number`, `name`, `standard` или `unit` и `null`-значение `standardType` fail-fast исключением `IllegalArgumentException` с указанием поля.

#### Scenario: Пустое поле
- **WHEN** создаётся материал или сортамент с пустым или пробельным `number`, `name`, `standard` или `unit`
- **THEN** создание завершается `IllegalArgumentException`, идентифицирующим поле

#### Scenario: Null-тип стандарта
- **WHEN** создаётся материал или сортамент с `standardType = null`
- **THEN** создание завершается `IllegalArgumentException`, идентифицирующим `standardType`

### Requirement: Организация и аудит
Агрегаты `Material` и `Sortament` SHALL хранить непустой `shared.api.OrganizationId organizationId`, `createdAt` и `updatedAt`; поля аудита SHALL содержать только временные метки и SHALL не включать `createdBy` или `updatedBy`.

#### Scenario: Создание с организацией и аудитом
- **WHEN** агрегат создаётся с заданными `organizationId`, `createdAt` и `updatedAt`
- **THEN** агрегат сохраняет эти значения и не содержит полей идентификатора пользователя аудита

### Requirement: Неизменяемость number
Поле `number` агрегатов `Material` и `Sortament` SHALL быть неизменяемым после создания; операция обновления SHALL изменять только `name`, `standardType`, `standard` и `unit`.

#### Scenario: Отсутствие мутатора number
- **WHEN** анализируются публичные методы агрегата
- **THEN** отсутствует метод, изменяющий `number`, а операция обновления не принимает `number`

### Requirement: Обновление изменяемых полей
Агрегаты `Material` и `Sortament` SHALL предоставлять операцию обновления, которая изменяет `name`, `standardType`, `standard` и `unit`, обновляет `updatedAt` и SHALL отклонять пустое/пробельное `name`, `standard` или `unit` и `null`-`standardType` исключением `IllegalArgumentException`.

#### Scenario: Успешное обновление
- **WHEN** у активного агрегата вызывается обновление с непустыми `name`, `standard`, `unit` и непустым `standardType`
- **THEN** поля изменяются, `updatedAt` обновляется, а `number` остаётся прежним

#### Scenario: Некорректные значения обновления
- **WHEN** обновление получает пустое `name`, `standard` или `unit` либо `null`-`standardType`
- **THEN** операция завершается `IllegalArgumentException`, а состояние агрегата не изменяется

### Requirement: Логическое удаление
Агрегаты `Material` и `Sortament` SHALL хранить nullable `Instant deletedAt`, где `deletedAt = null` означает «не удалён». Метод `markDeleted()` SHALL устанавливать момент удаления, обновлять `updatedAt` и SHALL быть идемпотентным.

#### Scenario: Удаление активного агрегата
- **WHEN** вызывается `markDeleted()` у активного агрегата
- **THEN** `deletedAt` становится ненулевым `Instant`, а `updatedAt` обновляется

#### Scenario: Повторное удаление
- **WHEN** `markDeleted()` вызывается повторно
- **THEN** исходный `deletedAt` и `updatedAt` не изменяются

### Requirement: Порт репозитория
Домен `directories` SHALL предоставлять driven-порты `MaterialRepository` и `SortamentRepository`, свободные от аннотаций и независимые от Spring, JPA и хранилища. Каждый порт SHALL предоставлять `save`, чтение по типизированному ID, чтение списка записей организации и проверку уникальности `number`; операции чтения SHALL принимать `OrganizationId` явным аргументом и ограничиваться текущей организацией. Отдельная операция физического удаления SHALL отсутствовать.

#### Scenario: Tenant-scoped чтение порта
- **WHEN** вызывается операция чтения порта
- **THEN** она принимает `OrganizationId` и возвращает только активные записи этой организации

#### Scenario: Отсутствие физического удаления
- **WHEN** анализируется порт репозитория
- **THEN** операция `delete` отсутствует, а логическое удаление выполняется через `markDeleted()` и `save`

### Requirement: Доменная чистота directories
Доменный код модуля `directories` SHALL оставаться независимым от Spring, JPA, PostgreSQL, Flyway и внешних интеграций; агрегаты SHALL использовать только jMolecules-аннотации, а порты репозиториев SHALL быть свободными от аннотаций интерфейсами.

#### Scenario: Проверка импортов и аннотаций домена
- **WHEN** анализируются импорты и аннотации доменного пакета `directories.domain`
- **THEN** отсутствуют зависимости и аннотации Spring, JPA, PostgreSQL и Flyway
