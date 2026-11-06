# Proposal

## Why

Модуль `technology` содержит полностью готовый доменный слой (агрегат `TechnologyProcess`, value object `Operation`, доменный сервис `TechnologyProcessService`, driven-порт `TechnologyProcessRepository`, идентификатор `TechnologyProcessId`), но не имеет application-слоя, персистентности и REST-адаптера. Без них технологические процессы невозможно создавать, хранить и использовать из приложения. Настоящий change завершает модуль на backend по образцу уже работающего модуля `planning`.

## What Changes

- Добавить входной порт `TechnologyService` в `technology.api` и его реализацию `TechnologyApplicationService` (use case'ы: создание, получение по id, список с фильтром по детали, переименование, логическое удаление, добавление/удаление операции, назначение/снятие дефолтности).
- Добавить публичные DTO (`CreateTechnologyProcessRequest`, `AddOperationRequest`, `TechnologyProcessView`, `OperationView`) в `technology.api`.
- Добавить JPA-персистентность: сущности `technology_process` / `technology_process_operation`, Spring Data-репозитории, JPA-реализацию порта `TechnologyProcessRepository` и мапперы.
- Добавить Flyway-миграцию `V5__technology.sql` (таблицы процесса и операций + частичный уникальный индекс номера).
- Добавить REST-контроллер `TechnologyProcessController` по базовому пути `/api/v1/technology-processes`.
- Расширить composition root `WebConfig` бинами технологии (репозиторий, маппер, доменный `TechnologyProcessService`, application-сервис) и расширить `RestExceptionHandler` маппингом ошибок технологии.
- Добавить в декларацию модуля `web` разрешённую зависимость `technology :: technology`.
- Добавить минимальную фабрику восстановления агрегата `TechnologyProcess.restore(...)` в домен (единственное точечное дополнение домена, необходимое для полного round-trip персистентности; существующие доменные правила и сигнатуры не меняются).

## Capabilities

### New Capabilities

- `technology`: application-слой и персистентность технологических процессов — use case'ы, tenant-изоляция, валидация, транзакционность, постоянное хранение в PostgreSQL и границы модуля `technology`.
- `technology-rest`: tenant-scoped REST-контракт `/api/v1/technology-processes` (эндпоинты, статусы, коды ошибок, определение организации только серверно).

### Modified Capabilities

- `persistence-foundation`: расширяется перечень модулей, данные которых сохраняются в PostgreSQL и восстанавливаются после перезапуска, — в него добавляется модуль `technology`.

## Impact

- Новые/изменяемые пакеты: `technology.api`, `technology.application`, `technology.infrastructure.persistence`, `technology.domain` (только фабрика `restore`), `web`, корневой `RestExceptionHandler` и `WebConfig`.
- Новая миграция `V5__technology.sql` (следующая после `V4__planning.sql`); `ApplicationModules.verify()` должен проходить с добавленной зависимостью `web -> technology`.
- Тесты: unit-тесты application-сервиса (на in-memory репозитории), тесты мапперов, тесты контроллера (MockMvc standalone), интеграционный тест JPA-репозитория на PostgreSQL; существующие тесты домена остаются зелёными.
- Зависимости: модуль `technology` продолжает зависеть только от `catalog`, `directories` и `shared`; `web` дополнительно зависит от `technology` (через `technology.api`).

## Non-Goals

- Интеграция технологических процессов со спецификациями (закрепление процесса за деталью в позиции спецификации) — вне объёма, отдельный change в `specifications`.
- Фронтенд (React) для технологических процессов — вне объёма.
- Пагинация и полнотекстовый поиск списка процессов — вне объёма; единственный фильтр MVP — по `partId`.
- Переопределение дефолтности при логическом удалении единственного дефолтного процесса (деталь остаётся без дефолтного) — принятый edge case, откладывается в future change.
- Валидация номера процесса по паттерну организации (`OrganizationSettingsProvider`) — future change.
- Оборудование/нормы времени/материалы для операций, переупорядочивание операций, перенос процесса на другую деталь — future changes (уже зафиксированы в change 21).
