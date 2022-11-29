package com.jpop.order.service;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

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
    public List<Order> getAllOrders(){
        List<Order> list = new ArrayList<>();
        list.add(new Order(1,"Order 1"));
        list.add(new Order(2,"Order 2"));
        list.add(new Order(3,"Order 3"));
        return list;
    }

    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name=ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public List<Product> getProductDetailsCircuitBreaker(){
        System.out.println("Inside order controller - calling getProductDetailsCircuitBreaker");
        RestTemplate rt = new RestTemplate();
        System.out.println("Inside getProductDetailsCircuitBreaker - retry attempt: "+attempt++ +" time: "+new Date());
        return rt.exchange("http://localhost:8083/product",
                HttpMethod.GET,null,new ParameterizedTypeReference<List<Product>>() {}).getBody();
    }

    @GetMapping("/retry")
    @Retry(name=ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public List<Product> getProductDetailsRetry(){
        System.out.println("Inside order controller - calling getProductDetailsRetry");
        RestTemplate rt = new RestTemplate();
        System.out.println("Inside getProductDetailsRetry - retry attempt: "+attempt++ +" time: "+new Date());
        return rt.exchange("http://localhost:8083/product",
                HttpMethod.GET,null,new ParameterizedTypeReference<List<Product>>() {}).getBody();
    }

    @GetMapping("/ratelimiter")
    public List<Product> getProductDetailsRateLimiter() throws InterruptedException {
        System.out.println("Inside order controller - calling getProductDetailsRateLimiter");
        List<Product> response = null;
        for (int i = 1; i <= 5; i++) {
            Thread.sleep(1000);
            RestTemplate rt = new RestTemplate();
            System.out.println("Inside getProductDetailsRateLimiter - retry attempt: "+i +" time: "+new Date());
            response = rt.exchange("http://localhost:8083/product/ratelimiter",
                    HttpMethod.GET,null,new ParameterizedTypeReference<List<Product>>() {}).getBody();
        }
        return response;
    }

    @GetMapping("/timelimiter")
    //@TimeLimiter(name=ORDER_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    // getAvailableProducts return type should be CompletableFuture
    @TimeLimiter(name=ORDER_SERVICE_CONFIG)
    public CompletableFuture<Void> getProductDetailsTimeLimiter() throws InterruptedException {
        System.out.println("Inside getProductDetailsTimeLimiter");
        return CompletableFuture.runAsync(runnable);
    }
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                System.out.println("Hello");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @GetMapping("/bulkDemo")
    //@Bulkhead(name=ORDER_SERVICE_CONFIG,fallbackMethod = "bulkHeadFallback")
    public ResponseEntity<String> getProductDetailsBulkHead(@RequestParam("task") int task) throws InterruptedException {
        System.out.println("Inside OrderController.getProductDetailsBulkHead");
        int i = 1;
        IntStream.range(i,task).parallel().forEach( t -> {
            System.out.println("IntStream calling Thread: "+t);
            String response = new RestTemplate().getForObject("http://localhost:8083/product/bulk", String.class);
        });
        return new ResponseEntity<String>("Bulk Head Success for "+task+" concurrent calls", HttpStatus.OK);
    }

    public ResponseEntity<String> bulkHeadFallback(Exception t) {
        return new ResponseEntity<String>(" orderService is full and does not permit further calls", HttpStatus.TOO_MANY_REQUESTS);
    }

    @GetMapping("/bulkDemoExecutor")
    public String getProductDetailsBulkDemoExecutor(@RequestParam("task") int task) throws InterruptedException {
        System.out.println("Inside OrderController.getProductDetailsBulkDemoExecutor");
        ExecutorService executorService = Executors.newFixedThreadPool(task);
        try {
            List<TestCallable> tasks = new ArrayList<>();
            for (int i = 1; i <= task; i++) {
                tasks.add(new TestCallable(i));
            }
            executorService.invokeAll(tasks);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
        return "Bulk Head Success for "+task+" concurrent calls";
    }

    @GetMapping("/product-url")
    public List<Product> getProductDetailsWithHardCodedUrl(){
        System.out.println("Inside order controller - calling getProductDetailsWithHardCodedUrl");
        RestTemplate rt = new RestTemplate();
        return rt.exchange("http://localhost:8083/product",
                        HttpMethod.GET,null,new ParameterizedTypeReference<List<Product>>() {}).getBody();
    }

    /**
     * Fallback Method
     * should be private
     */
    private List<Product> getAvailableProducts(RuntimeException e) {
        System.out.println("Inside order controller - returning fallback getAvailableProducts");
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Dummy Product 1"));
        list.add(new Product(2,"Dummy Product 2"));
        list.add(new Product(3,"Dummy Product 3"));
        return list;
    }

    @GetMapping("/product-feign")
    public ResponseEntity<Object> getProductDetailsUsingFeign(){
        System.out.println("Inside order controller - calling getProductDetailsUsingFeign");
        return productClient.getAllProducts();
    }

    /**
     * Uses Load Balanced RestTemplate
     */
    @GetMapping("/product")
    public List<Product> getProductDetails(){
        System.out.println("Inside order controller - calling getProductDetails");
        System.out.println("Inside getProductDetailsUsingFeign - retry attempt: "+attempt++ +" time: "+new Date());
        return restTemplate
                .exchange("http://product-service/product",
                        HttpMethod.GET,null,new ParameterizedTypeReference<List<Product>>() {}).getBody();
    }

    @GetMapping("/customer")
    public Object getCustomerDetails(){
        System.out.println("Inside order controller - calling getCustomerDetails");
        try {
            return restTemplate
                    .exchange("http://customer-service/customer",
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<Customer>>() {
                            }).getBody();
        } catch(IllegalStateException i){
            i.printStackTrace();
            return "No instances available for customer-service";
        }
    }
}

class TestCallable implements Callable<String> {

    private int i;
    public TestCallable() {
    }

    public TestCallable(int i) {
        this.i = i;
    }

    @Override
    public String call() throws Exception {
        System.out.println("TestCallable calling Thread: "+this.i);
        RestTemplate rt = new RestTemplate();
        return rt.exchange("http://localhost:8083/product/bulk",
                HttpMethod.GET,null,new ParameterizedTypeReference<String>() {}).getBody();
    }
}