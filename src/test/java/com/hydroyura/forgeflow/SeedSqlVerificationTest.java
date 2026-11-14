package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Выполняет корневой {@code seed.sql} на свежей схеме (Testcontainers {@code postgres:17} +
 * программный Flyway {@code V1..V8}) и проверяет контрольные суммы демо-каталога и
 * демо-спецификаций.
 *
 * <p>Тест сознательно НЕ наследует {@link AbstractPostgresIntegrationTest} и не запускает
 * Spring-контекст: {@code seed.sql} вставляет организацию {@code ON CONFLICT (id) DO NOTHING},
 * поэтому на уже подготовленной БД паттерны организации остались бы пустыми.
 *
 * <p>UUID деталей и сборок в {@code seed.sql} — детерминированные name-based UUID v5 (SHA-1):
 * namespace {@code 6ba7b810-9dad-11d1-80b4-00c04fd430c8} (DNS), имя
 * {@code forgeflow:<type>:<number>}, у сборок {@code number} с суффиксом {@code " СБ"}.
 */
class SeedSqlVerificationTest {

    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("forgeflow_seed")
            .withUsername("forgeflow")
            .withPassword("forgeflow");

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void setUpSchemaAndSeed() {
        POSTGRES.start();
        dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        runSeedSql();
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    private static void runSeedSql() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new FileSystemResource(Paths.get("seed.sql")));
        populator.execute(dataSource);
    }

    @Test
    void seedContainsExactlyTwentyFourPartsWithoutLegacyNumbers() {
        Long parts = jdbc.queryForObject(
                "SELECT count(*) FROM part WHERE organization_id = ?", Long.class, DEMO_ORG);
        Long legacyParts = jdbc.queryForObject(
                "SELECT count(*) FROM part WHERE organization_id = ? AND number LIKE 'Д-%'",
                Long.class, DEMO_ORG);

        assertThat(parts).isEqualTo(24);
        assertThat(legacyParts).isZero();
    }

    @Test
    void seedContainsExactlyNineteenAssembliesWithoutLegacyNumbers() {
        Long assemblies = jdbc.queryForObject(
                "SELECT count(*) FROM assembly WHERE organization_id = ?", Long.class, DEMO_ORG);
        Long legacyAssemblies = jdbc.queryForObject(
                "SELECT count(*) FROM assembly WHERE organization_id = ? AND number LIKE 'СБ-%'",
                Long.class, DEMO_ORG);

        assertThat(assemblies).isEqualTo(19);
        assertThat(legacyAssemblies).isZero();
    }

    @Test
    void seedContainsExactlyOneHundredSpecificationRows() {
        Long rows = jdbc.queryForObject(
                "SELECT count(*) FROM specification_row WHERE organization_id = ?", Long.class, DEMO_ORG);

        assertThat(rows).isEqualTo(100);
    }

    @Test
    void seedContainsExactlyOneSubstitution() {
        Long substitutions = jdbc.queryForObject(
                "SELECT count(*) FROM substitution WHERE organization_id = ?", Long.class, DEMO_ORG);

        assertThat(substitutions).isEqualTo(1);
    }

    @Test
    void specificationItemTypesMatchExpectedDistribution() {
        Map<String, Long> distribution = itemTypeDistribution();

        Map<String, Long> expected = new LinkedHashMap<>();
        expected.put("ASSEMBLY", 17L);
        expected.put("PART", 43L);
        expected.put("VZK", 40L);
        expected.put("BUY", 0L);
        expected.put("STANDART", 0L);

        assertThat(distribution).containsExactlyInAnyOrderEntriesOf(expected);
        assertThat(distribution.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100);
    }

    @Test
    void seedHasNoDanglingReferences() {
        Long danglingAssembly = jdbc.queryForObject("""
                SELECT count(*)
                FROM specification_row sr
                LEFT JOIN assembly a ON a.id = sr.assembly_id AND a.organization_id = sr.organization_id
                WHERE sr.organization_id = ? AND a.id IS NULL
                """, Long.class, DEMO_ORG);

        Long danglingItem = jdbc.queryForObject("""
                SELECT count(*)
                FROM specification_row sr
                LEFT JOIN part p ON sr.item_type = 'PART' AND p.id = sr.item_id AND p.organization_id = sr.organization_id
                LEFT JOIN assembly a ON sr.item_type = 'ASSEMBLY' AND a.id = sr.item_id AND a.organization_id = sr.organization_id
                LEFT JOIN vzk v ON sr.item_type = 'VZK' AND v.id = sr.item_id AND v.organization_id = sr.organization_id
                WHERE sr.organization_id = ?
                  AND ((sr.item_type = 'PART' AND p.id IS NULL)
                    OR (sr.item_type = 'ASSEMBLY' AND a.id IS NULL)
                    OR (sr.item_type = 'VZK' AND v.id IS NULL))
                """, Long.class, DEMO_ORG);

        Long danglingSubstitution = jdbc.queryForObject("""
                SELECT count(*)
                FROM substitution s
                LEFT JOIN assembly head ON head.id = s.assembly_id AND head.organization_id = s.organization_id
                LEFT JOIN part ip ON s.item_type = 'PART' AND ip.id = s.item_id AND ip.organization_id = s.organization_id
                LEFT JOIN assembly ia ON s.item_type = 'ASSEMBLY' AND ia.id = s.item_id AND ia.organization_id = s.organization_id
                LEFT JOIN vzk iv ON s.item_type = 'VZK' AND iv.id = s.item_id AND iv.organization_id = s.organization_id
                LEFT JOIN part sp ON s.substitute_type = 'PART' AND sp.id = s.substitute_id AND sp.organization_id = s.organization_id
                LEFT JOIN assembly sa ON s.substitute_type = 'ASSEMBLY' AND sa.id = s.substitute_id AND sa.organization_id = s.organization_id
                LEFT JOIN vzk sv ON s.substitute_type = 'VZK' AND sv.id = s.substitute_id AND sv.organization_id = s.organization_id
                WHERE s.organization_id = ?
                  AND (head.id IS NULL
                    OR (s.item_type = 'PART' AND ip.id IS NULL)
                    OR (s.item_type = 'ASSEMBLY' AND ia.id IS NULL)
                    OR (s.item_type = 'VZK' AND iv.id IS NULL)
                    OR (s.substitute_type = 'PART' AND sp.id IS NULL)
                    OR (s.substitute_type = 'ASSEMBLY' AND sa.id IS NULL)
                    OR (s.substitute_type = 'VZK' AND sv.id IS NULL))
                """, Long.class, DEMO_ORG);

        assertThat(danglingAssembly).isZero();
        assertThat(danglingItem).isZero();
        assertThat(danglingSubstitution).isZero();
    }

    @Test
    void substitutionPointsToExpectedHeadItemAndSubstitute() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT head.number AS head_number,
                       item.number AS item_number,
                       s.item_type,
                       substitute.number AS substitute_number,
                       s.substitute_type
                FROM substitution s
                JOIN assembly head ON head.id = s.assembly_id
                JOIN assembly item ON item.id = s.item_id
                JOIN assembly substitute ON substitute.id = s.substitute_id
                WHERE s.organization_id = ?
                """, DEMO_ORG);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0))
                .containsEntry("head_number", "RGS.100-000.00.000 СБ")
                .containsEntry("item_number", "RGS.100-000.00.102 СБ")
                .containsEntry("item_type", "ASSEMBLY")
                .containsEntry("substitute_number", "RGS.100-000.00.100-01 СБ")
                .containsEntry("substitute_type", "ASSEMBLY");
    }

    @Test
    void organizationPatternsAreStoredVerbatim() {
        Map<String, Object> patterns = jdbc.queryForMap("""
                SELECT part_number_pattern,
                       assembly_number_pattern,
                       buy_number_pattern,
                       standart_number_pattern,
                       vzk_number_pattern
                FROM organization
                WHERE id = ?
                """, DEMO_ORG);

        assertThat(patterns)
                .containsEntry("part_number_pattern", "(RGS|NAS)\\.100-000\\.\\d{2}\\.\\d{3}")
                .containsEntry("assembly_number_pattern", "(RGS|NAS)\\.100-000\\.\\d{2}\\.\\d{3}(-\\d{2})? СБ")
                .containsEntry("buy_number_pattern", "ПОК-\\d{4}")
                .containsEntry("standart_number_pattern", "СТ-\\d{4}")
                .containsEntry("vzk_number_pattern", "VKZ\\.\\d{3}-\\d{3}\\.\\d{2}\\.\\d{3}");
    }

    @Test
    void vzkSpecificationRowsReferenceOnlySeededVkzPositions() {
        Long vzkRows = jdbc.queryForObject("""
                SELECT count(*)
                FROM specification_row sr
                JOIN vzk v ON v.id = sr.item_id AND v.organization_id = sr.organization_id
                WHERE sr.organization_id = ? AND sr.item_type = 'VZK' AND v.number LIKE 'VKZ.%'
                """, Long.class, DEMO_ORG);

        Long legacyVzkRows = jdbc.queryForObject("""
                SELECT count(*)
                FROM specification_row sr
                JOIN vzk v ON v.id = sr.item_id AND v.organization_id = sr.organization_id
                WHERE sr.organization_id = ? AND sr.item_type = 'VZK' AND v.number LIKE 'ВЗК-%'
                """, Long.class, DEMO_ORG);

        Long distinctVzk = jdbc.queryForObject("""
                SELECT count(DISTINCT v.number)
                FROM specification_row sr
                JOIN vzk v ON v.id = sr.item_id AND v.organization_id = sr.organization_id
                WHERE sr.organization_id = ? AND sr.item_type = 'VZK' AND v.number LIKE 'VKZ.%'
                """, Long.class, DEMO_ORG);

        assertThat(vzkRows).isEqualTo(40);
        assertThat(legacyVzkRows).isZero();
        assertThat(distinctVzk).isEqualTo(13);
    }

    @Test
    void seedIsIdempotentOnSecondRun() {
        runSeedSql();

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM part WHERE organization_id = ?", Long.class, DEMO_ORG)).isEqualTo(24);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM assembly WHERE organization_id = ?", Long.class, DEMO_ORG)).isEqualTo(19);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM specification_row WHERE organization_id = ?", Long.class, DEMO_ORG)).isEqualTo(100);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM substitution WHERE organization_id = ?", Long.class, DEMO_ORG)).isEqualTo(1);
    }

    private Map<String, Long> itemTypeDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("ASSEMBLY", 0L);
        distribution.put("PART", 0L);
        distribution.put("VZK", 0L);
        distribution.put("BUY", 0L);
        distribution.put("STANDART", 0L);

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT item_type, count(*) AS cnt
                FROM specification_row
                WHERE organization_id = ?
                GROUP BY item_type
                """, DEMO_ORG);

        for (Map<String, Object> row : rows) {
            distribution.put((String) row.get("item_type"), ((Number) row.get("cnt")).longValue());
        }
        return distribution;
    }
}
