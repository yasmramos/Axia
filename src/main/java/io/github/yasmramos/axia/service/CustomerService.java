package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Customer business operations.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    @Inject
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(String code, String name, String taxId, String address,
                           String city, String phone, String email) {
        if (customerRepository.findByCode(code).isPresent()) {
            log.error("Customer already exists with code: {}", code);
            throw new IllegalArgumentException("Customer already exists with code: " + code);
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
