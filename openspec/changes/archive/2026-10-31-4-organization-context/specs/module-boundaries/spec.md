# Spec Delta

## MODIFIED Requirements

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

### Requirement: Публичные API модулей
Межмодульный доступ SHALL выполняться только через `api`-пакет целевого модуля; зависимость `organizations -> shared` SHALL использовать `shared.api` как единственную точку доступа к `shared`.

#### Scenario: Доступ через API
- **WHEN** `organizations` обращается к типу `shared`
- **THEN** обращение направлено в `shared.api`, а обращения к `shared.application`, `shared.domain` и `shared.infrastructure` отсутствуют

### Requirement: Отсутствие циклов модулей
Граф зависимостей восьми модулей SHALL оставаться ацикличным после добавления `organizations -> shared`, `security -> shared` и `catalog -> shared`.

#### Scenario: Проверка отсутствия циклов
- **WHEN** выполняется архитектурная проверка Spring Modulith
- **THEN** проверка не выявляет циклический путь между модулями
