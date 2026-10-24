package com.example.learning.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A single range filter clause. Bounds are optional individually, but at least one of
 * {@code gte}/{@code lte}/{@code gt}/{@code lt} must be present (validated in the service/controller
 * layer, mapped to HTTP 400). {@code gte}/{@code lte} are inclusive; {@code gt}/{@code lt} are
 * exclusive.
 *
 * <p>{@code type} selects how Elasticsearch interprets the bounds:
 * <ul>
 *     <li>{@code "number"} — numeric range (e.g. {@code price}, {@code rating}); bounds parsed as doubles</li>
 *     <li>{@code "date"} — date range (e.g. {@code releaseDate}); bounds are {@code yyyy-MM-dd} strings</li>
 * </ul>
 *
 * @param field the document field to filter (e.g. "price", "rating", "releaseDate")
 * @param type  "number" or "date"
 * @param gte   inclusive lower bound (optional)
 * @param lte   inclusive upper bound (optional)
 * @param gt    exclusive lower bound (optional)
 * @param lt    exclusive upper bound (optional)
 */
public record RangeFilter(
        @NotBlank(message = "field is required")
        String field,

        @NotNull(message = "type is required")
        @Pattern(regexp = "number|date", message = "type must be 'number' or 'date'")
        String type,

        String gte,
        String lte,
        String gt,
        String lt
) {
    /** True when at least one bound is set (non-null, non-blank). */
    public boolean hasAnyBound() {
        return isSet(gte) || isSet(lte) || isSet(gt) || isSet(lt);
    }

    private static boolean isSet(String bound) {
        return bound != null && !bound.isBlank();
    }
}
