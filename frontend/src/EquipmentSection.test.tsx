import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EquipmentSection from "./EquipmentSection";

const EQUIPMENT = {
  id: { value: "e-1" },
  number: "ОБ-0001",
  name: "Токарный станок",
  type: "Станок",
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

describe("EquipmentSection", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает оборудование и отображает строки", async () => {
    mockFetch({ "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]) });

    render(<EquipmentSection />);

    expect(await screen.findByText("ОБ-0001")).toBeInTheDocument();
    expect(screen.getByText("Токарный станок")).toBeInTheDocument();
    expect(screen.getByText("Станок")).toBeInTheDocument();
  });

  it("создаёт оборудование и очищает форму", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/equipment": jsonResponse(200, []),
      "POST /api/v1/equipment": jsonResponse(201, EQUIPMENT),
    });

    render(<EquipmentSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.type(screen.getByLabelText("Номер"), "ОБ-0001");
    await userEvent.type(screen.getByLabelText("Наименование"), "Токарный станок");
    await userEvent.type(screen.getByLabelText("Тип"), "Станок");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/equipment",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ number: "ОБ-0001", name: "Токарный станок", type: "Станок" }),
        }),
      );
    });
    expect(screen.getByLabelText("Номер")).toHaveValue("");
  });

  it("блокирует создание с пустыми полями", async () => {
    const fetchMock = mockFetch({ "GET /api/v1/equipment": jsonResponse(200, []) });

    render(<EquipmentSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    const postCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
    expect(postCalls).toHaveLength(0);
  });

  it("открывает деталь по клику на строку", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]),
      "GET /api/v1/equipment/e-1": jsonResponse(200, EQUIPMENT),
    });

    render(<EquipmentSection />);
    await screen.findByText("ОБ-0001");

    await userEvent.click(screen.getByText("ОБ-0001"));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/equipment/e-1"));
    expect(await screen.findByText("Обновлено")).toBeInTheDocument();
  });

  it("показывает ошибку дубликата номера", async () => {
    mockFetch({
      "GET /api/v1/equipment": jsonResponse(200, []),
      "POST /api/v1/equipment": jsonResponse(409, { error: "duplicate", message: "..." }),
    });

    render(<EquipmentSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.type(screen.getByLabelText("Номер"), "ОБ-0001");
    await userEvent.type(screen.getByLabelText("Наименование"), "Токарный станок");
    await userEvent.type(screen.getByLabelText("Тип"), "Станок");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    expect(await screen.findByText("Номер уже занят")).toBeInTheDocument();
  });
});
