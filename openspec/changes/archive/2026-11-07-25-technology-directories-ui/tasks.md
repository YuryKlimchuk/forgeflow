# Tasks

## 1. API-слой (`api.ts`) и его тесты

- [x] 1.1 Добавить в `frontend/src/api.ts` типы `StandardType`, `DirectoryEntry`, `CreateDirectoryRequest`, `UpdateDirectoryRequest`, `EquipmentEntry`, `CreateEquipmentRequest`, `UpdateEquipmentRequest`, `OperationView`, `TechnologyProcessView`, `CreateTechnologyProcessRequest`, `AddOperationRequest` и конфиг `DIRECTORY_TYPES` (materials/sortaments). Верификация: `npm run build` (tsc) успешен, `api.test.ts` компилируется.
- [x] 1.2 Добавить клиенты `directoriesApi` (create/list/get/update/remove через `authFetch`+`toJsonOrThrow`) и `equipmentApi` (create/list/get/update/remove). Верификация: новые тесты в `api.test.ts` проверяют endpoint, method и тело `JSON.stringify` для каждого метода.
- [x] 1.3 Добавить клиент `technologyApi` (create/list/get/rename/remove/addOperation/removeOperation/markDefault/unmarkDefault), включая тело операции с `equipmentId:{value}|null`. Верификация: тесты в `api.test.ts` проверяют все пути (`/api/v1/technology-processes`, `/…/{id}/name`, `/…/{id}/operations`, `/…/{id}/operations/{number}`, `/…/{id}/default`) и method/body.
- [x] 1.4 Добавить `humanizeDirectoriesError` и `humanizeTechnologyError` с сообщениями `duplicate`/`not_found`/`bad_request`/`invalid_state` по design D4. Верификация: юнит-тесты humanize-функций в `api.test.ts` проходят.

## 2. Справочники «Материалы» и «Сортаменты»

- [x] 2.1 Создать `frontend/src/DirectoryCreateForm.tsx` (поля `number`, `name`, select `standardType` ГОСТ/DIN/ISO/Другое, `standard`, `unit`; блокировка при пустых полях; сброс после успеха). Верификация: `DirectoriesSection.test.tsx` проверяет блокировку пустой формы и сброс полей после создания.
- [x] 2.2 Создать `frontend/src/DirectoryDetail.tsx` (загрузка `GET {endpoint}/{id}`, отображение всех полей + `createdAt`/`updatedAt`, редактирование `name/standardType/standard/unit` через `PUT` без `number`, удаление через `confirm` + `DELETE`, кнопка «Назад»). Верификация: `DirectoryDetail.test.tsx` покрывает загрузку, редактирование без изменения `number`, удаление с `window.confirm` (true/false) и возврат к списку.
- [x] 2.3 Создать `frontend/src/DirectoriesSection.tsx` (вкладки «Материалы»/«Сортаменты» по `DIRECTORY_TYPES`, список с колонками номер/наименование/тип стандарта/обозначение/единица, форма создания, открытие детали по клику на строку, «Загрузка…» и баннер ошибок). Верификация: `DirectoriesSection.test.tsx` проверяет `GET /api/v1/materials` и `GET /api/v1/sortaments`, переключение вкладок, отображение строк и переход в деталь.

## 3. Оборудование

- [x] 3.1 Создать `frontend/src/EquipmentCreateForm.tsx` (поля `number`, `name`, `type`; блокировка при пустых полях; сброс после успеха). Верификация: `EquipmentSection.test.tsx` проверяет блокировку пустой формы и сброс после создания.
- [x] 3.2 Создать `frontend/src/EquipmentDetail.tsx` (загрузка `GET /api/v1/equipment/{id}`, отображение полей + `createdAt`/`updatedAt`, редактирование `name/type` через `PUT` без `number`, удаление через `confirm` + `DELETE`, «Назад»). Верификация: `EquipmentDetail.test.tsx` покрывает загрузку, редактирование, удаление с подтверждением и возврат.
- [x] 3.3 Создать `frontend/src/EquipmentSection.tsx` (список с колонками номер/наименование/тип, форма создания, открытие детали, «Загрузка…» и баннер ошибок). Верификация: `EquipmentSection.test.tsx` проверяет `GET /api/v1/equipment`, отображение строк и переход в деталь.

## 4. Техпроцессы

- [x] 4.1 Создать `frontend/src/TechnologyProcessCreateForm.tsx` (поля `number`, `name`, обязательный выбор детали паттерном «поиск + список вариантов» из `GET /api/v1/parts`; отправка `POST /api/v1/technology-processes` с `{number,name,partId:{value}}`). Верификация: `TechnologyProcessSection.test.tsx` проверяет блокировку без детали, выбор детали и тело POST.
- [x] 4.2 Создать `frontend/src/TechnologyProcessSection.tsx` (список техпроцессов `GET /api/v1/technology-processes` с колонками номер/наименование/деталь/«по умолчанию», разрешение детали по `GET /api/v1/parts`, форма создания, открытие детали, «Загрузка…» и баннер ошибок). Верификация: `TechnologyProcessSection.test.tsx` проверяет список, разрешение имени детали и переход в деталь.
- [x] 4.3 Создать `frontend/src/OperationForm.tsx` (поля `number` положительное целое и `name` обязательные, `<select>` оборудования из `GET /api/v1/equipment` с пустой опцией «без оборудования»; отправка `{number,name,equipmentId}` с `null` или `{value}`). Верификация: `TechnologyProcessDetail.test.tsx` проверяет блокировку невалидного номера и тело с `equipmentId:null`/`{value}`.
- [x] 4.4 Создать `frontend/src/TechnologyProcessDetail.tsx` (загрузка `GET /api/v1/technology-processes/{id}`, отображение полей + список операций с оборудованием, переименование `PUT …/{id}/name`, удаление через `confirm` + `DELETE …/{id}`, добавление операции `POST …/{id}/operations`, удаление операции `DELETE …/{id}/operations/{number}`, переключение «по умолчанию» чекбоксом `PUT/DELETE …/{id}/default`, показ `invalid_state`). Верификация: `TechnologyProcessDetail.test.tsx` покрывает все операции и отображение ошибки `invalid_state`.

## 5. Навигация `App.tsx` и интеграция

- [x] 5.1 Расширить `frontend/src/App.tsx`: добавить разделы «Справочники» и «Технология» в шапку, представления `directories`/`technology`, вкладки и рендер `<DirectoriesSection/>`/`<TechnologySection/>` (state-based, без роутера). Верификация: `App.test.tsx` проверяет наличие четырёх кнопок разделов и вкладки каждой секции.
- [x] 5.2 Дополнить `frontend/src/App.test.tsx` сценариями: переход в «Справочники» загружает `/api/v1/materials`, переход в «Технология»/«Оборудование» — `/api/v1/equipment`, «Техпроцессы» — `/api/v1/technology-processes`. Верификация: тесты проходят с моком `globalThis.fetch`.

## 6. Итоговая проверка

- [x] 6.1 Прогнать `npm test` (Vitest) и убедиться, что новые и существующие тесты зелёные. Верификация: команда завершается без ошибок.
- [x] 6.2 Прогнать `npm run build` (`tsc --noEmit && vite build`) и убедиться в отсутствии ошибок типов. Верификация: команда завершается без ошибок.
