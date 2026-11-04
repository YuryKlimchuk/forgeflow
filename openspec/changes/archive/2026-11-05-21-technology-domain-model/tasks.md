# Tasks

## 1. Публичный тип TechnologyProcessId

- [x] 1.1 Создать `src/main/java/com/hydroyura/forgeflow/technology/api/TechnologyProcessId.java` как `record` с компонентом `UUID value`, проверкой `null` в компактном конструкторе (`NullPointerException`, конвенция `PartId`/`OrganizationId`) и фабриками `generate()`/`of(UUID)`; проверить компиляцией `mvn -q -DskipTests compile`.
- [x] 1.2 Добавить unit-тесты `TechnologyProcessIdTest` (`generate()` возвращает непустой UUID, `of(uuid)` сохраняет UUID, `of(null)` отклоняется `NullPointerException`); проверить `mvn -q -Dtest=TechnologyProcessIdTest test`.

## 2. Value object Operation

- [x] 2.1 Создать `src/main/java/com/hydroyura/forgeflow/technology/domain/Operation.java` как `record` с полями `int number` и `String name`, отклоняющий `number <= 0` и пустое/пробельное/`null` имя (`IllegalArgumentException` через семантику `StringUtils.isBlank`); проверить компиляцией.
- [x] 2.2 Добавить unit-тесты `OperationTest` (валидные значения сохраняются, `number <= 0` и пустые/пробельные/`null` имена отклоняются `IllegalArgumentException`); проверить `mvn -q -Dtest=OperationTest test`.

## 3. Агрегат TechnologyProcess

- [x] 3.1 Создать `src/main/java/com/hydroyura/forgeflow/technology/domain/TechnologyProcess.java` с аннотациями `@Entity`/`@Identity`, полями `TechnologyProcessId id`, `String number` (final), `String name`, `PartId partId` (final), `OrganizationId organizationId` (final), `boolean defaultProcess`, `Instant createdAt` (final), `Instant updatedAt`, `Instant deletedAt`, коллекцией операций и статической фабрикой `create(...)` с fail-fast валидацией `number`/`name`; проверить компиляцией.
- [x] 3.2 Реализовать `rename(String)` (отклонение пустого/пробельного/`null` имени, обновление `name` и `updatedAt`) и идемпотентный `markDeleted()`; проверить тестами агрегата.
- [x] 3.3 Реализовать `addOperation(int, String)` (валидация через `Operation`, отклонение дубликата номера), `removeOperation(int)` (ошибка при отсутствующем номере) и `operations()` (неизменяемая коллекция, сортировка по номеру); `addOperation`/`removeOperation` при фактическом изменении обновляют `updatedAt`; проверить тестами агрегата.
- [x] 3.4 Реализовать package-private `markAsDefault()`/`markAsNotDefault()` и getter `isDefaultProcess()`; при фактическом изменении флага обновлять `updatedAt`, повторный вызов с тем же состоянием — no-op; проверить локальное поведение флага тестами.
- [x] 3.5 Добавить unit-тесты `TechnologyProcessTest` (валидация полей, аудит без `createdBy`/`updatedBy`, идемпотентность soft-delete, rename, порядок/неизменяемость операций, добавление/удаление, обновление `updatedAt` всеми мутаторами и отсутствие изменения `updatedAt` при идемпотентных no-op); проверить `mvn -q -Dtest=TechnologyProcessTest test`.

## 4. Порт репозитория и доменные исключения

- [x] 4.1 Создать `src/main/java/com/hydroyura/forgeflow/technology/domain/TechnologyProcessRepository.java` с методами `save`, `findById(id, orgId)`, `findByPartId(partId, orgId)`, `findAll(orgId)`, `existsByNumber(number, orgId)` и без `delete`; проверить компиляцией.
- [x] 4.2 Создать доменные исключения в `technology.domain`: `DuplicateTechnologyProcessNumberException`, `DuplicateOperationException`, `OperationNotFoundException`, `TechnologyProcessNotFoundException`, `InvalidDefaultProcessStateException`; проверить компиляцией.
- [x] 4.3 Добавить reflection-тест `TechnologyProcessRepositoryPortTest` формы порта (методы и сигнатуры, отсутствие `delete`); проверить `mvn -q -Dtest=TechnologyProcessRepositoryPortTest test`.

## 5. Доменный сервис TechnologyProcessService

- [x] 5.1 Создать `src/main/java/com/hydroyura/forgeflow/technology/domain/TechnologyProcessService.java` (чистый доменный сервис, только `TechnologyProcessRepository`) с методами `register`, `markAsDefault`, `unmarkDefault` по D9 design.md; проверить компиляцией.
- [x] 5.2 Добавить in-memory test double `src/test/java/com/hydroyura/forgeflow/technology/infrastructure/InMemoryTechnologyProcessRepository.java` (tenant-scoped, исключает удалённые, без Spring/JPA); проверить компиляцией тестов.
- [x] 5.3 Добавить unit-тесты `TechnologyProcessServiceTest`: отклонение дубликата номера, переключение дефолта без двух дефолтных, отказ снимать единственный дефолтный при втором процессе, отказ регистрировать второй не-дефолтный процесс, идемпотентные no-op `markAsDefault` (уже дефолтный) и `unmarkDefault` (уже не-дефолтный); проверить `mvn -q -Dtest=TechnologyProcessServiceTest test`.

## 6. Доменная чистота и границы модуля

- [x] 6.1 Добавить тест доменной чистоты `technology` (отсутствие импортов Spring/JPA/PostgreSQL/Flyway в `technology.domain`); проверить `mvn -q -Dtest=TechnologyDomainPurityTest test`.
- [x] 6.2 Обновить `src/main/java/com/hydroyura/forgeflow/technology/package-info.java`: добавить `shared :: shared` в `allowedDependencies` (итоговый список `catalog :: catalog`, `directories :: directories`, `shared :: shared`) для использования `shared.api.OrganizationId`; убедиться, что `technology` ссылается только на `catalog.api.PartId` и `shared.api.OrganizationId`; проверить `mvn -q -Dtest=ModuleVerificationTest test`.
- [x] 6.3 Выполнить полный прогон `mvn -q test` и убедиться, что все тесты проекта проходят.
