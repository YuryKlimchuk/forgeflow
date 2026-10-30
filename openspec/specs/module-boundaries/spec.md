# module-boundaries Specification

## Purpose
Фиксирует восемь доменных границ ForgeFlow и правила направленных зависимостей, чтобы пустой каркас сохранял изоляцию модулей до появления бизнес-реализаций.

## Requirements

### Requirement: Объявление модулей ForgeFlow
Приложение SHALL объявлять ровно восемь верхнеуровневых модулей `organizations`, `adminpanel`, `technology`, `specifications`, `catalog`, `directories`, `shared` и `security`.

#### Scenario: Проверка состава модулей
- **WHEN** Spring Modulith строит модель приложения
- **THEN** модель содержит все восемь названных модулей и не содержит дополнительного бизнес-модуля

### Requirement: Декларация каждого модуля
Каждый из восьми модулей SHALL иметь `package-info.java` в корневом пакете с аннотацией `@ApplicationModule`.

#### Scenario: Проверка деклараций
- **WHEN** исходный код модулей анализируется
- **THEN** для каждого названного модуля обнаруживается ровно одна корневая декларация `@ApplicationModule`

### Requirement: Публичные API модулей
Межмодульный доступ SHALL выполняться только через `api`-пакет целевого модуля; зависимость `organizations -> shared` SHALL использовать `shared.api` как единственную точку доступа к `shared`.

#### Scenario: Доступ через API
- **WHEN** `organizations` обращается к типу `shared`
- **THEN** обращение направлено в `shared.api`, а обращения к `shared.application`, `shared.domain` и `shared.infrastructure` отсутствуют

### Requirement: Разрешённые зависимости модулей
Модульные декларации SHALL разрешать только зависимости, соответствующие предметным связям: `adminpanel` — `organizations`; `technology` — `catalog` и `directories`; `specifications` — `catalog` и `directories`; `catalog` — `directories` и `shared`; `security` — `organizations` и `shared`; `organizations` — `shared`; `shared` и `directories` — без зависимостей; прочие зависимости SHALL быть запрещены.

#### Scenario: Проверка направленного графа
- **WHEN** выполняется верификация модели модулей
- **THEN** зависимости `organizations -> shared`, `security -> organizations`, `security -> shared`, `catalog -> directories` и `catalog -> shared` проходят проверку, а обратные связи и неперечисленные зависимости отклоняются

#### Scenario: Доступ organizations к shared
- **WHEN** модуль `organizations` использует `OrganizationId`
- **THEN** он обращается к `shared.api.OrganizationId`, а не к пакету `shared.application`, `shared.domain` или `shared.infrastructure`

#### Scenario: Независимые общие модули
- **WHEN** модули `shared` и `directories` анализируются на межмодульные зависимости
- **THEN** каждый из них не зависит от `organizations`, `security`, `catalog` или любого другого бизнес-модуля

### Requirement: Отсутствие циклов модулей
Граф зависимостей восьми модулей SHALL оставаться ацикличным после добавления `organizations -> shared`, `security -> shared` и `catalog -> shared`.

#### Scenario: Проверка отсутствия циклов
- **WHEN** выполняется архитектурная проверка Spring Modulith
- **THEN** проверка не выявляет циклического пути между модулями
