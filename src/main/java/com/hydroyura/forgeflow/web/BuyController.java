package com.hydroyura.forgeflow.web;

import com.hydroyura.forgeflow.catalog.api.BuyFilter;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.api.CreateBuyRequest;
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
 * Тонкий REST-адаптер над {@link BuyService}.
 */
@RestController
@RequestMapping("/api/v1/buys")
public class BuyController {

    private final BuyService buyService;

    public BuyController(BuyService buyService) {
        this.buyService = buyService;
    }

    @GetMapping("/{id}")
    public BuyView get(@PathVariable UUID id) {
        return buyService.get(BuyId.of(id));
    }

    @GetMapping
    public List<BuyView> list() {
        return buyService.list(new BuyFilter());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuyView create(@RequestBody CreateBuyRequest request) {
        return buyService.create(request);
    }

    @PutMapping("/{id}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@PathVariable UUID id, @RequestBody RenameRequest request) {
        buyService.rename(BuyId.of(id), request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        buyService.delete(BuyId.of(id));
    }
}
