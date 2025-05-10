package com.ocado.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class OrderTest {

    @Test
    void testOrderCreation() {
        Order order = new Order("ORDER1", "100.00", List.of("mZysk"));
        assertEquals("ORDER1", order.getId());
        assertEquals(new BigDecimal("100.00"), order.getValue());
        assertEquals(List.of("mZysk"), order.getPromotions());
    }

    @Test
    void testOrderWithNullPromotions() {
        Order order = new Order("ORDER2", "50.00", null);
        assertEquals("ORDER2", order.getId());
        assertEquals(new BigDecimal("50.00"), order.getValue());
        assertNull(order.getPromotions());
    }


    @Test
    void testOrderImmutability() {
        List<String> promotions = new ArrayList<>(List.of("mZysk"));
        Order order = new Order("ORDER4", "200.00", promotions);
        promotions.add("BosBankrut");
        assertEquals(List.of("mZysk"), order.getPromotions(), "Promotions list should be immutable");
    }
}