# Design

## Context

См. `proposal.md` — Why. Модуль `technology` уже имеет пакеты `api`, `application`, `domain`, `infrastructure` и корневую декларацию `@ApplicationModule(allowedDependencies = {"catalog :: catalog", "directories :: directories"})`; `api` помечен `@NamedInterface("technology")`. Изменение следует образцу инкремента `catalog-domain-model` (change 1): только доменные типы, value objects, типизированный ID и driven port, без application-сервисов, JPA, Flyway и REST. Эталоном стиля служат `catalog.domain.part.Part` (валидация, аудит, soft-delete, `rename`), `catalog.api.PartId` (record с `UUID`), `planning.domain.Plan`/`PlanItem` (упорядоченные value objects) и `catalog.domain.part.PartRepository` (tenant-scoped driven port).

## Goals / Non-Goals

**Goals:**

- Представить технологический процесс как богатый агрегат с операциями и привязкой к детали каталога через `PartId`.
- Сделать идентификатор `TechnologyProcessId` типобезопасным публичным контрактом.
- Зафиксировать доменный механизм принуждения кросс-агрегатных правил (уникальность номера в организации, дефолтность на деталь) без прикладного слоя.
- Сохранить чистоту домена от Spring/JPA/PostgreSQL/Flyway и текущие границы Spring Modulith, добавив разрешённую зависимость `shared :: shared` для `shared.api.OrganizationId`.

**Non-Goals:**

- Application-сервисы/use case'ы, REST, JPA-сущности, Flyway-миграции и фронтенд для technology.
- Storage-уровневый гарант уникальности номера (частичный уникальный индекс и транзакция — только при появлении персистентности).
- Семантика дефолтности при логическом удалении дефолтного процесса (отдельный future change).
- Привязка к сборочным единицам (`Assembly`) и прочим типам каталога.
- Оборудование, нормы времени, материалы, сортамент, единицы измерения и текстовое описание операций.
- Переупорядочивание существующих операций и перенос процесса на другую деталь.
- Валидация номера по паттерну организации (`OrganizationSettingsProvider`).

## Decisions

### D1. Размещение типов

`TechnologyProcessId` размещается в `com.hydroyura.forgeflow.technology.api` (существующий `@NamedInterface("technology")`), так как на идентификатор будут ссылаться соседние модули. Агрегат `TechnologyProcess`, value object `Operation`, порт `TechnologyProcessRepository`, доменный сервис `TechnologyProcessService` и доменные исключения размещаются в `com.hydroyura.forgeflow.technology.domain`. Сущности остаются скрытыми в domain; публикуется только неизменяемый ID.

**Альтернатива:** разместить `TechnologyProcessId` в `technology.domain` рядом с агрегатом. Отклонено: межмодульные ссылки на деталь/процесс должны идти через `api`-пакеты, как `catalog.api.PartId`.

### D2. `TechnologyProcessId` как record

`TechnologyProcessId` — неизменяемый `record` с компонентом `UUID value`, статическими фабриками `generate()` и `of(UUID)` и проверкой `null` в компактном конструкторе. Зеркалит `PartId`/`OrganizationId`, включая исключение `NullPointerException` для `null` (`Objects.requireNonNull`).

**Альтернатива:** использовать `UUID` напрямую или общий `EntityId`. Отклонено: теряется типобезопасность на границах модулей.

### D3. `Operation` как record-значение

`Operation` — неизменяемый `record` с полями `int number` (положительное) и `String name` (непустое, не пробельное и не `null`), валидация в компактном конструкторе (`IllegalArgumentException` через семантику `StringUtils.isBlank`). Операция не имеет собственного ID и персистентности и идентифицируется внутри процесса по `number` — по образцу `SpecificationRow`/`PlanItem`.

**Альтернатива:** сущность операции с собственным ID. Отклонено: в этом инкременте операции — составные части агрегата; вопрос идентичности пересматривается при появлении норм/оборудования.

### D4. Состав агрегата `TechnologyProcess`

Поля: `@Identity TechnologyProcessId id` (final), `String number` (final), `String name`, `PartId partId` (final), `OrganizationId organizationId` (final), `boolean defaultProcess`, `Instant createdAt` (final), `Instant updatedAt`, `Instant deletedAt` (nullable), `List<Operation> operations`. Приватный конструктор + статическая фабрика `create(TechnologyProcessId id, String number, String name, PartId partId, OrganizationId organizationId, Instant createdAt, Instant updatedAt)`; `defaultProcess` при создании всегда `false` (дефолтность устанавливается явным доменным действием), `deletedAt` — `null`. `number`, `name`, `id`, `partId`, `organizationId`, `createdAt` валидируются fail-fast.

**Альтернатива:** передавать `defaultProcess` в фабрику. Отклонено: зафиксировано допущение «дефолтность при создании = false».

### D5. Операции: порядок, добавление, удаление

Операции хранятся во внутреннем `ArrayList<Operation>`, поддерживаемом в порядке возрастания `number` (вставка в нужную позицию). `addOperation(int number, String name)` создаёт `Operation` (валидация внутри), отклоняет дубликат номера (`DuplicateOperationException`) и добавляет. `removeOperation(int number)` ищет по номеру и удаляет, при отсутствии — `OperationNotFoundException`. `operations()` возвращает `Collections.unmodifiableList(...)`. «Дырки» в нумерации допустимы.

**Альтернатива:** сортировка только на чтении. Отклонено: поддержание отсортированного списка при вставке даёт стабильный порядок при любом способе чтения и упрощает тесты.

### D6. Soft-delete и `rename`

`markDeleted()` идемпотентен: при уже установленном `deletedAt` ничего не делает; иначе устанавливает `deletedAt` и обновляет `updatedAt`. `rename(String)` отклоняет пустое/пробельное/`null` имя и при валидном обновляет `name` и `updatedAt`. Физического удаления нет — как в `Part`.

### D7. Аудит-семантика `updatedAt`

Все мутаторы агрегата (`rename`, `markDeleted`, `addOperation`, `removeOperation`, `markAsDefault`, `markAsNotDefault`) обновляют `updatedAt` при фактическом изменении состояния — единая конвенция аудита, консистентная с `Part` («любое изменение трогает updatedAt»). Идемпотентные повторные вызовы, не меняющие состояние (повторный `markDeleted`, `markAsDefault` для уже дефолтного процесса, `markAsNotDefault` для уже не-дефолтного процесса), являются no-op и не обновляют `updatedAt`. Спецификация явно фиксирует это правило требованием «Аудит-семантика `updatedAt` мутаторов». Поля `createdBy`/`updatedBy` не вводятся.

### D8. Порт `TechnologyProcessRepository`

Driven port-интерфейс в `technology.domain` без Spring/JPA-аннотаций:

- `void save(TechnologyProcess process)`;
- `Optional<TechnologyProcess> findById(TechnologyProcessId id, OrganizationId organizationId)`;
- `List<TechnologyProcess> findByPartId(PartId partId, OrganizationId organizationId)`;
- `List<TechnologyProcess> findAll(OrganizationId organizationId)`;
- `boolean existsByNumber(String number, OrganizationId organizationId)`.

Чтение принимает `OrganizationId` явно и возвращает только активные (не удалённые) процессы организации. Операции `delete` нет; логическое удаление — `markDeleted()` + `save`. Аналог `PartRepository` + метод детали.

**Альтернатива:** добавить `delete`. Отклонено: физическое удаление не требуется и противоречит soft-delete.

### D9. Доменный сервис `TechnologyProcessService` — принуждение кросс-агрегатных правил

Ключевое решение по неблокирующему вопросу аналитика. Инвариант дефолтности и уникальность номера охватывают несколько агрегатов `TechnologyProcess`, ссылающихся на одну деталь/организацию, поэтому не могут быть обеспечены одним агрегатом. Выбран **чистый доменный сервис** `TechnologyProcessService` (пакет `technology.domain`, без Spring/транзакций/DTO), зависящий только от driven port `TechnologyProcessRepository`:

- `register(TechnologyProcess process)` — отклоняет дубликат номера через `existsByNumber` (`DuplicateTechnologyProcessNumberException`) и, если процесс не-дефолтный, а у детали уже есть активные процессы и ни один не дефолтный — отклоняет регистрацию (`InvalidDefaultProcessStateException`), иначе сохраняет через `save`. Так как `defaultProcess` при создании всегда `false` (D4), ветка «процесс уже дефолтный при регистрации» является защитной логикой и недостижима через публичный API; реальное принуждение «не более одного дефолтного» происходит в `markAsDefault`.
- `markAsDefault(TechnologyProcessId id, OrganizationId organizationId)` — загружает процесс (иначе `TechnologyProcessNotFoundException`); если целевой процесс уже дефолтный — завершается как **идемпотентный no-op** (не снимает дефолтность у прочих и не сохраняет изменений). Иначе снимает дефолтность у прочих дефолтных процессов той же детали (`markAsNotDefault` + `save`) и помечает целевой процесс (`markAsDefault` + `save`).
- `unmarkDefault(TechnologyProcessId id, OrganizationId organizationId)` — загружает процесс (иначе `TechnologyProcessNotFoundException`); если целевой процесс уже не-дефолтный — завершается как **идемпотентный no-op**. Иначе снимает дефолтность, но отклоняет (`InvalidDefaultProcessStateException`), если процесс является единственным дефолтным и у детали есть ещё активный процесс.

Идемпотентные no-op повторных вызовов `markAsDefault`/`unmarkDefault` заданы по аналогии с идемпотентным `markDeleted` (D6). Мутаторы дефолтности на агрегате (`markAsDefault()`/`markAsNotDefault()`) объявляются package-private, чтобы менять флаг можно было только через доменный сервис.

**Почему не application-сервис:** application-слой для technology отсутствует в этом инкременте (требование минимальности), а бизнес-правило принадлежит домену.
**Почему не репозиторий/порт:** порт — инфраструктурно-нейтральный контракт доступа к данным; размещение бизнес-правил в нём нарушило бы «бизнес-логика в доменном слое» и чистоту порта.
**Компромисс:** сервис `register` координирует сохранение, что отчасти похоже на use case; это допустимо для чистого доменного сервиса, а при появлении application-слоя он будет делегировать в этот сервис, а не дублировать правила.

### D10. Доменные исключения

В `technology.domain` добавляются: `DuplicateTechnologyProcessNumberException` (уникальность номера), `DuplicateOperationException` (дубликат операции), `OperationNotFoundException` (удаление отсутствующей операции), `TechnologyProcessNotFoundException` (чтение в сервисе), `InvalidDefaultProcessStateException` (нарушение инварианта дефолтности). Ошибки валидации значений (`number <= 0`, пустые/`null` строки) — `IllegalArgumentException`, как в `Part`/`Plan`. Для `TechnologyProcessId.of(null)` — `NullPointerException` (конвенция `PartId`/`OrganizationId`).

### D11. Тесты и проверяемость

Unit-тесты для `TechnologyProcessId`, `Operation`, `TechnologyProcess`, `TechnologyProcessService`; reflection-тест формы порта (по образцу `CatalogRepositoryPortTest`); in-memory test double `InMemoryTechnologyProcessRepository` в `src/test/.../technology/infrastructure` (тестовая инфраструктура, не production); тест доменной чистоты импортов; существующий `ModuleVerificationTest` подтверждает ацикличность графа.

## Risks / Trade-offs

- **[Риск]** Кросс-агрегатный инвариант дефолтности не атомарен без транзакций: между снятием старой дефолтности и установкой новой существует промежуточное состояние. → **Митигировать:** в доменном сервисе операции последовательны и детерминированы; при появлении персистентности application-слой обернёт их в транзакцию (как в `persistence-foundation`).
- **[Риск]** Уникальность номера не имеет storage-уровневого гаранта: в этом change она принуждается только проверкой `existsByNumber` в доменном сервисе, поэтому при параллельных регистрациях возможна гонка. → **Митигировать (future change):** при появлении персистентности обязателен частичный уникальный индекс `(organization_id, number) WHERE deleted_at IS NULL` и транзакционная обёртка регистрации; в текущем change уникальность гарантируется доменным сервисом в последовательной семантике.
- **[Риск / принятый edge case]** Soft-delete дефолтного процесса: при удалении единственного дефолтного процесса активным остаётся не-дефолтный процесс, и `register`-guard («есть активные и ни один не дефолтный») может заблокировать добавление нового процесса. → **Митигировать:** зафиксировано как out-of-scope; семантика дефолтности при логическом удалении дефолтного процесса откладывается на отдельный future change.
- **[Риск]** Доменный сервис `register` можно принять за application-сервис. → **Митигировать:** явно зафиксировать его как чистый доменный сервис (D9), без Spring/JPA/DTO, и проверить тестом доменной чистоты.
- **[Компромисс]** Package-private мутаторы дефолтности ограничивают прямые вызовы извне домена; прикладной код должен идти через сервис. → **Митигировать:** задокументировать контракт и покрыть тестами сервис.
- **[Риск]** Неизменяемый `partId` исключает перенос процесса на другую деталь. → **Митигировать:** зафиксировано допущением; перенос — отдельный future change.
- **[Компромисс]** Операции без собственного ID ограничивают будущую привязку норм/оборудования. → **Митигировать:** пересмотр идентичности операции — отдельный change.

## Migration Plan

Миграция данных не требуется: персистентность и Flyway не входят в change. Откат выполняется удалением добавленных типов, доменного сервиса и тестов; существующий скелет модуля `technology` остаётся неизменным, за исключением декларации `technology/package-info.java` (добавление `shared :: shared` в `allowedDependencies`).

## Open Questions

Нет блокирующих вопросов. Потенциально отложенные решения (не влияют на scope/подход текущего change): семантика дефолтности при логическом удалении дефолтного процесса (out-of-scope по решению F6; в текущем change `register`-guard может блокировать добавление нового процесса после удаления дефолтного — принятый известный edge case), переупорядочивание операций, паттерн валидации номера организации, storage-уровневый гарант уникальности номера (обязательный частичный уникальный индекс при появлении персистентности) — относятся к последующим changes.
