package com.example.learning.filtering.data;

import com.example.learning.filtering.domain.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The deterministic sample catalog used throughout Chapter 05. Values are chosen so that every
 * filter produces an observable, reproducible result set:
 * <ul>
 *     <li>{@code category}/{@code brand} are keyword values for exact {@code term}/{@code terms}.</li>
 *     <li>{@code price} has a value exactly at {@code 1000.00} ({@code p06}) so inclusive
 *         ({@code gte:1000}) vs exclusive ({@code gt:1000}) boundaries differ observably.</li>
 *     <li>{@code rating} has a value exactly at {@code 4.0} ({@code p03}) for gt/lt boundary demos.</li>
 *     <li>{@code releaseDate} spans 2023-2025 for date-range filtering.</li>
 *     <li>{@code inStock} mixes true/false for a boolean {@code term} filter.</li>
 *     <li>{@code discountPercent} is {@code null} for some products ({@code p02,p04,p07,p09}) so
 *         {@code exists} (and its complement) are meaningful.</li>
 *     <li>{@code name}/{@code description-like} words (e.g. {@code gaming}, {@code pro}) let a
 *         full-text {@code match} run in <em>query context</em> alongside unscored filters.</li>
 * </ul>
 * Ids are explicit ({@code p01}..{@code p10}) for reproducible tests and cURL examples.
 */
public final class SampleProducts {

    private SampleProducts() {
    }

    public static List<Product> all() {
        return List.of(
                new Product("p01", "Apple MacBook Pro", "laptop", "Apple",
                        new BigDecimal("1999.00"), 4.8, LocalDate.parse("2024-03-10"), true, 10),
                new Product("p02", "Apple MacBook Air", "laptop", "Apple",
                        new BigDecimal("1299.00"), 4.6, LocalDate.parse("2024-07-22"), true, null),
                new Product("p03", "Dell XPS Laptop", "laptop", "Dell",
                        new BigDecimal("1499.00"), 4.0, LocalDate.parse("2023-11-05"), false, 15),
                new Product("p04", "Lenovo ThinkPad", "laptop", "Lenovo",
                        new BigDecimal("1399.00"), 4.3, LocalDate.parse("2023-02-18"), true, null),
                new Product("p05", "Samsung Gaming Laptop", "laptop", "Samsung",
                        new BigDecimal("1799.00"), 4.5, LocalDate.parse("2025-01-30"), false, 20),
                new Product("p06", "Dell Office Monitor", "monitor", "Dell",
                        new BigDecimal("1000.00"), 4.1, LocalDate.parse("2024-05-12"), true, 5),
                new Product("p07", "Samsung Gaming Monitor", "monitor", "Samsung",
                        new BigDecimal("329.00"), 4.7, LocalDate.parse("2025-03-08"), true, null),
                new Product("p08", "Logitech Wireless Mouse", "accessory", "Logitech",
                        new BigDecimal("29.99"), 4.2, LocalDate.parse("2023-09-01"), true, 25),
                new Product("p09", "Logitech Mechanical Keyboard", "accessory", "Logitech",
                        new BigDecimal("89.99"), 3.9, LocalDate.parse("2024-12-15"), false, null),
                new Product("p10", "Apple Magic Keyboard", "accessory", "Apple",
                        new BigDecimal("99.00"), 4.4, LocalDate.parse("2025-06-20"), true, 10)
        );
    }
}
