import { useState, type FormEvent } from "react";
import {
  standardTypeLabel,
  type CreateDirectoryRequest,
  type StandardType,
} from "./api";

const STANDARD_TYPES: StandardType[] = ["GOST", "DIN", "ISO", "OTHER"];

interface Props {
  onCreate: (request: CreateDirectoryRequest) => Promise<boolean>;
}

export default function DirectoryCreateForm({ onCreate }: Props) {
  const [number, setNumber] = useState("");
  const [name, setName] = useState("");
  const [standardType, setStandardType] = useState<StandardType | "">("");
  const [standard, setStandard] = useState("");
  const [unit, setUnit] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !name.trim() || !standardType || !standard.trim() || !unit.trim()) {
      return;
    }
    setSubmitting(true);
    const ok = await onCreate({
      number: number.trim(),
      name: name.trim(),
      standardType,
      standard: standard.trim(),
      unit: unit.trim(),
    });
    setSubmitting(false);
    if (ok) {
      setNumber("");
      setName("");
      setStandardType("");
      setStandard("");
      setUnit("");
    }
  };

  return (
    <form className="create-form" onSubmit={handleSubmit}>
      <input
        value={number}
        onChange={(e) => setNumber(e.target.value)}
        placeholder="Номер"
        aria-label="Номер"
      />
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Наименование"
        aria-label="Наименование"
      />
      <select
        value={standardType}
        onChange={(e) => setStandardType(e.target.value as StandardType | "")}
        aria-label="Тип стандарта"
      >
        <option value="">— выберите тип —</option>
        {STANDARD_TYPES.map((type) => (
          <option key={type} value={type}>
            {standardTypeLabel(type)}
          </option>
        ))}
      </select>
      <input
        value={standard}
        onChange={(e) => setStandard(e.target.value)}
        placeholder="Обозначение стандарта"
        aria-label="Обозначение стандарта"
      />
      <input
        value={unit}
        onChange={(e) => setUnit(e.target.value)}
        placeholder="Единица измерения"
        aria-label="Единица измерения"
      />
      <button type="submit" disabled={submitting}>
        {submitting ? "Создание…" : "Создать"}
      </button>
    </form>
  );
}
