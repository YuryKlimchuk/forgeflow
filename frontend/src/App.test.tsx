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

const PART = {
  id: { value: "1" },
  number: "P-1",
  name: "Деталь",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("App", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("отображает список деталей", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, [PART]));

    render(<App />);

    expect(await screen.findByText("P-1")).toBeInTheDocument();
    expect(screen.getByText("Деталь")).toBeInTheDocument();
  });

  it("создаёт деталь и очищает форму", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(201, PART))
      .mockResolvedValueOnce(jsonResponse(200, [PART]));

    render(<App />);
    await screen.findByText(/Пока нет деталей/);

    await userEvent.type(screen.getByLabelText("Номер"), "P-1");
    await userEvent.type(screen.getByLabelText("Название"), "Деталь");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    await waitFor(() => {
      expect(screen.getByLabelText("Номер")).toHaveValue("");
    });

    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(true);
  });

  it("показывает ошибку при дубликате номера (409)", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(
        jsonResponse(409, { error: "duplicate", message: "Part number already exists: P-1" }),
      );

    render(<App />);
    await screen.findByText(/Пока нет деталей/);

    await userEvent.type(screen.getByLabelText("Номер"), "P-1");
    await userEvent.type(screen.getByLabelText("Название"), "Деталь");
    await userEvent.click(screen.getByRole("button", { name: "Создать" }));

    expect(await screen.findByText(/Номер уже занят/)).toBeInTheDocument();
  });
});
