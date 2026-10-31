package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий REST-адаптер над {@link OrganizationService} для настроек организации.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PutMapping("/{id}/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSettings(@PathVariable UUID id, @RequestBody OrganizationSettings settings) {
        organizationService.updateSettings(OrganizationId.of(id), settings);
    }
}
