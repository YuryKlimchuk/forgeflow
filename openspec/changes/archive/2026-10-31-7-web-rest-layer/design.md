# Design

## Context

Текущий проект содержит `catalog.api.PartService` и DTO `PartView`/`CreatePartRequest`, но не имеет Spring MVC-зависимости и web-модуля. `PartApplicationService` требует `PartRepository`, `OrganizationContext`, `PartMapper` и `Clock`; security уже предоставляет `OrganizationContextLifecycle`, `OrganizationContextResolver`, `StubAuthenticatedUserProvider` и `ThreadLocalOrganizationContext`. См. `proposal.md` и delta-спеки для внешнего контракта.

## Goals / Non-Goals

**Goals:**

- Создать composition root, который собирает существующие порты и адаптеры без загрязнения доменных модулей Spring-аннотациями.
- Предоставить стабильный `/api/v1/parts` REST-контракт.
- Сохранить серверное tenant-разрешение и очистку контекста при успехе и исключении.
- Сохранить проверяемые границы Spring Modulith.

**Non-Goals:**

- Интеграция Keycloak или реализация полноценной аутентификации.
- REST-эндпоинты для остальных типов каталога.
- Изменение доменного контракта `PartService`, persistence или фронтенд React.

## Decisions

### D1. Web как composition root

В `com.hydroyura.forgeflow.web` создаётся `@ApplicationModule`, а wiring оформляется через `@Configuration` и `@Bean`. В конфигурации создаются `InMemoryPartRepository`, `ThreadLocalOrganizationContext`, `Clock`, `PartMapper.INSTANCE`, `PartApplicationService`, security lifecycle и MVC-контроллеры. Альтернатива — `@Component`/`@Service` в доменных модулях — отклонена, поскольку нарушает чистоту domain/application/infrastructure.

### D2. Тонкий MVC-адаптер

Контроллер принимает URL-параметры и переиспользуемые API DTO, преобразует идентификатор в `PartId` и делегирует `PartService`; бизнес-правила остаются в application service. Для изменения имени выбран `PUT /api/v1/parts/{id}/name` с ответом 204 как идемпотентным по форме командным контрактом.

### D3. Tenant lifecycle через security

MVC-фильтр реализуется как `OncePerRequestFilter` и оборачивает `filterChain.doFilter` вызовом `OrganizationContextLifecycle.run`. Lifecycle использует `StubAuthenticatedUserProvider` и `OrganizationContextResolver`; очистка выполняется в его `finally`. Фильтр не читает `X-Organization-Id`. Это сохраняет единый уже протестированный механизм и не дублирует resolver в web.

### D4. Ошибки через минимальный advice

`@RestControllerAdvice` содержит только маппинги `PartNotFoundException -> 404`, `DuplicatePartNumberException -> 409`, `IllegalArgumentException` и ошибок валидации -> 400. Формат тела ошибки должен быть простым и стабильным для тестирования; отдельная модель ошибок и трассировка вне scope.

### D5. Публичные границы модулей

Web использует API-пакеты для `PartService`, DTO, `OrganizationContext` и security-портов. Для wiring конкретных реализаций допускается разрешённый доступ к публичным типам, экспортированным security/catalog infrastructure согласно существующей структуре границ; при этом бизнес-код контроллера не обращается к domain.

### D6. Переиспользование DTO

JSON сериализация/десериализация выполняется Jackson непосредственно для `catalog.api.PartView` и `CreatePartRequest`. Web не создаёт копии DTO. Для rename тело содержит поле `name`, а list передаёт пустой `PartFilter`.

## Risks / Trade-offs

- **[Risk]** `StubAuthenticatedUserProvider` не извлекает реальный bearer-токен. → **Mitigation:** использовать его явно как временный provider до отдельного Keycloak change; не принимать tenant из заголовка.
- **[Risk]** In-memory repository теряет данные при рестарте. → **Mitigation:** это уже текущая инфраструктурная реализация и persistence не входит в данный change.
- **[Risk]** Прямое wiring infrastructure-типов может конфликтовать с ограничениями Modulith. → **Mitigation:** проверить `ApplicationModules.verify()` и явно настроить allowedDependencies/NamedInterface, не ослабляя границы для контроллеров.
- **[Risk]** DTO без Bean Validation-аннотаций не дают полного семантического контроля входа. → **Mitigation:** обработать `IllegalArgumentException`/validation как 400 и покрыть контракт тестами; расширенные правила валидации оставить отдельному change.

## Migration Plan

Добавление starter-web не требует миграции данных. После внедрения проверить Maven-тесты и `ApplicationModules.verify()`. Откат выполняется удалением web-артефактов и зависимости starter-web; существующие catalog/security API не меняются.

## Open Questions

Нет: путь rename, временный provider, источник tenant и границы scope согласованы до создания change.
