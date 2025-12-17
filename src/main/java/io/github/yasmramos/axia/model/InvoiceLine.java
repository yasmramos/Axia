package io.github.yasmramos.axia.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Represents a single line item on an invoice.
 * 
 * <p>Each line contains product/service details, quantities, pricing,
 * and tax information. Totals are automatically calculated when
 * quantity, unit price, or tax rate changes.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "invoice_lines")
public class InvoiceLine extends BaseModel {

    /** Parent invoice */
    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /** Product or service description */
    @Column(nullable = false, length = 200)
    private String description;

    /** Quantity */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    /** Unit price */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Tax rate as percentage (e.g., 16.00 for 16%) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    /** Subtotal (quantity * unitPrice) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Tax amount (subtotal * taxRate / 100) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Line total (subtotal + taxAmount) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;

    /** Income/expense account for this line */
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // ==================== Getters and Setters ====================

    /** @return the parent invoice */
    public Invoice getInvoice() {
        return invoice;
    }

    /** @param invoice the parent invoice */
    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the quantity */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Sets quantity and recalculates totals.
     * @param quantity the quantity
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        calculate();
    }

    /** @return the unit price */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * Sets unit price and recalculates totals.
     * @param unitPrice the unit price
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculate();
    }

    /** @return the tax rate */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /**
     * Sets tax rate and recalculates totals.
     * @param taxRate the tax rate as percentage
     */
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
        calculate();
    }

    /** @return the subtotal */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** @return the tax amount */
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    /** @return the line total */
    public BigDecimal getTotal() {
        return total;
    }

    /** @return the account */
    public Account getAccount() {
        return account;
    }

    /** @param account the account */
    public void setAccount(Account account) {
        this.account = account;
    }

    // ==================== Business Methods ====================

    /**
     * Recalculates subtotal, tax amount, and total.
     */
    private void calculate() {
        this.subtotal = quantity.multiply(unitPrice);
        this.taxAmount = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100));
        this.total = subtotal.add(taxAmount);
    }
}
