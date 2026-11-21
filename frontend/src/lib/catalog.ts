import { ApiError, apiFetch } from "@/lib/api-client";
import type {
  Category,
  PageResponse,
  ProductDetail,
  ProductSummary,
} from "@/lib/catalog-types";

/** Résultat tolérant aux pannes : la vitrine reste consultable si l'API est hors ligne. */
export type CatalogResult<T> =
  | { status: "ok"; data: T }
  | { status: "not-found" }
  | { status: "unavailable" };

/** Une liste ne peut pas être « introuvable » : seulement disponible ou non. */
export type ProductListResult<T> = { status: "ok"; data: T } | { status: "unavailable" };

const REQUEST_TIMEOUT_MS = 5000;

type ProductQuery = {
  category?: string;
  q?: string;
  featured?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export async function getCategories(): Promise<Category[]> {
  try {
    return await apiFetch<Category[]>("/api/v1/categories", {
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
      cache: "no-store",
    });
  } catch {
    return [];
  }
}

export async function getProducts(query: ProductQuery): Promise<ProductListResult<PageResponse<ProductSummary>>> {
  const params = new URLSearchParams();
  if (query.category) params.set("category", query.category);
  if (query.q) params.set("q", query.q);
  if (query.featured !== undefined) params.set("featured", String(query.featured));
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 12));
  if (query.sort) params.set("sort", query.sort);

  try {
    const data = await apiFetch<PageResponse<ProductSummary>>(
      `/api/v1/products?${params.toString()}`,
      { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS), cache: "no-store" },
    );
    return { status: "ok", data };
  } catch {
    return { status: "unavailable" };
  }
}

export async function getProduct(slug: string): Promise<CatalogResult<ProductDetail>> {
  try {
    const data = await apiFetch<ProductDetail>(
      `/api/v1/products/${encodeURIComponent(slug)}`,
      { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS), cache: "no-store" },
    );
    return { status: "ok", data };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return { status: "not-found" };
    }
    return { status: "unavailable" };
  }
}
