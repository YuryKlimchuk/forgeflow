# Test plan — change 25-technology-directories-ui

> Этап: QA Planning (G4). Независимая оценка тестируемости пакета
> «требования + спецификации + дизайн + задачи» для фронтенда (React 19 + TypeScript + Vite).
> Настоящий план — вход для этапа G7 (фактическая проверка по Test plan). Реализация на этом
> этапе не выполняется.

## 1. Scope & goals

**Что тестируется**

1. API-слой `frontend/src/api.ts`:
   - типы `StandardType`, `DirectoryEntry`, `EquipmentEntry`, `OperationView`,
     `TechnologyProcessView` и запросы создания/обновления (включая `id:{value}`,
     `partId:{value}`, nullable `equipmentId`);
   - клиенты `directoriesApi`, `equipmentApi`, `technologyApi` — endpoint, HTTP-метод, тело
     (`JSON.stringify`), обработка `204 → undefined`;
   - `humanizeDirectoriesError` / `humanizeTechnologyError` — маппинг кодов
     `duplicate`/`not_found`/`bad_request`/`invalid_state` в русские сообщения.

2. Компоненты справочников «Материалы»/«Сортаменты» (`DirectoriesSection`,
   `DirectoryDetail`, `DirectoryCreateForm`): список, создание, деталь, редактирование
   изменяемых полей без `number`, удаление с подтверждением.

3. Компонент «Оборудование» (`EquipmentSection`, `EquipmentDetail`, `EquipmentCreateForm`):
   CRUD с теми же паттернами.

4. Компоненты «Техпроцессы» (`TechnologyProcessSection`, `TechnologyProcessDetail`,
   `TechnologyProcessCreateForm`, `OperationForm`): список с разрешением детали и признаком
   «по умолчанию», создание с обязательным выбором детали, детальная страница с операциями,
   переименование, удаление, добавление/удаление операции, выбор оборудования `<select>`
   (nullable), переключение «по умолчанию» с показом `invalid_state`.

5. Навигация `App.tsx`: разделы «Каталог»/«Справочники»/«Технология»/«Планы» и вкладки
   секций, state-based без роутера, без регресса существующих экранов.

**Цели проверки**

- Подтвердить, что требования R1–R26 и Acceptance Criteria покрыты исполняемыми сценариями.
- Выявить негативные и граничные сценарии (маппинг `standardType`, nested `id:{value}`,
  nullable `equipmentId`, положительное целое номера операции, async-загрузка и сброс
  состояния при навигации).
- Зафиксировать окружение, данные и критерии выхода (G7) для независимой приёмки.

## 2. Test levels

| Уровень | Что проверяет | Инструменты |
|---|---|---|
| **Unit (API/чистые функции)** | `directoriesApi`/`equipmentApi`/`technologyApi` (endpoint/method/body/204), humanize-функции, `standardTypeLabel`, конфиг `DIRECTORY_TYPES` | Vitest, мок `globalThis.fetch` |
| **Component (slice)** | секции списка, формы создания, детальные страницы, форма операции — DOM-состояния, валидация, обработка ошибок | Vitest + @testing-library/react + user-event, jsdom, мок `globalThis.fetch` |
| **App (component/integration)** | навигация разделов/вкладок, аддитивность `App.tsx`, отсутствие лишних fetch | Vitest + testing-library, мок `globalThis.fetch` |
| **Build/type** | отсутствие ошибок типов и сборки | `npm run build` (`tsc --noEmit && vite build`) |

**Не применимо в этом change:** e2e (Playwright/Cypress) — отсутствует в стеке, вне объёма;
Java/бэкенд-тесты — бэкенд не меняется (change 21–24 уже покрыт).

## 3. Test scenarios

Тип сценария: **P** — позитивный, **N** — негативный, **B** — граничный.

### 3.1 API-слой (unit, мок `globalThis.fetch`)

- **S1** (P, unit) `DIRECTORY_TYPES` содержит `[{materials, «Материалы», /api/v1/materials},
  {sortaments, «Сортаменты», /api/v1/sortaments}]` в заданном порядке. → AC-2/AC-3.
- **S2** (P, unit) `directoriesApi.list(endpoint)` отправляет `GET {endpoint}` и возвращает
  типизированный массив с `id:{value}`. → R5/R6/R9.
- **S3** (P, unit) `directoriesApi.get(endpoint, id)` отправляет `GET {endpoint}/{id}`. → R5.
- **S4** (P, unit) `directoriesApi.create(endpoint, {number,name,standardType,standard,unit})`
  отправляет `POST {endpoint}` с телом `{number,name,standardType,standard,unit}`. → AC-4, R10.
- **S5** (P, unit) `directoriesApi.update(endpoint, id, {name,standardType,standard,unit})`
  отправляет `PUT {endpoint}/{id}` с телом `{name,standardType,standard,unit}` и **без** поля
  `number`. → AC-7, R11.
- **S6** (P, unit) `directoriesApi.remove(endpoint, id)` отправляет `DELETE {endpoint}/{id}`.
  → AC-8, R11.
- **S7** (P, unit) `equipmentApi` create/list/get/update/remove отправляют
  `POST /api/v1/equipment` (`{number,name,type}`), `GET /api/v1/equipment`,
  `GET /api/v1/equipment/{id}`, `PUT /api/v1/equipment/{id}` (`{name,type}` без `number`),
  `DELETE /api/v1/equipment/{id}`. → AC-10/AC-11/AC-12, R13–R15.
- **S8** (P, unit) `technologyApi.create` отправляет `POST /api/v1/technology-processes` с телом
  `{number,name,partId:{value}}`. → AC-14, R17.
- **S9** (P, unit) `technologyApi.list` → `GET /api/v1/technology-processes`; `get(id)` →
  `GET /api/v1/technology-processes/{id}`. → R16/AC-13.
- **S10** (P, unit) `technologyApi.rename(id, name)` → `PUT /api/v1/technology-processes/{id}/name`
  с телом `{name}`. → R19.
- **S11** (P, unit) `technologyApi.remove(id)` → `DELETE /api/v1/technology-processes/{id}`.
  → R19.
- **S12** (P, unit) `technologyApi.addOperation(id, {number,name,equipmentId:{value:"e-1"}})`
  → `POST /api/v1/technology-processes/{id}/operations` с телом
  `{number,name,equipmentId:{value:"e-1"}}`. → AC-16, R20.
- **S13** (B, unit) `addOperation` с `equipmentId:null` → тело
  `{number,name,equipmentId:null}` (D7: null, не отсутствие поля). → AC-16, R21.
- **S14** (P, unit) `technologyApi.removeOperation(id, number)` →
  `DELETE /api/v1/technology-processes/{id}/operations/{number}`. → AC-18, R22.
- **S15** (P, unit) `technologyApi.markDefault(id)` → `PUT /api/v1/technology-processes/{id}/default`;
  `unmarkDefault(id)` → `DELETE /api/v1/technology-processes/{id}/default`. → AC-19, R23.
- **S16** (P, unit) все void-методы (`update`/`remove`/`rename`/`removeOperation`/
  `markDefault`/`unmarkDefault`) при `204` возвращают `undefined` и не вызывают `response.json()`.
  → R7/R15/R19/R22/R23.
- **S17** (P, unit) `humanizeDirectoriesError` отображает `duplicate`→«Номер уже занят»,
  `not_found`→«Запись не найдена», `bad_request`→«Некорректный запрос», прочее→`error.message`.
  → AC-20, R8.
- **S18** (P, unit) `humanizeTechnologyError` дополнительно отображает `invalid_state`→
  «Нельзя снять признак „по умолчанию“ с единственного техпроцесса детали». → AC-19, R8/R23.
- **S19** (P, unit) `standardTypeLabel` отображает `GOST`→ГОСТ, `DIN`→DIN, `ISO`→ISO,
  `OTHER`→Другое (A2, D5). → R9/R10/AC-3.

### 3.2 Справочники (DirectoriesSection, component)

Сценарии S20–S30 параметризуются по `DIRECTORY_TYPES` (materials/sortaments) и проверяются для
обоих endpoint (`/api/v1/materials`, `/api/v1/sortaments`).

- **S20** (P) открытие вкладки «Материалы»/«Сортаменты» загружает соответствующий endpoint и
  отображает строки: номер, наименование, тип стандарта (подпись ГОСТ/DIN/ISO/Другое),
  обозначение стандарта, единица. → AC-3, AC-9.
- **S21** (P) переключение вкладки «Материалы» ↔ «Сортаменты» меняет endpoint и сбрасывает
  список к новому. → AC-2, AC-9.
- **S22** (P) пустой список → сообщение об отсутствии записей + форма создания видима. → R24.
- **S23** (P) до завершения запроса отображается «Загрузка…». → R24.
- **S24** (N) ошибка `GET` (не-2xx) → баннер `.banner-error` с humanize-сообщением, приложение
  не падает. → R24.
- **S25** (P) создание: заполнение `number`/`name`/`standardType`/`standard`/`unit` →
  `POST {endpoint}` с корректным телом; после 201 список перезагружается, форма сбрасывается.
  → AC-4, AC-9.
- **S26** (N) блокировка создания: пустое любое из `number`/`name`/`standardType`/`standard`/
  `unit` (по отдельности) → `POST` не вызывается. → AC-5.
- **S27** (N) дубликат номера (`409 duplicate`) → баннер «Номер уже занят», форма не сбрасывается,
  приложение остаётся работоспособным. → AC-20.
- **S28** (P) клик по строке → рендер детальной страницы и `GET {endpoint}/{id}`. → AC-6.

### 3.3 Деталь справочника (DirectoryDetail, component)

- **S29** (P) загрузка `GET {endpoint}/{id}` отображает `number`, `name`, `standardType`
  (подпись), `standard`, `unit`, `createdAt`, `updatedAt`. → AC-6.
- **S30** (P) редактирование `name`/`standardType`/`standard`/`unit` → `PUT {endpoint}/{id}` с
  телом `{name,standardType,standard,unit}` (без `number`); после 204 данные перезагружены.
  → AC-7.
- **S31** (N) сохранение с пустым изменяемым полем → `PUT` не вызывается (или кнопка
  заблокирована). → AC-7, R11.
- **S32** (P) удаление с `window.confirm() === true` → `DELETE {endpoint}/{id}` и возврат к
  списку (`onBack`). → AC-8.
- **S33** (N) удаление с `window.confirm() === false` → `DELETE` не вызывается, остаёмся на
  детали. → AC-8.
- **S34** (N) ошибка `GET {endpoint}/{id}` → баннер, состояние ошибки (без падения). → R24.

### 3.4 Оборудование (EquipmentSection/EquipmentDetail, component)

- **S35** (P) открытие вкладки «Оборудование» загружает `GET /api/v1/equipment`, отображает
  строки: номер, наименование, тип. → AC-10.
- **S36** (P) создание: `number`/`name`/`type` → `POST /api/v1/equipment` `{number,name,type}`;
  список обновляется, форма сбрасывается. → AC-11.
- **S37** (N) блокировка создания при пустом `number`/`name`/`type` → `POST` не вызывается.
  → AC-11.
- **S38** (P) клик по строке → `EquipmentDetail`, `GET /api/v1/equipment/{id}`. → AC-12.
- **S39** (P) деталь отображает все поля + `createdAt`/`updatedAt`. → AC-12.
- **S40** (P) редактирование `name`/`type` → `PUT /api/v1/equipment/{id}` `{name,type}` без
  `number`. → AC-12.
- **S41** (P/N) удаление через `confirm` (true → `DELETE`; false → без `DELETE`). → AC-12.
- **S42** (N) дубликат номера оборудования (`409 duplicate`) → баннер «Номер уже занят».
  → AC-20.

### 3.5 Список и создание техпроцессов (TechnologyProcessSection/TechnologyProcessCreateForm)

- **S43** (P) открытие вкладки «Техпроцессы» загружает `GET /api/v1/technology-processes` и
  `GET /api/v1/parts`; каждая строка показывает номер, наименование, деталь (номер/наименование
  по `partId`) и признак «по умолчанию». → AC-13.
- **S44** (B) если `partId` не найден в загруженных частях → «Элемент не найден» (без раскрытия
  UUID). → R16, риск дизайна.
- **S45** (P) создание: выбор детали паттерном «поиск + список вариантов» из `GET /api/v1/parts`,
  заполнение `number`/`name` → `POST /api/v1/technology-processes` с `{number,name,partId:{value}}`.
  → AC-14.
- **S46** (N) блокировка создания без детали или при пустых `number`/`name` → `POST` не
  вызывается. → AC-14.
- **S47** (N) ошибка списка техпроцессов или частей → баннер, приложение не падает. → R24.

### 3.6 Деталь техпроцесса и операции (TechnologyProcessDetail/OperationForm)

- **S48** (P) загрузка `GET /api/v1/technology-processes/{id}` отображает номер, наименование,
  деталь, признак «по умолчанию», `createdAt`/`updatedAt` и список операций (номер, наименование,
  наименование оборудования или «без оборудования»). Разрешение `equipmentId` через
  `GET /api/v1/equipment`. → AC-15.
- **S49** (B) операция с `equipmentId:null` → «без оборудования» (без раскрытия id). → AC-15, R18.
- **S50** (P) переименование → `PUT /api/v1/technology-processes/{id}/name` `{name}`, новое имя
  отображается. → R19 (см. Finding F1 — отсутствует явный AC).
- **S51** (P) удаление процесса через `confirm` → `DELETE /api/v1/technology-processes/{id}`,
  возврат к списку. → R19.
- **S52** (P) форма операции содержит `<select aria-label="Оборудование">`, наполняемый
  `GET /api/v1/equipment`, с пустой опцией «без оборудования». → AC-17.
- **S53** (P) добавление операции с оборудованием → `POST /api/v1/technology-processes/{id}/operations`
  с телом `{number,name,equipmentId:{value}}`; список операций обновляется. → AC-16, R20.
- **S54** (P) добавление операции «без оборудования» → тело `{number,name,equipmentId:null}`.
  → AC-16, R21.
- **S55** (N) блокировка добавления операции: `number` пусто/`0`/отрицательное/не-целое или
  `name` пусто → `POST` не вызывается. → R20, A6/D9.
- **S56** (B) `number` операции сериализуется числом, а не строкой: тело содержит
  `"number":1`, а не `"number":"1"`. → R20 (int-контракт бэкенда).
- **S57** (P) удаление операции → `DELETE /api/v1/technology-processes/{id}/operations/{number}`,
  список обновляется. → AC-18.
- **S58** (P) установка признака «по умолчанию» (вкл чекбокса) → `PUT /api/v1/technology-processes/{id}/default`,
  после 204 признак установлен. → AC-19.
- **S59** (P) снятие признака (выкл чекбокса) → `DELETE /api/v1/technology-processes/{id}/default`,
  после 204 признак снят. → AC-19.
- **S60** (N) снятие признака с единственного процесса → `409 invalid_state` → баннер с русским
  сообщением; чекбокс отражает серверное состояние (перезагрузка процесса), приложение не падает.
  → AC-19.
- **S61** (N) дубликат номера операции (`409 duplicate`) → баннер «Номер уже занят». → AC-20.

### 3.7 Навигация App.tsx (component/integration)

- **S62** (P) шапка содержит кнопки «Каталог», «Справочники», «Технология», «Планы». → AC-1.
- **S63** (P) клик «Справочники» → раздел с вкладками «Материалы»/«Сортаменты», активная вкладка
  загружает `/api/v1/materials`. → AC-2, AC-3.
- **S64** (P) клик «Технология» → вкладки «Оборудование»/«Техпроцессы»; «Оборудование» загружает
  `/api/v1/equipment`, «Техпроцессы» — `/api/v1/technology-processes`. → AC-2, AC-10, AC-13.
- **S65** (B) только активная секция рендерится и шлёт запросы: переход в «Справочники» не
  вызывает fetch оборудования/техпроцессов, и наоборот (условный рендер по D1). → R1–R4.
- **S66** (P, регресс) существующие разделы «Каталог» и «Планы» сохраняют поведение; существующие
  `App.test.tsx`/`PlanList.test.tsx`/`PlanDetail.test.tsx`/`CatalogDetail`-сценарии зелёные. → R26,
  AC-21.

## 4. Acceptance mapping

| Сценарий | AC | Сценарий | AC |
|---|---|---|---|
| S1–S6 | AC-2, AC-3, AC-4, AC-7, AC-8 | S35–S42 | AC-10, AC-11, AC-12, AC-20 |
| S7 | AC-10, AC-11, AC-12 | S43–S47 | AC-13, AC-14 |
| S8–S16 | AC-13, AC-14, AC-16, AC-18, AC-19 | S48–S51 | AC-15, R18, R19 |
| S17–S19 | AC-3, AC-19, AC-20 | S52–S61 | AC-15, AC-16, AC-17, AC-18, AC-19, AC-20 |
| S20–S28 | AC-2, AC-3, AC-4, AC-5, AC-6, AC-9, AC-20 | S62–S66 | AC-1, AC-2, AC-3, AC-10, AC-13, AC-21 |
| S29–S34 | AC-6, AC-7, AC-8 | | |

**Полнота трассировки по Acceptance Criteria (1–22):**

- AC-1 → S62; AC-2 → S1, S21, S63, S64; AC-3 → S1, S19, S20, S63;
- AC-4 → S4, S25; AC-5 → S26; AC-6 → S28, S29;
- AC-7 → S5, S30, S31; AC-8 → S6, S32, S33; AC-9 → S20, S21, S25;
- AC-10 → S7, S35; AC-11 → S7, S36, S37; AC-12 → S7, S38, S39, S40, S41;
- AC-13 → S9, S43; AC-14 → S8, S45, S46; AC-15 → S48, S49;
- AC-16 → S12, S13, S53, S54; AC-17 → S52; AC-18 → S14, S57;
- AC-19 → S15, S18, S58, S59, S60; AC-20 → S17, S27, S42, S61;
- AC-21 → `npm test` зелёный (Exit criteria); AC-22 → `npm run build` зелёный (Exit criteria).

**Замечание по трассировке:** требование R19 (переименование техпроцесса `PUT .../{id}/name`)
покрыто сценарием S50 и спецификацией `frontend-technology`, но отсутствует в списке Acceptance
Criteria в требованиях — см. **Finding F1**.

## 5. Environment & data

- **Окружение**: `frontend/`; Node + npm; Vitest 5 + jsdom; React 19; TypeScript 7
  (`tsc --noEmit`); `@testing-library/react` + `user-event`.
- **Сборка/тесты**: `npm test` (`vitest run`), `npm run build` (`tsc --noEmit && vite build`).
- **Сеть**: реальных HTTP-запросов нет — `globalThis.fetch` мокается (`vi.spyOn(globalThis,
  "fetch")`); `authFetch` в тестах без Keycloak-инстанса вызывает `fetch` напрямую (токен `null`),
  как в существующих `api.test.ts`/`App.test.tsx`.
- **Тестовые данные**:
  - справочники: `MaterialEntry`/`SortamentEntry` c `id:{value:"m-1"}`, `number:"М-0001"`,
    `standardType:"GOST"|"DIN"|"ISO"|"OTHER"`, `standard`, `unit`, `createdAt`/`updatedAt`;
  - оборудование: `id:{value:"e-1"}`, `number`, `name`, `type`;
  - техпроцесс: `id:{value:"tp-1"}`, `number`, `name`, `partId:{value:"p-1"}`, `defaultProcess`,
    `operations:[{number:10,name,equipmentId:{value:"e-1"}|null}]`;
  - части: `catalogApi.list("/api/v1/parts")` → `CatalogEntry` (с `id.value`, `number`, `name`).
- **Мок-конвенция**: `mockFetch({ "METHOD /url": jsonResponse(...) })` (как в `PlanDetail.test.tsx`)
  для сценариев, где нужно различать несколько запросов; для простых — `mockResolvedValue(...)`.
  В сценариях S43/S48 учитывать, что список техпроцессов и деталь загружают **два** запроса
  (`technology-processes` + `parts`, либо `technology-processes/{id}` + `equipment`).
- **Зависимости**: новые runtime-зависимости не добавляются.

## 6. Risks & assumptions

**Риски (из дизайна и оценки):**

1. **State-based навигация и сброс состояния.** D1 переносит состояние вкладки/списка/детали
   внутрь секций, поэтому переключение раздела (например, «Справочники» → «Каталог» →
   «Справочники») размонтирует секцию и сбрасывает активную вкладку к первой. Это отличается от
   существующего «Каталога», где `activeKey` живёт в `App`. Дизайн не фиксирует ожидаемое
   поведение. Покрывается S63/S64/S65; поведение «сброса» следует зафиксировать тестом или
   поднять состояние вкладки в `App`, если нужно сохранение. → **Finding F2.**
2. **Async-загрузка и race-условия.** Размонтирование секции во время in-flight запроса может
   привести к `setState` на размонтированном компоненте (React 18+ без предупреждений, но это
   нежелательно). `PlanDetail` использует паттерн `cancelled`-флага; новые секции должны его
   повторить для вспомогательных загрузок (parts/equipment). Прямой тест гонки нестабилен —
   фиксируется ревью и статически, не отдельным сценарием.
3. **Маппинг nested `id:{value}`.** Ключи строк и навигация должны использовать `entry.id.value`,
   а не объект; тело — `{value: ...}`. Покрывается S1/S3/S8/S12/S45.
4. **Сериализация `equipmentId`.** Спецификация допускает «null или отсутствие поля», дизайн D7
   фиксирует `null`. Тесты пин-ят `null` (S13/S54). → **Finding F4.**
5. **Положительное целое номера операции.** Клиент валидирует только `> 0` и целочисленность;
   тело должно сериализовать `number` как число (`int`), а не строку (S56). Уникальность и
   существование оборудования — на бэкенде (показываются как `duplicate`/`not_found`).
6. **Разрешение `partId`/`equipmentId`.** При отсутствии соответствия в загруженных справочниках —
   нейтральные «Элемент не найден»/«без оборудования» без раскрытия UUID (S44/S49). Поведение
   описано в рисках дизайна, но не в спецификациях — рекомендуется добавить сценарий. → **Finding F5.**
7. **Двойной запрос `GET /api/v1/equipment`.** `TechnologyProcessDetail` может грузить оборудование
   дважды: для разрешения имён операций и для `<select>` в `OperationForm`. Не нарушает
   корректность, но тесты должны быть устойчивы к одному/двум вызовам; при реализации желательно
   переиспользовать один список. → **Finding F6.**
8. **Регресс существующих экранов.** `App.tsx` меняется аддитивно; существующие тесты должны
   остаться зелёными (S66), `tsc --noEmit` ловит ошибки типов. Рендер секций условный (D1), поэтому
   новые fetch не возникают без перехода (S65).
9. **Начальное значение `standardType`.** D5 не фиксирует начальное состояние select в форме
   создания; для проверки AC-5 («блокировка при пустом standardType») форма должна стартовать с
   пустой опцией «— выберите тип —». → **Finding F3.**

**Допущения (A1–A10 приняты как есть):**

- A1 (структура меню), A2 (русские подписи и значения standardType), A3 (минимальный MVP без
  пагинации/фильтра/поиска в справочниках/графики), A4 (выбор детали паттерном «поиск + список»,
  оборудование — нативный `<select>`), A5 (инвариант «по умолчанию» не перереализуется на клиенте),
  A6 (номер операции — только `> 0` и целочисленность на клиенте), A7 (неизменяемые `number`),
  A8 (техпроцесс привязывается к детали, список из `GET /api/v1/parts`), A9 (только `frontend/src`,
  без новых runtime-зависимостей), A10 (материалы/сортаменты — один переиспользуемый компонент).

## 7. Entry / exit criteria

**Entry (условия начала G7):**

- Пакет «дизайн + Test plan» утверждён пользователем (G4).
- Findings F1 (явный AC на переименование) зафиксирован/закрыт; F2–F6 приняты или закрыты
  (см. раздел Findings).
- Реализация завершена (G5) и код-ревью пройдено (G6).

**Exit (G7 pass criteria):**

- `npm test` зелёный: все новые (`api.test.ts`, `DirectoriesSection.test.tsx`,
  `DirectoryDetail.test.tsx`, `EquipmentSection.test.tsx`, `EquipmentDetail.test.tsx`,
  `TechnologyProcessSection.test.tsx`, `TechnologyProcessDetail.test.tsx`) и существующие
  (`App.test.tsx`, `PlanList.test.tsx`, `PlanDetail.test.tsx`, `SpecificationDetail.test.tsx`)
  тесты проходят.
- `npm run build` зелёный: `tsc --noEmit` без ошибок типов и `vite build` успешен.
- Все сценарии S1–S66 выполнены; для каждого негативного сценария подтверждён ожидаемый
  endpoint/method/body или отсутствие запроса, а не только «не упало».
- Маппинг `standardType` (S19) и сериализация `equipmentId`/`number` операции (S13/S54/S56)
  проверены на точное тело запроса.
- Существующие экраны «Каталог»/«Планы» не сломаны (регресс S66).
- Нецелевые артефакты отсутствуют: пагинация, серверная фильтрация/поиск в справочниках,
  графические представления, редактирование существующей операции, клиентская реализация
  инварианта «по умолчанию» — не реализованы.
