package io.github.yasmramos.axia.model;

import io.ebean.annotation.Index;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invoice document (sales or purchase).
 * 
 * <p>Invoices track commercial transactions with customers or suppliers.
 * When posted, they automatically generate the corresponding journal entry
 * to update account balances.
 * 
 * <p>Lifecycle:
 * <ol>
 *   <li>DRAFT - Being prepared, can be modified</li>
 *   <li>POSTED - Recorded in the ledger</li>
 *   <li>PAID - Fully settled</li>
 *   <li>CANCELLED - Voided with reversal entry</li>
 * </ol>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseModel {

    /** Unique invoice number (e.g., "FV-2024-000001") */
    @Index(unique = true)
    @Column(nullable = false, length = 30)
    private String invoiceNumber;

    /** Invoice type (SALE or PURCHASE) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType type;

    /** Current invoice status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    /** Invoice date */
    @Column(nullable = false)
    private LocalDate date;

    /** Payment due date */
    @Column
    private LocalDate dueDate;

    /** Customer (for sales invoices) */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Supplier (for purchase invoices) */
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Invoice line items */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    /** Subtotal before taxes */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Total tax amount */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Grand total (subtotal + tax) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;

    /** Additional notes */
    @Column(length = 500)
    private String notes;

    /** Associated journal entry (when posted) */
    @ManyToOne
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    // ==================== Getters and Setters ====================

    /** @return the invoice number */
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    /** @param invoiceNumber the invoice number */
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    /** @return the invoice type */
    public InvoiceType getType() {
        return type;
    }

    /** @param type the invoice type */
    public void setType(InvoiceType type) {
        this.type = type;
    }

    /** @return the current status */
    public InvoiceStatus getStatus() {
        return status;
    }

    /** @param status the status */
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    /** @return the invoice date */
    public LocalDate getDate() {
        return date;
    }

    /** @param date the invoice date */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /** @return the due date */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** @param dueDate the due date */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** @return the customer */
    public Customer getCustomer() {
        return customer;
    }

    /** @param customer the customer */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /** @return the supplier */
    public Supplier getSupplier() {
        return supplier;
    }

    /** @param supplier the supplier */
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    /** @return the invoice lines */
    public List<InvoiceLine> getLines() {
        return lines;
    }

    /** @param lines the invoice lines */
    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }

    /** @return the subtotal */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** @param subtotal the subtotal */
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    /** @return the tax amount */
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    /** @param taxAmount the tax amount */
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /** @return the total */
    public BigDecimal getTotal() {
        return total;
    }

    /** @param total the total */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    /** @return the notes */
    public String getNotes() {
        return notes;
    }

    /** @param notes the notes */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** @return the journal entry */
    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    /** @param journalEntry the journal entry */
    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    // ==================== Business Methods ====================

    /**
     * Adds a line item and recalculates totals.
     * @param line the line to add
     */
    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        this.lines.add(line);
        calculateTotals();
    }

    /**
     * Recalculates subtotal, tax, and total from line items.
     */
    public void calculateTotals() {
        this.subtotal = lines.stream()
                .map(InvoiceLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = lines.stream()
                .map(InvoiceLine::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = subtotal.add(taxAmount);
    }
}
