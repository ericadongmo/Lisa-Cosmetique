package cm.lisacosmetique.backend.support;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/** Remise à zéro de la base H2 entre les tests (pas de PostgreSQL disponible). */
public final class DatabaseCleaner {

    private static final List<String> TABLES = List.of(
            "payment_events", "payments", "order_items", "shipments", "orders",
            "cart_items", "carts", "promotion_products", "promotions",
            "product_images", "products", "categories", "addresses", "users",
            "delivery_zones");

    private DatabaseCleaner() {
    }

    public static void clean(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
