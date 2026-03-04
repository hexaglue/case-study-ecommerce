package com.acme.shop.domain.customer;

import com.acme.shop.domain.order.Address;

public class Customer {

    private CustomerId id;
    private String firstName;
    private String lastName;
    private final Email email;
    private String phone;
    private Address address;

    public Customer(CustomerId id, String firstName, String lastName, Email email,
                    String phone, Address address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public static Customer create(String firstName, String lastName, Email email) {
        return new Customer(null, firstName, lastName, email, null, null);
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public void updateAddress(Address address) {
        this.address = address;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public CustomerId getId() { return id; }
    public void setId(CustomerId id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Email getEmail() { return email; }
    public String getPhone() { return phone; }
    public Address getAddress() { return address; }
}
