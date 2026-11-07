import { useCallback, useEffect, useState } from "react";
import {
  catalogApi,
  equipmentApi,
  humanizeTechnologyError,
  technologyApi,
  type CatalogEntry,
  type EquipmentEntry,
  type OperationView,
  type TechnologyProcessView,
} from "./api";
import OperationForm from "./OperationForm";

interface Props {
  id: string;
  onBack: () => void;
}

type Status = "loading" | "ready" | "error";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

function formatPart(part: CatalogEntry | undefined): string {
  return part ? `${part.number} — ${part.name}` : "Элемент не найден";
}

export default function TechnologyProcessDetail({ id, onBack }: Props) {
  const [process, setProcess] = useState<TechnologyProcessView | null>(null);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);

  const [parts, setParts] = useState<CatalogEntry[]>([]);
  const [equipment, setEquipment] = useState<EquipmentEntry[]>([]);

  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState("");
  const [busy, setBusy] = useState(false);
  const [defaultBusy, setDefaultBusy] = useState(false);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setProcess(await technologyApi.get(id));
      setStatus("ready");
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setStatus("error");
    }
  }, [id]);

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
      .catch(() => {
        // остаёмся с пустым списком деталей
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    equipmentApi
      .list()
      .then((list) => {
        if (!cancelled) setEquipment(list);
      })
      .catch(() => {
        // остаёмся с пустым списком оборудования
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const resolvePart = (partId: string): CatalogEntry | undefined =>
    parts.find((entry) => entry.id.value === partId);

  const resolveEquipment = (equipmentId: string | null): EquipmentEntry | undefined =>
    equipmentId ? equipment.find((entry) => entry.id.value === equipmentId) : undefined;

  const startEdit = () => {
    if (!process) return;
    setDraftName(process.name);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!process || !draftName.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await technologyApi.rename(id, draftName.trim());
      setEditing(false);
      await load();
    } catch (e) {
      setError(humanizeTechnologyError(e));
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!process) return;
    if (!window.confirm(`Удалить техпроцесс «${process.number}»?`)) return;
    setBusy(true);
    setError(null);
    try {
      await technologyApi.remove(id);
      onBack();
    } catch (e) {
      setError(humanizeTechnologyError(e));
      setBusy(false);
    }
  };

  const handleToggleDefault = async (next: boolean) => {
    setError(null);
    setDefaultBusy(true);
    let toggleError: string | null = null;
    try {
      if (next) {
        await technologyApi.markDefault(id);
      } else {
        await technologyApi.unmarkDefault(id);
      }
    } catch (e) {
      toggleError = humanizeTechnologyError(e);
    } finally {
      setDefaultBusy(false);
    }
    await load();
    if (toggleError) {
      setError(toggleError);
    }
  };

  const handleRemoveOperation = async (operation: OperationView) => {
    setError(null);
    try {
      await technologyApi.removeOperation(id, operation.number);
      await load();
    } catch (e) {
      setError(humanizeTechnologyError(e));
    }
  };

  return (
    <section>
      <button onClick={onBack}>Назад</button>
      {error && <div className="banner banner-error">{error}</div>}
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "error" && !error && <p className="muted">Не удалось загрузить техпроцесс.</p>}
      {status === "ready" && process && (
        <>
          <dl>
            <dt>Номер</dt>
            <dd>{process.number}</dd>
            <dt>Наименование</dt>
            <dd>
              {editing ? (
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  aria-label="Новое наименование"
                />
              ) : (
                process.name
              )}
            </dd>
            <dt>Деталь</dt>
            <dd>{formatPart(resolvePart(process.partId.value))}</dd>
            <dt>По умолчанию</dt>
            <dd>
              <input
                type="checkbox"
                aria-label="По умолчанию"
                checked={process.defaultProcess}
                disabled={defaultBusy}
                onChange={(e) => void handleToggleDefault(e.target.checked)}
              />
            </dd>
            <dt>Создано</dt>
            <dd>{formatDate(process.createdAt)}</dd>
            <dt>Обновлено</dt>
            <dd>{formatDate(process.updatedAt)}</dd>
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
              <button onClick={startEdit}>Переименовать</button>
              <button onClick={() => void handleDelete()} disabled={busy}>
                Удалить процесс
              </button>
            </div>
          )}

          <h3>Операции</h3>
          {process.operations.length === 0 ? (
            <p className="muted">Операции не заданы</p>
          ) : (
            <table className="catalog-table">
              <thead>
                <tr>
                  <th>Номер</th>
                  <th>Наименование</th>
                  <th>Оборудование</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {process.operations.map((operation) => (
                  <tr key={operation.number}>
                    <td>{operation.number}</td>
                    <td>{operation.name}</td>
                    <td>
                      {operation.equipmentId === null
                        ? "без оборудования"
                        : (resolveEquipment(operation.equipmentId.value)?.name ?? "Элемент не найден")}
                    </td>
                    <td className="actions">
                      <button onClick={() => void handleRemoveOperation(operation)}>
                        Удалить операцию
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <OperationForm processId={id} onAdded={() => void load()} />
        </>
      )}
    </section>
  );
}
