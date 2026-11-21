import Link from "next/link";
import Image from "next/image";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { AddToCartButton } from "@/components/add-to-cart-button";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { getProduct } from "@/lib/catalog";
import { ProductPrice } from "@/components/product-price";

type Params = Promise<{ slug: string }>;

export default function ProductPage({ params }: { params: Params }) {
  return (
    <>
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 sm:py-16">
        <Suspense fallback={<ProductSkeleton />}>
          <ProductView params={params} />
        </Suspense>
      </main>
      <SiteFooter />
    </>
  );
}

async function ProductView({ params }: { params: Params }) {
  const { slug } = await params;
  const result = await getProduct(slug);

  if (result.status === "not-found") {
    notFound();
  }

  if (result.status === "unavailable") {
    return (
      <div className="py-16 text-center">
        <p className="font-display text-xl font-bold">Fiche produit momentanément indisponible</p>
        <p className="mx-auto mt-2 max-w-md text-sm text-noir-soft">
          Le produit n&apos;a pas pu être chargé. Veuillez réessayer dans quelques instants.
        </p>
        <Link
          href="/boutique"
          className="mt-6 inline-block border-b-2 border-rose pb-1 text-sm font-semibold"
        >
          Retour à la boutique
        </Link>
      </div>
    );
  }

  const product = result.data;
  const primaryImage = product.images.find((image) => image.primary && image.imageUrl)
    ?? product.images.find((image) => image.imageUrl);

  return (
    <>
      <nav className="flex flex-wrap items-center gap-2 text-xs text-noir-soft" aria-label="Fil d'Ariane">
        <Link href="/boutique" className="hover:text-noir">
          Boutique
        </Link>
        <span aria-hidden>/</span>
        <Link
          href={`/boutique?categorie=${encodeURIComponent(product.categorySlug)}`}
          className="hover:text-noir"
        >
          {product.categoryName}
        </Link>
        <span aria-hidden>/</span>
        <span className="text-noir">{product.name}</span>
      </nav>

      <div className="mt-8 grid gap-8 lg:grid-cols-2 lg:gap-14">
        <div>
          <div className="relative aspect-[4/5] overflow-hidden rounded-3xl bg-blush-soft">
            {primaryImage?.imageUrl ? (
              <Image
                src={primaryImage.imageUrl}
                alt={primaryImage.altText ?? product.name}
                fill
                sizes="(max-width: 1024px) 100vw, 50vw"
                unoptimized
                className="object-cover"
              />
            ) : (
              <div className="relative flex h-full flex-col justify-between p-8">
                <span className="text-xs font-semibold uppercase tracking-[0.25em] text-gold-deep">
                  {product.categoryName}
                </span>
                <span className="font-display text-5xl font-bold text-noir/60">Lisa</span>
              </div>
            )}
          </div>
          {product.images.length > 1 ? (
            <div className="mt-3 grid grid-cols-4 gap-3">
              {product.images.filter((image) => image.imageUrl).map((image) => (
                <div key={image.id} className="relative aspect-square overflow-hidden rounded-xl bg-blush-soft">
                  <Image
                    src={image.imageUrl!}
                    alt={image.altText ?? product.name}
                    fill
                    sizes="120px"
                    unoptimized
                    className="object-cover"
                  />
                </div>
              ))}
            </div>
          ) : null}
        </div>

        <div>
          <h1 className="font-display text-3xl font-bold leading-tight sm:text-4xl">
            {product.name}
          </h1>
          {product.shortDescription ? (
            <p className="mt-3 text-base text-noir-soft">{product.shortDescription}</p>
          ) : null}

          <p className="mt-6">
            <ProductPrice
              priceXaf={product.priceXaf}
              compareAtPriceXaf={product.compareAtPriceXaf}
              size="lg"
            />
          </p>
          <p
            className={`mt-2 text-sm ${
              product.inStock ? "text-emerald-700" : "text-noir-soft"
            }`}
          >
            {product.inStock ? `En stock (${product.stock} disponible${product.stock > 1 ? "s" : ""})` : "Épuisé"}
          </p>

          {product.description ? (
            <p className="mt-8 whitespace-pre-line text-sm leading-relaxed text-noir-soft">
              {product.description}
            </p>
          ) : null}

          <div className="mt-8">
            <AddToCartButton productId={product.id} disabled={!product.inStock} />
          </div>
          <p className="mt-3 text-xs text-noir-soft">
            Ajoutez le produit au panier, puis finalisez votre commande en quelques étapes.
          </p>

          <ul className="mt-8 space-y-3 border-t border-noir/5 pt-6 text-sm text-noir-soft">
            <li>Livraison à Yaoundé et dans les autres villes du Cameroun.</li>
            <li>Aucun paiement en ligne : le règlement est convenu avec la boutique.</li>
            <li>Retrait en boutique possible sur demande.</li>
          </ul>
        </div>
      </div>
    </>
  );
}

function ProductSkeleton() {
  return (
    <div className="grid animate-pulse gap-8 lg:grid-cols-2 lg:gap-14" aria-hidden>
      <div className="aspect-[4/5] rounded-3xl bg-blush-soft" />
      <div className="space-y-4">
        <div className="h-10 w-3/4 rounded bg-blush-soft" />
        <div className="h-4 w-1/2 rounded bg-blush-soft" />
        <div className="h-6 w-1/3 rounded bg-blush-soft" />
        <div className="h-32 w-full rounded bg-blush-soft" />
      </div>
    </div>
  );
}
