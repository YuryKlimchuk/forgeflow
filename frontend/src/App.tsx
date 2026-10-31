import { useCallback, useEffect, useState } from "react";
import {
  CATALOG_TYPES,
  catalogApi,
  humanizeError,
  type CatalogEntry,
  type CatalogTypeConfig,
} from "./api";
import CatalogTable from "./CatalogTable";
import CreateCatalogEntryForm from "./CreateCatalogEntryForm";
import SpecificationPanel from "./SpecificationPanel";
import "./App.css";

type Status = "loading" | "ready" | "error";

export default function App() {
  const [activeKey, setActiveKey] = useState(CATALOG_TYPES[0].key);
  const [entries, setEntries] = useState<CatalogEntry[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  const [showSpec, setShowSpec] = useState(false);

  const activeType =
    CATALOG_TYPES.find((type) => type.key === activeKey) ?? CATALOG_TYPES[0];

  const load = useCallback(async (type: CatalogTypeConfig) => {
    setStatus("loading");
    setError(null);
    try {
      setEntries(await catalogApi.list(type.endpoint));
      setStatus("ready");
    } catch (e) {
      setError(humanizeError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load(activeType);
  }, [load, activeType]);

  const handleCreate = async (request: { number: string; name: string }): Promise<boolean> => {
    setError(null);
    try {
      await catalogApi.create(activeType.endpoint, request);
      await load(activeType);
      return true;
    } catch (e) {
      setError(humanizeError(e));
      return false;
    }
  };

  const handleRename = async (id: string, name: string) => {
    setError(null);
    try {
      await catalogApi.rename(activeType.endpoint, id, name);
      await load(activeType);
    } catch (e) {
      setError(humanizeError(e));
    }
  };

  const handleDelete = async (id: string) => {
    setError(null);
    try {
      await catalogApi.remove(activeType.endpoint, id);
      await load(activeType);
    } catch (e) {
      setError(humanizeError(e));
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>ForgeFlow — Каталог</h1>
      </header>
      <main className="app-main">
        <nav className="tabs" aria-label="Типы каталога">
          {CATALOG_TYPES.map((type) => (
            <button
              key={type.key}
              className={type.key === activeKey ? "tab tab-active" : "tab"}
              onClick={() => setActiveKey(type.key)}
            >
              {type.label}
            </button>
          ))}
        </nav>
        {error && <div className="banner banner-error">{error}</div>}
        <CreateCatalogEntryForm onCreate={handleCreate} />
        {status === "loading" && <p className="muted">Загрузка…</p>}
        {status === "ready" && (
          <CatalogTable entries={entries} onRename={handleRename} onDelete={handleDelete} />
        )}
        {status === "error" && !error && <p className="muted">Не удалось загрузить список.</p>}

        <button onClick={() => setShowSpec((value) => !value)}>
          {showSpec ? "Скрыть спецификацию" : "Спецификация"}
        </button>
        {showSpec && <SpecificationPanel />}
      </main>
    </div>
  );
}
