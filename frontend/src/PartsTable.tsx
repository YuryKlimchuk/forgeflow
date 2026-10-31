import { useState } from "react";
import type { PartView } from "./api";

interface Props {
  parts: PartView[];
  onRename: (id: string, name: string) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function PartsTable({ parts, onRename, onDelete }: Props) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draftName, setDraftName] = useState("");

  if (parts.length === 0) {
    return <p className="muted">Пока нет деталей. Создайте первую.</p>;
  }

  const startEdit = (part: PartView) => {
    setEditingId(part.id.value);
    setDraftName(part.name);
  };

  const saveEdit = async (id: string) => {
    await onRename(id, draftName);
    setEditingId(null);
  };

  return (
    <table className="parts-table">
      <thead>
        <tr>
          <th>Номер</th>
          <th>Название</th>
          <th>Создано</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {parts.map((part) => (
          <tr key={part.id.value}>
            <td>{part.number}</td>
            <td>
              {editingId === part.id.value ? (
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  aria-label="Новое название"
                />
              ) : (
                part.name
              )}
            </td>
            <td>{formatDate(part.createdAt)}</td>
            <td className="actions">
              {editingId === part.id.value ? (
                <>
                  <button onClick={() => void saveEdit(part.id.value)}>Сохранить</button>
                  <button onClick={() => setEditingId(null)}>Отмена</button>
                </>
              ) : (
                <>
                  <button onClick={() => startEdit(part)}>Переименовать</button>
                  <button
                    onClick={() => {
                      if (window.confirm(`Удалить деталь «${part.number}»?`)) {
                        void onDelete(part.id.value);
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
