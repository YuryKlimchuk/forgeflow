# Spec Delta

## MODIFIED Requirements

### Requirement: Сущность Assembly
Домен каталога SHALL предоставлять сущность `Assembly` с jMolecules-аннотациями `@Entity` и `@Identity`. `Assembly` SHALL содержать идентификатор `AssemblyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности сборочной единицы
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.assembly.Assembly`
- **THEN** класс имеет указанные аннотации и поля типов `AssemblyId`, `String`, `String`, `UUID`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Part
Домен каталога SHALL предоставлять сущность `Part` с jMolecules-аннотациями `@Entity` и `@Identity`. `Part` SHALL содержать идентификатор `PartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности детали
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.part.Part`
- **THEN** класс имеет указанные аннотации и поля типов `PartId`, `String`, `String`, `UUID`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Buy
Домен каталога SHALL предоставлять сущность `Buy` с jMolecules-аннотациями `@Entity` и `@Identity`. `Buy` SHALL содержать идентификатор `BuyId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности покупной детали
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.buy.Buy`
- **THEN** класс имеет указанные аннотации и поля типов `BuyId`, `String`, `String`, `UUID`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Standart
Домен каталога SHALL предоставлять сущность `Standart` с jMolecules-аннотациями `@Entity` и `@Identity`. `Standart` SHALL содержать идентификатор `StandartId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности стандартного изделия
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.standart.Standart`
- **THEN** класс имеет указанные аннотации и поля типов `StandartId`, `String`, `String`, `UUID`, `Instant`, `Instant`, `Instant`

### Requirement: Сущность Vzk
Домен каталога SHALL предоставлять сущность `Vzk` с jMolecules-аннотациями `@Entity` и `@Identity`. `Vzk` SHALL содержать идентификатор `VzkId` и поля `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и nullable `deletedAt`.

#### Scenario: Проверка сущности группы ВЗК
- **WHEN** анализируется класс `com.forgeflow.catalog.domain.vzk.Vzk`
- **THEN** класс имеет указанные аннотации и поля типов `VzkId`, `String`, `String`, `UUID`, `Instant`, `Instant`, `Instant`

### Requirement: Минимальность доменной модели
Домен каталога SHALL содержать пять перечисленных сущностей, пять типизированных идентификаторов и по одному доменному repository port для каждой сущности; дополнительные сущности, бизнес-правила, use case'ы и инфраструктурные адаптеры SHALL отсутствовать.

#### Scenario: Проверка границ изменения
- **WHEN** проверяется состав классов модуля `catalog`
- **THEN** обнаруживаются пять сущностей, пять ID и пять repository port'ов без use case'ов и production-инфраструктурных адаптеров

## ADDED Requirements

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
Каждая сущность каталога SHALL хранить непустой `UUID organizationId`, `Instant createdAt` и `Instant updatedAt`; поля аудита SHALL содержать только временные метки и не SHALL включать `createdBy` или `updatedBy`.

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
Домен каталога SHALL предоставлять отдельный driven port для каждой сущности с операциями `save(entity)` и `delete(id)`. Операция `delete(id)` SHALL означать логическое удаление, а порты SHALL находиться в доменном слое и не зависеть от Spring, JPA или хранилища.

#### Scenario: Сохранение сущности через порт
- **WHEN** вызывается `save(entity)` соответствующего порта
- **THEN** порт принимает сущность именно своего типа без зависимости от инфраструктурного типа

#### Scenario: Удаление через порт
- **WHEN** вызывается `delete(id)` соответствующего порта
- **THEN** контракт операции означает вызов логического удаления по типизированному ID, а не физическое удаление записи
