package com.ocado;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocado.model.Order;
import com.ocado.model.PaymentMethod;
import com.ocado.service.OptimizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            logger.error("Usage: java -jar app.jar <orders.json> <paymentmethods.json>");
            throw new IllegalArgumentException("Expected two arguments: orders file and payment methods file");
        }

        String ordersFile = args[0];
        String methodsFile = args[1];

        ObjectMapper mapper = new ObjectMapper();
        List<Order> orders;
        List<PaymentMethod> methods;

        try {
            orders = mapper.readValue(Files.readString(Paths.get(ordersFile)),
                    new TypeReference<List<Order>>() {});
            logger.info("Loaded {} orders from {}", orders.size(), ordersFile);
        } catch (IOException e) {
            logger.error("Failed to load orders from {}", ordersFile, e);
            throw e;
        }

        try {
            methods = mapper.readValue(Files.readString(Paths.get(methodsFile)),
                    new TypeReference<List<PaymentMethod>>() {});
            logger.info("Loaded {} payment methods from {}", methods.size(), methodsFile);
        } catch (IOException e) {
            logger.error("Failed to load payment methods from {}", methodsFile, e);
            throw e;
        }

        OptimizerService service = new OptimizerService(methods);
        service.processOrders(orders);
        service.printSummary();
    }
}