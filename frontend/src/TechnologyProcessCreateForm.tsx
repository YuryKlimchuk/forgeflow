import { useEffect, useState, type FormEvent } from "react";
import {
  catalogApi,
  type CatalogEntry,
  type CreateTechnologyProcessRequest,
} from "./api";

interface Props {
  onCreate: (request: CreateTechnologyProcessRequest) => Promise<boolean>;
}

export default function TechnologyProcessCreateForm({ onCreate }: Props) {
  const [number, setNumber] = useState("");
  const [name, setName] = useState("");
  const [parts, setParts] = useState<CatalogEntry[]>([]);
  const [selectedPart, setSelectedPart] = useState<CatalogEntry | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    catalogApi
      .list("/api/v1/parts")
      .then((list) => {
        if (!cancelled) setParts(list);
      })
      .catch(() => {
        // остаёмся с пустым списком — выбор детали недоступен
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const query = searchQuery.trim().toLowerCase();
  const filteredParts = parts.filter(
    (entry) =>
      !query ||
      entry.number.toLowerCase().includes(query) ||
      entry.name.toLowerCase().includes(query),
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!number.trim() || !name.trim() || !selectedPart) return;
    setSubmitting(true);
    const ok = await onCreate({
      number: number.trim(),
      name: name.trim(),
      partId: { value: selectedPart.id.value },
    });
    setSubmitting(false);
    if (ok) {
      setNumber("");
      setName("");
      setSelectedPart(null);
      setSearchQuery("");
    }
  };

  return (
    <>
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
        {selectedPart ? (
          <span className="selected-element">
            {selectedPart.number} — {selectedPart.name}
            <button
              type="button"
              onClick={() => {
                setSelectedPart(null);
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
            placeholder="Поиск детали"
            aria-label="Поиск детали"
          />
        )}
        <button type="submit" disabled={submitting || !selectedPart}>
          {submitting ? "Создание…" : "Создать"}
        </button>
      </form>
      {!selectedPart && (
        <ul className="element-options" aria-label="Варианты детали">
          {filteredParts.map((entry) => (
            <li key={entry.id.value}>
              <button
                type="button"
                onClick={() => {
                  setSelectedPart(entry);
                  setSearchQuery("");
                }}
              >
                {entry.number} — {entry.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
