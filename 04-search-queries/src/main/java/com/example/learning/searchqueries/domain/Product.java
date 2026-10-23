package com.example.learning.searchqueries.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * The Product document stored in the {@code search-products} index.
 *
 * <p>Field mappings are chosen to make query differences observable:
 * <ul>
 *     <li>{@code name}: {@code text} — analyzed, for full-text {@code match} queries</li>
 *     <li>{@code category}: {@code keyword} — not analyzed, for exact {@code term} queries</li>
 *     <li>{@code brand}: {@code keyword} — not analyzed, for exact {@code term} queries</li>
 *     <li>{@code description}: {@code text} — analyzed, for full-text {@code match} queries</li>
 *     <li>{@code price}: {@code double}; {@code quantity}: {@code integer}</li>
 * </ul>
 *
 * <p>Mutable entity (not a record): Spring Data Elasticsearch and the Java API Client (Jackson)
 * need a no-args constructor and setters for deserialization. Lombok supplies that boilerplate
 * with targeted annotations; the Elasticsearch mapping annotations stay on the fields.
 */
@Document(indexName = "search-products")
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

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer quantity;
}
