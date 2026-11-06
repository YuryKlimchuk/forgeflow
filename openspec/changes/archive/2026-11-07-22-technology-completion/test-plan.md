# Test plan — 22-technology-completion

## 1. Scope & goals

**Покрываем (in scope):**
- application-слой модуля `technology`: входной порт `TechnologyService` + `TechnologyApplicationService` (создание, чтение по id, список с фильтром по `partId`, переименование, логическое удаление, добавление/удаление операции, назначение/снятие дефолтности).
- JPA-персистентность `technology_process` / `technology_process_operation` и мапперы entity↔domain (полный round-trip без потерь полей, включая `defaultProcess`, `deletedAt`, временные метки и упорядоченные операции).
- Flyway-миграция `V5__technology.sql` и частичный уникальный индекс `(organization_id, number) WHERE deleted_at IS NULL`.
- REST-контракт `/api/v1/technology-processes` (9 эндпоинтов, статусы, коды ошибок).
- Tenant-изоляция: организация определяется только серверно, клиентский `X-Organization-Id` игнорируется.
- Инварианты: уникальность номера в организации, «ровно один дефолтный процесс на деталь при 2+ процессах», мягкое удаление и освобождение номера.
- Транзакционность изменяющих операций.
- Архитектурные границы: `ApplicationModules.verify()`, чистота домена `technology`.

**Не покрываем (out of scope):**
- Интеграция технологических процессов со спецификациями (закрепление процесса за позицией спецификации).
- Фронтенд.
- Пагинация и полнотекстовый поиск списка.
- Валидация номера по паттерну организации (`OrganizationSettingsProvider`).
- Оборудование/нормы времени/материалы операций, переупорядочивание операций, перенос процесса на другую деталь.
- Реальный Keycloak-сервер в E2E (используется тестовый issuer JWT).

**Цели:** подтвердить, что каждая Acceptance Criteria проверяема, у каждого use case есть позитивный/негативный/граничный сценарий, а риски решения (D6, гонка уникальности, N+1, двойная регистрация контроллера, расхождение часов) явно покрыты тестами либо зафиксированы как осознанные компромиссы.

## 2. Testing levels

| Уровень | Что проверяем | Инструменты |
|---|---|---|
| **Unit** | `TechnologyApplicationService` (use case'ы, tenant-изоляция, приоритет ошибок 400→404→409, инвариант дефолтности), фабрика `restore`, application-маппер `TechnologyMapper` | JUnit 5, AssertJ, Mockito, in-memory `InMemoryTechnologyProcessRepository`, fake `OrganizationContext`, `Clock.fixed` |
| **Unit** | Инфраструктурный `TechnologyMapper` (entity ↔ domain round-trip через `restore`) | JUnit 5, AssertJ |
| **Slice** | `TechnologyProcessController` (все эндпоинты, статусы, коды ошибок) | MockMvc standalone + `RestExceptionHandler`, mock `TechnologyService` |
| **Integration (DB)** | `JpaTechnologyProcessRepository` на реальном PostgreSQL: round-trip, tenant-изоляция, soft-delete, порядок операций, частичный уникальный индекс, транзакционность | Testcontainers PostgreSQL (`AbstractPostgresIntegrationTest`) |
| **Integration (REST)** | Сквозной tenant-scoped REST-контракт + игнорирование `X-Organization-Id` | `@SpringBootTest` + `AbstractPostgresIntegrationTest` + `demoUserJwt()` |
| **Architecture** | `ApplicationModules.verify()`, чистота домена, загрузка контекста | `ModuleVerificationTest`, `TechnologyDomainPurityTest`, `ApplicationContextTest` |

**Обоснование уровней:** бизнес-правила — в домене/application-слое, поэтому быстрая unit-проверка use case'ов на in-memory репозитории покрывает основную логику. Slice-тест контроллера проверяет HTTP-контракт без подъёма БД. Интеграционные тесты обязательны для гарантий, которые mock'и дать не могут: частичный уникальный индекс, `@Transactional`-откат, корректный маппинг `merge`/`flush` в JPA-адаптере, tenant-изоляция на уровне SQL.

## 3. Test scenarios

Легенда статусов: 201/200/204 — HTTP; `IllegalArgumentException`/доменное исключение — для unit-уровня. Сценарии, помеченные **[DB]**, требуют реального PostgreSQL.

### Unit — `TechnologyApplicationService`

- **U1. Создание валидного процесса [positive]**
  Шаги: `create({number="TP-1", name="Фрезеровка", partId=P1})`; `partService.get(P1)` → доступная деталь.
  Ожидается: возвращается view с `defaultProcess=false`, `id != null`, `createdAt==updatedAt==NOW` (Clock.fixed); `get(id)` и `list(null)` видят процесс. → AC2, AC6.

- **U2. Создание с пустым/пробельным number [negative]**
  Шаги: `create({number="   ", name="X", partId=P1})`.
  Ожидается: `IllegalArgumentException` (→400), процесс не создан, `partService.get` НЕ вызывается. → AC4.

- **U3. Создание с пустым/пробельным name [negative]**
  Шаги: `create({number="TP-1", name="", partId=P1})`.
  Ожидается: `IllegalArgumentException` (→400). → AC4.

- **U4. Создание с null partId [negative]**
  Шаги: `create({number="TP-1", name="X", partId=null})`.
  Ожидается: `IllegalArgumentException` (→400) до обращения к `partService`. → AC4.

- **U5. Дубликат номера в одной организации [negative]**
  Шаги: создать `TP-1`; повторно `create` с тем же `number` (та же организация).
  Ожидается: `DuplicateTechnologyProcessNumberException` (→409), второй процесс не сохранён. → AC3.

- **U6. Один номер в разных организациях [positive/tenant]**
  Шаги: тот же `number` в orgA и orgB.
  Ожидается: оба успешны, каждая организация видит только свой процесс. → AC3, AC22.

- **U7. Несуществующая или чужая деталь [negative]**
  Шаги: `partService.get(P2)` бросает `PartNotFoundException`.
  Ожидается: `PartNotFoundException` (→404), процесс не создан; приоритет 400→404→409 соблюдён (пустой number даёт 400 раньше, чем 404 детали). → AC5.

- **U8. Второй не-дефолтный процесс без дефолтного [negative/инвариант]**
  Шаги: у детали один не-дефолтный процесс; `create` второго не-дефолтного.
  Ожидается: `InvalidDefaultProcessStateException` (→409), второй процесс не сохранён. → AC15.

- **U9. Инвариант соблюдён после назначения дефолтного [positive]**
  Шаги: создать P1#1, `markAsDefault(P1#1)`, создать P1#2 (не-дефолтный).
  Ожидается: P1#2 создан, дефолтным остаётся P1#1 (ровно один дефолтный). → AC13, AC15.

- **U10. get активного процесса [positive]**
  Шаги: `get(id)`.
  Ожидается: возвращается view. → AC8.

- **U11. get отсутствующего [negative]** → `TechnologyProcessNotFoundException` (→404). → AC8.
- **U12. get удалённого [negative]** → 404 (soft-delete исключён из чтения). → AC8.
- **U13. get чужого [negative/tenant]** → 404. → AC8, AC22.

- **U14. list сортировка по номеру [positive]**
  Шаги: создать `TP-2`, затем `TP-1`.
  Ожидается: порядок `[TP-1, TP-2]`. → AC6.

- **U15. list исключает удалённые и чужие [negative]** → только активные своей организации. → AC6, AC22.

- **U16. list с фильтром по partId [positive]** → только процессы указанной детали. → AC7.

- **U17. rename валидный [positive]**
  Шаги: `rename(id, "Новое имя")`.
  Ожидается: `name` обновлён, `updatedAt` увеличился (≥ прежнего). → AC9.

- **U18. rename пустой [negative]** → `IllegalArgumentException` (→400), имя не изменилось. → AC9.

- **U19. delete [positive]**
  Шаги: `delete(id)`.
  Ожидается: процесс не читается по id и отсутствует в `list`; в хранилище `deletedAt != null` (физического удаления нет). → AC10.

- **U20. delete единственного дефолтного [boundary/принятый edge case]**
  Шаги: единственный дефолтный процесс детали → `delete`.
  Ожидается: успех (204), дефолтность других процессов не меняется, деталь остаётся без дефолтного. → AC16.

- **U21. addOperation валидные и порядок [positive]**
  Шаги: добавить операции с номерами 20 и 10.
  Ожидается: обе операции, упорядочены `[10, 20]`. → AC11.

- **U22. addOperation дубликат [negative]** → `DuplicateOperationException` (→409), существующая операция не изменена. → AC11.

- **U23. addOperation невалидная (number ≤ 0 или пустое имя) [negative]** → `IllegalArgumentException` (→400). → AC11.

- **U24. removeOperation валидный [positive]** → операция удалена. → AC12.
- **U25. removeOperation отсутствующий [negative]** → `OperationNotFoundException` (→404). → AC12.

- **U26. markAsDefault переключение [positive]**
  Шаги: два процесса, первый дефолтный; `markAsDefault(второй)`.
  Ожидается: второй дефолтный, первый больше не дефолтный (не более одного). → AC13.

- **U27. markAsDefault уже дефолтного [boundary/idempotent]** → успех, состояние не меняется. → AC13.

- **U28. unmarkDefault валидный [positive]** → 204, процесс не дефолтный. → AC14.

- **U29. unmarkDefault единственного дефолтного при наличии других активных [negative]** → `InvalidDefaultProcessStateException` (→409), дефолтность сохраняется. → AC14.

- **U30. unmarkDefault не-дефолтного [boundary/idempotent]** → 204 (no-op). → AC14.

### Unit — мапперы и фабрика `restore`

- **M1. `restore` без потерь [positive]**
  Шаги: `restore(id, number, name, partId, orgId, defaultProcess=true, createdAt, updatedAt, deletedAt=non-null, operations=[20,10])`.
  Ожидается: все поля восстановлены (`defaultProcess=true`, `deletedAt` не null, временные метки), операции отсортированы `[10,20]`, `updatedAt` НЕ перезаписан вызовом `addOperation`. → AC24.

- **M2. application `TechnologyMapper` → view [positive]**
  Шаги: домен → `TechnologyProcessView`.
  Ожидается: все поля (`id, number, name, partId, defaultProcess, createdAt, updatedAt, operations`), операции в порядке агрегата. → AC24.

- **M3. infrastructure `TechnologyMapper` entity↔domain round-trip [positive]**
  Шаги: domain → entity(+operations) → domain.
  Ожидается: равенство всех полей (`defaultProcess`, `deletedAt`, временные метки, операции), операции отсортированы по `operation_number`. → AC24, AC17.

### Slice — `TechnologyProcessController`

- **C1. POST → 201 [positive]**. → AC2, AC21.
- **C2. GET список → 200 [positive]**. → AC6.
- **C3. GET `?partId=` → 200 только детали [positive]**. → AC7.
- **C4. GET `/{id}` → 200 [positive]**. → AC8.
- **C5. PUT `/{id}/name` → 204 [positive]**. → AC9.
- **C6. DELETE `/{id}` → 204 [positive]**. → AC10.
- **C7. POST `/{id}/operations` → 201 [positive]**. → AC11.
- **C8. DELETE `/{id}/operations/{number}` → 204 [positive]**. → AC12.
- **C9. PUT `/{id}/default` → 204 [positive]**. → AC13.
- **C10. DELETE `/{id}/default` → 204 [positive]**. → AC14.
- **C11. 404 `not_found`** — отсутствующий/чужой/удалённый процесс, отсутствующая операция. → AC21, AC8, AC12.
- **C12. 409 `duplicate`** — дубликат номера процесса и номера операции. → AC21, AC3, AC11.
- **C13. 409 `invalid_state`** — нарушение инварианта дефолтности. → AC21, AC14, AC15.
- **C14. 400 `bad_request`** — пустой `number`/`name`, `null`-`partId`. → AC21, AC4, AC9.
- **C15. 404 `not_found`** при создании с чужой/отсутствующей деталью (маппинг `PartNotFoundException`). → AC21, AC5.

### Integration — JPA repository (PostgreSQL) **[DB]**

- **I1. Round-trip [positive]**
  Шаги: `save(process + operations)`; `findById`.
  Ожидается: агрегат восстановлен с теми же id, полями и операциями, операции упорядочены по `operation_number`. → AC17.

- **I2. Tenant-изоляция чтений [negative]** — `findById`/`findAll`/`findByPartId` для чужой организации пусты. → AC22, AC6.

- **I3. Soft-delete исключён из чтений [negative]** — после `markDeleted`+`save` процесс не виден в `findById`/`findAll`, `existsByNumber` возвращает false. → AC6, AC10.

- **I4. Частичный уникальный индекс существует [positive]**
  Шаги: запрос к `pg_indexes`/`pg_index` по имени `technology_process_number_org_uq`.
  Ожидается: индекс задан на `(organization_id, number)` с `WHERE deleted_at IS NULL`. → AC18.

- **I5. Отклонение дубликата активного процесса на уровне БД [negative]**
  Шаги: прямая вставка/`save` второго активного процесса с тем же `(org, number)`.
  Ожидается: `DataIntegrityViolationException` (ограничение уникальности). → AC18.

- **I6. Номер освобождается после soft-delete [positive]**
  Шаги: активный процесс → `markDeleted`+`save` → создать активный процесс с тем же номером в той же организации.
  Ожидается: вставка успешна. → AC19.

- **I7. `findByPartId` сортировка по номеру [positive]** → порядок по `number ASC`. → AC7.

- **I8. Транзакционность изменяющей операции [negative]**
  Шаги: `TechnologyApplicationService` (реальный бин, реальный JPA-репозиторий) + принудительная ошибка в середине `markAsDefault`/`create` (например, дубликат на втором `save`).
  Ожидается: после отката в БД нет частичных изменений (ни одного нового/изменённого процесса/операции). → AC20.

### Integration — REST (tenant context, E2E) **[DB]**

- **E1. Сквозной сценарий [positive]**
  Шаги: с `demoUserJwt()` — `POST` (201) → `GET` (200) → `GET ?partId=` (200) → `PUT name` (204) → `DELETE` (204) → `GET` (404).
  Ожидается: статусы и состояния соответствуют; `defaultProcess=false` у созданного. → AC2, AC6, AC7, AC8, AC9, AC10.

- **E2. Игнорирование `X-Organization-Id` [negative/tenant]**
  Шаги: создать процесс в orgB; запрос от `demo-user` (orgA) с заголовком `X-Organization-Id: orgB`.
  Ожидается: 404 `not_found`, данные orgB не раскрываются; контекст организации очищен после запроса. → AC22.

- **E3. Миграция на чистой базе + восстановление после перезапуска [positive]**
  Шаги: чистая БД → Flyway применяет V1–V5; создать процесс; повторно загрузить (имитация рестарта через повторное чтение).
  Ожидается: `V5__technology.sql` применена; процесс читается с теми же id/значениями. → AC17.

### Architecture

- **A1. Модульный граф [positive]** — `ApplicationModules.verify()` проходит, `web -> technology` разрешена, циклов нет. → AC1.
- **A2. Чистота домена [positive]** — `TechnologyDomainPurityTest` зелёный (домен без Spring/JPA/PostgreSQL/Flyway; фабрика `restore` не вносит инфраструктурных зависимостей). → AC23.
- **A3. Загрузка контекста [positive]** — `ApplicationContextTest` (или аналог) подтверждает, что бины технологии собираются в `WebConfig` без двойной регистрации контроллера и без ошибок маппинга запросов. → AC1.

## 4. Acceptance mapping

| AC | Сценарии |
|---|---|
| 1. Компиляция + `ApplicationModules.verify()` + `web→technology` | A1, A3 |
| 2. POST 201, `defaultProcess=false`, виден в GET | U1, C1, E1 |
| 3. Дубликат номера 409; одинаковый номер в разных орг. | U5, U6, C12 |
| 4. Пустой/пробельный number/name, null partId → 400 | U2, U3, U4, C14 |
| 5. Чужая/отсутствующая деталь → 404 | U7, C15 |
| 6. Список 200, сортировка, без удалённых/чужих | U14, U15, C2, I2, I3 |
| 7. Список с фильтром partId | U16, C3, I7 |
| 8. get 200 / 404 (отсутствующий/удалённый/чужой) | U10–U13, C4, C11 |
| 9. rename 204 + name/updatedAt; пустой → 400 | U17, U18, C5 |
| 10. delete 204, soft-delete, deleted_at non-null | U19, C6, I3 |
| 11. addOperation 201, порядок, дубликат 409 | U21–U23, C7, C12 |
| 12. removeOperation 204 / 404 | U24, U25, C8, C11 |
| 13. default 204, переключение прежнего | U9, U26, U27, C9 |
| 14. unmark default 204 / 409 (единственный дефолтный при 2+) | U28, U29, U30, C10, C13 |
| 15. 2-й не-дефолтный без дефолтного → 409 | U8, U9, C13 |
| 16. Удаление единственного дефолтного разрешено | U20 |
| 17. Flyway V5 + восстановление после рестарта | I1, E3, M3 |
| 18. Частичный уникальный индекс + отказ дубликата на БД | I4, I5 |
| 19. Удалённый процесс не учитывается в уникальности | I6 |
| 20. Транзакционность изменений | I8 |
| 21. Коды ошибок REST (404/409/400) | C11–C15 |
| 22. Игнорирование `X-Organization-Id` | U6, U13, U15, I2, E2 |
| 23. Чистота домена | A2 |
| 24. Новые unit/слайс/интеграционные тесты + существующие зелёные | U*, M1–M3, C*, I*, A1–A3 |

## 5. Environment & data

- **БД:** PostgreSQL через Testcontainers (`postgres:17`, singleton в `AbstractPostgresIntegrationTest`); Flyway `enabled: true`, миграции V1–V5 применяются автоматически; `ddl-auto: none`.
- **Аутентификация/tenant:** реальный Keycloak НЕ требуется. Для интеграционных REST-тестов используется тестовый JWT (`demoUserJwt()`, issuer `http://localhost:8081/realms/forgeflow-users`) и `TenantContextFilter`. Демо-организация `00000000-0000-0000-0000-000000000001` и `demo-user` создаются в `AbstractPostgresIntegrationTest`.
- **Test data:**
  - orgA = `DEMO_ORGANIZATION_ID`; orgB = `OrganizationId.generate()`.
  - Детали: `Part` orgA (доступная) и orgB (чужая) — через `PartRepository`/`PartService` в тесте; для unit-тестов `PartService` мок.
  - Номера процессов: `TP-1`, `TP-2`; номера операций: `10`, `20`.
- **Конфигурация времени:** `Clock.fixed(NOW)` для unit-тестов create; для мутаторов (`rename`, `addOperation`, `markAsDefault` и т.д.) `updatedAt` берётся из `Instant.now()` — тесты проверяют `updatedAt` относительно (≥ прежнего), а не точным значением.
- **Слайс-тест контроллера:** `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler())`; `TechnologyService` — `@MockitoBean`/mock (без Spring-контекста БД).

## 6. Risks and recommendations

| Риск | Уровень | Митигация / рекомендация |
|---|---|---|
| Гонка уникальности номера: между `existsByNumber` и `save` параллельные создания могут дать `DataIntegrityViolationException` (→500), если не замаплен | major | Явно замапить `DataIntegrityViolationException` пути сохранения технологии → 409 `duplicate` (см. Finding F1) и покрыть тестом I5/доп. |
| Транзакционность проверяется только на уровне unit (in-memory) — `@Transactional` откат не проверяем | major | Добавить integration-сценарий I8 на реальном PostgreSQL (см. Finding F2) |
| Двойная регистрация контроллера (`@RestController` + `@Bean`) | minor | Следовать D9: только component scan; A3 подтверждает загрузку контекста без ambiguous mapping (см. F3) |
| `updatedAt` мутаторов из `Instant.now()`, `create` из `Clock` — флак в точных проверках времени | minor | Тесты используют `Clock.fixed` для create и относительные проверки `updatedAt` (см. F4) |
| N+1 по операциям при чтении списка | minor | Принято для MVP (как `planning`); в тестах проверяем корректность, не производительность (см. F5) |
| Отсутствие org-контекста (admin realm) → `OrganizationContextNotAvailableException` не замаплен (→500) | minor | Вне объёма (консистентно с part/planning); зафиксировать как assumption/future change |

## 7. Entry/exit criteria

**Entry (вход на реализацию G5):**
- Требования (G2) утверждены.
- Пакет «design + test plan» (G4) утверждён пользователем.
- Все блокирующие findings закрыты (см. F1).

**Exit (G7 — фактическая проверка по QA):**
- Все сценарии раздела 3 реализованы и проходят.
- Все Acceptance Criteria (24) покрыты и выполнены.
- `mvn test` зелёный: существующие доменные тесты (`TechnologyProcessTest`, `TechnologyProcessServiceTest`, `TechnologyDomainPurityTest`, `TechnologyProcessRepositoryPortTest`), `ModuleVerificationTest` и контекстные тесты проходят.
- `ApplicationModules.verify()` проходит, граф ацикличен, `web -> technology` разрешена.
- Flyway `V5__technology.sql` применяется на чистой базе; частичный уникальный индекс подтверждён (I4).
- Не осталось незакрытых `blocking` findings.

## Findings

Сводка findings (полные блоки `### F<n>` переданы Orchestrator'у):

- **F1 (major, blocking, userDecisionRequired: no)** — гонка уникальности номера: маппинг `DataIntegrityViolationException` → 409 не зафиксирован в design/tasks.
- **F2 (major, blocking: no)** — отсутствует integration-тест транзакционности (`@Transactional`-откат) на реальном PostgreSQL.
- **F3 (minor, blocking: no)** — двойная регистрация контроллеров как pre-existing паттерн; для нового контроллера строго следовать D9.
- **F4 (minor, blocking: no)** — расхождение часов `Clock`/`Instant.now()`; рекомендации по проверкам времени.
- **F5 (minor, blocking: no)** — N+1 по операциям, принято для MVP.
