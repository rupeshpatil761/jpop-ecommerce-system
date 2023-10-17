package com.jpop.customer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
/*@Entity
@Table(name = "CUSTOMER")*/
public class Customer {

    @Id
    @GeneratedValue
    private int id;
    private String first_name;

    private String email;

    public Customer(){}

    public Customer(int id, String name) {
        this.id = id;
        this.first_name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
}
