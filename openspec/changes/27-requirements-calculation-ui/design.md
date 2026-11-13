# Design

## Context

Мотивация и границы — в `proposal.md`. Поведение — в `specs/frontend-planning-requirements`.

Backend уже реализован (change 26): `RequirementCalculationController` отдаёт `POST/GET /api/v1/plans/{planId}/calculations` и `GET .../calculations/{calculationId}`. Представление `RequirementCalculationView(id, planId, executedAt, items)` сериализуется как `{"id":{"value":"..."},"planId":{"value":"..."},"executedAt":"<ISO>","items":[...]}`; `RequirementItemView(type, id, number, name, quantity, needType, unit, sources)` — `type` ∈ `PART/ASSEMBLY/BUY/STANDART/VZK`, `needType` ∈ `MANUFACTURE/PURCHASE`, `unit` = `шт`, `sources` = массив `AssemblyId`, сериализуемый как `[{"value":"<uuid>"}]`. `list` возвращает полные представления с `items`, отсортированные по `executedAt` убыванию (`findByOrganizationIdAndPlanIdOrderByExecutedAtDesc`).

Актуальный фронтенд: `frontend/src/api.ts` уже содержит `CatalogItemType` (`PART/ASSEMBLY/BUY/STANDART/VZK`), `ApiErrorBody {error, message}`, `ApiClientError(status, code, message)`, `toJsonOrThrow`, `authFetch`, `planningApi` (CRUD планов + `flat`) и семейство `humanize*Error`. `PlanDetail.tsx` загружает план и список сборок `assemblies` (для резолва элементов плана), рендерит таблицу элементов плана, форму добавления и секцию «Плоская структура». Стилевые конвенции: `.catalog-table`, `.banner.banner-error`, `.muted`, `.actions`; даты форматируются `new Date(iso).toLocaleString("ru-RU")`.

## Goals / Non-Goals

**Goals:**

- Встроить запуск и просмотр расчётов потребностей в `PlanDetail` без новых маршрутов и без изменения backend.
- Типизировать контракт расчёта и клиентские методы по фактическому контракту backend.
- Человекочитаемые русские сообщения для четырёх кодов ошибок расчёта, включая идентификатор проблемной ссылки.
- Сохранить неизменным существующий UI «Плоская структура» и страницы планов.

**Non-Goals (design-level):**

- Не менять backend-контракт, расчётную логику, `App.tsx` и основные OpenSpec-спеки.
- Не вводить маршрутизацию/роутер для истории расчётов.
- Не реализовывать экспорт, документы и учёт остатков.

## Decisions

### D1. Capability и граница spec

Вводится одна новая capability `frontend-planning-requirements` (дельта `specs/frontend-planning-requirements/spec.md`), повторяющая конвенцию `frontend-*` и зеркалящая backend `planning-requirements`. Существующие capabilities не модифицируются: отдельного spec про фронтенд планов ранее не было, а существующий UI планов/«Плоской структуры» не меняется (это non-goal, а не изменение требований).

### D2. Типы контракта в `api.ts`

Переиспользуется существующий `CatalogItemType`. Добавляются:

```ts
export type RequirementNeedType = "MANUFACTURE" | "PURCHASE";

export interface RequirementItemView {
  type: CatalogItemType;
  id: string;
  number: string;
  name: string;
  quantity: number;
  needType: RequirementNeedType;
  unit: string;
  sources: { value: string }[];
}

export interface RequirementCalculationView {
  id: { value: string };
  planId: { value: string };
  executedAt: string;
  items: RequirementItemView[];
}
```

`id` позиции — строка (UUID), `sources` — массив `{value}` (соответствует сериализации `List<AssemblyId>`), `executedAt` — ISO-строка `Instant`. Дополнительных полей не вводится (FR-01).

### D3. Методы `planningApi`

В существующий объект `planningApi` добавляются:

- `calculate(planId: string): Promise<RequirementCalculationView>` → `POST /api/v1/plans/${planId}/calculations`;
- `listCalculations(planId: string): Promise<RequirementCalculationView[]>` → `GET /api/v1/plans/${planId}/calculations`;
- `getCalculation(planId: string, calculationId: string): Promise<RequirementCalculationView>` → `GET /api/v1/plans/${planId}/calculations/${calculationId}`.

Все — через `authFetch` + `toJsonOrThrow`, без тела для GET/POST (расчёт запускается без параметров). `getCalculation` реализуется для полноты типизированного клиента и покрывается тестами API-клиента (AC-02); UI в текущем варианте использует полные данные из `listCalculations` (список возвращает `items`) и раскрывает выбранный расчёт из локального состояния без дополнительного запроса. Это фиксируется осознанно: если в будущем `list` станет сводным, `getCalculation` уже готов.

### D4. Обработка ошибок и Q-02 (идентификатор проблемной ссылки)

**Проверка фактического тела ошибки:** `RestExceptionHandler.handleSpecificationItemNotFound` уже возвращает структурированное тело:

```json
{ "error": "specification_item_not_found", "message": "...", "itemType": "ASSEMBLY", "itemId": "<uuid>" }
```

Поля `itemType`/`itemId` берутся из `e.item().type()` / `e.item().id()`. Следовательно, **backend-изменение не требуется**, и извлекать идентификатор из `message` не нужно.

Решение:

- `ApiErrorBody` дополняется необязательными полями `itemType?: string; itemId?: string;` (остальные error-тела их просто не содержат).
- `ApiClientError` получает необязательные `readonly itemType?: string` и `readonly itemId?: string`; конструктор остаётся обратно совместимым (существующие вызовы с тремя аргументами не ломаются).
- `toJsonOrThrow` при разборе ошибки пробрасывает `body.itemType` и `body.itemId` в `ApiClientError`.
- `humanizeRequirementError` для `specification_item_not_found` возвращает `«В спецификации отсутствует сборка: {itemId}»`, если `itemId` задан, иначе fallback `«В спецификации отсутствует сборка»`.

Прочие коды: `not_found` → «Запись не найдена», `bom_cycle` → «В спецификации обнаружен цикл», `quantity_overflow` → «Количество позиции превысило допустимое значение». Неизвестный код → `error.message`; не-`ApiClientError` → «Что-то пошло не так» (конвенция существующих `humanize*Error`).

### D5. Q-01 — русские подписи типов и вида потребности

Принимаются предложенные подписи: `PART` → «Деталь», `ASSEMBLY` → «Сборка», `BUY` → «Покупной», `STANDART` → «Стандартный», `VZK` → «ВЗК»; `needType`: `MANUFACTURE` → «изготовить», `PURCHASE` → «закупить».

Добавляются чистые функции-подписи в `api.ts` (по образцу `standardTypeLabel`) для тестируемости без рендера:

```ts
export function requirementItemTypeLabel(type: CatalogItemType): string { ... }
export function requirementNeedTypeLabel(needType: RequirementNeedType): string { ... }
```

Для неизвестного значения функция возвращает исходное значение (защита от будущих типов, как в `standardTypeLabel`).

### D6. Компонентная структура

Вводится отдельный компонент `frontend/src/RequirementCalculationsSection.tsx` с пропсами `{ planId: string; assemblies: CatalogEntry[] }`. Он владеет всей логикой расчётов и не смешивается с существующей логикой плана; `PlanDetail.tsx` лишь импортирует и рендерит его внутри уже существующего блока `status === "ready" && plan`, передавая `id` и уже загруженный `assemblies`.

Пропс `assemblies` переиспользует существующий список сборок (тот же, что резолвит элементы плана): источники позиций резолвятся по `entry.id.value === source.value`, без дополнительных запросов. Неразрешимый UUID → «Сборка не найдена» (FR-11).

Альтернатива — держать состояние в `PlanDetail` — отклонена: это раздуло бы и без того насыщенный компонент и усложнило бы изолированные тесты секции.

### D7. Состояние и поток данных секции

Состояние секции:

- `calculations: RequirementCalculationView[] | null` — история;
- `historyStatus: "idle" | "loading" | "ready" | "error"` и `historyError: string | null`;
- `selected: RequirementCalculationView | null` — раскрытый расчёт;
- `runStatus: "idle" | "running"` и `runError: string | null` — запуск.

При монтировании выполняется `planningApi.listCalculations(planId)` → `calculations` (порядок из API, новые первыми, без клиентской сортировки). Ошибка истории → `historyError = humanizeRequirementError(e)`.

Запуск: `handleCalculate` ставит `runStatus = "running"`, очищает `runError`; на 201 — `setSelected(result)` и `setCalculations(prev => [result, ...(prev ?? [])])` (история растёт, не перезаписывается — AC-08; новые первыми совпадает с порядком backend). На ошибку — `runError = humanizeRequirementError(e)`, `selected` и `calculations` не меняются (FR-13). `finally` — `runStatus = "idle"`.

Выбор из истории: `setSelected(calculation)` — рендер таблицы из `selected.items` (полные данные уже есть в списке). Пустой `selected.items` → «Позиции отсутствуют», таблица не рендерится (FR-06).

### D8. Разметка и подписи

Секция использует существующие классы: заголовок `<h3>Расчёты потребностей</h3>`, кнопка запуска, баннеры `.banner.banner-error`, состояние `.muted`, таблица `.catalog-table`, кнопки действий `.actions`. Таблица позиций имеет колонки: Тип, Номер, Наименование, Количество, Вид потребности, Единица, Источники. `executedAt` форматируется `new Date(iso).toLocaleString("ru-RU")` (конвенция проекта). Порядок строк — `selected.items` как есть (FR-12).

Кнопка запуска: `disabled={runStatus === "running"}`, текст `{runStatus === "running" ? "Расчёт…" : "Рассчитать потребности"}` (FR-15).

### D9. Видимость секции

Секция рендерится только внутри существующего условия `status === "ready" && plan` в `PlanDetail` (FR-04): при загрузке плана или ошибке плана секции нет. При этом она должна быть видна и при пустом плане (`plan.items.length === 0`), поэтому кнопка запуска остаётся активной для пустого плана.

### D10. Тестовая стратегия

- `api.test.ts` (расширение): методы `planningApi.calculate/listCalculations/getCalculation` (пути, метод, тела), `humanizeRequirementError` для всех четырёх кодов (включая `specification_item_not_found` с `itemId` и без), проброс `itemType/itemId` из тела ошибки через `ApiClientError`.
- Новый `RequirementCalculationsSection.test.tsx` (Vitest + Testing Library, мок `fetch`): успешный запуск с таблицей всех полей и русскими подписями; история с порядком из API и временем выполнения; раскрытие выбранного расчёта; пустой план → «Позиции отсутствуют» без пустой таблицы; все четыре ошибки запуска → баннер, история не меняется; ошибка истории 404 → «Запись не найдена»; блокировка кнопки во время запроса; разрешение источников + «Сборка не найдена».
- `PlanDetail.test.tsx`: существующие сценарии («элементы плана», «плоская структура») дополняются моками `GET /api/v1/plans/{id}/calculations` (пустой список), чтобы секция не давала ложный баннер; добавляется проверка, что «Плоская структура» работает как раньше (AC-04, AC-17).
- `npm run test` (vitest run) проходит со всеми новыми кейсами (AC-18).

## Risks / Trade-offs

- [Расширение `ApiErrorBody`/`ApiClientError`] — необязательные поля сохраняют обратную совместимость, но `ApiClientError` получает дополнительные поля. **Митигирование:** поля опциональны, существующие конструкторы не меняются; покрыто тестами `api.test.ts`.
- [История раскрывается из локального списка, а не через `getCalculation`] — список уже возвращает полные `items`, поэтому отдельный запрос не нужен; при будущем изменении контракта списка UI потребует доработки. **Митигирование:** `getCalculation` реализован и покрыт тестами, зафиксировано в D3.
- [Неразрешимый источник] — если головная сборка плана была удалена после создания плана, источник не резолвится. **Митигирование:** плейсхолдер «Сборка не найдена» без падения рендера (FR-11).
- [Изменение `PlanDetail.test.tsx`] — добавление мока расчётов в существующие сценарии немного расширяет существующий тест. **Митигирование:** мок — пустой список, поведение исходных сценариев не меняется (AC-17).
- [Формат `executedAt` зависит от локали браузера] — `toLocaleString("ru-RU")` принят во всех существующих экранах. **Митигирование:** зафиксирована локаль `ru-RU`, как в проекте.

## Migration Plan

1. Дополнить `api.ts`: типы, `RequirementNeedType`, методы `planningApi`, подписи, `humanizeRequirementError`, расширить `ApiErrorBody`/`ApiClientError` и `toJsonOrThrow`.
2. Добавить `RequirementCalculationsSection.tsx` и подключить его в `PlanDetail.tsx` (в блок `ready`).
3. Добавить/обновить тесты (`api.test.ts`, `RequirementCalculationsSection.test.tsx`, `PlanDetail.test.tsx`).
4. Прогнать `npm run test` в `frontend/`; убедиться, что существующие тесты зелёные.

Rollback: удалить компонент секции, откатить изменения `api.ts`/`PlanDetail.tsx` и тестов; backend и роутинг не затрагиваются.

## Open Questions

Нет блокирующих. Решения Q-01 (подписи типов) и Q-02 (идентификатор ссылки из структурированного тела ошибки backend, без backend-изменений) зафиксированы в D4/D5 и согласуются с утверждёнными FR/AC.
