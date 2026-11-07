# Spec Delta

## MODIFIED Requirements

### Requirement: Модуль web как тонкий REST-адаптер
Модуль `web` SHALL быть закрытым (`type = CLOSED`) и SHALL зависеть только от публичных API бизнес-модулей (`catalog :: catalog`, `organizations :: organizations`, `planning :: planning`, `specifications :: specifications`, `technology :: technology`, `directories :: directories` и `shared :: shared`); контроллеры `web` SHALL обращаться к соответствующим `*.api` пакетам и SHALL не использовать внутренние пакеты других модулей.

#### Scenario: Доступ web через API
- **WHEN** модуль `web` обращается к типу другого модуля
- **THEN** обращение направлено в публичный `api`-пакет целевого модуля, а внутренние пакеты `catalog`, `organizations`, `security`, `planning`, `specifications`, `technology`, `directories` и `shared` не используются

### Requirement: Открытые модули для composition root
Модули `catalog`, `organizations`, `security`, `specifications`, `planning`, `technology` и `directories` SHALL быть открытыми (`type = OPEN`), чтобы composition root (корневой пакет `com.hydroyura.forgeflow`) мог собирать их конкретные адаптеры в Spring-бины без размещения Spring-аннотаций внутри самих модулей.

#### Scenario: Композиция бинов
- **WHEN** composition root объявляет `@Bean` для репозиториев, сервисов и tenant-lifecycle
- **THEN** он получает доступ к реализациям `catalog`, `organizations`, `security`, `specifications`, `planning`, `technology` и `directories`, а доменные/application/infrastructure классы этих модулей не содержат Spring-аннотаций

### Requirement: Разрешённые зависимости модулей
Модульные декларации SHALL разрешать только зависимости, соответствующие предметным связям: `adminpanel` — `organizations`; `technology` — `catalog`, `directories` и `shared`; `specifications` — `catalog`, `directories` и `shared`; `catalog` — `directories`, `shared` и `organizations`; `planning` — `catalog`, `specifications` и `shared`; `security` — `organizations` и `shared`; `organizations` — `shared`; `web` — `catalog`, `organizations`, `shared`, `specifications`, `planning`, `technology` и `directories`; `directories` — `shared`; `shared` — без зависимостей; прочие зависимости SHALL быть запрещены.

#### Scenario: Проверка направленного графа
- **WHEN** выполняется верификация модели модулей
- **THEN** перечисленные зависимости проходят проверку, а обратные связи и неперечисленные зависимости отклоняются

#### Scenario: Доступ organizations к shared
- **WHEN** модуль `organizations` использует `OrganizationId`
- **THEN** он обращается к `shared.api.OrganizationId`, а не к пакету `shared.application`, `shared.domain` или `shared.infrastructure`

#### Scenario: Независимые общие модули
- **WHEN** модули `shared` и `directories` анализируются на межмодульные зависимости
- **THEN** `shared` не зависит ни от одного бизнес-модуля, а `directories` зависит только от `shared`

### Requirement: Отсутствие циклов модулей
Граф зависимостей модулей SHALL оставаться ацикличным после добавления разрешённых зависимостей, включая `directories -> shared` и `web -> directories`.

#### Scenario: Проверка отсутствия циклов
- **WHEN** выполняется архитектурная проверка Spring Modulith
- **THEN** проверка не выявляет циклического пути между модулями
