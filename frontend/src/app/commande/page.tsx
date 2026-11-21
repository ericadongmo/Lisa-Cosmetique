import type { Metadata } from "next";
import { CheckoutView } from "@/components/checkout-view";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "Commande",
  description: "Finalisez votre commande Lisa Cosmétique et Bien-être.",
};

export default function CheckoutPage() {
  return (
    <>
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-12 sm:px-6 sm:py-16">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Dernière étape</p>
        <h1 className="mt-3 font-display text-3xl font-bold sm:text-4xl">Votre commande</h1>
        <CheckoutView />
      </main>
      <SiteFooter />
    </>
  );
}
