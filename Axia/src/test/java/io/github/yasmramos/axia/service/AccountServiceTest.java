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

    @Test
    @Order(10)
    @DisplayName("Should find account by ID")
    void testFindById() {
        Account account = accountService.findByCode("1").orElseThrow();
        var found = accountService.findById(account.getId());
        
        assertTrue(found.isPresent());
        assertEquals("Assets", found.get().getName());
    }

    @Test
    @Order(11)
    @DisplayName("Should return empty when ID not found")
    void testFindByIdNotFound() {
        var result = accountService.findById(999999L);
        
        assertFalse(result.isPresent());
    }

    @Test
    @Order(12)
    @DisplayName("Should find all accounts")
    void testFindAll() {
        List<Account> all = accountService.findAll();
        
        assertFalse(all.isEmpty());
        assertTrue(all.size() >= 3);
    }

    @Test
    @Order(13)
    @DisplayName("Should find root accounts")
    void testFindRootAccounts() {
        List<Account> roots = accountService.findRoots();
        
        assertFalse(roots.isEmpty());
        assertTrue(roots.stream().allMatch(a -> a.getParent() == null));
    }

    @Test
    @Order(14)
    @DisplayName("Should find children accounts")
    void testFindChildren() {
        Account parent = accountService.findByCode("1").orElseThrow();
        List<Account> children = accountService.findChildren(parent.getId());
        
        assertFalse(children.isEmpty());
        assertTrue(children.stream().allMatch(c -> c.getParent() != null && 
            c.getParent().getId().equals(parent.getId())));
    }

    @Test
    @Order(15)
    @DisplayName("Should throw exception when creating account with invalid parent")
    void testCreateWithInvalidParent() {
        assertThrows(IllegalArgumentException.class, () -> 
            accountService.create("9.9", "Invalid", AccountType.ASSET, new Account())
        );
    }

    @Test
    @Order(16)
    @DisplayName("Should handle null balance in account")
    void testAccountWithNullBalance() {
        Account account = accountService.create("8", "Test Account", AccountType.EXPENSE, null);
        
        assertNotNull(account);
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    @Order(17)
    @DisplayName("Should create account with all types")
    void testCreateAccountWithAllTypes() {
        for (AccountType type : AccountType.values()) {
            String code = "9." + type.ordinal();
            Account account = accountService.create(code, type.name(), type, null);
            
            assertNotNull(account);
            assertEquals(type, account.getType());
        }
    }

    @Test
    @Order(18)
    @DisplayName("Should deactivate account correctly")
    void testDeactivateAccount() {
        Account account = accountService.create("7", "To Deactivate", AccountType.LIABILITY, null);
        Long id = account.getId();
        
        accountService.deactivate(id);
        
        Account deactivated = accountService.findById(id).orElseThrow();
        assertFalse(deactivated.isActive());
    }

    @Test
    @Order(19)
    @DisplayName("Should throw exception when deleting non-existent account")
    void testDeleteNonExistentAccount() {
        assertThrows(IllegalArgumentException.class, () -> 
            accountService.delete(999999L)
        );
    }

    @Test
    @Order(20)
    @DisplayName("Should search accounts by name")
    void testSearchByName() {
        List<Account> results = accountService.searchByName("Assets");
        
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(a -> a.getName().contains("Assets")));
    }

    @Test
    @Order(21)
    @DisplayName("Should return empty when searching by name with no results")
    void testSearchByNameNoResults() {
        List<Account> results = accountService.searchByName("NonExistentAccount12345");
        
        assertTrue(results.isEmpty());
    }

    @Test
    @Order(22)
    @DisplayName("Should count accounts correctly")
    void testCountAccounts() {
        long count = accountService.count();
        
        assertTrue(count >= 3);
    }

    @Test
    @Order(23)
    @DisplayName("Should find active accounts only")
    void testFindActiveAccounts() {
        // Create and deactivate an account
        Account account = accountService.create("6", "To Deactivate", AccountType.INCOME, null);
        accountService.deactivate(account.getId());
        
        List<Account> active = accountService.findActive();
        
        assertFalse(active.isEmpty());
        assertTrue(active.stream().allMatch(Account::isActive));
    }
}
