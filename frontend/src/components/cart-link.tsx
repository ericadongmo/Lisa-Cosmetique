"use client";

import Link from "next/link";
import { useCart } from "@/lib/cart";

/** Icône panier de la barre de navigation, avec pastille du nombre d'articles. */
export function CartLink() {
  const { data: cart } = useCart();
  const count = cart?.totalQuantity ?? 0;
  const label = `Panier, ${count} article${count > 1 ? "s" : ""}`;

  return (
    <Link
      href="/panier"
      className="relative inline-flex size-10 items-center justify-center rounded-full text-white transition-colors hover:text-rose-light"
      aria-label={label}
      title="Panier"
    >
      <svg
        viewBox="0 0 24 24"
        className="size-6"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <path d="M5 8h14l-1.2 11.1a2 2 0 0 1-2 1.9H8.2a2 2 0 0 1-2-1.9z" />
        <path d="M9 8V6a3 3 0 0 1 6 0v2" />
      </svg>
      {count > 0 && (
        <span
          className="absolute -top-0.5 -right-0.5 inline-flex min-w-5 items-center justify-center rounded-full bg-rose px-1 text-[0.65rem] leading-5 font-bold text-white ring-2 ring-noir"
          aria-hidden
        >
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}
