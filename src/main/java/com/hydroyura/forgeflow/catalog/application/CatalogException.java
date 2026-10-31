package com.hydroyura.forgeflow.catalog.application;

/**
 * Базовое исключение модуля каталога.
 */
public class CatalogException extends RuntimeException {

    public CatalogException(String message) {
        super(message);
    }
}
