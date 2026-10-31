package com.hydroyura.forgeflow;

/**
 * Seed-строка спецификации: ссылка на элемент каталога по типу и номеру.
 */
public record SeedSpecificationRow(SeedEntryType type, String number, int quantity) {
}
