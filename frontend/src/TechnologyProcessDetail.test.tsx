import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TechnologyProcessDetail from "./TechnologyProcessDetail";

const PART = {
  id: { value: "p-1" },
  number: "Д-0001",
  name: "Втулка",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const EQUIPMENT = {
  id: { value: "e-1" },
  number: "ОБ-0001",
  name: "Токарный станок",
  type: "Станок",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

const PROCESS = {
  id: { value: "tp-1" },
  number: "ТП-0001",
  name: "Токарная обработка",
  partId: { value: "p-1" },
  defaultProcess: false,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  operations: [{ number: 10, name: "Точение", equipmentId: { value: "e-1" } }],
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

describe("TechnologyProcessDetail", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("загружает техпроцесс, деталь и операции с оборудованием", async () => {
    mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);

    expect(await screen.findByText("ТП-0001")).toBeInTheDocument();
    expect(screen.getByText("Токарная обработка")).toBeInTheDocument();
    expect(screen.getByText("Д-0001 — Втулка")).toBeInTheDocument();
    expect(screen.getByLabelText("По умолчанию")).not.toBeChecked();
    expect(screen.getByText("Создано")).toBeInTheDocument();
    expect(screen.getByText("Обновлено")).toBeInTheDocument();

    const row = screen.getByText("Точение").closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("10")).toBeInTheDocument();
    expect(within(row).getByText("Токарный станок")).toBeInTheDocument();
  });

  it("показывает «без оборудования» для операции с equipmentId:null", async () => {
    mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, {
        ...PROCESS,
        operations: [{ number: 10, name: "Точение", equipmentId: null }],
      }),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);

    const row = (await screen.findByText("Точение")).closest("tr") as HTMLTableRowElement;
    expect(within(row).getByText("без оборудования")).toBeInTheDocument();
  });

  it("переименовывает техпроцесс", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "PUT /api/v1/technology-processes/tp-1/name": jsonResponse(204, undefined),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    await userEvent.click(screen.getByRole("button", { name: "Переименовать" }));
    await userEvent.clear(screen.getByLabelText("Новое наименование"));
    await userEvent.type(screen.getByLabelText("Новое наименование"), "Фрезерная обработка");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/name",
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify({ name: "Фрезерная обработка" }),
        }),
      );
    });
  });

  it("удаляет техпроцесс с подтверждением и вызывает onBack", async () => {
    const onBack = vi.fn();
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "DELETE /api/v1/technology-processes/tp-1": jsonResponse(204, undefined),
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<TechnologyProcessDetail id="tp-1" onBack={onBack} />);
    await screen.findByText("Токарная обработка");

    await userEvent.click(screen.getByRole("button", { name: "Удалить процесс" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    expect(onBack).toHaveBeenCalled();
  });

  it("форма операции содержит select с пустой опцией «без оборудования»", async () => {
    mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    const select = screen.getByLabelText("Оборудование");
    expect(within(select).getByRole("option", { name: "без оборудования" })).toHaveValue("");

    const equipmentOption = await screen.findByRole("option", { name: "Токарный станок" });
    expect(equipmentOption).toHaveValue("e-1");
  });

  it("добавляет операцию с оборудованием", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "POST /api/v1/technology-processes/tp-1/operations": jsonResponse(201, {
        number: 20,
        name: "Шлифование",
        equipmentId: { value: "e-1" },
      }),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    await userEvent.type(screen.getByLabelText("Номер операции"), "20");
    await userEvent.type(screen.getByLabelText("Название операции"), "Шлифование");
    await screen.findByRole("option", { name: "Токарный станок" });
    await userEvent.selectOptions(screen.getByLabelText("Оборудование"), "e-1");
    await userEvent.click(screen.getByRole("button", { name: "Добавить операцию" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/operations",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ number: 20, name: "Шлифование", equipmentId: { value: "e-1" } }),
        }),
      );
    });
  });

  it("добавляет операцию без оборудования с equipmentId:null", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "POST /api/v1/technology-processes/tp-1/operations": jsonResponse(201, {
        number: 20,
        name: "Шлифование",
        equipmentId: null,
      }),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    await userEvent.type(screen.getByLabelText("Номер операции"), "20");
    await userEvent.type(screen.getByLabelText("Название операции"), "Шлифование");
    await userEvent.click(screen.getByRole("button", { name: "Добавить операцию" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/operations",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ number: 20, name: "Шлифование", equipmentId: null }),
        }),
      );
    });
  });

  it.each(["", "0", "-5", "1.5"])(
    "блокирует добавление операции с некорректным номером «%s»",
    async (number) => {
      const fetchMock = mockFetch({
        "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
        "GET /api/v1/equipment": jsonResponse(200, []),
        "GET /api/v1/parts": jsonResponse(200, [PART]),
      });

      render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
      await screen.findByText("Токарная обработка");

      if (number) {
        await userEvent.type(screen.getByLabelText("Номер операции"), number);
      }
      await userEvent.type(screen.getByLabelText("Название операции"), "Шлифование");
      await userEvent.click(screen.getByRole("button", { name: "Добавить операцию" }));

      const postCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
      expect(postCalls).toHaveLength(0);
    },
  );

  it("блокирует добавление операции с пустым названием", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    await userEvent.type(screen.getByLabelText("Номер операции"), "10");
    await userEvent.click(screen.getByRole("button", { name: "Добавить операцию" }));

    const postCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === "POST");
    expect(postCalls).toHaveLength(0);
  });

  it("удаляет операцию", async () => {
    const fetchMock = mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, [EQUIPMENT]),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "DELETE /api/v1/technology-processes/tp-1/operations/10": jsonResponse(204, undefined),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Точение");

    await userEvent.click(screen.getByRole("button", { name: "Удалить операцию" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/operations/10",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
  });

  it("устанавливает признак «по умолчанию»", async () => {
    let current = { ...PROCESS, defaultProcess: false };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
      const url = typeof input === "string" ? input : String(input);
      const method = init?.method ?? "GET";
      if (method === "GET" && url === "/api/v1/technology-processes/tp-1") {
        return Promise.resolve(jsonResponse(200, current));
      }
      if (method === "GET" && url === "/api/v1/equipment") {
        return Promise.resolve(jsonResponse(200, []));
      }
      if (method === "GET" && url === "/api/v1/parts") {
        return Promise.resolve(jsonResponse(200, [PART]));
      }
      if (method === "PUT" && url === "/api/v1/technology-processes/tp-1/default") {
        current = { ...current, defaultProcess: true };
        return Promise.resolve(jsonResponse(204, undefined));
      }
      return Promise.resolve(jsonResponse(404, { error: "not_found", message: "not found" }));
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    const checkbox = await screen.findByLabelText("По умолчанию");
    expect(checkbox).not.toBeChecked();

    await userEvent.click(checkbox);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/default",
        expect.objectContaining({ method: "PUT" }),
      );
    });
    await waitFor(() => expect(screen.getByLabelText("По умолчанию")).toBeChecked());
  });

  it("снимает признак «по умолчанию»", async () => {
    let current = { ...PROCESS, defaultProcess: true };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
      const url = typeof input === "string" ? input : String(input);
      const method = init?.method ?? "GET";
      if (method === "GET" && url === "/api/v1/technology-processes/tp-1") {
        return Promise.resolve(jsonResponse(200, current));
      }
      if (method === "GET" && url === "/api/v1/equipment") {
        return Promise.resolve(jsonResponse(200, []));
      }
      if (method === "GET" && url === "/api/v1/parts") {
        return Promise.resolve(jsonResponse(200, [PART]));
      }
      if (method === "DELETE" && url === "/api/v1/technology-processes/tp-1/default") {
        current = { ...current, defaultProcess: false };
        return Promise.resolve(jsonResponse(204, undefined));
      }
      return Promise.resolve(jsonResponse(404, { error: "not_found", message: "not found" }));
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    const checkbox = await screen.findByLabelText("По умолчанию");
    expect(checkbox).toBeChecked();

    await userEvent.click(checkbox);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/default",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    await waitFor(() => expect(screen.getByLabelText("По умолчанию")).not.toBeChecked());
  });

  it("показывает invalid_state при снятии признака с единственного процесса", async () => {
    const current = { ...PROCESS, defaultProcess: true };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
      const url = typeof input === "string" ? input : String(input);
      const method = init?.method ?? "GET";
      if (method === "GET" && url === "/api/v1/technology-processes/tp-1") {
        return Promise.resolve(jsonResponse(200, current));
      }
      if (method === "GET" && url === "/api/v1/equipment") {
        return Promise.resolve(jsonResponse(200, []));
      }
      if (method === "GET" && url === "/api/v1/parts") {
        return Promise.resolve(jsonResponse(200, [PART]));
      }
      if (method === "DELETE" && url === "/api/v1/technology-processes/tp-1/default") {
        return Promise.resolve(
          jsonResponse(409, { error: "invalid_state", message: "..." }),
        );
      }
      return Promise.resolve(jsonResponse(404, { error: "not_found", message: "not found" }));
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    const checkbox = await screen.findByLabelText("По умолчанию");
    expect(checkbox).toBeChecked();

    await userEvent.click(checkbox);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/technology-processes/tp-1/default",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    expect(
      await screen.findByText(
        "Нельзя снять признак „по умолчанию“ с единственного техпроцесса детали",
      ),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("По умолчанию")).toBeChecked());
  });

  it("показывает ошибку дубликата номера операции", async () => {
    mockFetch({
      "GET /api/v1/technology-processes/tp-1": jsonResponse(200, PROCESS),
      "GET /api/v1/equipment": jsonResponse(200, []),
      "GET /api/v1/parts": jsonResponse(200, [PART]),
      "POST /api/v1/technology-processes/tp-1/operations": jsonResponse(409, {
        error: "duplicate",
        message: "...",
      }),
    });

    render(<TechnologyProcessDetail id="tp-1" onBack={() => {}} />);
    await screen.findByText("Токарная обработка");

    await userEvent.type(screen.getByLabelText("Номер операции"), "10");
    await userEvent.type(screen.getByLabelText("Название операции"), "Дубль");
    await userEvent.click(screen.getByRole("button", { name: "Добавить операцию" }));

    expect(await screen.findByText("Номер уже занят")).toBeInTheDocument();
  });
});
