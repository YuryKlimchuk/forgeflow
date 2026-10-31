import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SpecificationPanel from "./SpecificationPanel";

const ROW_ID = "11111111-1111-1111-1111-111111111111";
const ROW = { item: { type: "PART", id: ROW_ID }, quantity: 3 };

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: "",
    json: async () => body,
  } as Response;
}

async function openSpec() {
  await userEvent.type(screen.getByLabelText("ID сборки"), "A-1");
  await userEvent.click(screen.getByRole("button", { name: "Открыть" }));
}

describe("SpecificationPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("открывает спецификацию и отображает строки", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, [ROW]));

    render(<SpecificationPanel />);
    await openSpec();

    expect(await screen.findByText(ROW_ID)).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("показывает пустую спецификацию", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, []));

    render(<SpecificationPanel />);
    await openSpec();

    expect(await screen.findByText("Спецификация пуста")).toBeInTheDocument();
  });

  it("добавляет строку", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(201, ROW))
      .mockResolvedValueOnce(jsonResponse(200, [ROW]));

    render(<SpecificationPanel />);
    await openSpec();
    await screen.findByText("Спецификация пуста");

    await userEvent.type(screen.getByLabelText("ID элемента"), ROW_ID);
    await userEvent.type(screen.getByLabelText("Количество"), "3");
    await userEvent.click(screen.getByRole("button", { name: "Добавить строку" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({ method: "POST" }),
      );
    });
  });

  it("изменяет количество строки", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ROW]))
      .mockResolvedValueOnce(jsonResponse(204, undefined))
      .mockResolvedValueOnce(jsonResponse(200, [{ ...ROW, quantity: 9 }]));

    render(<SpecificationPanel />);
    await openSpec();
    await screen.findByText(ROW_ID);

    await userEvent.click(screen.getByRole("button", { name: "Изменить" }));
    await userEvent.clear(screen.getByLabelText("Новое количество"));
    await userEvent.type(screen.getByLabelText("Новое количество"), "9");
    await userEvent.click(screen.getByRole("button", { name: "Сохранить количество" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({ method: "PUT" }),
      );
    });
  });

  it("удаляет строку", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, [ROW]))
      .mockResolvedValueOnce(jsonResponse(204, undefined))
      .mockResolvedValueOnce(jsonResponse(200, []));

    render(<SpecificationPanel />);
    await openSpec();
    await screen.findByText(ROW_ID);

    await userEvent.click(screen.getByRole("button", { name: "Удалить строку" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/rows",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
  });

  it("копирует спецификацию", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(
        jsonResponse(201, {
          id: { value: "2" },
          number: "B-1",
          name: "Новая",
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        }),
      );

    render(<SpecificationPanel />);
    await openSpec();
    await screen.findByText("Спецификация пуста");

    await userEvent.type(screen.getByLabelText("Номер сборки"), "B-1");
    await userEvent.type(screen.getByLabelText("Имя сборки"), "Новая");
    await userEvent.click(screen.getByRole("button", { name: "Скопировать" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/specifications/A-1/copy",
        expect.objectContaining({ method: "POST" }),
      );
    });
    expect(await screen.findByText(/Создана сборка: B-1/)).toBeInTheDocument();
  });

  it("показывает ошибку дубликата (409)", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(409, { error: "duplicate", message: "..." }));

    render(<SpecificationPanel />);
    await openSpec();
    await screen.findByText("Спецификация пуста");

    await userEvent.type(screen.getByLabelText("ID элемента"), ROW_ID);
    await userEvent.type(screen.getByLabelText("Количество"), "3");
    await userEvent.click(screen.getByRole("button", { name: "Добавить строку" }));

    expect(await screen.findByText(/Элемент уже присутствует/)).toBeInTheDocument();
  });
});
