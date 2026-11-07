package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.web.EquipmentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Проверка, что composition root собирает справочник оборудования и контроллер
 * оборудования зарегистрирован однократно (component scan, без дублирования
 * через @Bean).
 */
class TechnologyEquipmentContextTest extends AbstractPostgresIntegrationTest {

    @Autowired
    EquipmentService equipmentService;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void contextLoadsWithEquipmentService() {
        assertThat(equipmentService).isNotNull();
    }

    @Test
    void equipmentControllerRegisteredExactlyOnce() {
        assertThat(applicationContext.getBeansOfType(EquipmentController.class)).hasSize(1);
    }
}
