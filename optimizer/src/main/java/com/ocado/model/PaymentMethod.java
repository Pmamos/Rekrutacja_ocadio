package com.ocado.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a payment method with an identifier, discount percentage, and available limit.
 * The limit can be modified to track usage during order processing.
 */
public class PaymentMethod {
    private final String id;
    private final int discount;
    private BigDecimal limit;

    /**
     * Constructs a PaymentMethod with the specified id, discount, and limit.
     *
     * @param id       The unique identifier of the payment method (e.g., "PUNKTY", "mZysk").
     * @param discount The percentage discount (0-100).
     * @param limit    The maximum available amount (non-negative, two decimal places).
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    @JsonCreator
    public PaymentMethod(
            @JsonProperty("id") String id,
            @JsonProperty("discount") int discount,
            @JsonProperty("limit") String limit) {
        validateId(id);
        validateDiscount(discount);
        this.id = id;
        this.discount = discount;
        this.limit = parseAndValidateLimit(limit);
    }

    /**
     * Validates the payment method ID.
     *
     * @param id The ID to validate.
     * @throws IllegalArgumentException if ID is null or empty.
     */
    private void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method ID cannot be null or empty");
        }
    }

    /**
     * Validates the discount percentage.
     *
     * @param discount The discount to validate.
     * @throws IllegalArgumentException if discount is negative or exceeds 100.
     */
    private void validateDiscount(int discount) {
        if (discount < 0 || discount > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100, got: " + discount);
        }
    }

    /**
     * Parses and validates the limit amount.
     *
     * @param limit The limit as a string (from JSON).
     * @return The validated BigDecimal limit.
     * @throws IllegalArgumentException if limit is invalid.
     */
    private BigDecimal parseAndValidateLimit(String limit) {
        if (limit == null) {
            throw new IllegalArgumentException("Limit cannot be null");
        }
        try {
            BigDecimal value = new BigDecimal(limit).setScale(2, RoundingMode.HALF_UP);
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Limit cannot be negative, got: " + limit);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid limit format: " + limit, e);
        }
    }

    /**
     * Gets the payment method ID.
     *
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the discount percentage.
     *
     * @return The discount.
     */
    public int getDiscount() {
        return discount;
    }

    /**
     * Gets the available limit.
     *
     * @return The limit.
     */
    public BigDecimal getLimit() {
        return limit;
    }

    /**
     * Sets the available limit, ensuring it is non-negative and properly scaled.
     *
     * @param newLimit The new limit value.
     * @throws IllegalArgumentException if the new limit is negative or null.
     */
    public void setLimit(BigDecimal newLimit) {
        if (newLimit == null) {
            throw new IllegalArgumentException("New limit cannot be null");
        }
        BigDecimal scaledLimit = newLimit.setScale(2, RoundingMode.HALF_UP);
        if (scaledLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Limit cannot.small be negative, got: " + scaledLimit);
        }
        this.limit = scaledLimit;
    }

    @Override
    public String toString() {
        return "PaymentMethod{id='" + id + "', discount=" + discount + ", limit=" + limit + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return discount == that.discount &&
               Objects.equals(id, that.id) &&
               Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, discount, limit);
    }
}