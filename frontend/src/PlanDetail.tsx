import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  catalogApi,
  humanizePlanError,
  planningApi,
  type CatalogEntry,
  type FlatBomItem,
  type PlanView,
} from "./api";
import { formatPeriod } from "./PlanList";

interface Props {
  id: string;
  onBack: () => void;
}

export default function PlanDetail({ id, onBack }: Props) {
  const [plan, setPlan] = useState<PlanView | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  const [assemblies, setAssemblies] = useState<CatalogEntry[]>([]);
  const [selectedAssembly, setSelectedAssembly] = useState<CatalogEntry | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [quantity, setQuantity] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [editingAssemblyId, setEditingAssemblyId] = useState<string | null>(null);
  const [draftQuantity, setDraftQuantity] = useState("");

  const [flatItems, setFlatItems] = useState<FlatBomItem[] | null>(null);
  const [flatStatus, setFlatStatus] = useState<"idle" | "loading" | "ready" | "error">("idle");
  const [flatError, setFlatError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setPlan(await planningApi.get(id));
      setStatus("ready");
    } catch (e) {
      setError(humanizePlanError(e));
      setStatus("error");
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    let cancelled = false;
    catalogApi
      .list("/api/v1/assemblies")
      .then((list) => {
        if (!cancelled) setAssemblies(list);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const resolve = (assemblyId: string): CatalogEntry | undefined => {
    return assemblies.find((entry) => entry.id.value === assemblyId);
  };

  const query = searchQuery.trim().toLowerCase();
  const filteredAssemblies = assemblies.filter(
    (entry) =>
      !query ||
      entry.number.toLowerCase().includes(query) ||
      entry.name.toLowerCase().includes(query),
  );

  const handleAdd = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedAssembly || !quantity.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await planningApi.addItem(id, selectedAssembly.id.value, Number(quantity));
      setSelectedAssembly(null);
      setQuantity("");
      setSearchQuery("");
      await load();
    } catch (e) {
      setError(humanizePlanError(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeQuantity = async (assemblyId: string) => {
    if (!draftQuantity.trim()) return;
    setError(null);
    try {
      await planningApi.changeQuantity(id, assemblyId, Number(draftQuantity));
      setEditingAssemblyId(null);
      setDraftQuantity("");
      await load();
    } catch (e) {
      setError(humanizePlanError(e));
    }
  };

  const handleRemove = async (assemblyId: string) => {
    setError(null);
    try {
      await planningApi.removeItem(id, assemblyId);
      await load();
    } catch (e) {
      setError(humanizePlanError(e));
    }
  };

  const handleFlat = async () => {
    setFlatStatus("loading");
    setFlatError(null);
    setFlatItems(null);
    try {
      setFlatItems(await planningApi.flat(id));
      setFlatStatus("ready");
    } catch (e) {
      setFlatError(humanizePlanError(e));
      setFlatStatus("error");
    }
  };

  return (
    <section>
      <button onClick={onBack}>Назад</button>
      <h2>{plan ? plan.name : "План"}</h2>
      {plan && <p className="muted">Период: {formatPeriod(plan.year, plan.month)}</p>}
      {error && <div className="banner banner-error">{error}</div>}
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "ready" && plan && (
        <>
          {plan.items.length === 0 ? (
            <p className="muted">План пуст</p>
          ) : (
            <table className="catalog-table">
              <thead>
                <tr>
                  <th>Номер</th>
                  <th>Название</th>
                  <th>Количество</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {plan.items.map((item) => {
                  const entry = resolve(item.assemblyId.value);
                  return (
                    <tr key={item.assemblyId.value}>
                      <td>{entry?.number ?? "Элемент не найден"}</td>
                      <td>{entry?.name ?? ""}</td>
                      <td>
                        {editingAssemblyId === item.assemblyId.value ? (
                          <input
                            value={draftQuantity}
                            onChange={(e) => setDraftQuantity(e.target.value)}
                            aria-label="Новое количество"
                          />
                        ) : (
                          item.quantity
                        )}
                      </td>
                      <td className="actions">
                        {editingAssemblyId === item.assemblyId.value ? (
                          <button onClick={() => void handleChangeQuantity(item.assemblyId.value)}>
                            Сохранить количество
                          </button>
                        ) : (
                          <button
                            onClick={() => {
                              setEditingAssemblyId(item.assemblyId.value);
                              setDraftQuantity(String(item.quantity));
                            }}
                          >
                            Изменить
                          </button>
                        )}
                        <button onClick={() => void handleRemove(item.assemblyId.value)}>
                          Удалить
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          <form className="create-form" onSubmit={handleAdd}>
            {selectedAssembly ? (
              <span className="selected-element">
                {selectedAssembly.number} — {selectedAssembly.name}
                <button
                  type="button"
                  onClick={() => {
                    setSelectedAssembly(null);
                    setSearchQuery("");
                  }}
                >
                  Сменить
                </button>
              </span>
            ) : (
              <input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Поиск сборки"
                aria-label="Поиск сборки"
              />
            )}
            <input
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="Количество"
              aria-label="Количество"
            />
            <button type="submit" disabled={submitting || !selectedAssembly}>
              {submitting ? "Создание…" : "Добавить сборку"}
            </button>
          </form>
          {!selectedAssembly && (
            <ul className="element-options" aria-label="Варианты сборки">
              {filteredAssemblies.map((entry) => (
                <li key={entry.id.value}>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedAssembly(entry);
                      setSearchQuery("");
                    }}
                  >
                    {entry.number} — {entry.name}
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="flat-bom">
            <h3>Плоская структура</h3>
            <button onClick={() => void handleFlat()} disabled={flatStatus === "loading"}>
              {flatStatus === "loading" ? "Разворачивание…" : "Плоская структура"}
            </button>
            {flatStatus === "loading" && <p className="muted">Разворачивание…</p>}
            {flatStatus === "error" && <div className="banner banner-error">{flatError}</div>}
            {flatStatus === "ready" && flatItems && (
              <table className="catalog-table">
                <thead>
                  <tr>
                    <th>Номер</th>
                    <th>Название</th>
                    <th>Количество</th>
                  </tr>
                </thead>
                <tbody>
                  {flatItems.map((item) => (
                    <tr key={item.number}>
                      <td>{item.number}</td>
                      <td>{item.name}</td>
                      <td>{item.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </section>
  );
}
