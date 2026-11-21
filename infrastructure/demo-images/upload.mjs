// Envoie les photos préparées comme image principale de chaque produit.
// Usage : node upload.mjs <dossierFinal> <backend/.env> [apiUrl]
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
const [, , DIR, ENV_FILE, API = "http://localhost:8080"] = process.argv;

const env = Object.fromEntries(readFileSync(ENV_FILE, "utf8").split(/\r?\n/)
  .filter((l) => /^[A-Z_]+=/.test(l)).map((l) => [l.slice(0, l.indexOf("=")), l.slice(l.indexOf("=") + 1).trim()]));
const auth = "Basic " + Buffer.from(`${env.ADMIN_EMAIL}:${env.ADMIN_PASSWORD}`).toString("base64");

const list = await (await fetch(`${API}/api/v1/products?size=100`)).json();
const bySlug = Object.fromEntries(list.content.map((p) => [p.slug, p]));

let ok = 0;
for (const f of readdirSync(DIR).filter((f) => f.endsWith(".jpg")).sort()) {
  const slug = f.replace(/\.jpg$/, "");
  const product = bySlug[slug];
  if (!product) { console.log(`✘ ${slug} : produit introuvable`); continue; }
  if (product.imageUrl) { console.log(`- ${slug} : a déjà une image, ignoré`); continue; }
  const form = new FormData();
  form.set("file", new Blob([readFileSync(join(DIR, f))], { type: "image/jpeg" }), f);
  form.set("altText", product.name);
  form.set("primary", "true");
  const r = await fetch(`${API}/api/v1/admin/products/${product.id}/images`, {
    method: "POST", headers: { Authorization: auth }, body: form,
  });
  if (r.ok) { ok++; console.log(`✔ ${slug}`); }
  else console.log(`✘ ${slug} : HTTP ${r.status} ${(await r.text()).slice(0, 160)}`);
}
console.log(`${ok} image(s) envoyée(s)`);
