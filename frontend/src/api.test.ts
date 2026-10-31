import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  CATALOG_TYPES,
  catalogApi,
  humanizeSpecificationError,
  planningApi,
  specificationApi,
  type CatalogItemReference,
} from "./api";

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

  it("get отправляет GET на {endpoint}/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, ENTRY));

    await catalogApi.get("/api/v1/parts", "1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/parts/1");
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

  it("flat отправляет GET на /api/v1/specifications/{assemblyId}/flat", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, [{ number: "Д-0001", name: "Втулка", quantity: 11 }]),
    );

    await specificationApi.flat("A-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/specifications/A-1/flat");
  });

  it("listSubstitutes отправляет GET с query itemType и itemId", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    await specificationApi.listSubstitutes("A-1", ITEM);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/substitutes?itemType=PART&itemId=00000000-0000-0000-0000-000000000001",
    );
  });

  it("addSubstitute отправляет POST с телом item и substituteRef", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, ITEM));

    await specificationApi.addSubstitute("A-1", ITEM, ITEM);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/substitutes",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ item: ITEM, substituteRef: ITEM }),
      }),
    );
  });

  it("removeSubstitute отправляет DELETE с телом item и substituteRef", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await specificationApi.removeSubstitute("A-1", ITEM, ITEM);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/specifications/A-1/substitutes",
      expect.objectContaining({
        method: "DELETE",
        body: JSON.stringify({ item: ITEM, substituteRef: ITEM }),
      }),
    );
  });

  it("humanizeSpecificationError сообщает о цикле для bom_cycle", () => {
    expect(humanizeSpecificationError(new ApiClientError(409, "bom_cycle", "...")))
      .toBe("В спецификации обнаружен цикл");
  });
});

describe("planningApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const PLAN = {
    id: { value: "plan-1" },
    name: "Декабрь 2027",
    year: 2027,
    month: 12,
    items: [],
  };

  it("list отправляет GET на /api/v1/plans", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    await planningApi.list();

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/plans");
  });

  it("create отправляет POST с телом плана", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, PLAN));

    await planningApi.create({ name: "Декабрь 2027", year: 2027, month: 12 });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/plans",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ name: "Декабрь 2027", year: 2027, month: 12 }),
      }),
    );
  });

  it("addItem отправляет POST с assemblyId и quantity", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(201, { assemblyId: { value: "a-1" }, quantity: 2 }),
    );

    await planningApi.addItem("plan-1", "a-1", 2);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/plans/plan-1/items",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ assemblyId: { value: "a-1" }, quantity: 2 }),
      }),
    );
  });

  it("flat отправляет GET на /api/v1/plans/{id}/flat", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, [{ number: "Д-0001", name: "Втулка", quantity: 10 }]),
    );

    await planningApi.flat("plan-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/plans/plan-1/flat");
  });
});
