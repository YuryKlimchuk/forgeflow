package com.hydroyura.forgeflow.organizations.application;

import com.hydroyura.forgeflow.organizations.api.UserId;

/**
 * Порт получения текущего аутентифицированного пользователя (subject).
 */
public interface SubjectProvider {

    UserId currentSubject();
}
