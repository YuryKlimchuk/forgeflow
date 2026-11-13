# Tasks

## 1. Типы и клиент расчётов в `api.ts`

- [x] 1.1 Добавить тип `RequirementNeedType = "MANUFACTURE" | "PURCHASE"` и интерфейсы `RequirementItemView` (`type: CatalogItemType`, `id: string`, `number`, `name`, `quantity`, `needType`, `unit`, `sources: { value: string }[]`) и `RequirementCalculationView` (`id: { value: string }`, `planId: { value: string }`, `executedAt: string`, `items: RequirementItemView[]`) по контракту backend (design D2)
- [x] 1.2 Добавить в `planningApi` методы `calculate(planId)` (POST `/api/v1/plans/${planId}/calculations`), `listCalculations(planId)` (GET `/api/v1/plans/${planId}/calculations`) и `getCalculation(planId, calculationId)` (GET `/api/v1/plans/${planId}/calculations/${calculationId}`) через `authFetch`/`toJsonOrThrow` (design D3)

## 2. Обработка ошибок расчёта в `api.ts`

- [x] 2.1 Расширить `ApiErrorBody` необязательными `itemType?: string` и `itemId?: string`; расширить `ApiClientError` необязательными `readonly itemType?: string` и `readonly itemId?: string` с обратной совместимостью конструктора
- [x] 2.2 В `toJsonOrThrow` пробрасывать `body.itemType`/`body.itemId` в `ApiClientError`
- [x] 2.3 Добавить `humanizeRequirementError`: `not_found` → «Запись не найдена», `bom_cycle` → «В спецификации обнаружен цикл», `quantity_overflow` → «Количество позиции превысило допустимое значение», `specification_item_not_found` → «В спецификации отсутствует сборка: {itemId}» (fallback без `itemId` → «В спецификации отсутствует сборка»); неизвестный код → `error.message`, прочее → «Что-то пошло не так» (design D4)
- [x] 2.4 Добавить функции подписей `requirementItemTypeLabel` (PART→«Деталь», ASSEMBLY→«Сборка», BUY→«Покупной», STANDART→«Стандартный», VZK→«ВЗК») и `requirementNeedTypeLabel` (MANUFACTURE→«изготовить», PURCHASE→«закупить») с fallback на исходное значение (design D5)

## 3. Компонент секции расчётов

- [x] 3.1 Создать `frontend/src/RequirementCalculationsSection.tsx` с пропсами `{ planId: string; assemblies: CatalogEntry[] }` и состоянием: история (`calculations`, `historyStatus`, `historyError`), выбранный расчёт (`selected`), запуск (`runStatus`, `runError`) (design D6/D7)
- [x] 3.2 Реализовать загрузку истории при монтировании через `planningApi.listCalculations(planId)`; ошибка чтения → `humanizeRequirementError`; порядок из API не менять; `executedAt` форматировать `new Date(iso).toLocaleString("ru-RU")`
- [x] 3.3 Реализовать запуск: кнопка `disabled` при `runStatus === "running"` с текстом «Расчёт…»/«Рассчитать потребности»; на 201 показать расчёт (`selected = result`) и добавить его в начало истории без перезагрузки; на ошибку — баннер `humanizeRequirementError`, `selected` и история не меняются (design D7)
- [x] 3.4 Реализовать рендер таблицы позиций с колонками Тип/Номер/Наименование/Количество/Вид потребности/Единица/Источники; подписи через `requirementItemTypeLabel`/`requirementNeedTypeLabel`; порядок строк = `selected.items` как из API; `sources` резолвить по `assemblies` с плейсхолдером «Сборка не найдена» для неразрешимого UUID
- [x] 3.5 Реализовать пустой результат: при `selected.items.length === 0` показывать «Позиции отсутствуют» и не рендерить таблицу; баннеры использовать `.banner.banner-error`, состояния `.muted`, таблицу `.catalog-table` (design D8)

## 4. Подключение в `PlanDetail.tsx`

- [x] 4.1 Импортировать `RequirementCalculationsSection` и отрендерить его внутри существующего блока `status === "ready" && plan`, передав `planId={id}` и `assemblies={assemblies}`; секцию «Плоская структура» и остальной UI плана не менять (design D6/D9)

## 5. Тесты API-клиента (`api.test.ts`)

- [x] 5.1 Добавить тесты методов `planningApi.calculate/listCalculations/getCalculation` (метод, путь, отсутствие тела для GET/POST) (AC-02)
- [x] 5.2 Добавить тесты `humanizeRequirementError` для `not_found`, `bom_cycle`, `quantity_overflow` и `specification_item_not_found` (с `itemId` и без) (AC-03, AC-13, AC-14)
- [x] 5.3 Добавить тест проброса `itemType`/`itemId` из тела ошибки в `ApiClientError` через `toJsonOrThrow`

## 6. Тесты секции (`RequirementCalculationsSection.test.tsx`)

- [x] 6.1 Тест успешного запуска: 201 → таблица со всеми полями и русскими подписями «изготовить»/«закупить» и типа (AC-05, AC-09)
- [x] 6.2 Тест истории: загрузка при открытии, порядок из API (новые первыми), человекочитаемое `executedAt`; после запуска история растёт без перезаписи (AC-07, AC-08)
- [x] 6.3 Тест пустого плана: 201 с пустым `items` → «Позиции отсутствуют» и отсутствие таблицы (AC-06)
- [x] 6.4 Тесты четырёх ошибок запуска: 409 `bom_cycle`, 400 `quantity_overflow`, 404 `not_found`, 404 `specification_item_not_found` → баннер, без таблицы, история не меняется (AC-11, AC-12, AC-13, AC-14)
- [x] 6.5 Тест ошибки чтения истории 404 `not_found` → «Запись не найдена» (FR-14)
- [x] 6.6 Тест блокировки кнопки во время запроса («Расчёт…», повторная отправка невозможна) (AC-15)
- [x] 6.7 Тест источников: разрешение UUID в номер/название сборки и плейсхолдер «Сборка не найдена» для неразрешимого UUID (AC-10)
- [x] 6.8 Тест сохранения порядка позиций из API (AC-16)

## 7. Обновление `PlanDetail.test.tsx`

- [x] 7.1 Дополнить существующие сценарии («элементы плана», «плоская структура») моком `GET /api/v1/plans/{id}/calculations` → пустой список, чтобы секция не давала ложный баннер; убедиться, что «Плоская структура» по-прежнему показывает колонки number/name/quantity (AC-04, AC-17)

## 8. Итоговая проверка

- [x] 8.1 Запустить `npm run test` в `frontend/` и убедиться, что все новые и существующие тесты проходят; `git status` показывает только ожидаемые изменения во фронтенде без staging и без изменений backend/`App.tsx`
