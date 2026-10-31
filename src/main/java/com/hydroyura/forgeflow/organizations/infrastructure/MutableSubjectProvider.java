package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.application.SubjectProvider;

/**
 * Простейший держатель текущего subject. На этом этапе не интегрируется с
 * внешней системой идентификации.
 */
public class MutableSubjectProvider implements SubjectProvider {

    private volatile UserId subject;

    public void set(UserId subject) {
        this.subject = subject;
    }

    @Override
    public UserId currentSubject() {
        if (subject == null) {
            throw new IllegalStateException("No current subject set");
        }
        return subject;
    }
}
