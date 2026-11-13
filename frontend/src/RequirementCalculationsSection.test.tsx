import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RequirementCalculationsSection from "./RequirementCalculationsSection";
import type { CatalogEntry } from "./api";

const ASSEMBLY: CatalogEntry = {
  id: { value: "a-1" },
  number: "СБ-0001",
  name: "Насос",
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

function mockFetch(handlers: Record<string, Response>) {
  const spy = vi.spyOn(globalThis, "fetch");
  spy.mockImplementation((input, init) => {
    const url = typeof input === "string" ? input : String(input);
    const method = init?.method ?? "GET";
    const handler = handlers[`${method} ${url}`];
    return Promise.resolve(
      handler ?? jsonResponse(404, { error: "not_found", message: "not found" }),
    );
  });
  return spy;
}

const CALCULATION = {
  id: { value: "calc-1" },
  planId: { value: "plan-1" },
  executedAt: "2027-12-01T10:00:00Z",
  items: [
    {
      type: "ASSEMBLY",
      id: "item-1",
      number: "СБ-0001",
      name: "Насос",
      quantity: 2,
      needType: "MANUFACTURE",
      unit: "шт",
      sources: [{ value: "a-1" }],
    },
    {
      type: "PART",
      id: "item-2",
      number: "Д-0001",
      name: "Втулка",
      quantity: 4,
      needType: "PURCHASE",
      unit: "шт",
      sources: [{ value: "a-1" }],
    },
  ],
};

describe("RequirementCalculationsSection", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("запускает расчёт и отображает таблицу со всеми полями и русскими подписями", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, CALCULATION),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    const row = (await screen.findByText("СБ-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("Сборка")).toBeInTheDocument();
    expect(within(row).getByText("Насос")).toBeInTheDocument();
    expect(within(row).getByText("2")).toBeInTheDocument();
    expect(within(row).getByText("изготовить")).toBeInTheDocument();
    expect(within(row).getByText("шт")).toBeInTheDocument();
    expect(within(row).getByText("СБ-0001 — Насос")).toBeInTheDocument();

    const partRow = (screen.getByText("Д-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(partRow).getByText("Деталь")).toBeInTheDocument();
    expect(within(partRow).getByText("Втулка")).toBeInTheDocument();
    expect(within(partRow).getByText("4")).toBeInTheDocument();
    expect(within(partRow).getByText("закупить")).toBeInTheDocument();
    expect(within(partRow).getByText("шт")).toBeInTheDocument();
  });

  it("раскрывает таблицу позиций выбранного расчёта из истории", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, [CALCULATION]),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);

    const history = await screen.findByRole("list", { name: "История расчётов" });
    await userEvent.click(within(history).getByRole("button"));

    const table = await screen.findByRole("table");
    expect(
      within(table).getAllByRole("columnheader").map((header) => header.textContent),
    ).toEqual([
      "Тип",
      "Номер",
      "Наименование",
      "Количество",
      "Вид потребности",
      "Единица",
      "Источники",
    ]);

    const rows = within(table).getAllByRole("row").slice(1);
    expect(rows).toHaveLength(2);

    const assemblyRow = rows[0];
    expect(within(assemblyRow).getByText("Сборка")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("СБ-0001")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("Насос")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("2")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("изготовить")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("шт")).toBeInTheDocument();
    expect(within(assemblyRow).getByText("СБ-0001 — Насос")).toBeInTheDocument();

    const partRow = rows[1];
    expect(within(partRow).getByText("Деталь")).toBeInTheDocument();
    expect(within(partRow).getByText("Д-0001")).toBeInTheDocument();
    expect(within(partRow).getByText("Втулка")).toBeInTheDocument();
    expect(within(partRow).getByText("4")).toBeInTheDocument();
    expect(within(partRow).getByText("закупить")).toBeInTheDocument();
    expect(within(partRow).getByText("шт")).toBeInTheDocument();
    expect(within(partRow).getByText("СБ-0001 — Насос")).toBeInTheDocument();
  });

  it("загружает историю в порядке из API и добавляет новый расчёт в начало без перезаписи", async () => {
    const older = {
      ...CALCULATION,
      id: { value: "calc-old" },
      executedAt: "2027-12-01T10:00:00Z",
      items: [],
    };
    const newer = {
      ...CALCULATION,
      id: { value: "calc-new" },
      executedAt: "2027-12-02T10:00:00Z",
      items: [],
    };
    const fresh = {
      ...CALCULATION,
      id: { value: "calc-fresh" },
      executedAt: "2027-12-03T10:00:00Z",
      items: [],
    };
    const fetchMock = mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, [newer, older]),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, fresh),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);

    const list = await screen.findByRole("list", { name: "История расчётов" });
    expect(within(list).getAllByRole("button").map((button) => button.textContent)).toEqual([
      new Date("2027-12-02T10:00:00Z").toLocaleString("ru-RU"),
      new Date("2027-12-01T10:00:00Z").toLocaleString("ru-RU"),
    ]);

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));
    await screen.findByText("Позиции отсутствуют");

    const listAfter = screen.getByRole("list", { name: "История расчётов" });
    expect(within(listAfter).getAllByRole("button").map((button) => button.textContent)).toEqual([
      new Date("2027-12-03T10:00:00Z").toLocaleString("ru-RU"),
      new Date("2027-12-02T10:00:00Z").toLocaleString("ru-RU"),
      new Date("2027-12-01T10:00:00Z").toLocaleString("ru-RU"),
    ]);

    const listCalls = fetchMock.mock.calls.filter(([input, init]) => {
      const url = typeof input === "string" ? input : String(input);
      return url === "/api/v1/plans/plan-1/calculations" && !init?.method;
    });
    expect(listCalls).toHaveLength(1);
  });

  it("показывает «Позиции отсутствуют» и не рендерит таблицу для пустого результата", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, {
        ...CALCULATION,
        items: [],
      }),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    expect(await screen.findByText("Позиции отсутствуют")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it.each([
    { status: 409, code: "bom_cycle", expected: "В спецификации обнаружен цикл" },
    {
      status: 400,
      code: "quantity_overflow",
      expected: "Количество позиции превысило допустимое значение",
    },
    { status: 404, code: "not_found", expected: "Запись не найдена" },
    {
      status: 404,
      code: "specification_item_not_found",
      expected: "В спецификации отсутствует сборка: a-1",
    },
  ])("показывает баннер ошибки запуска $code и не меняет историю", async ({ status, code, expected }) => {
    const body =
      code === "specification_item_not_found"
        ? { error: code, message: "x", itemType: "ASSEMBLY", itemId: "a-1" }
        : { error: code, message: "x" };
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(status, body),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    expect(await screen.findByText(expected)).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(screen.queryByText("Позиции отсутствуют")).not.toBeInTheDocument();
    expect(screen.getByText("История расчётов пуста")).toBeInTheDocument();
  });

  it("показывает «Запись не найдена» при ошибке чтения истории и оставляет кнопку доступной", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(404, {
        error: "not_found",
        message: "x",
      }),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, CALCULATION),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);

    expect(await screen.findByText("Запись не найдена")).toBeInTheDocument();
    const button = screen.getByRole("button", { name: "Рассчитать потребности" });
    expect(button).toBeEnabled();

    await userEvent.click(button);

    expect(await screen.findByText("СБ-0001")).toBeInTheDocument();
  });

  it("блокирует кнопку во время запроса и не допускает повторную отправку", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");
    fetchMock.mockImplementation((input, init) => {
      const url = typeof input === "string" ? input : String(input);
      const method = init?.method ?? "GET";
      if (method === "POST" && url === "/api/v1/plans/plan-1/calculations") {
        return new Promise<Response>(() => {});
      }
      return Promise.resolve(
        url === "/api/v1/plans/plan-1/calculations"
          ? jsonResponse(200, [])
          : jsonResponse(404, { error: "not_found", message: "not found" }),
      );
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    const runningButton = screen.getByRole("button", { name: "Расчёт…" });
    expect(runningButton).toBeDisabled();

    await userEvent.click(runningButton);

    const postCalls = fetchMock.mock.calls.filter(
      ([input, init]) =>
        (typeof input === "string" ? input : String(input)) ===
          "/api/v1/plans/plan-1/calculations" && init?.method === "POST",
    );
    expect(postCalls).toHaveLength(1);
  });

  it("разрешает источники в номер и название сборки и показывает плейсхолдер для неразрешимого", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, {
        ...CALCULATION,
        items: [
          {
            type: "PART",
            id: "item-2",
            number: "Д-0001",
            name: "Втулка",
            quantity: 4,
            needType: "PURCHASE",
            unit: "шт",
            sources: [{ value: "a-1" }, { value: "missing" }],
          },
        ],
      }),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    const row = (await screen.findByText("Д-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("СБ-0001 — Насос, Сборка не найдена")).toBeInTheDocument();
  });

  it("сохраняет порядок позиций из API", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "POST /api/v1/plans/plan-1/calculations": jsonResponse(201, {
        ...CALCULATION,
        items: [
          {
            type: "ASSEMBLY",
            id: "item-1",
            number: "СБ-0001",
            name: "Насос",
            quantity: 2,
            needType: "MANUFACTURE",
            unit: "шт",
            sources: [{ value: "a-1" }],
          },
          {
            type: "PART",
            id: "item-2",
            number: "Д-0001",
            name: "Втулка",
            quantity: 4,
            needType: "PURCHASE",
            unit: "шт",
            sources: [{ value: "a-1" }],
          },
        ],
      }),
    });

    render(<RequirementCalculationsSection planId="plan-1" assemblies={[ASSEMBLY]} />);
    await screen.findByText("История расчётов пуста");

    await userEvent.click(screen.getByRole("button", { name: "Рассчитать потребности" }));

    const table = await screen.findByRole("table");
    const rows = within(table).getAllByRole("row").slice(1);
    expect(rows.map((row) => within(row).getAllByRole("cell")[1].textContent)).toEqual([
      "СБ-0001",
      "Д-0001",
    ]);
  });
});
