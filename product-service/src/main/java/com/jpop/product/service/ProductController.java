package com.jpop.product.service;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    private static final String PRODUCT_SERVICE_CONFIG = "productService";

    @GetMapping()
    public List<Product> getAllProducts(){
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Product 1"));
        list.add(new Product(2,"Product 2"));
        list.add(new Product(3,"Product 3"));
        return list;
    }

    @GetMapping("/ratelimiter")
    @RateLimiter(name=PRODUCT_SERVICE_CONFIG)
    //@RateLimiter(name=PRODUCT_SERVICE_CONFIG, fallbackMethod = "getAvailableProducts")
    public List<Product> getAllProductsWithRateLimiter(){
        System.out.println("Inside ProductController.getAllProductsWithRateLimiter");
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Product 1"));
        list.add(new Product(2,"Product 2"));
        list.add(new Product(3,"Product 3"));
        return list;
    }

    private List<Product> getAvailableProducts(RuntimeException e) {
        System.out.println("Inside order controller - returning fallback getAvailableProducts");
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Dummy Product 1"));
        list.add(new Product(2,"Dummy Product 2"));
        list.add(new Product(3,"Dummy Product 3"));
        return list;
    }

    @GetMapping("/bulk")
    @Bulkhead(name=PRODUCT_SERVICE_CONFIG,fallbackMethod = "bulkHeadFallback")
    public ResponseEntity<String> getProductDetailsBulkHead() throws InterruptedException {
        System.out.println("Inside ProductController.getProductDetailsBulkHead | now: " + LocalTime.now());
        Thread.sleep(5000);
        System.out.println(LocalTime.now() + " Call processing finished = " + Thread.currentThread().getName());
        return new ResponseEntity<String>("Bulk call success", HttpStatus.OK);
    }

    public ResponseEntity<String> bulkHeadFallback(Exception t) {
        return new ResponseEntity<String>(" productService is full and does not permit further calls", HttpStatus.TOO_MANY_REQUESTS);
    }
}
