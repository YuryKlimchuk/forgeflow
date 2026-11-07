import { useCallback, useEffect, useState } from "react";
import {
  DIRECTORY_TYPES,
  directoriesApi,
  humanizeDirectoriesError,
  standardTypeLabel,
  type CatalogTypeConfig,
  type CreateDirectoryRequest,
  type DirectoryEntry,
} from "./api";
import DirectoryCreateForm from "./DirectoryCreateForm";
import DirectoryDetail from "./DirectoryDetail";

export default function DirectoriesSection() {
  const [activeKey, setActiveKey] = useState(DIRECTORY_TYPES[0].key);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [entries, setEntries] = useState<DirectoryEntry[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  const activeType =
    DIRECTORY_TYPES.find((type) => type.key === activeKey) ?? DIRECTORY_TYPES[0];

  const load = useCallback(async (type: CatalogTypeConfig) => {
    setStatus("loading");
    setError(null);
    try {
      setEntries(await directoriesApi.list(type.endpoint));
      setStatus("ready");
    } catch (e) {
      setError(humanizeDirectoriesError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load(activeType);
  }, [load, activeType]);

  const handleCreate = async (request: CreateDirectoryRequest): Promise<boolean> => {
    setError(null);
    try {
      await directoriesApi.create(activeType.endpoint, request);
      await load(activeType);
      return true;
    } catch (e) {
      setError(humanizeDirectoriesError(e));
      return false;
    }
  };

  if (selectedId) {
    return (
      <DirectoryDetail
        type={activeType}
        id={selectedId}
        onBack={() => setSelectedId(null)}
      />
    );
  }

  return (
    <section>
      <nav className="tabs" aria-label="Разделы справочников">
        {DIRECTORY_TYPES.map((type) => (
          <button
            key={type.key}
            className={type.key === activeKey ? "tab tab-active" : "tab"}
            onClick={() => {
              setActiveKey(type.key);
              setSelectedId(null);
            }}
          >
            {type.label}
          </button>
        ))}
      </nav>
      {error && <div className="banner banner-error">{error}</div>}
      <DirectoryCreateForm onCreate={handleCreate} />
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "ready" && entries.length === 0 && (
        <p className="muted">Пока нет записей. Создайте первую.</p>
      )}
      {status === "ready" && entries.length > 0 && (
        <table className="catalog-table">
          <thead>
            <tr>
              <th>Номер</th>
              <th>Наименование</th>
              <th>Тип стандарта</th>
              <th>Обозначение стандарта</th>
              <th>Единица измерения</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr
                key={entry.id.value}
                className="clickable-row"
                onClick={() => setSelectedId(entry.id.value)}
              >
                <td>{entry.number}</td>
                <td>{entry.name}</td>
                <td>{standardTypeLabel(entry.standardType)}</td>
                <td>{entry.standard}</td>
                <td>{entry.unit}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {status === "error" && !error && <p className="muted">Не удалось загрузить список.</p>}
    </section>
  );
}
