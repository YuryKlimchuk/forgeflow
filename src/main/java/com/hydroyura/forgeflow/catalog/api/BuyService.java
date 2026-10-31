package com.hydroyura.forgeflow.catalog.api;

import java.util.List;

/**
 * Публичный контракт операций над покупными изделиями каталога.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно, а не передаётся клиентом.</p>
 */
public interface BuyService {

    BuyView create(CreateBuyRequest request);

    BuyView get(BuyId id);

    List<BuyView> list(BuyFilter filter);

    void rename(BuyId id, String newName);

    void delete(BuyId id);
}
