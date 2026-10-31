import { useState } from "react";
import type { CatalogEntry } from "./api";

interface Props {
  entries: CatalogEntry[];
  onRename: (id: string, name: string) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function CatalogTable({ entries, onRename, onDelete }: Props) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draftName, setDraftName] = useState("");

  if (entries.length === 0) {
    return <p className="muted">Пока нет записей. Создайте первую.</p>;
  }

  const startEdit = (entry: CatalogEntry) => {
    setEditingId(entry.id.value);
    setDraftName(entry.name);
  };

  const saveEdit = async (id: string) => {
    await onRename(id, draftName);
    setEditingId(null);
  };

  return (
    <table className="catalog-table">
      <thead>
        <tr>
          <th>Номер</th>
          <th>Название</th>
          <th>Создано</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {entries.map((entry) => (
          <tr key={entry.id.value}>
            <td>{entry.number}</td>
            <td>
              {editingId === entry.id.value ? (
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  aria-label="Новое название"
                />
              ) : (
                entry.name
              )}
            </td>
            <td>{formatDate(entry.createdAt)}</td>
            <td className="actions">
              {editingId === entry.id.value ? (
                <>
                  <button onClick={() => void saveEdit(entry.id.value)}>Сохранить</button>
                  <button onClick={() => setEditingId(null)}>Отмена</button>
                </>
              ) : (
                <>
                  <button onClick={() => startEdit(entry)}>Переименовать</button>
                  <button
                    onClick={() => {
                      if (window.confirm(`Удалить запись «${entry.number}»?`)) {
                        void onDelete(entry.id.value);
                      }
                    }}
                  >
                    Удалить
                  </button>
                </>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
