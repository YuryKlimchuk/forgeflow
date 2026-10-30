# Tasks

## 1. Shared API контекста

- [x] 1.1 Перенести record `OrganizationId` с компонентом `UUID value` и фабриками `generate()`/`of()` из `organizations.api` в `shared.api`; проверить публичный пакет и компиляцию.
- [x] 1.2 Добавить `shared.api.OrganizationContext` с методами `currentOrganizationId()` и `requiredOrganizationId()`; проверить, что контракт доступен через named interface `shared` и `required` возвращает установленный идентификатор.
- [x] 1.3 Добавить специализированное исключение отсутствующего контекста в `shared.api`; проверить тестом пустой контекст и выброс при `requiredOrganizationId()`.

## 2. Security-реализация и realm-заглушка

- [x] 2.1 Добавить `ThreadLocalOrganizationContext` с недоступными бизнес-модулям операциями установки/очистки; проверить установку, чтение и очистку unit-тестами.
- [x] 2.2 Добавить модель `AuthenticatedUser(userId, realm)` и порт получения текущего authenticated user в `security`; проверить тестовой заглушкой значения subject и realm.
- [x] 2.3 Добавить `OrganizationContextResolver`, использующий `OrganizationService.getAffiliation(UserId)` и правила `forgeflow-users`/`forgeflow-admins`; проверить owner, member, отсутствие организации и admins без контекста.
- [x] 2.4 Добавить lifecycle-обёртку `try/finally` для установки и очистки контекста; проверить успешный обработчик, исключение обработчика и отсутствие утечки между последовательными запросами.

## 3. Публичный контракт organizations

- [x] 3.1 Обновить `organizations` на импорт `shared.api.OrganizationId` во всех затронутых типах (`Organization`, `Owner`, `OrganizationView`, `Affiliation`, `OrganizationService` и связанных классах), удалить тип из `organizations.api`, а `UserId` и `OwnerId` оставить в `organizations.api`; проверить компиляцию и тесты.
- [x] 3.2 Добавить `OrganizationService.getAffiliation(UserId)` и перенаправить существующий `getAffiliation()` через subject provider без изменения остальных правил принадлежности; проверить owner/member/none и эквивалентность двух overload-ов.
- [x] 3.3 Обновить `organizations/package-info.java`, разрешив зависимость `shared` и сохранив доступ к нему через `shared.api`; проверить архитектурную верификацию.
- [x] 3.4 Проверить, что resolver использует явный subject и не дублирует логику определения принадлежности; зафиксировать это интеграционными тестами security/organizations.

## 4. Границы модулей

- [x] 4.1 Обновить `security/package-info.java` и `catalog/package-info.java` разрешением зависимости от `shared`, сохранив остальные зависимости; проверить `ModuleVerificationTest` и отсутствие циклов.
- [x] 4.2 Проверить, что `shared` не зависит от `security`, `catalog`, `organizations` или других бизнес-модулей, а бизнес-модули не импортируют `ThreadLocalOrganizationContext`/`ThreadLocal`; результат подтвердить архитектурным тестом или анализом зависимостей.

## 5. Пример catalog

- [x] 5.1 Добавить минимальный application service catalog, который инжектирует `OrganizationContext`, вызывает `requiredOrganizationId()` и передаёт `shared.api.OrganizationId` явно в repository; проверить тестом вызов repository с идентификатором и отказ без контекста.
- [x] 5.2 Не расширять catalog бизнес-логику сверх примера и документировать в коде границу между context API и repository-портом; проверить, что catalog зависит только от `shared.api` и `directories.api`.

## 6. Интеграционная проверка

- [x] 6.1 Выполнить полный Maven test suite и проверить тесты переноса/импортов `OrganizationId`, установки/получения, пустого Optional, required exception, очистки, lifecycle exception, последовательных запросов и admins realm.
- [x] 6.2 Выполнить `openspec validate 4-organization-context` и убедиться, что все planning-артефакты и delta-specs проходят валидацию.
