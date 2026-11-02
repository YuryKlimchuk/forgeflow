package com.hydroyura.forgeflow.security.infrastructure;

import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.application.UserAlreadyOccupiedException;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.api.Realms;
import com.hydroyura.forgeflow.security.application.OrganizationContextResolver;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Обёртка жизненного цикла запроса: резолвит организацию, устанавливает её
 * в контекст до обработчика и гарантированно очищает в {@code finally}.
 * Для пользователей realm {@code forgeflow-users} лениво регистрирует
 * владельца без организации при первом входе.
 */
public class OrganizationContextLifecycle {

    private final AuthenticatedUserProvider userProvider;
    private final OrganizationContextResolver resolver;
    private final ThreadLocalOrganizationContext context;
    private final OrganizationService organizationService;

    public OrganizationContextLifecycle(
            AuthenticatedUserProvider userProvider,
            OrganizationContextResolver resolver,
            ThreadLocalOrganizationContext context,
            OrganizationService organizationService) {
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.organizationService = Objects.requireNonNull(organizationService, "organizationService must not be null");
    }

    public <T> T run(Supplier<T> handler) {
        userProvider.current().ifPresent(this::registerIfNeeded);
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

    private void registerIfNeeded(AuthenticatedUser user) {
        if (!Realms.FORGEFLOW_USERS.equals(user.realm())) {
            return;
        }
        try {
            organizationService.registerCurrentUser();
        } catch (UserAlreadyOccupiedException e) {
            // Пользователь уже зарегистрирован — идемпотентно пропускаем.
        }
    }
}
