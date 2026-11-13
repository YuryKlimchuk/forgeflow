# Test plan — «Расчёт потребностей производства» (change `26-requirements-calculation`)

> Этап QA Planning (G4). Повторная независимая оценка решения Architect после закрытия блокирующих
> находок F1 и F7 и учёта неблокирующих F3–F6. Документ — контракт проверки для G7.
> Артефакты на русском языке. Сводка findings и статус G4 — в отчёте сабагента; здесь — резюме в
> разделах «Risks & assumptions» и «Entry/exit criteria».

## 1. Scope & goals

Проверяем новую функцию модуля `planning`: сохраняемый, tenant-scoped расчёт gross-потребностей по
производственному плану — рекурсивное разузлование спецификаций, умножение количеств по цепочке,
агрегация одинаковых элементов, разделение на «изготовить»/«закупить», снапшот результата,
прослеживаемость источников, история расчётов, детект циклов и переполнения, а также REST-контракт
`POST/GET /api/v1/plans/{planId}/calculations` и обратную совместимость `GET /api/v1/plans/{id}/flat`.

Цели:

- подтвердить соответствие реализации 19 требованиям и 16 AC (Analyst, G2) и OpenSpec-спекам;
- проверить корректность доменного алгоритма разузлования, включая негативные и граничные случаи;
- проверить multi-tenant изоляцию расчётов и стабильность снапшота;
- проверить REST-контракт (201/200/404 `not_found`/404 `specification_item_not_found`/409 `bom_cycle`/400 `quantity_overflow`) и неизменность `flat`;
- зафиксировать риски тестируемости/надёжности решения (см. findings).

**Вне scope (non-goals):** материалы (`directories.Material`), склад/закупки/net-потребности,
версионирование спецификаций (только лёгкий снапшот), экспорт/документы, фронтенд.

**Статус ранее слабых/недостающих AC (по итогам правок Architect):**

- Dangling-ссылка на удалённую/чужую вложенную сборку: **закрыто (F7)** — отдельная ошибка
  `specification_item_not_found` + идентификатор проблемной ссылки, без частичного сохранения
  (решение пользователя, вариант A); зафиксировано в `planning-requirements` («Обработка отсутствующей
  ссылки спецификации») и `planning-requirements-rest` («REST ошибки расчётов»), design D5–D7/D10, tasks 3.4/3.5/4.4/6.2/6.3.
- `GET .../calculations` и `GET .../calculations/{calculationId}` для чужого/несуществующего плана → 404: **закрыто (F1)** — валидация плана перед чтением в design D9/D10, REST-спека «REST чтения расчётов», tasks 4.2/4.4/6.3.
- Сценарий «сборка с пустой спецификацией попадает в результат как „изготовить"»: **добавлен** в спеки (P2).
- VZK — непрозрачная закупка: **добавлен** сценарий в спеки (P14).
- Порядок позиций после round-trip через БД: **закрыто (F3)** — детерминированная сортировка при реконструкции агрегата (design D8/D12, tasks 5.2/5.4).

## 2. Test levels

По пирамиде тестирования (скилл `testing-pyramid`):

| Уровень | Что покрывает | Инструменты |
|---|---|---|
| **Unit — домен** | `RequirementItem`, `RequirementCalculation`, `NeedType`, `RequirementCalculator` (на in-memory fake `RequirementSource`) | JUnit 5 + AssertJ, без Spring |
| **Unit — application** | `CatalogRequirementSource` (маппинг строк/дескрипторов), `RequirementCalculationMapper`, `RequirementCalculationApplicationService` (in-memory репозитории + mocks `OrganizationContext`/`Clock`) | JUnit 5 + AssertJ + Mockito |
| **Slice — persistence** | `JpaRequirementCalculationRepository` round-trip, tenant-фильтрация, каскад item/source, уникальность `(calculation_id, item_type, item_id)`, восстановление порядка позиций | Testcontainers PostgreSQL (наследует `AbstractPostgresIntegrationTest`) |
| **Slice — web** | `RequirementCalculationControllerTest` (standalone `MockMvc` + `RestExceptionHandler`): 201/200/404/409/400; регрессия `PlanControllerTest.flat` | MockMvc standalone |
| **Integration** | Полный `mvn test`, `ModuleVerificationTest` (`ApplicationModules.verify()`), опционально REST tenant-context тест по образцу `CatalogRestTenantContextTest` | Spring Modulith + Testcontainers |

Дополнительно (рекомендация, не блокирует): добавить `RequirementCalculationRestTenantContextTest`
по образцу `CatalogRestTenantContextTest` для подтверждения, что организация берётся только из
серверного `OrganizationContext` (REST-спека, сценарий «Изоляция запроса»), а контекст очищается.

## 3. Test scenarios

### 3.1 Positive

- **P1 Многоуровневое разузлование (AC1, AC2).** План A×2; spec A = {Д1×3, B×2}; spec B = {П1×4}.
  Ожидание: «изготовить» A=2, B=4, Д1=6; «закупить» П1=16. Головная и промежуточная сборка включены.
- **P2 Сборка с пустой спецификацией (R3, R10).** План A×5, spec A пустая → результат содержит A=5 (MANUFACTURE), без ошибки.
- **P3 Агрегация через разные сборки плана (AC3, R13).** Деталь X входит через головные A и B → одна позиция X с суммой количеств и источниками `{A, B}`.
- **P4 Вид потребности по типу (AC4, R4).** PART→MANUFACTURE, ASSEMBLY→MANUFACTURE, STANDART→PURCHASE, BUY→PURCHASE, VZK→PURCHASE.
- **P5 Полный состав полей (AC5, R6).** Каждая позиция содержит `type`, `id`, `number`, `name`, `quantity`, `needType`, `unit` с непустыми `number`/`name` и `quantity > 0`.
- **P6 Единица измерения (R «Единица»).** Для позиций всех типов `unit == "шт"`.
- **P7 Заменитель игнорируется (AC6, R7).** Строка с заменителем → учитывается только основная позиция с её количеством.
- **P8 Пустой план (AC8, R10).** План без элементов → 201 и расчёт с пустым списком позиций; расчёт сохраняется.
- **P9 Сохранение и чтение в том же tenant (AC9).** Сохранённый расчёт читается повторно: `executedAt`, строки, `needType`, количества, источники.
- **P10 История без перезаписи (AC10).** Два запуска → два разных расчёта; `list` возвращает оба; предыдущий не затирается.
- **P11 Стабильность снапшота (AC11, R12).** После сохранения изменить спецификацию/каталог → сохранённый расчёт возвращает прежние `number`/`name`/`quantity`/`needType`/`unit`/`sources`.
- **P12 Источники (R13).** Один источник → набор из одного `assemblyId`; одинаковый элемент из двух головных сборок → объединённый набор из двух.
- **P13 Детерминированная сортировка (design D5, D8).** Результат отсортирован по `number`, затем `type`, затем `id`; повторный расчёт и чтение из БД дают тот же порядок.
- **P14 VZK — непрозрачная закупка (Assumption).** VZK не разузлуется и попадает в «закупить» как лист.

### 3.2 Negative

- **N1 Косвенный цикл (AC7, R8).** A→B→C→A → `RequirementCycleException` → HTTP 409 `bom_cycle`; частичный результат НЕ сохраняется (нет записи в БД).
- **N2 Переполнение при умножении (AC14, R9).** Произведение по цепочке превышает `Integer.MAX_VALUE` → `RequirementQuantityOverflowException` → HTTP 400 `quantity_overflow`; результат НЕ сохраняется.
- **N3 Переполнение при агрегации (AC14, R9).** Сумма одинаковых элементов превышает `Integer.MAX_VALUE` → 400 `quantity_overflow`; результат НЕ сохраняется.
- **N4 Чужой план (AC12, AC13).** Запуск расчёта для плана другой организации → 404 `not_found`, данные не раскрываются.
- **N5 Отсутствующий/чужой расчёт (AC12, AC15).** `GET .../calculations/{id}` для чужого/несуществующего расчёта в существующем плане → 404 `not_found`.
- **N6 Список для чужого/несуществующего плана (AC15, F1).** `GET .../calculations` для чужого/несуществующего плана → 404 `not_found` (не 200 с пустым списком).
- **N7 Dangling-ссылка на удалённую/чужую вложенную сборку (решение пользователя, F7).** Спецификация ссылается на удалённую/чужую сборку → расчёт прерывается ошибкой `specification_item_not_found` с идентификатором проблемной ссылки; частичный результат НЕ сохраняется (нет записи в БД).
- **N8 Удалённая головная сборка плана (edge).** План ссылается на soft-deleted сборку → расчёт даёт 404 `not_found` (`RequirementSourceItemNotFoundException`).
- **N9 Удалённый лист каталога (edge).** Строка ссылается на soft-deleted `PART` → 404 `not_found` (поведение, аналогичное `flat`).
- **N10 Один расчёт для чужого/несуществующего плана (AC15, F1).** `GET .../calculations/{calculationId}` для несуществующего/чужого плана → 404 `not_found` (план не найден/чужой, а не 200).

### 3.3 Boundary

- **B1 Количество == `Integer.MAX_VALUE` (без переполнения).** Умножение на 1/сложение без перехода → успех, количество точно `MAX_VALUE`.
- **B2 Переход границы `int`.** `MAX_VALUE` × 2 → 400 `quantity_overflow`.
- **B3 Одноуровневый план.** План A×1 без вложенности → A=1 (MANUFACTURE).
- **B4 Ромб (общая подсборка в двух ветвях).** A содержит B и C, B и C содержат D → D суммируется без ложного детекта цикла (путь, а не глобальный visited).
- **B5 Сумма достигает `MAX_VALUE`.** Агрегация доводит до `MAX_VALUE` → успех; `MAX_VALUE + 1` → 400.
- **B6 Глубокая цепочка.** A×2 → B×3 → C×4 → D×5 → корректное произведение по всем уровням.
- **B7 Несколько головных сборок с общей подсборкой.** Источники объединяются, количество суммируется (связь с P3/P12).

## 4. Acceptance mapping (traceability)

| Сценарий | AC | Требования (R) | OpenSpec-спека |
|---|---|---|---|
| P1 | AC1, AC2 | R1, R2, R3, R4 | planning-requirements: «Рекурсивное разузлование», «Расчёт количества», «Сборки как самостоятельные позиции» |
| P2 | (слабый AC) | R3, R10 | «Сборки как самостоятельные позиции», «Пустой план» |
| P3 | AC3 | R5, R13 | «Агрегация одинаковых элементов», «Источники позиции» |
| P4 | AC4 | R4 | «Вид потребности по типу элемента» |
| P5 | AC5 | R6 | «Поля позиции результата» |
| P6 | (из AC5) | R6 | «Единица измерения позиции» |
| P7 | AC6 | R7 | «Игнорирование заменителей» |
| P8 | AC8 | R10 | «Пустой план» |
| P9 | AC9 | R11, R13 | «Сохранение неизменяемого расчёта», «Источники позиции» |
| P10 | AC10 | R14 | «История расчётов» |
| P11 | AC11 | R12 | «Снапшот использованных данных» |
| P12 | (AC9) | R13 | «Источники позиции» |
| P13 | (design D5, D8) | R6 | — |
| P14 | (Assumption) | R4 | «Вид потребности по типу элемента» |
| N1 | AC7 | R8 | «Обнаружение циклов BOM» |
| N2 | AC14 | R9 | «Отклонение переполнения количества» |
| N3 | AC14 | R9 | «Отклонение переполнения количества» |
| N4 | AC12, AC13 | R15 | «Tenant-изоляция расчётов» |
| N5 | AC12, AC15 | R15, R17 | planning-requirements-rest: «REST ошибки расчётов» |
| N6 | AC15 | R17 | planning-requirements-rest: «REST чтения расчётов» («Список для чужого или несуществующего плана») |
| N7 | (решение пользователя, F7) | R1, R17 | planning-requirements: «Обработка отсутствующей ссылки спецификации»; planning-requirements-rest: «REST ошибки расчётов» |
| N8 | (edge) | R17 | planning-requirements-rest: «REST ошибки расчётов» |
| N9 | (edge) | R17 | planning-requirements-rest: «REST ошибки расчётов» (поведение листа — design D6, см. finding F8) |
| N10 | AC15 | R17 | planning-requirements-rest: «REST чтения расчётов» («Один расчёт для чужого или несуществующего плана») |
| B1–B6 | AC14, R9 | R2, R9 | «Расчёт количества», «Отклонение переполнения» |
| B7 | AC3 | R5, R13 | «Агрегация одинаковых элементов», «Источники позиции» |
| REST 201/200/409/400/404 | AC15, AC16 | R16, R17, R18 | planning-requirements-rest: все требования |
| `flat` совместимость | AC16 | R18 | planning-requirements-rest: «REST совместимость плоской структуры» |

## 5. Environment & data

- **Среда:** Maven, Java 25, Spring Boot 4.1.x; Testcontainers PostgreSQL (`postgres:17`, singleton по образцу `AbstractPostgresIntegrationTest`) с Flyway-миграциями до `V8__requirement_calculation.sql` включительно.
- **Зависимости для тестов:** JUnit 5, AssertJ, Mockito, Spring Modulith (`ApplicationModules.verify()`), MockMvc standalone; для JPA — Testcontainers + Spring Data JPA.
- **Тестовые данные (домен/application):** in-memory fakes (`InMemoryPlanRepository`, новый `InMemoryRequirementCalculationRepository`, fake `RequirementSource`), mock `OrganizationContext` с фиксированным `OrganizationId`, mock `Clock` с фиксированным `Instant`, `AssemblyId`/`PlanId`/`RequirementCalculationId` со случайными UUID.
- **Тестовые данные (BOM):** каталог из 5 типов (`Assembly`/`Part`/`Buy`/`Standart`/`Vzk`) с уникальными `number`/`name`; спецификации с многоуровневой вложенностью, общими подсборками, заменителями, циклом, dangling-ссылкой, количествами вплоть до `Integer.MAX_VALUE`.
- **Фикстуры (при необходимости):** SQL-фикстуры для JPA-тестов по образцу существующих, с двумя организациями для проверки изоляции.
- **Исходные точки:** `PlanApplicationServiceTest`, `PlanControllerTest`, `InMemoryPlanRepository`, `JpaPlanRepository`, `AbstractPostgresIntegrationTest`, `CatalogRestTenantContextTest`.

## 6. Risks & assumptions

**Assumptions (принятые на G2/G3):** материалы вне инкремента; лёгкий снапшот без версионирования;
результат — сохраняемый агрегат с историей; единица «шт»; VZK — непрозрачная закупка; заменители не
участвуют; REST-маршруты и модель хранения — зона Architect.

**Риски (и статус после правок):**

- **R-A Дублирование результата и промежуточного снапшота при чтении через `SpecificationService.get`** — N+1 запросов (включая `hasSubstitutes`, хотя заменители игнорируются). Принято (design Risks); не оптимизировать преждевременно.
- **R-B Переполнение только на `int`** — ограничение наследуется от существующих моделей; покрыто тестами N2/N3/B1/B2/B5; расширение типа — отдельное изменение.
- **R-C Dangling-ссылка → отдельная ошибка `specification_item_not_found`** — **закрыто (F7)**: зафиксировано решением пользователя (вариант A) в спеках/design/tasks; частичный результат не сохраняется.
- **R-D Консистентность снапшота при конкурентном изменении спецификации** — **закрыто (F4)**: `@Transactional` (READ COMMITTED) на application-сервисе объединяет чтение BOM и сохранение; чтения — read-only (design D10, tasks 4.2). Редкие гонки между параллельными правками и расчётом — вне scope.
- **R-E Порядок позиций после round-trip** — **закрыто (F3)**: сортировка при реконструкции агрегата (`number` → `type` → `id`) в design D8/D12, tasks 5.2/5.4.
- **R-F JSON-форма `sources`** — **закрыто (F5)**: зафиксирован массив объектов `{"value":"<uuid>"}` через штатную сериализацию `AssemblyId` (design D11, REST-спека «REST поля позиции расчёта»).

## 7. Entry/exit criteria

**Entry (условия начала реализации, G4→G5):**

- [x] Блокирующие находки закрыты: F1 (валидация плана в `list`/`get` → 404) и F7 (отдельная ошибка `specification_item_not_found` для dangling-ссылки, без частичного сохранения) отражены в spec/design/tasks.
- [x] Решение по dangling-ссылке (вариант A) отражено в AC/спеках/design.
- [x] Неблокирующие F3–F6 учтены в design/spec/tasks.
- [ ] Test plan утверждён пользователем (G4).

**Exit (G7 pass criteria):**

- [ ] Все сценарии P1–P14, N1–N10, B1–B7 выполнены автоматизированными тестами и прошли (`PASSED`).
- [ ] REST-контракт подтверждён: 201 (POST), 200 (GET list/get), 404 (чужой/отсутствующий план/расчёт, `specification_item_not_found`), 409 `bom_cycle`, 400 `quantity_overflow`.
- [ ] `GET /api/v1/plans/{id}/flat` регрессия зелёная (контракт не изменился).
- [ ] Tenant-изоляция подтверждена: чужие планы/расчёты недоступны (service + JPA + опционально REST-тест).
- [ ] `mvn test` зелёный, включая `ModuleVerificationTest` (`ApplicationModules.verify()`).
- [ ] Нет незакрытых дефектов уровня blocker/major.
- [ ] Если автоматизация невозможна — ручная проверка подтверждена пользователем (по G7).

**Fail-условия:** любой failed-тест сценариев P/N/B, нарушение multi-tenant, изменение контракта `flat`,
несохранение/перезапись истории, нестабильный снапшот, сохранение частичного результата при ошибке,
незакрытые blocker/major findings → возврат к Developer (после правок — повторный Reviewer и QA Testing).
