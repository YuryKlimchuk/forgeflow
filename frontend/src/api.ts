export interface CatalogTypeConfig {
  key: string;
  label: string;
  endpoint: string;
}

export interface CatalogEntry {
  id: { value: string };
  number: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCatalogEntryRequest {
  number: string;
  name: string;
}

export interface ApiErrorBody {
  error: string;
  message: string;
}

export class ApiClientError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

/**
 * Конфигурация пяти типов каталога: каждая вкладка отправляет запросы на
 * собственный endpoint, а список/создание/переименование/удаление используют
 * один общий клиент.
 */
export const CATALOG_TYPES: CatalogTypeConfig[] = [
  { key: "parts", label: "Детали", endpoint: "/api/v1/parts" },
  { key: "assemblies", label: "Сборки", endpoint: "/api/v1/assemblies" },
  { key: "buys", label: "Покупные", endpoint: "/api/v1/buys" },
  { key: "standarts", label: "Стандартные", endpoint: "/api/v1/standarts" },
  { key: "vzks", label: "ВЗК", endpoint: "/api/v1/vzks" },
];

async function toJsonOrThrow<T>(response: Response): Promise<T> {
  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  let body: ApiErrorBody = { error: "unknown", message: response.statusText };
  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    // тело не JSON — оставляем statusText
  }
  throw new ApiClientError(response.status, body.error, body.message);
}

export const catalogApi = {
  async list(endpoint: string): Promise<CatalogEntry[]> {
    return toJsonOrThrow<CatalogEntry[]>(await fetch(endpoint));
  },

  async create(endpoint: string, request: CreateCatalogEntryRequest): Promise<CatalogEntry> {
    return toJsonOrThrow<CatalogEntry>(
      await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async rename(endpoint: string, id: string, name: string): Promise<void> {
    return toJsonOrThrow<void>(
      await fetch(`${endpoint}/${id}/name`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      }),
    );
  },

  async remove(endpoint: string, id: string): Promise<void> {
    return toJsonOrThrow<void>(await fetch(`${endpoint}/${id}`, { method: "DELETE" }));
  },
};

export function humanizeError(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.code) {
      case "duplicate":
        return "Номер уже занят";
      case "not_found":
        return "Запись не найдена";
      case "bad_request":
        return "Некорректный запрос";
      default:
        return error.message;
    }
  }
  return "Что-то пошло не так";
}
