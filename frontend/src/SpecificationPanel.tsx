import { useState, type FormEvent } from "react";
import {
  humanizeSpecificationError,
  specificationApi,
  type CatalogItemReference,
  type CatalogItemType,
  type SpecificationRow,
} from "./api";

type Status = "idle" | "loading" | "ready" | "error";

export default function SpecificationPanel() {
  const [assemblyId, setAssemblyId] = useState("");
  const [openedId, setOpenedId] = useState<string | null>(null);
  const [rows, setRows] = useState<SpecificationRow[]>([]);
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string | null>(null);

  const [itemType, setItemType] = useState<CatalogItemType>("PART");
  const [itemId, setItemId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [copyNumber, setCopyNumber] = useState("");
  const [copyName, setCopyName] = useState("");
  const [copyResult, setCopyResult] = useState<string | null>(null);

  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [draftQuantity, setDraftQuantity] = useState("");

  const load = async (id: string) => {
    setStatus("loading");
    setError(null);
    try {
      setRows(await specificationApi.list(id));
      setStatus("ready");
    } catch (e) {
      setError(humanizeSpecificationError(e));
      setStatus("error");
    }
  };

  const handleOpen = async (event: FormEvent) => {
    event.preventDefault();
    const id = assemblyId.trim();
    if (!id) return;
    setOpenedId(id);
    await load(id);
  };

  const handleAdd = async (event: FormEvent) => {
    event.preventDefault();
    if (!openedId || !itemId.trim() || !quantity.trim()) return;
    const item: CatalogItemReference = { type: itemType, id: itemId.trim() };
    setSubmitting(true);
    setError(null);
    try {
      await specificationApi.addRow(openedId, item, Number(quantity));
      setItemId("");
      setQuantity("");
      await load(openedId);
    } catch (e) {
      setError(humanizeSpecificationError(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeQuantity = async (item: CatalogItemReference) => {
    if (!openedId || !draftQuantity.trim()) return;
    setError(null);
    try {
      await specificationApi.changeQuantity(openedId, item, Number(draftQuantity));
      setEditingItemId(null);
      setDraftQuantity("");
      await load(openedId);
    } catch (e) {
      setError(humanizeSpecificationError(e));
    }
  };

  const handleRemove = async (item: CatalogItemReference) => {
    if (!openedId) return;
    setError(null);
    try {
      await specificationApi.removeRow(openedId, item);
      await load(openedId);
    } catch (e) {
      setError(humanizeSpecificationError(e));
    }
  };

  const handleCopy = async (event: FormEvent) => {
    event.preventDefault();
    if (!openedId || !copyNumber.trim() || !copyName.trim()) return;
    setSubmitting(true);
    setError(null);
    setCopyResult(null);
    try {
      const created = await specificationApi.copy(openedId, copyNumber.trim(), copyName.trim());
      setCopyResult(`Создана сборка: ${created.number}`);
      setCopyNumber("");
      setCopyName("");
    } catch (e) {
      setError(humanizeSpecificationError(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="specification-panel">
      <h2>Спецификация</h2>
      <form className="create-form" onSubmit={handleOpen}>
        <input
          value={assemblyId}
          onChange={(e) => setAssemblyId(e.target.value)}
          placeholder="ID сборки"
          aria-label="ID сборки"
        />
        <button type="submit">Открыть</button>
      </form>

      {error && <div className="banner banner-error">{error}</div>}

      {status === "loading" && <p className="muted">Загрузка…</p>}

      {status === "ready" && openedId && (
        <>
          {rows.length === 0 ? (
            <p className="muted">Спецификация пуста</p>
          ) : (
            <table className="catalog-table">
              <thead>
                <tr>
                  <th>Тип</th>
                  <th>ID</th>
                  <th>Количество</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={`${row.item.type}:${row.item.id}`}>
                    <td>{row.item.type}</td>
                    <td>{row.item.id}</td>
                    <td>
                      {editingItemId === row.item.id ? (
                        <input
                          value={draftQuantity}
                          onChange={(e) => setDraftQuantity(e.target.value)}
                          aria-label="Новое количество"
                        />
                      ) : (
                        row.quantity
                      )}
                    </td>
                    <td className="actions">
                      {editingItemId === row.item.id ? (
                        <button onClick={() => void handleChangeQuantity(row.item)}>
                          Сохранить количество
                        </button>
                      ) : (
                        <button
                          onClick={() => {
                            setEditingItemId(row.item.id);
                            setDraftQuantity(String(row.quantity));
                          }}
                        >
                          Изменить
                        </button>
                      )}
                      <button onClick={() => void handleRemove(row.item)}>Удалить строку</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          <form className="create-form" onSubmit={handleAdd}>
            <select
              value={itemType}
              onChange={(e) => setItemType(e.target.value as CatalogItemType)}
              aria-label="Тип элемента"
            >
              <option value="PART">PART</option>
              <option value="ASSEMBLY">ASSEMBLY</option>
              <option value="BUY">BUY</option>
              <option value="STANDART">STANDART</option>
              <option value="VZK">VZK</option>
            </select>
            <input
              value={itemId}
              onChange={(e) => setItemId(e.target.value)}
              placeholder="ID элемента"
              aria-label="ID элемента"
            />
            <input
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="Количество"
              aria-label="Количество"
            />
            <button type="submit" disabled={submitting}>
              {submitting ? "Создание…" : "Добавить строку"}
            </button>
          </form>

          <form className="create-form" onSubmit={handleCopy}>
            <input
              value={copyNumber}
              onChange={(e) => setCopyNumber(e.target.value)}
              placeholder="Номер сборки"
              aria-label="Номер сборки"
            />
            <input
              value={copyName}
              onChange={(e) => setCopyName(e.target.value)}
              placeholder="Имя сборки"
              aria-label="Имя сборки"
            />
            <button type="submit" disabled={submitting}>
              Скопировать
            </button>
          </form>
          {copyResult && <p className="muted">{copyResult}</p>}
        </>
      )}

      {status === "error" && !error && <p className="muted">Не удалось загрузить спецификацию.</p>}
    </section>
  );
}
