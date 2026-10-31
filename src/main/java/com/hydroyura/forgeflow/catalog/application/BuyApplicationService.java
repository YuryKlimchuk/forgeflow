package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.BuyFilter;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.CreateBuyRequest;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link BuyService}: CRUD-срез покупных изделий каталога.
 */
public class BuyApplicationService implements BuyService {

    private final BuyRepository repository;
    private final OrganizationContext organizationContext;
    private final OrganizationSettingsProvider settingsProvider;
    private final BuyMapper mapper;
    private final Clock clock;

    public BuyApplicationService(
            BuyRepository repository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            BuyMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public BuyView create(CreateBuyRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (!settingsProvider.findSettings(organizationId).matchesBuy(request.number())) {
            throw new IllegalArgumentException("Buy number does not match the organization pattern: " + request.number());
        }
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateBuyNumberException(request.number());
        }
        Instant now = clock.instant();
        Buy buy = Buy.create(BuyId.generate(), request.number(), request.name(), organizationId, now, now);
        repository.save(buy);
        return mapper.toView(buy);
    }

    @Override
    public BuyView get(BuyId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Buy buy = repository.findById(id, organizationId)
                .orElseThrow(() -> new BuyNotFoundException(id));
        return mapper.toView(buy);
    }

    @Override
    public List<BuyView> list(BuyFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(BuyId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Buy buy = repository.findById(id, organizationId)
                .orElseThrow(() -> new BuyNotFoundException(id));
        buy.rename(newName);
        repository.save(buy);
    }

    @Override
    public void delete(BuyId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Buy buy = repository.findById(id, organizationId)
                .orElseThrow(() -> new BuyNotFoundException(id));
        buy.markDeleted();
        repository.save(buy);
    }
}
