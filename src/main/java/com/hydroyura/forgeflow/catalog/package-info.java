/**
 * Каталог изделий: сборочные единицы, детали, ВЗК.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"directories :: directories", "shared :: shared", "organizations :: organizations"}
)
package com.hydroyura.forgeflow.catalog;
