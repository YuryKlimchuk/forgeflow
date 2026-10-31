import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  humanizeSpecificationError,
  loadCatalogIndex,
  specificationApi,
  type CatalogEntry,
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

const EMPTY_INDEX: Record<CatalogItemType, CatalogEntry[]> = {
  PART: [],
  ASSEMBLY: [],
  BUY: [],
  STANDART: [],
  VZK: [],
};

export default function SpecificationDetail({ assemblyId, onBack }: Props) {
  const [rows, setRows] = useState<SpecificationRow[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);

  const [catalogByType, setCatalogByType] =
    useState<Record<CatalogItemType, CatalogEntry[]>>(EMPTY_INDEX);

  const [itemType, setItemType] = useState<CatalogItemType>("PART");
  const [selectedItem, setSelectedItem] = useState<CatalogEntry | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
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

  const [substituteRow, setSubstituteRow] = useState<SpecificationRow | null>(null);
  const [substitutes, setSubstitutes] = useState<CatalogItemReference[]>([]);
  const [substitutesStatus, setSubstitutesStatus] = useState<"loading" | "ready" | "error">("loading");
  const [substitutesError, setSubstitutesError] = useState<string | null>(null);
  const [substituteSearch, setSubstituteSearch] = useState("");
  const [selectedSubstitute, setSelectedSubstitute] = useState<CatalogEntry | null>(null);
  const [substituting, setSubstituting] = useState(false);

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

  useEffect(() => {
    let cancelled = false;
    loadCatalogIndex()
      .then((index) => {
        if (!cancelled) setCatalogByType(index);
      })
      .catch(() => {
        // остаёмся с пустым индексом — строки покажут «Элемент не найден»
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const resolve = (item: CatalogItemReference): CatalogEntry | undefined => {
    return (catalogByType[item.type] ?? []).find((entry) => entry.id.value === item.id);
  };

  const query = searchQuery.trim().toLowerCase();
  const filteredOptions = (catalogByType[itemType] ?? []).filter(
    (entry) =>
      !query ||
      entry.number.toLowerCase().includes(query) ||
      entry.name.toLowerCase().includes(query),
  );

  const substituteQuery = substituteSearch.trim().toLowerCase();
  const substituteCandidates = substituteRow
    ? (catalogByType[substituteRow.item.type] ?? []).filter(
        (entry) =>
          entry.id.value !== substituteRow.item.id &&
          !substitutes.some((substitute) => substitute.id === entry.id.value) &&
          (!substituteQuery ||
            entry.number.toLowerCase().includes(substituteQuery) ||
            entry.name.toLowerCase().includes(substituteQuery)),
      )
    : [];

  const handleTypeChange = (value: CatalogItemType) => {
    setItemType(value);
    setSelectedItem(null);
    setSearchQuery("");
  };

  const handleAdd = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedItem || !quantity.trim()) return;
    const item: CatalogItemReference = { type: itemType, id: selectedItem.id.value };
    setSubmitting(true);
    setError(null);
    try {
      await specificationApi.addRow(assemblyId, item, Number(quantity));
      setSelectedItem(null);
      setQuantity("");
      setSearchQuery("");
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

  const loadSubstitutes = useCallback(async (row: SpecificationRow) => {
    setSubstitutesStatus("loading");
    setSubstitutesError(null);
    try {
      setSubstitutes(await specificationApi.listSubstitutes(assemblyId, row.item));
      setSubstitutesStatus("ready");
    } catch (e) {
      setSubstitutesError(humanizeSpecificationError(e));
      setSubstitutesStatus("error");
    }
  }, [assemblyId]);

  const openSubstitutes = (row: SpecificationRow) => {
    setSubstituteRow(row);
    setSelectedSubstitute(null);
    setSubstituteSearch("");
    void loadSubstitutes(row);
  };

  const closeSubstitutes = () => {
    setSubstituteRow(null);
    setSubstitutes([]);
    setSubstitutesError(null);
  };

  const handleRemoveSubstitute = async (substitute: CatalogItemReference) => {
    if (!substituteRow) return;
    setSubstitutesError(null);
    try {
      await specificationApi.removeSubstitute(assemblyId, substituteRow.item, substitute);
      await loadSubstitutes(substituteRow);
      await load();
    } catch (e) {
      setSubstitutesError(humanizeSpecificationError(e));
    }
  };

  const handleAddSubstitute = async (event: FormEvent) => {
    event.preventDefault();
    if (!substituteRow || !selectedSubstitute) return;
    setSubstituting(true);
    setSubstitutesError(null);
    try {
      const substituteRef: CatalogItemReference = {
        type: substituteRow.item.type,
        id: selectedSubstitute.id.value,
      };
      await specificationApi.addSubstitute(assemblyId, substituteRow.item, substituteRef);
      setSelectedSubstitute(null);
      setSubstituteSearch("");
      await loadSubstitutes(substituteRow);
      await load();
    } catch (e) {
      setSubstitutesError(humanizeSpecificationError(e));
    } finally {
      setSubstituting(false);
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
                  <th>Номер</th>
                  <th>Название</th>
                  <th>Количество</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const entry = resolve(row.item);
                  return (
                    <tr key={`${row.item.type}:${row.item.id}`}>
                      <td>
                        {entry?.number ?? "Элемент не найден"}
                        {row.hasSubstitutes && (
                          <button
                            type="button"
                            className="substitute-marker"
                            aria-label="Заменители"
                            onClick={() => openSubstitutes(row)}
                          >
                            *
                          </button>
                        )}
                      </td>
                      <td>{entry?.name ?? ""}</td>
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
                  );
                })}
              </tbody>
            </table>
          )}

          <form className="create-form" onSubmit={handleAdd}>
            <select
              value={itemType}
              onChange={(e) => handleTypeChange(e.target.value as CatalogItemType)}
              aria-label="Тип элемента"
            >
              <option value="PART">PART</option>
              <option value="ASSEMBLY">ASSEMBLY</option>
              <option value="BUY">BUY</option>
              <option value="STANDART">STANDART</option>
              <option value="VZK">VZK</option>
            </select>
            {selectedItem ? (
              <span className="selected-element">
                {selectedItem.number} — {selectedItem.name}
                <button
                  type="button"
                  onClick={() => {
                    setSelectedItem(null);
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
                placeholder="Поиск элемента"
                aria-label="Поиск элемента"
              />
            )}
            <input
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="Количество"
              aria-label="Количество"
            />
            <button type="submit" disabled={submitting || !selectedItem}>
              {submitting ? "Создание…" : "Добавить строку"}
            </button>
          </form>
          {!selectedItem && (
            <ul className="element-options" aria-label="Варианты элемента">
              {filteredOptions.map((entry) => (
                <li key={`${itemType}:${entry.id.value}`}>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedItem(entry);
                      setSearchQuery("");
                    }}
                  >
                    {entry.number} — {entry.name}
                  </button>
                </li>
              ))}
            </ul>
          )}

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

      {substituteRow && (
        <div className="substitute-modal" role="dialog" aria-label="Дополнительные замены">
          <button onClick={closeSubstitutes}>Закрыть</button>
          <h3>Дополнительные замены</h3>
          {substitutesError && <div className="banner banner-error">{substitutesError}</div>}
          {substitutesStatus === "loading" && <p className="muted">Загрузка…</p>}
          {substitutesStatus === "ready" && (
            <>
              {substitutes.length === 0 ? (
                <p className="muted">Замены не заданы</p>
              ) : (
                <ul aria-label="Список заменителей">
                  {substitutes.map((substitute) => {
                    const entry = resolve(substitute);
                    return (
                      <li key={`${substitute.type}:${substitute.id}`}>
                        {entry?.number ?? "Элемент не найден"} — {entry?.name ?? ""}
                        <button
                          type="button"
                          onClick={() => void handleRemoveSubstitute(substitute)}
                        >
                          Удалить
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </>
          )}
          <form className="create-form" onSubmit={handleAddSubstitute}>
            {selectedSubstitute ? (
              <span className="selected-element">
                {selectedSubstitute.number} — {selectedSubstitute.name}
                <button
                  type="button"
                  onClick={() => {
                    setSelectedSubstitute(null);
                    setSubstituteSearch("");
                  }}
                >
                  Сменить
                </button>
              </span>
            ) : (
              <input
                value={substituteSearch}
                onChange={(e) => setSubstituteSearch(e.target.value)}
                placeholder="Поиск замены"
                aria-label="Поиск замены"
              />
            )}
            <button type="submit" disabled={substituting || !selectedSubstitute}>
              {substituting ? "Добавление…" : "Добавить замену"}
            </button>
          </form>
          {!selectedSubstitute && (
            <ul className="element-options" aria-label="Варианты замены">
              {substituteCandidates.map((entry) => (
                <li key={`${substituteRow.item.type}:${entry.id.value}`}>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedSubstitute(entry);
                      setSubstituteSearch("");
                    }}
                  >
                    {entry.number} — {entry.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
