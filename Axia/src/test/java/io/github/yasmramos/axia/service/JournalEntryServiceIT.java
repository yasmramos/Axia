package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.FiscalYear;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryLine;
import io.github.yasmramos.axia.repository.FiscalYearRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete accounting journal workflows.
 * 
 * <p>Tests the full lifecycle of journal entries including creation,
 * validation, posting, and subsequent operations.</p>
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JournalEntryServiceIT {

    private static JournalEntryService journalEntryService;
    private static AccountService accountService;
    private static FiscalYearService fiscalYearService;
    private static FiscalYear currentYear;

    @BeforeAll
    static void setUp() {
        accountService = new AccountService(new io.github.yasmramos.axia.repository.AccountRepository());
        fiscalYearService = new FiscalYearService(new FiscalYearRepository());
        journalEntryService = new JournalEntryService(
            new io.github.yasmramos.axia.repository.JournalEntryRepository(),
            accountService
        );

        // Initialize fiscal year
        int year = LocalDate.now().getYear();
        if (fiscalYearService.findByYear(year).isEmpty()) {
            currentYear = fiscalYearService.create(year,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
            fiscalYearService.setCurrent(currentYear.getId());
        } else {
            currentYear = fiscalYearService.findByYear(year).orElseThrow();
        }

        // Initialize accounts
        accountService.initializeDefaultAccounts();
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Account.class);
        db.truncate(FiscalYear.class);
    }

    @Test
    @Order(1)
    @DisplayName("Complete journal entry workflow: Create and validate balanced entry")
    void testCompleteBalancedEntryWorkflow() {
        // Find required accounts
        Account cashAccount = accountService.findByCode("1.1.01").orElseThrow();
        Account revenueAccount = accountService.findByCode("4.1.01").orElseThrow();

        // Create journal entry
        JournalEntry entry = journalEntryService.create(
            LocalDate.now(),
            "Test Revenue Entry",
            "IT-TEST-001"
        );

        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertFalse(entry.isPosted());
        assertEquals(0, entry.getVersion());

        // Add debit line (Cash)
        JournalEntryLine debitLine = journalEntryService.addLine(
            entry.getId(),
            cashAccount.getId(),
            1000.00,
            0.00,
            "Cash received from customer"
        );

        assertNotNull(debitLine);
        assertEquals(1000.00, debitLine.getDebit());
        assertEquals(0.00, debitLine.getCredit());

        // Add credit line (Revenue)
        JournalEntryLine creditLine = journalEntryService.addLine(
            entry.getId(),
            revenueAccount.getId(),
            0.00,
            1000.00,
            "Service revenue recognition"
        );

        assertNotNull(creditLine);
        assertEquals(0.00, creditLine.getDebit());
        assertEquals(1000.00, creditLine.getCredit());

        // Validate balance
        JournalEntry validatedEntry = journalEntryService.findById(entry.getId()).orElseThrow();
        assertTrue(journalEntryService.isBalanced(validatedEntry));
        assertEquals(1000.00, journalEntryService.getTotalDebit(validatedEntry));
        assertEquals(1000.00, journalEntryService.getTotalCredit(validatedEntry));
    }

    @Test
    @Order(2)
    @DisplayName("Complete journal entry workflow: Post entry and verify account balances")
    void testPostEntryWorkflow() {
        // Create a new balanced entry
        Account cashAccount = accountService.findByCode("1.1.01").orElseThrow();
        Account expenseAccount = accountService.findByCode("5.1.01").orElseThrow();

        JournalEntry entry = journalEntryService.create(
            LocalDate.now(),
            "Expense Payment Entry",
            "IT-TEST-002"
        );

        journalEntryService.addLine(entry.getId(), cashAccount.getId(), 0.00, 500.00, "Payment made");
        journalEntryService.addLine(entry.getId(), expenseAccount.getId(), 500.00, 0.00, "Expense incurred");

        assertTrue(journalEntryService.isBalanced(entry));

        // Post the entry
        JournalEntry postedEntry = journalEntryService.post(entry.getId());

        assertTrue(postedEntry.isPosted());
        assertNotNull(postedEntry.getPostedAt());
        assertEquals(1, postedEntry.getVersion());

        // Verify account balances updated
        Account updatedCash = accountService.findById(cashAccount.getId()).orElseThrow();
        assertEquals(-500.00, updatedCash.getBalance()); // Debit was 0, Credit was 500

        Account updatedExpense = accountService.findById(expenseAccount.getId()).orElseThrow();
        assertEquals(500.00, updatedExpense.getBalance()); // Debit was 500, Credit was 0
    }

    @Test
    @Order(3)
    @DisplayName("Verify posted entries cannot be modified")
    void testPostedEntryCannotBeModified() {
        // Find existing entry
        List<JournalEntry> entries = journalEntryService.findByReference("IT-TEST-001");
        assertFalse(entries.isEmpty());

        JournalEntry postedEntry = entries.stream()
            .filter(JournalEntry::isPosted)
            .findFirst()
            .orElseThrow();

        // Attempting to add line should throw exception
        Account anyAccount = accountService.findByCode("1.1.01").orElseThrow();
        assertThrows(IllegalStateException.class, () ->
            journalEntryService.addLine(postedEntry.getId(), anyAccount.getId(), 100, 0, "Should fail")
        );

        // Attempting to delete should throw exception
        assertThrows(IllegalStateException.class, () ->
            journalEntryService.deleteLine(postedEntry.getId(), 
                postedEntry.getLines().get(0).getId())
        );
    }

    @Test
    @Order(4)
    @DisplayName("Verify entry cannot be posted if unbalanced")
    void testUnbalancedEntryCannotBePosted() {
        // Create unbalanced entry
        Account cashAccount = accountService.findByCode("1.1.01").orElseThrow();

        JournalEntry entry = journalEntryService.create(
            LocalDate.now(),
            "Unbalanced Entry",
            "IT-TEST-003"
        );

        // Add only debit line (no credit)
        journalEntryService.addLine(entry.getId(), cashAccount.getId(), 100.00, 0.00, "Only debit");

        assertFalse(journalEntryService.isBalanced(entry));

        // Attempting to post should throw exception
        assertThrows(IllegalArgumentException.class, () ->
            journalEntryService.post(entry.getId())
        );
    }

    @Test
    @Order(5)
    @DisplayName("Test entry retrieval with filters")
    void testEntryRetrievalWithFilters() {
        // Get entries by date range
        LocalDate today = LocalDate.now();
        List<JournalEntry> todayEntries = journalEntryService.findByDateRange(
            today.minusDays(1),
            today.plusDays(1)
        );

        assertFalse(todayEntries.isEmpty());
        assertTrue(todayEntries.size() >= 3); // At least our IT test entries

        // Get entries by reference pattern
        List<JournalEntry> itTestEntries = journalEntryService.findByReference("IT-TEST");
        assertEquals(3, itTestEntries.size());

        // Get posted entries only
        List<JournalEntry> postedEntries = journalEntryService.findPosted();
        assertTrue(postedEntries.size() >= 1);

        // Verify each posted entry has posted date
        for (JournalEntry posted : postedEntries) {
            assertTrue(posted.isPosted());
            assertNotNull(posted.getPostedAt());
        }
    }
}
