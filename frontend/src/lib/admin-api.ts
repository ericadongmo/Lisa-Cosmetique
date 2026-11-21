import { apiFetch } from "@/lib/api-client";
import { apiFetchVoid } from "@/lib/api-client";
import type {
  AdminProductImage,
  AdminProductSummary,
  AdminOrderDetail,
  AdminOrderStatus,
  AdminOrderSummary,
  AdminPage,
  UpdateOrderStatusPayload,
} from "@/lib/admin-types";

export type AdminCredentials = {
  /** Valeur complète de l'en-tête Authorization (Basic). */
  header: string;
  email: string;
};

function jsonHeaders(auth: AdminCredentials): HeadersInit {
  return {
    Authorization: auth.header,
    "Content-Type": "application/json",
  };
}

export async function getAdminOrders(
  auth: AdminCredentials,
  filters: { status?: AdminOrderStatus | ""; q?: string; page?: number; size?: number },
): Promise<AdminPage<AdminOrderSummary>> {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (filters.q?.trim()) params.set("q", filters.q.trim());
  params.set("page", String(filters.page ?? 0));
  params.set("size", String(filters.size ?? 20));
  return apiFetch<AdminPage<AdminOrderSummary>>(`/api/v1/admin/orders?${params}`, {
    headers: { Authorization: auth.header },
    signal: AbortSignal.timeout(15000),
  });
}

export async function getAdminProducts(
  auth: AdminCredentials,
  size = 100,
): Promise<AdminPage<AdminProductSummary>> {
  const params = new URLSearchParams({ page: "0", size: String(size) });
  return apiFetch<AdminPage<AdminProductSummary>>(`/api/v1/admin/products?${params}`, {
    headers: { Authorization: auth.header },
    signal: AbortSignal.timeout(15000),
  });
}

export async function getAdminProductImages(
  auth: AdminCredentials,
  productId: string,
): Promise<AdminProductImage[]> {
  return apiFetch<AdminProductImage[]>(`/api/v1/admin/products/${productId}/images`, {
    headers: { Authorization: auth.header },
    signal: AbortSignal.timeout(15000),
  });
}

export async function uploadAdminProductImage(
  auth: AdminCredentials,
  productId: string,
  file: File,
  altText: string,
  primary: boolean,
): Promise<AdminProductImage> {
  const form = new FormData();
  form.set("file", file);
  form.set("altText", altText);
  form.set("primary", String(primary));
  return apiFetch<AdminProductImage>(`/api/v1/admin/products/${productId}/images`, {
    method: "POST",
    headers: { Authorization: auth.header },
    body: form,
    signal: AbortSignal.timeout(30000),
  });
}

export async function deleteAdminProductImage(
  auth: AdminCredentials,
  productId: string,
  imageId: string,
): Promise<void> {
  return apiFetchVoid(`/api/v1/admin/products/${productId}/images/${imageId}`, {
    method: "DELETE",
    headers: { Authorization: auth.header },
    signal: AbortSignal.timeout(15000),
  });
}

export async function getAdminOrder(
  auth: AdminCredentials,
  id: string,
): Promise<AdminOrderDetail> {
  return apiFetch<AdminOrderDetail>(`/api/v1/admin/orders/${id}`, {
    headers: { Authorization: auth.header },
    signal: AbortSignal.timeout(15000),
  });
}

export async function updateAdminOrderStatus(
  auth: AdminCredentials,
  id: string,
  payload: UpdateOrderStatusPayload,
): Promise<AdminOrderDetail> {
  return apiFetch<AdminOrderDetail>(`/api/v1/admin/orders/${id}/status`, {
    method: "PATCH",
    headers: jsonHeaders(auth),
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(15000),
  });
}

/** Construit l'en-tête Basic. Le mot de passe ne quitte jamais la mémoire de l'onglet. */
export function createAdminCredentials(email: string, password: string): AdminCredentials {
  const bytes = new TextEncoder().encode(`${email}:${password}`);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return { header: `Basic ${btoa(binary)}`, email };
}
