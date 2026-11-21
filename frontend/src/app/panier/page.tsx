import type { Metadata } from "next";
import { CartView } from "@/components/cart-view";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "Panier",
  description: "Votre sélection de produits Lisa Cosmétique et Bien-être.",
};

export default function CartPage() {
  return (
    <>
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-12 sm:px-6 sm:py-16">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Votre sélection</p>
        <h1 className="mt-3 font-display text-3xl font-bold sm:text-4xl">Panier</h1>
        <CartView />
      </main>
      <SiteFooter />
    </>
  );
}
