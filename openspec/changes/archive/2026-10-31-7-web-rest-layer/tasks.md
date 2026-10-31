# Tasks

## 1. Зависимость и модульная структура

- [x] 1.1 Добавить `spring-boot-starter-web` в `pom.xml` и проверить разрешение зависимостей командой `mvn test`.
- [x] 1.2 Создать `com.hydroyura.forgeflow.web` с `package-info.java` и `@ApplicationModule`, настроив разрешённые зависимости на `catalog`, `organizations`, `security` и `shared`; проверить модель через архитектурный тест.

## 2. Composition root и tenant lifecycle

- [x] 2.1 Создать web-конфигурацию `@Configuration` с `@Bean` для `InMemoryPartRepository`, `ThreadLocalOrganizationContext`, `Clock`, `PartMapper.INSTANCE` и `PartApplicationService`; проверить загрузку контекста Spring.
- [x] 2.2 Собрать в web-конфигурации `StubAuthenticatedUserProvider`, `OrganizationContextResolver` и `OrganizationContextLifecycle`, используя публичные security/organizations API; проверить разрешение организации для authenticated user.
- [x] 2.3 Реализовать `OncePerRequestFilter` (или эквивалент) вокруг `OrganizationContextLifecycle.run`, не читая `X-Organization-Id`; проверить установку tenant-контекста до обработчика и очистку после успешного и аварийного завершения.
- [x] 2.4 Добавить тест tenant-фильтра на серверное разрешение организации, игнорирование `X-Organization-Id` и очистку `ThreadLocal` в `finally`; проверить тест командой `mvn test`.

## 3. REST-контроллер Part

- [x] 3.1 Реализовать тонкий контроллер под `/api/v1/parts`, зависящий только от `catalog.api.PartService` и переиспользующий `PartView`/`CreatePartRequest`; проверить `GET /api/v1/parts/{id}` и `GET /api/v1/parts` со статусом 200.
- [x] 3.2 Реализовать `POST /api/v1/parts` с `CreatePartRequest` и статусом 201; проверить JSON-представление созданного `PartView` через MockMvc.
- [x] 3.3 Реализовать `PUT /api/v1/parts/{id}/name` с новым именем и статусом 204; проверить делегирование `rename` и отсутствие обязательного тела ответа.
- [x] 3.4 Реализовать `DELETE /api/v1/parts/{id}` со статусом 204; проверить делегирование `delete` и последующее отсутствие детали.
- [x] 3.5 Добавить MVC-тесты на каждый endpoint (`GET` по id, `GET` списка, `POST`, `PUT` rename, `DELETE`) и проверить сериализацию/десериализацию переиспользуемых catalog DTO.

## 4. Обработка ошибок REST

- [x] 4.1 Реализовать минимальный `@RestControllerAdvice` с маппингом `PartNotFoundException` в 404, `DuplicatePartNumberException` в 409 и `IllegalArgumentException`/ошибок валидации в 400; проверить статусы и JSON-ответы.
- [x] 4.2 Дополнить web-тесты сценариями 404, 409 и 400 для соответствующих исключений и некорректного запроса; проверить их через `mvn test`.

## 5. Архитектурные границы и интеграционная проверка

- [x] 5.1 Обновить проверку module boundaries с восьми до девяти модулей и зависимостями `web -> catalog, organizations, security, shared`; проверить `ApplicationModules.verify()`.
- [x] 5.2 Проверить отсутствие Spring-зависимостей и web-прямого доступа к domain/application/infrastructure, кроме разрешённого composition-root wiring; проверить полный набор `mvn test`.
- [x] 5.3 Выполнить финальную проверку REST-контракта и архитектуры командой `mvn test`, убедившись, что существующие тесты catalog/security и новый web-модуль проходят.
