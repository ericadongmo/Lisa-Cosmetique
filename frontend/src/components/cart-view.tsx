"use client";

import Link from "next/link";
import { ApiError } from "@/lib/api-client";
import { useCart, useRemoveCartItem, useUpdateCartItem } from "@/lib/cart";
import { formatXaf } from "@/lib/format";

export function CartView() {
  const { data: cart, isPending, isError, refetch } = useCart();
  const updateQuantity = useUpdateCartItem();
  const removeItem = useRemoveCartItem();
  const mutationError = updateQuantity.error ?? removeItem.error;

  if (isPending) {
    return (
      <div className="mt-10 grid animate-pulse gap-8 lg:grid-cols-[1fr_20rem]" aria-hidden>
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-28 rounded-2xl bg-white" />
          ))}
        </div>
        <div className="h-56 rounded-2xl bg-white" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-16 text-center">
        <p className="font-display text-xl font-bold">Votre panier est momentanément indisponible</p>
        <p className="mx-auto mt-2 max-w-md text-sm text-noir-soft">
          Impossible de charger votre panier. Vérifiez votre connexion, puis réessayez.
        </p>
        <button
          type="button"
          onClick={() => void refetch()}
          className="mt-6 rounded-full bg-noir px-5 py-2.5 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Réessayer
        </button>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="py-16 text-center">
        <p className="font-display text-2xl font-bold">Votre panier est vide</p>
        <p className="mx-auto mt-3 max-w-md text-sm text-noir-soft">
          Parcourez la boutique et ajoutez vos produits préférés pour passer commande.
        </p>
        <Link
          href="/boutique"
          className="mt-8 inline-block rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Découvrir la boutique
        </Link>
      </div>
    );
  }

  return (
    <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_20rem] lg:items-start">
      <div>
        {mutationError ? (
          <p className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
            {mutationError instanceof ApiError
              ? mutationError.message
              : "La modification du panier a échoué."}
          </p>
        ) : null}

        <ul className="divide-y divide-noir/5 border-y border-noir/5">
          {cart.items.map((item) => (
            <li key={item.productId} className="flex gap-4 py-5">
              <div
                className="relative hidden size-20 shrink-0 overflow-hidden rounded-xl bg-blush-soft sm:block"
                aria-hidden
              >
                <span className="absolute inset-0 flex items-center justify-center font-display text-base text-noir/60 font-bold">
                  Lisa
                </span>
              </div>

              <div className="flex min-w-0 flex-1 flex-col gap-3">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <Link
                      href={`/boutique/${item.productSlug}`}
                      className="font-display text-base font-bold hover:text-rose-deep"
                    >
                      {item.productName}
                    </Link>
                    <p className="mt-1 text-xs text-noir-soft">
                      {formatXaf(item.unitPriceXaf)} l&apos;unité
                    </p>
                  </div>
                  <p className="text-sm font-medium">{formatXaf(item.lineTotalXaf)}</p>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="inline-flex items-center rounded-full border border-noir/10 bg-white">
                    <button
                      type="button"
                      aria-label={`Diminuer la quantité de ${item.productName}`}
                      disabled={updateQuantity.isPending || item.quantity <= 1}
                      onClick={() =>
                        updateQuantity.mutate({
                          productId: item.productId,
                          quantity: item.quantity - 1,
                        })
                      }
                      className="px-3 py-1.5 text-sm text-noir-soft disabled:opacity-40"
                    >
                      −
                    </button>
                    <span className="min-w-6 text-center text-sm" aria-live="polite">
                      {item.quantity}
                    </span>
                    <button
                      type="button"
                      aria-label={`Augmenter la quantité de ${item.productName}`}
                      disabled={updateQuantity.isPending || item.quantity >= item.availableStock}
                      onClick={() =>
                        updateQuantity.mutate({
                          productId: item.productId,
                          quantity: item.quantity + 1,
                        })
                      }
                      className="px-3 py-1.5 text-sm text-noir-soft disabled:opacity-40"
                    >
                      +
                    </button>
                  </div>

                  <div className="flex items-center gap-4">
                    <span className="text-[0.7rem] text-noir-soft">
                      {item.availableStock} en stock
                    </span>
                    <button
                      type="button"
                      disabled={removeItem.isPending}
                      onClick={() => removeItem.mutate(item.productId)}
                      className="text-xs text-noir-soft underline underline-offset-4 hover:text-noir disabled:opacity-40"
                    >
                      Retirer
                    </button>
                  </div>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>

      <aside className="h-fit rounded-2xl border border-noir/5 bg-white p-6 lg:sticky lg:top-24">
        <h2 className="font-display text-xl font-bold">Récapitulatif</h2>
        <div className="mt-5 flex items-center justify-between border-b border-noir/5 pb-4 text-sm">
          <span className="text-noir-soft">Sous-total</span>
          <span className="font-medium">{formatXaf(cart.subtotalXaf)}</span>
        </div>
        <p className="mt-4 text-xs leading-relaxed text-noir-soft">
          Les frais de livraison sont calculés à l&apos;étape suivante selon votre zone.
        </p>
        <Link
          href="/commande"
          className="mt-6 block rounded-full bg-noir px-6 py-3 text-center text-sm text-blanc font-semibold transition-colors hover:bg-rose"
        >
          Passer commande
        </Link>
        <Link
          href="/boutique"
          className="mt-3 block text-center text-xs text-noir-soft underline underline-offset-4 hover:text-noir"
        >
          Continuer mes achats
        </Link>
      </aside>
    </div>
  );
}
