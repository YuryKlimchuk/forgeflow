# Tasks

## 1. Переименование Java-пакетов

- [x] 1.1 Выполнить замену `com.forgeflow` на `com.hydroyura.forgeflow` во всех Java-файлах `src/main/java` и `src/test/java`, включая все `package`-объявления, imports и `package-info.java`; проверить поиском, что в этих каталогах не осталось старого префикса.
- [x] 1.2 Переместить каталоги исходников из `src/main/java/com/forgeflow` и `src/test/java/com/forgeflow` в соответствующие пути `com/hydroyura/forgeflow`; проверить наличие `ForgeFlowApplication` в `src/main/java/com/hydroyura/forgeflow` и отсутствие старой Java-иерархии.

## 2. Обновление OpenSpec-ссылок

- [x] 2.1 Заменить `com.forgeflow` на `com.hydroyura.forgeflow` в `openspec/specs/catalog-domain-model/spec.md`; проверить, что ссылки на классы каталога используют новый пакет, а `openspec/changes/archive/` не изменён.
- [x] 2.2 Не изменять `openspec/specs/module-boundaries/spec.md`; проверить, что в дельте отсутствуют требования о переименовании идентификаторов модулей `catalog`, `organizations`, `security`, `shared`, `adminpanel`, `specifications`, `directories`, `technology`.

## 3. Проверка сборки и поведения

- [x] 3.1 Запустить `mvn test`; проверить успешное прохождение всех 97 тестов, включая `ModuleVerificationTest` с `ApplicationModules.of(ForgeFlowApplication.class)`.
- [x] 3.2 Выполнить финальный поиск `com.forgeflow` вне `openspec/changes/archive/`; проверить, что старый префикс отсутствует в Java-файлах и главной спецификации, а оставшиеся совпадения, если есть, документированы как не относящиеся к области change.
- [x] 3.3 Запустить `openspec status --change "6-rename-base-package"` и `openspec validate --change "6-rename-base-package"`; проверить, что все planning-артефакты присутствуют и change проходит валидацию.
