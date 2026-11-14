# Tasks

## 1. Подготовка данных

- [ ] 1.1 Выписать из `docs/user-guide/catalog/examples.md` 24 детали (12 RGS.100 + 12 NAS.100), 19 сборок (13 RGS.100 + 6 NAS.100) и все составы сборок дословно; сверить полноту по разделам «… — детали», «… — сборки» и «… — состав сборок»
- [ ] 1.2 Сгенерировать детерминированные UUID для 24 деталей и 19 сборок (name-based UUID по `forgeflow:<type>:<number>` с суффиксом ` СБ` у сборок); проверить уникальность всех новых UUID среди себя и против существующих UUID в файле

## 2. Правка `seed.sql`

- [ ] 2.1 Заменить секцию `-- part` на 24 строки демо-организации `00000000-0000-0000-0000-000000000001` с `ON CONFLICT DO NOTHING`; убедиться, что старых `Д-0001…Д-0050` в файле не осталось
- [ ] 2.2 Заменить секцию `-- assembly` на 19 строк с обозначениями `RGS.100-000.ГГ.ППП СБ` / `NAS.100-000.ГГ.ППП СБ` (включая `RGS.100-000.00.100-01 СБ`); убедиться, что старых `СБ-0001…СБ-0052` в файле не осталось
- [ ] 2.3 В `INSERT INTO organization` установить `part_number_pattern = '(RGS|NAS)\.100-000\.\d{2}\.\d{3}'` и `assembly_number_pattern = '(RGS|NAS)\.100-000\.\d{2}\.\d{3}(-\d{2})? СБ'`; поля `buy_number_pattern`, `standart_number_pattern`, `vzk_number_pattern` оставить без изменений
- [ ] 2.4 Полностью переписать секцию `-- specification_row` по составам из `examples.md` (включая «Состав тот же, что и у …»); `item_type` — заглавные `ASSEMBLY`/`PART`/`VZK`; `VZK`-строки ссылаются на UUID засеянных позиций `VKZ.xxx`
- [ ] 2.5 Заменить секцию `-- substitution` ровно одной строкой: `assembly_id`=UUID `RGS.100-000.00.000 СБ`, `item_type`/`item_id`=`ASSEMBLY`/UUID `RGS.100-000.00.102 СБ`, `substitute_type`/`substitute_id`=`ASSEMBLY`/UUID `RGS.100-000.00.100-01 СБ`
- [ ] 2.6 Подтвердить, что секции `-- buy`, `-- standart`, `-- vzk` (обе части), `-- material`, `-- sortament`, `-- owner` побайтово не изменились (diff только по целевым секциям)

## 3. Автоматизированная проверка `seed.sql` (Testcontainers)

- [ ] 3.1 Создать `src/test/java/com/hydroyura/forgeflow/SeedSqlVerificationTest.java`: изолированный Testcontainers `postgres:17` (отдельная БД/схема), НЕ наследующий `AbstractPostgresIntegrationTest`, чтобы демо-организация не создавалась заранее с пустыми паттернами (см. D12)
- [ ] 3.2 Программно применить Flyway-миграции `V1…V8` (`Flyway.configure().dataSource(...).locations("classpath:db/migration").load().migrate()`) — строится только схема, `seed.sql` не участвует
- [ ] 3.3 Выполнить корневой `seed.sql` как обычный скрипт через `ResourceDatabasePopulator`/`JdbcTemplate` (НЕ через Flyway и НЕ через `@SpringBootTest`); читать файл из корня репозитория (`Paths.get("seed.sql")`/`FileSystemResource`), не дублируя его в `src/test/resources`
- [ ] 3.4 Добавить SQL-asserts через `JdbcTemplate`: `part`=24, `assembly`=19, `specification_row`=100, `substitution`=1; `GROUP BY item_type` = `ASSEMBLY`=17, `PART`=43, `VZK`=40, `BUY`/`STANDART`=0; отсутствие висячих UUID (`LEFT JOIN` к `part`/`assembly`/`vzk` — нет `NULL`)
- [ ] 3.5 Проверить идемпотентность: повторный прогон `seed.sql` на той же схеме — те же счётчики, без дубликатов и ошибок ограничений (AC-11)
- [ ] 3.6 Прогнать `./mvnw test -Dtest=SeedSqlVerificationTest` и убедиться, что тест зелёный; полный `./mvnw test` (включая `ModuleVerificationTest`) остаётся зелёным

## 4. Проверка SQL (psql, ручная)

- [ ] 4.1 На свежей БД: Flyway → `seed.sql`; проверить `SELECT count(*)` по `part`=24 (без `Д-`), `assembly`=19 (без `СБ-`) и что все номера самосогласованы с паттернами
- [ ] 4.2 Проверить `specification_row`: `GROUP BY item_type` даёт `ASSEMBLY`=17, `PART`=43, `VZK`=40, `BUY`/`STANDART`=0, всего 100; состав `RGS.100-000.00.000 СБ` = 9 прямых строк
- [ ] 4.3 Проверить дублирование: состав `RGS.100-000.00.100-01 СБ` равен составу `RGS.100-000.00.100 СБ`, `RGS.100-000.00.120 СБ` равен `RGS.100-000.00.115 СБ`, `NAS.100-000.00.120 СБ` равен `NAS.100-000.00.110 СБ`
- [ ] 4.4 Проверить flatten `RGS.100-000.00.000 СБ` → деталь `RGS.100-000.30.001` с итоговым количеством 4 (через приложение или ручной расчёт цепочек)
- [ ] 4.5 Проверить `substitution`=1 с указанными head/item/substitute и что чтение спецификации `00.000 СБ` возвращает `hasSubstitutes=true` для строки `00.102 СБ`
- [ ] 4.6 Проверить отсутствие висячих ссылок: каждый `assembly_id`, `item_id`, `substitute_id` из `specification_row`/`substitution` существует в `part`/`assembly`/`vzk` той же организации
- [ ] 4.7 Выполнить `seed.sql` повторно и подтвердить отсутствие дубликатов (те же `count`, без ошибок ограничений)

## 5. Актуализация main-спеков

- [ ] 5.1 Подтвердить, что дельты `specs/organizations/spec.md` и `specs/specifications/spec.md` отражают новые паттерны и перенос seed-спецификаций в `seed.sql` (требования, сценарии WHEN/THEN)
- [ ] 5.2 Прогнать `openspec validate --change 28-catalog-seed-data` и устранить замечания по дельтам спецификаций
