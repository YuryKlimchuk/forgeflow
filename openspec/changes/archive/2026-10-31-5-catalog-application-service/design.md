# Design

## Context

Текущее состояние и мотивация описаны в `proposal.md`. Модуль `catalog` уже содержит пять доменных сущностей и порты, но `PartRepository` пока не имеет адаптера, а публичного application-контракта для UI нет. Контекст организации предоставляется через `shared.api.OrganizationContext`; модуль `catalog` может зависеть только от `directories` и `shared`.

## Goals / Non-Goals

**Goals:**

- Реализовать вертикальный срез `Part` в границах `catalog.api`, `catalog.application` и `catalog.infrastructure`.
- Сохранить Rich Domain Model: валидация обязательных строк и `markDeleted()` остаются в домене.
- Обеспечить явную передачу `OrganizationId` от application-сервиса к repository и изоляцию чтения по tenant.
- Изолировать публичный DTO-контракт от доменных объектов и использовать MapStruct только для `Part` → `PartView`.
- Сохранить проверяемую структуру Spring Modulith и покрыть поведение JUnit 5-тестами.

**Non-Goals:**

- REST-контроллеры, Thymeleaf UI и интеграционное постоянное хранилище.
- Application-сервисы для `Assembly`, `Buy`, `Standart` и `Vzk`.
- Новые бизнес-правила сверх уникальности активного `number` в организации, валидации non-blank и soft-delete.

## Decisions

### D1. Публичный контракт в `catalog.api`

`PartService`, `CreatePartRequest`, `PartView` и `PartFilter` размещаются в `catalog.api`; реализация находится в `catalog.application`. Это позволяет Thymeleaf и будущему REST-адаптеру зависеть только от named interface `catalog` и не раскрывает внутренние repository-классы.

Альтернатива — разрешить UI обращаться к application-классам напрямую — отклонена из-за нарушения границы публичного API модуля.

### D2. Явный tenant scope

`PartApplicationService` один раз получает `OrganizationId` через `OrganizationContext.requiredOrganizationId()` для каждой операции и передаёт его в repository-методы. Repository не читает контекст самостоятельно. Контракт `PartRepository` расширяется операциями поиска/списка с явным `OrganizationId`, сохраняя `save` и `delete` как доменный driven port.

Альтернатива — хранить tenant scope внутри in-memory repository — отклонена: она скрыла бы границу безопасности и нарушила существующий паттерн `CatalogOrganizationScopeService` / `OrganizationScopedRepository`.

### D3. In-memory storage и soft-delete

`InMemoryPartRepository` хранит доменные объекты по `PartId` и фильтрует их по переданному `OrganizationId`. Проверка уникальности активного номера выполняется в repository перед сохранением; удалённые записи не участвуют в чтении и не блокируют повторное использование номера, если это не противоречит согласованным правилам. Операция удаления изменяет доменный объект через `markDeleted()` и сохраняет его.

Альтернатива — физически удалять объект — отклонена, поскольку требуется soft-delete и сохранение audit-полей.

### D4. Время и доменная сборка

Сервис создаёт `PartId.generate()` и получает `Instant.now()` один раз для `createdAt` и `updatedAt`. Для тестируемости фиксированное время подаётся через небольшую application-зависимость часов или эквивалентный порт; домен при этом не зависит от Spring. `markDeleted()` сохраняет собственное доменное правило обновления времени.

### D5. MapStruct только для исходящего DTO

MapStruct mapper преобразует `Part` в `PartView`. `CreatePartRequest` не маппится автоматически: сервис вручную передаёт request-поля в factory, поскольку идентификатор, audit timestamps и `OrganizationId` являются системными значениями.

Альтернатива — ручной mapping `Part` → `PartView` — отклонена как несоответствующая согласованному решению и не оставляет единого mapper-контракта для последующих типов.

### D6. Унификация OrganizationId

Factory, поле и accessor всех пяти сущностей используют `shared.api.OrganizationId`. Существующие тесты и вызовы factory обновляются одновременно; сырые `UUID` не адаптируются внутри каталога.

### D7. Зависимость MapStruct

MapStruct добавляется в Maven как compile-time библиотека и annotation processor без runtime-фреймворка. Это сохраняет домен и application-контракт независимыми от Spring и не добавляет отражение в runtime.

## Risks / Trade-offs

- **[Risk]** Текущие repository-порты минимальны и их расширение может затронуть тесты и архитектурные проверки → обновить порт и его тесты в одной задаче, сохранив domain-only зависимости.
- **[Risk]** In-memory storage не моделирует конкурентную запись в производственной БД → ограничить адаптер текущим эталонным срезом и явно покрыть проверку уникальности тестами; постоянное хранилище оставить следующим изменением.
- **[Risk]** `markDeleted()` получает время напрямую из домена, поэтому тесты удаления могут зависеть от диапазона времени → проверять монотонное обновление и ненулевое значение, а фиксированное время применять к create-сценариям.
- **[Risk]** Изменение типа `organizationId` затрагивает все пять сущностей → выполнить поиск всех UUID-based factory/accessor использований и обновить `CatalogEntityTest` до проверки `OrganizationId`.

## Migration Plan

Production-миграция базы данных не требуется: change добавляет in-memory адаптер и не изменяет схему PostgreSQL. При откате удаляются новые API/application/infrastructure-классы и MapStruct-конфигурация; существующие доменные контракты возвращаются только согласованным отдельным изменением, если rollback выполняется до потребителей нового `OrganizationId`.

## Open Questions

Нет: согласованные решения покрывают scope, контракт, tenant-политику, soft-delete, типизацию и тестовые критерии.
