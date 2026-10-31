package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Проверка, что composition root собирается без циклических зависимостей и
 * все пять публичных сервисов каталога доступны в контексте.
 */
@SpringBootTest(properties = "forgeflow.seed.enabled=false")
class ApplicationContextTest {

    @Autowired
    PartService partService;

    @Autowired
    AssemblyService assemblyService;

    @Autowired
    BuyService buyService;

    @Autowired
    StandartService standartService;

    @Autowired
    VzkService vzkService;

    @Test
    void contextLoadsWithAllCatalogServices() {
        assertThat(partService).isNotNull();
        assertThat(assemblyService).isNotNull();
        assertThat(buyService).isNotNull();
        assertThat(standartService).isNotNull();
        assertThat(vzkService).isNotNull();
    }
}
