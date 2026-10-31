import { useCallback, useEffect, useState } from "react";
import { api, humanizeError, type PartView } from "./api";
import PartsTable from "./PartsTable";
import CreatePartForm from "./CreatePartForm";
import "./App.css";

type Status = "loading" | "ready" | "error";

export default function App() {
  const [parts, setParts] = useState<PartView[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setParts(await api.list());
      setStatus("ready");
    } catch (e) {
      setError(humanizeError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (request: { number: string; name: string }): Promise<boolean> => {
    setError(null);
    try {
      await api.create(request);
      await load();
      return true;
    } catch (e) {
      setError(humanizeError(e));
      return false;
    }
  };

  const handleRename = async (id: string, name: string) => {
    setError(null);
    try {
      await api.rename(id, name);
      await load();
    } catch (e) {
      setError(humanizeError(e));
    }
  };

  const handleDelete = async (id: string) => {
    setError(null);
    try {
      await api.remove(id);
      await load();
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
        {error && <div className="banner banner-error">{error}</div>}
        <CreatePartForm onCreate={handleCreate} />
        {status === "loading" && <p className="muted">Загрузка…</p>}
        {status === "ready" && (
          <PartsTable parts={parts} onRename={handleRename} onDelete={handleDelete} />
        )}
        {status === "error" && !error && <p className="muted">Не удалось загрузить список.</p>}
      </main>
    </div>
  );
}
