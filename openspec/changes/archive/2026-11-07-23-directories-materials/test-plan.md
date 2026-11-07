# Test plan — 23-directories-materials

> Этап QA Planning (G4). Независимая оценка тестируемости решения и контракт верификации.
> Реализацию не начинает, артефакты проектирования не меняет.

## 1. Scope & goals

**Что тестируется:** backend модуля `directories` — два справочника `Material` и `Sortament`
(поля `number`/`name`/`standardType`/`standard`/`unit`, multi-tenant, soft-delete), Flyway-миграция
`V6__directories.sql`, идемпотентный `seed.sql`, REST `/api/v1/materials` и `/api/v1/sortaments`.

**Цели верификации:**

1. Доменные агрегаты `Material`/`Sortament` валидируют обязательные поля, неизменяемость `number`,
   обновление изменяемых полей и мягкое удаление.
2. Application-слой обеспечивает tenant-изоляцию через `OrganizationContext`, уникальность `number`
   в организации (проверка + частичный индекс), сортировку списка и мягкое удаление.
3. JPA-адаптер корректно маппирует агрегаты, фильтрует по `organization_id`/`deleted_at IS NULL` и
   точечно транслирует нарушение уникального индекса в доменное исключение.
4. REST-контроллеры возвращают верные статусы (201/200/204) и ошибки (404/409/400) в формате
   `{"error","message"}`.
5. Миграция V6 и `seed.sql` создают/наполняют схему идемпотентно; демо-данные покрывают
   ГОСТ/DIN/ISO в обоих справочниках.
6. Архитектурные границы (`ApplicationModules.verify()`) и чистота домена сохраняются.

**Вне объёма (по допущениям A1–A10):** связи Material↔Sortament, справочники стандартов и единиц
измерения, number-паттерны организаций, фронтенд, пагинация и поиск.

## 2. Test levels

| Уровень | Тип | Цель |
|---------|-----|------|
| Unit — домен | JUnit 5, без Spring | валидация, инварианты, идемпотентность `markDeleted`, ID/enum |
| Unit — application | JUnit 5 + in-memory репозиторий | поведение сервиса: CRUD, уникальность, изоляция, сортировка, контекст |
| Unit — мапперы | JUnit 5 | корректность отображения domain ↔ entity ↔ view |
| Slice/Integration — JPA | Testcontainers/PostgreSQL + Flyway | round-trip, tenant-фильтрация, частичный индекс, трансляция дубликата |
| Slice — контроллеры | standalone MockMvc + `RestExceptionHandler` | статусы, тела, маппинг ошибок |
| Architecture | `ApplicationModules.verify()`, purity-тест | границы модулей, чистота домена |
| Integration/REST-tenant | `@SpringBootTest` + Testcontainers | tenant-контекст на REST-уровне (рекомендуется) |
| Seed (manual/SQL) | ручной прогон `seed.sql` | идемпотентность и состав демо-данных |

## 3. Test scenarios

### 3.1 Домен (unit) — `DIR-UT-*`

- **DIR-UT-01** `Material.create` с валидными полями → создан агрегат; геттеры возвращают значения; `deletedAt == null`.
- **DIR-UT-02** `Sortament.create` с валидными полями → аналог DIR-UT-01.
- **DIR-UT-03** `create` с blank `number` / `name` / `standard` / `unit` (каждый по отдельности) → `IllegalArgumentException` с указанием поля.
- **DIR-UT-04** `create` с `standardType == null` → `IllegalArgumentException` с указанием `standardType`.
- **DIR-UT-05** `create` с `null` `id` / `organizationId` / `createdAt` / `updatedAt` → `NullPointerException` (конвенция `PartId`/`OrganizationId`).
- **DIR-UT-06** `update(...)` меняет `name`/`standardType`/`standard`/`unit`, обновляет `updatedAt`, оставляет `number` прежним.
- **DIR-UT-07** `update(...)` с blank/нулевыми значениями → `IllegalArgumentException`, состояние агрегата не изменено.
- **DIR-UT-08** `markDeleted()` устанавливает `deletedAt` и обновляет `updatedAt`; повторный вызов идемпотентен (значения не меняются).
- **DIR-UT-09** неизменяемость `number`: нет мутатора; `update` не принимает `number` (рефлексивная проверка).
- **DIR-UT-10** `StandardType` содержит ровно `GOST`, `DIN`, `ISO`, `OTHER`.
- **DIR-UT-11** `MaterialId`/`SortamentId`: `generate()` непустой; `of(uuid)` round-trip; `of(null)` → `NullPointerException`.
- **DIR-UT-12** порты `MaterialRepository`/`SortamentRepository`: свободные от аннотаций интерфейсы с сигнатурами `save`/`findById`/`findAll`/`existsByNumber` и без `delete` (по образцу `CatalogRepositoryPortTest`).

### 3.2 Application (unit, in-memory) — `DIR-AS-*`

- **DIR-AS-01** `create` возвращает `*View` с системными `id`/`createdAt`/`updatedAt`.
- **DIR-AS-02** `create` с дублирующим `number` в той же организации → `Duplicate*NumberException`.
- **DIR-AS-03** `create` с тем же `number` в другой организации успешен (tenant-изоляция).
- **DIR-AS-04** `create` с blank обязательным полем или `null` `standardType` → `IllegalArgumentException`.
- **DIR-AS-05** `get` возвращает активную запись; отсутствующая/удалённая/чужая → `*NotFoundException`.
- **DIR-AS-06** `list` возвращает только активные записи текущей организации, отсортированные по `number`; удалённые и чужие исключены.
- **DIR-AS-07** `update` меняет изменяемые поля, сохраняет `number`, обновляет `updatedAt`; отсутствующая → `*NotFoundException`; невалидные значения → `IllegalArgumentException`.
- **DIR-AS-08** `delete` мягко удаляет (скрывает из `get`/`list`); повторный `delete`/отсутствующая → `*NotFoundException`.
- **DIR-AS-09** создание записи с тем же `number` после мягкого удаления успешно.
- **DIR-AS-10** сервис передаёт `organizationId` из `OrganizationContext` в репозиторий (записывающий репозиторий фиксирует полученный org).
- **DIR-AS-11** отсутствие контекста → `OrganizationContextNotAvailableException`.

### 3.3 Мапперы (unit) — `DIR-MAP-*`

- **DIR-MAP-01** application-маппер `domain → *View`: отображает `id`/`number`/`name`/`standardType`/`standard`/`unit`/`createdAt`/`updatedAt`; НЕ отображает `organizationId`/`deletedAt`.
- **DIR-MAP-02** infra-маппер `entity ↔ domain`: `toDomain` восстанавливает активную запись через `create` (без `deletedAt`); `toEntity` сохраняет все поля, включая `deletedAt` после `markDeleted`. Ассиметрия `deletedAt` — осознанная (см. F1).

### 3.4 JPA-адаптер (Testcontainers/PostgreSQL + Flyway) — `DIR-JPA-*`

- **DIR-JPA-01** round-trip сохраняет и восстанавливает все поля, включая `standardType` (enum → STRING).
- **DIR-JPA-02** чтения tenant-scoped: для организации B `findById`/`findAll` записи организации A пусты.
- **DIR-JPA-03** soft-deleted исключается из `findById`/`findAll`/`existsByNumber`.
- **DIR-JPA-04** частичный уникальный индекс существует: `pg_indexes.indexdef` содержит `organization_id`, `number` и `deleted_at IS NULL` (для `material_number_org_uq`/`sortament_number_org_uq`).
- **DIR-JPA-05** дубликат на уровне БД (прямой `INSERT`) отклоняется `DataIntegrityViolationException`.
- **DIR-JPA-06** `number` освобождается после мягкого удаления: повторная вставка активной записи с тем же номером успешна.
- **DIR-JPA-07** `save` дублирующего активного `number` транслируется в доменное `Duplicate*NumberException`.
- **DIR-JPA-08** `findAll` сортирован по `number` по возрастанию.
- **DIR-JPA-09** `standardType` сохраняется строкой (`GOST`/`DIN`/`ISO`/`OTHER`), проверка значения колонки `standard_type`.

### 3.5 Контроллеры (standalone MockMvc + `RestExceptionHandler`) — `DIR-CTRL-*`

- **DIR-CTRL-01** `POST /api/v1/{materials|sortaments}` → 201, тело `*View`.
- **DIR-CTRL-02** `GET /api/v1/{materials|sortaments}` → 200, массив.
- **DIR-CTRL-03** `GET /api/v1/{materials|sortaments}/{id}` → 200.
- **DIR-CTRL-04** `PUT /api/v1/{materials|sortaments}/{id}` → 204 без тела; вызывает сервис с телом изменяемых полей (без `number`).
- **DIR-CTRL-05** `DELETE /api/v1/{materials|sortaments}/{id}` → 204.
- **DIR-CTRL-06** чужой/удалённый/отсутствующий `id` → 404, `{"error":"not_found",...}`.
- **DIR-CTRL-07** дубликат `number` → 409, `{"error":"duplicate",...}`.
- **DIR-CTRL-08** blank/`null` обязательные поля → 400, `{"error":"bad_request",...}`.
- **DIR-CTRL-09** формат ошибки для 404/409/400 содержит ключи `error` и `message`.

### 3.6 Архитектура — `DIR-ARCH-*`

- **DIR-ARCH-01** `ModuleVerificationTest` проходит: `directories` OPEN + `shared :: shared`; `web` + `directories :: directories`; граф ацикличен; `directories` в `EXPECTED_MODULES`.
- **DIR-ARCH-02** `DirectoriesDomainPurityTest`: агрегаты только с jMolecules-аннотациями; порты — аннотационно-свободные интерфейсы; доменные типы без Spring/JPA/PostgreSQL/Flyway.
- **DIR-ARCH-03** контекст стартует (`ApplicationContextTest` или `DirectoriesContextTest`): `MaterialService`/`SortamentService` доступны, контроллеры зарегистрированы однократно (guard от двойной регистрации, F2).

### 3.7 REST tenant-контекст (integration, рекомендуется) — `DIR-INT-*`

- **DIR-INT-01** данные организации A не видны организации B через REST; клиентский заголовок `X-Organization-Id` игнорируется (по образцу `CatalogRestTenantContextTest`).
- **DIR-INT-02** (опционально) e2e: create → get → update → delete → 404 через REST с JWT демо-пользователя.

### 3.8 Seed (manual/SQL) — `DIR-SEED-*`

- **DIR-SEED-01** `seed.sql` содержит секции `-- material`/`-- sortament` с фиксированными UUID, `ON CONFLICT DO NOTHING`, демо-организация `00000000-0000-0000-0000-000000000001`.
- **DIR-SEED-02** двукратное выполнение `seed.sql` после Flyway не создаёт дубликатов (одинаковый `count`).
- **DIR-SEED-03** демо-данные содержат минимум по одному представителю `GOST`, `DIN`, `ISO` в каждом справочнике.
- **DIR-SEED-04** после Flyway + `seed.sql` чтение через REST возвращает демо-записи демо-организации (ручной smoke или SQL-проверка).

## 4. Acceptance mapping (трассировка)

| Сценарий(и) | Acceptance Criteria |
|-------------|---------------------|
| DIR-UT-01…09 | AC-1 (агрегаты + create/update/markDeleted) |
| DIR-UT-10, DIR-UT-11, DIR-MAP-01, DIR-AS-01, DIR-CTRL-* | AC-2 (типы в `directories/api`) |
| DIR-ARCH-01 | AC-3 (границы модулей + `verify()`) |
| DIR-ARCH-02 | AC-4 (чистота домена) |
| DIR-ARCH-03 | AC-5 (WebConfig + старт приложения) |
| DIR-JPA-04, DIR-JPA-01, DIR-ARCH-03 | AC-6 (V6 + частичные уникальные индексы) |
| DIR-AS-02, DIR-AS-03, DIR-JPA-07, DIR-CTRL-07 | AC-7 (дубликат 409; чужой номер разрешён) |
| DIR-AS-09, DIR-JPA-06 | AC-8 (повторный POST после мягкого удаления) |
| DIR-AS-05, DIR-AS-06, DIR-AS-08, DIR-JPA-02, DIR-JPA-03, DIR-CTRL-06 | AC-9 (get/list не возвращают удалённые/чужие; 404) |
| DIR-AS-06, DIR-JPA-08 | AC-10 (сортировка по number) |
| DIR-AS-07, DIR-UT-06, DIR-CTRL-04 | AC-11 (PUT меняет 4 поля, не number, 204) |
| DIR-UT-03, DIR-UT-04, DIR-AS-04, DIR-CTRL-08 | AC-12 (blank/null → 400) |
| DIR-CTRL-01…05 | AC-13 (5 эндпоинтов, статусы/тела) |
| DIR-CTRL-06…09 | AC-14 (404/409/400 в формате `{"error","message"}`) |
| DIR-SEED-01, DIR-SEED-02 | AC-15 (seed идемпотентен) |
| DIR-SEED-03 | AC-16 (ГОСТ/DIN/ISO в обоих справочниках) |
| DIR-SEED-04 | AC-17 (REST возвращает демо после Flyway+seed) |
| DIR-JPA-02, DIR-INT-01 | AC-18 (изоляция двух организаций на PostgreSQL) |
| exit criteria, §7 | AC-19 (полный `mvn test` зелёный) |

## 5. Environment & data

- **Среда:** Maven + JUnit 5 + AssertJ; Spring Boot Test + Testcontainers (`postgres:17`, как в
  `AbstractPostgresIntegrationTest`); Flyway применяет `V1…V6`.
- **Зависимости для интеграционных тестов:** Docker с PostgreSQL (Testcontainers); Keycloak в
  интеграционных тестах не обязателен — JWT подделывается через `SecurityMockMvcRequestPostProcessors.jwt()`
  (`demoUserJwt()` в `AbstractPostgresIntegrationTest`).
- **Данные демо-организации:** `AbstractPostgresIntegrationTest.ensureDemoOrganization()` создаёт
  организацию `00000000-0000-0000-0000-000000000001` и владельца `demo-user`; `seed.sql` в тестах НЕ
  выполняется (ручной инструмент).
- **Тестовые данные домена/application:** фиксированный `Clock` (`2026-01-01T00:00:00Z`) для
  детерминированных `createdAt`/`updatedAt` при `create`; для `update`/`markDeleted` — относительные
  проверки (см. F4).
- **Seed-проверка:** выполняется вручную против локальной/Testcontainers БД после `flyway:migrate`,
  дважды, с проверкой `SELECT count(*)` и списка демо-записей.

## 6. Risks & assumptions

**Риски (см. findings):**

- **Гонка уникальности `number`** — закрыта двухуровневой защитой: `existsByNumber` + частичный
  уникальный индекс + трансляция `ConstraintViolationException` → `Duplicate*NumberException`
  (DIR-JPA-05…07). Замечание: application-проверка не атомарна; тест трансляции обязателен.
- **Seed-идемпотентность** — `ON CONFLICT DO NOTHING`; проверка двукратного выполнения (DIR-SEED-02).
  Уточнение по форме `ON CONFLICT` — F5.
- **Двойная регистрация контроллеров** — новые контроллеры регистрируются только component scan
  (D5), НЕ как `@Bean` в `WebConfig`; guard — DIR-ARCH-03. Прецедент legacy-двойной регистрации — F2.
- **Транзакционность** — сервисы `@Transactional`; атомарность для одноагрегатных операций тривиальна,
  откат при дубликате проверяется DIR-JPA-07 (трансляция исключения внутри транзакции).
- **Точное строковое совпадение `number`** — без trim/case-normalization (как в `catalog`);
  `"45"` и `" 45"` считаются разными (зафиксировано в design Risks).
- **Enum без CHECK-ограничения** — невалидное значение может попасть только прямым SQL; принято.

**Допущения:**

- `PUT` возвращает `204` (а не `200` из generic rest-api-conventions) — соответствует установленному
  контракту проекта (`PartController.rename`, R19, A6).
- `*View` без `organizationId`/`deletedAt`; организация определяется серверно.
- Малформированный UUID в path/body не входит в текущий error-контракт (нет обработчика
  `ResponseEntityExceptionHandler`/catch-all) — прецедент для всех контроллеров, не вводится новое поведение.
- Точные значения `updatedAt`/`deletedAt` у `update`/`markDeleted` не детерминированы (используют
  `Instant.now()`), поэтому тесты проверяют относительное изменение (см. F4).

## 7. Entry/exit criteria

**Entry (G4 → готовность к реализации):**

- Требования + AC (G2) утверждены; OpenSpec-артефакты (proposal/specs/design/tasks) согласованы.
- Блокирующих findings нет (все findings в пакете — неблокирующие, см. F1–F5).
- Окружение для Testcontainers доступно (Docker).

**Exit (G7 pass criteria):**

- Все сценарии §3 покрыты автоматизированными тестами (кроме `DIR-SEED-*` — ручная проверка), тесты
  зелёные.
- `mvn test` полностью зелёный, включая `ModuleVerificationTest`, `DirectoriesDomainPurityTest`,
  application/JPA/controller-тесты `directories` (AC-19).
- `V6__directories.sql` применяется без ошибок; частичные уникальные индексы подтверждены (DIR-JPA-04).
- Двукратное выполнение `seed.sql` не создаёт дубликатов; демо покрывает ГОСТ/DIN/ISO (DIR-SEED-02/03).
- Каждая ошибка 404/409/400 возвращает `{"error","message"}`; статусы 201/200/204 соблюдены.
- Найдено 0 новых нарушений границ/циклов модулей и 0 нарушений чистоты домена.
