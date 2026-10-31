package com.hydroyura.forgeflow.planning.api;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import java.util.List;

/**
 * Публичный контракт производственных планов.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface PlanService {

    PlanView create(CreatePlanRequest request);

    PlanView get(PlanId id);

    List<PlanView> list();

    PlanItemView addItem(PlanId id, AddPlanItemRequest request);

    void changeQuantity(PlanId id, AssemblyId assemblyId, ChangePlanItemQuantityRequest request);

    void removeItem(PlanId id, AssemblyId assemblyId);

    /**
     * Возвращает сводную потребность плана: листовые элементы, просуммированные
     * по всем сборкам плана, отсортированные по номеру.
     */
    List<FlatBomItem> flatten(PlanId id);
}
