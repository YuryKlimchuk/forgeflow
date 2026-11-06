package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.technology.api.AddOperationRequest;
import com.hydroyura.forgeflow.technology.api.CreateTechnologyProcessRequest;
import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.api.TechnologyService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий REST-адаптер над {@link TechnologyService}.
 */
@RestController
@RequestMapping("/api/v1/technology-processes")
public class TechnologyProcessController {

    private final TechnologyService technologyService;

    public TechnologyProcessController(TechnologyService technologyService) {
        this.technologyService = technologyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TechnologyProcessView create(@RequestBody CreateTechnologyProcessRequest request) {
        return technologyService.create(request);
    }

    @GetMapping
    public List<TechnologyProcessView> list(@RequestParam(required = false) UUID partId) {
        return technologyService.list(partId == null ? null : PartId.of(partId));
    }

    @GetMapping("/{id}")
    public TechnologyProcessView get(@PathVariable UUID id) {
        return technologyService.get(TechnologyProcessId.of(id));
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenameTechnologyProcessRequest request) {
        technologyService.rename(TechnologyProcessId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        technologyService.delete(TechnologyProcessId.of(id));
    }

    @PostMapping("/{id}/operations")
    @ResponseStatus(HttpStatus.CREATED)
    public OperationView addOperation(@PathVariable UUID id, @RequestBody AddOperationRequest request) {
        return technologyService.addOperation(TechnologyProcessId.of(id), request);
    }

    @DeleteMapping("/{id}/operations/{number}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOperation(@PathVariable UUID id, @PathVariable int number) {
        technologyService.removeOperation(TechnologyProcessId.of(id), number);
    }

    @PutMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsDefault(@PathVariable UUID id) {
        technologyService.markAsDefault(TechnologyProcessId.of(id));
    }

    @DeleteMapping("/{id}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmarkDefault(@PathVariable UUID id) {
        technologyService.unmarkDefault(TechnologyProcessId.of(id));
    }
}
