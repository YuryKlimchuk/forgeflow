package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/**
 * Проверка поставляемых seed-файлов и детерминизма генератора.
 */
class SeedDataFilesTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<SeedEntry> catalog() throws Exception {
        return List.of(objectMapper.readValue(Path.of("seed-data", "catalog.json").toFile(), SeedEntry[].class));
    }

    private List<SeedSpecification> specifications() throws Exception {
        return List.of(objectMapper.readValue(Path.of("seed-data", "specifications.json").toFile(), SeedSpecification[].class));
    }

    @Test
    void catalogContains50To100EntriesOfEachType() throws Exception {
        Map<SeedEntryType, Long> counts = new HashMap<>();
        for (SeedEntryType type : SeedEntryType.values()) {
            counts.put(type, catalog().stream().filter(entry -> entry.type() == type).count());
        }
        for (SeedEntryType type : SeedEntryType.values()) {
            assertThat(counts.get(type)).as("type %s", type).isBetween(50L, 100L);
        }
    }

    @Test
    void catalogNumbersMatchPatterns() throws Exception {
        Map<SeedEntryType, Pattern> patterns = Map.of(
                SeedEntryType.PART, Pattern.compile("Д-\\d{4}"),
                SeedEntryType.ASSEMBLY, Pattern.compile("СБ-\\d{4}"),
                SeedEntryType.BUY, Pattern.compile("ПОК-\\d{4}"),
                SeedEntryType.STANDART, Pattern.compile("СТ-\\d{4}"),
                SeedEntryType.VZK, Pattern.compile("ВЗК-\\d{4}"));
        for (SeedEntry entry : catalog()) {
            assertThat(patterns.get(entry.type()).matcher(entry.number()).matches())
                    .as("number %s of type %s", entry.number(), entry.type())
                    .isTrue();
        }
    }

    @Test
    void specificationsReferenceOnlyExistingElements() throws Exception {
        var index = new java.util.HashSet<String>();
        for (SeedEntry entry : catalog()) {
            index.add(entry.type().name() + ":" + entry.number());
        }
        for (SeedSpecification spec : specifications()) {
            assertThat(index).contains(SeedEntryType.ASSEMBLY.name() + ":" + spec.assemblyNumber());
            for (SeedSpecificationRow row : spec.rows()) {
                assertThat(row.quantity()).isPositive();
                assertThat(index).contains(row.type().name() + ":" + row.number());
            }
        }
    }

    @Test
    void specificationsHaveNoSelfReferences() throws Exception {
        for (SeedSpecification spec : specifications()) {
            for (SeedSpecificationRow row : spec.rows()) {
                boolean selfReference = row.type() == SeedEntryType.ASSEMBLY && row.number().equals(spec.assemblyNumber());
                assertThat(selfReference).as("self reference in %s", spec.assemblyNumber()).isFalse();
            }
        }
    }

    @Test
    void hierarchyHasDepthBetweenThreeAndFour() throws Exception {
        Map<String, List<SeedSpecificationRow>> byAssembly = new HashMap<>();
        for (SeedSpecification spec : specifications()) {
            byAssembly.put(spec.assemblyNumber(), spec.rows());
        }
        int maxDepth = byAssembly.keySet().stream()
                .mapToInt(number -> depth(number, byAssembly, new java.util.HashSet<>()))
                .max()
                .orElse(0);
        assertThat(maxDepth).isBetween(3, 4);
    }

    private static int depth(String assembly, Map<String, List<SeedSpecificationRow>> byAssembly, java.util.Set<String> visited) {
        if (!visited.add(assembly)) {
            return 1;
        }
        List<SeedSpecificationRow> rows = byAssembly.getOrDefault(assembly, List.of());
        int childDepth = 0;
        for (SeedSpecificationRow row : rows) {
            if (row.type() == SeedEntryType.ASSEMBLY) {
                childDepth = Math.max(childDepth, depth(row.number(), byAssembly, visited));
            }
        }
        return 1 + childDepth;
    }

    @Test
    void generatorIsDeterministic() throws Exception {
        assumeTrue(python3Available());
        Path first = tempDir.resolve("first");
        Path second = tempDir.resolve("second");
        runGenerator(first);
        runGenerator(second);
        for (String file : List.of("catalog.json", "specifications.json", "patterns.json")) {
            assertThat(Files.readAllBytes(first.resolve(file)))
                    .as("byte-identical %s", file)
                    .isEqualTo(Files.readAllBytes(second.resolve(file)));
        }
    }

    private static void runGenerator(Path out) throws Exception {
        Process process = new ProcessBuilder("python3", "scripts/generate-seed-data.py", "--out", out.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        assertThat(exit).as("generator output: %s", output).isZero();
    }

    private static boolean python3Available() {
        try {
            Process process = new ProcessBuilder("python3", "--version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
