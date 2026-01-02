package io.github.yasmramos.axia.dashboard;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.ebean.DB;
import io.github.yasmramos.axia.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for dashboard statistics and KPIs.
 * Provides real-time accounting metrics and insights.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    /**
     * Gets summary statistics for the dashboard.
     *
     * @return map containing various statistics
     */
    public Map<String, Object> getSummaryStats() {
        logger.debug("Generating dashboard summary statistics");
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalAccounts", countAccounts());
        stats.put("activeAccounts", countActiveAccounts());
        stats.put("totalCustomers", countCustomers());
        stats.put("totalSuppliers", countSuppliers());
        stats.put("totalInvoices", countInvoices());
        stats.put("pendingInvoices", countPendingInvoices());
        stats.put("totalJournalEntries", countJournalEntries());

        return stats;
    }

    /**
     * Gets financial KPIs for a specific period.
     *
     * @param startDate start of period
     * @param endDate end of period
     * @return map containing financial KPIs
     */
    public Map<String, BigDecimal> getFinancialKPIs(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating financial KPIs from {} to {}", startDate, endDate);
        Map<String, BigDecimal> kpis = new HashMap<>();

        kpis.put("totalRevenue", calculateTotalRevenue(startDate, endDate));
        kpis.put("totalExpenses", calculateTotalExpenses(startDate, endDate));
        kpis.put("netIncome", calculateNetIncome(startDate, endDate));
        kpis.put("accountsReceivable", calculateAccountsReceivable());
        kpis.put("accountsPayable", calculateAccountsPayable());

        return kpis;
    }

    /**
     * Gets invoice statistics by status.
     *
     * @return map of status to count
     */
    public Map<String, Long> getInvoicesByStatus() {
        logger.debug("Getting invoice counts by status");
        Map<String, Long> statusCounts = new HashMap<>();

        for (InvoiceStatus status : InvoiceStatus.values()) {
            long count = DB.find(Invoice.class)
                    .where()
                    .eq("status", status)
                    .findCount();
            statusCounts.put(status.name(), count);
        }

        return statusCounts;
    }

    /**
     * Gets account balances summary by type.
     *
     * @return map of account type to total balance
     */
    public Map<String, BigDecimal> getAccountBalancesByType() {
        logger.debug("Getting account balances by type");
        Map<String, BigDecimal> balances = new HashMap<>();

        for (AccountType type : AccountType.values()) {
            BigDecimal total = calculateTotalBalanceByType(type);
            balances.put(type.name(), total);
        }

        return balances;
    }

    /**
     * Gets monthly revenue trend for the current year.
     *
     * @param year the year to analyze
     * @return map of month number to revenue amount
     */
    public Map<Integer, BigDecimal> getMonthlyRevenueTrend(int year) {
        logger.debug("Getting monthly revenue trend for year: {}", year);
        Map<Integer, BigDecimal> trend = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            BigDecimal revenue = calculateTotalRevenue(startDate, endDate);
            trend.put(month, revenue);
        }

        return trend;
    }

    /**
     * Gets top customers by invoice total.
     *
     * @param limit number of customers to return
     * @return list of customer stats
     */
    public List<Map<String, Object>> getTopCustomers(int limit) {
        logger.debug("Getting top {} customers", limit);

        List<Customer> customers = DB.find(Customer.class)
                .where()
                .eq("active", true)
                .setMaxRows(limit)
                .findList();

        return customers.stream()
                .map(c -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("id", c.getId());
                    stat.put("name", c.getName());
                    stat.put("invoiceCount", countCustomerInvoices(c.getId()));
                    stat.put("totalAmount", calculateCustomerTotal(c.getId()));
                    return stat;
                })
                .toList();
    }

    // Private helper methods
    private long countAccounts() {
        return DB.find(Account.class).findCount();
    }

    private long countActiveAccounts() {
        return DB.find(Account.class).where().eq("active", true).findCount();
    }

    private long countCustomers() {
        return DB.find(Customer.class).findCount();
    }

    private long countSuppliers() {
        return DB.find(Supplier.class).findCount();
    }

    private long countInvoices() {
        return DB.find(Invoice.class).findCount();
    }

    private long countPendingInvoices() {
        return DB.find(Invoice.class)
                .where()
                .eq("status", InvoiceStatus.DRAFT)
                .findCount();
    }

    private long countJournalEntries() {
        return DB.find(JournalEntry.class).findCount();
    }

    private BigDecimal calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = DB.find(Invoice.class)
                .where()
                .isNotNull("customer")
                .ge("date", startDate)
                .le("date", endDate)
                .eq("status", InvoiceStatus.PAID)
                .findList();

        return invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalExpenses(LocalDate startDate, LocalDate endDate) {
        List<Invoice> invoices = DB.find(Invoice.class)
                .where()
                .isNotNull("supplier")
                .ge("date", startDate)
                .le("date", endDate)
                .eq("status", InvoiceStatus.PAID)
                .findList();

        return invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateNetIncome(LocalDate startDate, LocalDate endDate) {
        return calculateTotalRevenue(startDate, endDate)
                .subtract(calculateTotalExpenses(startDate, endDate));
    }

    private BigDecimal calculateAccountsReceivable() {
        List<Invoice> invoices = DB.find(Invoice.class)
                .where()
                .isNotNull("customer")
                .eq("status", InvoiceStatus.DRAFT)
                .findList();

        return invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAccountsPayable() {
        List<Invoice> invoices = DB.find(Invoice.class)
                .where()
                .isNotNull("supplier")
                .eq("status", InvoiceStatus.DRAFT)
                .findList();

        return invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalBalanceByType(AccountType type) {
        List<Account> accounts = DB.find(Account.class)
                .where()
                .eq("type", type)
                .eq("active", true)
                .findList();

        return accounts.stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countCustomerInvoices(Long customerId) {
        return DB.find(Invoice.class)
                .where()
                .eq("customer.id", customerId)
                .findCount();
    }

    private BigDecimal calculateCustomerTotal(Long customerId) {
        List<Invoice> invoices = DB.find(Invoice.class)
                .where()
                .eq("customer.id", customerId)
                .findList();

        return invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
