export interface PartId {
  value: string;
}

export interface PartView {
  id: PartId;
  number: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePartRequest {
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

const BASE_URL = "/api/v1/parts";

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

export const api = {
  async list(): Promise<PartView[]> {
    return toJsonOrThrow<PartView[]>(await fetch(BASE_URL));
  },
  async create(request: CreatePartRequest): Promise<PartView> {
    return toJsonOrThrow<PartView>(
      await fetch(BASE_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      }),
    );
  },
  async rename(id: string, name: string): Promise<void> {
    return toJsonOrThrow<void>(
      await fetch(`${BASE_URL}/${id}/name`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      }),
    );
  },
  async remove(id: string): Promise<void> {
    return toJsonOrThrow<void>(await fetch(`${BASE_URL}/${id}`, { method: "DELETE" }));
  },
};

export function humanizeError(error: unknown): string {
  if (error instanceof ApiClientError) {
    switch (error.code) {
      case "duplicate":
        return "Номер уже занят другой деталью";
      case "not_found":
        return "Деталь не найдена";
      case "bad_request":
        return "Некорректный запрос";
      default:
        return error.message;
    }
  }
  return "Что-то пошло не так";
}
