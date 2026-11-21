#!/usr/bin/env node
// Vérification de bout en bout du site Lisa Cosmétique à partir de donnees-test.json.
//
//   node tests-e2e/verifier-site.mjs              lance tous les tests et crée les commandes TEST
//   node tests-e2e/verifier-site.mjs --nettoyer   annule les commandes TEST encore annulables
//
// Variables facultatives : API_URL (défaut http://localhost:8081), FRONT_URL (défaut
// http://localhost:3000). Les identifiants admin sont lus dans backend/.env (jamais affichés).
// Aucune dépendance : Node ≥ 20 suffit.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ici = dirname(fileURLToPath(import.meta.url));
const API = process.env.API_URL || 'http://localhost:8081';
const FRONT = process.env.FRONT_URL || 'http://localhost:3000';
const data = JSON.parse(readFileSync(join(ici, 'donnees-test.json'), 'utf8'));

// ---------------------------------------------------------------- outils
const env = Object.fromEntries(
  readFileSync(join(ici, '..', 'backend', '.env'), 'utf8')
    .split(/\r?\n/).filter(l => /^[A-Z_]+=/.test(l))
    .map(l => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()]));
const ADMIN = 'Basic ' + Buffer.from(`${env.ADMIN_EMAIL}:${env.ADMIN_PASSWORD}`).toString('base64');

const resultats = [];
let section = '';
function titre(t) { section = t; console.log(`\n\x1b[1m== ${t}\x1b[0m`); }
function verifier(nom, ok, detail = '') {
  resultats.push({ section, nom, ok, detail });
  console.log(`  ${ok ? '\x1b[32m✔' : '\x1b[31m✘'}\x1b[0m ${nom}${!ok && detail ? `  → ${detail}` : ''}`);
  return ok;
}
async function appel(methode, chemin, { corps, entetes = {}, admin = false, base = API } = {}) {
  const h = { ...entetes };
  if (corps !== undefined) h['Content-Type'] = 'application/json';
  if (admin) h.Authorization = ADMIN;
  const res = await fetch(base + chemin, {
    method: methode, headers: h, body: corps === undefined ? undefined : JSON.stringify(corps),
  });
  const texte = await res.text();
  let json = null; try { json = texte ? JSON.parse(texte) : null; } catch { /* HTML */ }
  return { status: res.status, json, texte, headers: res.headers };
}
const zones = {}; const produits = {};
const stockDe = async () => {
  const r = await appel('GET', '/api/v1/admin/products?size=100', { admin: true });
  return Object.fromEntries((r.json?.content || []).map(p => [p.slug, p.stockQuantity]));
};
const commandeDe = client => ({
  items: client.panier.map(l => ({ productId: produits[l.slug].id, quantity: l.quantite })),
  deliveryZoneId: zones[client.zone].id,
  customerName: client.customerName,
  customerPhone: client.customerPhone,
  customerEmail: client.customerEmail,
  deliveryAddress: client.deliveryAddress,
  notes: client.notes,
});
const changerStatut = (id, status, trackingNumber) =>
  appel('PATCH', `/api/v1/admin/orders/${id}/status`, { admin: true, corps: { status, trackingNumber } });

// ---------------------------------------------------------------- nettoyage
async function nettoyer() {
  titre('Nettoyage des commandes TEST');
  const r = await appel('GET', '/api/v1/admin/orders?q=TEST&size=100', { admin: true });
  const cibles = (r.json?.content || []).filter(o =>
    o.customerName.startsWith('TEST ') && ['PENDING', 'CONFIRMED'].includes(o.orderStatus));
  for (const o of cibles) {
    const a = await changerStatut(o.id, 'CANCELLED');
    verifier(`${o.orderNumber} (${o.customerName}) annulée, stock restitué`, a.status === 200, `HTTP ${a.status}`);
  }
  if (!cibles.length) console.log('  Aucune commande TEST annulable.');
}

// ---------------------------------------------------------------- tests
async function tester() {
  titre('1. Disponibilité');
  const h = await appel('GET', '/api/v1/health');
  if (!verifier('API joignable (/api/v1/health = UP)', h.status === 200 && h.json?.status === 'UP', `HTTP ${h.status}`)) {
    console.log(`\nL'API ne répond pas sur ${API} : démarrer le backend puis relancer.`); return;
  }
  verifier('Actuator health UP', (await appel('GET', '/actuator/health')).json?.status === 'UP');
  const cors = await appel('GET', '/api/v1/health', { entetes: { Origin: FRONT } });
  verifier(`CORS autorise ${FRONT}`, cors.headers.get('access-control-allow-origin') === FRONT);
  const corsPirate = await appel('GET', '/api/v1/health', { entetes: { Origin: 'https://site-pirate.example' } });
  verifier('CORS refuse une origine inconnue', !corsPirate.headers.get('access-control-allow-origin'));

  titre('2. Catalogue');
  const cats = await appel('GET', '/api/v1/categories');
  verifier('6 catégories actives', cats.json?.length === 6, `reçu ${cats.json?.length}`);
  const tous = await appel('GET', '/api/v1/products?size=100');
  for (const p of tous.json?.content || []) produits[p.slug] = p;
  verifier('Liste des produits', Object.keys(produits).length > 0, `${Object.keys(produits).length} produits`);
  const manquants = data.clients.flatMap(c => c.panier.map(l => l.slug)).filter(s => !produits[s]);
  if (!verifier('Tous les produits du jeu de test existent', !manquants.length, manquants.join(', '))) return;
  for (const c of cats.json) {
    const r = await appel('GET', `/api/v1/products?category=${c.slug}&size=100`);
    verifier(`Filtre catégorie « ${c.name} »`,
      r.status === 200 && r.json.content.every(p => p.categorySlug === c.slug),
      `${r.json?.content?.length} produits`);
  }
  for (const s of data.recherches) {
    const r = await appel('GET', `/api/v1/products?q=${encodeURIComponent(s.q)}&size=100`);
    const slugs = (r.json?.content || []).map(p => p.slug);
    verifier(`Recherche « ${s.q} »`, s.attendu.every(a => slugs.includes(a)) && (s.attendu.length || !slugs.length),
      `reçu [${slugs.join(', ')}]`);
  }
  const vedettes = await appel('GET', '/api/v1/products?featured=true&size=100');
  verifier('Filtre « produits vedettes »', vedettes.json?.content?.length > 0 && vedettes.json.content.every(p => p.featured));
  const p1 = await appel('GET', '/api/v1/products?size=5&page=0');
  const p2 = await appel('GET', '/api/v1/products?size=5&page=1');
  verifier('Pagination (pages distinctes)', p1.json?.content?.length === 5 &&
    p1.json.content[0].id !== p2.json?.content?.[0]?.id);
  const fiche = await appel('GET', '/api/v1/products/lait-corporel-nourrissant');
  verifier('Fiche produit par slug', fiche.status === 200 && fiche.json?.slug === 'lait-corporel-nourrissant');
  verifier('Produit inconnu → 404', (await appel('GET', '/api/v1/products/produit-qui-nexiste-pas')).status === 404);

  titre('3. Zones de livraison');
  const z = await appel('GET', '/api/v1/delivery-zones');
  for (const zone of z.json || []) zones[zone.name] = zone;
  verifier('Zones du jeu de test présentes', data.clients.every(c => zones[c.zone]),
    Object.keys(zones).join(', '));

  titre('4. Panier invité');
  const vide = await appel('GET', '/api/v1/cart');
  const jeton = vide.headers.get('x-cart-token');
  verifier('Un jeton X-Cart-Token est émis', !!jeton);
  const T = { 'X-Cart-Token': jeton };
  const lait = produits['lait-corporel-nourrissant']; const karite = produits['beurre-karite-brut'];
  let c = await appel('POST', '/api/v1/cart/items', { entetes: T, corps: { productId: lait.id, quantity: 2 } });
  verifier('Ajout de 2 laits corporels', c.status === 200 && c.json?.totalQuantity === 2, `HTTP ${c.status}`);
  c = await appel('POST', '/api/v1/cart/items', { entetes: T, corps: { productId: karite.id, quantity: 1 } });
  verifier('Sous-total calculé par le serveur',
    c.json?.subtotalXaf === lait.priceXaf * 2 + karite.priceXaf, `reçu ${c.json?.subtotalXaf}`);
  c = await appel('PATCH', `/api/v1/cart/items/${lait.id}`, { entetes: T, corps: { quantity: 3 } });
  verifier('Modification de quantité (3)', c.json?.items?.find(i => i.productId === lait.id)?.quantity === 3);
  c = await appel('DELETE', `/api/v1/cart/items/${karite.id}`, { entetes: T });
  verifier('Suppression d\'une ligne', c.status === 200 && c.json?.items?.length === 1);
  c = await appel('POST', '/api/v1/cart/items', { entetes: T, corps: { productId: lait.id, quantity: 9999 } });
  verifier('Quantité supérieure au stock refusée', c.status >= 400 && c.status < 500, `HTTP ${c.status}`);
  c = await appel('POST', '/api/v1/cart/items', { entetes: T, corps: { productId: lait.id, quantity: 0 } });
  verifier('Quantité 0 refusée', c.status === 400 || c.status === 422, `HTTP ${c.status}`);
  c = await appel('POST', '/api/v1/cart/items', { entetes: T, corps: { productId: '00000000-0000-0000-0000-000000000000', quantity: 1 } });
  verifier('Produit inconnu refusé', c.status >= 400 && c.status < 500, `HTTP ${c.status}`);
  const autre = await appel('GET', '/api/v1/cart', { entetes: { 'X-Cart-Token': 'jeton-invente' } });
  verifier('Un faux jeton ne donne pas accès au panier', !(autre.json?.items?.length), `HTTP ${autre.status}`);
  c = await appel('DELETE', '/api/v1/cart', { entetes: T });
  const apres = await appel('GET', '/api/v1/cart', { entetes: T });
  verifier('Vidage du panier', (c.status === 204 || c.status === 200) && !apres.json?.items?.length);

  titre('5. Administration — sécurité');
  verifier('Sans identifiants → 401', (await appel('GET', '/api/v1/admin/orders')).status === 401);
  const faux = await appel('GET', '/api/v1/admin/orders', {
    entetes: { Authorization: 'Basic ' + Buffer.from('pirate@example.com:mauvais').toString('base64') } });
  verifier('Mauvais identifiants → 401', faux.status === 401, `HTTP ${faux.status}`);
  const okAdmin = await appel('GET', '/api/v1/admin/orders?size=1', { admin: true });
  if (!verifier('Identifiants ADMIN du .env acceptés', okAdmin.status === 200, `HTTP ${okAdmin.status}`)) return;

  titre('6. Commandes clients');
  const stockAvant = await stockDe();
  const creees = {};
  for (const client of data.clients) {
    const corps = commandeDe(client);
    const r = await appel('POST', '/api/v1/orders', { corps });
    const sousTotal = client.panier.reduce((s, l) => s + produits[l.slug].priceXaf * l.quantite, 0);
    const ok = r.status === 201 || r.status === 200;
    verifier(`Commande ${client.customerName} (${client.zone})`, ok, `HTTP ${r.status} ${r.json?.detail || ''}`);
    if (!ok) continue;
    const o = r.json; creees[client.cle] = { ...o, client };
    verifier(`  ${o.orderNumber} : statut PENDING`, o.orderStatus === 'PENDING', o.orderStatus);
    verifier(`  sous-total ${sousTotal} FCFA`, o.subtotalXaf === sousTotal, `reçu ${o.subtotalXaf}`);
    verifier(`  frais de livraison ${zones[client.zone].deliveryFeeXaf} FCFA`,
      o.deliveryFeeXaf === zones[client.zone].deliveryFeeXaf, `reçu ${o.deliveryFeeXaf}`);
    verifier('  total = sous-total + livraison − remise',
      o.totalXaf === o.subtotalXaf + o.deliveryFeeXaf - o.discountXaf, `reçu ${o.totalXaf}`);
  }
  const stockApres = await stockDe();
  const conso = {};
  for (const k of Object.keys(creees)) for (const l of creees[k].client.panier) conso[l.slug] = (conso[l.slug] || 0) + l.quantite;
  verifier('Stock décrémenté du bon nombre pour chaque produit',
    Object.entries(conso).every(([s, q]) => stockAvant[s] - stockApres[s] === q),
    Object.entries(conso).map(([s, q]) => `${s}: ${stockAvant[s]}→${stockApres[s]} (−${q} attendu)`).join('; '));

  titre('7. Commandes invalides (doivent être refusées)');
  const base = commandeDe(data.clients[0]);
  const invalides = [
    ['Panier vide', { ...base, items: [] }],
    ['Quantité 0', { ...base, items: [{ productId: lait.id, quantity: 0 }] }],
    ['Produit en double', { ...base, items: [{ productId: lait.id, quantity: 1 }, { productId: lait.id, quantity: 1 }] }],
    ['Quantité supérieure au stock', { ...base, items: [{ productId: lait.id, quantity: 9999 }] }],
    ['Produit inexistant', { ...base, items: [{ productId: '00000000-0000-0000-0000-000000000000', quantity: 1 }] }],
    ['Zone de livraison inexistante', { ...base, deliveryZoneId: '00000000-0000-0000-0000-000000000000' }],
    ['Nom du client manquant', { ...base, customerName: '' }],
    ['Téléphone manquant', { ...base, customerPhone: '' }],
    ['E-mail invalide', { ...base, customerEmail: 'pas-un-email' }],
    ['Adresse incomplète (quartier vide)', { ...base, deliveryAddress: { ...base.deliveryAddress, neighborhood: '' } }],
  ];
  const stockAvantInvalides = await stockDe();
  for (const [nom, corps] of invalides) {
    const r = await appel('POST', '/api/v1/orders', { corps });
    verifier(nom, r.status >= 400 && r.status < 500, `HTTP ${r.status}`);
  }
  const triche = await appel('POST', '/api/v1/orders', { corps: {
    ...base, items: [{ productId: karite.id, quantity: 1, unitPriceXaf: 1 }], subtotalXaf: 1, totalXaf: 1, discountXaf: 99999 } });
  if (triche.json?.id) creees.triche = { ...triche.json, client: { ...data.clients[0], panier: [{ slug: karite.slug, quantite: 1 }] } };
  verifier('Prix envoyé par le client ignoré (recalculé par le serveur)',
    triche.json?.subtotalXaf === karite.priceXaf, `HTTP ${triche.status}, sous-total ${triche.json?.subtotalXaf}`);
  const stockApresInvalides = await stockDe();
  verifier('Aucun stock consommé par les commandes refusées',
    Object.keys(stockAvantInvalides).every(s => stockAvantInvalides[s] - stockApresInvalides[s] === (s === karite.slug && triche.json?.id ? 1 : 0)));

  titre('8. Suivi public de commande');
  const a = creees['awa-yaounde'];
  if (a) {
    const variantes = ['+237 6 90 11 22 01', '690112201', '00237690112201', '237 690-11-22-01'];
    for (const tel of variantes) {
      const r = await appel('POST', '/api/v1/orders/track', { corps: { orderNumber: a.orderNumber, phone: tel } });
      verifier(`Suivi avec le téléphone « ${tel} »`, r.status === 200 && r.json?.orderNumber === a.orderNumber, `HTTP ${r.status}`);
    }
    const r = await appel('POST', '/api/v1/orders/track', { corps: { orderNumber: a.orderNumber, phone: '690112201' } });
    const fuite = ['Bastos', 'awa.test@example.com', '690112201', 'Rue 1.750'].filter(x => r.texte.includes(x));
    verifier('Le suivi n\'expose ni adresse, ni e-mail, ni téléphone', !fuite.length, `trouvé : ${fuite.join(', ')}`);
    verifier('Mauvais téléphone → 404', (await appel('POST', '/api/v1/orders/track', {
      corps: { orderNumber: a.orderNumber, phone: '699999999' } })).status === 404);
    verifier('Numéro inconnu → 404', (await appel('POST', '/api/v1/orders/track', {
      corps: { orderNumber: 'LC-20000101-XXXXXX', phone: '690112201' } })).status === 404);
  }

  titre('9. Administration des commandes');
  const liste = await appel('GET', '/api/v1/admin/orders?q=TEST&size=100', { admin: true });
  verifier('Recherche « TEST » retrouve les commandes créées',
    Object.values(creees).every(o => liste.json?.content?.some(x => x.id === o.id)));
  const filtre = await appel('GET', '/api/v1/admin/orders?status=PENDING&size=100', { admin: true });
  verifier('Filtre par statut PENDING', filtre.json?.content?.every(o => o.orderStatus === 'PENDING'));
  const detail = a && await appel('GET', `/api/v1/admin/orders/${a.id}`, { admin: true });
  if (detail) verifier('Détail admin : coordonnées et adresse visibles',
    detail.json?.customerEmail === 'awa.test@example.com' && detail.json?.deliveryAddress?.neighborhood === 'Bastos');

  const pc = creees['parcours-complet'];
  if (pc) {
    verifier('Transition interdite PENDING → DELIVERED refusée', (await changerStatut(pc.id, 'DELIVERED')).status === 422);
    const suivi = 'TEST-' + Date.now();
    for (const [st, tn] of [['CONFIRMED'], ['PROCESSING'], ['SHIPPED', suivi], ['DELIVERED']]) {
      const r = await changerStatut(pc.id, st, tn);
      verifier(`${pc.orderNumber} → ${st}`, r.status === 200 && r.json?.orderStatus === st, `HTTP ${r.status} ${r.json?.detail || ''}`);
      if (st === 'SHIPPED') verifier('  numéro de suivi enregistré', r.json?.shipment?.trackingNumber === suivi);
      const pub = await appel('POST', '/api/v1/orders/track', { corps: { orderNumber: pc.orderNumber, phone: pc.client.customerPhone } });
      verifier(`  visible immédiatement dans le suivi public (${st})`, pub.json?.orderStatus === st);
    }
    verifier('Retour à PENDING refusé', (await changerStatut(pc.id, 'PENDING')).status === 422);
    verifier('Annulation d\'une commande livrée refusée', (await changerStatut(pc.id, 'CANCELLED')).status === 422);
  }

  const an = creees.annulation;
  if (an) {
    const s0 = await stockDe();
    const r = await changerStatut(an.id, 'CANCELLED');
    verifier(`${an.orderNumber} → CANCELLED`, r.status === 200 && r.json?.orderStatus === 'CANCELLED', `HTTP ${r.status}`);
    const s1 = await stockDe();
    verifier('Stock restitué après annulation',
      an.client.panier.every(l => s1[l.slug] - s0[l.slug] === l.quantite),
      an.client.panier.map(l => `${l.slug}: ${s0[l.slug]}→${s1[l.slug]}`).join('; '));
    await changerStatut(an.id, 'CANCELLED');
    const s2 = await stockDe();
    verifier('Une 2e annulation ne restitue pas le stock deux fois',
      an.client.panier.every(l => s2[l.slug] === s1[l.slug]));
  }

  titre('10. Pages du frontend');
  for (const page of ['/', '/boutique', '/boutique/lait-corporel-nourrissant', '/panier', '/commande', '/suivi-commande', '/admin', '/admin/commandes', '/admin/produits']) {
    try {
      const r = await appel('GET', page, { base: FRONT });
      verifier(`${FRONT}${page}`, r.status === 200, `HTTP ${r.status}`);
    } catch (e) { verifier(`${FRONT}${page}`, false, e.cause?.code || e.message); }
  }

  // Récapitulatif réutilisable pour les tests manuels dans le navigateur
  const recap = Object.values(creees).map(o => ({
    client: o.client.customerName, numero: o.orderNumber, telephone: o.client.customerPhone,
    totalXaf: o.totalXaf, id: o.id }));
  writeFileSync(join(ici, 'commandes-creees.json'), JSON.stringify(recap, null, 2));
  console.log('\nCommandes TEST créées (aussi dans tests-e2e/commandes-creees.json) :');
  console.table(recap.map(({ id, ...r }) => r));
}

// ---------------------------------------------------------------- lancement
try {
  if (process.argv.includes('--nettoyer')) await nettoyer(); else await tester();
} catch (e) {
  verifier('Exécution du script', false, e.cause?.code || e.message);
}
const ko = resultats.filter(r => !r.ok);
console.log(`\n\x1b[1mRésultat : ${resultats.length - ko.length}/${resultats.length} vérifications réussies\x1b[0m`);
if (ko.length) { console.log('Échecs :'); for (const r of ko) console.log(`  - [${r.section}] ${r.nom} ${r.detail}`); }
process.exit(ko.length ? 1 : 0);
