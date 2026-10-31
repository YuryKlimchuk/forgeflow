import { useCallback, useEffect, useState, type FormEvent } from "react";
import { humanizePlanError, planningApi, type PlanView } from "./api";

interface Props {
  onOpen: (id: string) => void;
}

const MONTHS = [
  "январь", "февраль", "март", "апрель", "май", "июнь",
  "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
];

export function formatPeriod(year: number, month: number): string {
  return `${MONTHS[month - 1] ?? month} ${year}`;
}

export default function PlanList({ onOpen }: Props) {
  const [plans, setPlans] = useState<PlanView[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [year, setYear] = useState("2027");
  const [month, setMonth] = useState("1");
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setPlans(await planningApi.list());
      setStatus("ready");
    } catch (e) {
      setError(humanizePlanError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await planningApi.create({ name: name.trim(), year: Number(year), month: Number(month) });
      setName("");
      await load();
    } catch (e) {
      setError(humanizePlanError(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <h2>Планы</h2>
      {error && <div className="banner banner-error">{error}</div>}
      <form className="create-form" onSubmit={handleCreate}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Имя плана"
          aria-label="Имя плана"
        />
        <input
          value={year}
          onChange={(e) => setYear(e.target.value)}
          placeholder="Год"
          aria-label="Год"
        />
        <input
          value={month}
          onChange={(e) => setMonth(e.target.value)}
          placeholder="Месяц"
          aria-label="Месяц"
        />
        <button type="submit" disabled={submitting}>
          {submitting ? "Создание…" : "Создать план"}
        </button>
      </form>
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "ready" && plans.length === 0 && <p className="muted">Пока нет планов.</p>}
      {status === "ready" && plans.length > 0 && (
        <table className="catalog-table">
          <thead>
            <tr>
              <th>Имя</th>
              <th>Период</th>
            </tr>
          </thead>
          <tbody>
            {plans.map((plan) => (
              <tr
                key={plan.id.value}
                className="clickable-row"
                onClick={() => onOpen(plan.id.value)}
              >
                <td>{plan.name}</td>
                <td>{formatPeriod(plan.year, plan.month)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
