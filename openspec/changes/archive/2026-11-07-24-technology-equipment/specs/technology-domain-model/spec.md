# Spec Delta

## MODIFIED Requirements

### Requirement: Операция как value object
Домен technology SHALL предоставлять операцию техпроцесса как неизменяемый value object `Operation` с полями `number` (положительное целое), `name` и необязательной ссылкой `equipmentId` (`technology.api.EquipmentId`, nullable). `Operation` SHALL отклонять `number <= 0` и пустое, пробельное или `null` значение `name` исключением `IllegalArgumentException` (через семантику `StringUtils.isBlank`); `equipmentId = null` SHALL означать операцию без оборудования.

#### Scenario: Валидная операция
- **WHEN** создаётся операция с положительным номером и непустым именем
- **THEN** операция сохраняет номер и имя без изменений

#### Scenario: Операция без оборудования
- **WHEN** создаётся операция без `equipmentId`
- **THEN** операция сохраняет `number` и `name`, а `equipmentId` равен `null`

#### Scenario: Операция с оборудованием
- **WHEN** создаётся операция с непустым `equipmentId`
- **THEN** операция сохраняет ту же ссылку на оборудование

#### Scenario: Некорректный номер операции
- **WHEN** создаётся операция с `number <= 0`
- **THEN** создание завершается `IllegalArgumentException`

#### Scenario: Некорректное имя операции
- **WHEN** создаётся операция с пустым, пробельным или `null` значением `name`
- **THEN** создание завершается `IllegalArgumentException`
