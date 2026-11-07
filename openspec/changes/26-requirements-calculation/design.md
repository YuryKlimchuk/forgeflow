# Design

## Context

Мотивация и границы — в `proposal.md`. Поведение — в `specs/planning-requirements` и `specs/planning-requirements-rest`.

Актуальное состояние: модуль `planning` уже содержит богатый агрегат `Plan`/`PlanItem`, tenant-scoped порт `PlanRepository`, application-сервис `PlanApplicationService` и JPA-адаптер (`V4__planning.sql`). Сводный расчёт на лету `PlanService.flatten` вызывает `specifications.api.SpecificationService.flatten(assemblyId)`, который возвращает только листья (`number`/`name`/`quantity`) и не содержит сборок, типа/id, вида потребности и единицы — поэтому для сохраняемого расчёта он непригоден. `specifications.api.SpecificationService.get(assemblyId)` возвращает прямые строки (`CatalogItemReference` = `тип+id`, `quantity`), а `catalog.api.*Service` возвращают `number`/`name` элементов. Модуль `planning` уже декларирует `allowedDependencies = {"catalog :: catalog", "specifications :: specifications", "shared :: shared"}`, `web` зависит от `planning :: planning`; последняя миграция — `V7__technology_equipment.sql`.

## Goals / Non-Goals

**Goals:**

- Сохраняемый, неизменяемый, tenant-scoped расчёт потребностей с историей, снапшотом и прослеживаемостью источников.
- Бизнес-алгоритм разузлования — в доменном слое `planning`, без Spring/JPA/внешних сервисов; данные получаются через доменный порт.
- Переиспользовать существующие публичные API `specifications` и `catalog` без их изменения.
- Сохранить контракт `GET /api/v1/plans/{id}/flat` и все существующие операции планов.

**Non-Goals (design-level):**

- Не менять модули `specifications` и `catalog`; не добавлять тип `MATERIAL` в ссылки спецификаций.
- Не моделировать версионирование спецификаций (лёгкий снапшот, а не история версий).
- Не учитывать остатки/закупки/net; только gross.
- Не реализовывать фронтенд и экспорт.

## Decisions

### D1. Размещение и границы capabilities

Функция остаётся в модуле `planning` и описывается двумя новыми capabilities OpenSpec: `planning-requirements` (домен/application) и `planning-requirements-rest` (REST). Существующие `planning`/`planning-rest` не изменяются: `flatten` и операции планов не затрагиваются.

Альтернатива — расширить `planning`/`planning-rest` — отклонена: это размыло бы уже утверждённые требования и нарушило обратную совместимость без необходимости.

### D2. Доменная модель

В `planning.domain` добавляются:

- `RequirementCalculationId` (UUID, по образцу `PlanId`).
- `RequirementCalculation` — корень агрегата: `id`, `organizationId`, `planId`, `executedAt`, `List<RequirementItem>`. Приватный конструктор + фабрика `create(...)`, коллекция `Collections.unmodifiableList`, мутаторов нет → агрегат неизменяем после создания.
- `RequirementItem` — value object позиции: `CatalogItemType type`, `UUID id`, `String number`, `String name`, `int quantity`, `NeedType needType`, `String unit`, `List<AssemblyId> sources`. Инварианты: непустые `number`/`name`, `quantity > 0`, `unit` непустой, `sources` непустой и без дубликатов.
- `NeedType` — перечисление размещается в `planning.api` (входит в публичный контракт, как `CatalogItemType` в `specifications.api`): `MANUFACTURE` («изготовить»), `PURCHASE` («закупить»). Поле JSON — `needType`.
- `RequirementSource` — driven-порт домена (см. D6).
- `RequirementCalculator` — доменный сервис алгоритма (см. D5).
- `RequirementCalculationRepository` — driven-порт хранения.

`type` позиции переиспользует `specifications.api.CatalogItemType` (те же пять значений), `sources` — `catalog.api.AssemblyId`; домен `planning` уже легально зависит от `catalog.api` и `specifications.api` (как `PlanItem` от `AssemblyId`).

### D3. Вид потребности и единица на проводе

`NeedType` сериализуется в JSON как строковые значения `MANUFACTURE`/`PURCHASE` (соответствуют «изготовить»/«закупить»), единица — строка `шт`. Выбор строкового enum по образцу `CatalogItemType` сохраняет единообразие API; русские литералы в поле enum не вводятся.

Альтернатива — русские строки `"изготовить"/"закупить"` — отклонена как несовместимая с принятой конвенцией enum-значений (`PART`, `ASSEMBLY`, …) в существующих контрактах.

### D4. Источники позиции — набор сборок плана (Вариант A)

Позиция агрегируется по паре `тип+id`, а источник хранится как набор `AssemblyId` (сборок плана), через которые позиция попала в результат. Это зафиксированное решение (повторное согласование не требуется): удовлетворяет одновременно агрегации по `тип+id` и прослеживаемости при плане из нескольких сборок. В API позиция отдаёт `sources: [assemblyId, ...]`, в БД — дочерняя таблица `requirement_calculation_item_source`.

### D5. Алгоритм разузлования

`RequirementCalculator.calculate(Plan plan, RequirementSource source)` — чистая функция без Spring/IO:

1. Для каждого `PlanItem` (головная сборка `rootAssemblyId`, `planQuantity`) запускается DFS с `path = new LinkedHashSet<>()` и аккумулятором `Map<CatalogItemReference, Contribution>`.
2. При входе в сборку: если `!path.add(assemblyId)` → `RequirementCycleException`. Затем резолвится `descriptor(ASSEMBLY, assemblyId)`: для головной сборки пустой результат даёт `RequirementSourceItemNotFoundException` (404 `not_found`), для вложенной — `SpecificationItemNotFoundException` (404 `specification_item_not_found` с идентификатором ссылки). Сборка эмитится как позиция `ASSEMBLY` с количеством `multiplier`, `needType=MANUFACTURE`, `unit="шт"`, источником `rootAssemblyId`.
3. Прямые строки читаются через `source.rows(assemblyId)`; для каждой строки `rowQuantity = Math.multiplyExact(multiplier, row.quantity())`. Если тип строки `ASSEMBLY` — рекурсивный спуск с `rowQuantity`; иначе резолвится `descriptor(row.item)` (пустой результат → `RequirementSourceItemNotFoundException`, 404 `not_found`) и строка эмитится как позиция (для `PART` — `MANUFACTURE`, для `BUY`/`STANDART`/`VZK` — `PURCHASE`) с источником `rootAssemblyId`.
4. `path.remove(assemblyId)` в `finally` (корректный откат при исключении).
5. Агрегация: ключ `CatalogItemReference` (`тип+id`), `Math.addExact` для количества, объединение множества источников.
6. Итог сортируется детерминированно: по `number`, затем по `type`, затем по `id`.

Источник позиции — именно головная сборка плана (`rootAssemblyId`), а не непосредственный родитель в BOM: требование №13 определяет источник как «сборку плана, породившую позицию».

### D6. Доменный порт данных и адаптер

Домен не обращается к application-сервисам других модулей. Вводится порт:

```java
// planning.domain
public interface RequirementSource {
    record SourceRow(CatalogItemReference item, int quantity) {}
    record ItemDescriptor(String number, String name) {}
    List<SourceRow> rows(AssemblyId assemblyId);
    Optional<ItemDescriptor> descriptor(CatalogItemReference item);
}
```

`rows(assemblyId)` возвращает прямые строки существующей сборки (сборка должна быть уже валидирована через `descriptor`). `descriptor(item)` возвращает пустой `Optional`, если элемент каталога отсутствует, удалён или принадлежит другой организации.

Адаптер `CatalogRequirementSource` (в `planning.application`) реализует порт поверх существующих публичных API: `rows()` → `specificationService.get(assemblyId)` (маппинг `SpecificationRowView` → `SourceRow`), `descriptor()` → switch по `type` и вызов `assemblyService/partService/buyService/standartService/vzkService.get(...)`; `*NotFoundException` из каталога отлавливается и превращается в `Optional.empty()`.

Следствия, фиксируемые как принятое поведение:

- Отсутствующая/чужая **головная сборка плана** → `RequirementSourceItemNotFoundException` → 404 `not_found` (как и у `flatten`).
- Ссылка строки спецификации на отсутствующую/чужую **вложенную сборку** → `SpecificationItemNotFoundException` с идентификатором проблемной ссылки → 404 `specification_item_not_found`; частичный результат не сохраняется (решение пользователя, вариант A).
- Ссылка строки спецификации на отсутствующий/чужой **лист каталога** (`PART`/`BUY`/`STANDART`/`VZK`) → `RequirementSourceItemNotFoundException` → 404 `not_found` (поведение, аналогичное `flat`).

Циклы и переполнение детектируются самим `planning` (собственные исключения), а не переиспользованием внутреннего `flatten`/`BomCycleException` из `specifications`.

Альтернатива — новый read-only порт в `specifications` без каталог-валидации — отклонена как расширение scope и изменение соседнего модуля без необходимости.

### D7. Исключения и HTTP-маппинг

В `planning.domain` добавляются `RequirementCycleException`, `RequirementQuantityOverflowException` (оборачивает `ArithmeticException` из `multiplyExact`/`addExact`), `RequirementCalculationNotFoundException`, `SpecificationItemNotFoundException` (несёт `CatalogItemReference` проблемной ссылки) и `RequirementSourceItemNotFoundException` (несёт `CatalogItemReference` отсутствующего элемента источника). В `RestExceptionHandler`:

- `RequirementCalculationNotFoundException` → 404 `not_found`;
- `SpecificationItemNotFoundException` → 404 `specification_item_not_found` (сообщение содержит идентификатор проблемной ссылки);
- `RequirementSourceItemNotFoundException` → 404 `not_found`;
- `RequirementCycleException` → 409 `bom_cycle`;
- `RequirementQuantityOverflowException` → 400 `quantity_overflow`;
- существующие `PlanNotFoundException`/`PlanItemNotFoundException` уже дают 404 `not_found` для отсутствующего/чужого плана и элемента плана.

### D8. Persistence и миграция `V8__requirement_calculation.sql`

Три таблицы (стиль V4):

```sql
CREATE TABLE requirement_calculation (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES plan (id),
    executed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX requirement_calculation_org_plan_idx ON requirement_calculation (organization_id, plan_id);

CREATE TABLE requirement_calculation_item (
    id UUID PRIMARY KEY,
    calculation_id UUID NOT NULL REFERENCES requirement_calculation (id) ON DELETE CASCADE,
    item_type VARCHAR(32) NOT NULL,
    item_id UUID NOT NULL,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    need_type VARCHAR(32) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    UNIQUE (calculation_id, item_type, item_id)
);

CREATE TABLE requirement_calculation_item_source (
    calculation_item_id UUID NOT NULL REFERENCES requirement_calculation_item (id) ON DELETE CASCADE,
    source_assembly_id UUID NOT NULL,
    PRIMARY KEY (calculation_item_id, source_assembly_id)
);
```

Снапшот гарантируется денормализацией `number`/`name`/`quantity`/`need_type`/`unit` в позиции и отсутствием FK на элементы каталога/строки спецификаций. `organization_id` хранится на расчёте; все запросы JPA-адаптера фильтруют по `organizationId` (tenant-изоляция на уровне приложения, общая схема БД). JPA-сущности (`RequirementCalculationEntity`, `RequirementCalculationItemEntity`, `RequirementCalculationItemSourceEntity` с `@IdClass`) и ручные мапперы — по образцу `planning`/`directories`. `save` — только вставка (`persist`/`save` нового агрегата), без update.

Порядок позиций в БД не гарантирован, поэтому при реконструкции агрегата из сущностей маппер восстанавливает детерминированную сортировку позиций по `number`, затем `type`, затем `id` (та же, что в `RequirementCalculator`).

### D9. REST-контракт и composition root

Новый тонкий контроллер `RequirementCalculationController` (`web`):

- `POST /api/v1/plans/{planId}/calculations` → 201 `RequirementCalculationView`;
- `GET /api/v1/plans/{planId}/calculations` → 200 `List<RequirementCalculationView>`;
- `GET /api/v1/plans/{planId}/calculations/{calculationId}` → 200 `RequirementCalculationView`.

Для `GET .../calculations` и `GET .../calculations/{calculationId}` перед чтением application-сервис валидирует существование плана и его принадлежность текущей организации через `PlanRepository.findById(org, planId)`; отсутствующий/чужой план → `PlanNotFoundException` → 404 `not_found` (а не 200 с пустым списком). Отсутствующий/чужой расчёт внутри существующего плана → `RequirementCalculationNotFoundException` → 404 `not_found`.

`GET /api/v1/plans/{id}/flat` в `PlanController` остаётся без изменений. В `WebConfig` регистрируются бины: `RequirementCalculationRepository` (JPA), `RequirementCalculationMapper`, `RequirementCalculator`, `CatalogRequirementSource` (с `SpecificationService` + пять `catalog`-сервисов) и `RequirementCalculationApplicationService` (репозиторий, `OrganizationContext`, калькулятор, источник, `PlanRepository`, `Clock`, маппер). Используется существующий bean `systemClock` (`java.time.Clock`).

### D10. Application-сервис

`RequirementCalculationApplicationService` (реализует `planning.api.RequirementCalculationService`): получает текущую организацию из `OrganizationContext`, загружает план через `PlanRepository.findById(org, planId)` (иначе `PlanNotFoundException`), вызывает `RequirementCalculator`, создаёт `RequirementCalculation` с `executedAt = clock.instant()` и сохраняет через репозиторий, затем маппит в `RequirementCalculationView`.

`list(org, planId)` и `get(org, planId, calculationId)` сначала валидируют план через `PlanRepository.findById(org, planId)` (иначе `PlanNotFoundException` → 404), затем читают расчёты только этого плана/организации; `get` при отсутствии/чужом расчёте бросает `RequirementCalculationNotFoundException` (404). Пустой план даёт валидный расчёт с пустым списком позиций (сохраняется и возвращается).

Сервис помечается `@Transactional` (уровень консистентности снапшота — READ COMMITTED): чтение BOM и сохранение агрегата выполняются в одной транзакции, поэтому при ошибке (`bom_cycle`, `quantity_overflow`, `specification_item_not_found`) частичный результат не сохраняется (откат). Читающие операции `list`/`get` выполняются в read-only транзакции.

### D11. Контракты представлений

`planning.api`:

- `RequirementCalculationView(RequirementCalculationId id, PlanId planId, Instant executedAt, List<RequirementItemView> items)`;
- `RequirementItemView(CatalogItemType type, UUID id, String number, String name, int quantity, NeedType needType, String unit, List<AssemblyId> sources)`.

Поле `type` сериализуется как значение `CatalogItemType`, `needType` — как `NeedType`. Поле `sources` — JSON-массив объектов вида `{"value": "<uuid>"}`: `List<AssemblyId>` сериализуется штатной сериализацией `AssemblyId` (как `PlanItemView.assemblyId` и `AssemblyView.id`), без введения кастомной сериализации. Каждый элемент `sources` идентифицирует головную сборку плана, породившую позицию.

### D12. Тестовая стратегия

- Домен: тесты `RequirementItem`/`RequirementCalculation` (инварианты, неизменяемость), `NeedType` (состав), `RequirementCalculator` на in-memory фейке `RequirementSource` (многоуровневость, агрегация по `тип+id`, включение сборок, вид потребности по типу, источники от нескольких сборок, сортировка); отдельно — цикл (`bom_cycle`), переполнение, пустой план, сборка с пустой спецификацией, VZK как непрозрачный лист, отсутствующая головная сборка (→ `RequirementSourceItemNotFoundException`), dangling-ссылка на вложенную сборку (→ `SpecificationItemNotFoundException` с идентификатором ссылки), отсутствующий лист (→ `RequirementSourceItemNotFoundException`).
- Application: `RequirementCalculationApplicationServiceTest` на in-memory репозиториях/фейке источника (создание/история/tenant-изоляция/404/409/overflow, `list`/`get` для чужого или несуществующего плана → 404, ошибка dangling не сохраняет частичный результат, `@Transactional`-граница/откат).
- JPA: `JpaRequirementCalculationRepositoryTest` (Testcontainers/PostgreSQL): round-trip, фильтрация по организации, каскад item/source, уникальность `(calculation_id, item_type, item_id)`, восстановление порядка позиций при чтении (`number` → `type` → `id`).
- Controller: `RequirementCalculationControllerTest` (MockMvc standalone + `RestExceptionHandler`): 201/200/404 `not_found` (чужой/несуществующий план в `POST` и `GET` list/get)/404 `specification_item_not_found`/409 `bom_cycle`/400 `quantity_overflow`, совместимость `flat`.
- Архитектура: существующий `ModuleVerificationTest` остаётся зелёным (границы `planning` не меняются).

## Risks / Trade-offs

- [Ссылка на удалённую/чужую вложенную сборку в спецификации] → расчёт прерывается ошибкой `specification_item_not_found` с идентификатором проблемной ссылки (не generic `not_found`), частичный результат не сохраняется. Это осознанное отклонение от `flatten` (который молча пропускает отсутствующую вложенную спецификацию), зафиксировано решением пользователя (вариант A). **Митигирование:** закреплено в D5–D7 и покрыто тестами.
- [Денормализованный снапшот без FK на каталог] → позднее удаление/переименование элемента каталога не ломает расчёт и не меняет историю, но снапшот не ссылается на «живой» элемент. **Митигирование:** это требование №12 (снапшот неизменен), ссылка остаётся только парой `тип+id`.
- [Переполнение только на `int`] → количество ограничено `int` (как во всех существующих моделях); при выходе за диапазон — 400 `quantity_overflow`. **Митигирование:** покрыто тестами, расширение типа — отдельное изменение.
- [Большой BOM и N+1 чтение строк/каталога] → рекурсивный обход выполняет чтение на каждый узел через публичные сервисы. **Митигирование:** не оптимизировать преждевременно; корректность важнее, оптимизация — отдельным измеримым изменением.
- [Консистентность снапшота при конкурентном изменении спецификации] → `@Transactional` (READ COMMITTED) на application-сервисе объединяет чтение BOM и сохранение в одну транзакцию, исключая «разорванный» снапшот в рамках одной транзакции. **Митигирование:** закреплено в D10; редкие гонки между параллельными правками спецификации и расчётом — вне scope.
- [Источники в отдельной таблице] → чуть больше запросов при чтении, но сохраняет набор источников без JSON-колонки и с внешним ключом. **Митигирование:** принято ради явной схемы и каскадного удаления.

## Migration Plan

1. Добавить `V8__requirement_calculation.sql` (только новые таблицы/индексы, без изменения существующих).
2. Реализовать `planning.api` (`NeedType`, `RequirementCalculationId`, `RequirementCalculationService`, view-записи).
3. Реализовать `planning.domain` (агрегат, value object, порты, калькулятор, исключения).
4. Реализовать `planning.application` (адаптер источника, application-сервис с `@Transactional`, маппер).
5. Реализовать `planning.infrastructure.persistence` (entities, JpaRepository, JPA-адаптер с восстановлением порядка позиций, мапперы).
6. Собрать бины в `WebConfig`, добавить `RequirementCalculationController`, расширить `RestExceptionHandler` (включая `specification_item_not_found`).
7. Прогнать `mvn test` (unit, application, JPA/Testcontainers, controller, `ApplicationModules.verify()`).

Rollback: миграция `V8` только добавляет таблицы; откат — удаление таблиц и возврат `WebConfig`/контроллера/обработчика исключений. Существующие данные не затрагиваются.

## Open Questions

Нет блокирующих: источник-набор зафиксирован (Вариант A), вид потребности и единица зафиксированы, JSON-форма `sources` зафиксирована (массив объектов `{"value": "<uuid>"}`). Поведение для dangling-ссылки на удалённую/чужую вложенную сборку зафиксировано решением пользователя (вариант A): ошибка `specification_item_not_found` с идентификатором ссылки и запрет частичного сохранения.
