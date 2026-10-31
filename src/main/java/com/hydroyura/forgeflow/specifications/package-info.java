/**
 * Электронные спецификации изделий по ЕСКД.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"catalog :: catalog", "directories :: directories", "shared :: shared"}
)
package com.hydroyura.forgeflow.specifications;
