package com.hydroyura.forgeflow.organizations.api;

import org.apache.commons.lang3.StringUtils;

/**
 * Настройки организации: опциональные regex-паттерны номеров пяти типов
 * каталога. {@code null} или пустое значение поля означает отсутствие
 * ограничения для соответствующего типа.
 */
public record OrganizationSettings(
        String partNumberPattern,
        String assemblyNumberPattern,
        String buyNumberPattern,
        String standartNumberPattern,
        String vzkNumberPattern) {

    public OrganizationSettings {
        partNumberPattern = blankToNull(partNumberPattern);
        assemblyNumberPattern = blankToNull(assemblyNumberPattern);
        buyNumberPattern = blankToNull(buyNumberPattern);
        standartNumberPattern = blankToNull(standartNumberPattern);
        vzkNumberPattern = blankToNull(vzkNumberPattern);
    }

    /**
     * Настройки без ограничений — ни один паттерн не задан.
     */
    public static OrganizationSettings empty() {
        return new OrganizationSettings(null, null, null, null, null);
    }

    /**
     * Полное совпадение номера детали с паттерном; {@code true} при
     * отсутствии паттерна.
     */
    public boolean matchesPart(String number) {
        return matches(partNumberPattern, number);
    }

    public boolean matchesAssembly(String number) {
        return matches(assemblyNumberPattern, number);
    }

    public boolean matchesBuy(String number) {
        return matches(buyNumberPattern, number);
    }

    public boolean matchesStandart(String number) {
        return matches(standartNumberPattern, number);
    }

    public boolean matchesVzk(String number) {
        return matches(vzkNumberPattern, number);
    }

    private static boolean matches(String pattern, String number) {
        return pattern == null || (number != null && number.matches(pattern));
    }

    private static String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }
}
