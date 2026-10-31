# Proposal

## Why

Модуль `catalog` уже содержит доменные сущности и порты, но UI не имеет публичного application-контракта для работы с деталями (`Part`). Нужен первый вертикальный срез каталога с контролируемой multi-tenant изоляцией, soft-delete и проверяемыми правилами, чтобы последующие типы каталога могли следовать тому же API-паттерну.

## What Changes

- Добавить публичный `catalog.api` контракт `PartService` и DTO-рекорды для создания, чтения и фильтрации деталей.
- Добавить application-реализацию полного CRUD-среза `Part`: create, get, list, rename и soft-delete.
- Добавить application-исключения каталога для отсутствующей детали и дубликата номера.
- Добавить in-memory реализацию `PartRepository` с явной передачей `OrganizationId`, tenant-изоляцией, уникальностью номера и исключением удалённых записей из чтения.
- Использовать MapStruct для преобразования доменной `Part` в `PartView`.
- Перевести `organizationId` во всех пяти доменных сущностях каталога и их factory-методах с `UUID` на `shared.api.OrganizationId`.
- Добавить зависимость и annotation processor MapStruct в Maven-конфигурацию.
- Добавить обязательные unit-, repository-, mapper- и архитектурные тесты.
- Не добавлять REST-контроллеры, Thymeleaf UI или application-сервисы для `Assembly`, `Buy`, `Standart`, `Vzk`.

## Capabilities

### New Capabilities

- `catalog-part-application`: Публичный application-контракт и поведение полного CRUD-среза для деталей каталога с multi-tenant изоляцией и soft-delete.

### Modified Capabilities

- `catalog-domain-model`: Тип `organizationId` пяти сущностей каталога изменяется с `UUID` на `shared.api.OrganizationId`.

## Impact

Изменения затронут `catalog.api`, `catalog.application`, `catalog.infrastructure`, доменные сущности и repository port `Part`, все пять factory-контрактов каталога, существующие тесты и Maven `pom.xml`. Межмодульная зависимость останется ограниченной разрешёнными `shared` и `directories`; REST и UI не затрагиваются.
