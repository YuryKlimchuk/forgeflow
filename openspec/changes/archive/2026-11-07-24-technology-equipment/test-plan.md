# Test plan — change 24-technology-equipment

> Этап: QA Planning (G4). Оценка тестируемости пакета «дизайн + спецификации + задачи»
> на предмет готовности к реализации. Настоящий план — вход для этапа G7 (фактическая
> проверка по Test plan). Реализация не выполняется на этом этапе.

## 1. Scope & goals

**Что тестируется**

1. Справочник оборудования `Equipment` в модуле `technology`:
   - доменный агрегат (поля, валидация, неизменяемость `number`, обновление `name`/`type`,
     soft-delete, идемпотентность удаления);
   - application-сервис `EquipmentApplicationService` (tenant-изоляция, уникальность номера,
     сортировка, повторное использование номера после удаления, отсутствие контекста);
   - JPA-персистентность (round-trip, частичный уникальный индекс, трансляция дубликата,
     tenant-фильтрация на уровне SQL-запросов);
   - REST `/api/v1/equipment` (статусы, тела, коды ошибок, tenant-контекст).

2. Привязка оборудования к операциям:
   - value object `Operation` с опциональным `equipmentId` (обратная совместимость);
   - `TechnologyApplicationService.addOperation` (валидное оборудование, `null`,
     отсутствующее/удалённое/чужое оборудование, порядок ошибок, «процесс не изменяется»);
   - отражение `equipmentId` в представлении операции (`get`/`list`/`addOperation`);
   - висячая ссылка после удаления оборудования (операция сохраняет `equipmentId`).

3. Обратная совместимость существующего REST-контракта `/api/v1/technology-processes`:
   существующие пути, статусы и тела запросов/ответов не ломаются, добавлено только
   опциональное поле `equipmentId`.

**Цели проверки**

- Подтвердить, что требования R1–R26 и AC покрыты исполняемыми сценариями.
- Выявить негативные и граничные сценарии, которые дизайн мог не покрыть явно.
- Зафиксировать окружение, данные и критерии выхода (G7) для независимой приёмки.

## 2. Test levels

| Уровень | Что проверяет | Инструменты |
|---|---|---|
| **Unit (домен)** | `Equipment`, `EquipmentId`, `Operation`, `TechnologyProcess.addOperation`, сигнатуры порта `EquipmentRepository`, доменная чистота | JUnit 5, AssertJ, reflection |
| **Slice (application)** | `EquipmentApplicationService`, `TechnologyApplicationService.addOperation` на in-memory репозиториях | JUnit 5, in-memory репозитории, fake `OrganizationContext` |
| **Mapper** | `EquipmentMapper` (application и infra), `TechnologyMapper` (application и infra), round-trip полей | JUnit 5, AssertJ |
| **Integration (persistence)** | JPA-адаптеры/репозитории, Flyway-миграция `V7`, частичный индекс, трансляция дубликата, round-trip | Testcontainers/PostgreSQL 17, `AbstractPostgresIntegrationTest`, `JdbcTemplate` |
| **REST (controller)** | `EquipmentController`, расширение `TechnologyProcessController` — статусы/коды ошибок/тела | MockMvc standalone + `RestExceptionHandler` |
| **REST (full-context)** | tenant-контекст через `TenantContextFilter`, игнорирование `X-Organization-Id`, реальная безопасность | MockMvc `webAppContextSetup` + `springSecurity` |
| **Architecture** | границы модулей, ацикличность, однократная регистрация контроллера | `ModuleVerificationTest`, purity-тест, context-тест |

## 3. Test scenarios

Тип сценария: **P** — позитивный, **N** — негативный, **B** — граничный.
Уровень в скобках после имени.

### 3.1 Домен оборудования (unit)

- **S1** (P, unit) `Equipment.create` с валидными `number`/`name`/`type` и `organizationId`/
  `createdAt`/`updatedAt` сохраняет все значения; `deletedAt == null`. → AC-1.
- **S2** (N, unit) `Equipment.create` с пустым/пробельным `number`, `name` или `type`
  бросает `IllegalArgumentException` с указанием поля; то же для `null`. → AC-2, R2.
- **S3** (B, unit) `Equipment.update` с пустым/пробельным `name` или `type` бросает
  `IllegalArgumentException` и не изменяет состояние агрегата. → AC-6, R7.
- **S4** (P, unit) `Equipment.update` с непустыми `name`/`type` изменяет их, обновляет
  `updatedAt` и оставляет `number` прежним. → AC-2, AC-6.
- **S5** (B, unit) отсутствие публичного мутатора `number` (reflection: нет `setNumber`/
  `changeNumber`, поле `number` final; `update` не принимает `number`). → AC-2, R1/R7.
- **S6** (P, unit) `markDeleted()` на активном агрегате устанавливает ненулевой `deletedAt`
  и обновляет `updatedAt`. → AC-7.
- **S7** (B, unit) повторный `markDeleted()` идемпотентен (`deletedAt`/`updatedAt` не меняются).
  → R8, D3.
- **S8** (B, unit) `EquipmentId.generate()` даёт непустой UUID; `EquipmentId.of(uuid)` сохраняет
  UUID; `EquipmentId.of(null)` → `NullPointerException`. → AC-1/R (типизированный id).
- **S9** (B, unit) аудит `Equipment` не содержит `createdBy`/`updatedBy` (reflection). → R1.
- **S10** (B, unit) порт `EquipmentRepository` — интерфейс без аннотаций, имеет `save`,
  `findById(id, organizationId)`, `findAll(organizationId)`, `existsByNumber(number,
  organizationId)` и не имеет физического `delete`. → R23, AC-17.

### 3.2 Расширение `Operation` и `TechnologyProcess` (unit)

- **S11** (P, unit) `new Operation(number, name, equipmentId)` с непустым `EquipmentId`
  сохраняет ссылку. → AC-11, R10.
- **S12** (P, unit) `new Operation(number, name)` (2-арг. конструктор) даёт
  `equipmentId == null` (обратная совместимость). → AC-11, R11.
- **S13** (N, unit) `Operation` с `number <= 0` или blank/null `name` бросает
  `IllegalArgumentException` (существующая валидация сохраняется). → AC-11, R10.
- **S14** (P, unit) `TechnologyProcess.addOperation(number, name, equipmentId)` добавляет
  операцию со ссылкой; `addOperation(number, name)` делегирует с `null`. → AC-11, D10.
- **S15** (P, unit) `TechnologyProcess.restore(...)` восстанавливает операции с их
  `equipmentId` и упорядочивает по номеру. → AC-13, D9.
- **S16** (N, unit) `addOperation` с дубликатом номера → `DuplicateOperationException`
  (существующее поведение сохраняется). → AC-18, R.

### 3.3 Application-сервис оборудования (slice, in-memory)

- **S17** (P, slice) `create` возвращает представление с системными полями; запись видна в
  `get`/`list`. → AC-1, R4.
- **S18** (N, slice) `create` с дубликатом номера в той же организации →
  `DuplicateEquipmentNumberException`, новая запись не создаётся. → AC-3, R3.
- **S19** (P, slice) одинаковый `number` в разных организациях создаётся успешно; каждая
  организация видит только свою запись. → AC-3, R3.
- **S20** (N, slice) `create`/`get`/`list`/`update`/`delete` без `OrganizationContext` →
  `OrganizationContextNotAvailableException`. → R9.
- **S21** (N, slice) `get` отсутствующего/удалённого/чужого оборудования →
  `EquipmentNotFoundException`. → AC-5, R5/R9.
- **S22** (P, slice) `list` возвращает активное оборудование организации, отсортированное по
  `number` по возрастанию; удалённое и чужое исключаются. → AC-4, R6.
- **S23** (P, slice) `update` меняет `name`/`type`, сохраняет `number`, обновляет `updatedAt`;
  `updatedAt >= createdAt` (см. риск о `Instant.now()`). → AC-6, R7.
- **S24** (N, slice) `update` с пустым `name`/`type` → `IllegalArgumentException`, состояние
  сохраняется. → AC-6, R7.
- **S25** (P, slice) `delete` выполняет soft-delete: `get` → `EquipmentNotFoundException`,
  `list` не содержит запись, `existsByNumber` возвращает `false`. → AC-7, R8.
- **S26** (P, slice) после soft-delete тот же `number` можно создать заново (новый id).
  → AC-9, R8/R17.
- **S27** (B, slice) `number` сохраняется/сравнивается «как есть» (без trim/case-normalization):
  «5» и « 5 » — разные номера (принятое поведение, как в catalog/directories). → R3, риск дизайна.

### 3.4 Привязка оборудования к операции (slice, application)

- **S28** (P, slice) `addOperation` с `equipmentId` активного оборудования текущей организации
  добавляет операцию, и представление содержит этот `equipmentId`. → AC-11, AC-13, R11/R12.
- **S29** (P, slice) `addOperation` с `equipmentId == null` добавляет операцию без оборудования
  (обратная совместимость). → AC-11, R11.
- **S30** (N, slice) `addOperation` со ссылкой на отсутствующее/удалённое/чужое оборудование →
  `EquipmentNotFoundException`, операция не добавляется, процесс не изменяется (операции до/после
  одинаковы, `updatedAt` не меняется). → AC-12, R12.
- **S31** (N, slice) порядок ошибок A8: отсутствующий процесс → 404 раньше проверки оборудования;
  при невалидном оборудовании проверка выполняется до валидации `number`/`name` и до проверки
  дубликата номера операции. → A8, R12.
- **S32** (P, slice) чтение процесса (`get`/`list`) возвращает операции в порядке возрастания
  номера с их `equipmentId` (включая `null`). → AC-13, R13.
- **S33** (P, slice) удаление оборудования, на которое ссылаются операции, выполняется успешно;
  последующее чтение процесса сохраняет прежний `equipmentId` (висячая ссылка). → AC-14, R14.
- **S34** (B, slice) отдельного эндпоинта изменения `equipmentId` операции нет; смена оборудования
  достигается `removeOperation` + `addOperation` (проверка отсутствия такого метода в
  `TechnologyService`/контроллере). → AC-15, R15.

### 3.5 Мапперы (unit)

- **S35** (P, mapper) application `EquipmentMapper.toView` отображает все поля, кроме
  `organizationId`/`deletedAt`. → AC-1, R20.
- **S36** (P, mapper) infra `EquipmentMapper`: `entity ↔ domain` round-trip всех полей; `toDomain`
  строит агрегат через `create` без `deletedAt`; `toEntity` пишет `deletedAt`. → AC-16, D9.
- **S37** (P, mapper) application `TechnologyMapper.toView` отображает `operation.equipmentId()`
  в `OperationView.equipmentId` (non-null и null). → AC-13, R13. **См. F2.**
- **S38** (P, mapper) infra `TechnologyMapper.toOperationEntities`/`toDomain` round-trip
  `equipment_id` (null и non-null) без потерь. → AC-16, D9.

### 3.6 JPA-персистентность и миграция (integration, PostgreSQL)

- **S39** (P, integration) round-trip `Equipment`: `save` → `findById` возвращает все поля
  (`id/number/name/type/organizationId/createdAt/updatedAt`, `deletedAt == null`). → AC-16, R19.
- **S40** (P, integration) tenant-изоляция: запись организации A не читается через `findById`/
  `findAll` организации B. → AC-4/AC-5, R9.
- **S41** (P, integration) soft-delete исключает запись из `findById`/`findAll`/`existsByNumber`.
  → AC-7, R8.
- **S42** (P, integration) частичный уникальный индекс `equipment_number_org_uq` существует
  (pg_indexes содержит `organization_id`, `number`, `deleted_at IS NULL`). → AC-8, R16/R17.
- **S43** (N, integration) прямая вставка активного оборудования с занятым `(organization_id,
  number)` отклоняется ограничением БД. → AC-9, R17.
- **S44** (P, integration) после soft-delete номер освобождается и повторно используется. → AC-9,
  R17.
- **S45** (N, integration) нарушение `equipment_number_org_uq` при `save` транслируется в
  `DuplicateEquipmentNumberException`. → AC-10, R18.
- **S46** (N, integration) нарушение чужого ограничения (не `equipment_number_org_uq`) не
  маскируется (пробрасывается исходное исключение). → AC-10, R18.
- **S47** (P, integration) `findAll` сортирует по `number` по возрастанию. → AC-4, R6.
- **S48** (P, integration) round-trip `equipment_id` в `technology_process_operation`:
  операция с `equipmentId` и операция без него восстанавливаются без потерь и в порядке номера.
  → AC-16, R19.
- **S49** (B, integration) миграция `V7` применима на чистой БД (Testcontainers): таблица
  `equipment` и nullable колонка `equipment_id` (без FK) созданы. → AC-8, R16.
- **S50** (B, integration) существующие операции (без `equipment_id`, `NULL`) корректно читаются
  после миграции (аддитивность nullable-колонки). → AC-18, R16/R19.

### 3.7 REST оборудование (MockMvc standalone + full-context)

- **S51** (P, rest) `POST /api/v1/equipment` с валидным телом → 201 и представление. → AC-1, R20.
- **S52** (P, rest) `GET /api/v1/equipment` → 200, массив, отсортирован по `number`. → AC-4, R20.
- **S53** (P, rest) `GET /api/v1/equipment/{id}` → 200 для активного своего. → AC-5, R20.
- **S54** (P, rest) `PUT /api/v1/equipment/{id}` с `{name, type}` → 204; `number` не изменяется
  (даже если клиент передаёт `number` в теле — игнорируется). → AC-6, R20/R22.
- **S55** (P, rest) `DELETE /api/v1/equipment/{id}` → 204; последующее чтение → 404. → AC-7, R20.
- **S56** (N, rest) `GET/PUT/DELETE` для отсутствующего/удалённого/чужого оборудования → 404
  `not_found`. → AC-5/AC-6/AC-7, R22. **См. F3.**
- **S57** (N, rest) `POST` с дубликатом номера → 409 `duplicate`. → AC-3, R22.
- **S58** (N, rest) `POST`/`PUT` с пустым `number`/`name`/`type` → 400 `bad_request`. → AC-2, R22.
- **S59** (B, rest) JSON-представление `equipmentId` — `{"value": "<uuid>"}`; `null` для операции
  без оборудования. → R21, D12.

### 3.8 REST привязка к операции (MockMvc standalone)

- **S60** (P, rest) `POST /api/v1/technology-processes/{id}/operations` с телом
  `{number, name, equipmentId: {"value": "<uuid>"}}` → 201, ответ содержит `equipmentId`. → AC-11,
  R21.
- **S61** (P, rest) `POST .../operations` с телом `{number, name}` (без `equipmentId`) → 201,
  `equipmentId == null`. → AC-11, R21.
- **S62** (N, rest) `POST .../operations` со ссылкой на отсутствующее оборудование → 404
  `not_found`, операция не добавляется. → AC-12, R22.
- **S63** (N, rest) существующие эндпоинты `/api/v1/technology-processes` (create/get/list/rename/
  delete/removeOperation/default) сохраняют прежние статусы и пути (регресс). → AC-18, R21.

### 3.9 Tenant-контекст REST (full-context integration)

- **S64** (N, full-context) чужое оборудование через `GET /api/v1/equipment/{id}` → 404; данные
  организации B не раскрываются. → R9, AC-5.
- **S65** (N, full-context) клиентский заголовок `X-Organization-Id` организации B игнорируется:
  операция выполняется в контексте организации A (аутентифицированный `demo-user`). → R, rest-spec
  «Игнорирование клиентского tenant». **См. F5.**
- **S66** (B, full-context) после запроса `OrganizationContext` очищен
  (`currentOrganizationId().isEmpty()`). → R9, tenant-гигиена.

### 3.10 Архитектура и границы модулей (architecture)

- **S67** (P, architecture) `ModuleVerificationTest` проходит: модули ацикличны, у `technology`
  разрешены зависимости только `catalog`/`directories`/`shared`, `web` зависит от `technology`. → AC-17.
- **S68** (P, architecture) `TechnologyDomainPurityTest` (расширенный) подтверждает отсутствие
  Spring/JPA/PostgreSQL/Flyway в `technology.domain.equipment` и что `Operation` ссылается на
  `technology.api.EquipmentId`. → AC-17, R23.
- **S69** (B, architecture) `TechnologyEquipmentContextTest` собирает `EquipmentService` и
  `EquipmentController` **однократно** (защита от двойной регистрации через `@Bean` + component
  scan). → R20, риск двойной регистрации.

## 4. Acceptance mapping

| Сценарий | AC | Сценарий | AC |
|---|---|---|---|
| S1–S9 | AC-1, AC-2, AC-6, AC-7 | S39–S50 | AC-4, AC-5, AC-7, AC-8, AC-9, AC-10, AC-16, AC-18 |
| S10 | AC-17 | S51–S59 | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7 |
| S11–S16 | AC-11, AC-13, AC-18 | S60–S63 | AC-11, AC-12, AC-18 |
| S17–S27 | AC-1, AC-3, AC-4, AC-5, AC-6, AC-7, AC-9 | S64–S66 | AC-4, AC-5, R9 |
| S28–S34 | AC-11, AC-12, AC-13, AC-14, AC-15 | S67–S69 | AC-17, AC-19 |
| S35–S38 | AC-1, AC-13, AC-16 | | |

Полнота трассировки: каждый AC из списка (1–20) покрыт хотя бы одним сценарием:
- AC-1 → S1, S17, S35, S51; AC-2 → S2, S4, S5, S58; AC-3 → S18, S19, S57;
- AC-4 → S22, S40, S47, S52; AC-5 → S21, S40, S53, S56, S64; AC-6 → S3, S4, S23, S24, S54, S56;
- AC-7 → S6, S25, S41, S55; AC-8 → S42, S49; AC-9 → S26, S43, S44;
- AC-10 → S45, S46; AC-11 → S11, S12, S13, S14, S28, S29, S60, S61;
- AC-12 → S30, S62; AC-13 → S15, S32, S37; AC-14 → S33; AC-15 → S34;
- AC-16 → S36, S38, S39, S48; AC-17 → S10, S67, S68; AC-18 → S16, S50, S63;
- AC-19 → S67–S69 и полный прогон `mvn test` (entry/exit); AC-20 → проверка отсутствия
  нецелевых артефактов (см. Exit criteria).

## 5. Environment & data

- **Окружение**: Java 25, Maven, Spring Boot 4.1.x, PostgreSQL 17 (Testcontainers singleton —
  `AbstractPostgresIntegrationTest`), Flyway `V1…V7`.
- **Сборка**: `mvn test` (включая `ModuleVerificationTest`, purity-тест, Testcontainers-интеграцию).
- **Данные**:
  - домен/application-тесты — in-memory репозитории (`InMemoryEquipmentRepository`,
    `InMemoryTechnologyProcessRepository`), fake `OrganizationContext`, `Clock.fixed(...)`.
  - integration-тесты — демо-организация `00000000-0000-0000-0000-000000000001` и
    `demo-user` (создаются в `AbstractPostgresIntegrationTest`); прочие организации — `generate()`.
  - REST full-context — JWT `demoUserJwt()`, `TenantContextFilter`, `WebApplicationContext`.
- **Зависимости**: новых внешних библиотек не требуется; используются существующие Testcontainers,
  Spring Security Test, AssertJ, Mockito.

## 6. Risks & assumptions

**Риски (из дизайна и оценки):**

1. **Гонка уникальности номера** — предпроверка `existsByNumber` не атомарна; защита частичным
   уникальным индексом + точечной трансляцией `ConstraintViolationException` → 409. Покрывается
   S45/S46 и S43/S44.
2. **Висячая ссылка `equipment_id`** — операции сохраняют ссылку на удалённое оборудование
   (консистентно с `part_id` без FK). Принятое поведение R14/A6; покрывается S33.
3. **Расширение `Operation`/`OperationView`/`AddOperationRequest`** — аддитивное, но сохранение
   исходной совместимости конструкторов критично для компиляции существующих тестов. **См. F1/F2.**
4. **`ALTER TABLE technology_process_operation ADD COLUMN equipment_id UUID`** — аддитивная
   nullable-колонка; существующие строки получают `NULL`. Риск низкий; покрывается S49/S50.
   Отдельного теста «миграция на уже заполненной таблице» нет (Testcontainers применяет V5→V6→V7
   на пустой БД), но nullable-колонка не требует backfill.
5. **Двойная регистрация контроллера** — в кодовой базе два паттерна (`@Bean` для catalog,
   component scan для directories/technology). Дизайн выбрал component scan + запрет `@Bean`;
   защита — `TechnologyEquipmentContextTest` с `hasSize(1)` (S69).
6. **`updatedAt` через `Instant.now()` в домене** — application `Clock` управляет только
   `createdAt`; тесты обновления должны проверять `updatedAt >= createdAt` (S23), а не точное
   фиксированное время (паттерн `Material`/`TechnologyProcess`).
7. **Точное строковое сравнение номера** — без trim/case-normalization («5» и « 5 » различны);
   принятое поведение, покрывается S27.

**Допущения (A1–A11 приняты как есть):**

- Объём = справочник + привязка (A1); размещение в `technology` (A2).
- `type` — обязательная свободная строка без enum (A3, D4).
- Одна опциональная ссылка на операцию (A4); проверка существования в application-слое без FK (A5).
- Удаление оборудования без проверки/очистки ссылок (A6); смена оборудования через
  удаление + повторное добавление (A7); порядок ошибок A8.
- Seed оборудования не обязателен (A9); обратная совместимость контракта аддитивна (A11).
- REST-путь `/api/v1/equipment` — в единственном числе (зафиксирован требованиями R20; отличен от
  plural-путей справочников, но не является дефектом).

## 7. Entry / exit criteria

**Entry (условия начала G7):**

- Пакет «дизайн + Test plan» утверждён пользователем (G4).
- Findings F1/F2/F5 закрыты (Architect внёс правки) или приняты пользователем.
- Реализация завершена (G5) и код-ревью пройдено (G6).

**Exit (G7 pass criteria):**

- `mvn test` зелёный: все unit/slice/mapper/controller/architecture-тесты проходят.
- Testcontainers-интеграция зелёная: миграция `V7` применилась, round-trip, частичный индекс,
  трансляция дубликата, tenant-изоляция проверены.
- Все сценарии S1–S69 выполнены; для каждого негативного сценария подтверждён ожидаемый статус/код
  ошибки, а не только отсутствие исключения.
- Существующие тесты `technology`/`directories` не сломаны (обратная совместимость контракта).
- `ModuleVerificationTest` и расширенный `TechnologyDomainPurityTest` зелёные (границы модулей и
  доменная чистота).
- Нецелевые артефакты отсутствуют (нормы времени/материалы/производительность/пагинация/множество
  оборудования на операцию/референсные проверки при удалении — не реализованы) → AC-20.
