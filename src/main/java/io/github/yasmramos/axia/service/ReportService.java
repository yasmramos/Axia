package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.InvoiceRepository;
import io.github.yasmramos.axia.repository.JournalEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for financial report generation.
 *
 * <p>Generates standard accounting reports including:
 * <ul>
 *   <li>Balance Sheet</li>
 *   <li>Income Statement</li>
 *   <li>Trial Balance</li>
 *   <li>General Ledger</li>
 *   <li>Journal Book</li>
 * </ul>
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final InvoiceRepository invoiceRepository;

    @Inject
    public ReportService(AccountRepository accountRepository, JournalEntryRepository journalEntryRepository,
                        InvoiceRepository invoiceRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // Balance General
    public Map<String, Object> getBalanceSheet(LocalDate asOfDate) {
        Map<String, Object> balanceSheet = new LinkedHashMap<>();

        List<Account> assets = accountRepository.findByType(AccountType.ASSET);
        List<Account> liabilities = accountRepository.findByType(AccountType.LIABILITY);
        List<Account> equity = accountRepository.findByType(AccountType.EQUITY);

        BigDecimal totalAssets = sumBalances(assets);
        BigDecimal totalLiabilities = sumBalances(liabilities);
        BigDecimal totalEquity = sumBalances(equity);

        balanceSheet.put("fecha", asOfDate);
        balanceSheet.put("activos", buildAccountList(assets));
        balanceSheet.put("totalActivos", totalAssets);
        balanceSheet.put("pasivos", buildAccountList(liabilities));
        balanceSheet.put("totalPasivos", totalLiabilities);
        balanceSheet.put("patrimonio", buildAccountList(equity));
        balanceSheet.put("totalPatrimonio", totalEquity);
        balanceSheet.put("totalPasivoPatrimonio", totalLiabilities.add(totalEquity));

        return balanceSheet;
    }

    // Estado de Resultados
    public Map<String, Object> getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> incomeStatement = new LinkedHashMap<>();

        List<Account> incomeAccounts = accountRepository.findByType(AccountType.INCOME);
        List<Account> expenseAccounts = accountRepository.findByType(AccountType.EXPENSE);

        BigDecimal totalIncome = sumBalances(incomeAccounts);
        BigDecimal totalExpenses = sumBalances(expenseAccounts);
        BigDecimal netIncome = totalIncome.subtract(totalExpenses);

        incomeStatement.put("fechaInicio", startDate);
        incomeStatement.put("fechaFin", endDate);
        incomeStatement.put("ingresos", buildAccountList(incomeAccounts));
        incomeStatement.put("totalIngresos", totalIncome);
        incomeStatement.put("gastos", buildAccountList(expenseAccounts));
        incomeStatement.put("totalGastos", totalExpenses);
        incomeStatement.put("utilidadNeta", netIncome);

        return incomeStatement;
    }

    // Libro Mayor
    public Map<String, Object> getLedger(Account account, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> ledger = new LinkedHashMap<>();

        List<JournalEntryLine> lines = journalEntryRepository.findLinesByAccount(account, startDate, endDate);

        BigDecimal runningBalance = BigDecimal.ZERO;
        List<Map<String, Object>> movements = new ArrayList<>();

        for (JournalEntryLine line : lines) {
            Map<String, Object> movement = new LinkedHashMap<>();
            movement.put("fecha", line.getJournalEntry().getDate());
            movement.put("asiento", line.getJournalEntry().getEntryNumber());
            movement.put("descripcion", line.getDescription());
            movement.put("debe", line.getDebit());
            movement.put("haber", line.getCredit());

            if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
                runningBalance = runningBalance.add(line.getDebit()).subtract(line.getCredit());
            } else {
                runningBalance = runningBalance.subtract(line.getDebit()).add(line.getCredit());
            }
            movement.put("saldo", runningBalance);

            movements.add(movement);
        }

        ledger.put("cuenta", account.getCode() + " - " + account.getName());
        ledger.put("fechaInicio", startDate);
        ledger.put("fechaFin", endDate);
        ledger.put("movimientos", movements);
        ledger.put("saldoFinal", runningBalance);

        return ledger;
    }

    // Libro Diario
    public Map<String, Object> getJournal(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> journal = new LinkedHashMap<>();

        List<JournalEntry> entries = journalEntryRepository.findByDateRange(startDate, endDate);
        List<Map<String, Object>> entryList = new ArrayList<>();

        for (JournalEntry entry : entries) {
            Map<String, Object> entryMap = new LinkedHashMap<>();
            entryMap.put("numero", entry.getEntryNumber());
            entryMap.put("fecha", entry.getDate());
            entryMap.put("descripcion", entry.getDescription());
            entryMap.put("referencia", entry.getReference());
            entryMap.put("contabilizado", entry.isPosted());

            List<Map<String, Object>> lineList = new ArrayList<>();
            for (JournalEntryLine line : entry.getLines()) {
                Map<String, Object> lineMap = new LinkedHashMap<>();
                lineMap.put("cuenta", line.getAccount().getCode() + " - " + line.getAccount().getName());
                lineMap.put("descripcion", line.getDescription());
                lineMap.put("debe", line.getDebit());
                lineMap.put("haber", line.getCredit());
                lineList.add(lineMap);
            }
            entryMap.put("lineas", lineList);
            entryMap.put("totalDebe", entry.getTotalDebit());
            entryMap.put("totalHaber", entry.getTotalCredit());

            entryList.add(entryMap);
        }

        journal.put("fechaInicio", startDate);
        journal.put("fechaFin", endDate);
        journal.put("asientos", entryList);

        return journal;
    }

    // Balance de Comprobación
    public Map<String, Object> getTrialBalance(LocalDate asOfDate) {
        Map<String, Object> trialBalance = new LinkedHashMap<>();

        List<Account> allAccounts = accountRepository.findAll();
        List<Map<String, Object>> accountList = new ArrayList<>();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Account account : allAccounts) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> accountMap = new LinkedHashMap<>();
                accountMap.put("codigo", account.getCode());
                accountMap.put("nombre", account.getName());

                if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
                    if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        accountMap.put("debe", account.getBalance());
                        accountMap.put("haber", BigDecimal.ZERO);
                        totalDebit = totalDebit.add(account.getBalance());
                    } else {
                        accountMap.put("debe", BigDecimal.ZERO);
                        accountMap.put("haber", account.getBalance().abs());
                        totalCredit = totalCredit.add(account.getBalance().abs());
                    }
                } else {
                    if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        accountMap.put("debe", BigDecimal.ZERO);
                        accountMap.put("haber", account.getBalance());
                        totalCredit = totalCredit.add(account.getBalance());
                    } else {
                        accountMap.put("debe", account.getBalance().abs());
                        accountMap.put("haber", BigDecimal.ZERO);
                        totalDebit = totalDebit.add(account.getBalance().abs());
                    }
                }

                accountList.add(accountMap);
            }
        }

        trialBalance.put("fecha", asOfDate);
        trialBalance.put("cuentas", accountList);
        trialBalance.put("totalDebe", totalDebit);
        trialBalance.put("totalHaber", totalCredit);
        trialBalance.put("cuadrado", totalDebit.compareTo(totalCredit) == 0);

        return trialBalance;
    }

    private BigDecimal sumBalances(List<Account> accounts) {
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> buildAccountList(List<Account> accounts) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Account account : accounts) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("codigo", account.getCode());
                map.put("nombre", account.getName());
                map.put("saldo", account.getBalance());
                list.add(map);
            }
        }
        return list;
    }
}
