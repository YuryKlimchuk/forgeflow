# Proposal

## Why

Модуль `catalog` содержит только идентификаторы сущностей и не может сохранять обязательные атрибуты каталожных объектов, обеспечивать изоляцию по организациям или выполнять логическое удаление. Сейчас необходимо расширить доменную модель минимально необходимыми данными и портами репозиториев, не добавляя инфраструктурные адаптеры и неподтверждённые бизнес-правила.

## What Changes

- **BREAKING** Изменить требование минимальности `catalog-domain-model`: пять сущностей SHALL содержать `number`, `name`, `organizationId`, `createdAt`, `updatedAt` и состояние soft delete.
- Добавить во все пять сущностей базовую валидацию непустых `number` и `name` с fail-fast ошибками.
- Добавить доменные порты репозиториев для `Assembly`, `Part`, `Buy`, `Standart` и `Vzk` с операциями `save(entity)` и `delete(id)`.
- Зафиксировать soft delete через `deletedAt` и доменный метод `markDeleted()`; `delete(id)` SHALL означать логическое удаление.
- Сохранить типизированные ID в `catalog.api`, UUID `organizationId` и существующую границу зависимости модуля.
- Не добавлять JPA/Spring-инфраструктуру, миграции или production-адаптеры репозиториев.

## Capabilities

### New Capabilities

Нет: изменение расширяет существующую capability каталожной доменной модели.

### Modified Capabilities

- `catalog-domain-model`: изменяется состав сущностей, добавляются обязательные поля, audit/organization-данные, soft delete и доменные repository ports.

## Impact

Затрагиваются доменные классы `com.forgeflow.catalog.domain.*`, новые интерфейсы портов в доменном слое и тесты. Публичные ID в `com.forgeflow.catalog.api` и межмодульная зависимость `catalog -> directories` сохраняются. Maven-зависимость `commons-lang3` потребуется добавить для требуемой базовой проверки строк; Spring, JPA, PostgreSQL, Flyway и production-адаптеры не затрагиваются.
