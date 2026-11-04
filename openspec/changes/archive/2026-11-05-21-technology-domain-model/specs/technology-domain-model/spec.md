# Spec Delta

## Purpose

Определяет доменную модель технологических процессов ForgeFlow: агрегат технологического процесса с операциями, привязкой к деталям каталога и правилами дефолтности, без прикладных сервисов и персистентности.

## ADDED Requirements

### Requirement: Агрегат технологического процесса
Домен technology SHALL предоставлять агрегат `TechnologyProcess` с jMolecules-аннотациями `@Entity` и `@Identity`. Агрегат SHALL содержать идентификатор `TechnologyProcessId`, поля `number` и `name`, ссылку `partId` на деталь каталога, `organizationId`, флаг `defaultProcess`, временные метки `createdAt`/`updatedAt`, nullable `deletedAt` и упорядоченный набор операций.

#### Scenario: Структура агрегата
- **WHEN** анализируется класс `com.hydroyura.forgeflow.technology.domain.TechnologyProcess`
- **THEN** класс имеет аннотации `@Entity` и `@Identity` и поля типов `TechnologyProcessId`, `String`, `String`, `OrganizationId`, `PartId`, `boolean`, `Instant`, `Instant`, `Instant` и коллекцию операций

### Requirement: Типизированный идентификатор TechnologyProcessId
Публичный API technology SHALL предоставлять `TechnologyProcessId` как неизменяемый `record` с компонентом `UUID value`, фабриками `generate()` и `of(UUID)`; конструктор SHALL отклонять `null` исключением `NullPointerException` (конвенция `PartId`/`OrganizationId`).

#### Scenario: Генерация идентификатора
- **WHEN** вызывается `TechnologyProcessId.generate()`
- **THEN** возвращается идентификатор с непустым `UUID`

#### Scenario: Создание из UUID
- **WHEN** вызывается `TechnologyProcessId.of(uuid)`
- **THEN** возвращается идентификатор, содержащий тот же `UUID`

#### Scenario: Отклонение null
- **WHEN** вызывается `TechnologyProcessId.of(null)`
- **THEN** создание завершается `NullPointerException`

### Requirement: Операция как value object
Домен technology SHALL предоставлять операцию техпроцесса как неизменяемый value object `Operation` с полями `number` (положительное целое) и `name`. `Operation` SHALL отклонять `number <= 0` и пустое, пробельное или `null` значение `name` исключением `IllegalArgumentException` (через семантику `StringUtils.isBlank`).

#### Scenario: Валидная операция
- **WHEN** создаётся операция с положительным номером и непустым именем
- **THEN** операция сохраняет номер и имя без изменений

#### Scenario: Некорректный номер операции
- **WHEN** создаётся операция с `number <= 0`
- **THEN** создание завершается `IllegalArgumentException`

#### Scenario: Некорректное имя операции
- **WHEN** создаётся операция с пустым, пробельным или `null` значением `name`
- **THEN** создание завершается `IllegalArgumentException`

### Requirement: Привязка к детали каталога
`TechnologyProcess` SHALL ссылаться на деталь через типизированный идентификатор `catalog.api.PartId`; дублирование полей детали в модели техпроцесса SHALL отсутствовать, а `partId` SHALL быть неизменяемым после создания.

#### Scenario: Ссылка на деталь
- **WHEN** техпроцесс создаётся с заданным `partId`
- **THEN** процесс хранит тот же `PartId` и не содержит полей номера или имени детали

### Requirement: Валидация обязательных атрибутов процесса
`TechnologyProcess` SHALL отклонять при создании пустое или пробельное значение `number` и `name` fail-fast исключением `IllegalArgumentException` с указанием поля.

#### Scenario: Пустой номер процесса
- **WHEN** создаётся техпроцесс с `number = "   "`
- **THEN** создание завершается `IllegalArgumentException`, идентифицирующим поле `number`

#### Scenario: Пустое имя процесса
- **WHEN** создаётся техпроцесс с `name = ""`
- **THEN** создание завершается `IllegalArgumentException`, идентифицирующим поле `name`

### Requirement: Уникальность номера в организации
Два активных (не удалённых) техпроцесса одной организации SHALL не иметь одинаковый `number`. Доменный сервис `TechnologyProcessService` SHALL отклонять регистрацию процесса, номер которого уже занят в организации.

#### Scenario: Дубликат номера
- **WHEN** `TechnologyProcessService.register` вызывается для процесса с номером, уже занятым активным процессом той же организации
- **THEN** регистрация завершается ошибкой и процесс не сохраняется

### Requirement: Организация и аудит
`TechnologyProcess` SHALL хранить непустой `shared.api.OrganizationId organizationId`, `createdAt` и `updatedAt`; поля аудита SHALL содержать только временные метки и SHALL не включать `createdBy` или `updatedBy`.

#### Scenario: Создание с организацией и аудитом
- **WHEN** техпроцесс создаётся с заданными `organizationId`, `createdAt` и `updatedAt`
- **THEN** процесс сохраняет эти значения и не содержит полей идентификатора пользователя аудита

### Requirement: Логическое удаление
`TechnologyProcess` SHALL хранить nullable `Instant deletedAt`, где `deletedAt = null` означает «не удалён». Метод `markDeleted()` SHALL устанавливать момент удаления, обновлять `updatedAt` и SHALL быть идемпотентным.

#### Scenario: Удаление активного процесса
- **WHEN** вызывается `markDeleted()` у активного процесса
- **THEN** `deletedAt` становится ненулевым `Instant`, а `updatedAt` обновляется

#### Scenario: Повторное удаление
- **WHEN** `markDeleted()` вызывается повторно
- **THEN** исходный `deletedAt` не изменяется

### Requirement: Переименование
`TechnologyProcess.rename(String)` SHALL отклонять пустое, пробельное или `null` имя исключением `IllegalArgumentException`, а при валидном имени — обновлять `name` и `updatedAt`.

#### Scenario: Некорректное имя
- **WHEN** вызывается `rename("   ")` или `rename(null)`
- **THEN** метод выбрасывает `IllegalArgumentException`, а имя не меняется

#### Scenario: Корректное имя
- **WHEN** вызывается `rename("Новое имя")`
- **THEN** имя меняется, а `updatedAt` обновляется

### Requirement: Добавление операции
`TechnologyProcess` SHALL предоставлять метод добавления операции, который отклоняет `number <= 0`, номер, уже занятый в данном процессе, и пустое или пробельное имя; валидная операция добавляется в набор.

#### Scenario: Валидное добавление
- **WHEN** в процесс добавляется операция с валидным номером и именем
- **THEN** операция появляется в наборе операций процесса

#### Scenario: Дубликат номера операции
- **WHEN** в процесс добавляется операция с номером, уже существующим в этом процессе
- **THEN** добавление отклоняется

### Requirement: Порядок операций
Операции техпроцесса SHALL быть упорядочены по номеру операции, а коллекция, возвращаемая агрегатом, SHALL быть неизменяемой снаружи.

#### Scenario: Упорядоченный и неизменяемый набор
- **WHEN** в процесс добавляются операции с номерами 20 и 10
- **THEN** `operations()` возвращает их в порядке [10, 20], и возвращаемая коллекция неизменяема

### Requirement: Удаление операции
`TechnologyProcess` SHALL предоставлять метод удаления операции по её номеру; удаление операции с отсутствующим номером SHALL завершаться ошибкой.

#### Scenario: Удаление существующей операции
- **WHEN** удаляется операция по существующему номеру
- **THEN** операция удаляется из набора

#### Scenario: Удаление отсутствующей операции
- **WHEN** удаляется операция по отсутствующему номеру
- **THEN** метод завершается ошибкой

### Requirement: Инвариант дефолтного процесса
Каждый `TechnologyProcess` SHALL хранить флаг `defaultProcess`. Доменный сервис `TechnologyProcessService` SHALL обеспечивать для каждой детали: (i) не более одного процесса с `defaultProcess = true`; (ii) при двух и более процессах — ровно один дефолтный; (iii) при нуле или одном процессе требование «ровно один дефолтный» не применяется.

#### Scenario: Две попытки дефолтности
- **WHEN** через `TechnologyProcessService.markAsDefault` процесс детали помечается дефолтным, а у той же детали уже есть дефолтный процесс
- **THEN** прежний дефолтный процесс перестаёт быть дефолтным, и состояние с двумя дефолтными процессами не возникает

#### Scenario: Снятие единственного дефолтного
- **WHEN** через `TechnologyProcessService.unmarkDefault` снимается дефолтность с единственного дефолтного процесса детали, у которой есть ещё один процесс
- **THEN** операция отклоняется, и состояние с двумя процессами без дефолтного не возникает

#### Scenario: Добавление второго процесса без дефолтного
- **WHEN** регистрируется не-дефолтный процесс для детали, у которой уже есть процесс и ни один не дефолтный
- **THEN** регистрация отклоняется

#### Scenario: Единственный процесс
- **WHEN** у детали ровно один процесс
- **THEN** допустимы оба состояния: `defaultProcess = true` и `defaultProcess = false`

#### Scenario: Отсутствие процессов
- **WHEN** у детали нет процессов
- **THEN** инвариант дефолтности не нарушен

#### Scenario: Повторная установка дефолтности (идемпотентность)
- **WHEN** через `TechnologyProcessService.markAsDefault` помечается дефолтным процесс, который уже является дефолтным
- **THEN** вызов завершается успешно как no-op: флаг остаётся `true`, прочие процессы не изменяются, состояние с двумя дефолтными не возникает

#### Scenario: Повторное снятие дефолтности (идемпотентность)
- **WHEN** через `TechnologyProcessService.unmarkDefault` снимается дефолтность с процесса, который уже не является дефолтным
- **THEN** вызов завершается успешно как no-op: флаг остаётся `false`, состояние не изменяется

### Requirement: Порт репозитория
Домен technology SHALL предоставлять driven port репозитория техпроцессов, независимый от Spring, JPA и хранилища. Порт SHALL предоставлять `save`, чтение по `TechnologyProcessId`, чтение процессов детали по `PartId`, чтение списка процессов организации и проверку уникальности номера; операции чтения SHALL принимать `OrganizationId` явным аргументом и ограничиваться текущей организацией. Отдельная операция физического удаления SHALL отсутствовать.

#### Scenario: Tenant-scoped чтение
- **WHEN** вызывается операция чтения порта
- **THEN** она принимает `OrganizationId` и возвращает только активные процессы этой организации

#### Scenario: Отсутствие физического удаления
- **WHEN** анализируется порт репозитория
- **THEN** операция `delete` отсутствует, а логическое удаление выполняется через `markDeleted()` и `save`

### Requirement: Доменная чистота
Доменный код technology SHALL оставаться независимым от Spring, JPA, PostgreSQL, Flyway и внешних интеграций.

#### Scenario: Проверка импортов
- **WHEN** анализируются импорты доменного пакета technology
- **THEN** отсутствуют зависимости от Spring, JPA, PostgreSQL и Flyway

### Requirement: Границы модуля technology
Модуль technology SHALL обращаться к каталогу только через `catalog.api` (в этом инкременте — `PartId`), а к организации — через `shared.api.OrganizationId`. Декларация `@ApplicationModule` модуля technology SHALL разрешать зависимости `catalog :: catalog`, `directories :: directories` и `shared :: shared` (зависимость `shared :: shared` добавляется для использования `shared.api.OrganizationId`) и SHALL не содержать циклов.

#### Scenario: Разрешённые зависимости
- **WHEN** выполняется `ApplicationModules.verify()`
- **THEN** проверка проходит, граф модулей ацикличен, у technology разрешены зависимости только на `catalog`, `directories` и `shared`, запрещённых зависимостей нет

### Requirement: Минимальность инкремента
В этом инкременте модуль technology SHALL содержать только доменные типы (агрегат, value object, порт репозитория, доменный сервис) и публичный тип `TechnologyProcessId`; application-сервисы, REST-контроллеры, JPA-сущности, Flyway-миграции и фронтенд-код SHALL отсутствовать.

#### Scenario: Состав модуля
- **WHEN** анализируется состав модуля technology
- **THEN** отсутствуют application-сервисы, REST-контроллеры, JPA-сущности, Flyway-миграции и фронтенд-код

### Requirement: Аудит-семантика `updatedAt` мутаторов
Каждый мутатор агрегата `TechnologyProcess` — `rename`, `markDeleted`, `addOperation`, `removeOperation`, `markAsDefault`, `markAsNotDefault` — SHALL обновлять `updatedAt` при фактическом изменении состояния (единая конвенция аудита, как в `Part`). Идемпотентные повторные вызовы, не меняющие состояние (повторный `markDeleted`, повторный `markAsDefault` для уже дефолтного процесса, повторный `markAsNotDefault` для уже не-дефолтного процесса), SHALL не изменять `updatedAt`.

#### Scenario: Аудит при переименовании и удалении
- **WHEN** вызываются `rename` или `markDeleted`, фактически изменяющие состояние
- **THEN** `updatedAt` обновляется

#### Scenario: Аудит при изменении операций
- **WHEN** вызываются `addOperation` или `removeOperation`, изменяющие набор операций
- **THEN** `updatedAt` обновляется

#### Scenario: Аудит при изменении дефолтности
- **WHEN** вызываются `markAsDefault` или `markAsNotDefault`, изменяющие `defaultProcess`
- **THEN** `updatedAt` обновляется

#### Scenario: Идемпотентный вызов без изменения состояния
- **WHEN** вызывается повторный `markDeleted`, или `markAsDefault` для уже дефолтного процесса, или `markAsNotDefault` для уже не-дефолтного процесса
- **THEN** состояние не меняется и `updatedAt` не изменяется

### Requirement: Автоматизированные тесты домена
Доменная модель technology SHALL сопровождаться автоматизированными тестами, покрывающими валидацию, порядок операций, логическое удаление, аудит и инвариант дефолтного процесса.

#### Scenario: Прогон тестов
- **WHEN** запускаются автоматизированные тесты домена technology
- **THEN** все тесты проходят
