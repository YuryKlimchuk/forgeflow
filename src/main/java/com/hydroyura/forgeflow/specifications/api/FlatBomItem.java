package com.hydroyura.forgeflow.specifications.api;

/**
 * Плоский элемент состава: листовой элемент каталога с итоговым количеством.
 */
public record FlatBomItem(String number, String name, int quantity) {
}
