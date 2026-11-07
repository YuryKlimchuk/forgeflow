import { useCallback, useEffect, useState } from "react";
import {
  catalogApi,
  humanizeTechnologyError,
  technologyApi,
  type CatalogEntry,
  type CreateTechnologyProcessRequest,
  type TechnologyProcessView,
} from "./api";
import TechnologyProcessCreateForm from "./TechnologyProcessCreateForm";
import TechnologyProcessDetail from "./TechnologyProcessDetail";

function formatPart(part: CatalogEntry | undefined): string {
  return part ? `${part.number} — ${part.name}` : "Элемент не найден";
}

export default function TechnologyProcessSection() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [entries, setEntries] = useState<TechnologyProcessView[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);
  const [parts, setParts] = useState<CatalogEntry[]>([]);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setEntries(await technologyApi.list());
      setStatus("ready");
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    let cancelled = false;
    catalogApi
      .list("/api/v1/parts")
      .then((list) => {
        if (!cancelled) setParts(list);
      })
      .catch((e) => {
        if (!cancelled) setError(humanizeTechnologyError(e));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleCreate = async (request: CreateTechnologyProcessRequest): Promise<boolean> => {
    setError(null);
    try {
      await technologyApi.create(request);
      await load();
      return true;
    } catch (e) {
      setError(humanizeTechnologyError(e));
      return false;
    }
  };

  if (selectedId) {
    return <TechnologyProcessDetail id={selectedId} onBack={() => setSelectedId(null)} />;
  }

  return (
    <section>
      <h2>Техпроцессы</h2>
      {error && <div className="banner banner-error">{error}</div>}
      <TechnologyProcessCreateForm onCreate={handleCreate} />
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
              <th>Деталь</th>
              <th>По умолчанию</th>
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
                <td>{formatPart(parts.find((part) => part.id.value === entry.partId.value))}</td>
                <td>{entry.defaultProcess ? "Да" : "Нет"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {status === "error" && !error && <p className="muted">Не удалось загрузить список.</p>}
    </section>
  );
}
