import type { CatalogEntry } from "./api";

interface Props {
  entries: CatalogEntry[];
  onSelect: (entry: CatalogEntry) => void;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ru-RU");
}

export default function CatalogTable({ entries, onSelect }: Props) {
  if (entries.length === 0) {
    return <p className="muted">Пока нет записей. Создайте первую.</p>;
  }

  return (
    <table className="catalog-table">
      <thead>
        <tr>
          <th>Номер</th>
          <th>Название</th>
          <th>Создано</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry) => (
          <tr
            key={entry.id.value}
            className="clickable-row"
            onClick={() => onSelect(entry)}
          >
            <td>{entry.number}</td>
            <td>{entry.name}</td>
            <td>{formatDate(entry.createdAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
