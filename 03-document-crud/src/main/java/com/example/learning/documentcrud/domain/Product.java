package com.example.learning.documentcrud.domain;

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
 */
@Document(indexName = "learning-products")
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

    public Product() {
    }

    public Product(String id, String name, String description, BigDecimal price, Integer quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
