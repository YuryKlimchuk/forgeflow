# Spec Delta

## Purpose

Определяет минимальный запускаемый каркас ForgeFlow как Maven-приложения на Java 25 с единым корневым пакетом и проверяемой архитектурой модульного монолита.

## ADDED Requirements

### Requirement: Maven-проект ForgeFlow
Корневой проект SHALL содержать `pom.xml`, задающий Java 25, Spring Boot версии 4.1 или выше, Spring Modulith и jMolecules DDD как зависимости приложения.

#### Scenario: Проверка базовой конфигурации
- **WHEN** выполняется сборка проекта Maven
- **THEN** проект разрешает зависимости Spring Boot, Spring Modulith и jMolecules DDD и компилируется с уровнем Java 25

### Requirement: Главный класс приложения
Проект SHALL содержать класс `ForgeFlowApplication` в корневом пакете приложения с точкой входа Spring Boot.

#### Scenario: Обнаружение приложения
- **WHEN** Spring Boot запускается через `ForgeFlowApplication`
- **THEN** приложение использует этот класс как источник конфигурации и корень обнаружения модулей

### Requirement: Пустые слои каркаса
Каждый модуль SHALL содержать пакеты `api`, `application`, `domain` и `infrastructure` без бизнес-классов и прикладной логики.

#### Scenario: Проверка пустого каркаса
- **WHEN** проверяется содержимое исходного кода модулей до реализации функциональности
- **THEN** в указанных слоях отсутствуют бизнес-сущности, use case'ы, репозитории, адаптеры и контроллеры

### Requirement: Архитектурная проверка модулей
Тестовый набор SHALL содержать `ModuleVerificationTest`, который проверяет модель модулей через `ApplicationModules.of(ForgeFlowApplication.class).verify()`.

#### Scenario: Успешная проверка границ
- **WHEN** тест `ModuleVerificationTest` запускается на каркасе проекта
- **THEN** проверка завершается успешно и не обнаруживает циклических зависимостей или обращений к запрещённым пакетам
