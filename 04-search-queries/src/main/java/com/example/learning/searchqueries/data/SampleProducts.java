package com.example.learning.searchqueries.data;

import com.example.learning.searchqueries.domain.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * The deterministic sample catalog used throughout Chapter 04. Values are chosen so that term,
 * match, and bool queries produce observably different results:
 * <ul>
 *     <li>{@code category}/{@code brand} are keyword values for exact {@code term} matching
 *         (e.g. category {@code laptop} vs {@code monitor}; brand {@code Apple} vs {@code Dell}).</li>
 *     <li>{@code name}/{@code description} contain shared words ({@code wireless}, {@code gaming},
 *         {@code pro}) so {@code match} and {@code should} affect which documents match and their
 *         relevance.</li>
 * </ul>
 * Ids are explicit ({@code p01}..{@code p10}) for reproducible tests and cURL examples.
 */
public final class SampleProducts {

    private SampleProducts() {
    }

    public static List<Product> all() {
        return List.of(
                new Product("p01", "Apple MacBook Pro", "laptop", "Apple",
                        "14-inch pro laptop with M3 chip for professional work", new BigDecimal("1999.00"), 12),
                new Product("p02", "Apple MacBook Air", "laptop", "Apple",
                        "lightweight laptop with M3 chip, silent fanless design", new BigDecimal("1299.00"), 20),
                new Product("p03", "Dell XPS Laptop", "laptop", "Dell",
                        "premium ultrabook laptop with InfinityEdge display", new BigDecimal("1499.00"), 8),
                new Product("p04", "Lenovo ThinkPad", "laptop", "Lenovo",
                        "business laptop with legendary keyboard and durability", new BigDecimal("1399.00"), 15),
                new Product("p05", "Logitech Wireless Mouse", "accessory", "Logitech",
                        "wireless mouse with silent clicks and long battery life", new BigDecimal("29.99"), 100),
                new Product("p06", "Logitech Mechanical Keyboard", "accessory", "Logitech",
                        "wireless mechanical keyboard with hot-swappable switches", new BigDecimal("89.99"), 60),
                new Product("p07", "Samsung Gaming Monitor", "monitor", "Samsung",
                        "27-inch curved gaming monitor with 165Hz refresh rate", new BigDecimal("329.00"), 25),
                new Product("p08", "Dell Office Monitor", "monitor", "Dell",
                        "24-inch office monitor with adjustable stand for productivity", new BigDecimal("179.00"), 40),
                new Product("p09", "Apple Magic Keyboard", "accessory", "Apple",
                        "wireless keyboard with scissor mechanism for a quiet typing experience", new BigDecimal("99.00"), 35),
                new Product("p10", "Samsung Gaming Laptop", "laptop", "Samsung",
                        "gaming laptop with RTX graphics and high refresh display", new BigDecimal("1799.00"), 6)
        );
    }
}
