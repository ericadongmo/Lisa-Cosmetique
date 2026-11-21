import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from "@tanstack/react-query";
import { apiFetchResponse, apiFetchVoid } from "@/lib/api-client";
import type { Cart } from "@/lib/cart-types";

const CART_TOKEN_KEY = "lisa.cart.token";

function readCartToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(CART_TOKEN_KEY);
  } catch {
    return null;
  }
}

function storeCartToken(token: string | null) {
  if (typeof window === "undefined" || !token) return;
  try {
    window.localStorage.setItem(CART_TOKEN_KEY, token);
  } catch {
    // Navigation privée ou stockage plein : le panier reste utilisable pour la session.
  }
}

function cartHeaders(extra?: HeadersInit): HeadersInit {
  const token = readCartToken();
  return {
    Accept: "application/json",
    ...(token ? { "X-Cart-Token": token } : {}),
    ...extra,
  };
}

async function cartRequest(path: string, init?: RequestInit): Promise<Cart> {
  const { data, headers } = await apiFetchResponse<Cart>(path, {
    ...init,
    headers: cartHeaders(init?.headers),
    cache: "no-store",
  });
  // L'API émet le jeton au premier appel d'un visiteur ; on le conserve pour la suite.
  storeCartToken(headers.get("X-Cart-Token"));
  return data;
}

export const cartQueryKey = ["cart"] as const;

export function fetchCart(): Promise<Cart> {
  return cartRequest("/api/v1/cart");
}

function addItem({ productId, quantity }: { productId: string; quantity: number }): Promise<Cart> {
  return cartRequest("/api/v1/cart/items", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ productId, quantity }),
  });
}

function updateItem({ productId, quantity }: { productId: string; quantity: number }): Promise<Cart> {
  return cartRequest(`/api/v1/cart/items/${productId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ quantity }),
  });
}

function removeItem(productId: string): Promise<Cart> {
  return cartRequest(`/api/v1/cart/items/${productId}`, { method: "DELETE" });
}

export async function clearCart(): Promise<void> {
  await apiFetchVoid("/api/v1/cart", {
    method: "DELETE",
    headers: cartHeaders(),
  });
}

export function useCart(): UseQueryResult<Cart> {
  return useQuery({
    queryKey: cartQueryKey,
    queryFn: fetchCart,
    staleTime: 0,
  });
}

function useCartMutation<TVariables>(
  mutationFn: (variables: TVariables) => Promise<Cart>,
): UseMutationResult<Cart, Error, TVariables> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (cart) => {
      queryClient.setQueryData(cartQueryKey, cart);
    },
  });
}

export function useAddToCart() {
  return useCartMutation(addItem);
}

export function useUpdateCartItem() {
  return useCartMutation(updateItem);
}

export function useRemoveCartItem() {
  return useCartMutation(removeItem);
}
