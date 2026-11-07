import { useCallback, useEffect, useState } from "react";
import {
  directoriesApi,
  humanizeDirectoriesError,
  standardTypeLabel,
  type CatalogTypeConfig,
  type DirectoryEntry,
  type StandardType,
} from "./api";

const STANDARD_TYPES: StandardType[] = ["GOST", "DIN", "ISO", "OTHER"];

interface Props {
  type: CatalogTypeConfig;
  id: string;
  onBack: () => void;
}

type Status = "loading" | "ready" | "error";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function DirectoryDetail({ type, id, onBack }: Props) {
  const [entry, setEntry] = useState<DirectoryEntry | null>(null);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [draftStandardType, setDraftStandardType] = useState<StandardType>("GOST");
  const [draftStandard, setDraftStandard] = useState("");
  const [draftUnit, setDraftUnit] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setEntry(await directoriesApi.get(type.endpoint, id));
      setStatus("ready");
    } catch (e) {
      setError(humanizeDirectoriesError(e));
      setStatus("error");
    }
  }, [type.endpoint, id]);

  useEffect(() => {
    void load();
  }, [load]);

  const startEdit = () => {
    if (!entry) return;
    setDraftName(entry.name);
    setDraftStandardType(entry.standardType);
    setDraftStandard(entry.standard);
    setDraftUnit(entry.unit);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (
      !entry ||
      !draftName.trim() ||
      !draftStandardType ||
      !draftStandard.trim() ||
      !draftUnit.trim()
    ) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await directoriesApi.update(type.endpoint, entry.id.value, {
        name: draftName.trim(),
        standardType: draftStandardType,
        standard: draftStandard.trim(),
        unit: draftUnit.trim(),
      });
      setEditing(false);
      await load();
    } catch (e) {
      setError(humanizeDirectoriesError(e));
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
      await directoriesApi.remove(type.endpoint, entry.id.value);
      onBack();
    } catch (e) {
      setError(humanizeDirectoriesError(e));
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
            <dt>Тип стандарта</dt>
            <dd>
              {editing ? (
                <select
                  value={draftStandardType}
                  onChange={(e) => setDraftStandardType(e.target.value as StandardType)}
                  aria-label="Новый тип стандарта"
                >
                  {STANDARD_TYPES.map((standardType) => (
                    <option key={standardType} value={standardType}>
                      {standardTypeLabel(standardType)}
                    </option>
                  ))}
                </select>
              ) : (
                standardTypeLabel(entry.standardType)
              )}
            </dd>
            <dt>Обозначение стандарта</dt>
            <dd>
              {editing ? (
                <input
                  value={draftStandard}
                  onChange={(e) => setDraftStandard(e.target.value)}
                  aria-label="Новое обозначение стандарта"
                />
              ) : (
                entry.standard
              )}
            </dd>
            <dt>Единица измерения</dt>
            <dd>
              {editing ? (
                <input
                  value={draftUnit}
                  onChange={(e) => setDraftUnit(e.target.value)}
                  aria-label="Новая единица измерения"
                />
              ) : (
                entry.unit
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
