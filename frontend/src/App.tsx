import { useCallback, useEffect, useState } from "react";
import {
  CATALOG_TYPES,
  catalogApi,
  humanizeError,
  type CatalogEntry,
  type CatalogTypeConfig,
} from "./api";
import CatalogTable from "./CatalogTable";
import CatalogDetail from "./CatalogDetail";
import CreateCatalogEntryForm from "./CreateCatalogEntryForm";
import SpecificationDetail from "./SpecificationDetail";
import PlanList from "./PlanList";
import PlanDetail from "./PlanDetail";
import { login, logout, isAuthenticated } from "./auth";
import "./App.css";

type Status = "loading" | "ready" | "error";
type View = "list" | "catalog-detail" | "spec-detail" | "plans" | "plan-detail";

export default function App() {
  const [activeKey, setActiveKey] = useState(CATALOG_TYPES[0].key);
  const [view, setView] = useState<View>("list");
  const [selectedEntry, setSelectedEntry] = useState<CatalogEntry | null>(null);
  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);
  const [entries, setEntries] = useState<CatalogEntry[]>([]);
  const [status, setStatus] = useState<Status>("loading");
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [authenticated, setAuthenticated] = useState(isAuthenticated());

  const activeType =
    CATALOG_TYPES.find((type) => type.key === activeKey) ?? CATALOG_TYPES[0];

  const filteredEntries = entries.filter((entry) => {
    const query = searchQuery.trim().toLowerCase();
    return (
      !query ||
      entry.number.toLowerCase().includes(query) ||
      entry.name.toLowerCase().includes(query)
    );
  });

  const load = useCallback(async (type: CatalogTypeConfig) => {
    setStatus("loading");
    setError(null);
    try {
      setEntries(await catalogApi.list(type.endpoint));
      setStatus("ready");
    } catch (e) {
      setError(humanizeError(e));
      setStatus("error");
    }
  }, []);

  useEffect(() => {
    if (view === "list") {
      void load(activeType);
    }
  }, [load, activeType, view]);

  const handleCreate = async (request: { number: string; name: string }): Promise<boolean> => {
    setError(null);
    try {
      await catalogApi.create(activeType.endpoint, request);
      await load(activeType);
      return true;
    } catch (e) {
      setError(humanizeError(e));
      return false;
    }
  };

  const openDetail = (entry: CatalogEntry) => {
    setSelectedEntry(entry);
    setView("catalog-detail");
  };

  const backToList = () => {
    setSelectedEntry(null);
    setView("list");
  };

  const openSpec = () => {
    setView("spec-detail");
  };

  const backToDetail = () => {
    setView("catalog-detail");
  };

  const handleLogin = async (realm: "users" | "admins") => {
    await login(realm);
    setAuthenticated(isAuthenticated());
  };

  const handleLogout = async () => {
    await logout();
    setAuthenticated(false);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>ForgeFlow — Каталог</h1>
        <nav className="app-nav">
          <button onClick={() => { setView("list"); setSelectedEntry(null); setSelectedPlanId(null); }}>
            Каталог
          </button>
          <button onClick={() => { setView("plans"); setSelectedPlanId(null); }}>
            Планы
          </button>
        </nav>
        <div className="app-auth">
          {authenticated ? (
            <button onClick={handleLogout}>Выйти</button>
          ) : (
            <>
              <button onClick={() => handleLogin("users")}>Войти как пользователь</button>
              <button onClick={() => handleLogin("admins")}>Войти в админку</button>
            </>
          )}
        </div>
      </header>
      <main className="app-main">
        {view === "list" && (
          <>
            <nav className="tabs" aria-label="Типы каталога">
              {CATALOG_TYPES.map((type) => (
                <button
                  key={type.key}
                  className={type.key === activeKey ? "tab tab-active" : "tab"}
                  onClick={() => {
                    setActiveKey(type.key);
                    setSearchQuery("");
                  }}
                >
                  {type.label}
                </button>
              ))}
            </nav>
            {error && <div className="banner banner-error">{error}</div>}
            <CreateCatalogEntryForm onCreate={handleCreate} />
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Поиск по номеру или названию"
              aria-label="Поиск каталога"
              className="catalog-search"
            />
            {status === "loading" && <p className="muted">Загрузка…</p>}
            {status === "ready" && filteredEntries.length > 0 && (
              <CatalogTable entries={filteredEntries} onSelect={openDetail} />
            )}
            {status === "ready" && entries.length === 0 && (
              <p className="muted">Пока нет записей. Создайте первую.</p>
            )}
            {status === "ready" && entries.length > 0 && filteredEntries.length === 0 && (
              <p className="muted">Ничего не найдено.</p>
            )}
            {status === "error" && !error && (
              <p className="muted">Не удалось загрузить список.</p>
            )}
          </>
        )}

        {view === "catalog-detail" && selectedEntry && (
          <CatalogDetail
            type={activeType}
            id={selectedEntry.id.value}
            onBack={backToList}
            onOpenSpec={activeType.key === "assemblies" ? openSpec : undefined}
          />
        )}

        {view === "spec-detail" && selectedEntry && (
          <SpecificationDetail assemblyId={selectedEntry.id.value} onBack={backToDetail} />
        )}

        {view === "plans" && (
          <PlanList
            onOpen={(id) => {
              setSelectedPlanId(id);
              setView("plan-detail");
            }}
          />
        )}

        {view === "plan-detail" && selectedPlanId && (
          <PlanDetail
            id={selectedPlanId}
            onBack={() => setView("plans")}
          />
        )}
      </main>
    </div>
  );
}
