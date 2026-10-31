/**
 * Управление организациями (тенантами), участниками, членством и ролями.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"shared :: shared"}
)
package com.hydroyura.forgeflow.organizations;
