package com.hydroyura.forgeflow;

/**
 * Конфигурация seed-загрузчика каталога.
 *
 * @param enabled  включать ли загрузку при старте
 * @param dataFile путь к локальному JSON (относительно рабочей директории)
 */
public record SeedProperties(boolean enabled, String dataFile) {

    public static SeedProperties defaults() {
        return new SeedProperties(true, "seed-data/catalog.json");
    }
}
