package com.hydroyura.forgeflow.organizations.application;

import java.time.Instant;

/**
 * Порт получения текущего времени.
 */
public interface Clock {

    Instant now();
}
