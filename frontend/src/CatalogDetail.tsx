import { useCallback, useEffect, useState } from "react";
import { catalogApi, humanizeError, type CatalogEntry, type CatalogTypeConfig } from "./api";

interface Props {
  type: CatalogTypeConfig;
  id: string;
  onBack: () => void;
  onOpenSpec?: (assemblyId: string) => void;
}

type Status = "loading" | "ready" | "error";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function CatalogDetail({ type, id, onBack, onOpenSpec }: Props) {
  const [entry, setEntry] = useState<CatalogEntry | null>(null);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setEntry(await catalogApi.get(type.endpoint, id));
      setStatus("ready");
    } catch (e) {
      setError(humanizeError(e));
      setStatus("error");
    }
  }, [type.endpoint, id]);

  useEffect(() => {
    void load();
  }, [load]);

  const startEdit = () => {
    setDraftName(entry?.name ?? "");
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!entry || !draftName.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await catalogApi.rename(type.endpoint, entry.id.value, draftName.trim());
      setEditing(false);
      await load();
    } catch (e) {
      setError(humanizeError(e));
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
      await catalogApi.remove(type.endpoint, entry.id.value);
      onBack();
    } catch (e) {
      setError(humanizeError(e));
      setBusy(false);
    }
  };

  return (
    <div className="catalog-detail">
      <button onClick={onBack}>Назад</button>
      {error && <div className="banner banner-error">{error}</div>}
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "error" && !error && <p className="muted">Не удалось загрузить запись.</p>}
      {status === "ready" && entry && (
        <>
          <dl>
            <dt>Номер</dt>
            <dd>{entry.number}</dd>
            <dt>Название</dt>
            <dd>
              {editing ? (
                <>
                  <input
                    value={draftName}
                    onChange={(e) => setDraftName(e.target.value)}
                    aria-label="Новое название"
                  />
                  <button onClick={() => void saveEdit()} disabled={busy}>
                    Сохранить
                  </button>
                  <button onClick={() => setEditing(false)}>Отмена</button>
                </>
              ) : (
                entry.name
              )}
            </dd>
            <dt>Создано</dt>
            <dd>{formatDate(entry.createdAt)}</dd>
            <dt>Обновлено</dt>
            <dd>{formatDate(entry.updatedAt)}</dd>
          </dl>
          {!editing && (
            <div className="actions">
              <button onClick={startEdit}>Переименовать</button>
              <button onClick={() => void handleDelete()} disabled={busy}>
                Удалить
              </button>
              {type.key === "assemblies" && onOpenSpec && (
                <button onClick={() => onOpenSpec(entry.id.value)}>Состав</button>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
