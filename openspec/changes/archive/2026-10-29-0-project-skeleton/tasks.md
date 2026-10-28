# Tasks

## 1. Maven-каркас и точка входа

- [x] 1.1 Создать корневой `pom.xml` с Java 25, совместимой версией Spring Boot 4.1+, BOM/управлением версиями Spring Modulith и jMolecules DDD; проверить конфигурацию командой `mvn validate`.
- [x] 1.2 Создать `src/main/java/com/forgeflow/ForgeFlowApplication.java` с точкой входа Spring Boot; проверить компиляцию командой `mvn test -DskipTests`.

## 2. Структура и декларации модулей

- [x] 2.1 Создать корневые пакеты и `package-info.java` для `organizations`, `adminpanel`, `technology`, `specifications`, `catalog`, `directories`, `shared` и `security` с `@ApplicationModule`; проверить наличие восьми деклараций скриптом поиска файлов и аннотаций.
- [x] 2.2 Создать в каждом из восьми модулей пустые пакеты `api`, `application`, `domain` и `infrastructure`, добавив `package-info.java` там, где это нужно для сохранения пакетов; проверить отсутствие бизнес-классов в этих слоях.
- [x] 2.3 Объявить публичные `api`-пакеты как именованные интерфейсы Spring Modulith без добавления бизнес-типов; проверить, что межмодульные публичные точки входа находятся только под `api`.
- [x] 2.4 Настроить `allowedDependencies` согласно графу: `adminpanel -> organizations`, `technology -> catalog, directories`, `specifications -> catalog, directories`, `catalog -> directories`, `security -> organizations`; проверить отсутствие разрешений для остальных связей.

## 3. Архитектурная проверка

- [x] 3.1 Создать `src/test/java/com/forgeflow/ModuleVerificationTest.java` с вызовом `ApplicationModules.of(ForgeFlowApplication.class).verify()`; проверить успешным запуском `mvn test`.
- [x] 3.2 Проверить интеграцию всех деклараций и отсутствие циклов/обращений к внутренним слоям командой `mvn test`; зафиксировать результат как критерий готовности каркаса.
