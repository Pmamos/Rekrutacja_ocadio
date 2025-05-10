package com.ocado.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.ocado.model.Order;
import com.ocado.model.PaymentMethod;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class OptimizerServiceTest {

    private OptimizerService service;
    private List<PaymentMethod> defaultMethods;
    private ByteArrayOutputStream outputStream;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        defaultMethods = List.of(
                new PaymentMethod("PUNKTY", 15, "100.00"),
                new PaymentMethod("mZysk", 10, "180.00"),
                new PaymentMethod("BosBankrut", 5, "200.00")
        );
        service = new OptimizerService(defaultMethods);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Configure log appender
        Logger logger = (Logger) LoggerFactory.getLogger(OptimizerService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    void testProcessOrdersWithDefaultInput() {
        List<Order> orders = List.of(
                new Order("ORDER1", "100.00", List.of("mZysk")),
                new Order("ORDER2", "200.00", List.of("BosBankrut")),
                new Order("ORDER3", "150.00", List.of("mZysk", "BosBankrut")),
                new Order("ORDER4", "50.00", null)
        );

        service.processOrders(orders);
        service.printSummary();

        // Po poprawkach priorytetów oczekujemy:
        Set<String> expectedOutput = Set.of(
                "mZysk 160.00",
                "BosBankrut 200.00",
                "PUNKTY 87.50"
        );
        Set<String> actualOutput = outputStream.toString().trim().lines().collect(Collectors.toSet());
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void testProcessOrdersWithPartialPoints() {
        List<Order> orders = List.of(
                new Order("ORDER1", "50.00", null)
        );
        List<PaymentMethod> methods = List.of(
                new PaymentMethod("PUNKTY", 15, "20.00"),
                new PaymentMethod("mZysk", 10, "50.00")
        );
        service = new OptimizerService(methods);

        service.processOrders(orders);
        service.printSummary();

        Set<String> expectedOutput = Set.of(
                "PUNKTY 5.00",
                "mZysk 40.00"
        );
        Set<String> actualOutput = outputStream.toString().trim().lines().collect(Collectors.toSet());
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void testProcessOrdersWithNoPoints() {
        List<Order> orders = List.of(
                new Order("ORDER1", "100.00", List.of("mZysk")),
                new Order("ORDER2", "50.00", List.of("BosBankrut"))
        );
        List<PaymentMethod> methods = List.of(
                new PaymentMethod("mZysk", 10, "100.00"),
                new PaymentMethod("BosBankrut", 5, "50.00")
        );
        service = new OptimizerService(methods);

        service.processOrders(orders);
        service.printSummary();

        Set<String> expectedOutput = Set.of(
                "mZysk 90.00",
                "BosBankrut 47.50"
        );
        Set<String> actualOutput = outputStream.toString().trim().lines().collect(Collectors.toSet());
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    void testProcessOrdersWithInsufficientFundsThrowsException() {
        List<Order> orders = List.of(
                new Order("ORDER1", "200.00", List.of("mZysk"))
        );
        List<PaymentMethod> methods = List.of(
                new PaymentMethod("mZysk", 10, "50.00")
        );
        service = new OptimizerService(methods);

        assertThrows(IllegalStateException.class, () -> service.processOrders(orders));
    }

    @Test
    void testProcessOrdersWithEmptyOrderList() {
        List<Order> orders = List.of();
        service.processOrders(orders);
        service.printSummary();
        assertEquals("", outputStream.toString().trim());
    }

    @Test
    void testProcessOrdersWithSingleOrderAndPOINTS() {
        List<Order> orders = List.of(
                new Order("ORDER1", "100.00", null)
        );
        List<PaymentMethod> methods = List.of(
                new PaymentMethod("PUNKTY", 15, "100.00")
        );
        service = new OptimizerService(methods);

        service.processOrders(orders);
        service.printSummary();

        String expectedOutput = "PUNKTY 85.00";
        assertEquals(expectedOutput, outputStream.toString().trim());
    }

    @Test
    void testProcessOrdersWithNoValidPaymentOptionsThrowsException() {
        List<Order> orders = List.of(
                new Order("ORDER1", "100.00", List.of("NonExistentPromo"))
        );
        List<PaymentMethod> methods = List.of(
                new PaymentMethod("PUNKTY", 15, "0.00")
        );
        service = new OptimizerService(methods);

        assertThrows(IllegalStateException.class, () -> service.processOrders(orders));
    }
}