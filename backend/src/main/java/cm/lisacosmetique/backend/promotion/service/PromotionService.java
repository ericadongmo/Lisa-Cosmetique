package cm.lisacosmetique.backend.promotion.service;

import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.promotion.entity.DiscountType;
import cm.lisacosmetique.backend.promotion.entity.Promotion;
import cm.lisacosmetique.backend.promotion.repository.PromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Une seule promotion s'applique par commande : la plus avantageuse pour le
 * client parmi celles actives à la date de la commande. Pas de cumul.
 */
@Service
@Profile("!nodb")
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public record AppliedDiscount(long amountXaf, Promotion promotion) {

        public static AppliedDiscount none() {
            return new AppliedDiscount(0L, null);
        }

        public boolean isApplied() {
            return promotion != null && amountXaf > 0;
        }
    }

    /**
     * @param lineTotals montant par produit (prix unitaire × quantité), prix
     *                   issus de la base uniquement.
     */
    public AppliedDiscount bestDiscount(Map<UUID, Long> lineTotals, Instant now) {
        List<Promotion> activePromotions = promotionRepository.findActiveAt(now);
        AppliedDiscount best = AppliedDiscount.none();

        for (Promotion promotion : activePromotions) {
            long eligibleBase = eligibleBase(promotion, lineTotals);
            if (eligibleBase <= 0) {
                continue;
            }
            long discount = computeDiscount(promotion, eligibleBase);
            if (discount > best.amountXaf()) {
                best = new AppliedDiscount(discount, promotion);
            }
        }
        return best;
    }

    private static long eligibleBase(Promotion promotion, Map<UUID, Long> lineTotals) {
        if (promotion.getProducts().isEmpty()) {
            return lineTotals.values().stream().mapToLong(Long::longValue).sum();
        }
        long base = 0L;
        for (Product product : promotion.getProducts()) {
            Long lineTotal = lineTotals.get(product.getId());
            if (lineTotal != null) {
                base += lineTotal;
            }
        }
        return base;
    }

    private static long computeDiscount(Promotion promotion, long eligibleBase) {
        long discount;
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = promotion.getDiscountValue()
                    .multiply(BigDecimal.valueOf(eligibleBase))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        } else {
            discount = Math.min(promotion.getDiscountValue().longValueExact(), eligibleBase);
        }
        return Math.max(0L, Math.min(discount, eligibleBase));
    }
}
