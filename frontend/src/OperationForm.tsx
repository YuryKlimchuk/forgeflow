import { useEffect, useState, type FormEvent } from "react";
import {
  equipmentApi,
  humanizeTechnologyError,
  technologyApi,
  type EquipmentEntry,
} from "./api";

interface Props {
  processId: string;
  onAdded: () => void;
}

export default function OperationForm({ processId, onAdded }: Props) {
  const [number, setNumber] = useState("");
  const [name, setName] = useState("");
  const [equipment, setEquipment] = useState<EquipmentEntry[]>([]);
  const [selected, setSelected] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const parsed = Number(number);
    if (!name.trim() || !Number.isInteger(parsed) || parsed <= 0) return;
    setSubmitting(true);
    setError(null);
    try {
      await technologyApi.addOperation(processId, {
        number: parsed,
        name: name.trim(),
        equipmentId: selected ? { value: selected } : null,
      });
      setNumber("");
      setName("");
      setSelected("");
      onAdded();
    } catch (e) {
      setError(humanizeTechnologyError(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="create-form" onSubmit={handleSubmit}>
      {error && <div className="banner banner-error">{error}</div>}
      <input
        value={number}
        onChange={(e) => setNumber(e.target.value)}
        placeholder="Номер операции"
        aria-label="Номер операции"
      />
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Название операции"
        aria-label="Название операции"
      />
      <select
        value={selected}
        onChange={(e) => setSelected(e.target.value)}
        aria-label="Оборудование"
      >
        <option value="">без оборудования</option>
        {equipment.map((entry) => (
          <option key={entry.id.value} value={entry.id.value}>
            {entry.name}
          </option>
        ))}
      </select>
      <button type="submit" disabled={submitting}>
        {submitting ? "Добавление…" : "Добавить операцию"}
      </button>
    </form>
  );
}
