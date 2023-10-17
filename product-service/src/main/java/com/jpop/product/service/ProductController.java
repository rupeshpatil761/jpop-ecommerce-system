package com.jpop.product.service;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @GetMapping()
    public List<Product> getAllProducts() throws InterruptedException {
        List<Product> list = new ArrayList<>();
        list.add(new Product(1,"Product 1"));
        list.add(new Product(2,"Product 2"));
        list.add(new Product(3,"Product 3"));
        return list;
    }
}
