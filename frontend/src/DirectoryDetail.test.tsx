import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DirectoryDetail from "./DirectoryDetail";
import { DIRECTORY_TYPES, type CatalogTypeConfig } from "./api";

const TYPE: CatalogTypeConfig = DIRECTORY_TYPES[0];

const ENTRY = {
  id: { value: "m-1" },
  number: "М-0001",
  name: "Сталь",
  standardType: "GOST",
  standard: "ГОСТ 1050-88",
  unit: "кг",
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

describe("DirectoryDetail", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает и отображает все поля записи", async () => {
    mockFetch({ "GET /api/v1/materials/m-1": jsonResponse(200, ENTRY) });

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={() => {}} />);

    expect(await screen.findByText("М-0001")).toBeInTheDocument();
    expect(screen.getByText("Сталь")).toBeInTheDocument();
    expect(screen.getByText("ГОСТ")).toBeInTheDocument();
    expect(screen.getByText("ГОСТ 1050-88")).toBeInTheDocument();
    expect(screen.getByText("кг")).toBeInTheDocument();
    expect(screen.getByText("Создано")).toBeInTheDocument();
    expect(screen.getByText("Обновлено")).toBeInTheDocument();
  });

  it("редактирует изменяемые поля без number", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/materials/m-1": jsonResponse(200, ENTRY),
      "PUT /api/v1/materials/m-1": jsonResponse(204, undefined),
    });

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={() => {}} />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByRole("button", { name: "Редактировать" }));
    await userEvent.clear(screen.getByLabelText("Новое наименование"));
    await userEvent.type(screen.getByLabelText("Новое наименование"), "Сталь 45");
    await userEvent.selectOptions(screen.getByLabelText("Новый тип стандарта"), "DIN");
    await userEvent.clear(screen.getByLabelText("Новое обозначение стандарта"));
    await userEvent.type(screen.getByLabelText("Новое обозначение стандарта"), "DIN 123");
    await userEvent.clear(screen.getByLabelText("Новая единица измерения"));
    await userEvent.type(screen.getByLabelText("Новая единица измерения"), "шт");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/materials/m-1",
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
  });

  it("не сохраняет пустое изменяемое поле", async () => {
    const fetchMock = mockFetch({ "GET /api/v1/materials/m-1": jsonResponse(200, ENTRY) });

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={() => {}} />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByRole("button", { name: "Редактировать" }));
    await userEvent.clear(screen.getByLabelText("Новое наименование"));
    await userEvent.click(screen.getByRole("button", { name: "Сохранить" }));

    const putCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "PUT");
    expect(putCalls).toHaveLength(0);
  });

  it("удаляет с подтверждением и вызывает onBack", async () => {
    const onBack = vi.fn();
    const fetchMock = mockFetch({
      "GET /api/v1/materials/m-1": jsonResponse(200, ENTRY),
      "DELETE /api/v1/materials/m-1": jsonResponse(204, undefined),
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={onBack} />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/materials/m-1",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    expect(onBack).toHaveBeenCalled();
  });

  it("отмена удаления не отправляет DELETE", async () => {
    const fetchMock = mockFetch({ "GET /api/v1/materials/m-1": jsonResponse(200, ENTRY) });
    vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={() => {}} />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    expect(window.confirm).toHaveBeenCalled();
    const deleteCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "DELETE");
    expect(deleteCalls).toHaveLength(0);
  });

  it("показывает баннер ошибки при сбое загрузки", async () => {
    mockFetch({
      "GET /api/v1/materials/m-1": jsonResponse(404, { error: "not_found", message: "..." }),
    });

    render(<DirectoryDetail type={TYPE} id="m-1" onBack={() => {}} />);

    expect(await screen.findByText("Запись не найдена")).toBeInTheDocument();
  });
});
