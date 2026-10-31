import { useState, type FormEvent } from "react";

interface Props {
  onCreate: (request: { number: string; name: string }) => Promise<boolean>;
}

export default function CreatePartForm({ onCreate }: Props) {
  const [number, setNumber] = useState("");
  const [name, setName] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !name.trim()) {
      return;
    }
    setSubmitting(true);
    const ok = await onCreate({ number: number.trim(), name: name.trim() });
    setSubmitting(false);
    if (ok) {
      setNumber("");
      setName("");
    }
  };

  return (
    <form className="create-form" onSubmit={handleSubmit}>
      <input
        value={number}
        onChange={(e) => setNumber(e.target.value)}
        placeholder="Номер (например P-100)"
        aria-label="Номер"
      />
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Название"
        aria-label="Название"
      />
      <button type="submit" disabled={submitting}>
        {submitting ? "Создание…" : "Создать"}
      </button>
    </form>
  );
}
