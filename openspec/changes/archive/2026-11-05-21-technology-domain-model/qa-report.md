# QA Report — change `21-technology-domain-model` (G7)

**Status: PASSED**

Верификация фактической реализации доменного слоя модуля `technology` против утверждённого
Test plan (`openspec/changes/21-technology-domain-model/test-plan.md`), требований/AC (G2) и
OpenSpec `spec.md`.

---

## 1. Environment & executed commands

- Ветка: `technology-domain-model`.
- JDK: `JAVA_HOME=/home/yury/.jdks/corretto-25.0.3` (Corretto 25.0.3 — корректная JDK проекта;
  дефолтный shell-`java` — Corretto 27, порождает ArchUnit warning, поэтому не использовался).
- Spring Modulith: `2.1.0`.

| Команда | Результат |
|---|---|
| `JAVA_HOME=/home/yury/.jdks/corretto-25.0.3 ./mvnw -q -DskipTests compile` | ✅ exit 0 |
| `JAVA_HOME=/home/yury/.jdks/corretto-25.0.3 ./mvnw -q test` | ✅ exit 0 (весь проект) |
| `JAVA_HOME=/home/yury/.jdks/corretto-25.0.3 ./mvnw -Dtest='TechnologyProcessIdTest,OperationTest,TechnologyProcessTest,TechnologyProcessRepositoryPortTest,TechnologyProcessServiceTest,TechnologyDomainPurityTest,InMemoryTechnologyProcessRepositoryTest,ModuleVerificationTest' test` | ✅ BUILD SUCCESS |

Агрегированный итог surefire по полному прогону: **suites=64, tests=415, failures=0,
errors=0, skipped=0**.

Количество тестов пакета `technology` (подтверждено фактически):

| Класс | Тестов |
|---|---|
| TechnologyProcessIdTest | 3 |
| OperationTest | 5 |
| TechnologyProcessTest | 24 |
| TechnologyProcessRepositoryPortTest | 1 |
| TechnologyProcessServiceTest | 14 |
| TechnologyDomainPurityTest | 3 |
| InMemoryTechnologyProcessRepositoryTest | 4 |
| **Итого technology** | **54** |
| ModuleVerificationTest | 1 |

Заявленные dev-implementer'ом цифры (54 теста technology, все зелёные, весь проект проходит)
**подтверждены**.

---

## 2. Traceability Test plan → фактические тесты

Все функциональные сценарии имеют выделенный автоматизированный тест и проходят.

### TS-1.x — TechnologyProcessId (TechnologyProcessIdTest)
| Сценарий | Тест | Результат |
|---|---|---|
| TS-1.1 generate() дважды различны/не-null | `generate_returnsDistinctNonNullUuids` | ✅ |
| TS-1.2 of(uuid) сохраняет значение | `of_preservesValue` | ✅ |
| TS-1.3 of(null) → NPE | `of_rejectsNull` | ✅ |

### TS-2.x — Operation (OperationTest)
| Сценарий | Тест | Результат |
|---|---|---|
| TS-2.1 валидная операция сохраняет значения | `constructor_withValidValues_preservesThem` | ✅ |
| TS-2.2 number=1 (минимум) | `constructor_withMinimalNumber_accepts` | ✅ |
| TS-2.3 number=0/-1 → IAE | `constructor_withNonPositiveNumber_rejects` | ✅ |
| TS-2.4 number=Integer.MAX_VALUE | `constructor_withMaxNumber_accepts` | ✅ |
| TS-2.5 name=""/"   "/null → IAE | `constructor_withBlankOrNullName_rejects` | ✅ |

### TS-3.x — TechnologyProcess (TechnologyProcessTest)
| Сценарий | Тест | Результат |
|---|---|---|
| TS-3.1 структура (@Entity/@Identity/поля/типы) | `structure_hasEntityIdentityAndRequiredFields` | ✅ |
| TS-3.2 create с валидными аргументами + дефолты | `create_withValidArguments_savesValuesAndDefaults` | ✅ |
| TS-3.3 number/name null/blank → IAE с указанием поля | `create_withBlankNumberOrName_rejectsAndIdentifiesField` | ✅ |
| TS-3.4 null id/partId/orgId/createdAt/updatedAt → NPE | `create_withNullRequiredReferences_rejects` | ✅ |
| TS-3.5 отсутствуют createdBy/updatedBy | `auditFields_haveNoUserIdentifiers` | ✅ |
| TS-3.6 rename обновляет name+updatedAt | `rename_withValidName_updatesNameAndUpdatedAt` | ✅ |
| TS-3.7 rename blank/null → IAE, name не меняется | `rename_withBlankOrNullName_rejectsAndKeepsName` | ✅ |
| TS-3.8 markDeleted у активного | `markDeleted_onActiveProcess_setsDeletedAtAndUpdatesUpdatedAt` | ✅ |
| TS-3.9 markDeleted повторно (идемпотентность) | `markDeleted_repeatedly_isIdempotent` | ✅ |
| TS-3.10 partId final / нет мутатора | `partId_isFinalAndHasNoMutator` | ✅ |
| TS-3.11 нет partNumber/partName | `structure_hasNoDuplicatedPartFields` | ✅ |
| TS-3.12 addOperation(20,10) → [10,20] | `operations_addedOutOfOrder_areSortedByNumber` | ✅ |
| TS-3.13 operations() неизменяема | `operations_returnedList_isUnmodifiable` | ✅ |
| TS-3.14 дубликат номера → DuplicateOperationException | `addOperation_withDuplicateNumber_rejects` | ✅ |
| TS-3.15 number=0/-1 → IAE | `addOperation_withNonPositiveNumber_rejects` | ✅ |
| TS-3.16 name blank/null → IAE | `addOperation_withBlankOrNullName_rejects` | ✅ |
| TS-3.17 removeOperation существующей | `removeOperation_existing_removesIt` | ✅ |
| TS-3.18 removeOperation отсутствующей → OperationNotFoundException | `removeOperation_missing_rejects` | ✅ |
| TS-3.19 «дырки» 10/30, порядок по возрастанию | `operations_withNumberGaps_keepAscendingOrder` | ✅ |
| TS-3.20 add/remove обновляют updatedAt | `addAndRemoveOperation_updateUpdatedAt` | ✅ |

### TS-4.x — TechnologyProcessService (TechnologyProcessServiceTest)
| Сценарий | Тест | Результат |
|---|---|---|
| TS-4.1 register единственного не-дефолтного | `register_singleNonDefaultProcess_saves` | ✅ |
| TS-4.2 дубликат номера в той же организации → не сохранён | `register_duplicateNumberInSameOrganization_rejectsAndDoesNotSave` | ✅ |
| TS-4.3 тот же номер в другой организации — допускается | `register_sameNumberInOtherOrganization_allowed` | ✅ |
| TS-4.4 номер удалённого процесса — допускается | `register_numberOfDeletedProcessInSameOrganization_allowed` | ✅ |
| TS-4.5 второй не-дефолтный для детали → InvalidDefaultProcessStateException | `register_secondNonDefaultProcessForPart_rejects` | ✅ |
| TS-4.6 полный путь register→markAsDefault→register | `registerThenDefaultThenSecondProcess_keepsInvariant` | ✅ |
| TS-4.7 markAsDefault переключает без двух дефолтных | `markAsDefault_switchesDefaultWithoutTwoDefaults` | ✅ |
| TS-4.8 markAsDefault отсутствующего → TechnologyProcessNotFoundException | `markAsDefault_missingProcess_rejects` | ✅ |
| TS-4.9 markAsDefault чужой организации → not found (tenant) | `markAsDefault_processOfOtherOrganization_rejects` | ✅ |
| TS-4.10 unmarkDefault единственного дефолтного при другом активном → reject | `unmarkDefault_soleDefaultWithAnotherActiveProcess_rejects` | ✅ |
| TS-4.11 unmarkDefault единственного процесса — допускается | `unmarkDefault_singleProcess_allowed` | ✅ |
| TS-4.12 unmarkDefault уже не-дефолтного → no-op | `unmarkDefault_alreadyNotDefault_isNoOp` | ✅ |
| TS-4.13 markAsDefault уже дефолтного → no-op | `markAsDefault_alreadyDefault_isNoOp` | ✅ |
| TS-4.14 soft-delete дефолтного → out-of-scope (согласовано) | не тестируется | ⚪ |
| TS-4.15 markAsDefault/markAsNotDefault обновляют updatedAt | `defaultMutators_updateUpdatedAtWhenStateChanges` | ✅ |

### TS-5.x — порт и test double (TechnologyProcessRepositoryPortTest + InMemoryTechnologyProcessRepositoryTest)
| Сценарий | Тест | Результат |
|---|---|---|
| TS-5.1 структура порта (domain, annotation-free, методы, без delete) | `repositoryPort_hasOnlyCrudMethodsAndNoDelete` | ✅ |
| TS-5.2 findById: активный/та же организация; чужой/удалённый → empty | `findById_returnsOnlyActiveProcessOfOrganization` | ✅ |
| TS-5.3 findByPartId: только активные детали+организации | `findByPartId_returnsOnlyActiveProcessesOfPartAndOrganization` | ✅ |
| TS-5.4 findAll: активные организации, сортировка | `findAll_returnsActiveProcessesOfOrganizationSortedByNumber` | ✅ |
| TS-5.5 existsByNumber: только активный той же организации | `existsByNumber_checksOnlyActiveProcessesOfOrganization` | ✅ |

### TS-6.x — чистота домена и границы модуля
| Сценарий | Покрытие | Результат |
|---|---|---|
| TS-6.1 чистота домена | `TechnologyDomainPurityTest` (3 теста: jMolecules-only на агрегате; port annotation-free; domain-типы без инфраструктурных аннотаций) | ✅ (с замечанием M2) |
| TS-6.2 границы модуля | `ModuleVerificationTest` (`ApplicationModules.verify()`: 10 модулей, ациклично, у technology только разрешённые зависимости) | ✅ |
| TS-6.3 импорты technology | Нет выделенного теста. Свойство **фактически выполнено** (см. §3): импорты только `catalog.api.PartId`, `shared.api.OrganizationId`, `technology.api.*`, JDK, `org.apache.commons.lang3`, `org.jmolecules.ddd.annotation`. Кросс-модульный аспект (`catalog.domain`/`catalog.infrastructure` запрещены) принуждается `ApplicationModules.verify()` через named-интерфейсы. | ✅ (ручная проверка; см. M1) |
| TS-6.4 состав модуля | Нет выделенного теста. Свойство **фактически выполнено**: в `technology/application` и `technology/infrastructure` только `package-info.java` (нет application-сервисов/REST/JPA/Flyway/фронтенда). | ✅ (ручная проверка; см. M1) |

---

## 3. Ручная (статическая) проверка ключевых структурных свойств

- **TS-6.3 (импорты).** `grep '^import' src/main/java/.../technology/**`:
  ```
  com.hydroyura.forgeflow.catalog.api.PartId
  com.hydroyura.forgeflow.shared.api.OrganizationId
  com.hydroyura.forgeflow.technology.api.TechnologyProcessId
  java.time.Instant; java.util.*; java.util.UUID
  org.apache.commons.lang3.StringUtils
  org.jmolecules.ddd.annotation.Entity/Identity
  ```
  Отсутствуют Spring/JPA/PostgreSQL/Flyway, `catalog.domain`, `catalog.infrastructure` и
  production-типы других модулей. ✅
- **TS-6.1 (чистота домена).** Импорты доменных типов не содержат Spring/JPA/PostgreSQL/Flyway
  (см. выше). ✅
- **TS-6.4 (минимальность).** В `technology/application` и `technology/infrastructure`
  production-код отсутствует (только `package-info.java`). ✅
- **TS-6.2 (декларация модуля).** `technology/package-info.java`:
  `allowedDependencies = {"catalog :: catalog", "directories :: directories", "shared :: shared"}`;
  `technology/api/package-info.java` помечен `@NamedInterface("technology")`. ✅

---

## 4. Оценка minor-замечаний reviewer (M1–M3)

### M1 — TS-6.3/TS-6.4 не имеют выделенных автотестов
**Подтверждено.** Выделенных тестов для TS-6.3 (импорты) и TS-6.4 (состав модуля) нет.
- TS-6.3 частично принуждается `ModuleVerificationTest` (Spring Modulith запрещает обращение к
  внутренним пакетам других модулей мимо named-интерфейсов).
- Оба свойства **фактически выполнены** в коде (проверено в §3 вручную).
- **Влияние на G7: не блокирует.** Функциональные и архитектурные свойства корректны; пробел —
  только в явности покрытия. Рекомендуется (не обязательно) в будущем добавить небольшой
  reflection/import-тест для TS-6.3/TS-6.4, чтобы покрытие было буквально зафиксировано.

### M2 — purity-тест проверяет аннотации, а не импорты
**Подтверждено.** `TechnologyDomainPurityTest` проверяет `getAnnotations()`, а не `import`-ы.
- Это **точно совпадает** с конвенцией проекта (`OrganizationsDomainPurityTest` — та же схема).
- Фактические импорты домена чисты (проверено grep'ом в §3).
- **Влияние на G7: не блокирует.** Расхождение с буквальной формулировкой test-plan (`импорты/аннотации`)
  носит стилистический характер; реальная чистота домена соблюдена.

### M3 — хрупкость тестов по часам
**Подтверждено.** `TechnologyProcessTest`/`TechnologyProcessServiceTest` используют фиксированные
`Instant.parse("2026-…")` и ассерты `isAfter(UPDATED)`/`isAfterOrEqualTo(UPDATED)`, полагаясь на
системные часы позже 2026-01-02.
- Паттерн **наследуется** из `CatalogEntityTest` (проектная конвенция).
- На текущем окружении (системная дата 2026-11-05) тесты зелёные.
- **Влияние на G7: не блокирует.** Не является новым дефектом изменения.

---

## 5. Confirmed defects

**Блокирующих и major-дефектов не обнаружено.**

Единственное расхождение с буквальным текстом Test plan — отсутствие выделенных автотестов для
структурных сценариев **TS-6.3** и **TS-6.4** (minor, покрытие/стилистика; свойства фактически
выполнены). Отчёт dev-implementer «реализованы все сценарии, кроме TS-4.14» формально неточен
именно в части TS-6.3/TS-6.4.

---

## 6. Exit criteria (G7)

- [x] Все сценарии `TS-*`, кроме согласованного out-of-scope `TS-4.14`, реализованы и выполнены
      (TS-6.3/TS-6.4 — выполнены в коде и проверены статически; см. M1).
- [x] `mvn -q test` завершается успешно (415 тестов, 0 failures/errors/skipped).
- [x] `ModuleVerificationTest` (`ApplicationModules.verify()`) проходит.
- [x] Доменные unit-тесты technology проходят без Spring-контекста и без БД.
- [x] Дефектов нет; найденные замечания — minor, не блокируют.
- [x] Идемпотентные сценарии `TS-4.12`/`TS-4.13` определены и покрыты; `TS-4.14` зафиксирован как out-of-scope.

---

## 7. Итоговое заключение

**G7: PASSED.** Реализация доменного слоя модуля `technology` корректна, соответствует
требованиям (G2), OpenSpec `spec.md` и Test plan. Все функциональные сценарии покрыты
автоматизированными тестами и проходят; домен чист от инфраструктурных зависимостей; границы
модуля корректны; `ApplicationModules.verify()` проходит. Три minor-замечания ревьюера (M1–M3)
подтверждены как не блокирующие. Изменение готово к этапу Finalization и Pull Request (G8).
