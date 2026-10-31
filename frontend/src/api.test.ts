import { afterEach, describe, expect, it, vi } from "vitest";
import { CATALOG_TYPES, catalogApi } from "./api";

const ENTRY = {
  id: { value: "1" },
  number: "P-1",
  name: "Деталь",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: "",
    json: async () => body,
  } as Response;
}

describe("catalogApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("CATALOG_TYPES содержит пять вкладок в правильном порядке", () => {
    expect(CATALOG_TYPES.map((type) => type.label)).toEqual([
      "Детали",
      "Сборки",
      "Покупные",
      "Стандартные",
      "ВЗК",
    ]);
    expect(CATALOG_TYPES.map((type) => type.endpoint)).toEqual([
      "/api/v1/parts",
      "/api/v1/assemblies",
      "/api/v1/buys",
      "/api/v1/standarts",
      "/api/v1/vzks",
    ]);
  });

  it.each(CATALOG_TYPES)("list отправляет GET на $endpoint", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    await catalogApi.list(endpoint);

    expect(fetchMock).toHaveBeenCalledWith(endpoint);
  });

  it.each(CATALOG_TYPES)("create отправляет POST на $endpoint", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, ENTRY));

    await catalogApi.create(endpoint, { number: "P-1", name: "Деталь" });

    expect(fetchMock).toHaveBeenCalledWith(
      endpoint,
      expect.objectContaining({ method: "POST" }),
    );
  });

  it.each(CATALOG_TYPES)("rename отправляет PUT на $endpoint/{id}/name", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await catalogApi.rename(endpoint, "1", "Новое имя");

    expect(fetchMock).toHaveBeenCalledWith(
      `${endpoint}/1/name`,
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it.each(CATALOG_TYPES)("remove отправляет DELETE на $endpoint/{id}", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await catalogApi.remove(endpoint, "1");

    expect(fetchMock).toHaveBeenCalledWith(
      `${endpoint}/1`,
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});
