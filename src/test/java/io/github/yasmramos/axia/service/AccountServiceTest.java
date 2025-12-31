package io.github.yasmramos.axia.service;


import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.model.AccountType;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountServiceTest {

    private static AccountService accountService;

    @BeforeAll
    static void setUp() {
        accountService = new AccountService(new AccountRepository());
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(Account.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should create account successfully")
    void testCreateAccount() {
        Account account = accountService.create("1", "Assets", AccountType.ASSET, null);

        assertNotNull(account);
        assertNotNull(account.getId());
        assertEquals("1", account.getCode());
        assertEquals("Assets", account.getName());
        assertEquals(AccountType.ASSET, account.getType());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertTrue(account.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Should create child account with correct level")
    void testCreateChildAccount() {
        Account parent = accountService.findByCode("1").orElseThrow();
        Account child = accountService.create("1.1", "Current Assets", AccountType.ASSET, parent);

        assertNotNull(child);
        assertEquals(2, child.getLevel());
        assertEquals(parent.getId(), child.getParent().getId());
    }

    @Test
    @Order(3)
    @DisplayName("Should throw exception for duplicate code")
    void testDuplicateCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            accountService.create("1", "Duplicate", AccountType.ASSET, null)
        );
    }

    @Test
    @Order(4)
    @DisplayName("Should find account by code")
    void testFindByCode() {
        var account = accountService.findByCode("1");
        
        assertTrue(account.isPresent());
        assertEquals("Assets", account.get().getName());
    }

    @Test
    @Order(5)
    @DisplayName("Should find accounts by type")
    void testFindByType() {
        List<Account> assets = accountService.findByType(AccountType.ASSET);
        
        assertFalse(assets.isEmpty());
        assertTrue(assets.stream().allMatch(a -> a.getType() == AccountType.ASSET));
    }

    @Test
    @Order(6)
    @DisplayName("Should update account")
    void testUpdateAccount() {
        Account account = accountService.findByCode("1").orElseThrow();
        account.setName("Total Assets");
        
        accountService.update(account);
        
        Account updated = accountService.findById(account.getId()).orElseThrow();
        assertEquals("Total Assets", updated.getName());
    }

    @Test
    @Order(7)
    @DisplayName("Should not delete account with children")
    void testDeleteAccountWithChildrenThrowsException() {
        Account parent = accountService.findByCode("1").orElseThrow();
        
        assertThrows(IllegalArgumentException.class, () -> 
            accountService.delete(parent.getId())
        );
    }

    @Test
    @Order(8)
    @DisplayName("Should debit asset account correctly")
    void testDebitAssetAccount() {
        Account account = accountService.findByCode("1.1").orElseThrow();
        BigDecimal initialBalance = account.getBalance();
        
        account.debit(new BigDecimal("100.00"));
        
        assertEquals(initialBalance.add(new BigDecimal("100.00")), account.getBalance());
    }

    @Test
    @Order(9)
    @DisplayName("Should credit asset account correctly")
    void testCreditAssetAccount() {
        Account account = accountService.findByCode("1.1").orElseThrow();
        BigDecimal initialBalance = account.getBalance();
        
        account.credit(new BigDecimal("50.00"));
        
        assertEquals(initialBalance.subtract(new BigDecimal("50.00")), account.getBalance());
    }
}
