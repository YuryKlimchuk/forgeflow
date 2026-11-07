package com.hydroyura.forgeflow.directories.domain.material;

import com.hydroyura.forgeflow.directories.api.MaterialId;

/**
 * Материал отсутствует, удалён или принадлежит другой организации.
 */
public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(MaterialId id) {
        super("Material not found: " + id);
    }
}
