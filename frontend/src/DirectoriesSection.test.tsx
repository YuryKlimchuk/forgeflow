import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DirectoriesSection from "./DirectoriesSection";

const MATERIAL = {
  id: { value: "m-1" },
  number: "М-0001",
  name: "Сталь",
  standardType: "GOST",
  standard: "ГОСТ 1050-88",
  unit: "кг",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const SORTAMENT = {
  id: { value: "s-1" },
  number: "СОРТ-0001",
  name: "Круг",
  standardType: "DIN",
  standard: "DIN 1013",
  unit: "мм",
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

describe("DirectoriesSection", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает материалы и отображает строки", async () => {
    mockFetch({
      "GET /api/v1/materials": jsonResponse(200, [MATERIAL]),
      "GET /api/v1/sortaments": jsonResponse(200, []),
    });

    render(<DirectoriesSection />);

    const row = (await screen.findByText("М-0001")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("Сталь")).toBeInTheDocument();
    expect(within(row).getByText("ГОСТ")).toBeInTheDocument();
    expect(within(row).getByText("ГОСТ 1050-88")).toBeInTheDocument();
    expect(within(row).getByText("кг")).toBeInTheDocument();
  });

  it("переключает вкладку и загружает сортаменты", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/materials": jsonResponse(200, [MATERIAL]),
      "GET /api/v1/sortaments": jsonResponse(200, [SORTAMENT]),
    });

    render(<DirectoriesSection />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByRole("button", { name: "Сортаменты" }));

    expect(await screen.findByText("СОРТ-0001")).toBeInTheDocument();
    expect(screen.queryByText("М-0001")).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/sortaments");
  });

  it("показывает пустой список и форму создания", async () => {
    mockFetch({ "GET /api/v1/materials": jsonResponse(200, []) });

    render(<DirectoriesSection />);

    expect(await screen.findByText("Пока нет записей. Создайте первую.")).toBeInTheDocument();
    expect(screen.getByLabelText("Номер")).toBeInTheDocument();
    expect(screen.getByLabelText("Наименование")).toBeInTheDocument();
  });

  it("до завершения запроса показывает «Загрузка…»", () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() => new Promise(() => {}));

    render(<DirectoriesSection />);

    expect(screen.getByText("Загрузка…")).toBeInTheDocument();
  });

  it("показывает баннер ошибки при сбое загрузки", async () => {
    mockFetch({
      "GET /api/v1/materials": jsonResponse(400, { error: "bad_request", message: "..." }),
    });

    render(<DirectoriesSection />);

    expect(await screen.findByText("Некорректный запрос")).toBeInTheDocument();
  });

  it("создаёт запись и очищает форму", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/materials": jsonResponse(200, []),
      "POST /api/v1/materials": jsonResponse(201, MATERIAL),
    });

    render(<DirectoriesSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.type(screen.getByLabelText("Номер"), "М-0001");
    await userEvent.type(screen.getByLabelText("Наименование"), "Сталь");
    await userEvent.selectOptions(screen.getByLabelText("Тип стандарта"), "GOST");
    await userEvent.type(screen.getByLabelText("Обозначение стандарта"), "ГОСТ 1050-88");
    await userEvent.type(screen.getByLabelText("Единица измерения"), "кг");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/materials",
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
    expect(screen.getByLabelText("Номер")).toHaveValue("");
  });

  it("блокирует создание с пустыми полями", async () => {
    const fetchMock = mockFetch({ "GET /api/v1/materials": jsonResponse(200, []) });

    render(<DirectoriesSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    const postCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
    expect(postCalls).toHaveLength(0);
  });

  it("показывает ошибку дубликата и не сбрасывает форму", async () => {
    mockFetch({
      "GET /api/v1/materials": jsonResponse(200, []),
      "POST /api/v1/materials": jsonResponse(409, { error: "duplicate", message: "..." }),
    });

    render(<DirectoriesSection />);
    await screen.findByText("Пока нет записей. Создайте первую.");

    await userEvent.type(screen.getByLabelText("Номер"), "М-0001");
    await userEvent.type(screen.getByLabelText("Наименование"), "Сталь");
    await userEvent.selectOptions(screen.getByLabelText("Тип стандарта"), "GOST");
    await userEvent.type(screen.getByLabelText("Обозначение стандарта"), "ГОСТ 1050-88");
    await userEvent.type(screen.getByLabelText("Единица измерения"), "кг");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    expect(await screen.findByText("Номер уже занят")).toBeInTheDocument();
    expect(screen.getByLabelText("Номер")).toHaveValue("М-0001");
  });

  it("открывает деталь по клику на строку", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/materials": jsonResponse(200, [MATERIAL]),
      "GET /api/v1/materials/m-1": jsonResponse(200, MATERIAL),
    });

    render(<DirectoriesSection />);
    await screen.findByText("М-0001");

    await userEvent.click(screen.getByText("М-0001"));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith("/api/v1/materials/m-1"));
    expect(await screen.findByText("Обновлено")).toBeInTheDocument();
  });
});
