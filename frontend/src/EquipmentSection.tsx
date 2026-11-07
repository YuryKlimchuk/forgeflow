import { useCallback, useEffect, useState } from "react";
import {
  equipmentApi,
  humanizeTechnologyError,
  type CreateEquipmentRequest,
  type EquipmentEntry,
} from "./api";
import EquipmentCreateForm from "./EquipmentCreateForm";
import EquipmentDetail from "./EquipmentDetail";

export default function EquipmentSection() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [entries, setEntries] = useState<EquipmentEntry[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setEntries(await equipmentApi.list());
      setStatus("ready");
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (request: CreateEquipmentRequest): Promise<boolean> => {
    setError(null);
    try {
      await equipmentApi.create(request);
      await load();
      return true;
    } catch (e) {
      setError(humanizeTechnologyError(e));
      return false;
    }
  };

  if (selectedId) {
    return <EquipmentDetail id={selectedId} onBack={() => setSelectedId(null)} />;
  }

  return (
    <section>
      <h2>Оборудование</h2>
      {error && <div className="banner banner-error">{error}</div>}
      <EquipmentCreateForm onCreate={handleCreate} />
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
              <th>Тип</th>
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
                <td>{entry.type}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {status === "error" && !error && <p className="muted">Не удалось загрузить список.</p>}
    </section>
  );
}
