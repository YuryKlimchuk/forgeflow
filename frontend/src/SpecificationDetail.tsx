import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  humanizeSpecificationError,
  specificationApi,
  type CatalogItemReference,
  type CatalogItemType,
  type FlatBomItem,
  type SpecificationRow,
} from "./api";

interface Props {
  assemblyId: string;
  onBack: () => void;
}

type Status = "loading" | "ready" | "error";

export default function SpecificationDetail({ assemblyId, onBack }: Props) {
  const [rows, setRows] = useState<SpecificationRow[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);

  const [itemType, setItemType] = useState<CatalogItemType>("PART");
  const [itemId, setItemId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [copyNumber, setCopyNumber] = useState("");
  const [copyName, setCopyName] = useState("");
  const [copyResult, setCopyResult] = useState<string | null>(null);

  const [flatItems, setFlatItems] = useState<FlatBomItem[] | null>(null);
  const [flatStatus, setFlatStatus] = useState<"idle" | "loading" | "ready" | "error">("idle");
  const [flatError, setFlatError] = useState<string | null>(null);

  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [draftQuantity, setDraftQuantity] = useState("");

  const load = useCallback(async () => {
    setStatus("loading");
    setError(null);
    try {
      setRows(await specificationApi.list(assemblyId));
      setStatus("ready");
    } catch (e) {
      setError(humanizeSpecificationError(e));
      setStatus("error");
    }
  }, [assemblyId]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleAdd = async (event: FormEvent) => {
    event.preventDefault();
    if (!itemId.trim() || !quantity.trim()) return;
    const item: CatalogItemReference = { type: itemType, id: itemId.trim() };
    setSubmitting(true);
    setError(null);
    try {
      await specificationApi.addRow(assemblyId, item, Number(quantity));
      setItemId("");
      setQuantity("");
      await load();
    } catch (e) {
      setError(humanizeSpecificationError(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeQuantity = async (item: CatalogItemReference) => {
    if (!draftQuantity.trim()) return;
    setError(null);
    try {
      await specificationApi.changeQuantity(assemblyId, item, Number(draftQuantity));
      setEditingItemId(null);
      setDraftQuantity("");
      await load();
    } catch (e) {
      setError(humanizeSpecificationError(e));
    }
  };

  const handleRemove = async (item: CatalogItemReference) => {
    setError(null);
    try {
      await specificationApi.removeRow(assemblyId, item);
      await load();
    } catch (e) {
      setError(humanizeSpecificationError(e));
    }
  };

  const handleCopy = async (event: FormEvent) => {
    event.preventDefault();
    if (!copyNumber.trim() || !copyName.trim()) return;
    setSubmitting(true);
    setError(null);
    setCopyResult(null);
    try {
      const created = await specificationApi.copy(assemblyId, copyNumber.trim(), copyName.trim());
      setCopyResult(`Создана сборка: ${created.number}`);
      setCopyNumber("");
      setCopyName("");
    } catch (e) {
      setError(humanizeSpecificationError(e));
    } finally {
      setSubmitting(false);
    }
  };

  const handleFlat = async () => {
    setFlatStatus("loading");
    setFlatError(null);
    setFlatItems(null);
    try {
      setFlatItems(await specificationApi.flat(assemblyId));
      setFlatStatus("ready");
    } catch (e) {
      setFlatError(humanizeSpecificationError(e));
      setFlatStatus("error");
    }
  };

  return (
    <section className="specification-panel">
      <button onClick={onBack}>Назад</button>
      <h2>Спецификация</h2>
      {error && <div className="banner banner-error">{error}</div>}
      {status === "loading" && <p className="muted">Загрузка…</p>}
      {status === "error" && !error && <p className="muted">Не удалось загрузить спецификацию.</p>}
      {status === "ready" && (
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
    </section>
  );
}
