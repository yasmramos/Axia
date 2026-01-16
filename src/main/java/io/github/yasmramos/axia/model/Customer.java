package io.github.yasmramos.axia.model;

import io.ebean.annotation.Index;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer entity.
 * 
 * <p>Customers are parties to whom the company sells goods or services.
 * They are linked to sales invoices and accounts receivable.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "customers")
public class Customer extends BaseModel {

    /** Unique customer code */
    @Index(unique = true)
    @Column(nullable = false, length = 20)
    private String code;

    /** Customer name or company name */
    @Column(nullable = false, length = 200)
    private String name;

    /** Tax identification number */
    @Column(length = 20)
    private String taxId;

    /** Street address */
    @Column(length = 300)
    private String address;

    /** City */
    @Column(length = 100)
    private String city;

    /** Phone number */
    @Column(length = 20)
    private String phone;

    /** Email address */
    @Column(length = 100)
    private String email;

    /** Whether the customer is active */
    @Column(nullable = false)
    private boolean active = true;

    /** Invoices issued to this customer */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Invoice> invoices = new ArrayList<>();

    // ==================== Getters and Setters ====================

    /** @return the customer code */
    public String getCode() {
        return code;
    }

    /** @param code the customer code */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return the customer name */
    public String getName() {
        return name;
    }

    /** @param name the customer name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the tax ID */
    public String getTaxId() {
        return taxId;
    }

    /** @param taxId the tax ID */
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    /** @return the address */
    public String getAddress() {
        return address;
    }

    /** @param address the address */
    public void setAddress(String address) {
        this.address = address;
    }

    /** @return the city */
    public String getCity() {
        return city;
    }

    /** @param city the city */
    public void setCity(String city) {
        this.city = city;
    }

    /** @return the phone number */
    public String getPhone() {
        return phone;
    }

    /** @param phone the phone number */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** @return the email address */
    public String getEmail() {
        return email;
    }

    /** @param email the email address */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return true if active */
    public boolean isActive() {
        return active;
    }

    /** @param active the active status */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return list of invoices */
    public List<Invoice> getInvoices() {
        return invoices;
    }

    /** @param invoices the invoices list */
    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }
}
