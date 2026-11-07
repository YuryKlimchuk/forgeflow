package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.MaterialService;
import com.hydroyura.forgeflow.directories.api.SortamentService;
import com.hydroyura.forgeflow.web.MaterialController;
import com.hydroyura.forgeflow.web.SortamentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Проверка, что composition root собирает справочники directories и
 * контроллеры материалов/сортаментов зарегистрированы однократно
 * (component scan, без дублирования через @Bean).
 */
class DirectoriesContextTest extends AbstractPostgresIntegrationTest {

    @Autowired
    MaterialService materialService;

    @Autowired
    SortamentService sortamentService;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void contextLoadsWithDirectoriesServices() {
        assertThat(materialService).isNotNull();
        assertThat(sortamentService).isNotNull();
    }

    @Test
    void directoriesControllersRegisteredExactlyOnce() {
        assertThat(applicationContext.getBeansOfType(MaterialController.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(SortamentController.class)).hasSize(1);
    }
}
