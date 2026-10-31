# Spec Delta

## ADDED Requirements

### Requirement: REST список замен
REST SHALL предоставлять `listSubstitutes(assemblyId, item)` через `GET /api/v1/specifications/{assemblyId}/substitutes` с ссылкой основной позиции в query-параметрах `itemType` и `itemId` и SHALL возвращать статус `200` и массив `CatalogItemReference`.

#### Scenario: Список замен
- **WHEN** клиент запрашивает замены существующей строки с корректными `itemType` и `itemId`
- **THEN** ответ имеет статус `200` и содержит массив ссылок заменителей

### Requirement: REST добавление замены
REST SHALL предоставлять `addSubstitute(assemblyId, item, substituteRef)` через `POST /api/v1/specifications/{assemblyId}/substitutes` и SHALL возвращать статус `201` с добавленной ссылкой.

#### Scenario: Добавление допустимой замены
- **WHEN** тело содержит основную ссылку `item` и однотипную ссылку `substituteRef`, отсутствующую в списке
- **THEN** ответ имеет статус `201` и содержит сохранённую ссылку заменителя

### Requirement: REST удаление замены
REST SHALL предоставлять `removeSubstitute(assemblyId, item, substituteRef)` через `DELETE /api/v1/specifications/{assemblyId}/substitutes` и SHALL возвращать статус `204` после удаления.

#### Scenario: Удаление замены
- **WHEN** тело содержит основную ссылку и существующую ссылку заменителя
- **THEN** ответ имеет статус `204`, а повторный GET списка не содержит удалённую ссылку

### Requirement: REST ошибки заменителей
REST SHALL возвращать `404` с кодом `not_found` для отсутствующей или чужой строки, `409` с кодом `duplicate` для дубликата и `400` с кодом `bad_request` для кросс-типа или самозамены.

#### Scenario: Отсутствующая строка
- **WHEN** запрос списка, добавления или удаления адресует несуществующую строку
- **THEN** ответ имеет статус `404` и код `not_found`

#### Scenario: Дубликат
- **WHEN** POST повторно добавляет уже сохранённого заменителя
- **THEN** ответ имеет статус `409` и код `duplicate`

#### Scenario: Невалидная замена
- **WHEN** POST передаёт заменитель другого типа или совпадающий с основной позицией
- **THEN** ответ имеет статус `400` и код `bad_request`

### Requirement: REST признак замен
Ответ `GET /api/v1/specifications/{assemblyId}/rows` SHALL включать boolean `hasSubstitutes` для каждой строки без изменения существующих полей `item` и `quantity`.

#### Scenario: Строка отмечена
- **WHEN** строка имеет сохранённую замену
- **THEN** JSON содержит `hasSubstitutes: true`

#### Scenario: Строка не отмечена
- **WHEN** строка не имеет замен
- **THEN** JSON содержит `hasSubstitutes: false`

### Requirement: REST tenant-контекст замен
REST SHALL определять организацию через серверный `OrganizationContext` и SHALL НЕ принимать tenant из URL, тела или заголовка для операций замен.

#### Scenario: Чужая строка
- **WHEN** клиент обращается к строке другой организации
- **THEN** ответ имеет статус `404` с кодом `not_found`, а ссылки заменителей не раскрываются
