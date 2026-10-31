package com.hydroyura.forgeflow;

import java.util.List;

/**
 * Seed-строка спецификации: ссылка на элемент каталога по типу и номеру,
 * количество и необязательный список доп. замен.
 */
public record SeedSpecificationRow(
        SeedEntryType type, String number, int quantity, List<SeedSubstitute> substitutes) {
}
