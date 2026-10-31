# Proposal

## Why

Базовый Java-пакет `com.forgeflow` не отражает требуемое пространство имён проекта. Механическое переименование в `com.hydroyura.forgeflow` устранит это расхождение без изменения поведения приложения и сохранит текущие границы Spring Modulith-модулей.

## What Changes

- Во всех Java-файлах production- и test-кода заменить пакет `com.forgeflow` на `com.hydroyura.forgeflow`, включая объявления `package`, импорты и `package-info.java`.
- Переместить корневой класс `ForgeFlowApplication` в пакет `com.hydroyura.forgeflow`.
- Обновить упоминания классов с префиксом `com.forgeflow` в главной спецификации каталога.
- Сохранить идентификаторы модулей Spring Modulith `catalog`, `organizations`, `security`, `shared`, `adminpanel`, `specifications`, `directories`, `technology`.
- Подтвердить отсутствие изменения поведения запуском полного набора тестов Maven.

## Capabilities

### New Capabilities

Нет: изменение является механическим рефакторингом и не добавляет системного поведения.

### Modified Capabilities

- `catalog-domain-model`: обновляются только ссылки на полные имена классов в сценариях; требования к доменной модели не меняются.

## Impact

Затрагиваются 122 Java-файла в `src/main/java` и `src/test/java`, пути исходных файлов, package/import references, корневой класс Spring Boot и архитектурный тест `ModuleVerificationTest`. Публичное поведение, идентификаторы модулей и бизнес-контракты не изменяются. Исторические файлы в `openspec/changes/archive/` не изменяются.
