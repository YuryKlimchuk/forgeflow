import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TechnologyProcessSection from "./TechnologyProcessSection";

const PART = {
  id: { value: "p-1" },
  number: "Д-0001",
  name: "Втулка",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const PROCESS = {
  id: { value: "tp-1" },
  number: "ТП-0001",
  name: "Токарная обработка",
  partId: { value: "p-1" },
  defaultProcess: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  operations: [],
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

describe("TechnologyProcessSection", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает техпроцессы и разрешает имя детали", async () => {
    mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(200, [PROCESS]),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessSection />);

    const row = (await screen.findByText("ТП-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("Токарная обработка")).toBeInTheDocument();
    expect(within(row).getByText("Д-0001 — Втулка")).toBeInTheDocument();
    expect(within(row).getByText("Да")).toBeInTheDocument();
  });

  it("показывает «Элемент не найден» без раскрытия UUID", async () => {
    mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(200, [
        { ...PROCESS, partId: { value: "missing-id" } },
      ]),
      "GET /api/v1/parts": jsonResponse(200, []),
    });

    render(<TechnologyProcessSection />);

    const row = (await screen.findByText("ТП-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("Элемент не найден")).toBeInTheDocument();
    expect(screen.queryByText("missing-id")).not.toBeInTheDocument();
  });

  it("создаёт техпроцесс с выбранной деталью", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "POST /api/v1/technology-processes": jsonResponse(201, PROCESS),
    });

    render(<TechnologyProcessSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.click(screen.getByRole("button", { name: "Д-0001 — Втулка" }));
    await userEvent.type(screen.getByLabelText("Номер"), "ТП-0001");
    await userEvent.type(screen.getByLabelText("Наименование"), "Токарная обработка");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    await waitFor(() => {
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
  });

  it("блокирует создание без детали или с пустыми полями", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.click(screen.getByRole("button", { name: "Д-0001 — Втулка" }));
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    const postCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
    expect(postCalls).toHaveLength(0);
  });

  it("показывает баннер ошибки при сбое загрузки списка", async () => {
    mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(400, {
        error: "bad_request",
        message: "...",
      }),
      "GET /api/v1/parts": jsonResponse(200, []),
    });

    render(<TechnologyProcessSection />);

    expect(await screen.findByText("Некорректный запрос")).toBeInTheDocument();
  });

  it("показывает баннер ошибки при сбое загрузки деталей", async () => {
    mockFetch({
      "GET /api/v1/technology-processes": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(500, { error: "unknown", message: "..." }),
    });

    render(<TechnologyProcessSection />);

    expect(await screen.findByText("...")).toBeInTheDocument();
  });
});
