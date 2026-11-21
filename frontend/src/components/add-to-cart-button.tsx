"use client";

import { useEffect } from "react";
import { ApiError } from "@/lib/api-client";
import { useAddToCart } from "@/lib/cart";

type AddToCartButtonProps = {
  productId: string;
  disabled?: boolean;
  compact?: boolean;
  label?: string;
};

export function AddToCartButton({
  productId,
  disabled = false,
  compact = false,
  label = "Ajouter au panier",
}: AddToCartButtonProps) {
  const { mutate, isPending, isSuccess, error, reset } = useAddToCart();

  useEffect(() => {
    if (!isSuccess) return;
    const timer = window.setTimeout(reset, 2000);
    return () => window.clearTimeout(timer);
  }, [isSuccess, reset]);

  const message = isPending
    ? "Ajout…"
    : isSuccess
      ? "Ajouté au panier ✓"
      : error
        ? "Réessayer"
        : label;

  return (
    <div>
      <button
        type="button"
        disabled={disabled || isPending}
        onClick={() => mutate({ productId, quantity: 1 })}
        className={[
          "rounded-full bg-noir text-blanc font-semibold transition-colors hover:bg-rose disabled:cursor-not-allowed disabled:opacity-60",
          compact ? "w-full px-4 py-2 text-xs" : "w-full px-6 py-3 text-sm sm:w-auto sm:min-w-64",
        ].join(" ")}
      >
        {disabled ? "Épuisé" : message}
      </button>
      {error ? (
        <p className="mt-2 text-xs text-red-700" role="alert">
          {error instanceof ApiError ? error.message : "Impossible d'ajouter ce produit."}
        </p>
      ) : null}
    </div>
  );
}
