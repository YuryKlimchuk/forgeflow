# Spec Delta

## MODIFIED Requirements

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
