# Spec Delta

## MODIFIED Requirements

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

### Requirement: Модуль web как тонкий REST-адаптер
Модуль `web` SHALL быть закрытым (`type = CLOSED`) и SHALL зависеть только от публичного API каталога (`catalog :: catalog`); контроллеры `web` SHALL обращаться к `catalog.api` и не SHALL использовать внутренние пакеты других модулей.

#### Scenario: Доступ web через API
- **WHEN** модуль `web` обращается к типу другого модуля
- **THEN** обращение направлено в `catalog.api`, а внутренние пакеты `catalog`, `organizations`, `security` и `shared` не используются

### Requirement: Открытые модули для composition root
Модули `catalog`, `organizations` и `security` SHALL быть открытыми (`type = OPEN`), чтобы composition root (корневой пакет `com.hydroyura.forgeflow` с `WebConfig`, tenant-фильтром и обработчиком ошибок) мог собирать их конкретные адаптеры в Spring-бины без размещения Spring-аннотаций внутри самих модулей.

#### Scenario: Композиция бинов
- **WHEN** composition root объявляет `@Bean` для репозиториев, сервисов и tenant-lifecycle
- **THEN** он получает доступ к реализациям `catalog`, `organizations` и `security`, а доменные/application/infrastructure классы этих модулей не содержат Spring-аннотаций

### Requirement: Разрешённые зависимости модулей
Модульные декларации SHALL разрешать зависимости `adminpanel -> organizations`; `technology -> catalog, directories`; `specifications -> catalog, directories`; `catalog -> directories, shared`; `security -> organizations, shared`; `organizations -> shared`; `web -> catalog`; `shared` и `directories` — без зависимостей; прочие зависимости SHALL быть запрещены.

#### Scenario: Проверка направленного графа
- **WHEN** выполняется верификация модели модулей
- **THEN** перечисленные зависимости проходят проверку, а обратные связи и неперечисленные зависимости отклоняются

### Requirement: Отсутствие циклов модулей
Граф зависимостей девяти модулей SHALL оставаться ацикличным после добавления модуля `web` и зависимости `web -> catalog`.

#### Scenario: Проверка отсутствия циклов
- **WHEN** выполняется архитектурная проверка Spring Modulith
- **THEN** проверка не выявляет циклического пути между модулями
