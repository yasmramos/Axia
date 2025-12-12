package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.AccountType;
import io.github.yasmramos.axia.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Account business operations.
 *
 * <p>Provides account management including creation, updates,
 * and initialization of the default chart of accounts.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService() {
        this.accountRepository = new AccountRepository();
    }

    public Account create(String code, String name, AccountType type, Account parent) {
        if (accountRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el código: " + code);
        }

        Account account = new Account();
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setParent(parent);
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);

        accountRepository.save(account);
        return account;
    }

    public Account update(Account account) {
        accountRepository.update(account);
        return account;
    }

    public void delete(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));

        if (!account.getChildren().isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar una cuenta con subcuentas");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("No se puede eliminar una cuenta con saldo");
        }

        accountRepository.delete(account);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> findByCode(String code) {
        return accountRepository.findByCode(code);
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public List<Account> findByType(AccountType type) {
        return accountRepository.findByType(type);
    }

    public List<Account> findActive() {
        return accountRepository.findActive();
    }

    public List<Account> findRootAccounts() {
        return accountRepository.findRootAccounts();
    }

    public List<Account> getChartOfAccounts() {
        return accountRepository.findAll();
    }

    public void initializeDefaultAccounts() {
        if (!accountRepository.findAll().isEmpty()) {
            return;
        }

        // Activos
        Account activos = create("1", "ACTIVOS", AccountType.ASSET, null);
        create("1.1", "Activo Corriente", AccountType.ASSET, activos);
        create("1.1.01", "Caja", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.1.02", "Bancos", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.1.03", "Cuentas por Cobrar", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.2", "Activo No Corriente", AccountType.ASSET, activos);

        // Pasivos
        Account pasivos = create("2", "PASIVOS", AccountType.LIABILITY, null);
        create("2.1", "Pasivo Corriente", AccountType.LIABILITY, pasivos);
        create("2.1.01", "Cuentas por Pagar", AccountType.LIABILITY, findByCode("2.1").orElse(null));
        create("2.1.02", "Impuestos por Pagar", AccountType.LIABILITY, findByCode("2.1").orElse(null));

        // Patrimonio
        Account patrimonio = create("3", "PATRIMONIO", AccountType.EQUITY, null);
        create("3.1", "Capital Social", AccountType.EQUITY, patrimonio);
        create("3.2", "Utilidades Retenidas", AccountType.EQUITY, patrimonio);

        // Ingresos
        Account ingresos = create("4", "INGRESOS", AccountType.INCOME, null);
        create("4.1", "Ingresos Operacionales", AccountType.INCOME, ingresos);
        create("4.1.01", "Ventas", AccountType.INCOME, findByCode("4.1").orElse(null));

        // Gastos
        Account gastos = create("5", "GASTOS", AccountType.EXPENSE, null);
        create("5.1", "Gastos Operacionales", AccountType.EXPENSE, gastos);
        create("5.1.01", "Gastos de Personal", AccountType.EXPENSE, findByCode("5.1").orElse(null));
        create("5.1.02", "Gastos Administrativos", AccountType.EXPENSE, findByCode("5.1").orElse(null));
    }
}
