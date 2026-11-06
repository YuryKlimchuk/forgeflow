package com.hydroyura.forgeflow.technology.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.hydroyura.forgeflow.AbstractPostgresIntegrationTest;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.security.infrastructure.ThreadLocalOrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.CreateTechnologyProcessRequest;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Проверка {@code @Transactional}-отката application-сервиса на реальном PostgreSQL:
 * ошибка на середине изменяющей операции не оставляет частичных изменений.
 */
class TechnologyApplicationServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    TechnologyApplicationService technologyService;

    @MockitoSpyBean
    TechnologyProcessRepository technologyProcessRepository;

    @Autowired
    ThreadLocalOrganizationContext organizationContext;

    @Autowired
    PartRepository partRepository;

    private OrganizationId orgId;

    @BeforeEach
    void setUp() {
        orgId = OrganizationId.generate();
        organizationContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        organizationContext.clear();
    }

    @Test
    void markAsDefault_rollsBackWhenSecondSaveFails() {
        PartId partId = PartId.generate();
        partRepository.save(Part.create(partId, "Д-9001", "Тестовая деталь", orgId, NOW, NOW));

        TechnologyProcessView first = technologyService.create(
                new CreateTechnologyProcessRequest("TP-1", "Первый", partId));
        technologyService.markAsDefault(first.id());
        TechnologyProcessView second = technologyService.create(
                new CreateTechnologyProcessRequest("TP-2", "Второй", partId));

        doAnswer(invocation -> {
            TechnologyProcess process = invocation.getArgument(0);
            if (process.getId().equals(second.id())) {
                throw new RuntimeException("forced failure on second save");
            }
            return invocation.callRealMethod();
        }).when(technologyProcessRepository).save(any(TechnologyProcess.class));

        assertThatThrownBy(() -> technologyService.markAsDefault(second.id()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("forced failure");

        assertThat(technologyService.get(first.id()).defaultProcess()).isTrue();
        assertThat(technologyService.get(second.id()).defaultProcess()).isFalse();
    }
}
