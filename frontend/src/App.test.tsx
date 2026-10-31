import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 404 ? "Not Found" : "",
    json: async () => body,
  } as Response;
}

const ENTRY = {
  id: { value: "1" },
  number: "P-1",
  name: "Деталь",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const TAB_LABELS = ["Детали", "Сборки", "Покупные", "Стандартные", "ВЗК"];

describe("App", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("отображает вкладки всех пяти типов", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    render(<App />);

    for (const label of TAB_LABELS) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
  });

  it("при старте выбирает «Детали» и загружает /api/v1/parts", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    render(<App />);

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/parts"));
    expect(screen.getByRole("button", { name: "Детали" })).toHaveClass("tab-active");
  });

  it("переключение вкладки загружает соответствующий endpoint", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    render(<App />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/parts"));

    await userEvent.click(screen.getByRole("button", { name: "Покупные" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/buys"));
  });

  it("создаёт запись и очищает форму", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(201, ENTRY))
      .mockResolvedValueOnce(jsonResponse(200, [ENTRY]));

    render(<App />);
    await screen.findByText(/Пока нет записей/);

    await userEvent.type(screen.getByLabelText("Номер"), "P-1");
    await userEvent.type(screen.getByLabelText("Название"), "Деталь");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    await waitFor(() => {
      expect(screen.getByLabelText("Номер")).toHaveValue("");
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/parts",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("показывает ошибку при дубликате номера (409)", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(
        jsonResponse(409, { error: "duplicate", message: "Part number already exists: P-1" }),
      );

    render(<App />);
    await screen.findByText(/Пока нет записей/);

    await userEvent.type(screen.getByLabelText("Номер"), "P-1");
    await userEvent.type(screen.getByLabelText("Название"), "Деталь");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    expect(await screen.findByText(/Номер уже занят/)).toBeInTheDocument();
  });

  it("открывает деталь по клику на строку", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ENTRY]))
      .mockResolvedValueOnce(jsonResponse(200, ENTRY));

    render(<App />);
    await screen.findByText("P-1");

    await userEvent.click(screen.getByText("P-1"));

    expect(await screen.findByText("Обновлено")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/parts/1");
  });

  it("переименовывает на детальной странице", async () => {
    const renamed = { ...ENTRY, name: "Новая деталь" };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ENTRY]))
      .mockResolvedValueOnce(jsonResponse(200, ENTRY))
      .mockResolvedValueOnce(jsonResponse(204, undefined))
      .mockResolvedValueOnce(jsonResponse(200, renamed));

    render(<App />);
    await screen.findByText("P-1");
    await userEvent.click(screen.getByText("P-1"));
    await screen.findByText("Обновлено");

    await userEvent.click(screen.getByRole("button", { name: "Переименовать" }));
    await userEvent.clear(screen.getByLabelText("Новое название"));
    await userEvent.type(screen.getByLabelText("Новое название"), "Новая деталь");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/parts/1/name",
        expect.objectContaining({ method: "PUT" }),
      );
    });
    expect(await screen.findByText("Новая деталь")).toBeInTheDocument();
  });

  it("удаляет с подтверждением и возвращается к списку", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ENTRY]))
      .mockResolvedValueOnce(jsonResponse(200, ENTRY))
      .mockResolvedValueOnce(jsonResponse(204, undefined))
      .mockResolvedValueOnce(jsonResponse(200, []));
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<App />);
    await screen.findByText("P-1");
    await userEvent.click(screen.getByText("P-1"));
    await screen.findByText("Обновлено");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/parts/1",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    expect(await screen.findByText(/Пока нет записей/)).toBeInTheDocument();
  });

  it("отмена удаления не отправляет DELETE", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ENTRY]))
      .mockResolvedValueOnce(jsonResponse(200, ENTRY));
    vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<App />);
    await screen.findByText("P-1");
    await userEvent.click(screen.getByText("P-1"));
    await screen.findByText("Обновлено");

    await userEvent.click(screen.getByRole("button", { name: "Удалить" }));

    expect(window.confirm).toHaveBeenCalled();
    const deleteCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "DELETE");
    expect(deleteCalls).toHaveLength(0);
  });

  it("открывает состав из детали сборки", async () => {
    const ASSEMBLY = {
      id: { value: "10" },
      number: "СБ-1",
      name: "Узел",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(200, [ASSEMBLY]))
      .mockResolvedValueOnce(jsonResponse(200, ASSEMBLY))
      .mockResolvedValueOnce(jsonResponse(200, []));

    render(<App />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/parts"));

    await userEvent.click(screen.getByRole("button", { name: "Сборки" }));
    await screen.findByText("СБ-1");
    await userEvent.click(screen.getByText("СБ-1"));
    await screen.findByText("Обновлено");

    await userEvent.click(screen.getByRole("button", { name: "Состав" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith("/api/v1/specifications/10/rows");
    });
    expect(await screen.findByText("Спецификация")).toBeInTheDocument();
  });

  it("фильтрует список по номеру или названию", async () => {
    const A = {
      id: { value: "1" },
      number: "P-100",
      name: "Втулка",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    const B = {
      id: { value: "2" },
      number: "Д-0002",
      name: "Вал",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, [A, B]));

    render(<App />);
    await screen.findByText("P-100");

    await userEvent.type(screen.getByLabelText("Поиск каталога"), "p-1");
    expect(screen.getByText("P-100")).toBeInTheDocument();
    expect(screen.queryByText("Д-0002")).not.toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText("Поиск каталога"));
    await userEvent.type(screen.getByLabelText("Поиск каталога"), "вал");
    expect(screen.queryByText("P-100")).not.toBeInTheDocument();
    expect(screen.getByText("Д-0002")).toBeInTheDocument();
  });

  it("показывает сообщение при отсутствии совпадений", async () => {
    const A = {
      id: { value: "1" },
      number: "P-100",
      name: "Втулка",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, [A]));

    render(<App />);
    await screen.findByText("P-100");

    await userEvent.type(screen.getByLabelText("Поиск каталога"), "zzz");

    expect(await screen.findByText("Ничего не найдено.")).toBeInTheDocument();
    expect(screen.queryByText("P-100")).not.toBeInTheDocument();
  });
});
