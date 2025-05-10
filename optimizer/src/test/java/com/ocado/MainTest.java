package com.ocado;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MainTest {

    private ByteArrayOutputStream outputStream;
    private SecurityManager originalSecurityManager;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void tearDown() {
        System.setSecurityManager(originalSecurityManager);
        System.setOut(System.out);
    }

    @Test
    void testMainWithValidFiles() throws IOException {
        // Create temporary JSON files
        Path ordersPath = Files.createTempFile("orders", ".json");
        Path methodsPath = Files.createTempFile("paymentmethods", ".json");

        String ordersJson = """
                [
                    {"id": "ORDER1", "value": "100.00", "promotions": ["mZysk"]},
                    {"id": "ORDER2", "value": "200.00", "promotions": ["BosBankrut"]}
                ]
                """;
        String methodsJson = """
                [
                    {"id": "PUNKTY", "discount": 15, "limit": "100.00"},
                    {"id": "mZysk", "discount": 10, "limit": "180.00"},
                    {"id": "BosBankrut", "discount": 5, "limit": "200.00"}
                ]
                """;

        Files.writeString(ordersPath, ordersJson);
        Files.writeString(methodsPath, methodsJson);

        Main.main(new String[]{ordersPath.toString(), methodsPath.toString()});

        // Expected output as a set to ignore order
        Set<String> expectedOutput = Set.of(
                "mZysk 90.00",
                "BosBankrut 190.00"
        );
        // Filter out log messages and compare as a set
        Set<String> actualOutput = outputStream.toString().trim().lines()
                .filter(line -> !line.contains("INFO com.ocado.Main"))
                .collect(Collectors.toSet());

        assertEquals(expectedOutput, actualOutput);

        // Clean up
        Files.delete(ordersPath);
        Files.delete(methodsPath);
    }

    @Test
    void testMainWithInvalidJsonThrowsException() throws IOException {
        // Create a temp file with invalid JSON
        Path invalidPath = Files.createTempFile("invalid", ".json");
        Files.writeString(invalidPath, "invalid json");
        assertThrows(Exception.class, () -> Main.main(new String[]{invalidPath.toString(), invalidPath.toString()}));
        Files.delete(invalidPath);
    }

}