"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api-client";
import type { OrderTracking } from "@/lib/cart-types";
import { formatXaf } from "@/lib/format";
import { trackOrder } from "@/lib/orders";

const STATUS_STEPS = [
  { value: "PENDING", label: "En attente" },
  { value: "CONFIRMED", label: "Confirmée" },
  { value: "PROCESSING", label: "En préparation" },
  { value: "SHIPPED", label: "Expédiée" },
  { value: "DELIVERED", label: "Livrée" },
];

const STATUS_LABELS: Record<string, string> = {
  ...Object.fromEntries(STATUS_STEPS.map((step) => [step.value, step.label])),
  CANCELLED: "Annulée",
};

const PAYMENT_LABELS: Record<string, string> = {
  PENDING: "À convenir avec la boutique",
  PROCESSING: "En cours",
  SUCCESS: "Payé",
  FAILED: "Échoué",
  CANCELLED: "Annulé",
};

const dateFormatter = new Intl.DateTimeFormat("fr-FR", {
  dateStyle: "long",
  timeStyle: "short",
});

const inputClass =
  "w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose";

export function OrderTrackingView() {
  const searchParams = useSearchParams();
  const [orderNumber, setOrderNumber] = useState(searchParams.get("numero") ?? "");
  const [phone, setPhone] = useState("");

  const mutation = useMutation({ mutationFn: trackOrder });
  const error =
    mutation.error instanceof ApiError ? mutation.error.message : mutation.error?.message;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate({
      orderNumber: orderNumber.trim(),
      phone: phone.trim(),
    });
  }

  function handleReset() {
    mutation.reset();
    setOrderNumber("");
    setPhone("");
  }

  if (mutation.data) {
    return <TrackingResult order={mutation.data} onReset={handleReset} />;
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="mx-auto mt-8 max-w-xl rounded-2xl border border-noir/5 bg-white p-6 sm:p-8"
    >
      <div className="grid gap-4">
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">Numéro de commande *</span>
          <input
            name="orderNumber"
            required
            maxLength={32}
            value={orderNumber}
            onChange={(event) => setOrderNumber(event.target.value)}
            placeholder="LC-20261113-ABC123"
            autoComplete="off"
            className={`${inputClass} font-mono uppercase`}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">Numéro de téléphone utilisé *</span>
          <input
            name="phone"
            type="tel"
            required
            pattern="[+0-9 ]{8,20}"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="+237 6 90 00 00 00"
            autoComplete="tel"
            className={inputClass}
          />
        </label>
      </div>

      {error ? (
        <p className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      ) : null}

      <button
        type="submit"
        disabled={mutation.isPending}
        className="mt-5 w-full rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold transition-colors hover:bg-rose disabled:cursor-not-allowed disabled:opacity-60"
      >
        {mutation.isPending ? "Recherche…" : "Retrouver ma commande"}
      </button>

      <p className="mt-4 text-center text-xs leading-relaxed text-noir-soft">
        Utilisez le numéro reçu à la confirmation et le même numéro de téléphone que lors de la commande.
        Ces deux informations sont nécessaires pour protéger vos données.
      </p>
    </form>
  );
}

function TrackingResult({
  order,
  onReset,
}: {
  order: OrderTracking;
  onReset: () => void;
}) {
  const cancelled = order.orderStatus === "CANCELLED";
  const currentIndex = Math.max(
    0,
    STATUS_STEPS.findIndex((step) => step.value === order.orderStatus),
  );

  return (
    <div className="mx-auto mt-8 max-w-3xl">
      <section className="rounded-2xl border border-noir/5 bg-white p-6 sm:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.25em] text-rose-deep">Commande</p>
            <h2 className="mt-1 font-mono text-xl font-medium">{order.orderNumber}</h2>
            <p className="mt-1 text-xs text-noir-soft">
              Passée le {dateFormatter.format(new Date(order.createdAt))}
            </p>
          </div>
          <span
            className={`rounded-full px-3 py-1 text-xs font-medium ${
              cancelled ? "bg-red-50 text-red-700" : "bg-blush-soft text-noir"
            }`}
          >
            {STATUS_LABELS[order.orderStatus] ?? order.orderStatus}
          </span>
        </div>

        {cancelled ? (
          <p className="mt-6 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
            Cette commande a été annulée. Contactez la boutique pour toute question.
          </p>
        ) : (
          <ol className="mt-7 grid gap-2 sm:grid-cols-5">
            {STATUS_STEPS.map((step, index) => {
              const reached = index <= currentIndex;
              const current = index === currentIndex;
              return (
                <li
                  key={step.value}
                  className={`rounded-xl border px-3 py-3 text-center ${
                    current
                      ? "border-rose bg-blush-soft/60"
                      : reached
                        ? "border-rose/30 bg-white"
                        : "border-noir/5 bg-white/60"
                  }`}
                >
                  <span
                    className={`mx-auto flex size-6 items-center justify-center rounded-full text-xs ${
                      reached ? "bg-noir text-blanc font-semibold" : "bg-noir/5 text-noir-soft"
                    }`}
                  >
                    {index + 1}
                  </span>
                  <span
                    className={`mt-2 block text-xs ${
                      reached ? "font-medium text-noir" : "text-noir-soft"
                    }`}
                  >
                    {step.label}
                  </span>
                </li>
              );
            })}
          </ol>
        )}

        <dl className="mt-7 grid gap-3 border-t border-noir/5 pt-5 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs text-noir-soft">Livraison</dt>
            <dd className="mt-0.5">
              {order.deliveryCity} — {order.deliveryZoneName}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-noir-soft">Paiement</dt>
            <dd className="mt-0.5">{PAYMENT_LABELS[order.paymentStatus] ?? order.paymentStatus}</dd>
          </div>
        </dl>

        <ul className="mt-5 space-y-3 border-t border-noir/5 pt-5 text-sm">
          {order.items.map((item, index) => (
            <li key={`${item.productSku}-${index}`} className="flex justify-between gap-3">
              <span className="text-noir-soft">
                {item.productName} <span className="text-xs">× {item.quantity}</span>
              </span>
              <span>{formatXaf(item.subtotalXaf)}</span>
            </li>
          ))}
        </ul>

        <div className="mt-5 flex justify-between border-t border-noir/5 pt-4 text-base font-medium">
          <span>Total</span>
          <span>{formatXaf(order.totalXaf)}</span>
        </div>
      </section>

      <div className="mt-8 flex flex-wrap gap-4">
        <button
          type="button"
          onClick={onReset}
          className="rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Rechercher une autre commande
        </button>
        <Link
          href="/boutique"
          className="rounded-full border border-noir/10 px-6 py-3 text-sm hover:border-rose"
        >
          Retour à la boutique
        </Link>
      </div>
    </div>
  );
}
