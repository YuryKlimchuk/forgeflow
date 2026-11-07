# Design

## Context

Бэкенд `directories` и `technology` уже опубликован (change 21–24): `MaterialView`/`SortamentView` (`id:{value}`, `number`, `name`, `standardType` enum `GOST|DIN|ISO|OTHER`, `standard`, `unit`, `createdAt`, `updatedAt`), `EquipmentView` (`id`, `number`, `name`, `type`, `createdAt`, `updatedAt`), `TechnologyProcessView` (`id`, `number`, `name`, `partId`, `defaultProcess`, `createdAt`, `updatedAt`, `operations:OperationView[]`), `OperationView` (`number:int`, `name`, `equipmentId:{value}|null`). Эндпоинты и статусы: материалы/сортаменты/оборудование — CRUD (POST 201, GET 200, GET/{id} 200, PUT/{id} 204, DELETE/{id} 204); техпроцессы — POST 201, GET 200 (опц. `?partId=`), GET/{id} 200, PUT/{id}/name 204, DELETE/{id} 204, POST/{id}/operations 201, DELETE/{id}/operations/{number} 204, PUT/{id}/default 204, DELETE/{id}/default 204. Ошибки `{error,message}`: `duplicate` (409), `not_found` (404), `bad_request` (400), `invalid_state` (409).

Существующий фронтенд: `api.ts` (`authFetch`, `toJsonOrThrow`, `ApiClientError`, клиенты `catalogApi`/`planningApi`/`specificationApi`, humanize-функции), `App.tsx` (state-based `view`, шапка «Каталог»/«Планы», вкладки `.tab`/`.tab-active`), эталонные компоненты (`CatalogTable`, `CatalogDetail`, `CreateCatalogEntryForm`, `PlanList`, `PlanDetail`, `SpecificationDetail`), тесты с моком `globalThis.fetch`. Требования R1–R26 и допущения A1–A10 зафиксированы в `/tmp/forgeflow-25-ui-requirements.md`.

## Goals / Non-Goals

**Goals:**
- Добавить UI для справочников, оборудования и техпроцессов строго поверх существующих REST-контрактов, без изменений бэкенда.
- Сохранить единый стиль state-based навигации, состояний загрузки/ошибок и тестирования.
- Переиспользовать материалы и сортаменты одним конфигурируемым компонентом (A10).
- Точный маппинг DTO → TS-типы, включая `id:{value}`/`partId:{value}` и nullable `equipmentId`.

**Non-Goals (design-level):**
- Пагинация, серверная фильтрация, поиск в списках справочников/оборудования, графические представления.
- Роутер/`react-router` и любые новые runtime-зависимости.
- Редактирование существующей операции или смена `equipmentId` существующей операции.
- Клиентская реализация инварианта «процесс по умолчанию» (только вызов API и показ `invalid_state`).

## Decisions

### D1. Секции владеют внутренним списком/деталью, `App.tsx` — только разделы и вкладки
Добавляются два корневых представления `directories` и `technology` в union `View` в `App.tsx`; `App.tsx` лишь добавляет кнопки навигации и рендерит `<DirectoriesSection/>` / `<TechnologySection/>`. Внутреннее состояние списка, активной вкладки и выбранной записи (список ↔ деталь) живёт в компонентах секций. Альтернатива (поднимать `selectedEntry`/вкладки в `App.tsx`, как у «Каталога») отвергнута: она раздувает `App.tsx` и не нужна, так как ни один новый экран не является общей точкой навигации для других секций. Существующие `CatalogDetail`/`PlanDetail` не трогаются.

### D2. Материалы и сортаменты — один конфигурируемый компонент
В `api.ts` вводится конфиг `DIRECTORY_TYPES: [{key:"materials",label:"Материалы",endpoint:"/api/v1/materials"},{key:"sortaments",label:"Сортаменты",endpoint:"/api/v1/sortaments"}]`, а UI реализуется компонентами `DirectoriesSection` (вкладки + список + форма создания + открытие детали), `DirectoryDetail` и `DirectoryCreateForm`, параметризуемыми через `DirectoryTypeConfig`. Поля материалов и сортаментов идентичны (A10, R9–R12), поэтому отдельные пары компонентов не создаются. Альтернатива (дублировать код) отвергнута как источник расхождений.

### D3. Точные TS-типы и клиенты в `api.ts`
- `StandardType = "GOST" | "DIN" | "ISO" | "OTHER"`.
- `DirectoryEntry` (общий для материала и сортамента, JSON-форма идентична): `{ id:{value:string}; number:string; name:string; standardType:StandardType; standard:string; unit:string; createdAt:string; updatedAt:string }`; `CreateDirectoryRequest = { number,name,standardType,standard,unit }`; `UpdateDirectoryRequest = { name,standardType,standard,unit }`.
- `EquipmentEntry = { id:{value}; number; name; type; createdAt; updatedAt }`; `CreateEquipmentRequest = { number,name,type }`; `UpdateEquipmentRequest = { name,type }`.
- `OperationView = { number:number; name:string; equipmentId:{value:string}|null }`; `TechnologyProcessView = { id:{value}; number; name; partId:{value}; defaultProcess:boolean; createdAt; updatedAt; operations:OperationView[] }`; `CreateTechnologyProcessRequest = { number; name; partId:{value} }`; `AddOperationRequest = { number:number; name:string; equipmentId:{value:string}|null }`.
- Клиенты: `directoriesApi` (create/list/get/update/remove, endpoint-параметризованный, по образцу `catalogApi`), `equipmentApi` (create/list/get/update/remove), `technologyApi` (create/list/get/rename/remove/addOperation/removeOperation/markDefault/unmarkDefault). Все вызовы — через `authFetch` + `toJsonOrThrow`; 204 возвращает `undefined`.

### D4. Humanize-ошибки отдельными функциями
Добавляются `humanizeDirectoriesError` и `humanizeTechnologyError` (обе опираются на `ApiClientError`). Точные сообщения:
- `duplicate` → «Номер уже занят»;
- `not_found` → «Запись не найдена»;
- `bad_request` → «Некорректный запрос»;
- `invalid_state` (только `humanizeTechnologyError`) → «Нельзя снять признак „по умолчанию“ с единственного техпроцесса детали»;
- прочее → `error.message`.
Существующие `humanizeError`/`humanizePlanError`/`humanizeSpecificationError` не изменяются.

### D5. `standardType` — `<select>` с русскими подписями
Select отображает ГОСТ/DIN/ISO/Другое и сохраняет значения `GOST`/`DIN`/`ISO`/`OTHER` (A2). Отображение в таблице/детали использует ту же подпись через helper `standardTypeLabel`.

### D6. Выбор детали — паттерн «поиск + список вариантов», оборудование — нативный `<select>`
Выбор детали при создании техпроцесса повторяет `PlanDetail` (input поиска по `number`/`name` + список `.element-options` + выбранный элемент `.selected-element` со «Сменить»), данные — `catalogApi.list("/api/v1/parts")` (A4, A8). Оборудование операции — нативный `<select aria-label="Оборудование">` с пустой опцией `value=""` «без оборудования» и опциями `value={equipment.id.value}` из `GET /api/v1/equipment` (A4, R21). При `""` в теле отправляется `equipmentId: null`.

### D7. Сериализация `equipmentId`
`addOperation` строит тело `{ number, name, equipmentId: selected ? { value: selected } : null }`. Backend `AddOperationRequest.equipmentId` — nullable `EquipmentId`, поэтому `null` корректно десериализуется; выбор «без оборудования» однозначно даёт `null` (R21). Альтернатива «опустить поле» отвергнута как менее явная.

### D8. Признак «по умолчанию» — чекбокс с вызовом API
Детальная страница техпроцесса отображает `<input type="checkbox" aria-label="По умолчанию" checked={defaultProcess}>`; изменение вызывает `technologyApi.markDefault(id)` (PUT) при установке и `unmarkDefault(id)` (DELETE) при снятии, после 204 перезагружает процесс (A5, R23). Ошибка `invalid_state` показывается через `humanizeTechnologyError` в баннере, признак пересчитывается по ответу сервера (без локальной имитации инварианта).

### D9. Валидация операции только «не пусто и положительное целое»
Форма операции требует `name` непустым и `number` как положительное целое (`Number(number)` целое и `> 0`, A6). Уникальность номера операции и существование оборудования остаются на бэкенде и отображаются как ошибки API (`duplicate`/`not_found`).

### D10. Компоненты и файлы
Новые файлы в `frontend/src/`:
- `DirectoriesSection.tsx`, `DirectoryDetail.tsx`, `DirectoryCreateForm.tsx`;
- `EquipmentSection.tsx`, `EquipmentDetail.tsx`, `EquipmentCreateForm.tsx`;
- `TechnologyProcessSection.tsx`, `TechnologyProcessDetail.tsx`, `TechnologyProcessCreateForm.tsx` (форма создания с выбором детали), `OperationForm.tsx` (форма добавления операции с `<select>` оборудования);
- изменения в `api.ts` и `App.tsx`.

### D11. Тесты — Vitest + testing-library, мок `globalThis.fetch`
Новые тесты: `api.test.ts` расширяется блоками `directoriesApi`/`equipmentApi`/`technologyApi` и humanize-функций; новые `DirectoriesSection.test.tsx`, `DirectoryDetail.test.tsx`, `EquipmentSection.test.tsx`, `EquipmentDetail.test.tsx`, `TechnologyProcessSection.test.tsx`, `TechnologyProcessDetail.test.tsx`; `App.test.tsx` дополняется сценариями навигации разделов/вкладок. Проверяются endpoint, method, body (`JSON.stringify`), а также пользовательские сценарии (создание, деталь, редактирование, удаление с `window.confirm`, ошибки). Критерии: `npm test` и `npm run build` зелёные.

## Risks / Trade-offs

- [Рассинхрон `partId`/`equipmentId` с загруженными справочниками] → ссылки разрешаются по `id` из `GET /api/v1/parts` и `GET /api/v1/equipment`; при отсутствии соответствия показывается нейтральное «Элемент не найден»/«без оборудования», UUID не раскрывается (конвенция `frontend-catalog`).
- [Дублирование CRUD-логики между справочниками и оборудованием] → материалы/сортаменты параметризованы конфигом (D2); оборудование имеет другие поля и остаётся отдельным небольшим компонентом; преждевременная общая абстракция не вводится.
- [Согласованность русских подписей] → подписи стандартов и сообщений фиксируются в D4/D5 и покрываются тестами; изменение подписи не ломает REST-тела.
- [Риск регрессии существующих экранов] → `App.tsx` меняется аддитивно (только новые кнопки/представления); существующие тесты `App.test.tsx` остаются и дополняются, `npm run build` (`tsc --noEmit`) ловит ошибки типов.
- [Ошибка `invalid_state` при снятии признака] → показывается баннером без сброса всего приложения; состояние процесса перезагружается после любой ошибки переключения, чтобы чекбокс отражал серверную истину.
- [Положительное целое для номера операции] → клиент проверяет только `> 0` и целочисленность; все прочие правила — на бэкенде (A6), ошибки отображаются.

## Migration Plan

1. Расширить `api.ts` (типы, конфиги, клиенты, humanize-функции) и покрыть их в `api.test.ts`.
2. Реализовать `DirectoriesSection`/`DirectoryDetail`/`DirectoryCreateForm` и тесты.
3. Реализовать `EquipmentSection`/`EquipmentDetail`/`EquipmentCreateForm` и тесты.
4. Реализовать `TechnologyProcessSection`/`TechnologyProcessDetail`/`TechnologyProcessCreateForm`/`OperationForm` и тесты.
5. Расширить `App.tsx` (разделы/вкладки), дополнить `App.test.tsx`.
6. Прогнать `npm test` и `npm run build`.

Rollback: изменения локализованы во `frontend/src` и аддитивны; откат — возврат файлов фронтенда. Бэкенд и данные не затрагиваются.

## Open Questions

Нет: требования R1–R26 и допущения A1–A10 закрывают неопределённости; блокирующих вопросов для проектирования не осталось.
