-- Le mode de paiement est choisi lors de l'initialisation du paiement, pas à la
-- création de la commande. Le paiement porte déjà son propre payment_method.
alter table orders alter column payment_method drop not null;
