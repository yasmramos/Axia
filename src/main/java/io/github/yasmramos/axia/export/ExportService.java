package io.github.yasmramos.axia.export;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting accounting data to various formats.
 * Supports CSV and custom report formats.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Exports accounts to CSV format.
     *
     * @param accounts list of accounts to export
     * @param outputPath path for the output file
     * @throws IOException if export fails
     */
    public void exportAccountsToCsv(List<Account> accounts, String outputPath) throws IOException {
        logger.info("Exporting {} accounts to CSV: {}", accounts.size(), outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Code,Name,Type,Parent Code,Active");

            for (Account account : accounts) {
                writer.printf("%s,\"%s\",%s,%s,%s%n",
                        account.getCode(),
                        escapeCsv(account.getName()),
                        account.getType(),
                        account.getParent() != null ? account.getParent().getCode() : "",
                        account.isActive());
            }
        }

        logger.info("Accounts export completed successfully");
    }

    /**
     * Exports journal entries to CSV format.
     *
     * @param entries list of journal entries to export
     * @param outputPath path for the output file
     * @throws IOException if export fails
     */
    public void exportJournalEntriesToCsv(List<JournalEntry> entries, String outputPath) throws IOException {
        logger.info("Exporting {} journal entries to CSV: {}", entries.size(), outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Entry Number,Date,Description,Total Debit,Total Credit,Reference");

            for (JournalEntry entry : entries) {
                writer.printf("%s,%s,\"%s\",%s,%s,%s%n",
                        entry.getEntryNumber(),
                        entry.getDate().format(DATE_FORMAT),
                        escapeCsv(entry.getDescription()),
                        formatAmount(entry.getTotalDebit()),
                        formatAmount(entry.getTotalCredit()),
                        entry.getReference() != null ? entry.getReference() : "");
            }
        }

        logger.info("Journal entries export completed successfully");
    }

    /**
     * Exports customers to CSV format.
     *
     * @param customers list of customers to export
     * @param outputPath path for the output file
     * @throws IOException if export fails
     */
    public void exportCustomersToCsv(List<Customer> customers, String outputPath) throws IOException {
        logger.info("Exporting {} customers to CSV: {}", customers.size(), outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Name,Tax ID,Email,Phone,Address,Active");

            for (Customer customer : customers) {
                writer.printf("\"%s\",%s,%s,%s,\"%s\",%s%n",
                        escapeCsv(customer.getName()),
                        customer.getTaxId() != null ? customer.getTaxId() : "",
                        customer.getEmail() != null ? customer.getEmail() : "",
                        customer.getPhone() != null ? customer.getPhone() : "",
                        escapeCsv(customer.getAddress()),
                        customer.isActive());
            }
        }

        logger.info("Customers export completed successfully");
    }

    /**
     * Exports suppliers to CSV format.
     *
     * @param suppliers list of suppliers to export
     * @param outputPath path for the output file
     * @throws IOException if export fails
     */
    public void exportSuppliersToCsv(List<Supplier> suppliers, String outputPath) throws IOException {
        logger.info("Exporting {} suppliers to CSV: {}", suppliers.size(), outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Name,Tax ID,Email,Phone,Address,Active");

            for (Supplier supplier : suppliers) {
                writer.printf("\"%s\",%s,%s,%s,\"%s\",%s%n",
                        escapeCsv(supplier.getName()),
                        supplier.getTaxId() != null ? supplier.getTaxId() : "",
                        supplier.getEmail() != null ? supplier.getEmail() : "",
                        supplier.getPhone() != null ? supplier.getPhone() : "",
                        escapeCsv(supplier.getAddress()),
                        supplier.isActive());
            }
        }

        logger.info("Suppliers export completed successfully");
    }

    /**
     * Exports invoices to CSV format.
     *
     * @param invoices list of invoices to export
     * @param outputPath path for the output file
     * @throws IOException if export fails
     */
    public void exportInvoicesToCsv(List<Invoice> invoices, String outputPath) throws IOException {
        logger.info("Exporting {} invoices to CSV: {}", invoices.size(), outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("Invoice Number,Date,Due Date,Customer,Supplier,Subtotal,Tax,Total,Status");

            for (Invoice invoice : invoices) {
                writer.printf("%s,%s,%s,\"%s\",\"%s\",%s,%s,%s,%s%n",
                        invoice.getInvoiceNumber(),
                        invoice.getDate().format(DATE_FORMAT),
                        invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FORMAT) : "",
                        invoice.getCustomer() != null ? escapeCsv(invoice.getCustomer().getName()) : "",
                        invoice.getSupplier() != null ? escapeCsv(invoice.getSupplier().getName()) : "",
                        formatAmount(invoice.getSubtotal()),
                        formatAmount(invoice.getTaxAmount()),
                        formatAmount(invoice.getTotal()),
                        invoice.getStatus());
            }
        }

        logger.info("Invoices export completed successfully");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0.00";
    }
}
