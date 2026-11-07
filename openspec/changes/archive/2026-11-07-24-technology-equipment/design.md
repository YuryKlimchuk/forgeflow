# Design

## Context

Модуль `technology` полностью реализован: агрегат `TechnologyProcess` (id/number/name/partId/organizationId/defaultProcess + аудит + soft-delete), value object `Operation(int number, String name)`, доменный сервис `TechnologyProcessService`, порт `TechnologyProcessRepository`, application-слой `TechnologyApplicationService` + порт `TechnologyService`, JPA-персистентность (таблицы `technology_process`, `technology_process_operation`, Flyway `V5__technology.sql`), REST `/api/v1/technology-processes` (9 эндпоинтов). Оборудования в системе нет.

Ближайшие эталоны:
- `directories` (change 23): `Material`/`Sortament` — CRUD-агрегаты с `number`/`name`/`type`-подобными полями, уникальностью номера в организации, soft-delete, частичным уникальным индексом и точечной трансляцией `ConstraintViolationException` → доменное исключение → 409.
- `technology` (change 21/22): ручная замена набора операций при сохранении, `@IdClass`, внешние ссылки (`part_id`) без FK и без референсной проверки при удалении, проверка существования ссылки в application-слое.

Последняя миграция — `V6__directories.sql`; новая — `V7__technology_equipment.sql`. Требования R1–R26 и допущения A1–A11 зафиксированы в `/tmp/forgeflow-24-equipment-requirements.md`.

## Goals / Non-Goals

**Goals:**
- Богатый доменный агрегат `Equipment` с чистой доменной моделью (без Spring/JPA/PostgreSQL/Flyway).
- Полный backend-срез: api → application → JPA-адаптер → PostgreSQL + REST-контроллер.
- Опциональная привязка одной единицы оборудования к операции с обратной совместимостью существующего контракта операций.
- Multi-tenant изоляция через `OrganizationContext` и `organization_id` во всех запросах.
- Мягкое удаление, переиспользование номера после удаления и гарантия уникальности `number` на двух уровнях (application + частичный индекс БД).

**Non-Goals (design-level):**
- Нормы времени, материалы операций, производительность — не моделируются и не мигрируют.
- Пагинация/поиск списка оборудования; несколько единиц оборудования на операцию.
- FK и референсные проверки для `equipment_id` в операциях; очистка ссылок при удалении оборудования.
- Фронтенд (React).

## Decisions

### D1. Оборудование — агрегат в подпакете `technology.domain.equipment`
`Equipment` размещается в `technology/domain/equipment` (вместе с `EquipmentRepository`, `EquipmentNotFoundException`, `DuplicateEquipmentNumberException`), а не плоско в `technology.domain`. Обоснование: группировка типов нового агрегата по образцу `directories/domain/material`; допущение A10 прямо допускает этот вариант. Альтернатива (плоско в `technology.domain`) отвергнута: с ростом числа агрегатов плоский пакет теряет навигационную связность. Тест доменной чистоты `TechnologyDomainPurityTest` расширяется списком типов из нового подпакета.

### D2. `EquipmentId` — в `technology.api`, домен ссылается на него
`EquipmentId` — `record(UUID value)` с `generate()`/`of(UUID)` и отказом от `null`, размещается в `technology.api` (публичный контракт, используется `web`). Домен (`Equipment`, `Operation`) ссылается на `EquipmentId` из `api` — та же конвенция, что `PartId`/`TechnologyProcessId` (R23).

### D3. Фабрика `create` без `restore`
`Equipment` строится приватным конструктором + статической фабрикой `create(id, number, name, type, organizationId, createdAt, updatedAt)`, мутаторами `update(name, type)` и `markDeleted()`. Отдельная `restore(...)` не нужна: JPA-запросы фильтруют `deleted_at IS NULL`, удалённые агрегаты не загружаются в домен, а `deletedAt` пишется только при сохранении entity (паттерн `Material`, D3 change 23).

### D4. `type` — обязательная непустая строка, без enum
`type` — `String`, обязательная и непустая (`StringUtils.isBlank` → `IllegalArgumentException`), без enum/классификатора (A3). Свободная строка не навязывает невыдуманную таксономию; если `type` окажется не нужен — точечное упрощение без смены архитектуры.

### D5. Порт `EquipmentRepository` и Spring Data отделены
Доменный порт `technology.domain.equipment.EquipmentRepository` — свободный от аннотаций интерфейс: `save`, `findById(id, organizationId)`, `findAll(organizationId)`, `existsByNumber(number, organizationId)`; без физического `delete`. Spring Data `EquipmentJpaRepository` (`infrastructure.persistence`) — `JpaRepository<EquipmentEntity, UUID>` с derived-методами `findByOrganizationIdAndIdAndDeletedAtIsNull`, `findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc`, `existsByOrganizationIdAndNumberAndDeletedAtIsNull`.

### D6. `EquipmentApplicationService` — зеркало `MaterialApplicationService`
Application-сервис: tenant из `OrganizationContext.requiredOrganizationId()`, предпроверка `existsByNumber` → `DuplicateEquipmentNumberException`, `Clock` для временных меток, `@Transactional`. CRUD повторяет `MaterialApplicationService` (create/get/list/update/delete), но `update` меняет только `name`/`type`.

### D7. Уникальность номера: проверка + частичный индекс + точечная трансляция
`create` сначала вызывает `repository.existsByNumber(...)` (→ `DuplicateEquipmentNumberException`), затем создаёт и сохраняет агрегат. Финальная защита от гонки — частичный уникальный индекс `equipment_number_org_uq` на `(organization_id, number) WHERE deleted_at IS NULL`. `JpaEquipmentRepository.save` выполняет `repository.save(entity)` + `flush()` и перехватывает `DataIntegrityViolationException`; если в цепочке причин есть `ConstraintViolationException` с именем ограничения `equipment_number_org_uq`, транслирует в `DuplicateEquipmentNumberException`, иначе пробрасывает исходное исключение (прочие ограничения не маскируются). Паттерн 1:1 `JpaMaterialRepository` (R18).

### D8. Flyway `V7__technology_equipment.sql`
Миграция (R16): (а) `CREATE TABLE equipment` (`id UUID PK`, `number/name/type VARCHAR(255) NOT NULL`, `organization_id UUID NOT NULL`, `created_at/updated_at TIMESTAMPTZ NOT NULL`, `deleted_at TIMESTAMPTZ`) + `CREATE UNIQUE INDEX equipment_number_org_uq ON equipment (organization_id, number) WHERE deleted_at IS NULL`; (б) `ALTER TABLE technology_process_operation ADD COLUMN equipment_id UUID` (nullable, **без** FK). `ddl-auto` остаётся `none`.

### D9. JPA-сущности и маппинг
`EquipmentEntity` — `@Entity @Table(name = "equipment")`, публичные поля в стиле `TechnologyProcessEntity`. `TechnologyProcessOperationEntity` получает `@Column(name = "equipment_id") public UUID equipmentId;`. Инфраструктурный `TechnologyMapper.toOperationEntities` пишет `equipmentId` (nullable), `toDomain` восстанавливает `new Operation(..., equipmentId == null ? null : EquipmentId.of(equipmentId))`. Для `Equipment` — отдельный инфраструктурный `EquipmentMapper` (`entity ↔ domain` через `create`, без `deletedAt`) и application-`EquipmentMapper` (`domain → EquipmentView`).

### D10. Расширение `Operation` без ломки существующих вызовов
`Operation` становится `record Operation(int number, String name, EquipmentId equipmentId)` с компактным конструктором (валидация `number > 0`, `name` не blank; `equipmentId` допустим `null`) и дополнительным конструктором `Operation(int number, String name)` → `this(number, name, null)`. `TechnologyProcess.addOperation(int number, String name, EquipmentId equipmentId)` — основной метод; существующий `addOperation(int number, String name)` делегирует с `null`. Это сохраняет существующие доменные вызовы и тесты исходно-совместимыми, а новое поведение аддитивно (R11, A11).

### D11. Проверка оборудования при добавлении операции — в application-слое
`TechnologyApplicationService.addOperation` после загрузки процесса (404) проверяет непустой `request.equipmentId()` через внедрённый `EquipmentRepository.findById(equipmentId, organizationId).orElseThrow(EquipmentNotFoundException)` (404), затем вызывает `process.addOperation(number, name, equipmentId)` (400/409 от домена). Порядок ошибок соответствует A8: процесс 404 → оборудование 404 → валидация 400 → дубликат операции 409. `TechnologyApplicationService` получает зависимость `EquipmentRepository` (тот же модуль, без новой межмодульной зависимости).

### D12. REST и application: `EquipmentController` + аддитивное расширение операций
`EquipmentController` — тонкий `@RestController` в `web`, `@RequestMapping("/api/v1/equipment")`, регистрируется component scan (как `MaterialController`/`TechnologyProcessController`), без `@Bean`. Эндпоинты: `POST` 201, `GET` 200, `GET /{id}` 200, `PUT /{id}` 204, `DELETE /{id}` 204.

Аддитивное расширение операций:
- `AddOperationRequest` → `record(int number, String name, EquipmentId equipmentId)` с дополнительным 2-аргументным конструктором `AddOperationRequest(int number, String name) { this(number, name, null); }` — существующие вызовы `new AddOperationRequest(number, name)` (в т.ч. в `TechnologyApplicationServiceTest`) остаются исходно-совместимыми (конвенция `Operation`/`OperationView`).
- `OperationView` → `record(int number, String name, EquipmentId equipmentId)` с дополнительным 2-аргументным конструктором `OperationView(int number, String name) { this(number, name, null); }`.
- `TechnologyMapper.toView` маппит `operation.equipmentId()` в `OperationView.equipmentId` (nullable), а `TechnologyApplicationService.addOperation` возвращает `OperationView` с `equipmentId` из `request.equipmentId()` — ссылка на оборудование не теряется на `get`/`list`/`addOperation` (R13/R21).

JSON-представление `equipmentId` — `{"value": UUID}` или `null`, консистентно с `partId`/`TechnologyProcessId`.

### D13. Обработка ошибок в `RestExceptionHandler`
Добавляются `EquipmentNotFoundException` → 404 `not_found` и `DuplicateEquipmentNumberException` → 409 `duplicate`. `IllegalArgumentException` уже маппится → 400 `bad_request` (R22). Существующие маппинги не меняются. `EquipmentNotFoundException` покрывает и случай «оборудование в операции не найдено» → 404.

### D14. Composition root `WebConfig`
Добавляются `@Bean`: `equipmentRepository(EquipmentJpaRepository)`, application-`EquipmentMapper`, `EquipmentApplicationService(EquipmentRepository, OrganizationContext, EquipmentMapper, Clock)`. `TechnologyApplicationService` получает дополнительный параметр `EquipmentRepository`. `EquipmentController` не регистрируется как `@Bean` (component scan). Внутри модуля `technology` Spring-стереотипы не появляются.

### D15. Тесты по пирамиде
- Домен: `EquipmentTest` (валидация, `update`, `markDeleted` идемпотентность, неизменяемость `number`, структура), `EquipmentIdTest`, `OperationTest` (nullable `equipmentId`), `EquipmentRepositoryPortTest`, расширенный `TechnologyDomainPurityTest` (новые типы).
- Application: `EquipmentApplicationServiceTest` на `InMemoryEquipmentRepository` (CRUD, уникальность, изоляция, сортировка, повторное использование номера, отсутствие контекста); расширение `TechnologyApplicationServiceTest` сценариями `equipmentId` (валидное оборудование, `null`, отсутствующее/чужое → 404, порядок ошибок).
- Мапперы: application-`EquipmentMapperTest`, инфраструктурный `EquipmentMapperTest`, расширенный `TechnologyMapperTest` (round-trip nullable `equipment_id`; `toView` маппит `equipmentId` в `OperationView`).
- JPA: `JpaEquipmentRepositoryTest` (`AbstractPostgresIntegrationTest`): round-trip, tenant-изоляция, soft-delete, существование частичного индекса `equipment_number_org_uq`, дубликат на уровне БД, повторное использование номера после удаления, трансляция дубликата; `JpaEquipmentRepositoryAdapterTest` (трансляция конкретного ограничения, не-маскирование чужого).
- Контроллеры: `EquipmentControllerTest` (MockMvc standalone: 201/200/204, 404/409/400); расширение `TechnologyProcessControllerTest` сценариями `equipmentId`.
- Full-context tenant: `EquipmentRestTenantContextTest` (по образцу `DirectoriesRestTenantContextTest`): `webAppContextSetup(context)` + `springSecurity()` + `addFilters(tenantContextFilter)` + `demoUserJwt()`; проверка tenant-изоляции оборудования (чужое оборудование → 404) и игнорирования клиентского заголовка `X-Organization-Id` на реальном `TenantContextFilter`/security.
- Архитектура: существующие `ModuleVerificationTest` (без изменений — границы не меняются) и контекстный тест `TechnologyEquipmentContextTest` (composition root собирает `EquipmentService` и `EquipmentController` однократно).

### D16. Seed оборудования
Seed оборудования не добавляется (A9): приёмка не требует демо-данных, оборудование создаётся через API. При желании демо-секцию можно добавить идемпотентно позже без влияния на контракты.

## Risks / Trade-offs

- [Гонка уникальности номера] → application-проверка `existsByNumber` не атомарна; защита — частичный уникальный индекс + трансляция `ConstraintViolationException` → 409. Покрывается тестом трансляции.
- [Висячая ссылка `equipment_id`] → операции сохраняют ссылку на удалённое оборудование; консистентно с `technology_process.part_id` (без FK). Приемлемо и явно зафиксировано в R14/A6; вводить референсное правило — значит выдумывать бизнес-логику.
- [Точное строковое совпадение номера] → уникальность без trim/case-normalization (как в `catalog`/`directories`); номера «5» и « 5 » считаются разными. Принятое поведение проекта.
- [Смена сигнатуры `Operation`/`OperationView`/`AddOperationRequest`] → аддитивное расширение, но конструкторы существующих тестов/вызовов с двумя аргументами должны сохраниться через дополнительные конструкторы; это минимизирует диф и сохраняет обратную совместимость контракта.
- [`type` как свободная строка] → возможна типографская вариативность значений; невалидное значение может попасть только прямым SQL. Приемлемо: `type` обязателен и непуст на уровне домена, enum/классификатор осознанно не вводится (A3).
- [Порядок ошибок добавления операции] → проверка оборудования выполняется до мутации агрегата, поэтому при ошибке «не найдено» операция не добавляется и процесс не сохраняется (A8, R12). Покрывается тестом.

## Migration Plan

1. Добавить `V7__technology_equipment.sql` (только `CREATE TABLE equipment` + `CREATE UNIQUE INDEX` + `ALTER TABLE technology_process_operation ADD COLUMN equipment_id UUID`; существующие строки операций получают `NULL`).
2. Реализовать домен `technology.domain.equipment`, расширить `Operation` и `TechnologyProcess.addOperation`.
3. Реализовать `technology.api` (EquipmentId, DTO, EquipmentService), application-сервисы и мапперы.
4. Реализовать JPA-персистентность и обновить `TechnologyProcessOperationEntity`/`TechnologyMapper`.
5. Реализовать `EquipmentController`, расширить `TechnologyProcessController` и `WebConfig`/`RestExceptionHandler`.
6. Дополнить тесты и прогнать `mvn test` (включая `ModuleVerificationTest`, расширенный `TechnologyDomainPurityTest`, `EquipmentRestTenantContextTest` и Testcontainers-интеграцию).
Rollback: миграция `V7` аддитивна; откат — `ALTER TABLE technology_process_operation DROP COLUMN equipment_id` и `DROP TABLE equipment`, возврат кода. Существующие данные не затрагиваются (колонка nullable).

## Open Questions

Нет: допущения A1–A11 закрывают неопределённости; блокирующих вопросов для проектирования не осталось.
