# Proposal

## Why

Модуль `directories` существует только как пустой скелет (`package-info.java` в `api`/`application`/`domain`/`infrastructure` без классов). По карте саб-доменов (`docs/catalog-subdomains.md`) раздел «Материалы» ЕСКД отнесён к `directories`, но без backend-реализации справочники материалов и сортаментов невозможно создавать, хранить и использовать из приложения. Настоящий change реализует backend двух справочников по образцу уже работающих модулей `catalog` (CRUD-агрегат) и `technology` (application + JPA + Flyway + REST + точечная трансляция гонки уникальности).

## What Changes

- Реализовать два независимых доменных агрегата `Material` (материал) в `directories/domain/material` и `Sortament` (сортамент) в `directories/domain/sortament` с типизированными ID, полями `number`/`name`/`standardType`/`standard`/`unit`, организацией, аудитом и мягким удалением.
- Добавить публичный контракт в `directories/api`: `MaterialId`, `SortamentId`, `StandardType` (`GOST`/`DIN`/`ISO`/`OTHER`), `MaterialService`/`SortamentService`, `*View`, `Create*Request`, `Update*Request`.
- Добавить application-слой: `MaterialApplicationService`/`SortamentApplicationService` с tenant-изоляцией через `OrganizationContext`, проверкой уникальности `number` в организации и точечным маппингом исключений в домене.
- Добавить JPA-персистентность: сущности `material`/`sortament`, Spring Data-репозитории, JPA-реализации доменных портов и мапперы.
- Добавить Flyway-миграцию `V6__directories.sql` (таблицы `material`/`sortament` + частичные уникальные индексы `(organization_id, number) WHERE deleted_at IS NULL`).
- Добавить тонкие REST-контроллеры `MaterialController` (`/api/v1/materials`) и `SortamentController` (`/api/v1/sortaments`) с пятью эндпоинтами (create/get/list/update/delete).
- Расширить composition root `WebConfig` бинами `directories` и `RestExceptionHandler` маппингом ошибок `directories` (404/409/400).
- Обновить декларации модулей: `directories` → `type = OPEN` + `allowedDependencies = {"shared :: shared"}`; `web` → добавить `"directories :: directories"`.
- Дополнить идемпотентный `seed.sql` демо-материалами и демо-сортаментами демо-организации (ГОСТ/DIN/ISO, `ON CONFLICT DO NOTHING`).
- Добавить автоматизированные тесты: домен, application (in-memory), мапперы, JPA-адаптеры (Testcontainers/PostgreSQL), контроллеры (standalone MockMvc), чистота домена.

## Capabilities

### New Capabilities

- `directories-domain-model`: доменная модель справочников — агрегаты `Material`/`Sortament`, перечисление `StandardType`, типизированные ID, валидация полей, неизменяемость `number`, обновление изменяемых полей и мягкое удаление.
- `directories`: поведение управления материалами и сортаментами — создание, чтение, список, обновление, мягкое удаление, уникальность номера в организации, tenant-изоляция, сортировка, хранение в PostgreSQL.
- `directories-rest`: tenant-scoped REST-контракт `/api/v1/materials` и `/api/v1/sortaments` — эндпоинты, статусы, коды ошибок, определение организации только серверно.

### Modified Capabilities

- `module-boundaries`: декларация модуля `directories` становится `OPEN` и получает разрешённую зависимость `shared :: shared`; модуль `web` получает разрешённую зависимость `directories :: directories`.
- `persistence-foundation`: перечень постоянно хранимых модулей расширяется модулем `directories` (Flyway V6).

## Impact

- Новые/изменяемые пакеты: `directories.api`, `directories.domain.material`, `directories.domain.sortament`, `directories.application`, `directories.infrastructure.persistence`, `web`, корневой `WebConfig` и `RestExceptionHandler`.
- Новая миграция `V6__directories.sql` (следующая после `V5__technology.sql`); `ApplicationModules.verify()` должен проходить с изменёнными декларациями `directories` и `web`.
- `seed.sql` дополняется идемпотентными вставками; приложение по-прежнему не выполняет `seed.sql` автоматически.
- Тесты: доменные юнит-тесты, тесты application-сервисов (in-memory), тесты мапперов, тесты JPA-адаптеров на PostgreSQL, тесты контроллеров (MockMvc standalone), тест чистоты домена; существующие тесты остаются зелёными.
- Зависимости: `directories` начинает зависеть только от `shared :: shared`; `web` дополнительно зависит от `directories :: directories` (через `directories.api`). Модули `catalog`/`technology`/`specifications` уже декларируют зависимость `directories :: directories` — изменений не требуют.

## Non-Goals

- Связь «материал доступен в таких-то сортаментах» и использование материалов/сортаментов в строках спецификаций — вне объёма (A3).
- Отдельный справочник стандартов и отдельный справочник единиц измерения (с коэффициентами пересчёта) — вне объёма (A1, A2); `standardType`/`standard`/`unit` — атрибуты.
- Regex-паттерны номеров для материалов/сортаментов на уровне организации и расширение `OrganizationSettings` — вне объёма (A5).
- Фронтенд (React) для справочников — вне объёма.
- Пагинация и поиск списков — вне объёма; единственный контракт MVP — полный список активных записей организации.
