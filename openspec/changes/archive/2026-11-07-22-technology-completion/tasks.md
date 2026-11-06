# Tasks

## 1. Домен: фабрика восстановления

- [x] 1.1 Добавить в `src/main/java/com/hydroyura/forgeflow/technology/domain/TechnologyProcess.java` публичную статическую фабрику `restore(...)` (параметры по D6 design.md: id, number, name, partId, organizationId, defaultProcess, createdAt, updatedAt, deletedAt, operations) через приватный конструктор с копированием операций в отсортированный по номеру список без вызова `addOperation`; проверить `mvn -q -DskipTests compile`.
- [x] 1.2 Добавить unit-тесты `TechnologyProcessRestoreTest` (восстановление `defaultProcess = true`, `deletedAt`, временных меток и упорядоченных операций без изменения `updatedAt`); проверить `mvn -q -Dtest=TechnologyProcessRestoreTest test`.

## 2. Публичный API technology

- [x] 2.1 Создать DTO в `src/main/java/com/hydroyura/forgeflow/technology/api/`: `CreateTechnologyProcessRequest(String number, String name, PartId partId)`, `AddOperationRequest(int number, String name)`, `TechnologyProcessView(TechnologyProcessId id, String number, String name, PartId partId, boolean defaultProcess, Instant createdAt, Instant updatedAt, List<OperationView> operations)`, `OperationView(int number, String name)`; проверить `mvn -q -DskipTests compile`.
- [x] 2.2 Создать входной порт `src/main/java/com/hydroyura/forgeflow/technology/api/TechnologyService.java` с методами `create`, `get`, `list(PartId)`, `rename(TechnologyProcessId, String)`, `delete`, `addOperation`, `removeOperation`, `markAsDefault`, `unmarkDefault` (сигнатуры по D4 design.md); проверить `mvn -q -DskipTests compile`.

## 3. Application-слой

- [x] 3.1 Создать view-маппер `src/main/java/com/hydroyura/forgeflow/technology/application/TechnologyMapper.java` (домен → `TechnologyProcessView`/`OperationView`, операции в порядке агрегата); проверить компиляцией.
- [x] 3.2 Создать `src/main/java/com/hydroyura/forgeflow/technology/application/TechnologyApplicationService.java` по D5 design.md (`@Transactional`, зависимости repository/domainService/organizationContext/partService/mapper/clock, порядок ошибок 400→404→409); проверить `mvn -q -DskipTests compile`.
- [x] 3.3 Добавить `TechnologyApplicationServiceTest` (in-memory репозиторий, fake `OrganizationContext`, mock `PartService`, `Clock.fixed`, реальные `TechnologyMapper`/`TechnologyProcessService`): создание, получение, список+фильтр, переименование, логическое удаление, операции, дефолтность, tenant-изоляция, дубликаты и ошибки; проверить `mvn -q -Dtest=TechnologyApplicationServiceTest test`.
- [x] 3.4 Добавить `TechnologyMapperTest` для application-маппера (полнота полей view, порядок операций); проверить `mvn -q -Dtest=TechnologyMapperTest test`.

## 4. Персистентность (JPA + Flyway)

- [x] 4.1 Создать `src/main/resources/db/migration/V5__technology.sql` с таблицами `technology_process`, `technology_process_operation` и частичным уникальным индексом `(organization_id, number) WHERE deleted_at IS NULL` по D8 design.md; проверить применение миграции на чистой базе (интеграционный тест или локальный запуск).
- [x] 4.2 Создать сущности `TechnologyProcessEntity`, `TechnologyProcessOperationEntity` и `TechnologyProcessOperationId` в `src/main/java/com/hydroyura/forgeflow/technology/infrastructure/persistence/` (поля по D7, без `@GeneratedValue`); проверить компиляцией.
- [x] 4.3 Создать Spring Data-интерфейсы `TechnologyProcessJpaRepository` и `TechnologyProcessOperationJpaRepository` (методы по D7); проверить компиляцией.
- [x] 4.4 Создать инфраструктурный маппер `src/main/java/com/hydroyura/forgeflow/technology/infrastructure/persistence/TechnologyMapper.java` (`toEntity`/`toOperationEntities`/`toDomain` через `restore`, операции по `operation_number`); проверить компиляцией.
- [x] 4.5 Создать адаптер `JpaTechnologyProcessRepository implements TechnologyProcessRepository` (`save` с заменой набора операций, tenant-scoped чтения с `DeletedAtIsNull`, сортировка по `number`); проверить компиляцией.
- [x] 4.6 Добавить `TechnologyMapperTest` (инфраструктурный): entity ↔ domain round-trip с `defaultProcess`, `deletedAt`, временными метками и упорядоченными операциями; проверить `mvn -q -Dtest=TechnologyMapperTest test`.
- [x] 4.7 Добавить интеграционный тест `JpaTechnologyProcessRepositoryTest extends AbstractPostgresIntegrationTest`: round-trip, tenant-изоляция, исключение удалённых, порядок операций, существование частичного уникального индекса, отклонение дубликата на уровне БД и освобождение номера после soft-delete; проверить `mvn -q -Dtest=JpaTechnologyProcessRepositoryTest test`.

- [x] 4.8 Реализовать в `JpaTechnologyProcessRepository.save(...)` точечный маппинг нарушения частичного уникального индекса номера: обернуть `repository.save`+`repository.flush` в `try/catch (DataIntegrityViolationException)`, извлечь `org.hibernate.exception.ConstraintViolationException.getConstraintName()`; при `technology_process_number_org_uq` бросить доменный `DuplicateTechnologyProcessNumberException(process.getNumber())`, иначе пробросить исходное исключение (D10 design.md); проверить компиляцией.
- [x] 4.9 Добавить тесты пути дубликата/гонки на уровне БД: (а) интеграционный — повторный `save` активного процесса с одинаковым `(organization_id, number)` через `JpaTechnologyProcessRepository` (детерминированная имитация гонки `existsByNumber`→`save`) даёт `DuplicateTechnologyProcessNumberException` (а не `DataIntegrityViolationException`), который `RestExceptionHandler` маппит в 409 `duplicate`; (б) unit — `DataIntegrityViolationException` с чужим именем ограничения (например, FK) НЕ транслируется в `DuplicateTechnologyProcessNumberException`, а пробрасывается дальше; проверить `mvn -q -Dtest=JpaTechnologyProcessRepositoryTest,JpaTechnologyProcessRepositoryAdapterTest test`.
- [x] 4.10 Добавить интеграционный тест `TechnologyApplicationServiceIntegrationTest extends AbstractPostgresIntegrationTest` (реальный бин application-сервиса + `JpaTechnologyProcessRepository` на PostgreSQL): сценарий I8 — принудительная ошибка на середине изменяющей операции (например, spy на репозитории, бросающий исключение на втором `save` после успешного первого flush) → после `@Transactional`-отката в БД нет частичных записей; проверить `mvn -q -Dtest=TechnologyApplicationServiceIntegrationTest test`.

## 5. REST-слой

- [x] 5.1 Создать `src/main/java/com/hydroyura/forgeflow/web/RenameTechnologyProcessRequest.java` (`record` с полем `name`); проверить компиляцией.
- [x] 5.2 Создать `src/main/java/com/hydroyura/forgeflow/web/TechnologyProcessController.java` (`@RestController`, `@RequestMapping("/api/v1/technology-processes")`, тонкая делегация в `TechnologyService`, эндпоинты и статусы по требованиям 23); проверить компиляцией.
- [x] 5.3 Расширить `src/main/java/com/hydroyura/forgeflow/RestExceptionHandler.java` обработчиками технологии по D10 (`not_found`, `duplicate`, `invalid_state`); проверить компиляцией.
- [x] 5.4 Добавить `TechnologyProcessControllerTest` (MockMvc standalone + `RestExceptionHandler`, mock `TechnologyService`): все эндпоинты, статусы 201/200/204 и коды ошибок 404/409/400; проверить `mvn -q -Dtest=TechnologyProcessControllerTest test`.

## 6. Composition root и модульные границы

- [x] 6.1 Добавить в `WebConfig` секцию `// --- Technology ---` с бинами `technologyProcessRepository`, `technologyMapper`, `technologyProcessService` (доменный сервис как `@Bean`), `technologyService` по D9; проверить компиляцией.
- [x] 6.2 Обновить `src/main/java/com/hydroyura/forgeflow/web/package-info.java`: добавить `technology :: technology` в `allowedDependencies`; убедиться в отсутствии циклов; проверить `mvn -q -Dtest=ModuleVerificationTest test`.
- [x] 6.3 Выполнить полный прогон `mvn -q test` и убедиться, что все тесты (включая доменные, `TechnologyDomainPurityTest` и контекстный интеграционный тест) проходят.
