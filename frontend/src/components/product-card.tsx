import Link from "next/link";
import Image from "next/image";
import { AddToCartButton } from "@/components/add-to-cart-button";
import type { ProductSummary } from "@/lib/catalog-types";
import { DiscountBadge, ProductPrice } from "@/components/product-price";

/** Visuel de remplacement tant que les photos produits ne sont pas gérées. */
function ProductPlaceholder({ categoryName }: { categoryName: string }) {
  return (
    <div className="relative aspect-[4/5] overflow-hidden bg-blush-soft">
      <div
        className="absolute -top-10 -right-10 size-40 rounded-full bg-blush/70 blur-2xl"
        aria-hidden
      />
      <div
        className="absolute -bottom-12 -left-8 size-36 rounded-full bg-white/60 blur-2xl"
        aria-hidden
      />
      <div className="relative flex h-full flex-col justify-between p-5">
        <span className="text-[0.6rem] font-semibold uppercase tracking-[0.25em] text-gold-deep">
          {categoryName}
        </span>
        <span className="font-display text-2xl font-bold text-noir/70">Lisa</span>
      </div>
    </div>
  );
}

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <article className="group flex h-full flex-col overflow-hidden rounded-2xl border border-noir/5 bg-white transition-shadow hover:shadow-[0_16px_40px_-24px_rgba(17,17,17,0.35)]">
      <Link href={`/boutique/${product.slug}`} className="relative block flex-1">
        <DiscountBadge priceXaf={product.priceXaf} compareAtPriceXaf={product.compareAtPriceXaf} />
        {product.imageUrl ? (
          <div className="relative aspect-[4/5] overflow-hidden bg-blush-soft">
            <Image
              src={product.imageUrl}
              alt={product.name}
              fill
              sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
              unoptimized
              className="object-cover transition-transform duration-500 group-hover:scale-[1.03]"
            />
          </div>
        ) : (
          <ProductPlaceholder categoryName={product.categoryName} />
        )}
        <div className="space-y-2 p-4 pb-3 sm:p-5 sm:pb-3">
          <h3 className="font-display text-base font-bold leading-snug sm:text-lg">
            {product.name}
          </h3>
          {product.shortDescription ? (
            <p className="line-clamp-2 text-sm text-noir-soft">{product.shortDescription}</p>
          ) : null}
          <div className="flex items-baseline justify-between gap-3 pt-1">
            <ProductPrice priceXaf={product.priceXaf} compareAtPriceXaf={product.compareAtPriceXaf} />
            <span
              className={`shrink-0 text-xs font-medium ${product.inStock ? "text-emerald-700" : "text-rose-deep"}`}
            >
              {product.inStock ? "En stock" : "Épuisé"}
            </span>
          </div>
        </div>
      </Link>
      <div className="px-4 pb-4 sm:px-5 sm:pb-5">
        <AddToCartButton productId={product.id} disabled={!product.inStock} compact />
      </div>
    </article>
  );
}
