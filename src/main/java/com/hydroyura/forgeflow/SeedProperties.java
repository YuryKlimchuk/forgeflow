package com.hydroyura.forgeflow;

/**
 * Конфигурация seed-загрузчика.
 *
 * @param enabled             включать ли загрузку при старте
 * @param dataFile            путь к JSON каталога (относительно рабочей директории)
 * @param specificationsFile  путь к JSON спецификаций (относительно рабочей директории)
 */
public record SeedProperties(boolean enabled, String dataFile, String specificationsFile) {

    public static SeedProperties defaults() {
        return new SeedProperties(true, "seed-data/catalog.json", "seed-data/specifications.json");
    }
}
