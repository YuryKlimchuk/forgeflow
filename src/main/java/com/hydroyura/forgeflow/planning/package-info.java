/**
 * Производственное планирование: планы на период и расчёт потребностей.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"catalog :: catalog", "specifications :: specifications", "shared :: shared"}
)
package com.hydroyura.forgeflow.planning;
