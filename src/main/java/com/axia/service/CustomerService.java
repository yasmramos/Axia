package com.axia.service;

import com.axia.model.Customer;
import com.axia.repository.CustomerRepository;

import java.util.List;
import java.util.Optional;

public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService() {
        this.customerRepository = new CustomerRepository();
    }

    public Customer create(String code, String name, String taxId, String address,
                           String city, String phone, String email) {
        if (customerRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente con el código: " + code);
        }

        Customer customer = new Customer();
        customer.setCode(code);
        customer.setName(name);
        customer.setTaxId(taxId);
        customer.setAddress(address);
        customer.setCity(city);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setActive(true);

        customerRepository.save(customer);
        return customer;
    }

    public Customer update(Customer customer) {
        customerRepository.update(customer);
        return customer;
    }

    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        if (!customer.getInvoices().isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un cliente con facturas asociadas");
        }

        customerRepository.delete(customer);
    }

    public void deactivate(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        customer.setActive(false);
        customerRepository.update(customer);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByCode(String code) {
        return customerRepository.findByCode(code);
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public List<Customer> findActive() {
        return customerRepository.findActive();
    }

    public List<Customer> search(String term) {
        return customerRepository.search(term);
    }
}
