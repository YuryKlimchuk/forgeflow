package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.directories.api.CreateSortamentRequest;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.SortamentService;
import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.api.UpdateSortamentRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий REST-адаптер над {@link SortamentService}.
 */
@RestController
@RequestMapping("/api/v1/sortaments")
public class SortamentController {

    private final SortamentService sortamentService;

    public SortamentController(SortamentService sortamentService) {
        this.sortamentService = sortamentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SortamentView create(@RequestBody CreateSortamentRequest request) {
        return sortamentService.create(request);
    }

    @GetMapping
    public List<SortamentView> list() {
        return sortamentService.list();
    }

    @GetMapping("/{id}")
    public SortamentView get(@PathVariable UUID id) {
        return sortamentService.get(SortamentId.of(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id, @RequestBody UpdateSortamentRequest request) {
        sortamentService.update(SortamentId.of(id), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        sortamentService.delete(SortamentId.of(id));
    }
}
