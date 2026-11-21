import type { Metadata } from "next";
import Link from "next/link";
import { AdminOrdersView } from "@/components/admin-orders-view";

export const metadata: Metadata = {
  title: "Administration — Commandes",
  robots: { index: false, follow: false },
};

export default function AdminOrdersPage() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 sm:py-14">
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Back-office</p>
        <h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Commandes</h1>
        <p className="mt-3 max-w-xl text-sm leading-relaxed text-noir-soft">
          Confirmez, préparez, expédiez et livrez les commandes. Toute évolution est
          immédiatement visible dans le suivi public du client.
        </p>
        <Link
          href="/admin/produits"
          className="mt-4 inline-flex rounded-full border border-noir/10 bg-white px-4 py-2 text-sm text-noir transition-colors hover:border-rose"
        >
          Gérer les images des produits
        </Link>
      </div>
      <AdminOrdersView />
    </main>
  );
}
