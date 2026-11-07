# Tasks

## 1. Границы модулей и схема БД

- [x] 1.1 В `src/main/java/com/hydroyura/forgeflow/directories/package-info.java` установить `type = ApplicationModule.Type.OPEN` и `allowedDependencies = {"shared :: shared"}`; в `src/main/java/com/hydroyura/forgeflow/web/package-info.java` добавить `"directories :: directories"` в `allowedDependencies`. Верификация: `mvn -q -Dtest=ModuleVerificationTest test` проходит без нарушений границ и циклов.
- [x] 1.2 Создать `src/main/resources/db/migration/V6__directories.sql`: таблицы `material` и `sortament` (id/`number`/`name`/`standard_type`/`standard`/`unit`/`organization_id`/`created_at`/`updated_at`/`deleted_at` в стиле V2/V5) и частичные уникальные индексы `material_number_org_uq`/`sortament_number_org_uq` на `(organization_id, number) WHERE deleted_at IS NULL`. Верификация: `mvn -q -DskipTests compile` успешен, миграция применяется без ошибок при старте Testcontainers-контекста.

## 2. Доменный слой

- [x] 2.1 Создать `src/main/java/com/hydroyura/forgeflow/directories/api/StandardType.java` с константами `GOST`/`DIN`/`ISO`/`OTHER`. Верификация: юнит-тест состава перечисления проходит.
- [x] 2.2 Создать `MaterialId` и `SortamentId` в `directories/api` (record с `UUID value`, `generate()`/`of(UUID)`, отказ от `null`). Верификация: юнит-тесты ID проходят.
- [x] 2.3 Создать агрегат `Material` в `directories/domain/material` (поля R8, фабрика `create`, `update(...)`, `markDeleted()`, геттеры, валидация blank/`null`). Верификация: `MaterialTest` покрывает валидацию, обновление, неизменяемость `number` и мягкое удаление.
- [x] 2.4 Создать агрегат `Sortament` в `directories/domain/sortament` (поля R9, та же семантика, что у `Material`). Верификация: `SortamentTest` покрывает валидацию, обновление, неизменяемость `number` и мягкое удаление.
- [x] 2.5 Создать driven-порты `MaterialRepository`/`SortamentRepository` в `directories/domain/*` (аннотационно-свободные интерфейсы: `save`, `findById`, `findAll`, `existsByNumber`, с явным `OrganizationId`). Верификация: тесты формы порта подтверждают сигнатуры и отсутствие физического `delete`.
- [x] 2.6 Создать доменные исключения `MaterialNotFoundException`, `DuplicateMaterialNumberException`, `SortamentNotFoundException`, `DuplicateSortamentNumberException` в `directories/domain/*`. Верификация: код компилируется, исключения используются в application-слое.
- [x] 2.7 Добавить `DirectoriesDomainPurityTest` (аналог `TechnologyDomainPurityTest`): агрегаты — только jMolecules-аннотации, порты — свободные от аннотаций интерфейсы, доменные типы без Spring/JPA/PostgreSQL/Flyway. Верификация: `mvn -q -Dtest=DirectoriesDomainPurityTest test` проходит.

## 3. Публичный API-контракт

- [x] 3.1 Создать входные порты `MaterialService`/`SortamentService` в `directories/api` (create/get/list/update/delete с javadoc о tenant-контексте). Верификация: код компилируется, сигнатуры совпадают с контроллерами.
- [x] 3.2 Создать DTO `CreateMaterialRequest`/`UpdateMaterialRequest`/`MaterialView` и `CreateSortamentRequest`/`UpdateSortamentRequest`/`SortamentView` в `directories/api` (record; `*View` без `organizationId`/`deletedAt`, только читаемые поля). Верификация: мапперы и контроллеры компилируются с этими типами.

## 4. Application-слой

- [x] 4.1 Создать `MaterialApplicationService` и application-маппер `MaterialMapper` в `directories/application` (tenant из `OrganizationContext.requiredOrganizationId()`, проверка `existsByNumber`, `@Transactional`, `Clock`). Верификация: `MaterialApplicationServiceTest` на in-memory репозитории покрывает create/get/list/update/delete/уникальность/изоляцию/сортировку/отсутствие контекста.
- [x] 4.2 Создать `SortamentApplicationService` и application-маппер `SortamentMapper` в `directories/application`. Верификация: `SortamentApplicationServiceTest` на in-memory репозитории проходит по тем же сценариям.
- [x] 4.3 Создать тестовые `InMemoryMaterialRepository`/`InMemorySortamentRepository` в `src/test/java/.../directories/infrastructure` (фильтрация по организации, исключение удалённых, сортировка по `number`, `existsByNumber` по активным). Верификация: тесты 4.1/4.2 зелёные.

## 5. JPA-персистентность

- [x] 5.1 Создать `MaterialEntity`/`SortamentEntity` (`@Entity @Table`, публичные поля, `@Enumerated(EnumType.STRING)` для `standardType`) и Spring Data-интерфейсы `MaterialJpaRepository`/`SortamentJpaRepository` в `directories/infrastructure/persistence`. Верификация: `mvn -q -DskipTests compile` успешен.
- [x] 5.2 Создать инфраструктурные мапперы `MaterialMapper`/`SortamentMapper` (`entity ↔ domain`, восстановление через `create` без `deletedAt`). Верификация: тесты мапперов подтверждают round-trip всех полей.
- [x] 5.3 Создать `JpaMaterialRepository`/`JpaSortamentRepository` (реализация доменных портов; `save` + `flush` + трансляция `DataIntegrityViolationException` с именем ограничения `material_number_org_uq`/`sortament_number_org_uq` в доменное `Duplicate*NumberException`). Верификация: `JpaMaterialRepositoryTest`/`JpaSortamentRepositoryTest` на PostgreSQL (`AbstractPostgresIntegrationTest`) проверяют round-trip, tenant-изоляцию двух организаций, soft-delete, существование частичного индекса, дубликат на уровне БД и повторное использование номера после удаления.

## 6. REST и composition root

- [x] 6.1 Создать тонкие контроллеры `MaterialController` (`/api/v1/materials`) и `SortamentController` (`/api/v1/sortaments`) в `web` (POST 201, GET list 200, GET/{id} 200, PUT/{id} 204, DELETE/{id} 204; парсинг UUID в типизированный ID; `@RestController` + component scan, как `PlanController`). Верификация: `mvn -q -DskipTests compile` успешен.
- [x] 6.2 Дополнить `WebConfig` бинами `directories` (репозитории через Spring Data, application-мапперы, application-сервисы с `OrganizationContext` и `Clock`) и дополнить `RestExceptionHandler` маппингом новых исключений (404/409). Верификация: приложение стартует (`mvn -q -DskipTests spring-boot:run` в smoke-режиме или контекст-тест).
- [x] 6.3 Создать `MaterialControllerTest`/`SortamentControllerTest` (MockMvc standalone + `RestExceptionHandler`). Верификация: тесты покрывают статусы 201/200/204 и тела для пяти эндпоинтов и маппинг 404/409/400.

## 7. Дефолтные данные

- [x] 7.1 Дополнить `seed.sql` секциями `-- material` и `-- sortament` с фиксированными UUID и `ON CONFLICT DO NOTHING` по таблице D12 (минимум ГОСТ/DIN/ISO в обоих справочниках, демо-организация `00000000-0000-0000-0000-000000000001`). Верификация: после `flyway` + двукратного выполнения `seed.sql` дубликаты отсутствуют; `SELECT` по `material`/`sortament` возвращает демо-записи.

## 8. Интеграционная проверка

- [x] 8.1 Прогнать полный `mvn test` и подтвердить зелёный результат, включая `ModuleVerificationTest`, `DirectoriesDomainPurityTest`, application/JPA/controller-тесты `directories`. Верификация: все тесты проходят, сборка успешна.
