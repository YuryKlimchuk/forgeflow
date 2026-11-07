import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  CATALOG_TYPES,
  DIRECTORY_TYPES,
  catalogApi,
  directoriesApi,
  equipmentApi,
  humanizeDirectoriesError,
  humanizeSpecificationError,
  humanizeTechnologyError,
  planningApi,
  specificationApi,
  standardTypeLabel,
  technologyApi,
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

const DIRECTORY_ENTRY = {
  id: { value: "m-1" },
  number: "М-0001",
  name: "Сталь",
  standardType: "GOST",
  standard: "ГОСТ 1050-88",
  unit: "кг",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("directoriesApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("DIRECTORY_TYPES содержит материалы и сортаменты в правильном порядке", () => {
    expect(DIRECTORY_TYPES.map((type) => type.key)).toEqual(["materials", "sortaments"]);
    expect(DIRECTORY_TYPES.map((type) => type.label)).toEqual(["Материалы", "Сортаменты"]);
    expect(DIRECTORY_TYPES.map((type) => type.endpoint)).toEqual([
      "/api/v1/materials",
      "/api/v1/sortaments",
    ]);
  });

  it.each(DIRECTORY_TYPES)("list отправляет GET на $endpoint", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, [DIRECTORY_ENTRY]),
    );

    const result = await directoriesApi.list(endpoint);

    expect(fetchMock).toHaveBeenCalledWith(endpoint);
    expect(result).toEqual([DIRECTORY_ENTRY]);
  });

  it.each(DIRECTORY_TYPES)("get отправляет GET на $endpoint/{id}", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, DIRECTORY_ENTRY),
    );

    await directoriesApi.get(endpoint, "m-1");

    expect(fetchMock).toHaveBeenCalledWith(`${endpoint}/m-1`);
  });

  it.each(DIRECTORY_TYPES)("create отправляет POST на $endpoint", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(201, DIRECTORY_ENTRY),
    );

    await directoriesApi.create(endpoint, {
      number: "М-0001",
      name: "Сталь",
      standardType: "GOST",
      standard: "ГОСТ 1050-88",
      unit: "кг",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      endpoint,
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          number: "М-0001",
          name: "Сталь",
          standardType: "GOST",
          standard: "ГОСТ 1050-88",
          unit: "кг",
        }),
      }),
    );
  });

  it.each(DIRECTORY_TYPES)("update отправляет PUT на $endpoint/{id} без number", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await directoriesApi.update(endpoint, "m-1", {
      name: "Сталь 45",
      standardType: "DIN",
      standard: "DIN 123",
      unit: "шт",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${endpoint}/m-1`,
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({
          name: "Сталь 45",
          standardType: "DIN",
          standard: "DIN 123",
          unit: "шт",
        }),
      }),
    );
  });

  it.each(DIRECTORY_TYPES)("remove отправляет DELETE на $endpoint/{id}", async ({ endpoint }) => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await directoriesApi.remove(endpoint, "m-1");

    expect(fetchMock).toHaveBeenCalledWith(
      `${endpoint}/m-1`,
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});

describe("equipmentApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const EQUIPMENT = {
    id: { value: "e-1" },
    number: "ОБ-0001",
    name: "Токарный станок",
    type: "Станок",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  };

  it("list отправляет GET на /api/v1/equipment", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, [EQUIPMENT]),
    );

    await equipmentApi.list();

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/equipment");
  });

  it("get отправляет GET на /api/v1/equipment/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, EQUIPMENT),
    );

    await equipmentApi.get("e-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/equipment/e-1");
  });

  it("create отправляет POST с телом {number,name,type}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(201, EQUIPMENT),
    );

    await equipmentApi.create({ number: "ОБ-0001", name: "Токарный станок", type: "Станок" });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/equipment",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ number: "ОБ-0001", name: "Токарный станок", type: "Станок" }),
      }),
    );
  });

  it("update отправляет PUT с телом {name,type} без number", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await equipmentApi.update("e-1", { name: "Фрезерный станок", type: "Оборудование" });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/equipment/e-1",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ name: "Фрезерный станок", type: "Оборудование" }),
      }),
    );
  });

  it("remove отправляет DELETE на /api/v1/equipment/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await equipmentApi.remove("e-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/equipment/e-1",
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});

describe("technologyApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  const PROCESS = {
    id: { value: "tp-1" },
    number: "ТП-0001",
    name: "Токарная обработка",
    partId: { value: "p-1" },
    defaultProcess: false,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    operations: [],
  };

  it("create отправляет POST с телом {number,name,partId:{value}}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(201, PROCESS));

    await technologyApi.create({
      number: "ТП-0001",
      name: "Токарная обработка",
      partId: { value: "p-1" },
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          number: "ТП-0001",
          name: "Токарная обработка",
          partId: { value: "p-1" },
        }),
      }),
    );
  });

  it("list отправляет GET на /api/v1/technology-processes", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(200, [PROCESS]),
    );

    await technologyApi.list();

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/technology-processes");
  });

  it("get отправляет GET на /api/v1/technology-processes/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, PROCESS));

    await technologyApi.get("tp-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/technology-processes/tp-1");
  });

  it("rename отправляет PUT на {id}/name с телом {name}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await technologyApi.rename("tp-1", "Фрезерная обработка");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/name",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ name: "Фрезерная обработка" }),
      }),
    );
  });

  it("remove отправляет DELETE на /api/v1/technology-processes/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await technologyApi.remove("tp-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1",
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("addOperation отправляет POST с equipmentId:{value}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(201, { number: 10, name: "Точение", equipmentId: { value: "e-1" } }),
    );

    await technologyApi.addOperation("tp-1", {
      number: 10,
      name: "Точение",
      equipmentId: { value: "e-1" },
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/operations",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ number: 10, name: "Точение", equipmentId: { value: "e-1" } }),
      }),
    );
  });

  it("addOperation сериализует number числом и equipmentId:null", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(201, { number: 10, name: "Точение", equipmentId: null }),
    );

    await technologyApi.addOperation("tp-1", {
      number: 10,
      name: "Точение",
      equipmentId: null,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/operations",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ number: 10, name: "Точение", equipmentId: null }),
      }),
    );
  });

  it("removeOperation отправляет DELETE на {id}/operations/{number}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await technologyApi.removeOperation("tp-1", 10);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/operations/10",
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("markDefault отправляет PUT на {id}/default", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await technologyApi.markDefault("tp-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/default",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("unmarkDefault отправляет DELETE на {id}/default", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(204, undefined));

    await technologyApi.unmarkDefault("tp-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/technology-processes/tp-1/default",
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});

describe("void-методы API", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("204 возвращает undefined и не вызывает response.json()", async () => {
    const json = vi.fn();
    const response = { ok: true, status: 204, statusText: "", json } as unknown as Response;
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response);

    await expect(directoriesApi.update("/api/v1/materials", "m-1", {
      name: "Сталь",
      standardType: "GOST",
      standard: "ГОСТ 1050-88",
      unit: "кг",
    })).resolves.toBeUndefined();
    await expect(directoriesApi.remove("/api/v1/materials", "m-1")).resolves.toBeUndefined();
    await expect(equipmentApi.update("e-1", { name: "Токарный станок", type: "Станок" }))
      .resolves.toBeUndefined();
    await expect(equipmentApi.remove("e-1")).resolves.toBeUndefined();
    await expect(technologyApi.rename("tp-1", "Новое имя")).resolves.toBeUndefined();
    await expect(technologyApi.remove("tp-1")).resolves.toBeUndefined();
    await expect(technologyApi.removeOperation("tp-1", 10)).resolves.toBeUndefined();
    await expect(technologyApi.markDefault("tp-1")).resolves.toBeUndefined();
    await expect(technologyApi.unmarkDefault("tp-1")).resolves.toBeUndefined();

    expect(json).not.toHaveBeenCalled();
  });
});

describe("humanize-ошибки и подписи стандартов", () => {
  it("humanizeDirectoriesError отображает коды ошибок", () => {
    expect(humanizeDirectoriesError(new ApiClientError(409, "duplicate", "x")))
      .toBe("Номер уже занят");
    expect(humanizeDirectoriesError(new ApiClientError(404, "not_found", "x")))
      .toBe("Запись не найдена");
    expect(humanizeDirectoriesError(new ApiClientError(400, "bad_request", "x")))
      .toBe("Некорректный запрос");
    expect(humanizeDirectoriesError(new ApiClientError(500, "unknown", "Сервер упал")))
      .toBe("Сервер упал");
  });

  it("humanizeTechnologyError дополнительно отображает invalid_state", () => {
    expect(humanizeTechnologyError(new ApiClientError(409, "duplicate", "x")))
      .toBe("Номер уже занят");
    expect(humanizeTechnologyError(new ApiClientError(404, "not_found", "x")))
      .toBe("Запись не найдена");
    expect(humanizeTechnologyError(new ApiClientError(400, "bad_request", "x")))
      .toBe("Некорректный запрос");
    expect(humanizeTechnologyError(new ApiClientError(409, "invalid_state", "x")))
      .toBe("Нельзя снять признак „по умолчанию“ с единственного техпроцесса детали");
  });

  it("standardTypeLabel отображает русские подписи", () => {
    expect(standardTypeLabel("GOST")).toBe("ГОСТ");
    expect(standardTypeLabel("DIN")).toBe("DIN");
    expect(standardTypeLabel("ISO")).toBe("ISO");
    expect(standardTypeLabel("OTHER")).toBe("Другое");
  });
});
