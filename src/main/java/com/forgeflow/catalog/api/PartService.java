package com.forgeflow.catalog.api;

import java.util.List;

/**
 * Публичный контракт операций над деталями каталога.
 *
 * <p>Это входной порт модуля {@code catalog}: UI (Thymeleaf сейчас, REST позже)
 * зависит только от этого интерфейса и DTO-типов, не зная о repository и
 * доменных объектах.</p>
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно, а не передаётся клиентом.</p>
 */
public interface PartService {

    /**
     * Создаёт активную деталь с уникальным в пределах организации номером.
     */
    PartView create(CreatePartRequest request);

    /**
     * Возвращает активную деталь текущей организации.
     *
     * @throws com.forgeflow.catalog.application.PartNotFoundException если деталь
     *         отсутствует, удалена или принадлежит другой организации
     */
    PartView get(PartId id);

    /**
     * Возвращает активные детали текущей организации, отсортированные по номеру.
     */
    List<PartView> list(PartFilter filter);

    /**
     * Изменяет имя существующей активной детали.
     */
    void rename(PartId id, String newName);

    /**
     * Логически удаляет (soft-delete) активную деталь текущей организации.
     */
    void delete(PartId id);
}
