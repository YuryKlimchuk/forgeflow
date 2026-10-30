package com.forgeflow.organizations.infrastructure;

import com.forgeflow.organizations.application.Clock;
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
