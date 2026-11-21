# Crédits des photos de démonstration

Photos utilisées pour les produits de test. Toutes sont sous licence **CC0** ou **Public Domain Mark** : usage commercial autorisé, sans obligation de citer l’auteur. Elles sont listées ici par transparence.

Trouvées via [Openverse](https://openverse.org). À remplacer par les vraies photos des produits avant l’ouverture au public.

| Produit | Licence | Auteur | Source |
|---|---|---|---|
| apres-shampoing-demelant | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/6076096/body-wash-shampoo-and-conditioner) |
| bonnet-satin | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5904541/photo-image-background-texture-public-domain) |
| closure-frontal-13x4 | CC0 1.0 (domaine public) | Yam B Chhetri | [wordpress](https://wordpress.org/photos/photo/28267ffd87/) |
| creme-hydratante-karite | CC0 1.0 (domaine public) | Dana Tentis | [stocksnap](https://stocksnap.io/photo/oil-coconut-QYF5HNIQ7Z) |
| fer-lisser-ceramique | CC0 1.0 (domaine public) | kathrynwoodheadjisc | [flickr](https://www.flickr.com/photos/194234669@N04/51617571286) |
| fond-de-teint-fluide | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5947501/free-public-domain-cc0-photo) |
| gommage-sucre-vanille | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5941887/free-public-domain-cc0-photo) |
| huile-ricin-pure | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5919429/close-bottle-beard-oil) |
| huile-seche-monoi | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/11515770/dropper-bottle-blank-retro-label) |
| lait-corporel-nourrissant | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5944151/free-public-domain-cc0-photo) |
| mascara-volume-intense | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5969582/eyeliner-mascara) |
| masque-capillaire-avocat | CC0 1.0 (domaine public) | Krzysztof%20Puszczy%u0144ski | [stocksnap](https://stocksnap.io/photo/avocado-fruit-GM3FAABVJ8) |
| meches-deep-wave-3-bundles | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/6023211/photo-image-texture-public-domain-women) |
| meches-naturelles-3-bundles | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/3299149/free-photo-image-woman-sweatshirt-face-women-animals-images-pictures) |
| meches-tresser-xpression | CC0 1.0 (domaine public) | Candace McDaniel | [stocksnap](https://stocksnap.io/photo/braided-hair-EH7GKJMHBV) |
| palette-fards-12-teintes | CC0 1.0 (domaine public) | Beauty and Fashion | [stocksnap](https://stocksnap.io/photo/beauty-makeup-MKGGIJ360Q) |
| peigne-demelant | CC0 1.0 (domaine public) | nationalgalleryofart | [rawpixel](https://www.rawpixel.com/image/3391888/free-illustration-image-antique-art-artwork) |
| perruque-bob-carre | CC0 1.0 (domaine public) | tiffanyartistmodel | [flickr](https://www.flickr.com/photos/194724223@N02/55233135019) |
| perruque-body-wave | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/3292320/free-photo-image-face-apparel-back) |
| perruque-bouclee | CC0 1.0 (domaine public) | Jonas Svidras | [stocksnap](https://stocksnap.io/photo/girl-autumn-KVJUCEE9JY) |
| rouge-levres-mat | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/5916923/image-background-public-domain-blue) |
| serum-eclat-vitamine-c | CC0 1.0 (domaine public) | Authentic Stock | [stocksnap](https://stocksnap.io/photo/beauty-skincare-0Q23V7QFR8) |
| set-12-pinceaux | CC0 1.0 (domaine public) | Pawel Kadysz | [stocksnap](https://stocksnap.io/photo/makeup-brush-IJSBS0EFPO) |
| shampoing-hydratant | CC0 1.0 (domaine public) | inconnu | [rawpixel](https://www.rawpixel.com/image/6076096/body-wash-shampoo-and-conditioner) |

## Réimporter ces photos (nouvelle base, par exemple en production)

Le backend doit tourner et les produits de test doivent exister (profil `dev`).

```bash
node infrastructure/demo-images/upload.mjs infrastructure/demo-images backend/.env http://localhost:8080
```

Le script ignore les produits qui ont déjà une photo.
