package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.config.DatabaseConfig;
import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.InvoiceRepository;
import io.ebean.Database;
import io.ebean.Transaction;

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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final Database db;

    public InvoiceService() {
        this.invoiceRepository = new InvoiceRepository();
        this.accountRepository = new AccountRepository();
        this.journalEntryService = new JournalEntryService();
        this.db = DatabaseConfig.getDatabase();
    }

    public Invoice createSaleInvoice(Customer customer, LocalDate date, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceRepository.getNextInvoiceNumber(InvoiceType.SALE, date.getYear()));
        invoice.setType(InvoiceType.SALE);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDate(date);
        invoice.setDueDate(dueDate);
        invoice.setCustomer(customer);

        invoiceRepository.save(invoice);
        return invoice;
    }

    public Invoice createPurchaseInvoice(Supplier supplier, LocalDate date, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceRepository.getNextInvoiceNumber(InvoiceType.PURCHASE, date.getYear()));
        invoice.setType(InvoiceType.PURCHASE);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDate(date);
        invoice.setDueDate(dueDate);
        invoice.setSupplier(supplier);

        invoiceRepository.save(invoice);
        return invoice;
    }

    public Invoice addLine(Invoice invoice, String description, BigDecimal quantity,
                           BigDecimal unitPrice, BigDecimal taxRate, Account account) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden modificar facturas en borrador");
        }

        InvoiceLine line = new InvoiceLine();
        line.setDescription(description);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setTaxRate(taxRate);
        line.setAccount(account);

        invoice.addLine(line);
        invoiceRepository.update(invoice);

        return invoice;
    }

    public Invoice post(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden contabilizar facturas en borrador");
        }

        if (invoice.getLines().isEmpty()) {
            throw new IllegalStateException("La factura no tiene líneas");
        }

        try (Transaction txn = db.beginTransaction()) {
            JournalEntry entry = journalEntryService.create(
                    invoice.getDate(),
                    "Factura " + invoice.getInvoiceNumber(),
                    invoice.getInvoiceNumber()
            );

            if (invoice.getType() == InvoiceType.SALE) {
                createSaleJournalEntry(invoice, entry);
            } else {
                createPurchaseJournalEntry(invoice, entry);
            }

            journalEntryService.post(entry.getId());

            invoice.setJournalEntry(entry);
            invoice.setStatus(InvoiceStatus.POSTED);
            invoiceRepository.update(invoice);

            txn.commit();
        }

        return invoice;
    }

    private void createSaleJournalEntry(Invoice invoice, JournalEntry entry) {
        // Débito: Cuentas por Cobrar
        Account cxc = accountRepository.findByCode("1.1.03")
                .orElseThrow(() -> new IllegalStateException("Cuenta 'Cuentas por Cobrar' no encontrada"));
        journalEntryService.addLine(entry, cxc, invoice.getTotal(), null,
                "Factura " + invoice.getInvoiceNumber());

        // Crédito: Ingresos por cada línea
        for (InvoiceLine line : invoice.getLines()) {
            Account incomeAccount = line.getAccount() != null ? line.getAccount() :
                    accountRepository.findByCode("4.1.01").orElse(null);
            if (incomeAccount != null) {
                journalEntryService.addLine(entry, incomeAccount, null, line.getSubtotal(),
                        line.getDescription());
            }
        }

        // Crédito: Impuestos por Pagar
        if (invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account taxAccount = accountRepository.findByCode("2.1.02")
                    .orElseThrow(() -> new IllegalStateException("Cuenta 'Impuestos por Pagar' no encontrada"));
            journalEntryService.addLine(entry, taxAccount, null, invoice.getTaxAmount(),
                    "IVA Factura " + invoice.getInvoiceNumber());
        }
    }

    private void createPurchaseJournalEntry(Invoice invoice, JournalEntry entry) {
        // Débito: Gastos por cada línea
        for (InvoiceLine line : invoice.getLines()) {
            Account expenseAccount = line.getAccount() != null ? line.getAccount() :
                    accountRepository.findByCode("5.1.02").orElse(null);
            if (expenseAccount != null) {
                journalEntryService.addLine(entry, expenseAccount, line.getSubtotal(), null,
                        line.getDescription());
            }
        }

        // Débito: IVA por recuperar (simplificado como reducción de impuestos por pagar)
        if (invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            Account taxAccount = accountRepository.findByCode("2.1.02")
                    .orElseThrow(() -> new IllegalStateException("Cuenta 'Impuestos por Pagar' no encontrada"));
            journalEntryService.addLine(entry, taxAccount, invoice.getTaxAmount(), null,
                    "IVA Factura " + invoice.getInvoiceNumber());
        }

        // Crédito: Cuentas por Pagar
        Account cxp = accountRepository.findByCode("2.1.01")
                .orElseThrow(() -> new IllegalStateException("Cuenta 'Cuentas por Pagar' no encontrada"));
        journalEntryService.addLine(entry, cxp, null, invoice.getTotal(),
                "Factura " + invoice.getInvoiceNumber());
    }

    public Invoice cancel(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("La factura ya está anulada");
        }

        if (invoice.getStatus() == InvoiceStatus.POSTED && invoice.getJournalEntry() != null) {
            journalEntryService.reverse(
                    invoice.getJournalEntry().getId(),
                    LocalDate.now(),
                    "Anulación de factura " + invoice.getInvoiceNumber()
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.update(invoice);

        return invoice;
    }

    public Invoice markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        if (invoice.getStatus() != InvoiceStatus.POSTED) {
            throw new IllegalStateException("Solo se pueden marcar como pagadas facturas contabilizadas");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.update(invoice);

        return invoice;
    }

    public Optional<Invoice> findById(Long id) {
        return invoiceRepository.findById(id);
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> findByType(InvoiceType type) {
        return invoiceRepository.findByType(type);
    }

    public List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByDateRange(startDate, endDate);
    }

    public List<Invoice> findByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }
}
