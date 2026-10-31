# Tasks

## 1. Типизация организации в домене

- [x] 1.1 Заменить `UUID organizationId` на `shared.api.OrganizationId` в `Part`, `Assembly`, `Buy`, `Standart` и `Vzk`, обновить конструкторы, factory-методы и accessors; проверить компиляцией, что сырые UUID больше не используются в этих контрактах.
- [x] 1.2 Обновить существующие тесты каталога, включая `CatalogEntityTest`, для создания сущностей через `OrganizationId` и проверить `mvn test` для доменного слоя.

## 2. Зависимость и публичный API

- [x] 2.1 Добавить MapStruct runtime-зависимость и annotation processor в `pom.xml`; проверить успешную генерацию mapper-классов командой `mvn -DskipTests compile`.
- [x] 2.2 Создать в `catalog.api` record-типы `CreatePartRequest`, `PartView`, пустой `PartFilter` и интерфейс `PartService` с согласованными сигнатурами; проверить компиляцией отсутствие зависимостей API от application/infrastructure.
- [x] 2.3 Создать `CatalogException`, `PartNotFoundException` и `DuplicatePartNumberException` в `catalog.application`; проверить тестом иерархию исключений и сообщения для not-found/duplicate случаев.

## 3. Repository и mapper

- [x] 3.1 Расширить `PartRepository` операциями поиска, списка и проверки/сохранения с явным `OrganizationId`, сохранив `save` и логическое значение `delete`; проверить контракт тестами порта.
- [x] 3.2 Реализовать `InMemoryPartRepository` с tenant-фильтрацией, исключением удалённых записей из чтения, сортировкой по `number` и обнаружением дубликата активного номера; проверить unit-тестами уникальность, soft-delete и изоляцию двух `OrganizationId`.
- [x] 3.3 Создать MapStruct-маппер `Part` → `PartView`; проверить отдельным JUnit 5-тестом перенос `PartId`, `number`, `name`, `createdAt` и `updatedAt`.

## 4. Application service

- [x] 4.1 Реализовать `PartApplicationService implements PartService`, который получает `OrganizationId` через `OrganizationContext.requiredOrganizationId()` и явно передаёт его repository в каждой операции; проверить unit-тестом captured organization scope.
- [x] 4.2 Реализовать create с `PartId.generate()`, единым `Instant.now()` для `createdAt`/`updatedAt`, ручной сборкой домена и обработкой duplicate; проверить unit-тестом с fake repository и фиксированными часами.
- [x] 4.3 Реализовать get/list с `PartView`, `PartFilter`, сортировкой и преобразованием отсутствующей/удалённой/чужой записи в `PartNotFoundException`; проверить unit-тестами успешного и ошибочных сценариев.
- [x] 4.4 Реализовать rename с доменной валидацией имени и сохранением детали; проверить unit-тестом новое имя и `PartNotFoundException` для отсутствующей детали.
- [x] 4.5 Реализовать delete через доменный `markDeleted()` и сохранение без физического удаления; проверить unit-тестами soft-delete, повторное удаление и исключение для отсутствующей детали.
- [x] 4.6 Добавить unit-тесты `PartApplicationService` на create, get, list, rename, delete, duplicate и not-found с fake repository и фиксированным временем; проверить `mvn -Dtest=PartApplicationServiceTest test`.

## 5. Архитектурная интеграция

- [x] 5.1 Проверить размещение новых классов в `catalog.api`, `catalog.application` и `catalog.infrastructure`, named interface и допустимые зависимости `catalog -> shared/directories`; исправить нарушения до прохождения архитектурного теста.
- [x] 5.2 Запустить архитектурный тест `ApplicationModules.verify()` и полный `mvn test`; убедиться, что новые API, mapper, repository и service проходят существующие проверки без REST-контроллеров и UI.
