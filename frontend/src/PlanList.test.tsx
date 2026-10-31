import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PlanList from "./PlanList";

const PLAN = {
  id: { value: "plan-1" },
  name: "Декабрь 2027",
  year: 2027,
  month: 12,
  items: [],
};

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: "",
    json: async () => body,
  } as Response;
}

describe("PlanList", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("отображает список планов с периодом", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(200, [PLAN]));

    render(<PlanList onOpen={() => {}} />);

    expect(await screen.findByText("Декабрь 2027")).toBeInTheDocument();
    expect(screen.getByText("декабрь 2027")).toBeInTheDocument();
  });

  it("создаёт план", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(200, []))
      .mockResolvedValueOnce(jsonResponse(201, PLAN))
      .mockResolvedValueOnce(jsonResponse(200, [PLAN]));

    render(<PlanList onOpen={() => {}} />);
    await screen.findByText("Пока нет планов.");

    await userEvent.type(screen.getByLabelText("Имя плана"), "Декабрь 2027");
    await userEvent.clear(screen.getByLabelText("Год"));
    await userEvent.type(screen.getByLabelText("Год"), "2027");
    await userEvent.clear(screen.getByLabelText("Месяц"));
    await userEvent.type(screen.getByLabelText("Месяц"), "12");
    await userEvent.click(screen.getByRole("button", { name: "Создать план" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/plans",
        expect.objectContaining({ method: "POST" }),
      );
    });
  });
});
