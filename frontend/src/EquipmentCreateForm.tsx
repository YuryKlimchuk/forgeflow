import { useState, type FormEvent } from "react";
import type { CreateEquipmentRequest } from "./api";

interface Props {
  onCreate: (request: CreateEquipmentRequest) => Promise<boolean>;
}

export default function EquipmentCreateForm({ onCreate }: Props) {
  const [number, setNumber] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !name.trim() || !type.trim()) {
      return;
    }
    setSubmitting(true);
    const ok = await onCreate({
      number: number.trim(),
      name: name.trim(),
      type: type.trim(),
    });
    setSubmitting(false);
    if (ok) {
      setNumber("");
      setName("");
      setType("");
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
      <input
        value={type}
        onChange={(e) => setType(e.target.value)}
        placeholder="Тип"
        aria-label="Тип"
      />
      <button type="submit" disabled={submitting}>
        {submitting ? "Создание…" : "Создать"}
      </button>
    </form>
  );
}
