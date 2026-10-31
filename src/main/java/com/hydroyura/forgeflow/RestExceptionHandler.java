package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.BuyNotFoundException;
import com.hydroyura.forgeflow.catalog.application.DuplicateAssemblyNumberException;
import com.hydroyura.forgeflow.catalog.application.DuplicateBuyNumberException;
import com.hydroyura.forgeflow.catalog.application.CatalogException;
import com.hydroyura.forgeflow.catalog.application.DuplicatePartNumberException;
import com.hydroyura.forgeflow.catalog.application.DuplicateStandartNumberException;
import com.hydroyura.forgeflow.catalog.application.DuplicateVzkNumberException;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import com.hydroyura.forgeflow.catalog.application.StandartNotFoundException;
import com.hydroyura.forgeflow.catalog.application.VzkNotFoundException;
import com.hydroyura.forgeflow.organizations.application.OrganizationAccessDeniedException;
import com.hydroyura.forgeflow.organizations.application.OrganizationNotFoundException;
import com.hydroyura.forgeflow.organizations.application.OrganizationsException;
import com.hydroyura.forgeflow.organizations.application.OwnerRequiredException;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanItemException;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanNameException;
import com.hydroyura.forgeflow.planning.domain.PlanItemNotFoundException;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.domain.PlansException;
import com.hydroyura.forgeflow.specifications.domain.BomCycleException;
import com.hydroyura.forgeflow.specifications.domain.DuplicateItemException;
import com.hydroyura.forgeflow.specifications.domain.DuplicateSubstituteException;
import com.hydroyura.forgeflow.specifications.domain.InvalidSubstituteException;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.SelfReferenceException;
import com.hydroyura.forgeflow.specifications.domain.SubstituteNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Минимальный маппинг исключений каталога на HTTP-статусы.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(PartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(PartNotFoundException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(DuplicatePartNumberException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicate(DuplicatePartNumberException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler({
            AssemblyNotFoundException.class,
            BuyNotFoundException.class,
            StandartNotFoundException.class,
            VzkNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleCatalogNotFound(CatalogException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler({
            DuplicateAssemblyNumberException.class,
            DuplicateBuyNumberException.class,
            DuplicateStandartNumberException.class,
            DuplicateVzkNumberException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleCatalogDuplicate(CatalogException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", "bad_request", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleOrganizationNotFound(OrganizationNotFoundException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler({OwnerRequiredException.class, OrganizationAccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleOrganizationForbidden(OrganizationsException e) {
        return Map.of("error", "forbidden", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(RowNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleRowNotFound(RowNotFoundException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(DuplicateItemException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateItem(DuplicateItemException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(SelfReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleSelfReference(SelfReferenceException e) {
        return Map.of("error", "bad_request", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(BomCycleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleBomCycle(BomCycleException e) {
        return Map.of("error", "bom_cycle", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(InvalidSubstituteException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidSubstitute(InvalidSubstituteException e) {
        return Map.of("error", "bad_request", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(DuplicateSubstituteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicateSubstitute(DuplicateSubstituteException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(SubstituteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleSubstituteNotFound(SubstituteNotFoundException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler({PlanNotFoundException.class, PlanItemNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handlePlanNotFound(PlansException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler({DuplicatePlanNameException.class, DuplicatePlanItemException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handlePlanDuplicate(PlansException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }
}
