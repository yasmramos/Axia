package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.InvoiceRepository;
import io.ebean.Database;
import io.ebean.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Invoice business operations.
 *
 * <p>Manages invoice lifecycle including creation, posting
 * with automatic journal entry generation, and cancellation.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final Database db;

    @Inject
    public InvoiceService(InvoiceRepository invoiceRepository, AccountRepository accountRepository, 
                         JournalEntryService journalEntryService) {
        this.invoiceRepository = invoiceRepository;
        this.accountRepository = accountRepository;
        this.journalEntryService = journalEntryService;
        this.db = DatabaseManager.getDatabase();
        log.debug("InvoiceService initialized");
    }

    public Invoice create(String number, InvoiceType type, LocalDate date, LocalDate dueDate,
                          Customer customer, Supplier supplier, BigDecimal subtotal, BigDecimal tax) {
        log.info("Creating invoice: {}", number);
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setType(type);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDate(date);
        invoice.setDueDate(dueDate);
        invoice.setCustomer(customer);
        invoice.setSupplier(supplier);
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(tax);
        invoiceRepository.save(invoice);
        log.info("Invoice created: {}", number);
        return invoice;
    }

    public void delete(Long id) {
        log.info("Deleting invoice with id: {}", id);
        invoiceRepository.findById(id).ifPresent(invoice -> {
            if (invoice.getStatus() == InvoiceStatus.DRAFT) {
                invoiceRepository.delete(invoice);
                log.info("Invoice deleted: {}", invoice.getInvoiceNumber());
            } else {
                throw new IllegalStateException("Cannot delete non-draft invoice");
            }
        });
    }

    public Invoice createSaleInvoice(Customer customer, LocalDate date, LocalDate dueDate) {
        log.info("Creating sales invoice for customer: {}", customer.getName());
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceRepository.getNextInvoiceNumber(InvoiceType.SALE, date.getYear()));
        invoice.setType(InvoiceType.SALE);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDate(date);
        invoice.setDueDate(dueDate);
        invoice.setCustomer(customer);

        invoiceRepository.save(invoice);
        log.info("Sales invoice created: {}", invoice.getInvoiceNumber());
        return invoice;
    }

    public Invoice createPurchaseInvoice(Supplier supplier, LocalDate date, LocalDate dueDate) {
        log.info("Creating purchase invoice for supplier: {}", supplier.getName());
        
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceRepository.getNextInvoiceNumber(InvoiceType.PURCHASE, date.getYear()));
        invoice.setType(InvoiceType.PURCHASE);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDate(date);
        invoice.setDueDate(dueDate);
        invoice.setSupplier(supplier);

        invoiceRepository.save(invoice);
        log.info("Purchase invoice created: {}", invoice.getInvoiceNumber());
        return invoice;
    }

    public Invoice addLine(Invoice invoice, String description, BigDecimal quantity,
                           BigDecimal unitPrice, BigDecimal taxRate, Account account) {
        log.debug("Adding line to invoice {}: {}", invoice.getInvoiceNumber(), description);
        
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            log.error("Cannot modify non-draft invoice: {}", invoice.getInvoiceNumber());
            throw new IllegalStateException("Can only modify draft invoices");
        }

        InvoiceLine line = new InvoiceLine();
        line.setDescription(description);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setTaxRate(taxRate);
        line.setAccount(account);

        invoice.addLine(line);
        invoiceRepository.update(invoice);
        log.debug("Line added. Invoice total: {}", invoice.getTotal());

        return invoice;
    }

    public Invoice post(Long invoiceId) {
        log.info("Posting invoice ID: {}", invoiceId);
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found: {}", invoiceId);
                    return new IllegalArgumentException("Invoice not found");
                });

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            log.error("Cannot post non-draft invoice: {}", invoice.getInvoiceNumber());
            throw new IllegalStateException("Can only post draft invoices");
        }

        if (invoice.getLines().isEmpty()) {
            log.error("Invoice {} has no lines", invoice.getInvoiceNumber());
            throw new IllegalStateException("Invoice has no lines");
        }

        try (Transaction txn = db.beginTransaction()) {
            log.debug("Creating journal entry for invoice {}", invoice.getInvoiceNumber());
            
            JournalEntry entry = journalEntryService.create(
                    invoice.getDate(),
                    "Invoice " + invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumber()
            );

            if (invoice.getType() == InvoiceType.SALE) {
                createSaleJournalEntry(invoice, entry);
            } else {
                createPurchaseJournalEntry(invoice, entry);
            }

            entry = journalEntryService.post(entry.getId());

            invoice.setJournalEntry(entry);
            invoice.setStatus(InvoiceStatus.POSTED);
            invoiceRepository.update(invoice);

            txn.commit();
            log.info("Invoice {} posted successfully with journal entry #{}", 
                    invoice.getInvoiceNumber(), entry.getEntryNumber());
        }

        return invoice;
    }

    private void createSaleJournalEntry(Invoice invoice, JournalEntry entry) {
        log.debug("Creating sales journal entry for invoice {}", invoice.getInvoiceNumber());
        
        // Debit: Accounts Receivable
        Account accountsReceivable = accountRepository.findByCode("1.1.03")
                .orElseThrow(() -> {
                    log.error("Accounts Receivable account not found");
                    return new IllegalStateException("Accounts Receivable account not found");
                });
        journalEntryService.addLine(entry, accountsReceivable, invoice.getTotal(), null,
                "Invoice " + invoice.getInvoiceNumber());

        // Credit: Revenue for each line
        for (InvoiceLine line : invoice.getLines()) {
            Account incomeAccount = line.getAccount() != null ? line.getAccount() :
                    accountRepository.findByCode("4.1.01").orElse(null);
            if (incomeAccount != null) {
                journalEntryService.addLine(entry, incomeAccount, null, line.getSubtotal(),
                        line.getDescription());
            }
        }

        // Credit: Taxes Payable
        if (invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account taxAccount = accountRepository.findByCode("2.1.02")
                    .orElseThrow(() -> {
                        log.error("Taxes Payable account not found");
                        return new IllegalStateException("Taxes Payable account not found");
                    });
            journalEntryService.addLine(entry, taxAccount, null, invoice.getTaxAmount(),
                    "Tax Invoice " + invoice.getInvoiceNumber());
        }
    }

    private void createPurchaseJournalEntry(Invoice invoice, JournalEntry entry) {
        log.debug("Creating purchase journal entry for invoice {}", invoice.getInvoiceNumber());
        
        // Debit: Expenses for each line
        for (InvoiceLine line : invoice.getLines()) {
            Account expenseAccount = line.getAccount() != null ? line.getAccount() :
                    accountRepository.findByCode("5.1.02").orElse(null);
            if (expenseAccount != null) {
                journalEntryService.addLine(entry, expenseAccount, line.getSubtotal(), null,
                        line.getDescription());
            }
        }

        // Debit: Input Tax (simplified as reduction of taxes payable)
        if (invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account taxAccount = accountRepository.findByCode("2.1.02")
                    .orElseThrow(() -> {
                        log.error("Taxes Payable account not found");
                        return new IllegalStateException("Taxes Payable account not found");
                    });
            journalEntryService.addLine(entry, taxAccount, invoice.getTaxAmount(), null,
                    "Tax Invoice " + invoice.getInvoiceNumber());
        }

        // Credit: Accounts Payable
        Account accountsPayable = accountRepository.findByCode("2.1.01")
                .orElseThrow(() -> {
                    log.error("Accounts Payable account not found");
                    return new IllegalStateException("Accounts Payable account not found");
                });
        journalEntryService.addLine(entry, accountsPayable, null, invoice.getTotal(),
                "Invoice " + invoice.getInvoiceNumber());
    }

    public Invoice cancel(Long invoiceId) {
        log.info("Cancelling invoice ID: {}", invoiceId);
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found: {}", invoiceId);
                    return new IllegalArgumentException("Invoice not found");
                });

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            log.error("Invoice {} is already cancelled", invoice.getInvoiceNumber());
            throw new IllegalStateException("Invoice is already cancelled");
        }

        if (invoice.getStatus() == InvoiceStatus.POSTED && invoice.getJournalEntry() != null) {
            log.debug("Reversing journal entry for invoice {}", invoice.getInvoiceNumber());
            journalEntryService.reverse(
                    invoice.getJournalEntry().getId(),
                    LocalDate.now(),
                    "Cancellation of invoice " + invoice.getInvoiceNumber()
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.update(invoice);
        log.info("Invoice {} cancelled", invoice.getInvoiceNumber());

        return invoice;
    }

    public Invoice markAsPaid(Long invoiceId) {
        log.info("Marking invoice as paid, ID: {}", invoiceId);
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found: {}", invoiceId);
                    return new IllegalArgumentException("Invoice not found");
                });

        if (invoice.getStatus() != InvoiceStatus.POSTED) {
            log.error("Cannot mark non-posted invoice as paid: {}", invoice.getInvoiceNumber());
            throw new IllegalStateException("Can only mark posted invoices as paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.update(invoice);
        log.info("Invoice {} marked as paid", invoice.getInvoiceNumber());

        return invoice;
    }

    public Optional<Invoice> findById(Long id) {
        log.debug("Finding invoice by ID: {}", id);
        return invoiceRepository.findById(id);
    }

    public List<Invoice> findAll() {
        log.debug("Retrieving all invoices");
        return invoiceRepository.findAll();
    }

    public List<Invoice> findByType(InvoiceType type) {
        log.debug("Finding invoices by type: {}", type);
        return invoiceRepository.findByType(type);
    }

    public List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Finding invoices by date range: {} to {}", startDate, endDate);
        return invoiceRepository.findByDateRange(startDate, endDate);
    }

    public List<Invoice> findByStatus(InvoiceStatus status) {
        log.debug("Finding invoices by status: {}", status);
        return invoiceRepository.findByStatus(status);
    }
}
