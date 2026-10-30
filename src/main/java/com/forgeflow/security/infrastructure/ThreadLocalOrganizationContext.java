package com.forgeflow.security.infrastructure;

import com.forgeflow.shared.api.OrganizationContext;
import com.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.forgeflow.shared.api.OrganizationId;
import java.util.Objects;
import java.util.Optional;

/**
 * ThreadLocal-реализация {@link OrganizationContext}.
 *
 * <p>Операции установки и очистки доступны только инфраструктуре; бизнес-модули
 * видят лишь read-only контракт {@link OrganizationContext}.</p>
 */
public class ThreadLocalOrganizationContext implements OrganizationContext {

    private final ThreadLocal<OrganizationId> holder = new ThreadLocal<>();

    public void set(OrganizationId organizationId) {
        holder.set(Objects.requireNonNull(organizationId, "organizationId must not be null"));
    }

    public void clear() {
        holder.remove();
    }

    @Override
    public Optional<OrganizationId> currentOrganizationId() {
        return Optional.ofNullable(holder.get());
    }

    @Override
    public OrganizationId requiredOrganizationId() {
        OrganizationId organizationId = holder.get();
        if (organizationId == null) {
            throw new OrganizationContextNotAvailableException("No current organization context");
        }
        return organizationId;
    }
}
