package com.example.learning.filtering.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The Product document stored in the {@code filter-products} index.
 *
 * <p>Field mappings are chosen to make <em>filtering</em> observable:
 * <ul>
 *     <li>{@code name}: {@code text} — analyzed, for full-text {@code match} (query context)</li>
 *     <li>{@code category}: {@code keyword} — exact {@code term}/{@code terms} filter</li>
 *     <li>{@code brand}: {@code keyword} — exact {@code terms} filter (multi-value OR)</li>
 *     <li>{@code price}: {@code double} — numeric {@code range} filter (gte/lte boundaries)</li>
 *     <li>{@code rating}: {@code double} — numeric {@code range} filter (gt/lt exclusive boundaries)</li>
 *     <li>{@code releaseDate}: {@code date} (format {@code yyyy-MM-dd}) — date {@code range} filter</li>
 *     <li>{@code inStock}: {@code boolean} — boolean {@code term} filter</li>
 *     <li>{@code discountPercent}: {@code integer}, <strong>nullable</strong> — {@code exists} filter</li>
 * </ul>
 *
 * <p>Mutable entity (not a record): Spring Data Elasticsearch and the Java API Client (Jackson)
 * need a no-args constructor and setters for deserialization. Lombok supplies that boilerplate
 * with targeted annotations; the Elasticsearch mapping annotations stay on the fields.
 */
@Document(indexName = "filter-products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Date, format = DateFormat.date, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @Field(type = FieldType.Boolean)
    private Boolean inStock;

    /** Nullable on purpose: some products have no discount, so {@code exists} is meaningful. */
    @Field(type = FieldType.Integer)
    private Integer discountPercent;
}
