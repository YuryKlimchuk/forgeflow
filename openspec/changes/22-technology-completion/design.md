# Design

## Context

См. `proposal.md` — Why. Модуль `technology` уже имеет готовый домен: агрегат `TechnologyProcess` (`@Entity`/`@Identity`, soft-delete `deletedAt`, набор операций, аудит), value object `Operation`, чистый доменный сервис `TechnologyProcessService` (`register`/`markAsDefault`/`unmarkDefault`), driven-порт `TechnologyProcessRepository` (`save`/`findById`/`findByPartId`/`findAll`/`existsByNumber`, без физического удаления) и типизированный `TechnologyProcessId` в `technology.api`. Отсутствуют application-слой, персистентность и REST.

Эталон реализации — модуль `planning` (`PlanApplicationService`, `PlanMapper`, `JpaPlanRepository`, `PlanController`, `V4__planning.sql`) и срез `catalog.part` (`PartApplicationService` с `Clock`, `PartService`, `RestExceptionHandler`). Composition root — `WebConfig`. Миграции — только Flyway; идентификатор генерирует домен.

## Goals / Non-Goals

**Goals:**

- Завершить backend модуля `technology`: application-слой, JPA-персистентность, Flyway-миграция и REST-адаптер по образцу `planning`.
- Сохранить домен чистым (без Spring/JPA/PostgreSQL/Flyway) и перенести storage-уровневый гарант уникальности номера в частичный уникальный индекс.
- Обеспечить полный round-trip персистентности без потерь полей агрегата (включая `defaultProcess` и временные метки).

**Non-Goals:**

- Интеграция со спецификациями и фронтенд — вне объёма.
- Пагинация/полнотекстовый поиск, переупорядочивание операций, оборудование/нормы/материалы операций, перенос процесса на другую деталь, валидация номера по паттерну организации.
- Изменение доменных правил и сигнатур существующих доменных типов (кроме минимальной фабрики восстановления, см. D6).

## Decisions

### D1. Размещение публичного контракта

Входной порт `TechnologyService`, DTO и представления размещаются в `technology.api` (`@NamedInterface("technology")`). Реализация `TechnologyApplicationService` и view-маппер — в `technology.application`. Инфраструктура JPA — в `technology.infrastructure.persistence`. REST-контроллер — в `web`. Зеркалит `planning` (`api`/`application`/`infrastructure`/`web`).

**Альтернатива:** разместить порт в `technology.application`. Отклонено: межмодульные обращения (`web`) должны идти только через `api`.

### D2. Именование порта и сервисов

- Входной порт — `technology.api.TechnologyService`.
- Реализация — `technology.application.TechnologyApplicationService`.
- View-маппер — `technology.application.TechnologyMapper` (домен → представление), отдельный от инфраструктурного маппера (допущение A1).

Имя порта не конфликтует с доменным `TechnologyProcessService`.

**Альтернатива:** `TechnologyProcessService` как имя порта. Отклонено: уже занято доменным сервисом (A1).

### D3. DTO-контракт (уточнение A10)

В `technology.api`:

- `CreateTechnologyProcessRequest(String number, String name, PartId partId)`.
- `AddOperationRequest(int number, String name)`.
- `TechnologyProcessView(TechnologyProcessId id, String number, String name, PartId partId, boolean defaultProcess, Instant createdAt, Instant updatedAt, List<OperationView> operations)`.
- `OperationView(int number, String name)`.

В `web`: `RenameTechnologyProcessRequest(String name)` (тело `PUT /{id}/name`, зеркалит `RenamePartRequest`).

Типизированные id (`PartId`, `TechnologyProcessId`) сериализуются как `{"value":"..."}` — конвенция проекта (см. `AddPlanItemRequest`/`PlanControllerTest`); `partId` в JSON передаётся объектом `{"value":"..."}`.

**Альтернатива:** передавать `partId` строкой UUID. Отклонено: проект единообразно использует типизированные id в DTO.

### D4. Контракт входного порта `TechnologyService`

```java
TechnologyProcessView create(CreateTechnologyProcessRequest request);
TechnologyProcessView get(TechnologyProcessId id);
List<TechnologyProcessView> list(PartId partId);        // partId == null → все процессы
void rename(TechnologyProcessId id, String newName);
void delete(TechnologyProcessId id);
OperationView addOperation(TechnologyProcessId id, AddOperationRequest request);
void removeOperation(TechnologyProcessId id, int number);
void markAsDefault(TechnologyProcessId id);
void unmarkDefault(TechnologyProcessId id);
```

`list(PartId partId)` использует `null` как «без фильтра» (репозиторий уже разделяет `findAll` и `findByPartId`). Организация никогда не передаётся клиентом.

**Альтернатива:** отдельный `TechnologyProcessFilter`-record. Отклонено как избыточное для единственного фильтра MVP; при появлении новых критериев вернуться к record-фильтру.

### D5. `TechnologyApplicationService` — зависимости и поток операций

Зависимости: `TechnologyProcessRepository`, доменный `TechnologyProcessService`, `OrganizationContext`, `catalog.api.PartService`, `TechnologyMapper` (view), `Clock`.

- `create`: `requiredOrganizationId()` → проверка `request.partId() != null` (`IllegalArgumentException`, 400) → `TechnologyProcess.create(TechnologyProcessId.generate(), number, name, partId, orgId, clock.instant(), clock.instant())` (валидация `number`/`name` из домена, 400) → `partService.get(partId)` (существование + tenant, `PartNotFoundException`, 404) → `domainService.register(process)` (дубликат номера/инвариант дефолтности, 409) → `mapper.toView(process)`.
- `get`/`list`: чтение через порт `findById`/`findAll`/`findByPartId` (tenant-scoped, исключает удалённые), отсутствие → `TechnologyProcessNotFoundException` (404).
- `rename`/`delete`/`addOperation`/`removeOperation`: загрузка агрегата через порт, вызов мутатора агрегата (`rename`/`markDeleted`/`addOperation`/`removeOperation`), `repository.save`. Валидация и ошибки — из домена (`IllegalArgumentException` 400, `DuplicateOperationException` 409, `OperationNotFoundException` 404).
- `markAsDefault`/`unmarkDefault`: делегирование в доменный `TechnologyProcessService` без дублирования правил.

Порядок в `create` даёт приоритет ошибок **400 → 404 → 409** и не дублирует доменную валидацию и кросс-агрегатные правила в application-слое (требования 3–5). `createdAt`/`updatedAt` при создании берутся из `Clock` (A8); мутаторы агрегата продолжают использовать `Instant.now()` (существующее поведение домена).

**Альтернатива:** проверять деталь до создания агрегата. Отклонено: тогда пустые `number`/`name` дали бы 404 раньше 400, что противоречит приоритету валидации.

### D6. Минимальная доменная фабрика восстановления `TechnologyProcess.restore(...)`

Агрегат можно построить только через `create(...)`, которая принудительно задаёт `defaultProcess = false`, `deletedAt = null` и пустой набор операций, а `markAsDefault()`/`markAsNotDefault()` — package-private. Поэтому персистентный round-trip без потерь (требование 21: `defaultProcess`, временные метки, операции) невозможен без точечного дополнения домена.

Добавляется публичная статическая фабрика:

```java
public static TechnologyProcess restore(
        TechnologyProcessId id, String number, String name, PartId partId,
        OrganizationId organizationId, boolean defaultProcess,
        Instant createdAt, Instant updatedAt, Instant deletedAt,
        List<Operation> operations)
```

Она строит агрегат через приватный конструктор, задаёт все скалярные поля (включая `defaultProcess` и nullable `deletedAt`) и копирует операции во внутренний список, отсортированный по номеру, **не** вызывая `addOperation` (чтобы не перезаписать `updatedAt`). `create(...)` остаётся единственным способом создания новых процессов (с `defaultProcess = false`), существующие доменные правила и сигнатуры не меняются.

Это **единственное** изменение домена в данном change; остальные доменные типы остаются как есть.

**Альтернатива:** рефлексивное заполнение полей в маппере или public-сеттеры `defaultProcess`/`deletedAt`. Отклонено: ломает инкапсуляцию и чистоту домена.

### D7. JPA-персистентность по образцу `JpaPlanRepository`/`PlanMapper`

Сущности (без `@GeneratedValue`, идентификатор — доменный):

- `TechnologyProcessEntity` (`@Table("technology_process")`): `id` (`@Id` UUID), `number`, `name`, `partId` (`part_id`), `organizationId` (`organization_id`), `defaultProcess` (`default_process`), `createdAt` (`created_at`), `updatedAt` (`updated_at`), `deletedAt` (`deleted_at`, nullable `Instant`).
- `TechnologyProcessOperationEntity` (`@Table("technology_process_operation")`, `@IdClass(TechnologyProcessOperationId)`): `processId` (`process_id`, `@Id`), `operationNumber` (`operation_number`, `@Id`), `operationName` (`operation_name`).
- `TechnologyProcessOperationId` (`Serializable`): `processId`, `operationNumber`.

Spring Data-интерфейсы:

- `TechnologyProcessJpaRepository extends JpaRepository<TechnologyProcessEntity, UUID>`: `findByOrganizationIdAndId`, `findByOrganizationIdAndPartIdAndDeletedAtIsNullOrderByNumberAsc`, `findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc`, `existsByOrganizationIdAndNumberAndDeletedAtIsNull`.
- `TechnologyProcessOperationJpaRepository extends JpaRepository<TechnologyProcessOperationEntity, TechnologyProcessOperationId>`: `findByProcessIdOrderByOperationNumberAsc`, `deleteByProcessId`.

Адаптер `JpaTechnologyProcessRepository implements TechnologyProcessRepository`:

- `save`: `repository.save(mapper.toEntity(process)); repository.flush();` затем замена набора операций `operationRepository.deleteByProcessId(processId); operationRepository.flush(); operationRepository.saveAll(mapper.toOperationEntities(process));` (паттерн `JpaPlanRepository`).
- `findById`/`findByPartId`/`findAll`: выборка сущностей и восстановление агрегата с операциями через маппер (N+1 по операциям допустим для MVP, как в `JpaPlanRepository`).
- `existsByNumber`: производный запрос с фильтром `DeletedAtIsNull`.

Инфраструктурный маппер `technology.infrastructure.persistence.TechnologyMapper` (singleton): `toEntity`, `toOperationEntities`, `toDomain(entity, operations)`. `toDomain` использует `TechnologyProcess.restore(...)` (D6), передавая `defaultProcess`, `deletedAt`, временные метки и операции, отсортированные по `operation_number`.

**Альтернатива:** JPA-отношения `@OneToMany`/`cascade`. Отклонено: проект придерживается ручного маппинга родителя и детей (см. `planning`), что сохраняет контроль над заменой набора операций.

### D8. Flyway-миграция `V5__technology.sql`

```sql
CREATE TABLE technology_process (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    part_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    default_process BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX technology_process_number_org_uq
    ON technology_process (organization_id, number) WHERE deleted_at IS NULL;

CREATE TABLE technology_process_operation (
    process_id UUID NOT NULL REFERENCES technology_process (id) ON DELETE CASCADE,
    operation_number INT NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (process_id, operation_number)
);
```

Следующая после `V4__planning.sql`. Частичный уникальный индекс переносит обязательство из design change 21 (A7): уникальность номера активных процессов гарантируется на уровне БД, удалённый процесс номер освобождает. `part_id` — без FK (как `assembly_id` в `plan_item`, A6 проверяет существование детали через `PartService`).

**Альтернатива:** FK на `part(id)`. Отклонено: консистентно с `planning`, существование детали проверяется application-слоем.

### D9. Сборка бинов в `WebConfig` (composition root)

В `WebConfig` добавляется секция `// --- Technology ---`:

- `TechnologyProcessRepository technologyProcessRepository(TechnologyProcessJpaRepository, TechnologyProcessOperationJpaRepository)` → `new JpaTechnologyProcessRepository(...)` (инфраструктурный маппер — внутренний singleton `INSTANCE`).
- `TechnologyMapper technologyMapper()` → `new TechnologyMapper()` (application view-маппер).
- `TechnologyProcessService technologyProcessService(TechnologyProcessRepository)` → `new TechnologyProcessService(...)` — доменный сервис как `@Bean`, сам класс остаётся без Spring-аннотаций (A9).
- `TechnologyApplicationService technologyService(TechnologyProcessRepository, TechnologyProcessService, OrganizationContext, PartService, TechnologyMapper, Clock)` → `new TechnologyApplicationService(...)`.

`TechnologyProcessController` — тонкий `@RestController` в `web`, регистрируется component scan (как `PlanController`, `PartController`), **не** объявляется `@Bean` в `WebConfig`, чтобы избежать двойной регистрации с `@RestController`. Spring Data-интерфейсы (`*JpaRepository`) регистрируются автоматически.

**Альтернатива:** объявить контроллер `@Bean` в `WebConfig` (как `AssemblyController`). Отклонено как менее консистентное с эталоном `planning` для нового модуля; при стандартизации на явном `@Bean` следует снять `@RestController` с класса.

### D10. Маппинг ошибок в `RestExceptionHandler`

Добавляются обработчики (контракт ошибок — `{"error": "<code>", "message": "..."}`):

- `{TechnologyProcessNotFoundException, OperationNotFoundException}` → 404 `not_found`.
- `{DuplicateTechnologyProcessNumberException, DuplicateOperationException}` → 409 `duplicate`.
- `InvalidDefaultProcessStateException` → 409 `invalid_state` (уточнение A10).

`PartNotFoundException` (создание с чужой/отсутствующей деталью) и `IllegalArgumentException` (валидация) уже замапплены существующими обработчиками (404 `not_found`, 400 `bad_request`).

**Гонка уникальности номера (F1).** Проверка `existsByNumber` в `TechnologyProcessService.register` и `repository.save` не атомарны: два параллельных `create` с одинаковым `(organization_id, number)` могут оба пройти проверку, после чего вторая вставка нарушит частичный уникальный индекс (D8) и Spring Data выбросит `org.springframework.dao.DataIntegrityViolationException`, который без точечного маппинга превратится в 500. Поэтому маппинг выполняется **точечно в JPA-адаптере**, а не общим `@ExceptionHandler(DataIntegrityViolationException.class)` (иначе замаскировались бы FK-нарушения и прочие ограничения).

Механизм в `JpaTechnologyProcessRepository.save(...)`:

- `repository.save(mapper.toEntity(process)); repository.flush();` оборачивается в `try/catch (DataIntegrityViolationException e)`.
- По цепочке причин извлекается `org.hibernate.exception.ConstraintViolationException`; Hibernate заполняет имя нарушенного ограничения в `getConstraintName()`.
- Если `getConstraintName()` равен `"technology_process_number_org_uq"` — выбрасывается доменный `DuplicateTechnologyProcessNumberException(process.getNumber())` (уже замаплен в 409 `duplicate` выше); транзакция `@Transactional` откатывается как обычно.
- Иначе исходное исключение пробрасывается дальше (например, нарушение FK на `operation.process_id`) и остаётся 500/без ложного 409 — другие ограничения не маскируются.

### D11. Транзакционность

`TechnologyApplicationService` помечается `@Transactional` на уровне класса (как `PlanApplicationService`): все изменяющие операции атомарны, а вызовы доменного `TechnologyProcessService` (включая переключение дефолтности из нескольких `save`) выполняются в одной транзакции application-слоя. Домен и порты транзакциями не управляют.

### D12. Тестирование

- `TechnologyApplicationServiceTest`: `InMemoryTechnologyProcessRepository` (уже есть), fake `OrganizationContext` (как в `PlanApplicationServiceTest`), mock `PartService`, реальный `TechnologyMapper` и `TechnologyProcessService`, `Clock.fixed(NOW)`. Покрывает все use case'ы, tenant-изоляцию, приоритеты ошибок и инвариант дефолтности.
- Тесты мапперов: `TechnologyMapper` (application, домен → view, порядок операций) и инфраструктурный `TechnologyMapper` (entity ↔ domain round-trip с `defaultProcess`, `deletedAt`, временными метками и операциями через `restore`).
- `TechnologyProcessControllerTest`: MockMvc standalone + `RestExceptionHandler`, mock `TechnologyService` — все эндпоинты и коды ошибок.
- Интеграционный тест JPA-репозитория (extends `AbstractPostgresIntegrationTest`): round-trip, tenant-изоляция, исключение удалённых, порядок операций, существование частичного уникального индекса и отклонение дубликата на уровне БД (через `DataIntegrityViolationException`/прямую вставку), освобождение номера после soft-delete.
- Интеграционный тест адаптера (в составе `JpaTechnologyProcessRepositoryTest`): повторный `save` активного процесса с тем же `(organization_id, number)` через `JpaTechnologyProcessRepository` (детерминированная имитация гонки между `existsByNumber` и `save`) выбрасывает `DuplicateTechnologyProcessNumberException`, а не `DataIntegrityViolationException`; unit-тест адаптера подтверждает, что `DataIntegrityViolationException` с чужим именем ограничения (например, FK) пробрасывается дальше и не маскируется (F1).
- Интеграционный тест `TechnologyApplicationService` на реальном PostgreSQL (extends `AbstractPostgresIntegrationTest`): реальный бин application-сервиса + `JpaTechnologyProcessRepository`; принудительная ошибка на середине изменяющей операции → проверка `@Transactional`-отката (в БД нет частичных записей) — сценарий I8 (F2).
- Существующие тесты домена (`TechnologyProcessTest`, `TechnologyProcessServiceTest`, `TechnologyDomainPurityTest`, `TechnologyProcessRepositoryPortTest`) и `ModuleVerificationTest` остаются зелёными.

## Risks / Trade-offs

- **[Риск]** Точечное дополнение домена (`restore`) формально меняет доменный слой, который по условию «остаётся как есть». → **Митигировать:** это единственное минимальное добавление, требуемое для round-trip без потерь (D6); существующие фабрики, мутаторы и правила не меняются, а чистота домена проверяется тестом.
- **[Риск]** Гонка при параллельной регистрации одинакового номера между проверкой `existsByNumber` и `save`. → **Митигировать:** частичный уникальный индекс (D8) — финальный гарант на уровне БД; нарушение именно этого индекса точечно транслируется в доменный `DuplicateTechnologyProcessNumberException` в `JpaTechnologyProcessRepository.save` и далее в 409 `duplicate` (D10), без общего `@ExceptionHandler` на `DataIntegrityViolationException`.
- **[Риск]** `updatedAt` мутаторов домена использует `Instant.now()`, а `create` — инжектируемый `Clock`; расхождение часов в тестах. → **Митигировать:** консистентно с допущением A8 и `PartApplicationService`; тесты фиксируют `Clock`, а проверки `updatedAt` не зависят от точного значения.
- **[Компромисс]** N+1 запросов по операциям при чтении списка. → **Митигировать:** допустимо для MVP (как `JpaPlanRepository`); при росте объёмов — batch-загрузка (future change).
- **[Риск]** Двойная регистрация контроллера при неверном выборе wiring-паттерна. → **Митигировать:** контроллер регистрируется только component scan (D9); контекстный тест (`AbstractPostgresIntegrationTest`) подтвердит загрузку `ApplicationContext`.
- **[Компромисс]** `list(PartId partId)` с `null`-сентinelом. → **Митигировать:** задокументировано в D4; при новых фильтрах перейти на record-фильтр.

## Migration Plan

Новая миграция `V5__technology.sql` применяется на чистой базе и поверх существующих данных без переноса данных (таблицы новые). Откат — удаление миграции и добавленных классов/бинов; существующие модули и данные не затрагиваются. `ApplicationModules.verify()` должен проходить после добавления `technology :: technology` в `web/package-info.java`.

## Open Questions

Блокирующих вопросов нет. Отложенные решения (не влияют на scope/подход): семантика дефолтности при удалении единственного дефолтного процесса, валидация номера по паттерну организации, переупорядочивание операций, оборудование/нормы/материалы операций, перенос процесса на другую деталь, интеграция со спецификациями — относятся к future changes.
