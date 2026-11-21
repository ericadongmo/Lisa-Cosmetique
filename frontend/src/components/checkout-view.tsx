"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api-client";
import { cartQueryKey, clearCart, useCart } from "@/lib/cart";
import type { Order } from "@/lib/cart-types";
import { formatXaf } from "@/lib/format";
import { createOrder, getDeliveryZones } from "@/lib/orders";

const inputClass =
  "w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose";

export function CheckoutView() {
  const { data: cart, isPending, isError, refetch } = useCart();
  const zones = useQuery({
    queryKey: ["delivery-zones"],
    queryFn: getDeliveryZones,
    staleTime: 300_000,
  });

  const [selectedZoneId, setSelectedZoneId] = useState("");
  const [order, setOrder] = useState<Order | null>(null);
  const queryClient = useQueryClient();

  // Première zone active par défaut, sans effet de synchronisation supplémentaire.
  const zoneId = selectedZoneId || zones.data?.[0]?.id || "";
  const selectedZone = zones.data?.find((zone) => zone.id === zoneId);

  const mutation = useMutation({
    mutationFn: createOrder,
    onSuccess: async (created) => {
      setOrder(created);
      window.scrollTo({ top: 0, behavior: "smooth" });
      try {
        await clearCart();
      } catch {
        // La commande existe déjà : un panier non vidé ne doit pas masquer la confirmation.
      } finally {
        await queryClient.invalidateQueries({ queryKey: cartQueryKey });
      }
    },
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!cart || !zoneId || !selectedZone) return;

    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    mutation.mutate({
      items: cart.items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
      })),
      deliveryZoneId: zoneId,
      customerName: value("name"),
      customerPhone: value("phone"),
      customerEmail: value("email") || null,
      deliveryAddress: {
        recipientName: value("name"),
        phoneNumber: value("phone"),
        city: selectedZone.city,
        neighborhood: value("neighborhood"),
        streetAddress: value("streetAddress"),
        additionalInformation: value("additionalInformation") || null,
      },
      notes: value("notes") || null,
    });
  }

  if (order) {
    return <OrderConfirmation order={order} />;
  }

  if (isPending) {
    return (
      <div className="mt-10 grid animate-pulse gap-8 lg:grid-cols-[1fr_22rem]" aria-hidden>
        <div className="h-[34rem] rounded-2xl bg-white" />
        <div className="h-64 rounded-2xl bg-white" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-16 text-center">
        <p className="font-display text-xl font-bold">Votre panier est momentanément indisponible</p>
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
          Ajoutez des produits à votre panier avant de passer commande.
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

  const estimatedTotal = cart.subtotalXaf + (selectedZone?.deliveryFeeXaf ?? 0);
  const submitError =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error?.message;

  return (
    <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_22rem] lg:items-start">
      <form onSubmit={handleSubmit} className="space-y-6">
        <section className="rounded-2xl border border-noir/5 bg-white p-5 sm:p-6">
          <h2 className="font-display text-xl font-bold">Vos coordonnées</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <label className="block text-sm">
              <span className="mb-1.5 block text-noir-soft">Nom complet *</span>
              <input
                name="name"
                required
                minLength={2}
                maxLength={160}
                autoComplete="name"
                className={inputClass}
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1.5 block text-noir-soft">Téléphone *</span>
              <input
                name="phone"
                type="tel"
                required
                pattern="[+0-9 ]{8,20}"
                autoComplete="tel"
                placeholder="+237 6 90 00 00 00"
                className={inputClass}
              />
            </label>
            <label className="block text-sm sm:col-span-2">
              <span className="mb-1.5 block text-noir-soft">E-mail (facultatif)</span>
              <input name="email" type="email" autoComplete="email" className={inputClass} />
            </label>
          </div>
        </section>

        <section className="rounded-2xl border border-noir/5 bg-white p-5 sm:p-6">
          <h2 className="font-display text-xl font-bold">Livraison</h2>
          <div className="mt-5 grid gap-4">
            <label className="block text-sm">
              <span className="mb-1.5 block text-noir-soft">Zone de livraison *</span>
              <select
                required
                value={zoneId}
                onChange={(event) => setSelectedZoneId(event.target.value)}
                disabled={zones.isPending || zones.isError}
                className={inputClass}
              >
                {zones.isPending ? <option value="">Chargement des zones…</option> : null}
                {zones.isError ? <option value="">Zones indisponibles</option> : null}
                {zones.data?.map((zone) => (
                  <option key={zone.id} value={zone.id}>
                    {zone.city} — {zone.name} ({formatXaf(zone.deliveryFeeXaf)},{" "}
                    {zone.estimatedDeliveryDays} j)
                  </option>
                ))}
              </select>
              {zones.isError ? (
                <span className="mt-2 block text-xs text-red-700">
                  Impossible de charger les zones de livraison. Réessayez dans un instant.
                </span>
              ) : null}
            </label>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block text-sm">
                <span className="mb-1.5 block text-noir-soft">Quartier *</span>
                <input
                  name="neighborhood"
                  required
                  maxLength={160}
                  autoComplete="address-level3"
                  className={inputClass}
                />
              </label>
              <label className="block text-sm">
                <span className="mb-1.5 block text-noir-soft">Rue / repère *</span>
                <input
                  name="streetAddress"
                  required
                  maxLength={300}
                  autoComplete="street-address"
                  className={inputClass}
                />
              </label>
            </div>

            <label className="block text-sm">
              <span className="mb-1.5 block text-noir-soft">
                Informations complémentaires (facultatives)
              </span>
              <textarea
                name="additionalInformation"
                rows={2}
                maxLength={500}
                placeholder="Immeuble, étage, point de repère…"
                className={inputClass}
              />
            </label>

            <label className="block text-sm">
              <span className="mb-1.5 block text-noir-soft">Note pour la commande (facultative)</span>
              <textarea name="notes" rows={2} maxLength={1000} className={inputClass} />
            </label>
          </div>
        </section>

        {submitError ? (
          <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
            {submitError}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={mutation.isPending || !zoneId || zones.isError}
          className="w-full rounded-full bg-noir px-6 py-3.5 text-sm text-blanc font-semibold transition-colors hover:bg-rose disabled:cursor-not-allowed disabled:opacity-60"
        >
          {mutation.isPending
            ? "Enregistrement de la commande…"
            : `Confirmer la commande · ${formatXaf(estimatedTotal)}`}
        </button>
        <p className="text-center text-xs text-noir-soft">
          Aucun paiement n&apos;est débité sur ce site. Les frais de livraison sont confirmés avec
          la commande.
        </p>
      </form>

      <aside className="h-fit rounded-2xl border border-noir/5 bg-white p-6 lg:sticky lg:top-24">
        <h2 className="font-display text-xl font-bold">Votre commande</h2>
        <ul className="mt-5 space-y-3 border-b border-noir/5 pb-5 text-sm">
          {cart.items.map((item) => (
            <li key={item.productId} className="flex justify-between gap-3">
              <span className="text-noir-soft">
                {item.productName} <span className="text-xs">× {item.quantity}</span>
              </span>
              <span>{formatXaf(item.lineTotalXaf)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-5 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-noir-soft">Sous-total</dt>
            <dd>{formatXaf(cart.subtotalXaf)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-noir-soft">Livraison</dt>
            <dd>
              {selectedZone ? formatXaf(selectedZone.deliveryFeeXaf) : "À définir"}
            </dd>
          </div>
          <div className="flex justify-between border-t border-noir/5 pt-3 font-medium">
            <dt>Total estimé</dt>
            <dd>{formatXaf(estimatedTotal)}</dd>
          </div>
        </dl>
        <p className="mt-3 text-[0.7rem] leading-relaxed text-noir-soft">
          Les promotions en cours sont appliquées automatiquement par la boutique.
        </p>
        <Link
          href="/panier"
          className="mt-5 block text-center text-xs text-noir-soft underline underline-offset-4 hover:text-noir"
        >
          Modifier mon panier
        </Link>
      </aside>
    </div>
  );
}

function OrderConfirmation({ order }: { order: Order }) {
  return (
    <div className="mx-auto max-w-2xl py-8">
      <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">Commande confirmée</p>
      <h1 className="mt-3 font-display text-3xl font-bold sm:text-4xl">
        Merci pour votre commande
      </h1>
      <p className="mt-4 text-sm leading-relaxed text-noir-soft">
        Votre commande <span className="font-medium text-noir">{order.orderNumber}</span> a
        bien été enregistrée. Nous vous contacterons au numéro indiqué pour organiser la livraison.
      </p>

      <div className="mt-8 rounded-2xl border border-noir/5 bg-white p-6">
        <ul className="space-y-3 border-b border-noir/5 pb-5 text-sm">
          {order.items.map((item) => (
            <li key={`${item.productSku}-${item.productName}`} className="flex justify-between gap-3">
              <span className="text-noir-soft">
                {item.productName} <span className="text-xs">× {item.quantity}</span>
              </span>
              <span>{formatXaf(item.subtotalXaf)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-5 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-noir-soft">Sous-total</dt>
            <dd>{formatXaf(order.subtotalXaf)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-noir-soft">Livraison</dt>
            <dd>{formatXaf(order.deliveryFeeXaf)}</dd>
          </div>
          {order.discountXaf > 0 ? (
            <div className="flex justify-between text-emerald-700">
              <dt>Remise</dt>
              <dd>−{formatXaf(order.discountXaf)}</dd>
            </div>
          ) : null}
          <div className="flex justify-between border-t border-noir/5 pt-3 text-base font-medium">
            <dt>Total</dt>
            <dd>{formatXaf(order.totalXaf)}</dd>
          </div>
        </dl>
      </div>

      <div className="mt-4 rounded-2xl bg-blush-soft/60 px-5 py-4 text-sm">
        <p className="font-medium">Aucun paiement n&apos;est effectué en ligne.</p>
        <p className="mt-1 text-xs text-noir-soft">
          Notre équipe vous contactera au numéro indiqué pour convenir du règlement et de la
          livraison.
        </p>
      </div>

      <div className="mt-8 flex flex-wrap gap-4">
        <Link
          href={`/suivi-commande?numero=${encodeURIComponent(order.orderNumber)}`}
          className="rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Suivre ma commande
        </Link>
        <Link
          href="/boutique"
          className="rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Retour à la boutique
        </Link>
        <Link
          href="/"
          className="rounded-full border border-noir/10 px-6 py-3 text-sm hover:border-rose"
        >
          Accueil
        </Link>
      </div>
    </div>
  );
}
