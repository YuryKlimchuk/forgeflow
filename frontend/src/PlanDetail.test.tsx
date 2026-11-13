import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PlanDetail from "./PlanDetail";

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
    return Promise.resolve(handler ?? jsonResponse(404, { error: "not_found", message: "not found" }));
  });
  return spy;
}

describe("PlanDetail", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("отображает элементы плана с номером и названием", async () => {
    const ASSEMBLY = {
      id: { value: "a-1" },
      number: "СБ-0001",
      name: "Насос",
      createdAt: "",
      updatedAt: "",
    };
    mockFetch({
      "GET /api/v1/plans/plan-1": jsonResponse(200, {
        id: { value: "plan-1" },
        name: "Декабрь 2027",
        year: 2027,
        month: 12,
        items: [{ assemblyId: { value: "a-1" }, quantity: 3 }],
      }),
      "GET /api/v1/assemblies": jsonResponse(200, [ASSEMBLY]),
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
    });

    render(<PlanDetail id="plan-1" onBack={() => {}} />);

    expect(await screen.findByText("СБ-0001")).toBeInTheDocument();
    expect(screen.getByText("Насос")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("запрашивает и отображает плоскую структуру плана", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1": jsonResponse(200, {
        id: { value: "plan-1" },
        name: "Декабрь 2027",
        year: 2027,
        month: 12,
        items: [],
      }),
      "GET /api/v1/assemblies": jsonResponse(200, []),
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
      "GET /api/v1/plans/plan-1/flat": jsonResponse(200, [
        { number: "Д-0001", name: "Втулка", quantity: 10 },
      ]),
    });

    render(<PlanDetail id="plan-1" onBack={() => {}} />);
    await screen.findByText("План пуст");

    await userEvent.click(screen.getByRole("button", { name: "Плоская структура" }));

    expect(await screen.findByText("Д-0001")).toBeInTheDocument();
    expect(screen.getByText("10")).toBeInTheDocument();
  });

  it("не отображает секцию расчётов при загрузке плана", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");
    fetchMock.mockImplementation((input) => {
      const url = typeof input === "string" ? input : String(input);
      if (url === "/api/v1/plans/plan-1") {
        return new Promise<Response>(() => {});
      }
      return Promise.resolve(jsonResponse(404, { error: "not_found", message: "not found" }));
    });

    render(<PlanDetail id="plan-1" onBack={() => {}} />);

    expect(screen.getByText("Загрузка…")).toBeInTheDocument();
    expect(screen.queryByText("Расчёты потребностей")).not.toBeInTheDocument();
  });

  it("не отображает секцию расчётов при ошибке загрузки плана", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1": jsonResponse(404, { error: "not_found", message: "x" }),
    });

    render(<PlanDetail id="plan-1" onBack={() => {}} />);

    expect(await screen.findByText("Запись не найдена")).toBeInTheDocument();
    expect(screen.queryByText("Расчёты потребностей")).not.toBeInTheDocument();
  });

  it("отображает секцию расчётов после успешной загрузки плана", async () => {
    mockFetch({
      "GET /api/v1/plans/plan-1": jsonResponse(200, {
        id: { value: "plan-1" },
        name: "Декабрь 2027",
        year: 2027,
        month: 12,
        items: [],
      }),
      "GET /api/v1/assemblies": jsonResponse(200, []),
      "GET /api/v1/plans/plan-1/calculations": jsonResponse(200, []),
    });

    render(<PlanDetail id="plan-1" onBack={() => {}} />);

    expect(await screen.findByText("Расчёты потребностей")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Рассчитать потребности" })).toBeInTheDocument();
  });
});
