package io.github.yasmramos.axia.service;


import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.JournalEntryRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JournalEntryService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JournalEntryServiceTest {

    private static JournalEntryService journalEntryService;
    private static AccountService accountService;
    private static Account cashAccount;
    private static Account revenueAccount;

    @BeforeAll
    static void setUp() {
        AccountRepository accountRepository = new AccountRepository();
        JournalEntryRepository journalEntryRepository = new JournalEntryRepository();
        
        accountService = new AccountService(accountRepository);
        journalEntryService = new JournalEntryService(journalEntryRepository, accountRepository);
        
        // Create test accounts
        cashAccount = accountService.create("TEST-1", "Test Cash", AccountType.ASSET, null);
        revenueAccount = accountService.create("TEST-4", "Test Revenue", AccountType.INCOME, null);
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
    @DisplayName("Should create journal entry")
    void testCreateJournalEntry() {
        JournalEntry entry = journalEntryService.create(
                LocalDate.now(),
                "Test entry",
                "REF-001"
        );

        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertNotNull(entry.getEntryNumber());
        assertEquals("Test entry", entry.getDescription());
        assertEquals("REF-001", entry.getReference());
        assertFalse(entry.isPosted());
    }

    @Test
    @Order(2)
    @DisplayName("Should add lines to journal entry")
    void testAddLine() {
        JournalEntry entry = journalEntryService.create(
                LocalDate.now(),
                "Entry with lines",
                null
        );

        journalEntryService.addLine(entry, cashAccount, new BigDecimal("100.00"), null, "Debit cash");
        journalEntryService.addLine(entry, revenueAccount, null, new BigDecimal("100.00"), "Credit revenue");

        JournalEntry updated = journalEntryService.findById(entry.getId()).orElseThrow();
        assertEquals(2, updated.getLines().size());
        assertEquals(new BigDecimal("100.0000"), updated.getTotalDebit());
        assertEquals(new BigDecimal("100.0000"), updated.getTotalCredit());
        assertTrue(updated.isBalanced());
    }

    @Test
    @Order(3)
    @DisplayName("Should post balanced entry")
    void testPostEntry() {
        JournalEntry entry = journalEntryService.create(
                LocalDate.now(),
                "Entry to post",
                null
        );
        journalEntryService.addLine(entry, cashAccount, new BigDecimal("200.00"), null, "Debit");
        journalEntryService.addLine(entry, revenueAccount, null, new BigDecimal("200.00"), "Credit");

        BigDecimal cashBefore = cashAccount.getBalance();
        BigDecimal revenueBefore = revenueAccount.getBalance();

        JournalEntry posted = journalEntryService.post(entry.getId());

        assertTrue(posted.isPosted());
        
        // Verify balances updated
        Account updatedCash = accountService.findById(cashAccount.getId()).orElseThrow();
        Account updatedRevenue = accountService.findById(revenueAccount.getId()).orElseThrow();
        
        assertEquals(cashBefore.add(new BigDecimal("200.0000")), updatedCash.getBalance());
        assertEquals(revenueBefore.add(new BigDecimal("200.0000")), updatedRevenue.getBalance());
    }

    @Test
    @Order(4)
    @DisplayName("Should not post unbalanced entry")
    void testPostUnbalancedEntryThrowsException() {
        JournalEntry entry = journalEntryService.create(
                LocalDate.now(),
                "Unbalanced entry",
                null
        );
        journalEntryService.addLine(entry, cashAccount, new BigDecimal("100.00"), null, "Debit only");

        assertThrows(IllegalStateException.class, () -> 
            journalEntryService.post(entry.getId())
        );
    }

    @Test
    @Order(5)
    @DisplayName("Should not post empty entry")
    void testPostEmptyEntryThrowsException() {
        JournalEntry entry = journalEntryService.create(
                LocalDate.now(),
                "Empty entry",
                null
        );

        assertThrows(IllegalStateException.class, () -> 
            journalEntryService.post(entry.getId())
        );
    }

    @Test
    @Order(6)
    @DisplayName("Should not add lines to posted entry")
    void testAddLineToPostedEntryThrowsException() {
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Posted entry", null);
        journalEntryService.addLine(entry, cashAccount, new BigDecimal("50.00"), null, "Debit");
        journalEntryService.addLine(entry, revenueAccount, null, new BigDecimal("50.00"), "Credit");
        journalEntryService.post(entry.getId());

        JournalEntry posted = journalEntryService.findById(entry.getId()).orElseThrow();
        
        assertThrows(IllegalStateException.class, () -> 
            journalEntryService.addLine(posted, cashAccount, new BigDecimal("10.00"), null, "More")
        );
    }

    @Test
    @Order(7)
    @DisplayName("Should reverse posted entry")
    void testReverseEntry() {
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "To reverse", null);
        journalEntryService.addLine(entry, cashAccount, new BigDecimal("300.00"), null, "Debit");
        journalEntryService.addLine(entry, revenueAccount, null, new BigDecimal("300.00"), "Credit");
        journalEntryService.post(entry.getId());

        int entryCount = journalEntryService.findAll().size();
        
        journalEntryService.reverse(entry.getId(), LocalDate.now(), "Reversal");
        
        assertEquals(entryCount + 1, journalEntryService.findAll().size());
    }

    @Test
    @Order(8)
    @DisplayName("Should find entries by date range")
    void testFindByDateRange() {
        LocalDate today = LocalDate.now();
        var entries = journalEntryService.findByDateRange(today.minusDays(1), today.plusDays(1));
        
        assertFalse(entries.isEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("Should delete unposted entry")
    void testDeleteUnpostedEntry() {
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "To delete", null);
        Long id = entry.getId();
        
        journalEntryService.delete(id);
        
        assertTrue(journalEntryService.findById(id).isEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("Should not delete posted entry")
    void testDeletePostedEntryThrowsException() {
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Posted to delete", null);
        journalEntryService.addLine(entry, cashAccount, new BigDecimal("10.00"), null, "D");
        journalEntryService.addLine(entry, revenueAccount, null, new BigDecimal("10.00"), "C");
        journalEntryService.post(entry.getId());

        assertThrows(IllegalStateException.class, () -> 
            journalEntryService.delete(entry.getId())
        );
    }
}
