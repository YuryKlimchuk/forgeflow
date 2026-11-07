package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Driven port источника данных для расчёта потребностей: прямые строки
 * спецификаций и дескрипторы элементов каталога. Домен не зависит от
 * application-сервисов соседних модулей.
 */
public interface RequirementSource {

    record SourceRow(CatalogItemReference item, int quantity) {

        public SourceRow {
            Objects.requireNonNull(item, "item must not be null");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }

    record ItemDescriptor(String number, String name) {

        public ItemDescriptor {
            Objects.requireNonNull(number, "number must not be null");
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /**
     * Возвращает прямые строки существующей сборки.
     */
    List<SourceRow> rows(AssemblyId assemblyId);

    /**
     * Возвращает дескриптор элемента каталога либо {@link Optional#empty()},
     * если элемент отсутствует, удалён или принадлежит другой организации.
     */
    Optional<ItemDescriptor> descriptor(CatalogItemReference item);
}
