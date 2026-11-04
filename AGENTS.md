# ForgeFlow — инструкции для агента

## Что это

ForgeFlow — SaaS-платформа для управления электронными спецификациями изделий (BOM) и расчёта
потребностей производства. Простая альтернатива ERP/MRP для малых производственных предприятий.

## Стек

- Java 25, Maven, Spring Boot 4.1.x
- Spring Modulith — модульный монолит (переход к микросервисам не является целью)
- DDD + Hexagonal Architecture (порты и адаптеры), jMolecules
- PostgreSQL + Flyway; Keycloak — аутентификация, бизнес-права проверяет ForgeFlow
- Тесты: JUnit 5, интеграционные и архитектурные (`ApplicationModules.verify()`)

## Архитектурные правила (обязательные)

- Бизнес-логика — в доменном слое. **Домен не зависит от Spring, JPA, PostgreSQL и внешних интеграций.**
- Модули имеют чёткие границы и направленные зависимости без циклов
  (`@ApplicationModule`, `@NamedInterface`, межмодульная связь только через api-пакеты).
- Схема БД меняется **только** версионируемыми миграциями Flyway.
- Multi-tenant: изоляция данных организаций на уровне приложения — общая схема БД, колонка `organization_id`, фильтрация в приложении; schema-per-tenant не используется; проверяется тестами.
- Богатая доменная модель, а не анемичный CRUD.

## Правила разработки

- Новые функциональные изменения проходят по AgentCraft workflow и описываются через OpenSpec на этапе Architect.
- Новая функциональность сопровождается автоматизированными тестами.
- **Не придумывать бизнес-правила**: неопределённости фиксировать и согласовывать до реализации.
- Сохранять совместимость существующих контрактов, если их изменение не входит в задачу.
- Избегать необоснованных зависимостей, усложнения инфраструктуры и преждевременной оптимизации.
- Перед любой работой над модулем `catalog` перечитывать `docs/catalog-subdomains.md` —
  карту саб-доменов и их отображение на разделы спецификации ЕСКД.

## Кодовый стиль и конвенции

Применяй профильные скиллы по мере необходимости:

- `java-code-style` — утилиты Apache Commons/JDK вместо ручных проверок
  (`Objects.requireNonNull`, `StringUtils.isBlank`, `CollectionUtils.isEmpty`)
- `domain-driven-design` — богатая доменная модель (агрегаты, value objects, доменные события)
- `hexagonal-architecture` — порты и адаптеры, домен без Spring/JPA
- `spring-modulith-module` — структура пакетов `api/application/domain/infrastructure`,
  `@ApplicationModule`, `@NamedInterface`

## Workflow (AgentCraft)

Работа над изменениями идёт по 8 этапам. Координирует процесс Orchestrator (ведущий агент);
этапы делегируются сабагентам через `pi-subagents`. Переходы контролируются Quality Gates.

1. **Preparation** — задача и окружение готовы, рабочая ветка создана (G1).
2. **Analyst** (`analyst`) — требования и Acceptance Criteria; **утверждает пользователь** (G2).
3. **Architect** (`architect`) — OpenSpec-артефакты: proposal, specs, design, tasks (G3).
4. **QA Planning** (`qa-planner`) — Test plan и оценка рисков; **пакет утверждает пользователь** (G4).
5. **Developer** (`dev-implementer`) — реализация, тесты, первичные проверки (G5).
6. **Reviewer** (`dev-reviewer`) — независимое код-ревью (G6).
7. **QA Testing** (`qa-tester`) — фактическая проверка по Test plan (G7).
8. **Finalization and Pull Request** — финальный отчёт и PR (G8).

Детали этапов и переходов: `docs/agentcraft/workflow.md`.
Роль Orchestrator и правила делегирования: `docs/agentcraft/orchestrator.md`.
Условия Quality Gates: `docs/agentcraft/quality-gates.md`.

Реализация не начинается, пока не утверждены требования (G2) и пакет «дизайн + Test plan» (G4).

## Язык и нумерация

- Артефакты OpenSpec, требования и документация — **на русском языке**.
- Имена change — kebab-case; каждый proposal нумеруется по порядку (0, 1, 2, …).
- Общение с пользователем — на русском.

## OpenSpec

OpenSpec используется на этапах **Architect** (proposal/specs/design/tasks) и **QA Planning**
(test-plan.md). Конвенции и CLI: `docs/agentcraft/openspec.md`, скилл `openspec`.
