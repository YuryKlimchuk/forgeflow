import { afterEach, describe, expect, it, vi } from "vitest";
import { CATALOG_TYPES, catalogApi, specificationApi, type CatalogItemReference } from "./api";

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

describe("specificationApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const ITEM: CatalogItemReference = { type: "PART", id: "00000000-0000-0000-0000-000000000001" };

  it("list отправляет GET на /api/v1/specifications/{assemblyId}/rows", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    await specificationApi.list("A-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/specifications/A-1/rows");
  });

  it("addRow отправляет POST с телом item и quantity", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, { item: ITEM, quantity: 3 }));

    await specificationApi.addRow("A-1", ITEM, 3);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/rows",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ item: ITEM, quantity: 3 }) }),
    );
  });

  it("changeQuantity отправляет PUT", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await specificationApi.changeQuantity("A-1", ITEM, 5);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/rows",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("removeRow отправляет DELETE с телом item", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await specificationApi.removeRow("A-1", ITEM);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/rows",
      expect.objectContaining({ method: "DELETE", body: JSON.stringify({ item: ITEM }) }),
    );
  });

  it("copy отправляет POST с number и name", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, ENTRY));

    await specificationApi.copy("A-1", "B-1", "Новая");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/copy",
      expect.objectContaining({ method: "POST", body: JSON.stringify({ number: "B-1", name: "Новая" }) }),
    );
  });
});
