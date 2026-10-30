package com.forgeflow.organizations.application;

import com.forgeflow.organizations.api.UserId;
import java.util.function.Supplier;

/**
 * Координатор конкурентных операций: проверка занятости пользователя и
 * атомарное выполнение операций, затрагивающих несколько агрегатов.
 */
public interface MembershipCoordinator {

    /**
     * Занят ли пользователь — существует как владелец или участник любой организации.
     */
    boolean isUserIdOccupied(UserId userId);

    /**
     * Выполняет действие внутри атомарной критической секции.
     */
    <T> T atomic(Supplier<T> action);
}
