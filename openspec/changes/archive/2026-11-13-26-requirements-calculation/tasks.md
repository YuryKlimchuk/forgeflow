# Tasks

## 1. Схема базы данных

- [x] 1.1 Добавить миграцию `src/main/resources/db/migration/V8__requirement_calculation.sql` с таблицами `requirement_calculation`, `requirement_calculation_item`, `requirement_calculation_item_source`, индексами и ограничениями из design D8; убедиться, что приложение стартует на чистой БД и Flyway применяет V8 (`mvn test` с интеграционным контекстом Testcontainers проходит)

## 2. Публичные контракты `planning.api`

- [x] 2.1 Добавить `NeedType` (MANUFACTURE/PURCHASE), `RequirementCalculationId`, `RequirementItemView`, `RequirementCalculationView` и интерфейс `RequirementCalculationService` (calculate/list/get) в `com.hydroyura.forgeflow.planning.api`; проверить компиляцию `mvn -q -DskipTests compile` и наличие контракта юнит-тестом `NeedTypeTest`/`RequirementCalculationIdTest`

## 3. Доменный слой `planning.domain`

- [x] 3.1 Добавить value object `RequirementItem` (type, id, number, name, quantity, needType, unit, sources) с инвариантами (положительное количество, непустые поля, непустой набор источников без дубликатов) и юнит-тесты `RequirementItemTest`
- [x] 3.2 Добавить агрегат `RequirementCalculation` (id, organizationId, planId, executedAt, items) с фабрикой `create` и неизменяемой коллекцией, без мутаторов; юнит-тесты `RequirementCalculationTest` (неизменяемость, пустой список позиций)
- [x] 3.3 Добавить driven-порты `RequirementCalculationRepository` и `RequirementSource` (`rows` возвращает прямые строки, `descriptor` возвращает `Optional<ItemDescriptor>`) в `planning.domain`
- [x] 3.4 Добавить доменный сервис `RequirementCalculator` (DFS разузлование, умножение `multiplyExact`, агрегация по `тип+id` с `addExact`, включение сборок, источники = сборки плана, детерминированная сортировка; резолв `descriptor` с различением головной сборки, вложенной сборки и листа) и исключения `RequirementCycleException`, `RequirementQuantityOverflowException`, `RequirementCalculationNotFoundException`, `SpecificationItemNotFoundException` (несёт `CatalogItemReference`), `RequirementSourceItemNotFoundException` (несёт `CatalogItemReference`)
- [x] 3.5 Написать юнит-тесты `RequirementCalculatorTest` на in-memory фейке `RequirementSource`: многоуровневость, умножение по цепочке, включение головной/вложенной сборки, вид потребности по типу, агрегация через несколько сборок с набором источников, игнорирование заменителей, цикл (`bom_cycle`), переполнение, пустой план, сортировка; отдельно — сборка с пустой спецификацией, VZK как непрозрачный лист, отсутствующая головная сборка (→ `RequirementSourceItemNotFoundException`), dangling-ссылка на вложенную сборку (→ `SpecificationItemNotFoundException` с идентификатором ссылки), отсутствующий лист (→ `RequirementSourceItemNotFoundException`)

## 4. Application-слой `planning.application`

- [x] 4.1 Добавить адаптер `CatalogRequirementSource` (реализация `RequirementSource` поверх `SpecificationService.get` и `catalog.api.*Service.get`, `*NotFoundException` → `Optional.empty()`) и юнит-тест маппинга строк/дескрипторов на фейках сервисов
- [x] 4.2 Добавить `RequirementCalculationApplicationService` (получение организации из `OrganizationContext`, загрузка плана через `PlanRepository` для calculate/list/get — иначе `PlanNotFoundException`, вызов калькулятора, `executedAt` из `Clock`, сохранение, маппинг в view; `list`/`get` только в контексте организации; метод помечен `@Transactional` (READ COMMITTED) для целостности снапшота и отката частичного результата, чтения — read-only)
- [x] 4.3 Добавить маппер `RequirementCalculationMapper` (domain → view) и юнит-тест `RequirementCalculationMapperTest`
- [x] 4.4 Написать тесты `RequirementCalculationApplicationServiceTest` на in-memory репозиториях: создание расчёта, история без перезаписи, снапшот не меняется после изменения источника, tenant-изоляция (чужой план → 404), пустой план → пустой результат, `list`/`get` для чужого или несуществующего плана → 404 (не пустой список), прокидывание `RequirementCycleException`/`RequirementQuantityOverflowException`/`SpecificationItemNotFoundException` и отсутствие частичного сохранения при ошибке

## 5. Persistence `planning.infrastructure.persistence`

- [x] 5.1 Добавить JPA-сущности `RequirementCalculationEntity`, `RequirementCalculationItemEntity`, `RequirementCalculationItemSourceEntity` (`@IdClass`) и Spring Data-интерфейсы по образцу `planning`/`directories`
- [x] 5.2 Добавить ручные мапперы `infrastructure.persistence` (entity ↔ domain) с восстановлением детерминированного порядка позиций при реконструкции агрегата (`number` → `type` → `id`, как в `RequirementCalculator`) и юнит-тесты round-trip
- [x] 5.3 Добавить `JpaRequirementCalculationRepository` (save только вставкой, `findById`/`findAll` с фильтрацией по `organizationId` и `planId`)
- [x] 5.4 Написать интеграционные тесты `JpaRequirementCalculationRepositoryTest` (Testcontainers/PostgreSQL): round-trip, tenant-изоляция, каскад item/source, уникальность `(calculation_id, item_type, item_id)`, история, восстановление порядка позиций при чтении

## 6. REST-слой и composition root

- [x] 6.1 Добавить `RequirementCalculationController` (`POST /api/v1/plans/{planId}/calculations` → 201, `GET .../calculations` → 200, `GET .../calculations/{calculationId}` → 200) и убедиться, что `GET /api/v1/plans/{id}/flat` не изменён
- [x] 6.2 Зарегистрировать бины в `WebConfig` (JPA-репозиторий, маппер, калькулятор, `CatalogRequirementSource`, application-сервис) и добавить маппинги в `RestExceptionHandler`: 404 `not_found` (`RequirementCalculationNotFoundException`, `RequirementSourceItemNotFoundException`), 404 `specification_item_not_found` (`SpecificationItemNotFoundException`), 409 `bom_cycle`, 400 `quantity_overflow`
- [x] 6.3 Написать контроллер-тесты `RequirementCalculationControllerTest` (MockMvc standalone + `RestExceptionHandler`): 201 с телом расчёта, 200 списка/одного, 404 `not_found` для отсутствующего/чужого плана (POST и `GET` list/get, в т.ч. не 200 с пустым списком) и расчёта, 404 `specification_item_not_found` для dangling-ссылки, 409 `bom_cycle`, 400 `quantity_overflow`, неизменный контракт `flat`

## 7. Интеграционная проверка

- [x] 7.1 Прогнать полный `mvn test` и убедиться, что все unit-, application-, JPA- и controller-тесты, а также `ModuleVerificationTest` (`ApplicationModules.verify()`) проходят; `git status` показывает только ожидаемые новые/изменённые файлы без staging
