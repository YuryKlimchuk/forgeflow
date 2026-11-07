import { getToken } from "./auth";

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

export type StandardType = "GOST" | "DIN" | "ISO" | "OTHER";

export interface DirectoryEntry {
  id: { value: string };
  number: string;
  name: string;
  standardType: StandardType;
  standard: string;
  unit: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDirectoryRequest {
  number: string;
  name: string;
  standardType: StandardType;
  standard: string;
  unit: string;
}

export interface UpdateDirectoryRequest {
  name: string;
  standardType: StandardType;
  standard: string;
  unit: string;
}

export interface EquipmentEntry {
  id: { value: string };
  number: string;
  name: string;
  type: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEquipmentRequest {
  number: string;
  name: string;
  type: string;
}

export interface UpdateEquipmentRequest {
  name: string;
  type: string;
}

export interface OperationView {
  number: number;
  name: string;
  equipmentId: { value: string } | null;
}

export interface TechnologyProcessView {
  id: { value: string };
  number: string;
  name: string;
  partId: { value: string };
  defaultProcess: boolean;
  createdAt: string;
  updatedAt: string;
  operations: OperationView[];
}

export interface CreateTechnologyProcessRequest {
  number: string;
  name: string;
  partId: { value: string };
}

export interface AddOperationRequest {
  number: number;
  name: string;
  equipmentId: { value: string } | null;
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

/**
 * Справочники материалов и сортаментов структурно идентичны (A10/D2) и
 * параметризуются одним конфигом: вкладка отправляет запросы на собственный
 * endpoint, а список/создание/деталь/редактирование/удаление используют общий
 * клиент `directoriesApi`.
 */
export const DIRECTORY_TYPES: CatalogTypeConfig[] = [
  { key: "materials", label: "Материалы", endpoint: "/api/v1/materials" },
  { key: "sortaments", label: "Сортаменты", endpoint: "/api/v1/sortaments" },
];

export function standardTypeLabel(type: StandardType): string {
  switch (type) {
    case "GOST":
      return "ГОСТ";
    case "DIN":
      return "DIN";
    case "ISO":
      return "ISO";
    case "OTHER":
      return "Другое";
  }
  return type;
}

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

async function authFetch(input: RequestInfo, init?: RequestInit): Promise<Response> {
  const token = await getToken();
  if (!token) {
    return init === undefined ? fetch(input) : fetch(input, init);
  }
  const headers = new Headers(init?.headers);
  headers.set("Authorization", `Bearer ${token}`);
  return fetch(input, { ...init, headers });
}

export type CatalogItemType = "PART" | "ASSEMBLY" | "BUY" | "STANDART" | "VZK";

export const CATALOG_ITEM_TYPES: CatalogItemType[] = [
  "PART",
  "ASSEMBLY",
  "BUY",
  "STANDART",
  "VZK",
];

export const CATALOG_ITEM_TYPE_ENDPOINTS: Record<CatalogItemType, string> = {
  PART: "/api/v1/parts",
  ASSEMBLY: "/api/v1/assemblies",
  BUY: "/api/v1/buys",
  STANDART: "/api/v1/standarts",
  VZK: "/api/v1/vzks",
};

export interface CatalogItemReference {
  type: CatalogItemType;
  id: string;
}

export interface SpecificationRow {
  item: CatalogItemReference;
  quantity: number;
  hasSubstitutes: boolean;
}

export interface SubstituteRequest {
  item: CatalogItemReference;
  substituteRef: CatalogItemReference;
}

export interface FlatBomItem {
  number: string;
  name: string;
  quantity: number;
}

export interface PlanItemView {
  assemblyId: { value: string };
  quantity: number;
}

export interface PlanView {
  id: { value: string };
  name: string;
  year: number;
  month: number;
  items: PlanItemView[];
}

export interface CreatePlanRequest {
  name: string;
  year: number;
  month: number;
}

export const specificationApi = {
  async list(assemblyId: string): Promise<SpecificationRow[]> {
    return toJsonOrThrow<SpecificationRow[]>(
      await authFetch(`/api/v1/specifications/${assemblyId}/rows`),
    );
  },

  async addRow(
    assemblyId: string,
    item: CatalogItemReference,
    quantity: number,
  ): Promise<SpecificationRow> {
    return toJsonOrThrow<SpecificationRow>(
      await authFetch(`/api/v1/specifications/${assemblyId}/rows`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ item, quantity }),
      }),
    );
  },

  async changeQuantity(
    assemblyId: string,
    item: CatalogItemReference,
    quantity: number,
  ): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/specifications/${assemblyId}/rows`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ item, quantity }),
      }),
    );
  },

  async removeRow(assemblyId: string, item: CatalogItemReference): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/specifications/${assemblyId}/rows`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ item }),
      }),
    );
  },

  async copy(sourceAssemblyId: string, number: string, name: string): Promise<CatalogEntry> {
    return toJsonOrThrow<CatalogEntry>(
      await authFetch(`/api/v1/specifications/${sourceAssemblyId}/copy`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ number, name }),
      }),
    );
  },

  async flat(assemblyId: string): Promise<FlatBomItem[]> {
    return toJsonOrThrow<FlatBomItem[]>(
      await authFetch(`/api/v1/specifications/${assemblyId}/flat`),
    );
  },

  async listSubstitutes(
    assemblyId: string,
    item: CatalogItemReference,
  ): Promise<CatalogItemReference[]> {
    const params = new URLSearchParams({ itemType: item.type, itemId: item.id });
    return toJsonOrThrow<CatalogItemReference[]>(
      await authFetch(`/api/v1/specifications/${assemblyId}/substitutes?${params}`),
    );
  },

  async addSubstitute(
    assemblyId: string,
    item: CatalogItemReference,
    substituteRef: CatalogItemReference,
  ): Promise<CatalogItemReference> {
    return toJsonOrThrow<CatalogItemReference>(
      await authFetch(`/api/v1/specifications/${assemblyId}/substitutes`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ item, substituteRef } satisfies SubstituteRequest),
      }),
    );
  },

  async removeSubstitute(
    assemblyId: string,
    item: CatalogItemReference,
    substituteRef: CatalogItemReference,
  ): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/specifications/${assemblyId}/substitutes`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ item, substituteRef } satisfies SubstituteRequest),
      }),
    );
  },
};

export const catalogApi = {
  async get(endpoint: string, id: string): Promise<CatalogEntry> {
    return toJsonOrThrow<CatalogEntry>(await authFetch(`${endpoint}/${id}`));
  },

  async list(endpoint: string): Promise<CatalogEntry[]> {
    return toJsonOrThrow<CatalogEntry[]>(await authFetch(endpoint));
  },

  async create(endpoint: string, request: CreateCatalogEntryRequest): Promise<CatalogEntry> {
    return toJsonOrThrow<CatalogEntry>(
      await authFetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async rename(endpoint: string, id: string, name: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`${endpoint}/${id}/name`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      }),
    );
  },

  async remove(endpoint: string, id: string): Promise<void> {
    return toJsonOrThrow<void>(await authFetch(`${endpoint}/${id}`, { method: "DELETE" }));
  },
};

export const directoriesApi = {
  async list(endpoint: string): Promise<DirectoryEntry[]> {
    return toJsonOrThrow<DirectoryEntry[]>(await authFetch(endpoint));
  },

  async get(endpoint: string, id: string): Promise<DirectoryEntry> {
    return toJsonOrThrow<DirectoryEntry>(await authFetch(`${endpoint}/${id}`));
  },

  async create(endpoint: string, request: CreateDirectoryRequest): Promise<DirectoryEntry> {
    return toJsonOrThrow<DirectoryEntry>(
      await authFetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async update(endpoint: string, id: string, request: UpdateDirectoryRequest): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`${endpoint}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async remove(endpoint: string, id: string): Promise<void> {
    return toJsonOrThrow<void>(await authFetch(`${endpoint}/${id}`, { method: "DELETE" }));
  },
};

export const equipmentApi = {
  async list(): Promise<EquipmentEntry[]> {
    return toJsonOrThrow<EquipmentEntry[]>(await authFetch("/api/v1/equipment"));
  },

  async get(id: string): Promise<EquipmentEntry> {
    return toJsonOrThrow<EquipmentEntry>(await authFetch(`/api/v1/equipment/${id}`));
  },

  async create(request: CreateEquipmentRequest): Promise<EquipmentEntry> {
    return toJsonOrThrow<EquipmentEntry>(
      await authFetch("/api/v1/equipment", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async update(id: string, request: UpdateEquipmentRequest): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/equipment/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async remove(id: string): Promise<void> {
    return toJsonOrThrow<void>(await authFetch(`/api/v1/equipment/${id}`, { method: "DELETE" }));
  },
};

export const technologyApi = {
  async list(): Promise<TechnologyProcessView[]> {
    return toJsonOrThrow<TechnologyProcessView[]>(
      await authFetch("/api/v1/technology-processes"),
    );
  },

  async get(id: string): Promise<TechnologyProcessView> {
    return toJsonOrThrow<TechnologyProcessView>(
      await authFetch(`/api/v1/technology-processes/${id}`),
    );
  },

  async create(request: CreateTechnologyProcessRequest): Promise<TechnologyProcessView> {
    return toJsonOrThrow<TechnologyProcessView>(
      await authFetch("/api/v1/technology-processes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async rename(id: string, name: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/technology-processes/${id}/name`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      }),
    );
  },

  async remove(id: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/technology-processes/${id}`, { method: "DELETE" }),
    );
  },

  async addOperation(id: string, request: AddOperationRequest): Promise<OperationView> {
    return toJsonOrThrow<OperationView>(
      await authFetch(`/api/v1/technology-processes/${id}/operations`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async removeOperation(id: string, number: number): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/technology-processes/${id}/operations/${number}`, {
        method: "DELETE",
      }),
    );
  },

  async markDefault(id: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/technology-processes/${id}/default`, { method: "PUT" }),
    );
  },

  async unmarkDefault(id: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/technology-processes/${id}/default`, { method: "DELETE" }),
    );
  },
};

export const planningApi = {
  async create(request: CreatePlanRequest): Promise<PlanView> {
    return toJsonOrThrow<PlanView>(
      await authFetch("/api/v1/plans", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },

  async list(): Promise<PlanView[]> {
    return toJsonOrThrow<PlanView[]>(await authFetch("/api/v1/plans"));
  },

  async get(id: string): Promise<PlanView> {
    return toJsonOrThrow<PlanView>(await authFetch(`/api/v1/plans/${id}`));
  },

  async addItem(id: string, assemblyId: string, quantity: number): Promise<PlanItemView> {
    return toJsonOrThrow<PlanItemView>(
      await authFetch(`/api/v1/plans/${id}/items`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ assemblyId: { value: assemblyId }, quantity }),
      }),
    );
  },

  async changeQuantity(id: string, assemblyId: string, quantity: number): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/plans/${id}/items/${assemblyId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ quantity }),
      }),
    );
  },

  async removeItem(id: string, assemblyId: string): Promise<void> {
    return toJsonOrThrow<void>(
      await authFetch(`/api/v1/plans/${id}/items/${assemblyId}`, { method: "DELETE" }),
    );
  },

  async flat(id: string): Promise<FlatBomItem[]> {
    return toJsonOrThrow<FlatBomItem[]>(await authFetch(`/api/v1/plans/${id}/flat`));
  },
};

export async function loadCatalogIndex(): Promise<Record<CatalogItemType, CatalogEntry[]>> {
  const pairs = await Promise.all(
    CATALOG_ITEM_TYPES.map(async (type) => {
      const list = await catalogApi.list(CATALOG_ITEM_TYPE_ENDPOINTS[type]);
      return [type, list] as const;
    }),
  );
  return Object.fromEntries(pairs) as Record<CatalogItemType, CatalogEntry[]>;
}

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

export function humanizeSpecificationError(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.code) {
      case "duplicate":
        return "Элемент уже присутствует в спецификации";
      case "not_found":
        return "Запись не найдена";
      case "bad_request":
        return "Некорректный запрос";
      case "bom_cycle":
        return "В спецификации обнаружен цикл";
      default:
        return error.message;
    }
  }
  return "Что-то пошло не так";
}

export function humanizePlanError(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.code) {
      case "duplicate":
        return "Такое имя или сборка уже существуют в плане";
      case "not_found":
        return "Запись не найдена";
      case "bad_request":
        return "Некорректный запрос";
      case "bom_cycle":
        return "В спецификации обнаружен цикл";
      default:
        return error.message;
    }
  }
  return "Что-то пошло не так";
}

export function humanizeDirectoriesError(error: unknown): string {
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

export function humanizeTechnologyError(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.code) {
      case "duplicate":
        return "Номер уже занят";
      case "not_found":
        return "Запись не найдена";
      case "bad_request":
        return "Некорректный запрос";
      case "invalid_state":
        return "Нельзя снять признак „по умолчанию“ с единственного техпроцесса детали";
      default:
        return error.message;
    }
  }
  return "Что-то пошло не так";
}
