# Proposal

## Why

Сейчас полноценный application/REST/UI-срез каталога доступен только для `Part`, поэтому пользователи не могут управлять остальными четырьмя типами изделий через единый интерфейс. Change расширяет существующий проверенный контракт `Part` на весь каталог, сохраняя multi-tenant изоляцию, soft-delete и rich domain model.

## What Changes

- Добавить для `Assembly`, `Buy`, `Standart` и `Vzk` доменный метод `rename` с валидацией имени и обновлением `updatedAt`.
- Расширить repository-порты четырёх типов операциями tenant-scoped чтения и проверки уникальности номера; удалить отдельную операцию `delete` из этих портов.
- Добавить in-memory репозитории четырёх типов с tenant-фильтрацией, исключением удалённых записей и сортировкой по номеру.
- Добавить публичные сервисные порты, DTO, application-сервисы, MapStruct-мапперы и типовые исключения для четырёх типов.
- Добавить REST CRUD-контракты для `/api/v1/assemblies`, `/api/v1/buys`, `/api/v1/standarts` и `/api/v1/vzks`, включая единое отображение ошибок.
- Подключить новые адаптеры в `WebConfig`, не изменяя существующий контракт `Part`.
- Преобразовать frontend в единый экран с вкладками «Детали», «Сборки», «Покупные», «Стандартные», «ВЗК» и generic-клиентом CRUD-операций.
- Добавить backend- и frontend-тесты для новых типов, вкладок, CRUD и tenant-изоляции.

## Capabilities

### New Capabilities

- `catalog-all-types`: application-контракты и поведение управления Assembly, Buy, Standart и Vzk.
- `catalog-rest`: REST-контракты управления четырьмя дополнительными типами каталога.

### Modified Capabilities

- `catalog-domain-model`: добавить `rename` для Assembly, Buy, Standart и Vzk и заменить устаревшие repository-контракты.
- `frontend-catalog`: расширить экран и клиент с Part до пяти типов каталога.
- `part-rest-api`: уточнить общий REST error-handling контракт для новых типизированных исключений без изменения маршрутов Part.

## Impact

Затрагиваются доменные сущности и repository-порты `catalog`, in-memory инфраструктура, `catalog.api`, `catalog.application`, REST-контроллеры и `WebConfig`. Появятся новые HTTP-маршруты, но существующие `/api/v1/parts` сохраняются. Frontend изменит структуру компонентов и API-клиента, сохранив текущие операции Part. Изменений схемы БД и новых внешних зависимостей не требуется.
