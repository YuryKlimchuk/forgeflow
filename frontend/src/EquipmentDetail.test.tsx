import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EquipmentDetail from "./EquipmentDetail";

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

describe("EquipmentDetail", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает и отображает все поля записи", async () => {
    mockFetch({ "GET /api/v1/equipment/e-1": jsonResponse(200, EQUIPMENT) });

    render(<EquipmentDetail id="e-1" onBack={() => {}} />);

    expect(await screen.findByText("ОБ-0001")).toBeInTheDocument();
    expect(screen.getByText("Токарный станок")).toBeInTheDocument();
    expect(screen.getByText("Станок")).toBeInTheDocument();
    expect(screen.getByText("Создано")).toBeInTheDocument();
    expect(screen.getByText("Обновлено")).toBeInTheDocument();
  });

  it("редактирует name/type без number", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/equipment/e-1": jsonResponse(200, EQUIPMENT),
      "PUT /api/v1/equipment/e-1": jsonResponse(204, undefined),
    });

    render(<EquipmentDetail id="e-1" onBack={() => {}} />);
    await screen.findByText("ОБ-0001");

    await userEvent.click(screen.getByRole("button", { name: "Редактировать" }));
    await userEvent.clear(screen.getByLabelText("Новое наименование"));
    await userEvent.type(screen.getByLabelText("Новое наименование"), "Фрезерный станок");
    await userEvent.clear(screen.getByLabelText("Новый тип"));
    await userEvent.type(screen.getByLabelText("Новый тип"), "Оборудование");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/equipment/e-1",
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify({ name: "Фрезерный станок", type: "Оборудование" }),
        }),
      );
    });
  });

  it("удаляет с подтверждением и вызывает onBack", async () => {
    const onBack = vi.fn();
    const fetchMock = mockFetch({
      "GET /api/v1/equipment/e-1": jsonResponse(200, EQUIPMENT),
      "DELETE /api/v1/equipment/e-1": jsonResponse(204, undefined),
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<EquipmentDetail id="e-1" onBack={onBack} />);
    await screen.findByText("ОБ-0001");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/equipment/e-1",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    expect(onBack).toHaveBeenCalled();
  });

  it("отмена удаления не отправляет DELETE", async () => {
    const fetchMock = mockFetch({ "GET /api/v1/equipment/e-1": jsonResponse(200, EQUIPMENT) });
    vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<EquipmentDetail id="e-1" onBack={() => {}} />);
    await screen.findByText("ОБ-0001");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    expect(window.confirm).toHaveBeenCalled();
    const deleteCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "DELETE");
    expect(deleteCalls).toHaveLength(0);
  });
});
