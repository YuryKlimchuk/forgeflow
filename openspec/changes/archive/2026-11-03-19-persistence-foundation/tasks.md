# Tasks

## 1. Базовая persistence-инфраструктура

- [x] 1.1 Добавить в `pom.xml` зависимости Spring Data JPA, PostgreSQL-драйвера, Flyway и Testcontainers; проверить `mvn test` на конфигурационной сборке.
- [x] 1.2 Создать `application.yml` с PostgreSQL datasource, Flyway и отключённым Hibernate schema generation; проверить старт приложения на чистой базе.
- [x] 1.3 Создать `docker-compose.yml` только с PostgreSQL и Keycloak; проверить `docker compose config` и отсутствие сервиса ForgeFlow.
- [x] 1.4 Создать базовые Flyway-миграции с UUID-колонками, общими ограничениями и индексами; проверить применение миграций на чистом Testcontainers PostgreSQL.
- [x] 1.5 Подготовить общий тестовый Testcontainers-контекст и правила запуска Flyway; проверить, что интеграционный тест стартует с настоящим PostgreSQL.

## 2. Organizations

- [x] 2.1 Спроектировать и создать persistence-модели и миграции для `Organization`, `Owner`, `Membership` и настроек с сохранением UUID, аудита и связей; проверить миграции и ограничения на PostgreSQL.
- [x] 2.2 Реализовать мапперы `organizations` между JPA-моделями и доменными агрегатами без JPA/Spring-зависимостей в `organizations.domain`; проверить `OrganizationsDomainPurityTest`.
- [x] 2.3 Реализовать JPA-адаптеры `OrganizationRepository` и `OwnerRepository` с явным tenant/affiliation-фильтром и soft-delete; проверить round-trip организации, владельца и membership.
- [x] 2.4 Перевести `MembershipCoordinator` на постоянные repository-порты и добавить `@Transactional` на изменяющие методы application-сервисов; проверить атомарность создания организации и конкурентную уникальность пользователя.
- [x] 2.5 Заменить organization-бины в `WebConfig` на JPA-адаптеры, сохранив `StubAuthenticatedUserProvider`, `MutableSubjectProvider`, tenant lifecycle и фильтр; проверить `mvn test` и сценарии межtenant-доступа.
- [x] 2.6 Перенести существующие organization unit-тесты на test doubles в `src/test` и добавить PostgreSQL-интеграционные тесты восстановления, soft-delete и tenant isolation; проверить выполнение соответствующего набора тестов.

## 3. Catalog

- [x] 3.1 Создать Flyway-миграции и отдельные JPA-модели для `Part`, `Assembly`, `Buy`, `Standart` и `Vzk` с `organization_id`, аудитом, soft-delete и tenant-уникальностью номера; проверить миграции на чистой PostgreSQL.
- [x] 3.2 Реализовать persistence-мапперы и JPA-адаптеры для пяти доменных repository-портов без изменения доменных классов; проверить round-trip полей, UUID и `deletedAt`.
- [x] 3.3 Обеспечить tenant-фильтрацию во всех catalog find/exists/save-операциях и транзакционные границы catalog application-сервисов; проверить, что организация A не читает и не изменяет каталог B.
- [x] 3.4 Перевести catalog-бины в `WebConfig`, удалить production `InMemory*Repository` каталога после успешной замены и перенести unit doubles в `src/test`; проверить `ModuleVerificationTest` и catalog unit-тесты.
- [x] 3.5 Добавить PostgreSQL-интеграционные тесты пяти адаптеров для сохранения, переименования, soft-delete, уникальности номеров и восстановления после перезапуска тестового контекста; проверить тестовый набор catalog.

## 4. Specifications

- [x] 4.1 Создать Flyway-миграции и persistence-модели для спецификаций, строк, substitutions и ссылок catalog с tenant-колонками и ограничениями уникальности; проверить порядок миграций относительно catalog.
- [x] 4.2 Реализовать мапперы, загрузку вложенных строк и substitutions и JPA-адаптеры `SpecificationRepository`/`SubstitutionRepository`; проверить round-trip строк, количеств и заменителей.
- [x] 4.3 Сохранить доменные правила удаления строки, копирования, flat BOM и tenant-фильтрации при транзакционной записи агрегатов; проверить rollback при ошибке и отсутствие частичных данных.
- [x] 4.4 Перевести specification-бины в `WebConfig`, удалить production in-memory-репозитории specifications и перенести doubles в `src/test`; проверить существующие тесты specifications и архитектурные границы.
- [x] 4.5 Добавить PostgreSQL-интеграционные тесты пустой спецификации, строк, substitutions, soft-delete/жизненного цикла и cross-tenant доступа; проверить выполнение на Testcontainers.

## 5. Planning

- [x] 5.1 Создать Flyway-миграции и JPA-модели для `Plan` и `PlanItem` с tenant-колонкой, уникальностью имени и связями с assembly; проверить миграции после схемы specifications.
- [x] 5.2 Реализовать маппер и JPA-адаптер `PlanRepository`, восстанавливающий все элементы плана и сохраняющий доменные UUID без `@GeneratedValue`; проверить round-trip плана.
- [x] 5.3 Добавить транзакционные границы planning application-сервиса и сохранить проверки доступности assembly через существующий API; проверить tenant isolation, duplicate name и rollback.
- [x] 5.4 Перевести planning-бины в `WebConfig`, удалить production `InMemoryPlanRepository` и создать test double в `src/test`; проверить planning unit-тесты и `ModuleVerificationTest`.
- [x] 5.5 Добавить PostgreSQL-интеграционные тесты создания, изменения, удаления элементов, flat BOM через persistence и восстановления плана после перезапуска контекста; проверить Testcontainers-набор planning.

## 6. Seed и удаление in-memory-кода

- [x] 6.1 Создать идемпотентный `seed.sql` с фиксированными UUID для демо-организации, membership, каталога и спецификаций; проверить двукратное ручное выполнение после Flyway без дубликатов.
- [x] 6.2 Удалить `DataSeeder`, `SeedProperties`, `SeedEntry*`, `SeedSpecification*` и `seed-data/*.json`; изменить `DemoDataBootstrap`, чтобы он не создавал организацию и membership, и проверить запуск без Java-seed.
- [x] 6.3 Удалить оставшиеся production `InMemory*Repository` из активных модулей и обновить composition root; проверить отсутствием таких реализаций в `src/main` и успешным `mvn test`.
- [x] 6.4 Проверить, что `seed.sql` не подключён к application/Flyway lifecycle и что stub-auth runtime wiring сохраняется; проверить чистый запуск приложения после ручной загрузки seed.

## 7. Сквозная проверка

- [x] 7.1 Запустить полный `mvn test` с `OrganizationsDomainPurityTest`, `ModuleVerificationTest`, unit-тестами и Testcontainers-интеграционными тестами; устранить нарушения доменной чистоты и Modulith.
- [x] 7.2 Проверить сценарий чистая PostgreSQL → старт приложения → Flyway → ручной `seed.sql` → перезапуск → чтение всех четырёх модулей; зафиксировать успешное восстановление данных.
- [x] 7.3 Проверить cross-tenant read/write для organizations, catalog, specifications и planning одним интеграционным сценарием; подтвердить отсутствие данных организации B в ответах организации A.
