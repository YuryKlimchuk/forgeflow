# organization-context Specification

## Purpose
Capability предоставляет бизнес-модулям текущую организацию через стабильный read-only контракт и связывает её с серверно определённым пользователем на ограниченном жизненном цикле запроса. Общие типы контракта находятся в `shared.api`, который не зависит от `organizations`.

## Requirements

### Requirement: Общий идентификатор организации
`shared.api` SHALL предоставлять `OrganizationId` как record с компонентом `UUID value` и фабриками `generate()` и `of()`; `organizations.api` SHALL не объявлять этот тип.

#### Scenario: Использование общего идентификатора
- **WHEN** модуль `organizations` создаёт или принимает идентификатор организации
- **THEN** он использует тип `shared.api.OrganizationId`, а `shared` не импортирует `organizations`

### Requirement: Read-only organization context
`shared.api.OrganizationContext` SHALL предоставлять `Optional<shared.api.OrganizationId> currentOrganizationId()` и `shared.api.OrganizationId requiredOrganizationId()`, а `requiredOrganizationId()` SHALL выбрасывать специализированное исключение из `shared.api`, если организация отсутствует.

#### Scenario: Получение установленной организации
- **WHEN** контекст текущего запроса содержит `shared.api.OrganizationId`
- **THEN** `currentOrganizationId()` возвращает `Optional`, содержащий этот идентификатор, а `requiredOrganizationId()` возвращает тот же идентификатор

#### Scenario: Отсутствующий контекст
- **WHEN** текущий запрос не связан с организацией
- **THEN** `currentOrganizationId()` возвращает пустой `Optional`, а `requiredOrganizationId()` выбрасывает специализированное исключение отсутствующего контекста

### Requirement: Инфраструктурное управление контекстом
Инфраструктурная реализация контекста SHALL предоставлять операции установки и очистки, недоступные через публичный контракт `OrganizationContext` для бизнес-модулей.

#### Scenario: Очистка установленного значения
- **WHEN** инфраструктурный lifecycle вызывает очистку после запроса
- **THEN** последующий `currentOrganizationId()` в том же потоке возвращает пустой `Optional`

### Requirement: Разрешение организации по authenticated subject
Резолвер организации SHALL получать `UserId` и realm из порта текущего аутентифицированного пользователя и SHALL использовать принадлежность из `organizations` для определения `shared.api.OrganizationId`.

#### Scenario: Пользователь forgeflow-users с принадлежностью
- **WHEN** текущий authenticated user имеет realm `forgeflow-users`, а `organizations` возвращает `OwnerOf` или `MemberOf` с `shared.api.OrganizationId`
- **THEN** lifecycle устанавливает этот идентификатор в контекст

#### Scenario: Пользователь без организации
- **WHEN** текущий authenticated user не имеет принадлежности организации
- **THEN** lifecycle не устанавливает организацию, и `currentOrganizationId()` возвращает пустой `Optional`

### Requirement: Realm policy
Пользователь realm `forgeflow-admins` SHALL не получать автоматический контекст организации, даже если для его `UserId` существует принадлежность в `organizations`.

#### Scenario: Администраторский realm
- **WHEN** текущий authenticated user имеет realm `forgeflow-admins`
- **THEN** resolver не запрашивает организационный контекст для бизнес-запроса и `currentOrganizationId()` остаётся пустым

### Requirement: Request lifecycle isolation
Lifecycle-обёртка SHALL устанавливать разрешённый контекст до выполнения обработчика, SHALL очищать его в `finally` после успешного или аварийного завершения обработчика и SHALL не использовать `organizationId` из HTTP-запроса клиента как источник контекста.

#### Scenario: Успешный запрос
- **WHEN** обработчик выполняется после успешного разрешения организации
- **THEN** обработчик видит разрешённый `shared.api.OrganizationId`, а после завершения запроса контекст очищен

#### Scenario: Исключение обработчика
- **WHEN** обработчик выбрасывает исключение
- **THEN** lifecycle сохраняет исключение для вызывающего кода и очищает контекст до выхода из обёртки

#### Scenario: Последовательные запросы в одном потоке
- **WHEN** запрос организации A завершается, а затем в том же потоке выполняется запрос без организации
- **THEN** второй запрос не видит `OrganizationId` организации A

### Requirement: Explicit organization propagation for asynchronous work
Бизнес-модули SHALL получать организацию только через `shared.api.OrganizationContext` и SHALL передавать `shared.api.OrganizationId` явно в repository и асинхронные или фоновые операции; прямое использование `ThreadLocal` бизнес-модулями SHALL быть запрещено.

#### Scenario: Application service каталога
- **WHEN** application service `catalog` выполняет операцию tenant-данных
- **THEN** сервис получает `shared.api.OrganizationId` через `requiredOrganizationId()`, а repository получает этот идентификатор отдельным аргументом

#### Scenario: Фоновая задача
- **WHEN** бизнес-операция запускает асинхронную или фоновую задачу
- **THEN** задача получает `shared.api.OrganizationId` явным параметром и не рассчитывает на автоматическое наследование контекста
