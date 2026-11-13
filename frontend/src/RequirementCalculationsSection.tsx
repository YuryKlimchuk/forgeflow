import { useEffect, useState } from "react";
import {
  humanizeRequirementError,
  planningApi,
  requirementItemTypeLabel,
  requirementNeedTypeLabel,
  type CatalogEntry,
  type RequirementCalculationView,
} from "./api";

interface Props {
  planId: string;
  assemblies: CatalogEntry[];
}

function formatExecutedAt(executedAt: string): string {
  if (!executedAt) {
    return "";
  }
  const date = new Date(executedAt);
  if (Number.isNaN(date.getTime())) {
    return executedAt;
  }
  return date.toLocaleString("ru-RU");
}

export default function RequirementCalculationsSection({ planId, assemblies }: Props) {
  const [calculations, setCalculations] = useState<RequirementCalculationView[] | null>(null);
  const [historyStatus, setHistoryStatus] = useState<"idle" | "loading" | "ready" | "error">(
    "idle",
  );
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [selected, setSelected] = useState<RequirementCalculationView | null>(null);
  const [runStatus, setRunStatus] = useState<"idle" | "running">("idle");
  const [runError, setRunError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setHistoryStatus("loading");
    setHistoryError(null);
    setCalculations(null);
    setSelected(null);
    planningApi
      .listCalculations(planId)
      .then((list) => {
        if (cancelled) return;
        setCalculations(list);
        setHistoryStatus("ready");
      })
      .catch((e) => {
        if (cancelled) return;
        setHistoryError(humanizeRequirementError(e));
        setHistoryStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [planId]);

  const resolveSource = (sourceId: string): string => {
    const entry = assemblies.find((assembly) => assembly.id.value === sourceId);
    return entry ? `${entry.number} — ${entry.name}` : "Сборка не найдена";
  };

  const handleCalculate = async () => {
    if (runStatus === "running") return;
    setRunStatus("running");
    setRunError(null);
    try {
      const result = await planningApi.calculate(planId);
      setSelected(result);
      setCalculations((prev) => [result, ...(prev ?? [])]);
    } catch (e) {
      setRunError(humanizeRequirementError(e));
    } finally {
      setRunStatus("idle");
    }
  };

  return (
    <section className="requirements-calculations">
      <h3>Расчёты потребностей</h3>
      <button
        onClick={() => void handleCalculate()}
        disabled={runStatus === "running" || historyStatus === "loading"}
      >
        {runStatus === "running" ? "Расчёт…" : "Рассчитать потребности"}
      </button>

      {runError && <div className="banner banner-error">{runError}</div>}

      {historyStatus === "loading" && <p className="muted">Загрузка истории…</p>}
      {historyStatus === "error" && <div className="banner banner-error">{historyError}</div>}
      {historyStatus === "ready" && calculations && calculations.length === 0 && (
        <p className="muted">История расчётов пуста</p>
      )}
      {historyStatus === "ready" && calculations && calculations.length > 0 && (
        <ul className="calculation-history" aria-label="История расчётов">
          {calculations.map((calculation) => (
            <li key={calculation.id.value}>
              <button
                type="button"
                className={
                  selected?.id.value === calculation.id.value ? "selected" : undefined
                }
                onClick={() => setSelected(calculation)}
              >
                {formatExecutedAt(calculation.executedAt)}
              </button>
            </li>
          ))}
        </ul>
      )}

      {selected && (
        <>
          {selected.items.length === 0 ? (
            <p className="muted">Позиции отсутствуют</p>
          ) : (
            <table className="catalog-table">
              <thead>
                <tr>
                  <th>Тип</th>
                  <th>Номер</th>
                  <th>Наименование</th>
                  <th>Количество</th>
                  <th>Вид потребности</th>
                  <th>Единица</th>
                  <th>Источники</th>
                </tr>
              </thead>
              <tbody>
                {selected.items.map((item) => (
                  <tr key={item.id}>
                    <td>{requirementItemTypeLabel(item.type)}</td>
                    <td>{item.number}</td>
                    <td>{item.name}</td>
                    <td>{item.quantity}</td>
                    <td>{requirementNeedTypeLabel(item.needType)}</td>
                    <td>{item.unit}</td>
                    <td>
                      {item.sources.length === 0
                        ? "—"
                        : item.sources.map((source) => resolveSource(source.value)).join(", ")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}
