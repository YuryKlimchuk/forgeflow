import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SpecificationDetail from "./SpecificationDetail";
import type { CatalogEntry, CatalogItemType } from "./api";

const PART: CatalogEntry = {
  id: { value: "part-1" },
  number: "Д-0001",
  name: "Втулка",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const ROW = { item: { type: "PART", id: "part-1" }, quantity: 3 };

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: "",
    json: async () => body,
  } as Response;
}

const CATALOG_URLS: Record<string, CatalogItemType> = {
  "/api/v1/parts": "PART",
  "/api/v1/assemblies": "ASSEMBLY",
  "/api/v1/buys": "BUY",
  "/api/v1/standarts": "STANDART",
  "/api/v1/vzks": "VZK",
};

function mockFetch(
  catalog: Record<CatalogItemType, CatalogEntry[]>,
  handlers: Record<string, Response>,
) {
  const spy = vi.spyOn(globalThis, "fetch");
  spy.mockImplementation((input, init) => {
    const url = typeof input === "string" ? input : String(input);
    const method = init?.method ?? "GET";
    if (method === "GET" && CATALOG_URLS[url]) {
      return Promise.resolve(jsonResponse(200, catalog[CATALOG_URLS[url]] ?? []));
    }
    const handler = handlers[`${method} ${url}`];
    return Promise.resolve(handler ?? jsonResponse(404, { error: "not_found", message: "not found" }));
  });
  return spy;
}

function defaultCatalog(): Record<CatalogItemType, CatalogEntry[]> {
  return { PART: [PART], ASSEMBLY: [], BUY: [], STANDART: [], VZK: [] };
}

describe("SpecificationDetail", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("отображает строки спецификации с номером и названием", async () => {
    mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, [ROW]),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);

    expect(await screen.findByText("Д-0001")).toBeInTheDocument();
    expect(screen.getByText("Втулка")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.queryByText("part-1")).not.toBeInTheDocument();
  });

  it("показывает пустую спецификацию", async () => {
    mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);

    expect(await screen.findByText("Спецификация пуста")).toBeInTheDocument();
  });

  it("добавляет строку через выпадающий список", async () => {
    const fetchMock = mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
      "POST /api/v1/specifications/A-1/rows": jsonResponse(201, ROW),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Спецификация пуста");

    await userEvent.click(screen.getByRole("button", { name: "Д-0001 — Втулка" }));
    await userEvent.type(screen.getByLabelText("Количество"), "3");
    await userEvent.click(screen.getByRole("button", { name: "Добавить строку" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ item: { type: "PART", id: "part-1" }, quantity: 3 }),
        }),
      );
    });
  });

  it("поиск и переключение типа фильтруют выпадающий список", async () => {
    const BUY: CatalogEntry = {
      id: { value: "buy-1" },
      number: "ПОК-0001",
      name: "Подшипник",
      createdAt: "",
      updatedAt: "",
    };
    mockFetch(
      { PART: [PART], ASSEMBLY: [], BUY: [BUY], STANDART: [], VZK: [] },
      { "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []) },
    );

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Спецификация пуста");

    expect(screen.getByRole("button", { name: "Д-0001 — Втулка" })).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText("Поиск элемента"), "тул");
    expect(screen.getByRole("button", { name: "Д-0001 — Втулка" })).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText("Поиск элемента"));
    await userEvent.type(screen.getByLabelText("Поиск элемента"), "zzz");
    expect(screen.queryByRole("button", { name: "Д-0001 — Втулка" })).not.toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText("Поиск элемента"));
    await userEvent.selectOptions(screen.getByLabelText("Тип элемента"), "BUY");
    expect(screen.getByRole("button", { name: "ПОК-0001 — Подшипник" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Д-0001 — Втулка" })).not.toBeInTheDocument();
  });

  it("изменяет количество строки", async () => {
    const fetchMock = mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, [ROW]),
      "PUT /api/v1/specifications/A-1/rows": jsonResponse(204, undefined),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Д-0001");

    await userEvent.click(screen.getByRole("button", { name: "Изменить" }));
    await userEvent.clear(screen.getByLabelText("Новое количество"));
    await userEvent.type(screen.getByLabelText("Новое количество"), "9");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить количество" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({ method: "PUT" }),
      );
    });
  });

  it("удаляет строку", async () => {
    const fetchMock = mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, [ROW]),
      "DELETE /api/v1/specifications/A-1/rows": jsonResponse(204, undefined),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Д-0001");

    await userEvent.click(screen.getByRole("button", { name: "Удалить строку" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
  });

  it("копирует спецификацию", async () => {
    const fetchMock = mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
      "POST /api/v1/specifications/A-1/copy": jsonResponse(201, {
        id: { value: "2" },
        number: "СБ-0002",
        name: "Новая",
        createdAt: "",
        updatedAt: "",
      }),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Спецификация пуста");

    await userEvent.type(screen.getByLabelText("Номер сборки"), "СБ-0002");
    await userEvent.type(screen.getByLabelText("Имя сборки"), "Новая");
    await userEvent.click(screen.getByRole("button", { name: "Скопировать" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/copy",
        expect.objectContaining({ method: "POST" }),
      );
    });
    expect(await screen.findByText(/Создана сборка: СБ-0002/)).toBeInTheDocument();
  });

  it("запрашивает и отображает плоскую структуру", async () => {
    const fetchMock = mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
      "GET /api/v1/specifications/A-1/flat": jsonResponse(200, [
        { number: "Д-0001", name: "Втулка", quantity: 11 },
      ]),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Спецификация пуста");

    await userEvent.click(screen.getByRole("button", { name: "Плоская структура" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith("/api/v1/specifications/A-1/flat");
    });
    expect(await screen.findByText("Д-0001")).toBeInTheDocument();
  });

  it("показывает ошибку цикла без частичного списка", async () => {
    mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
      "GET /api/v1/specifications/A-1/flat": jsonResponse(409, { error: "bom_cycle", message: "..." }),
    });

    render(<SpecificationDetail assemblyId="A-1" onBack={() => {}} />);
    await screen.findByText("Спецификация пуста");

    await userEvent.click(screen.getByRole("button", { name: "Плоская структура" }));

    expect(await screen.findByText(/обнаружен цикл/)).toBeInTheDocument();
  });

  it("вызывает onBack", async () => {
    mockFetch(defaultCatalog(), {
      "GET /api/v1/specifications/A-1/rows": jsonResponse(200, []),
    });
    const onBack = vi.fn();

    render(<SpecificationDetail assemblyId="A-1" onBack={onBack} />);
    await screen.findByText("Спецификация пуста");

    await userEvent.click(screen.getByRole("button", { name: "Назад" }));

    expect(onBack).toHaveBeenCalled();
  });
});
