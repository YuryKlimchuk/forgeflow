# Design

## Context

Модуль `directories` — пустой скелет (`package-info.java` без классов); его `api` уже помечен `@NamedInterface("directories")`, а `catalog`, `technology` и `specifications` уже декларируют зависимость `directories :: directories`. Требования R1–R29 и допущения A1–A10 зафиксированы в `/tmp/forgeflow-23-directories-requirements.md`. Эталонные вертикальные срезы: `catalog/domain/part` (простой CRUD-агрегат) и `technology` (application + JPA + Flyway V5 + REST + точечная трансляция нарушения частичного уникального индекса). Последняя миграция — `V5__technology.sql`; seed — идемпотентный `seed.sql` в корне, выполняемый вручную после Flyway.

## Goals / Non-Goals

**Goals:**
- Два независимых, богатых доменных агрегата `Material` и `Sortament` с чистой доменной моделью (без Spring/JPA/PostgreSQL/Flyway).
- Полный backend-срез: api → application → JPA-адаптер → PostgreSQL + REST-контроллеры.
- Multi-tenant изоляция через `OrganizationContext` и `organization_id` во всех запросах.
- Мягкое удаление и переиспользование номера после удаления.
- Гарантия уникальности `number` в организации на двух уровнях (application-проверка + частичный уникальный индекс БД).

**Non-Goals (design-level):**
- Никаких связей Material↔Sortament и справочников стандартов/UoM (A1–A3).
- Никаких number-паттернов организации (A5).
- Никакого фронтенда, пагинации и поиска.

## Decisions

### D1. Два независимых агрегата, фабрика `create` без `restore`
`Material` и `Sortament` моделируются по образцу `catalog/domain/part/Part`: приватный конструктор, статическая фабрика `create(id, number, name, standardType, standard, unit, organizationId, createdAt, updatedAt)`, мутаторы `update(...)` и `markDeleted()`. Отдельная фабрика `restore(...)` (как в `TechnologyProcess`) не нужна: JPA-запросы фильтруют `deleted_at IS NULL`, поэтому удалённые агрегаты никогда не загружаются в домен, а `deletedAt` пишется только при сохранении entity. Альтернатива (единая `restore`-фабрика) отвергнута как избыточная для агрегатов без вложенных коллекций.

### D2. `StandardType` в `directories.api`
Перечисление `StandardType {GOST, DIN, ISO, OTHER}` размещается в `directories.api` (а не в `domain`), потому что оно входит в публичный контракт (`Create*Request`, `Update*Request`, `*View`) и используется `web` через `directories.api`. Домен импортирует его из `api` — это та же конвенция, что `PartId`/`TechnologyProcessId` (типизированные ID живут в `api`, домен на них ссылается).

### D3. `standard`/`unit` — строки, без отдельных справочников
Согласно A1/A2: `standardType` + `standard` (полное обозначение) описывают стандарт; `unit` — обязательная непустая строка. Никаких таблиц `standard`/`uom` и связей не создаётся.

### D4. Границы модуля `directories` и `web`
`directories/package-info.java` → `@ApplicationModule(type = OPEN, allowedDependencies = {"shared :: shared"})`. Тип `OPEN` обязателен: composition root (`WebConfig` в корневом пакете) инстанцирует `JpaMaterialRepository`/`MaterialApplicationService` и т.д., что для закрытого модуля запрещено. `web/package-info.java` добавляет `"directories :: directories"`; обращение идёт только через `directories.api` (`@NamedInterface("directories")`). `catalog`/`technology`/`specifications` уже декларируют `directories :: directories` — изменений не требуют. `ApplicationModules.verify()` проходит (граф ацикличен: `web -> directories -> shared`).

### D5. Composition root и тонкие контроллеры
Бизнес-бины `directories` (JPA-репозитории, application-мапперы, application-сервисы) собираются в `WebConfig` через `@Bean`, без `@Service`/`@Component`/`@Repository` внутри модуля. Контроллеры `MaterialController`/`SortamentController` — тонкие `@RestController` в пакете `web`, регистрируются component scan (как `PlanController`/`TechnologyProcessController`). Это удовлетворяет R6 (никаких Spring-стереотипов внутри модуля `directories`) и согласуется с актуальным паттерном контроллеров проекта; старый паттерн «контроллер как @Bean в WebConfig» (catalog-контроллеры) не используется.

### D6. Порты репозиториев и Spring Data отделены
Доменные порты `MaterialRepository`/`SortamentRepository` (`domain`) — свободные от аннотаций интерфейсы с методами `save`, `findById(id, organizationId)`, `findAll(organizationId)`, `existsByNumber(number, organizationId)`. Spring Data-интерфейсы `MaterialJpaRepository`/`SortamentJpaRepository` (`infrastructure.persistence`) — отдельные `JpaRepository<Entity, UUID>` с derived-методами `findByOrganizationIdAndIdAndDeletedAtIsNull`, `findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc`, `existsByOrganizationIdAndNumberAndDeletedAtIsNull`. Сортировка по `number` гарантируется на уровне запроса и в in-memory тестовом репозитории.

### D7. Уникальность номера: проверка + частичный индекс + точечная трансляция
`create` в application-сервисе сначала вызывает `repository.existsByNumber(...)` (→ `Duplicate*NumberException`), затем создаёт и сохраняет агрегат. Финальная защита от гонки — частичный уникальный индекс БД `(organization_id, number) WHERE deleted_at IS NULL`. JPA-адаптер при `save` выполняет `repository.save(entity)` + `repository.flush()` и перехватывает `DataIntegrityViolationException`; если в цепочке причин есть `ConstraintViolationException` с именем ограничения `material_number_org_uq`/`sortament_number_org_uq`, транслирует в доменное `Duplicate*NumberException`, иначе пробрасывает исходное исключение (не маскирует другие ограничения). Это 1:1 паттерн `JpaTechnologyProcessRepository`.

### D8. Исключения в `domain`, маппинг в `RestExceptionHandler`
В `domain` создаются `MaterialNotFoundException`, `DuplicateMaterialNumberException`, `SortamentNotFoundException`, `DuplicateSortamentNumberException` (наследуют `RuntimeException`, без базового класса — по образцу `technology`). `RestExceptionHandler` добавляет: `{MaterialNotFoundException, SortamentNotFoundException}` → 404 `not_found`; `{DuplicateMaterialNumberException, DuplicateSortamentNumberException}` → 409 `duplicate`. `IllegalArgumentException` уже маппится → 400 `bad_request`.

### D9. Миграция `V6__directories.sql`
Две таблицы `material` и `sortament` в стиле V2/V5: `id UUID PRIMARY KEY`, `number/name/standard_type/standard/unit VARCHAR(255) NOT NULL`, `organization_id UUID NOT NULL`, `created_at/updated_at TIMESTAMPTZ NOT NULL`, `deleted_at TIMESTAMPTZ`, и частичные уникальные индексы `material_number_org_uq`/`sortament_number_org_uq` на `(organization_id, number) WHERE deleted_at IS NULL`. `ddl-auto` остаётся `none`.

### D10. JPA-сущности
`MaterialEntity`/`SortamentEntity` — `@Entity @Table`, публичные поля (стиль `TechnologyProcessEntity`); `standardType` маппится `@Enumerated(EnumType.STRING)` в колонку `standard_type` (default naming strategy переводит `standardType` → `standard_type`). Мапперы `infrastructure.persistence.MaterialMapper`/`SortamentMapper` — ручные, `entity ↔ domain` (в домен через `create`, без `deletedAt`); `application.MaterialMapper`/`SortamentMapper` — ручные, `domain → *View`.

### D11. Обновление — полный `PUT /{id}` без `number` (A6)
`Update*Request(name, standardType, standard, unit)`; агрегатный метод `update(...)` валидирует все четыре поля и обновляет `updatedAt`; `number` неизменяем. Контроллер возвращает `204 No Content`. `IllegalArgumentException`/`null` → 400.

### D12. Seed: расширение `seed.sql`
Дополняется секциями `-- material` и `-- sortament` для демо-организации `00000000-0000-0000-0000-000000000001` с фиксированными UUID и `ON CONFLICT DO NOTHING` (идемпотентно). Приложение не выполняет `seed.sql` (R26). Демо-набор (минимум ГОСТ/DIN/ISO в обоих справочниках):

| id | number | name | standardType | standard | unit |
|----|--------|------|--------------|----------|------|
| 00000000-0000-0000-0000-000000000101 | 45 | Сталь 45 | GOST | ГОСТ 1050-2013 | кг |
| 00000000-0000-0000-0000-000000000102 | 09Г2С | Сталь 09Г2С | GOST | ГОСТ 19281-2014 | кг |
| 00000000-0000-0000-0000-000000000103 | Д16Т | Дюралюминий Д16Т | GOST | ГОСТ 4784-2019 | кг |
| 00000000-0000-0000-0000-000000000104 | 1.4301 | Сталь нержавеющая 1.4301 (X5CrNi18-10) | DIN | DIN EN 10088-2 | кг |
| 00000000-0000-0000-0000-000000000105 | C45E | Сталь C45E | ISO | ISO 683-1 | кг |
| 00000000-0000-0000-0000-000000000201 | Круг В-30 | Круг горячекатаный В-30 | GOST | ГОСТ 2590-2006 | мм |
| 00000000-0000-0000-0000-000000000202 | Лист 2 мм | Лист горячекатаный 2 мм | GOST | ГОСТ 19903-2015 | мм |
| 00000000-0000-0000-0000-000000000203 | Труба 57х3.5 | Труба стальная бесшовная 57х3.5 | GOST | ГОСТ 8732-78 | мм |
| 00000000-0000-0000-0000-000000000204 | Уголок 50х50х5 | Уголок стальной равнополочный 50х50х5 | GOST | ГОСТ 8509-93 | мм |
| 00000000-0000-0000-0000-000000000205 | Труба EN 10210 S235JRH | Труба конструкционная S235JRH | DIN | DIN EN 10210 | мм |
| 00000000-0000-0000-0000-000000000206 | Лист ISO 636 | Прокат плоский ISO 636 | ISO | ISO 636 | мм |

### D13. Тесты по пирамиде
- Домен: `MaterialTest`/`SortamentTest` (валидация полей, `update`, `markDeleted` idempotency, неизменяемость `number`), `MaterialIdTest`/`SortamentIdTest`, `StandardTypeTest` (состав), `MaterialRepositoryPortTest`/`SortamentRepositoryPortTest` (форма порта), `DirectoriesDomainPurityTest` (аннотации/импорты).
- Application: `MaterialApplicationServiceTest`/`SortamentApplicationServiceTest` на `InMemoryMaterialRepository`/`InMemorySortamentRepository` (создание/чтение/список/обновление/удаление/уникальность/изоляция/сортировка/отсутствие контекста).
- Мапперы: `application.MaterialMapperTest`/`SortamentMapperTest`, `infrastructure.persistence.MaterialMapperTest`/`SortamentMapperTest`.
- JPA-адаптер: `JpaMaterialRepositoryTest`/`JpaSortamentRepositoryTest` (`AbstractPostgresIntegrationTest`, Testcontainers/PostgreSQL): round-trip, tenant-изоляция, soft-delete, частичный индекс, дубликат на уровне БД, повторное использование номера после удаления, трансляция дубликата в доменное исключение.
- Контроллеры: `MaterialControllerTest`/`SortamentControllerTest` (MockMvc standalone + `RestExceptionHandler`): статусы 201/200/204, тела, 404/409/400.
- Архитектура: `ModuleVerificationTest` (уже существует) покрывает границы.

### D14. Сортировка и семантика списка
`list()` не имеет фильтра (R17/R19); возвращает активные записи текущей организации, отсортированные по `number` (лексикографически, `ORDER BY number ASC`). Никакой пагинации.

## Risks / Trade-offs

- [Pre-existing drift `module-boundaries`] → Спека `module-boundaries` исторически отстала от кода (не упоминает `planning`, занижает зависимости `web`). Этот change обновляет только затронутые требования («web», «open modules», «allowed deps», «cycles»); полную сверку остальных пунктов оставляем отдельному cleanup-изменению, чтобы не расширять scope.
- [Enum хранится как VARCHAR без CHECK-ограничения] → невалидное значение может попасть в БД только прямым SQL. Митigation: приложение всегда пишет через enum; стиль проекта не использует CHECK-ограничения. Приемлемо.
- [Точное строковое совпадение номера] → уникальность без trim/case-normalization (как в `catalog`); номера «45» и « 45 » считаются разными. Зафиксировано как принятое поведение.
- [Гонка уникальности] → application-проверка `existsByNumber` не атомарна; защита — частичный уникальный индекс + трансляция `ConstraintViolationException` → 409. Покрывается тестом трансляции.
- [Состав демо-данных] → точный набор A10 фиксируется таблицей D12; единицы сортаментов («мм») иллюстративны и семантически корректны для размерных сортаментов, но могут быть уточнены при реализации без изменения контрактов.

## Migration Plan

1. Добавить `V6__directories.sql` (только `CREATE TABLE` + `CREATE UNIQUE INDEX`, без изменения существующих таблиц).
2. Обновить `directories/package-info.java` (`OPEN` + `shared :: shared`) и `web/package-info.java` (+ `directories :: directories`).
3. Реализовать домен, api, application, infrastructure, контроллеры и `WebConfig`/`RestExceptionHandler`.
4. Дополнить `seed.sql`.
5. Прогнать `mvn test` (включая `ApplicationModules.verify()` и интеграционные тесты на Testcontainers).
Rollback: миграция `V6` только добавляет таблицы; откат — удаление таблиц `material`/`sortament` и возврат деклараций модулей. Существующие данные не затрагиваются.

## Open Questions

Нет: A1–A10 закрывают неопределённости; блокирующих вопросов для проектирования не осталось.
