# module-boundaries Specification

## Purpose
Фиксирует девять доменных границ ForgeFlow и правила направленных зависимостей, чтобы монолит сохранял изоляцию модулей: бизнес-модули взаимодействуют через публичные `api`-пакеты, а корневой composition root собирает конкретные адаптеры в Spring-бины.

## Requirements

### Requirement: Объявление модулей ForgeFlow
Приложение SHALL объявлять ровно девять верхнеуровневых модулей `organizations`, `adminpanel`, `technology`, `specifications`, `catalog`, `directories`, `shared`, `security` и `web`.

#### Scenario: Проверка состава модулей
- **WHEN** Spring Modulith строит модель приложения
- **THEN** модель содержит все девять названных модулей и не содержит дополнительного бизнес-модуля

### Requirement: Декларация каждого модуля
Каждый из девяти модулей SHALL иметь `package-info.java` в корневом пакете с аннотацией `@ApplicationModule`.

#### Scenario: Проверка деклараций
- **WHEN** исходный код модулей анализируется
- **THEN** для каждого названного модуля обнаруживается ровно одна корневая декларация `@ApplicationModule`

### Requirement: Публичные API модулей
Межмодульный доступ SHALL выполняться только через `api`-пакет целевого модуля; зависимость `organizations -> shared` SHALL использовать `shared.api` как единственную точку доступа к `shared`.

#### Scenario: Доступ через API
- **WHEN** `organizations` обращается к типу `shared`
- **THEN** обращение направлено в `shared.api`, а обращения к `shared.application`, `shared.domain` и `shared.infrastructure` отсутствуют

### Requirement: Модуль web как тонкий REST-адаптер
Модуль `web` SHALL быть закрытым (`type = CLOSED`) и SHALL зависеть только от публичного API каталога (`catalog :: catalog`); контроллеры `web` SHALL обращаться к `catalog.api` и не SHALL использовать внутренние пакеты других модулей.

#### Scenario: Доступ web через API
- **WHEN** модуль `web` обращается к типу другого модуля
- **THEN** обращение направлено в `catalog.api`, а внутренние пакеты `catalog`, `organizations`, `security` и `shared` не используются

### Requirement: Открытые модули для composition root
Модули `catalog`, `organizations` и `security` SHALL быть открытыми (`type = OPEN`), чтобы composition root (корневой пакет `com.hydroyura.forgeflow`) мог собирать их конкретные адаптеры в Spring-бины без размещения Spring-аннотаций внутри самих модулей.

#### Scenario: Композиция бинов
- **WHEN** composition root объявляет `@Bean` для репозиториев, сервисов и tenant-lifecycle
- **THEN** он получает доступ к реализациям `catalog`, `organizations` и `security`, а доменные/application/infrastructure классы этих модулей не содержат Spring-аннотаций

### Requirement: Разрешённые зависимости модулей
Модульные декларации SHALL разрешать только зависимости, соответствующие предметным связям: `adminpanel` — `organizations`; `technology` — `catalog` и `directories`; `specifications` — `catalog` и `directories`; `catalog` — `directories` и `shared`; `security` — `organizations` и `shared`; `organizations` — `shared`; `web` — `catalog`; `shared` и `directories` — без зависимостей; прочие зависимости SHALL быть запрещены.

#### Scenario: Проверка направленного графа
- **WHEN** выполняется верификация модели модулей
- **THEN** зависимости `organizations -> shared`, `security -> organizations`, `security -> shared`, `catalog -> directories`, `catalog -> shared` и `web -> catalog` проходят проверку, а обратные связи и неперечисленные зависимости отклоняются

#### Scenario: Доступ organizations к shared
- **WHEN** модуль `organizations` использует `OrganizationId`
- **THEN** он обращается к `shared.api.OrganizationId`, а не к пакету `shared.application`, `shared.domain` или `shared.infrastructure`

#### Scenario: Независимые общие модули
- **WHEN** модули `shared` и `directories` анализируются на межмодульные зависимости
- **THEN** каждый из них не зависит от `organizations`, `security`, `catalog` или любого другого бизнес-модуля

### Requirement: Отсутствие циклов модулей
Граф зависимостей девяти модулей SHALL оставаться ацикличным после добавления `organizations -> shared`, `security -> shared`, `catalog -> shared` и `web -> catalog`.

#### Scenario: Проверка отсутствия циклов
- **WHEN** выполняется архитектурная проверка Spring Modulith
- **THEN** проверка не выявляет циклического пути между модулями
