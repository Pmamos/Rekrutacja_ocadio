package com.ocado.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocado.model.Order;
import com.ocado.model.PaymentMethod;

/**
 * Service to optimize payment methods for orders, maximizing total discount while preferring loyalty points.
 */
public class OptimizerService {
    private static final Logger logger = LoggerFactory.getLogger(OptimizerService.class);
    private static final String POINTS_METHOD = "PUNKTY";
    private static final BigDecimal MIN_POINTS_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal PARTIAL_POINTS_DISCOUNT = new BigDecimal("0.90"); // 10% discount
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final Map<String, PaymentMethod> methodMap;
    private final Map<String, BigDecimal> methodUsage = new HashMap<>();
    private final PaymentMethod points;
    private final Map<Order, BigDecimal> discountCache = new HashMap<>();

    /**
     * Initializes the optimizer with available payment methods.
     *
     * @param methods List of payment methods.
     */
    public OptimizerService(List<PaymentMethod> methods) {
        methodMap = new HashMap<>();
        for (PaymentMethod method : methods) {
            methodMap.put(method.getId(), method);
        }
        points = methodMap.get(POINTS_METHOD);
        if (points == null) {
            methodMap.put(POINTS_METHOD, new PaymentMethod(POINTS_METHOD, 0, "0.00"));

        }
    }

    /**
     * Processes a list of orders, assigning optimal payment methods.
     *
     * @param orders List of orders to process.
     */
    public void processOrders(List<Order> orders) {
        // Calculate discounts for all orders and log them
        discountCache.clear();
        for (Order order : orders) {
            BigDecimal maxDiscount = calculateMaxDiscount(order);
            discountCache.put(order, maxDiscount);
        }

        // Sort orders by maximum potential discount (descending)
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparing(order -> discountCache.get(order), Comparator.reverseOrder()));

        // Try assigning payment methods
        if (!assignPayments(sortedOrders, new HashMap<>(methodMap), new ArrayList<>())) {
            throw new IllegalStateException("No valid payment combination found for all orders");
        }
    }
    /**
     * Calculates the maximum possible discount for an order.
     *
     * @param order The order.
     * @return The maximum discount amount.
     */
    private BigDecimal calculateMaxDiscount(Order order) {
        BigDecimal orderValue = order.getValue();
        List<BigDecimal> discounts = new ArrayList<>();

        // Card promotions
        if (order.getPromotions() != null) {
            for (String promo : order.getPromotions()) {
                PaymentMethod method = methodMap.get(promo);
                if (method != null && method.getLimit().compareTo(orderValue) >= 0) {
                    BigDecimal discount = orderValue.multiply(BigDecimal.valueOf(method.getDiscount()))
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                    discounts.add(discount);
                }
            }
        }

        // Full points
        if (points != null && points.getLimit().compareTo(orderValue) >= 0) {
            BigDecimal discount = orderValue.multiply(BigDecimal.valueOf(points.getDiscount()))
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            discounts.add(discount);
        }

        // Partial points
        BigDecimal minPoints = orderValue.multiply(MIN_POINTS_PERCENT).setScale(2, RoundingMode.HALF_UP);
        if (points != null && points.getLimit().compareTo(minPoints) >= 0) {
            BigDecimal discount = orderValue.multiply(BigDecimal.ONE.subtract(PARTIAL_POINTS_DISCOUNT));
            discounts.add(discount);
        }

        return discounts.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    /**
     * Recursively assigns payment methods to orders, backtracking if necessary.
     *
     * @param orders      Remaining orders to process.
     * @param limits      Current available limits for payment methods.
     * @param assignments Current payment assignments.
     * @return True if a valid assignment is found, false otherwise.
     */
    private boolean assignPayments(List<Order> orders, Map<String, PaymentMethod> limits, List<PaymentAssignment> assignments) {
        if (orders.isEmpty()) {
            for (PaymentAssignment assignment : assignments) {
                applyPayment(assignment);
            }
            return true;
        }

        Order order = orders.get(0);
        List<Order> remainingOrders = orders.subList(1, orders.size());
        BigDecimal orderValue = order.getValue();
        Map<String, PaymentOption> options = new LinkedHashMap<>();

        // Option 1: Full payment with a card (with promotion)
        if (order.getPromotions() != null) {
            for (String promo : order.getPromotions()) {
                PaymentMethod method = limits.get(promo);
                if (method != null && method.getLimit().compareTo(orderValue) >= 0) {
                    BigDecimal discountPercent = BigDecimal.valueOf(100 - method.getDiscount())
                            .divide(HUNDRED, 4, RoundingMode.HALF_UP);
                    BigDecimal discountedValue = orderValue.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
                    options.put(method.getId(), new PaymentOption(method.getId(), discountedValue, orderValue, 2)); // Karta: priorytet 2
                }
            }
        }

        // Option 2: Full payment with loyalty points
        if (points != null && limits.get(POINTS_METHOD).getLimit().compareTo(orderValue) >= 0) {
            BigDecimal discountPercent = BigDecimal.valueOf(100 - points.getDiscount())
                    .divide(HUNDRED, 4, RoundingMode.HALF_UP);
            BigDecimal discountedValue = orderValue.multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
            options.put(POINTS_METHOD, new PaymentOption(POINTS_METHOD, discountedValue, orderValue, 1)); // Punkty: priorytet 1
        }

        // Option 3: Partial payment with points (≥10%) + card (no promo)
        if (points != null && limits.get(POINTS_METHOD).getLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal minPoints = orderValue.multiply(MIN_POINTS_PERCENT).setScale(2, RoundingMode.HALF_UP);
            if (limits.get(POINTS_METHOD).getLimit().compareTo(minPoints) >= 0) {
                BigDecimal discountedValue = orderValue.multiply(PARTIAL_POINTS_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
                BigDecimal pointsUsed = minPoints.min(limits.get(POINTS_METHOD).getLimit());
                BigDecimal remaining = discountedValue.subtract(pointsUsed);

                Optional<PaymentMethod> backupCard = limits.values().stream()
                        .filter(m -> !m.getId().equals(POINTS_METHOD))
                        .filter(m -> m.getLimit().compareTo(remaining) >= 0)
                        .findFirst();

                if (backupCard.isPresent()) {
                    String key = POINTS_METHOD + "+" + backupCard.get().getId();
                    options.put(key, new PaymentOption(key, discountedValue, pointsUsed, remaining, 0)); // Częściowe punkty: priorytet 0
                }
            }
        }

        // Try each option in order of discount and priority
        List<PaymentOption> sortedOptions = options.values().stream()
                .sorted(Comparator.comparing(PaymentOption::getTotalAmount)
                        .thenComparingInt(PaymentOption::getPriority).reversed()) // Niższy priorytet = lepszy
                .toList();

        for (PaymentOption option : sortedOptions) {

            // Apply option and update limits
            Map<String, PaymentMethod> newLimits = new HashMap<>();
            for (PaymentMethod m : limits.values()) {
                newLimits.put(m.getId(), new PaymentMethod(m.getId(), m.getDiscount(), m.getLimit().toString()));
            }

            if (option.isPartialPoints()) {
                String[] methods = option.methodId.split("\\+");
                String pointsMethod = methods[0];
                String cardMethod = methods[1];

                newLimits.get(pointsMethod).setLimit(newLimits.get(pointsMethod).getLimit().subtract(option.pointsAmount));
                newLimits.get(cardMethod).setLimit(newLimits.get(cardMethod).getLimit().subtract(option.cardAmount));
            } else {
                newLimits.get(option.methodId).setLimit(newLimits.get(option.methodId).getLimit().subtract(option.originalAmount));
            }

            // Prune: Check if remaining funds can cover remaining orders
            BigDecimal remainingFunds = newLimits.values().stream()
                    .map(PaymentMethod::getLimit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainingOrderValue = remainingOrders.stream()
                    .map(Order::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (remainingFunds.compareTo(remainingOrderValue.multiply(PARTIAL_POINTS_DISCOUNT)) < 0) {
                continue;
            }

            // Recurse with updated limits
            assignments.add(new PaymentAssignment(order.getId(), option));
            if (assignPayments(remainingOrders, newLimits, assignments)) {
                return true;
            }
            assignments.remove(assignments.size() - 1);
        }

        return false;
    }

    /**
     * Applies a payment assignment to the method usage.
     *
     * @param assignment The payment assignment.
     */
    private void applyPayment(PaymentAssignment assignment) {
        PaymentOption option = assignment.option;
        if (option.isPartialPoints()) {
            String[] methods = option.methodId.split("\\+");
            String pointsMethod = methods[0];
            String cardMethod = methods[1];

            methodUsage.merge(pointsMethod, option.pointsAmount, BigDecimal::add);
            methodUsage.merge(cardMethod, option.cardAmount, BigDecimal::add);

            methodMap.get(pointsMethod).setLimit(methodMap.get(pointsMethod).getLimit().subtract(option.pointsAmount));
            methodMap.get(cardMethod).setLimit(methodMap.get(cardMethod).getLimit().subtract(option.cardAmount));

        } else {
            methodUsage.merge(option.methodId, option.totalAmount, BigDecimal::add);
            methodMap.get(option.methodId).setLimit(methodMap.get(option.methodId).getLimit().subtract(option.originalAmount));
        }
    }

    /**
     * Prints the summary of used payment methods and amounts.
     */
    public void printSummary() {
        methodUsage.forEach((methodId, amount) ->
                System.out.println(methodId + " " + amount.setScale(2, RoundingMode.HALF_UP)));
    }

    /**
     * Represents a payment option with its cost and priority.
     */
    private static class PaymentOption {
        final String methodId;
        final BigDecimal totalAmount; // Amount paid after discount
        final BigDecimal originalAmount; // Original order value (for limit deduction)
        final BigDecimal pointsAmount; // Points used (for partial payment)
        final BigDecimal cardAmount; // Card amount (for partial payment)
        final int priority; // Lower priority = prefer (points > partial points > card)

        // Full payment with one method
        PaymentOption(String methodId, BigDecimal totalAmount, BigDecimal originalAmount, int priority) {
            this.methodId = methodId;
            this.totalAmount = totalAmount;
            this.originalAmount = originalAmount;
            this.pointsAmount = BigDecimal.ZERO;
            this.cardAmount = BigDecimal.ZERO;
            this.priority = priority;
        }

        // Partial payment with points + card
        PaymentOption(String methodId, BigDecimal totalAmount, BigDecimal pointsAmount, BigDecimal cardAmount, int priority) {
            this.methodId = methodId;
            this.totalAmount = totalAmount;
            this.originalAmount = pointsAmount.add(cardAmount); // Not used in partial payment
            this.pointsAmount = pointsAmount;
            this.cardAmount = cardAmount;
            this.priority = priority;
        }

        BigDecimal getTotalAmount() {
            return totalAmount;
        }

        int getPriority() {
            return priority;
        }

        boolean isPartialPoints() {
            return pointsAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Represents an assignment of a payment option to an order.
     */
    private static class PaymentAssignment {
        final String orderId;
        final PaymentOption option;

        PaymentAssignment(String orderId, PaymentOption option) {
            this.orderId = orderId;
            this.option = option;
        }
    }
}