import type { Metadata } from "next";
import { Suspense } from "react";
import { OrderTrackingView } from "@/components/order-tracking-view";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

export const metadata: Metadata = {
  title: "Suivi de commande",
  description: "Retrouvez l'avancement de votre commande Lisa Cosmétique et Bien-être.",
};

export default function OrderTrackingPage() {
  return (
    <>
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-12 sm:px-6 sm:py-16">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Suivi</p>
          <h1 className="mt-3 font-display text-3xl font-bold sm:text-4xl">
            Où en est ma commande ?
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-noir-soft">
            Saisissez votre numéro de commande et le numéro de téléphone utilisé lors de l&apos;achat.
          </p>
        </div>
        <Suspense
          fallback={
            <div className="mx-auto mt-8 h-80 max-w-xl animate-pulse rounded-2xl bg-white" aria-hidden />
          }
        >
          <OrderTrackingView />
        </Suspense>
      </main>
      <SiteFooter />
    </>
  );
}
