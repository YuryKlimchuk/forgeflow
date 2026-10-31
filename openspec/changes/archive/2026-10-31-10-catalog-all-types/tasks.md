# Tasks

## 1. Домен и repository-порты

- [x] 1.1 Добавить `rename(String)` в `Assembly`, `Buy`, `Standart` и `Vzk` с non-blank валидацией и обновлением `updatedAt`; проверить unit-тестами валидные и невалидные имена.
- [x] 1.2 Привести `AssemblyRepository`, `BuyRepository`, `StandartRepository` и `VzkRepository` к контракту `save`, `findById`, `findAll`, `existsByNumber` с `OrganizationId`; проверить компиляцией отсутствие вызовов удалённого `delete`.
- [x] 1.3 Реализовать in-memory репозитории для четырёх типов с tenant-фильтрацией, исключением deleted-записей и сортировкой по number; покрыть unit-тестами сохранение, поиск, duplicate и изоляцию организаций.

## 2. Публичные API и application-сервисы

- [x] 2.1 Создать для каждого типа `XxxView`, `CreateXxxRequest`, `XxxFilter` и `XxxService` в `catalog.api`; проверить record-контракты и доступность только через API-пакеты.
- [x] 2.2 Создать `XxxMapper`, `XxxApplicationService`, `XxxNotFoundException` и `DuplicateXxxNumberException` для Assembly, Buy, Standart и Vzk по образцу Part; проверить unit-тестами create/get/list/rename/delete и tenant-передачу.
- [x] 2.3 Проверить обработку duplicate number, удалённых/чужих записей и сортировку списков в application-тестах каждого сервиса; выполнить `mvn test` для соответствующих тестов.

## 3. REST и composition root

- [x] 3.1 Добавить тонкие контроллеры `/api/v1/assemblies`, `/api/v1/buys`, `/api/v1/standarts`, `/api/v1/vzks` для GET/POST/PUT `/{id}/name`/DELETE; проверить MVC-тестами статусы 200/201/204 и JSON.
- [x] 3.2 Расширить `RestExceptionHandler` маппингом четырёх пар исключений на 404/409 и валидацией на 400; проверить MVC-тестами ошибочные запросы и сохранение поведения Part.
- [x] 3.3 Подключить репозитории, mapper'ы, application-сервисы и контроллеры в корневом `WebConfig`; проверить запуск контекста и `ApplicationModules.verify()` без циклических зависимостей.
- [x] 3.4 Проверить REST tenant-контекст: запросы не используют клиентский organization header, а контекст очищается после успешного и ошибочного запроса; добавить интеграционный тест.

## 4. Frontend generic-клиент и экран каталога

- [x] 4.1 Обобщить `frontend/src/api.ts` для list/create/rename/delete с endpoint-конфигурацией пяти типов; проверить Vitest-тестами HTTP-методы и URL каждой конфигурации.
- [x] 4.2 Обобщить `App.tsx`, `PartsTable.tsx` и `CreatePartForm.tsx` в type-agnostic компоненты с конфигурацией типа и вкладками «Детали», «Сборки», «Покупные», «Стандартные», «ВЗК»; проверить отображение всех названий.
- [x] 4.3 Сохранить подтверждение удаления, состояния загрузки/ошибок и стили существующего экрана при работе выбранного типа; проверить frontend-тестами отмену DELETE, создание, переименование и удаление.
- [x] 4.4 Проверить начальное состояние Part, пустые вкладки Buy/Standart/Vzk и корректное переключение endpoint без смешивания записей между вкладками; выполнить `npm test` или эквивалентную команду frontend.

## 5. Интеграционная проверка

- [x] 5.1 Выполнить полный backend-набор `mvn test`, включая unit/MVC/архитектурные тесты, и устранить нарушения модульных границ.
- [x] 5.2 Выполнить frontend lint/build/test-команды из `frontend/package.json` и убедиться, что сборка содержит все пять вкладок.
- [x] 5.3 Сверить реализованные маршруты, DTO, статусы ошибок, seed-данные и tenant-изоляцию с delta-спеками change; отметить все пункты выполненными только после успешных проверок.
