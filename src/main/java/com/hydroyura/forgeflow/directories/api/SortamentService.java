package com.hydroyura.forgeflow.directories.api;

import java.util.List;

/**
 * Публичный контракт управления сортаментами.
 *
 * <p>Все операции выполняются в контексте текущей организации (tenant);
 * организация определяется серверно и не передаётся клиентом.</p>
 */
public interface SortamentService {

    /**
     * Создаёт активный сортамент с уникальным в пределах организации номером.
     */
    SortamentView create(CreateSortamentRequest request);

    /**
     * Возвращает активный сортамент текущей организации.
     *
     * @throws com.hydroyura.forgeflow.directories.domain.sortament.SortamentNotFoundException
     *         если сортамент отсутствует, удалён или принадлежит другой организации
     */
    SortamentView get(SortamentId id);

    /**
     * Возвращает активные сортаменты текущей организации, отсортированные по номеру.
     */
    List<SortamentView> list();

    /**
     * Обновляет изменяемые поля существующего активного сортамента.
     */
    void update(SortamentId id, UpdateSortamentRequest request);

    /**
     * Логически удаляет (soft-delete) активный сортамент текущей организации.
     */
    void delete(SortamentId id);
}
