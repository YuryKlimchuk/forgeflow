# Proposal

## Why

ForgeFlow пока не имеет базового каркаса приложения, который фиксирует границы доменных модулей и позволяет безопасно развивать SaaS-платформу как модульный монолит. Сейчас необходимо создать проверяемую архитектурную основу до добавления бизнес-функциональности.

## What Changes

- Создать Maven-проект на Java 25 с Spring Boot 4.1+ и зависимостями Spring Modulith и jMolecules DDD.
- Добавить главный класс приложения `ForgeFlowApplication`.
- Объявить восемь пустых модулей: `organizations`, `adminpanel`, `technology`, `specifications`, `catalog`, `directories`, `shared`, `security`.
- Для каждого модуля создать `package-info.java` с `@ApplicationModule` и минимальным белым списком разрешённых зависимостей.
- Создать в каждом модуле пустые слои `api`, `application`, `domain`, `infrastructure` без бизнес-классов.
- Ограничить межмодульный доступ публичными `api`-пакетами и именованными интерфейсами Spring Modulith.
- Добавить архитектурный тест `ModuleVerificationTest` с вызовом `ApplicationModules.of(ForgeFlowApplication.class).verify()`.

## Capabilities

### New Capabilities

- `project-skeleton`: базовая Maven-конфигурация, главный класс приложения и проверяемая структура модульного монолита ForgeFlow.
- `module-boundaries`: восемь объявленных Spring Modulith-модулей, их разрешённые зависимости и правила публичного API.

### Modified Capabilities

Нет существующих спецификаций для изменения.

## Impact

Изменение затрагивает корневой `pom.xml`, структуру `src/main/java` и `src/test/java`, а также архитектурные зависимости Spring Boot, Spring Modulith и jMolecules. Бизнес-логика, API приложения, хранилища, интеграция с Keycloak и функциональность модулей в этот change не входят.
