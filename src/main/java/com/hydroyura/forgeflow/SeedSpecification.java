package com.hydroyura.forgeflow;

import java.util.List;

/**
 * Seed-запись спецификации: номер головы-сборки и список строк.
 */
public record SeedSpecification(String assemblyNumber, List<SeedSpecificationRow> rows) {
}
