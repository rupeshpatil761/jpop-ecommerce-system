package com.jpop.customer;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.print.Book;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @GetMapping()
    public List<Customer> getAllCustomers(){
        List<Customer> list = new ArrayList<>();
        list.add(new Customer(1,"Amit"));
        list.add(new Customer(2,"Pavan"));
        list.add(new Customer(3,"Suresh"));
        return list;
    }
}
