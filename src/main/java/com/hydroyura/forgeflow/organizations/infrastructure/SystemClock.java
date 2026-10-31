package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.organizations.application.Clock;
import java.time.Instant;

/**
 * Системные часы.
 */
public class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
