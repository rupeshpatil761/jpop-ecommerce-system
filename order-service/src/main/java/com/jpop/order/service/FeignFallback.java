package com.jpop.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FeignFallback implements ProductServiceClient {
    @Override
    public ResponseEntity<Object> getAllProducts() {
        System.out.println("Inside order controller - returning fallback getAvailableProducts");
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Dummy Product 1"));
        list.add(new Product(2,"Dummy Product 2"));
        list.add(new Product(3,"Dummy Product 3"));
        return new ResponseEntity<>(list, HttpStatus.OK);
    }
}
