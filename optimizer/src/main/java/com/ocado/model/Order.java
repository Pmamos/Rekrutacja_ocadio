package com.ocado.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an order with an identifier, value, and optional list of applicable promotions.
 * Immutable to ensure consistency during processing.
 */
public class Order {
    private final String id;
    private final BigDecimal value;
    private final List<String> promotions;

    /**
     * Constructs an Order with the specified id, value, and promotions.
     *
     * @param id          The unique identifier of the order.
     * @param value       The order value as a string (e.g., "150.00").
     * @param promotions  The list of promotion IDs applicable to this order (optional, can be null).
     * @throws IllegalArgumentException if id or value is invalid.
     */
    @JsonCreator
    public Order(
            @JsonProperty("id") String id,
            @JsonProperty("value") String value,
            @JsonProperty("promotions") List<String> promotions) {
        validateId(id);
        this.id = id;
        this.value = parseAndValidateValue(value);
        this.promotions = promotions != null ? List.copyOf(promotions) : null; // Immutable copy or null
    }

    /**
     * Validates the order ID.
     *
     * @param id The ID to validate.
     * @throws IllegalArgumentException if ID is null or empty.
     */
    private void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
    }

    /**
     * Parses and validates the order value.
     *
     * @param value The value as a string (from JSON).
     * @return The validated BigDecimal value.
     * @throws IllegalArgumentException if value is invalid.
     */
    private BigDecimal parseAndValidateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Order value cannot be null");
        }
        try {
            BigDecimal parsedValue = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (parsedValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Order value must be positive, got: " + value);
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid order value format: " + value, e);
        }
    }

    /**
     * Gets the order ID.
     *
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the order value.
     *
     * @return The value.
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Gets the list of promotions, or null if none are applicable.
     *
     * @return The promotions list, or null.
     */
    public List<String> getPromotions() {
        return promotions != null ? Collections.unmodifiableList(promotions) : null;
    }

    @Override
    public String toString() {
        return "Order{id='" + id + "', value=" + value + ", promotions=" + promotions + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) &&
               Objects.equals(value, order.value) &&
               Objects.equals(promotions, order.promotions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, promotions);
    }
}