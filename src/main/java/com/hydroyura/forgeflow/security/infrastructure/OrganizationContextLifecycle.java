package com.hydroyura.forgeflow.security.infrastructure;

import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.application.OrganizationContextResolver;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Обёртка жизненного цикла запроса: резолвит организацию, устанавливает её
 * в контекст до обработчика и гарантированно очищает в {@code finally}.
 */
public class OrganizationContextLifecycle {

    private final AuthenticatedUserProvider userProvider;
    private final OrganizationContextResolver resolver;
    private final ThreadLocalOrganizationContext context;

    public OrganizationContextLifecycle(
            AuthenticatedUserProvider userProvider,
            OrganizationContextResolver resolver,
            ThreadLocalOrganizationContext context) {
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    public <T> T run(Supplier<T> handler) {
        userProvider.current()
                .flatMap(resolver::resolve)
                .ifPresent(context::set);
        try {
            return handler.get();
        } finally {
            context.clear();
        }
    }

    public void run(Runnable handler) {
        run(() -> {
            handler.run();
            return null;
        });
    }
}
