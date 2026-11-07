import { useCallback, useEffect, useState } from "react";
import {
  equipmentApi,
  humanizeTechnologyError,
  type EquipmentEntry,
} from "./api";

interface Props {
  id: string;
  onBack: () => void;
}

type Status = "loading" | "ready" | "error";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function EquipmentDetail({ id, onBack }: Props) {
  const [entry, setEntry] = useState<EquipmentEntry | null>(null);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [draftType, setDraftType] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setEntry(await equipmentApi.get(id));
      setStatus("ready");
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setStatus("error");
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const startEdit = () => {
    if (!entry) return;
    setDraftName(entry.name);
    setDraftType(entry.type);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!entry || !draftName.trim() || !draftType.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await equipmentApi.update(entry.id.value, {
        name: draftName.trim(),
        type: draftType.trim(),
      });
      setEditing(false);
      await load();
    } catch (e) {
      setError(humanizeTechnologyError(e));
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!entry) return;
    if (!window.confirm(`Удалить запись «${entry.number}»?`)) return;
    setBusy(true);
    setError(null);
    try {
      await equipmentApi.remove(entry.id.value);
      onBack();
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setBusy(false);
    }
  };

  return (
    <section>
      <button onClick={onBack}>Назад</button>
      {error && <div className="banner banner-error">{error}</div>}
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "error" && !error && <p className="muted">Не удалось загрузить запись.</p>}
      {status === "ready" && entry && (
        <>
          <dl>
            <dt>Номер</dt>
            <dd>{entry.number}</dd>
            <dt>Наименование</dt>
            <dd>
              {editing ? (
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  aria-label="Новое наименование"
                />
              ) : (
                entry.name
              )}
            </dd>
            <dt>Тип</dt>
            <dd>
              {editing ? (
                <input
                  value={draftType}
                  onChange={(e) => setDraftType(e.target.value)}
                  aria-label="Новый тип"
                />
              ) : (
                entry.type
              )}
            </dd>
            <dt>Создано</dt>
            <dd>{formatDate(entry.createdAt)}</dd>
            <dt>Обновлено</dt>
            <dd>{formatDate(entry.updatedAt)}</dd>
          </dl>
          {editing ? (
            <div className="actions">
              <button onClick={() => void saveEdit()} disabled={busy}>
                Сохранить
              </button>
              <button onClick={() => setEditing(false)}>Отмена</button>
            </div>
          ) : (
            <div className="actions">
              <button onClick={startEdit}>Редактировать</button>
              <button onClick={() => void handleDelete()} disabled={busy}>
                Удалить
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
