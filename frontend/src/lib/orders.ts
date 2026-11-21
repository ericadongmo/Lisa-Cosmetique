import { apiFetch } from "@/lib/api-client";
import type {
  CreateOrderPayload,
  DeliveryZone,
  Order,
  OrderTracking,
  OrderTrackingPayload,
} from "@/lib/cart-types";

const REQUEST_TIMEOUT_MS = 8000;

export async function getDeliveryZones(): Promise<DeliveryZone[]> {
  return apiFetch<DeliveryZone[]>("/api/v1/delivery-zones", {
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    cache: "no-store",
  });
}

export async function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return apiFetch<Order>("/api/v1/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
}

export async function trackOrder(payload: OrderTrackingPayload): Promise<OrderTracking> {
  return apiFetch<OrderTracking>("/api/v1/orders/track", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
}
