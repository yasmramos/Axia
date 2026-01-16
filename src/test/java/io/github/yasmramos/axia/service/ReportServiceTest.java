package io.github.yasmramos.axia.service;


import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.JournalEntryRepository;
import io.github.yasmramos.axia.repository.InvoiceRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportServiceTest {

    private static ReportService reportService;
    private static AccountService accountService;
    private static JournalEntryService journalEntryService;

    @BeforeAll
    static void setUp() {
        AccountRepository accountRepository = new AccountRepository();
        JournalEntryRepository journalEntryRepository = new JournalEntryRepository();
        InvoiceRepository invoiceRepository = new InvoiceRepository();
        
        reportService = new ReportService(accountRepository, journalEntryRepository, invoiceRepository);
        accountService = new AccountService(accountRepository);
        journalEntryService = new JournalEntryService(journalEntryRepository, accountRepository);
        
        // Initialize accounts and create some transactions
        accountService.initializeDefaultAccounts();
        
        Account cash = accountService.findByCode("1.1.01").orElseThrow();
        Account revenue = accountService.findByCode("4.1.01").orElseThrow();
        
        // Create and post a transaction
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Test revenue", null);
        journalEntryService.addLine(entry, cash, new BigDecimal("1000"), null, "Cash received");
        journalEntryService.addLine(entry, revenue, null, new BigDecimal("1000"), "Sales revenue");
        journalEntryService.post(entry.getId());
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Account.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should generate balance sheet")
    void testGetBalanceSheet() {
        Map<String, Object> balanceSheet = reportService.getBalanceSheet(LocalDate.now());

        assertNotNull(balanceSheet);
        assertTrue(balanceSheet.containsKey("activos"));
        assertTrue(balanceSheet.containsKey("totalActivos"));
        assertTrue(balanceSheet.containsKey("pasivos"));
        assertTrue(balanceSheet.containsKey("patrimonio"));
    }

    @Test
    @Order(2)
    @DisplayName("Should generate income statement")
    void testGetIncomeStatement() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        Map<String, Object> incomeStatement = reportService.getIncomeStatement(startDate, endDate);

        assertNotNull(incomeStatement);
        assertTrue(incomeStatement.containsKey("ingresos"));
        assertTrue(incomeStatement.containsKey("totalIngresos"));
        assertTrue(incomeStatement.containsKey("gastos"));
        assertTrue(incomeStatement.containsKey("utilidadNeta"));
    }

    @Test
    @Order(3)
    @DisplayName("Should generate trial balance")
    void testGetTrialBalance() {
        Map<String, Object> trialBalance = reportService.getTrialBalance(LocalDate.now());

        assertNotNull(trialBalance);
        assertTrue(trialBalance.containsKey("cuentas"));
        assertTrue(trialBalance.containsKey("totalDebe"));
        assertTrue(trialBalance.containsKey("totalHaber"));
        assertTrue(trialBalance.containsKey("cuadrado"));
        
        // Trial balance should be balanced
        assertTrue((Boolean) trialBalance.get("cuadrado"));
    }

    @Test
    @Order(4)
    @DisplayName("Should generate ledger for account")
    void testGetLedger() {
        Account cash = accountService.findByCode("1.1.01").orElseThrow();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        Map<String, Object> ledger = reportService.getLedger(cash, startDate, endDate);

        assertNotNull(ledger);
        assertTrue(ledger.containsKey("cuenta"));
        assertTrue(ledger.containsKey("movimientos"));
        assertTrue(ledger.containsKey("saldoFinal"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> movements = (List<Map<String, Object>>) ledger.get("movimientos");
        assertFalse(movements.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Should generate journal book")
    void testGetJournal() {
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        Map<String, Object> journal = reportService.getJournal(startDate, endDate);

        assertNotNull(journal);
        assertTrue(journal.containsKey("asientos"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) journal.get("asientos");
        assertFalse(entries.isEmpty());
        
        // Each entry should have lines
        Map<String, Object> firstEntry = entries.get(0);
        assertTrue(firstEntry.containsKey("lineas"));
        assertTrue(firstEntry.containsKey("totalDebe"));
        assertTrue(firstEntry.containsKey("totalHaber"));
    }
}
