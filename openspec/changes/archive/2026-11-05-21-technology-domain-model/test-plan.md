# Test Plan — technology-domain-model (change 21)

Этап: QA Planning (G4). Документ — контракт верификации для этапа QA Testing (G7).
Исходные артефакты: `proposal.md`, `specs/technology-domain-model/spec.md`, `design.md`, `tasks.md`, утверждённые требования и Acceptance Criteria (G2).

---

## 1. Scope & goals

**Что тестируем.** Только доменный слой модуля `technology`: агрегат `TechnologyProcess`, value object `Operation`, типизированный идентификатор `TechnologyProcessId`, driven port `TechnologyProcessRepository`, чистый доменный сервис `TechnologyProcessService` и доменные исключения.

**Почему.** Инкремент фиксирует минимальную, но насыщенную доменную модель: fail-fast валидацию, упорядоченный набор операций, идемпотентный soft-delete, аудит без `createdBy`/`updatedBy`, кросс-агрегатные правила (уникальность номера в организации и инвариант дефолтности на деталь) и сохранение чистоты домена/границ модуля.

**Не тестируем (вне скоупа инкремента).** Application-сервисы, REST-контроллеры, JPA-сущности, Flyway-миграции, PostgreSQL, фронтенд, персистентность, привязку к `Assembly`, нормы/оборудование операций, переупорядочивание операций, перенос процесса на другую деталь, валидацию номера по паттерну организации. Для этих аспектов отсутствуют production-адаптеры, поэтому slice/e2e-уровни здесь неприменимы.

**Цель верификации.** Подтвердить, что каждый SHALL-сценарий спецификации реализован и покрыт автоматизированным тестом, домен остаётся чистым от инфраструктурных зависимостей, а `ApplicationModules.verify()` проходит.

---

## 2. Test levels

| Уровень | Применимость | Что покрывает |
|---|---|---|
| **Unit** (JUnit 5 + AssertJ, без Spring-контекста) | Основной уровень | `TechnologyProcessId`, `Operation`, `TechnologyProcess`, `TechnologyProcessService`, in-memory test double репозитория |
| **Reflection/architecture unit** (JUnit 5 + reflection, без Spring-контекста) | Применим | Форма порта репозитория, структура и аннотации агрегата, отсутствие `createdBy`/`updatedBy`, чистота домена от Spring/JPA/PostgreSQL/Flyway |
| **Module verification** (`ApplicationModules.verify()`, существующий `ModuleVerificationTest`) | Применим | Ацикличность графа модулей, отсутствие запрещённых зависимостей у `technology` |
| **Slice (web / persistence)** | **Неприменим** | Нет REST/JPA в этом инкременте |
| **Integration / e2e** | **Неприменим** | Нет application-слоя, БД, внешних интеграций в этом инкременте |

Стиль именования тестов — `methodName_condition_expectedBehavior` (согласно скиллу `testing-pyramid`). Стиль assertion и репозиторного test double — по образцу `CatalogEntityTest`, `CatalogRepositoryPortTest`, `InMemoryPartRepositoryTest`, `OrganizationsDomainPurityTest`.

---

## 3. Test scenarios

Обозначения исключений: `IAE` = `IllegalArgumentException`, `NPE` = `NullPointerException`. Идентификаторы сценариев `TS-*` используются в разделе трассируемости.

### 3.1 TechnologyProcessId (unit)

| ID | Тип | Сценарий (Given/When/Then) | Ожидаемый результат |
|---|---|---|---|
| TS-1.1 | positive | `generate()` вызван дважды | Оба `value` не-null; значения различны |
| TS-1.2 | positive | `of(uuid)` с заданным `UUID` | `value` равен переданному `UUID` |
| TS-1.3 | negative | `of(null)` | Отклоняется `NullPointerException` (конвенция `PartId`/`OrganizationId`; тип исключения зафиксировать тестом) |

### 3.2 Operation (unit)

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-2.1 | positive | `new Operation(5, "Сверление")` | `number()=5`, `name()="Сверление"` без изменений |
| TS-2.2 | boundary | `number` = 1 (минимум) | Принимается |
| TS-2.3 | boundary | `number` = 0, -1 | `IllegalArgumentException` |
| TS-2.4 | boundary | `number` = `Integer.MAX_VALUE` | Принимается |
| TS-2.5 | negative | `name` = `""`, `"   "`, `null` | `IllegalArgumentException` (null-имя также отклоняется через семантику `StringUtils.isBlank`; Finding 7 решён) |

### 3.3 TechnologyProcess — структура и валидация (unit + reflection)

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-3.1 | structure | reflection по `TechnologyProcess` | Есть `@Entity`; поле `id` помечено `@Identity`; поля `id`, `number`, `name`, `partId`, `organizationId`, `defaultProcess`, `createdAt`, `updatedAt`, `deletedAt`, коллекция операций присутствуют; типы `TechnologyProcessId`, `String`, `String`, `PartId`, `OrganizationId`, `boolean`, `Instant`, `Instant`, `Instant`, коллекция |
| TS-3.2 | positive | `create(...)` с валидными аргументами | Значения сохраняются; `defaultProcess=false`; `deletedAt=null`; коллекция операций пуста |
| TS-3.3 | negative | `create` с `number`/`name` = `null`, `""` или `"   "` | `IllegalArgumentException` (через семантику `StringUtils.isBlank`), сообщение идентифицирует поле `number` / `name` соответственно |
| TS-3.4 | negative | `create` с `null` для `id` / `partId` / `organizationId` / `createdAt` / `updatedAt` | `NullPointerException` (fail-fast, по образцу `Part`) |
| TS-3.5 | structure | reflection по полям аудита | Отсутствуют поля `createdBy` / `updatedBy` (только `createdAt` / `updatedAt` / `deletedAt`) |
| TS-3.6 | positive | `rename("Новое имя")` | `name` обновлён, `updatedAt` не раньше прежнего |
| TS-3.7 | negative | `rename("   ")`, `rename(null)` | `IllegalArgumentException`, `name` не изменён |
| TS-3.8 | positive | `markDeleted()` у активного процесса | `deletedAt` становится не-null, `updatedAt` обновляется |
| TS-3.9 | positive | `markDeleted()` повторно | Исходный `deletedAt` не изменяется, `updatedAt` не обновляется (идемпотентный no-op) |
| TS-3.10 | negative | `create` с `partId=null` отдельно проверяется на неизменяемость | `partId` неизменяем после создания: отсутствует мутатор `partId`, поле `final` (reflection-проверка отсутствия setter/поля не-final) |
| TS-3.11 | structure | reflection по модели | Дублирующие поля номера/имени детали (`partNumber`, `partName`) отсутствуют |

### 3.4 TechnologyProcess — операции (unit)

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-3.12 | positive | `addOperation(20, "А")`, затем `addOperation(10, "Б")` | Обе операции в наборе; `operations()` возвращает порядок [10, 20] |
| TS-3.13 | positive | `operations().add(...)` | `UnsupportedOperationException` — коллекция неизменяема снаружи |
| TS-3.14 | negative | `addOperation` с уже занятым номером | `DuplicateOperationException` |
| TS-3.15 | negative | `addOperation(0, ...)` / `addOperation(-1, ...)` | `IllegalArgumentException` |
| TS-3.16 | negative | `addOperation(1, "   ")` / `addOperation(1, null)` | `IllegalArgumentException` |
| TS-3.17 | positive | `removeOperation(10)` у существующей операции | Операция удалена из набора |
| TS-3.18 | negative | `removeOperation(99)` у отсутствующей операции | `OperationNotFoundException` |
| TS-3.19 | boundary | «дырки» в нумерации (10, 30 без 20) | Допускаются; порядок остаётся по возрастанию `number` |
| TS-3.20 | positive | мутаторы `addOperation`/`removeOperation` | Обновляют `updatedAt` (требование аудита `updatedAt` мутаторов; Finding 2 решён) |

### 3.5 TechnologyProcessService — уникальность номера и инвариант дефолтности (unit)

Используется in-memory test double `InMemoryTechnologyProcessRepository` (tenant-scoped, исключает удалённые).

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-4.1 | positive | `register` единственного (не-дефолтного) процесса | Сохраняется без ошибки |
| TS-4.2 | negative | `register` с номером, занятым активным процессом той же организации | `DuplicateTechnologyProcessNumberException`, процесс не сохранён |
| TS-4.3 | positive | `register` с тем же номером в **другой** организации | Допускается (уникальность в пределах организации) |
| TS-4.4 | boundary | `register` с номером ранее **логически удалённого** процесса той же организации | Допускается (уникальность распространяется только на активные) |
| TS-4.5 | negative | `register` не-дефолтного процесса для детали, у которой уже есть процесс и ни один не дефолтный | `InvalidDefaultProcessStateException` |
| TS-4.6 | positive | Полный валидный путь: `register(P1)` → `markAsDefault(P1)` → `register(P2, не-дефолтный)` | Инвариант выполнен: ровно один дефолтный (`P1`), второй процесс зарегистрирован |
| TS-4.7 | positive | `markAsDefault(P2)`, когда `P1` уже дефолтный у той же детали | `P1` перестаёт быть дефолтным, `P2` становится дефолтным; состояния с двумя дефолтными не возникает |
| TS-4.8 | negative | `markAsDefault(id)` при отсутствии процесса | `TechnologyProcessNotFoundException` |
| TS-4.9 | boundary | `markAsDefault(id, orgB)` для процесса организации `orgA` | `TechnologyProcessNotFoundException` (tenant-изоляция: чтение ограничено организацией) |
| TS-4.10 | negative | `unmarkDefault` единственного дефолтного процесса детали, у которой есть ещё активный процесс | `InvalidDefaultProcessStateException` |
| TS-4.11 | positive | `unmarkDefault` у единственного процесса детали | Допускается (Q4: единственный процесс может быть не-дефолтным) |
| TS-4.12 | positive | `unmarkDefault` у процесса, который уже не дефолтный | Идемпотентный no-op: `defaultProcess` остаётся `false`, состояние и `updatedAt` не изменяются (Finding 5 решён) |
| TS-4.13 | positive | `markAsDefault` у процесса, который уже дефолтный (повторный вызов) | Идемпотентный no-op: `defaultProcess` остаётся `true`, прочие процессы и `updatedAt` не изменяются (Finding 5 решён) |
| TS-4.14 | risk | Soft-delete дефолтного процесса, затем `register` нового процесса | Out-of-scope (Finding 6 решён — принятый edge case): `register`-guard может блокировать добавление нового процесса после удаления дефолтного; семантика дефолтности при удалении отложена на future change |
| TS-4.15 | positive | `markAsDefault`/`markAsNotDefault` фактически меняют `defaultProcess` | `updatedAt` обновляется (требование аудита `updatedAt` мутаторов) |

### 3.6 Порт репозитория и test double (unit + reflection)

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-5.1 | structure | reflection по `TechnologyProcessRepository` | Интерфейс в `technology.domain`, без аннотаций; методы `save`, `findById(id, orgId)`, `findByPartId(partId, orgId)`, `findAll(orgId)`, `existsByNumber(number, orgId)`; метод `delete` отсутствует |
| TS-5.2 | positive | `InMemoryTechnologyProcessRepository.findById` | Возвращает только активный процесс той же организации; для чужой организации и удалённого — `Optional.empty` |
| TS-5.3 | positive | `findByPartId` | Возвращает только активные процессы заданной детали и организации; чужие и удалённые исключены |
| TS-5.4 | positive | `findAll` | Возвращает активные процессы организации, отсортированные (по конвенции проекта) |
| TS-5.5 | positive | `existsByNumber` | `true` только для активного процесса той же организации; удалённый/чужая организация — `false` |

### 3.7 Чистота домена и границы модуля (reflection + module verification)

| ID | Тип | Сценарий | Ожидаемый результат |
|---|---|---|---|
| TS-6.1 | structure | `TechnologyDomainPurityTest` по типам `technology.domain` | Только jMolecules-аннотации; отсутствуют импорты/аннотации Spring, JPA, PostgreSQL, Flyway; порт — annotation-free интерфейс |
| TS-6.2 | structure | `ModuleVerificationTest` (`ApplicationModules.verify()`) | Проходит; 10 модулей; граф ацикличен; у `technology` нет запрещённых зависимостей |
| TS-6.3 | structure | Проверка импортов `technology` | Ссылки только на `catalog.api.PartId` и `shared.api.OrganizationId`; отсутствуют `catalog.domain`, `catalog.infrastructure` и любые production-типы других модулей |
| TS-6.4 | structure | Проверка состава модуля | Отсутствуют application-сервисы, REST-контроллеры, JPA-сущности, Flyway-миграции, фронтенд-код для `technology` |

---

## 4. Acceptance mapping (traceability)

Требования спецификации (`spec.md`) ↔ сценарии ↔ Acceptance Criteria (G2).

| Требование спецификации | Сценарии тест-плана | AC (G2) |
|---|---|---|
| R1. Агрегат технологического процесса | TS-3.1, TS-3.11 | AC-1 |
| R2. TechnologyProcessId | TS-1.1–TS-1.3 | AC-2 |
| R3. Operation как value object | TS-2.1–TS-2.5 | AC-3 |
| R4. Привязка к детали каталога | TS-3.2, TS-3.10, TS-3.11 | AC-1, AC-17 |
| R5. Валидация обязательных атрибутов процесса | TS-3.3, TS-3.4 | AC-4 |
| R6. Уникальность номера в организации | TS-4.1–TS-4.4 | AC-13 |
| R7. Организация и аудит | TS-3.2, TS-3.4, TS-3.5 | AC-5 |
| R8. Логическое удаление | TS-3.8, TS-3.9 | AC-8 |
| R9. Переименование | TS-3.6, TS-3.7 | AC-9 |
| R10. Добавление операции | TS-3.12, TS-3.14–TS-3.16 | AC-3, AC-6 |
| R11. Порядок операций | TS-3.12, TS-3.13, TS-3.19 | AC-6 |
| R12. Удаление операции | TS-3.17, TS-3.18 | AC-7 |
| R13. Инвариант дефолтного процесса | TS-4.5–TS-4.13, TS-4.15 (TS-4.14 — out-of-scope) | AC-10, AC-11, AC-12 |
| R14. Порт репозитория | TS-5.1–TS-5.5 | AC-14 |
| R15. Доменная чистота | TS-6.1 | AC-15 |
| R16. Границы модуля technology | TS-6.2, TS-6.3 | AC-16 |
| R17. Минимальность инкремента | TS-6.4 | AC-17 |
| R18. Обновление `updatedAt` всеми мутаторами | TS-3.6, TS-3.8, TS-3.9, TS-3.20, TS-4.12, TS-4.13, TS-4.15 | AC-6, AC-7, AC-8, AC-9, AC-10, AC-11, AC-12 |
| R19. Автоматизированные тесты домена | Весь набор + `mvn test` | AC-18 |

> **Примечание по трассируемости.** AC-16 (G2) ссылается на «граф девяти модулей»; фактически проект сейчас содержит 10 модулей (`organizations`, `adminpanel`, `technology`, `specifications`, `catalog`, `directories`, `shared`, `security`, `web`, `planning` — см. `ModuleVerificationTest.EXPECTED_MODULES`). Тест-план и `TS-6.2` используют актуальное значение «10 модулей»; сама проверка `ApplicationModules.verify()` не зависит от точного числа. Рекомендуется при следующей ревизии AC-16 (G2) выровнять формулировку до «10 модулей».

---

## 5. Environment & data

- **Среда:** Java 25, Maven, Spring Boot 4.1.x (для modularity-проверки используется Spring Modulith core, полный Spring-контекст не требуется).
- **Зависимости:** JUnit 5, AssertJ; доменные unit-тесты без Spring-контекста.
- **База данных:** не требуется. Персистентность вне скоупа.
- **Test double:** `InMemoryTechnologyProcessRepository` в `src/test/.../technology/infrastructure` (только тестовая инфраструктура, не production; tenant-scoped; исключает удалённые записи).
- **Данные:** фиксированные `Instant` (`Instant.parse("2026-01-01T00:00:00Z")` и т.п.), `OrganizationId.generate()` для разных организаций, `PartId.generate()` для разных деталей, номера операций с «дырками» (10, 20, 30) для проверки порядка.
- **Команды проверки:** `mvn -q -Dtest=TechnologyProcessIdTest test`, `mvn -q -Dtest=OperationTest test`, `mvn -q -Dtest=TechnologyProcessTest test`, `mvn -q -Dtest=TechnologyProcessRepositoryPortTest test`, `mvn -q -Dtest=TechnologyProcessServiceTest test`, `mvn -q -Dtest=TechnologyDomainPurityTest test`, `mvn -q -Dtest=ModuleVerificationTest test`, итогово `mvn -q test`.

---

## 6. Risks & assumptions

**Риски (технические/надёжностные):**

1. **Кросс-агрегатные инварианты не атомарны; уникальность без storage-гаранта.** Уникальность номера и дефолтность принуждаются в `TechnologyProcessService` без транзакций; между `existsByNumber` и `save` есть окно гонки (TOCTOU). В in-memory/unit-уровне это не проявляется. При появлении персистентности обязателен частичный уникальный индекс `(organization_id, number) WHERE deleted_at IS NULL` плюс транзакция. — Принятый риск этого инкремента (Finding 3), зафиксирован как обязательное условие для future change.
2. **`register` выглядит как use case.** Чистота домена проверяется тестом `TS-6.1`; при появлении application-слоя он должен делегировать в доменный сервис, а не дублировать правила.
3. **Package-private мутаторы дефолтности.** Прямые вызовы `markAsDefault`/`markAsNotDefault` ограничены доменом; прикладной код обязан идти через сервис. Проверяется размещением тестов в пакете `technology.domain` и покрытием сервиса.
4. **Неизменяемый `partId`.** Исключает перенос процесса на другую деталь — зафиксировано допущением, не риск текущего инкремента.
5. **Семантика при soft-delete дефолтного процесса** (см. Finding 6) — решена: out-of-scope, принятый известный edge case; `register`-guard может блокировать добавление нового процесса после удаления дефолтного.

**Допущения (Assumptions):**

- Инкремент = доменный слой (по образцу `catalog-domain-model`).
- Операции — value objects без собственного ID; идентифицируются номером внутри процесса.
- `defaultProcess` при создании всегда `false`; дефолтность — только явным доменным действием.
- «Дырки» в нумерации операций допустимы; переупорядочивание вне скоупа.
- Валидация номера по паттерну организации к техпроцессам в этом инкременте не применяется.
- `Assembly` не участвует; привязка только к `Part`.

---

## 7. Entry/exit criteria

**Entry (условия входа в реализацию/верификацию):**

- G2 выполнено: требования и Acceptance Criteria утверждены пользователем.
- G3 выполнено: `proposal.md`, `spec.md`, `design.md`, `tasks.md` готовы и непротиворечивы.
- G4: настоящий test-plan утверждён пользователем; findings из раздела ниже либо закрыты, либо явно приняты.

**Exit (критерии прохождения G7):**

- [ ] Все сценарии `TS-*`, кроме явно согласованного как out-of-scope `TS-4.14`, реализованы и выполнены.
- [ ] `mvn -q test` завершается успешно (все тесты проекта, включая существующие).
- [ ] `ModuleVerificationTest` (ApplicationModules.verify) проходит.
- [ ] Доменные unit-тесты technology проходят без Spring-контекста и без БД.
- [ ] Дефектов нет; либо найденные дефекты исправлены и перепроверены.
- [ ] Идемпотентные сценарии `TS-4.12`/`TS-4.13` определены (no-op без изменения состояния и `updatedAt`) и покрыты тестами; `TS-4.14` (soft-delete дефолтного процесса) явно зафиксирован как out-of-scope (согласованное отложенное решение).

---

## Review findings (на решение пользователя / Architect)
> **Статус:** все findings F1–F7 закрыты утверждёнными пользователем решениями (см. «Принятые решения» ниже); блокирующих findings не осталось. Документ сохранён как журнал оценки.

### Finding 1 (блокер консистентности): `technology` зависит от `shared`, но в `allowedDependencies` нет `shared :: shared`

Модель использует `shared.api.OrganizationId` (D1, R7, R16), а существующая декларация модуля `technology` разрешает только `catalog :: catalog` и `directories :: directories`. Все остальные модули (`catalog`, `planning`, `specifications`, `organizations`, `security`), использующие `OrganizationId`, явно указывают `shared :: shared`.

- **Следствие:** `ApplicationModules.verify()` завершится нарушением для `technology -> shared`; AC-16 и TS-6.2 не пройдут.
- **Противоречие в артефактах:** спецификация одновременно требует «обращаться к организации через `shared.api.OrganizationId`» и «существующая декларация SHALL сохраняться без новых межмодульных зависимостей»; задача 6.2 требует «ссылается только на `catalog.api.PartId` и `shared.api.OrganizationId` и не добавляет межмодульных зависимостей» — эти формулировки несовместимы, т.к. ссылка на `shared` и есть новая межмодульная зависимость относительно текущего скелета.
- **Рекомендация:** обновить `technology/package-info.java`, добавив `shared :: shared` в `allowedDependencies`, и скорректировать формулировку R16/task 6.2 (сохраняется каталог/справочники; добавляется разрешённая зависимость `shared`). Это техническая правка Architect, но требует подтверждения, т.к. отменяет буквальное требование «сохранять существующую декларацию».

### Finding 2 (средний): D7 расширяет аудит-семантику `updatedAt` за пределы спецификации

D7 обязывает **все** мутаторы (`addOperation`, `removeOperation`, `markAsDefault`, `markAsNotDefault`) обновлять `updatedAt`, тогда как спецификация явно требует это только для `rename`/`markDeleted`. Расхождение создаёт риск рассинхрона между реализацией и верификацией (G7).

- **Рекомендация:** либо добавить явные сценарии/AC для `updatedAt` на операциях и мутаторах дефолтности, либо ослабить D7 до требуемых спецификацией мутаторов. Нужно решение Architect.

### Finding 3 (средний, перспективный): уникальность номера не имеет storage-уровневого гаранта

В этом инкременте уникальность принуждается только `existsByNumber` в сервисе (без БД). При появлении персистентности обязателен частичный уникальный индекс `(organization_id, number) WHERE deleted_at IS NULL` плюс транзакция, иначе возможна гонка.

- **Рекомендация:** зафиксировать в будущем change `persistence` для technology как обязательное условие; в текущем change — принять как известный риск (тест `TS-4.2` проверяет только последовательную семантику).

### Finding 4 (низкий): `register` получает только не-дефолтные процессы (D4), часть guard'а недостижима через публичный API

`defaultProcess` при создании всегда `false`, а дефолтность устанавливается только `markAsDefault`. Значит ветка `register` для «процесс уже дефолтный» практически недостижима; инвариант «не более одного дефолтного» реально принуждается только в `markAsDefault` (TS-4.7), а не в `register`.

- **Рекомендация:** явно отразить это в design (guard в `register` — защитная логика) и не пытаться тестировать недостижимый путь «регистрация дефолтного при существующем дефолтном». Дополнительно добавить положительный сценарий полного валидного пути `TS-4.6`.

### Finding 5 (низкий): не определена семантика идемпотентных/повторных вызовов `markAsDefault` и `unmarkDefault`

Design не задаёт поведение `markAsDefault` для уже дефолтного процесса и `unmarkDefault` для уже не-дефолтного (no-op vs ошибка).

- **Рекомендация:** зафиксировать (рекомендуется идемпотентный no-op для обоих, по аналогии с `markDeleted`) и добавить сценарии `TS-4.12`/`TS-4.13`.

### Finding 6 (средний, отложенный edge case): soft-delete дефолтного процесса ломает `register`-guard

Если у детали два процесса (`P1` дефолтный, `P2` нет) и `P1` логически удаляется, активным остаётся только не-дефолтный `P2`. Последующая регистрация нового не-дефолтного процесса будет отклонена guard'ом («есть активные и ни один не дефолтный»), хотя пользователь может законно добавить процесс и затем сделать его дефолтным. Design явно откладывает «семантику дефолтности при удалении дефолтного процесса».

- **Рекомендация:** явно зафиксировать как out-of-scope (принято) с последующим change, но добавить в тест-план пометку; если это нежелательно — доработать правило `register` (например, допускать добавление, но требовать последующего `markAsDefault` до следующей регистрации). Требует решения.

### Finding 7 (низкий): null-имя у `Operation` и тип исключения для null-ID

Требование 3 (G2) включает отклонение `null`-имени операции, тогда как spec-сценарий покрывает только пустое/пробельное. Для `TechnologyProcessId.of(null)` тип исключения в требованиях не уточнён (конвенция `PartId` — `NullPointerException`).

- **Рекомендация:** зафиксировать в тестах и, при необходимости, уточнить spec: `null`-имя операции → `IllegalArgumentException` (через `StringUtils.isBlank`), `of(null)` → `NullPointerException`.


### Принятые решения (утверждены пользователем)

- **F1:** `technology/package-info.java` дополняется `shared :: shared`; итоговый список `allowedDependencies` — `catalog :: catalog`, `directories :: directories`, `shared :: shared`. Обновлены `spec.md` (R16), `proposal.md` (Impact), `tasks.md` (6.2).
- **F2:** добавлено требование «Аудит-семантика `updatedAt` мутаторов» с явными сценариями; каждый мутатор обновляет `updatedAt` при фактическом изменении состояния; D7 согласован.
- **F3:** принят известный риск: уникальность принуждается только доменным сервисом; при появлении персистентности обязателен частичный уникальный индекс `(organization_id, number) WHERE deleted_at IS NULL` + транзакция (отражено в `design.md` Risks и `proposal.md` Non-goals).
- **F4:** в D9 отражено, что ветка `register` для «процесс уже дефолтный» — защитная логика, недостижимая через публичный API (D4); реальное принуждение — в `markAsDefault`.
- **F5:** идемпотентный no-op: `markAsDefault` (уже дефолтный) и `unmarkDefault` (уже не-дефолтный); отражено в D9, R13, task 5.3; сценарии TS-4.12/TS-4.13 стали определёнными.
- **F6:** out-of-scope: семантика дефолтности при soft-delete дефолтного процесса отложена на future change; `register`-guard может блокировать добавление после удаления дефолтного — принятый edge case (отражено в `design.md` Open Questions/Risks и `proposal.md` Non-goals).
- **F7:** `null`-имя `Operation` → `IllegalArgumentException` (через `StringUtils.isBlank`); `TechnologyProcessId.of(null)` → `NullPointerException`; отражено в `spec.md` (R2/R3) и `tasks.md` (1.2/2.2).
