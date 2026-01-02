package io.github.yasmramos.axia.dashboard;

import io.github.yasmramos.axia.EmbeddedPostgresExtension;
import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.*;
import io.github.yasmramos.axia.service.*;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DashboardService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DashboardServiceTest {

    private static DashboardService dashboardService;
    private static AccountService accountService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static InvoiceService invoiceService;

    @BeforeAll
    static void setUp() {
        dashboardService = new DashboardService();
        
        AccountRepository accountRepo = new AccountRepository();
        JournalEntryRepository jeRepo = new JournalEntryRepository();
        JournalEntryService jes = new JournalEntryService(jeRepo, accountRepo);
        
        accountService = new AccountService(accountRepo);
        customerService = new CustomerService(new CustomerRepository());
        supplierService = new SupplierService(new SupplierRepository());
        invoiceService = new InvoiceService(new InvoiceRepository(), accountRepo, jes);
        
        // Initialize accounts
        accountService.initializeDefaultAccounts();
        
        // Create test data
        Customer customer = customerService.create("DASH-CUST", "Dashboard Customer", null, null, null, null, null);
        Supplier supplier = supplierService.create("DASH-SUPP", "Dashboard Supplier", null, null, null, null, null);
        
        // Create invoices
        Account salesAccount = accountService.findByCode("4.1.01").orElse(null);
        Invoice saleInvoice = invoiceService.createSaleInvoice(customer, LocalDate.now(), LocalDate.now().plusDays(30));
        invoiceService.addLine(saleInvoice, "Product", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("21"), salesAccount);
        invoiceService.post(saleInvoice.getId());
        invoiceService.markAsPaid(saleInvoice.getId());
        
        Invoice purchaseInvoice = invoiceService.createPurchaseInvoice(supplier, LocalDate.now(), LocalDate.now().plusDays(30));
        invoiceService.addLine(purchaseInvoice, "Expense", new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("21"), salesAccount);
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(InvoiceLine.class);
        db.truncate(Invoice.class);
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Customer.class);
        db.truncate(Supplier.class);
        db.truncate(Account.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should get summary stats")
    void testGetSummaryStats() {
        Map<String, Object> stats = dashboardService.getSummaryStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalAccounts"));
        assertTrue(stats.containsKey("activeAccounts"));
        assertTrue(stats.containsKey("totalCustomers"));
        assertTrue(stats.containsKey("totalSuppliers"));
        assertTrue(stats.containsKey("totalInvoices"));
        assertTrue((Long) stats.get("totalAccounts") > 0);
    }

    @Test
    @Order(2)
    @DisplayName("Should get financial KPIs")
    void testGetFinancialKPIs() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now().plusMonths(1);
        
        Map<String, BigDecimal> kpis = dashboardService.getFinancialKPIs(startDate, endDate);
        
        assertNotNull(kpis);
        assertTrue(kpis.containsKey("totalRevenue"));
        assertTrue(kpis.containsKey("totalExpenses"));
        assertTrue(kpis.containsKey("netIncome"));
        assertTrue(kpis.containsKey("accountsReceivable"));
        assertTrue(kpis.containsKey("accountsPayable"));
    }

    @Test
    @Order(3)
    @DisplayName("Should get invoices by status")
    void testGetInvoicesByStatus() {
        Map<String, Long> statusCounts = dashboardService.getInvoicesByStatus();
        
        assertNotNull(statusCounts);
        assertTrue(statusCounts.containsKey("DRAFT"));
        assertTrue(statusCounts.containsKey("POSTED"));
        assertTrue(statusCounts.containsKey("PAID"));
    }

    @Test
    @Order(4)
    @DisplayName("Should get account balances by type")
    void testGetAccountBalancesByType() {
        Map<String, BigDecimal> balances = dashboardService.getAccountBalancesByType();
        
        assertNotNull(balances);
        assertTrue(balances.containsKey("ASSET"));
        assertTrue(balances.containsKey("LIABILITY"));
        assertTrue(balances.containsKey("EQUITY"));
        assertTrue(balances.containsKey("INCOME"));
        assertTrue(balances.containsKey("EXPENSE"));
    }

    @Test
    @Order(5)
    @DisplayName("Should get monthly revenue trend")
    void testGetMonthlyRevenueTrend() {
        int currentYear = LocalDate.now().getYear();
        Map<Integer, BigDecimal> trend = dashboardService.getMonthlyRevenueTrend(currentYear);
        
        assertNotNull(trend);
        assertEquals(12, trend.size());
        for (int month = 1; month <= 12; month++) {
            assertTrue(trend.containsKey(month));
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should get top customers")
    void testGetTopCustomers() {
        List<Map<String, Object>> topCustomers = dashboardService.getTopCustomers(5);
        
        assertNotNull(topCustomers);
        assertFalse(topCustomers.isEmpty());
        
        Map<String, Object> first = topCustomers.get(0);
        assertTrue(first.containsKey("id"));
        assertTrue(first.containsKey("name"));
        assertTrue(first.containsKey("invoiceCount"));
        assertTrue(first.containsKey("totalAmount"));
    }
}
