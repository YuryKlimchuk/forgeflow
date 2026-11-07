package com.hydroyura.forgeflow.directories.domain.sortament;

import com.hydroyura.forgeflow.directories.api.SortamentId;

/**
 * Сортамент отсутствует, удалён или принадлежит другой организации.
 */
public class SortamentNotFoundException extends RuntimeException {

    public SortamentNotFoundException(SortamentId id) {
        super("Sortament not found: " + id);
    }
}
