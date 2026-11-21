import type { Metadata } from "next";
import { AdminProductImagesView } from "@/components/admin-product-images-view";

export const metadata: Metadata = {
  title: "Administration — Images des produits",
  robots: { index: false, follow: false },
};

export default function AdminProductsPage() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 sm:py-14">
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Back-office</p>
        <h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">Images des produits</h1>
        <p className="mt-3 max-w-xl text-sm leading-relaxed text-noir-soft">
          Ajoutez les photos des produits. Elles sont stockées de façon sécurisée et
          affichées avec des liens temporaires.
        </p>
      </div>
      <AdminProductImagesView />
    </main>
  );
}
