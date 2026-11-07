import { useState } from "react";
import EquipmentSection from "./EquipmentSection";
import TechnologyProcessSection from "./TechnologyProcessSection";

const TABS = [
  { key: "equipment", label: "Оборудование" },
  { key: "technology", label: "Техпроцессы" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

export default function TechnologySection() {
  const [activeKey, setActiveKey] = useState<TabKey>("equipment");

  return (
    <section>
      <nav className="tabs" aria-label="Разделы технологии">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={tab.key === activeKey ? "tab tab-active" : "tab"}
            onClick={() => setActiveKey(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      {activeKey === "equipment" ? <EquipmentSection /> : <TechnologyProcessSection />}
    </section>
  );
}
