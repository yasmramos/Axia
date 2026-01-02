package io.github.yasmramos.axia.validation;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountingValidator.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountingValidatorTest {

    private static AccountingValidator validator;
    private static AccountService accountService;
    private static JournalEntryService journalEntryService;

    @BeforeAll
    static void setUp() {
        validator = new AccountingValidator();
        
        AccountRepository accountRepo = new AccountRepository();
        JournalEntryRepository jeRepo = new JournalEntryRepository();
        
        accountService = new AccountService(accountRepo);
        journalEntryService = new JournalEntryService(jeRepo, accountRepo);
        
        accountService.initializeDefaultAccounts();
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Account.class);
    }

    // Balanced Entry Tests
    @Test
    @Order(1)
    @DisplayName("Should validate balanced entry")
    void testValidateBalancedEntry() {
        Account cash = accountService.findByCode("1.1.01").orElseThrow();
        Account revenue = accountService.findByCode("4.1.01").orElseThrow();
        
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Balanced", null);
        journalEntryService.addLine(entry, cash, new BigDecimal("100"), null, "Debit");
        journalEntryService.addLine(entry, revenue, null, new BigDecimal("100"), "Credit");
        
        JournalEntry refreshed = journalEntryService.findById(entry.getId()).orElseThrow();
        ValidationResult result = validator.validateBalancedEntry(refreshed);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Should reject unbalanced entry")
    void testValidateUnbalancedEntry() {
        Account cash = accountService.findByCode("1.1.01").orElseThrow();
        
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Unbalanced", null);
        journalEntryService.addLine(entry, cash, new BigDecimal("100"), null, "Debit only");
        
        JournalEntry refreshed = journalEntryService.findById(entry.getId()).orElseThrow();
        ValidationResult result = validator.validateBalancedEntry(refreshed);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Should reject empty entry")
    void testValidateEmptyEntry() {
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Empty", null);
        JournalEntry refreshed = journalEntryService.findById(entry.getId()).orElseThrow();
        
        ValidationResult result = validator.validateBalancedEntry(refreshed);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("at least one line")));
    }

    // Account Code Tests
    @Test
    @Order(4)
    @DisplayName("Should validate valid account code")
    void testValidateValidAccountCode() {
        ValidationResult result = validator.validateAccountCode("1000");
        
        assertTrue(result.isValid());
    }

    @Test
    @Order(5)
    @DisplayName("Should reject empty account code")
    void testValidateEmptyAccountCode() {
        ValidationResult result = validator.validateAccountCode("");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("cannot be empty")));
    }

    @Test
    @Order(6)
    @DisplayName("Should reject null account code")
    void testValidateNullAccountCode() {
        ValidationResult result = validator.validateAccountCode(null);
        
        assertFalse(result.isValid());
    }

    @Test
    @Order(7)
    @DisplayName("Should reject non-numeric account code")
    void testValidateNonNumericAccountCode() {
        ValidationResult result = validator.validateAccountCode("ABC123");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("must be numeric")));
    }

    // Tax ID Tests
    @Test
    @Order(8)
    @DisplayName("Should validate valid tax ID")
    void testValidateValidTaxId() {
        ValidationResult result = validator.validateTaxId("12345678A");
        
        assertTrue(result.isValid());
    }

    @Test
    @Order(9)
    @DisplayName("Should validate null tax ID")
    void testValidateNullTaxId() {
        ValidationResult result = validator.validateTaxId(null);
        
        assertTrue(result.isValid()); // null is acceptable
    }

    @Test
    @Order(10)
    @DisplayName("Should reject too short tax ID")
    void testValidateTooShortTaxId() {
        ValidationResult result = validator.validateTaxId("123");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("between 5 and 20")));
    }

    // Invoice Validation Tests
    @Test
    @Order(11)
    @DisplayName("Should validate customer invoice")
    void testValidateCustomerInvoice() {
        ValidationResult result = validator.validateInvoice(1L, null, new BigDecimal("100"));
        
        assertTrue(result.isValid());
    }

    @Test
    @Order(12)
    @DisplayName("Should validate supplier invoice")
    void testValidateSupplierInvoice() {
        ValidationResult result = validator.validateInvoice(null, 1L, new BigDecimal("100"));
        
        assertTrue(result.isValid());
    }

    @Test
    @Order(13)
    @DisplayName("Should reject invoice without customer or supplier")
    void testValidateInvoiceNoParty() {
        ValidationResult result = validator.validateInvoice(null, null, new BigDecimal("100"));
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("either a customer or supplier")));
    }

    @Test
    @Order(14)
    @DisplayName("Should reject invoice with both customer and supplier")
    void testValidateInvoiceBothParties() {
        ValidationResult result = validator.validateInvoice(1L, 1L, new BigDecimal("100"));
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("cannot have both")));
    }

    @Test
    @Order(15)
    @DisplayName("Should reject invoice with zero amount")
    void testValidateInvoiceZeroAmount() {
        ValidationResult result = validator.validateInvoice(1L, null, BigDecimal.ZERO);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("greater than zero")));
    }
}
