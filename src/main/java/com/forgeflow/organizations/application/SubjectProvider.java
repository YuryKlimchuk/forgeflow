package com.forgeflow.organizations.application;

import com.forgeflow.organizations.api.UserId;

/**
 * Порт получения текущего аутентифицированного пользователя (subject).
 */
public interface SubjectProvider {

    UserId currentSubject();
}
