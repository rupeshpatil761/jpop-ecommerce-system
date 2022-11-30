package com.jpop.order.service;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    ProductServiceClient productClient;

    private static final String ORDER_SERVICE_CONFIG = "orderService";
    private int attempt = 1;

    @GetMapping()
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        list.add(new Order(1, "Order 1"));
        list.add(new Order(2, "Order 2"));
        list.add(new Order(3, "Order 3"));
        return list;
    }

    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public ResponseEntity<Object> getProductDetailsCircuitBreaker() {
        System.out.println("Inside order controller - calling getProductDetailsCircuitBreaker");
        RestTemplate rt = new RestTemplate();
        System.out.println("Inside getProductDetailsCircuitBreaker - retry attempt: " + attempt++ + " time: " + new Date());
        List<Product> response = rt.exchange("http://localhost:8083/product",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
                }).getBody();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/retry")
    @Retry(name = ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public ResponseEntity<Object> getProductDetailsRetry() {
        System.out.println("Inside order controller - calling getProductDetailsRetry");
        RestTemplate rt = new RestTemplate();
        System.out.println("Previous attempt Count: " + attempt);
        System.out.println("Inside getProductDetailsRetry - retry attempt: " + attempt++ + " time: " + new Date());
        List<Product> response = rt.exchange("http://localhost:8083/product",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
                }).getBody();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/ratelimiter")
    @RateLimiter(name = ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public ResponseEntity<Object> getProductDetailsRateLimiter() throws InterruptedException {
        System.out.println("Inside order controller - calling getProductDetailsRateLimiter");
        RestTemplate rt = new RestTemplate();
        Thread.sleep(1000);
        List<Product> response = rt.exchange("http://localhost:8083/product",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
                }).getBody();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/timelimiter")
    @TimeLimiter(name = ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProductsTimeout")
    public CompletableFuture<String> getProductDetailsTimeLimiter() {
        System.out.println("Inside getProductDetailsTimeLimiter");
        final int arg = 8;
        CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return arg + "";
            }
            return (arg + 10) + "";
        });
        return f;
    }

    @GetMapping("/bulkDemo")
    @Bulkhead(name = ORDER_SERVICE_CONFIG, fallbackMethod = "bulkHeadFallback")
    public ResponseEntity<Object> getProductDetailsBulkHead() throws InterruptedException {
        System.out.println("Inside OrderController.getProductDetailsBulkHead");
        Thread.sleep(5000);
        String response = new RestTemplate().getForObject("http://localhost:8083/product", String.class);
        return new ResponseEntity<Object>("Bulk Head Success for concurrent calls", HttpStatus.OK);
    }

    public ResponseEntity<Object> bulkHeadFallback(Exception t) {
        if (t instanceof ResourceAccessException) {
            return new ResponseEntity<Object>(" Product Service is not running", HttpStatus.GATEWAY_TIMEOUT);
        } else {
            return new ResponseEntity<Object>(" orderService is full and does not permit further calls", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @GetMapping("/product-feign")
    @RateLimiter(name = ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public ResponseEntity<Object> getProductDetailsUsingFeign() {
        System.out.println("Inside order controller - calling getProductDetailsUsingFeign");
        return productClient.getAllProducts();
    }

    /**
     * Fallback Method
     */
    private ResponseEntity<Object> getAvailableProducts(RuntimeException e) {
        System.out.println("Inside order controller - returning fallback getAvailableProducts");
        List<Product> list = new ArrayList<>();
        list.add(new Product(1, "Dummy Product 1"));
        list.add(new Product(2, "Dummy Product 2"));
        list.add(new Product(3, "Dummy Product 3"));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * Fallback Method
     */
    private CompletableFuture<String> getAvailableProductsTimeout(TimeoutException rnp) {
        System.out.println("Inside order controller - returning fallback getAvailableProductsTimeout");
        return CompletableFuture.supplyAsync(() -> {
            return "TimeLimiter 'orderService' recorded a timeout exception";
        });
    }

    @GetMapping("/product-url")
    public ResponseEntity<Object> getProductDetailsWithHardCodedUrl() {
        System.out.println("Inside order controller - calling getProductDetailsWithHardCodedUrl");
        RestTemplate rt = new RestTemplate();
        List<Product> response = rt.exchange("http://localhost:8083/product",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
                }).getBody();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Uses Load Balanced RestTemplate
     */
    @GetMapping("/product")
    public ResponseEntity<Object> getProductDetails() {
        System.out.println("Inside order controller - calling getProductDetails");
        System.out.println("Inside getProductDetailsUsingFeign - retry attempt: " + attempt++ + " time: " + new Date());
        try {
            List<Product> response = restTemplate
                    .exchange("http://product-service/product",
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<Product>>() {
                            }).getBody();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("No instances available for product-service", HttpStatus.OK);
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<Object> getCustomerDetails() {
        System.out.println("Inside order controller - calling getCustomerDetails");
        try {
            return new ResponseEntity<>(restTemplate
                    .exchange("http://customer-service/customer",
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<Customer>>() {
                            }).getBody(), HttpStatus.OK);
        } catch (IllegalStateException i) {
            i.printStackTrace();
            return new ResponseEntity<>("No instances available for customer-service", HttpStatus.OK);
        }
    }
}