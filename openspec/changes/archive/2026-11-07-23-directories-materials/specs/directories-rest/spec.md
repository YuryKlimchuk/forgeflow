# Spec Delta

## Purpose

Предоставляет tenant-scoped HTTP-контракт для управления справочниками материалов и сортаментов по путям `/api/v1/materials` и `/api/v1/sortaments`.

## ADDED Requirements

### Requirement: REST создание, чтение и список
REST SHALL предоставлять для каждого ресурса `POST /api/v1/{materials|sortaments}` (201, возвращает представление), `GET /api/v1/{materials|sortaments}/{id}` (200) и `GET /api/v1/{materials|sortaments}` (200, массив представлений).

#### Scenario: Создание и чтение
- **WHEN** клиент отправляет валидные `number`, `name`, `standardType`, `standard` и `unit`
- **THEN** POST возвращает статус 201 с представлением, а GET созданной записи возвращает статус 200

#### Scenario: Список
- **WHEN** клиент запрашивает список ресурса
- **THEN** ответ содержит активные записи текущей организации, отсортированные по `number`

### Requirement: REST обновление и удаление
REST SHALL предоставлять для каждого ресурса `PUT /api/v1/{materials|sortaments}/{id}` (204, без тела) и `DELETE /api/v1/{materials|sortaments}/{id}` (204, мягкое удаление).

#### Scenario: Обновление и удаление
- **WHEN** клиент обновляет запись и затем удаляет её
- **THEN** обе операции возвращают 204, а последующее чтение записи возвращает 404

### Requirement: REST полное обновление изменяемых полей
`PUT /{id}` SHALL принимать тело с изменяемыми полями `name`, `standardType`, `standard` и `unit` и SHALL не принимать и не изменять `number`.

#### Scenario: Обновление без номера
- **WHEN** клиент отправляет PUT с телом `{name, standardType, standard, unit}` без `number`
- **THEN** операция возвращает 204, а `number` записи не изменяется

### Requirement: REST ошибки справочников
REST SHALL возвращать 404 с кодом `not_found` для отсутствующей, удалённой или чужой записи, 409 с кодом `duplicate` для дубликата номера в организации и 400 с кодом `bad_request` для ошибок валидации.

#### Scenario: Отсутствующий ресурс
- **WHEN** запрос адресует чужую, удалённую или отсутствующую запись
- **THEN** ответ имеет статус 404 и код `not_found`

#### Scenario: Дубликат номера
- **WHEN** создание получает номер, уже занятый активной записью организации
- **THEN** ответ имеет статус 409 и код `duplicate`

#### Scenario: Ошибка валидации
- **WHEN** запрос содержит пустой `number`, `name`, `standard` или `unit` либо `null`-`standardType`
- **THEN** ответ имеет статус 400 и код `bad_request`

### Requirement: REST tenant-контекст
REST SHALL определять организацию только через серверный `OrganizationContext` и SHALL игнорировать клиентский заголовок `X-Organization-Id` и tenant в URL или теле запроса.

#### Scenario: Игнорирование клиентского tenant
- **WHEN** клиент организации A передаёт заголовок `X-Organization-Id` организации B
- **THEN** операция выполняется в контексте организации A, а данные организации B не раскрываются

### Requirement: Модуль web зависит от directories
Декларация модуля `web` SHALL включать разрешённую зависимость `directories :: directories`, а обращение SHALL выполняться только через `directories.api`; граф модулей SHALL оставаться ацикличным.

#### Scenario: Проверка зависимости web
- **WHEN** выполняется `ApplicationModules.verify()`
- **THEN** проверка проходит, зависимость `web -> directories` разрешена, обращение направлено в `directories.api`, а циклов в графе нет
