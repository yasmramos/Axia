package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.*;
import io.ebean.Database;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Invoice entity persistence operations.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class InvoiceRepository {

    private final Database db;

    @Inject
    public InvoiceRepository() {
        this.db = DatabaseManager.getDatabase();
    }

    public void save(Invoice invoice) {
        db.save(invoice);
    }

    public void update(Invoice invoice) {
        db.update(invoice);
    }

    public void delete(Invoice invoice) {
        db.delete(invoice);
    }

    public Optional<Invoice> findById(Long id) {
        return Optional.ofNullable(
                db.find(Invoice.class)
                        .fetch("lines")
                        .fetch("customer")
                        .fetch("supplier")
                        .where()
                        .idEq(id)
                        .findOne()
        );
    }

    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return db.find(Invoice.class)
                .fetch("lines")
                .where()
                .eq("invoiceNumber", invoiceNumber)
                .findOneOrEmpty();
    }

    public List<Invoice> findAll() {
        return db.find(Invoice.class)
                .fetch("customer")
                .fetch("supplier")
                .orderBy("date desc")
                .findList();
    }

    public List<Invoice> findByType(InvoiceType type) {
        return db.find(Invoice.class)
                .fetch("customer")
                .fetch("supplier")
                .where()
                .eq("type", type)
                .orderBy("date desc")
                .findList();
    }

    public List<Invoice> findByCustomer(Customer customer) {
        return db.find(Invoice.class)
                .where()
                .eq("customer", customer)
                .orderBy("date desc")
                .findList();
    }

    public List<Invoice> findBySupplier(Supplier supplier) {
        return db.find(Invoice.class)
                .where()
                .eq("supplier", supplier)
                .orderBy("date desc")
                .findList();
    }

    public List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return db.find(Invoice.class)
                .where()
                .ge("date", startDate)
                .le("date", endDate)
                .orderBy("date")
                .findList();
    }

    public List<Invoice> findByStatus(InvoiceStatus status) {
        return db.find(Invoice.class)
                .where()
                .eq("status", status)
                .orderBy("date desc")
                .findList();
    }

    public String getNextInvoiceNumber(InvoiceType type, int year) {
        String prefix = type == InvoiceType.SALE ? "FV" : "FC";
        String pattern = prefix + "-" + year + "-%";

        Integer max = db.sqlQuery("SELECT MAX(CAST(SUBSTRING(invoice_number FROM '\\d+$') AS INTEGER)) FROM invoices WHERE invoice_number LIKE :pattern")
                .setParameter("pattern", pattern)
                .findOne()
                .getInteger("max");

        int next = (max != null ? max : 0) + 1;
        return String.format("%s-%d-%06d", prefix, year, next);
    }
}
