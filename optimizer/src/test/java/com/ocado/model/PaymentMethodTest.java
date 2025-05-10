package com.ocado.model;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class PaymentMethodTest {

    @Test
    void testPaymentMethodCreation() {
        PaymentMethod method = new PaymentMethod("mZysk", 10, "180.00");
        assertEquals("mZysk", method.getId());
        assertEquals(10, method.getDiscount());
        assertEquals(new BigDecimal("180.00"), method.getLimit());
    }

    @Test
    void testSetLimit() {
        PaymentMethod method = new PaymentMethod("PUNKTY", 15, "100.00");
        method.setLimit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("50.00"), method.getLimit());
    }

    @Test
    void testPaymentMethodWithZeroLimit() {
        PaymentMethod method = new PaymentMethod("BosBankrut", 5, "0.00");
        assertEquals(new BigDecimal("0.00"), method.getLimit());
    }

}