# Proposal

## Why

ForgeFlow уже предоставляет публичный `catalog.api.PartService`, но не имеет HTTP-контракта для React-клиента. Этот change добавляет первый REST-срез для `Part` и одновременно вводит web-модуль как composition root, сохраняя tenant-изоляцию и границы DDD-модулей.

## What Changes

- Добавить модуль `web` в `com.hydroyura.forgeflow.web` с REST-контроллерами и Spring wiring.
- Добавить REST-эндпоинты управления `Part` по адресу `/api/v1/parts` для создания, получения, списка, переименования и логического удаления.
- Переиспользовать `catalog.api.PartView` и `CreatePartRequest` как JSON-контракт без дублирования web-DTO.
- Подключить `spring-boot-starter-web`.
- Собрать зависимости каталога, организации, безопасности и web через `@Configuration`/`@Bean`; доменные и application-классы не должны получать Spring-аннотации.
- Добавить request-фильтр, который определяет организацию через текущего authenticated user и гарантированно очищает `ThreadLocalOrganizationContext`.
- Добавить минимальный `@RestControllerAdvice` для ответов 400, 404 и 409.
- Обновить спецификацию границ модулей: добавить девятый модуль `web` с зависимостями на `catalog`, `organizations`, `security` и `shared`.
- Добавить автоматизированные тесты REST-контрактов, tenant-фильтра и архитектурных границ.

## Capabilities

### New Capabilities

- `part-rest-api`: HTTP-контракт и обработка ошибок для операций `Part`.

### Modified Capabilities

- `module-boundaries`: состав приложения изменяется с восьми до девяти модулей; добавляется модуль `web` с разрешёнными зависимостями на `catalog`, `organizations`, `security` и `shared`.

## Impact

Затрагиваются `pom.xml`, новый пакет `com.hydroyura.forgeflow.web`, конфигурация Spring Modulith и тесты. REST-слой зависит от публичных API `catalog`, `organizations`, `security` и `shared`; домен, application и infrastructure каталогов остаются свободными от Spring-зависимостей. Текущий `StubAuthenticatedUserProvider` используется до интеграции Keycloak. Клиентский `X-Organization-Id` не является допустимым источником tenant-контекста.
