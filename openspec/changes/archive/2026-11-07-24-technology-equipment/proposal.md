# Proposal

## Why

Модуль `technology` реализует технологические процессы и их операции (change 21–22), но в системе нет оборудования: операцию нельзя связать с конкретной единицей оборудования, на которой она выполняется, а сам справочник оборудования отсутствует. Настоящий change добавляет справочник оборудования `Equipment` в модуль `technology` и опциональную привязку одной единицы оборудования к операции, замыкая «оборудование» из ранее явно отложенного объёма (change 21/22 фиксировали «оборудование / нормы времени / материалы для операций» как future change). Нормы времени, материалы операций и производительность остаются вне объёма.

## What Changes

- Добавить доменный агрегат `Equipment` в модуле `technology` (пакет `technology/domain/equipment`) с полями `id` (`EquipmentId`, типизированный UUID), `number` (обозначение, неизменяемое), `name` (наименование), `type` (тип оборудования, свободная обязательная строка), `organizationId`, `createdAt`, `updatedAt`, `deletedAt` (логическое удаление, nullable).
- Добавить публичный контракт в `technology/api`: `EquipmentId`, `EquipmentService`, `EquipmentView`, `CreateEquipmentRequest`, `UpdateEquipmentRequest`.
- Добавить application-слой `EquipmentApplicationService` с tenant-изоляцией через `OrganizationContext`, уникальностью `number` в организации, сортировкой по `number` и CRUD.
- Добавить JPA-персистентность: `EquipmentEntity`, `EquipmentJpaRepository`, `JpaEquipmentRepository`, инфраструктурный маппер.
- Добавить Flyway-миграцию `V7__technology_equipment.sql`: таблица `equipment` + частичный уникальный индекс `equipment_number_org_uq` на `(organization_id, number) WHERE deleted_at IS NULL`; в существующую `technology_process_operation` добавляется nullable колонка `equipment_id UUID` **без** FK.
- Расширить value object `Operation` опциональным `equipmentId` (`EquipmentId`, nullable); расширить `AddOperationRequest` и `OperationView`; проверять существование активного оборудования текущей организации при добавлении операции (существующие эндпоинты не ломаются).
- Добавить тонкий REST-контроллер `EquipmentController` (`/api/v1/equipment`, CRUD) и аддитивно расширить `POST /api/v1/technology-processes/{id}/operations` полем `equipmentId`.
- Расширить composition root `WebConfig` бинами оборудования и `RestExceptionHandler` маппингом ошибок оборудования (404/409; 400 уже покрыт).
- Добавить автоматизированные тесты: домен, application (in-memory), мапперы, JPA-адаптер/репозиторий (Testcontainers/PostgreSQL), контроллеры (MockMvc), чистота домена, границы модулей.

## Capabilities

### New Capabilities

- `technology-equipment-domain-model`: доменная модель оборудования — агрегат `Equipment`, типизированный `EquipmentId`, валидация `number`/`name`/`type`, неизменяемость `number`, обновление `name`/`type`, мягкое удаление, порт `EquipmentRepository`, расширение `Operation` ссылкой `equipmentId` и доменная чистота.
- `technology-equipment`: поведение справочника оборудования и привязки к операциям — создание, чтение, список, обновление, мягкое удаление, уникальность номера в организации, tenant-изоляция, сортировка, удаление оборудования со ссылками (висячая ссылка) и постоянное хранение в PostgreSQL.
- `technology-equipment-rest`: tenant-scoped REST-контракт `/api/v1/equipment` (CRUD) и аддитивное расширение контракта операций технологического процесса полем `equipmentId`.

### Modified Capabilities

- `technology-domain-model`: требование «Операция как value object» расширяется опциональным `equipmentId`.
- `technology`: требование «Добавление операции» расширяется опциональным `equipmentId` и проверкой существования оборудования; добавляется отражение `equipmentId` в представлении операции.
- `technology-rest`: контракт операции процесса расширяется опциональным `equipmentId` в теле запроса и ответе; ошибки процессов дополняются 404 для отсутствующего/удалённого/чужого оборудования при добавлении операции.

## Impact

- Новые/изменяемые пакеты: `technology.api`, `technology.domain.equipment`, `technology.application`, `technology.infrastructure.persistence`, `web`, корневой `WebConfig` и `RestExceptionHandler`.
- Новая миграция `V7__technology_equipment.sql` (следующая после `V6__directories.sql`); `ApplicationModules.verify()` должен проходить — модуль `technology` не получает новых межмодульных зависимостей (`catalog`, `directories`, `shared`), а `web` уже зависит от `technology`.
- Существующий REST-контракт `/api/v1/technology-processes` обратно совместим: добавляется только опциональное поле `equipmentId`, пути и статусы не меняются.
- Тесты: доменные юнит-тесты `Equipment`/`Operation`, тесты application-сервиса (in-memory), тесты мапперов, JPA-интеграционные тесты (Testcontainers), тесты контроллеров (MockMvc standalone), расширенный тест доменной чистоты `TechnologyDomainPurityTest` и существующий `ModuleVerificationTest`.
- Зависимости: новых внешних библиотек и новых межмодульных зависимостей не добавляется.

## Non-Goals

- Нормы времени на операции, материалы операций, производительность оборудования — вне объёма (future changes).
- Пагинация и полнотекстовый поиск списка оборудования — вне объёма; контракт MVP — полный список активных записей организации.
- Несколько единиц оборудования на одну операцию — вне объёма; операция ссылается не более чем на одно оборудование.
- Референсные проверки и очистка ссылок при удалении оборудования — вне объёма; операции сохраняют висячую ссылку `equipmentId`.
- Отдельный эндпоинт изменения `equipmentId` существующей операции — вне объёма; смена оборудования выполняется удалением и повторным добавлением операции.
- Фронтенд (React) для оборудования и операций — вне объёма.
