package com.example.learning.documentcrud.domain;

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
 * The Product document stored in the {@code learning-products} index.
 *
 * <p>Both clients operate on this same index and schema. The {@link Id} field maps to the
 * Elasticsearch document {@code _id} (the addressable key, used for real-time GET), while the
 * remaining fields are the {@code _source} (the actual JSON stored and returned).
 *
 * <p>Field mappings are chosen per data type:
 * <ul>
 *     <li>{@code name}: {@code text} for full-text search</li>
 *     <li>{@code description}: {@code text}</li>
 *     <li>{@code price}: {@code double}</li>
 *     <li>{@code quantity}: {@code integer}</li>
 * </ul>
 *
 * <p>This is a mutable entity, not a record: Spring Data Elasticsearch and the official Java API
 * Client (via Jackson) need a no-args constructor and setters to deserialize documents, and the
 * services set the id after a real-time GET. Lombok supplies that boilerplate with targeted
 * annotations while the Elasticsearch mapping annotations stay on the fields:
 * <ul>
 *     <li>{@code @Getter}/{@code @Setter} — accessors required for serialization/deserialization</li>
 *     <li>{@code @NoArgsConstructor} — required by the mapping/serialization frameworks</li>
 *     <li>{@code @AllArgsConstructor} — used by the services to build a Product from a request</li>
 * </ul>
 */
@Document(indexName = "learning-products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer quantity;
}
