const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Erreur renvoyée par l'API au format ProblemDetail (RFC 9457). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly title: string,
    readonly detail?: string,
  ) {
    super(detail ?? title);
    this.name = "ApiError";
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  const problem = await response.json().catch(() => null);
  return new ApiError(
    response.status,
    problem?.title ?? response.statusText,
    problem?.detail,
  );
}

/** Variante qui expose les en-têtes (le panier invité renvoie X-Cart-Token). */
export async function apiFetchResponse<T>(
  path: string,
  init?: RequestInit,
): Promise<{ data: T; headers: Headers }> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: { Accept: "application/json", ...init?.headers },
  });

  if (!response.ok) {
    throw await toApiError(response);
  }

  return { data: (await response.json()) as T, headers: response.headers };
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const { data } = await apiFetchResponse<T>(path, init);
  return data;
}

/** Pour les opérations qui répondent 204 sans corps. */
export async function apiFetchVoid(path: string, init?: RequestInit): Promise<void> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: { Accept: "application/json", ...init?.headers },
  });

  if (!response.ok) {
    throw await toApiError(response);
  }
}
