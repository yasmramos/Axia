package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.config.DatabaseConfig;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryLine;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.JournalEntryRepository;
import io.ebean.Database;
import io.ebean.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for JournalEntry business operations.
 *
 * <p>Manages journal entry lifecycle including creation,
 * posting, and reversal with proper account balance updates.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final Database db;

    public JournalEntryService() {
        this.journalEntryRepository = new JournalEntryRepository();
        this.accountRepository = new AccountRepository();
        this.db = DatabaseConfig.getDatabase();
    }

    public JournalEntry create(LocalDate date, String description, String reference) {
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(journalEntryRepository.getNextEntryNumber());
        entry.setDate(date);
        entry.setDescription(description);
        entry.setReference(reference);
        entry.setPosted(false);

        journalEntryRepository.save(entry);
        return entry;
    }

    public JournalEntry addLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        if (entry.isPosted()) {
            throw new IllegalStateException("No se pueden agregar líneas a un asiento contabilizado");
        }

        JournalEntryLine line = new JournalEntryLine();
        line.setAccount(account);
        line.setDebit(debit != null ? debit : BigDecimal.ZERO);
        line.setCredit(credit != null ? credit : BigDecimal.ZERO);
        line.setDescription(description);

        entry.addLine(line);
        journalEntryRepository.update(entry);

        return entry;
    }

    public JournalEntry post(Long entryId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));

        if (entry.isPosted()) {
            throw new IllegalStateException("El asiento ya está contabilizado");
        }

        if (!entry.isBalanced()) {
            throw new IllegalStateException("El asiento no está cuadrado. Débito: " +
                    entry.getTotalDebit() + ", Crédito: " + entry.getTotalCredit());
        }

        if (entry.getLines().isEmpty()) {
            throw new IllegalStateException("El asiento no tiene líneas");
        }

        try (Transaction txn = db.beginTransaction()) {
            for (JournalEntryLine line : entry.getLines()) {
                Account account = line.getAccount();
                account.debit(line.getDebit());
                account.credit(line.getCredit());
                accountRepository.update(account);
            }

            entry.setPosted(true);
            journalEntryRepository.update(entry);

            txn.commit();
        }

        return entry;
    }

    public void reverse(Long entryId, LocalDate reversalDate, String description) {
        JournalEntry original = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));

        if (!original.isPosted()) {
            throw new IllegalStateException("Solo se pueden reversar asientos contabilizados");
        }

        JournalEntry reversal = create(reversalDate, description != null ? description :
                "Reversión de asiento #" + original.getEntryNumber(), "REV-" + original.getEntryNumber());

        for (JournalEntryLine line : original.getLines()) {
            addLine(reversal, line.getAccount(), line.getCredit(), line.getDebit(),
                    "Reversión: " + line.getDescription());
        }

        post(reversal.getId());
    }

    public Optional<JournalEntry> findById(Long id) {
        return journalEntryRepository.findById(id);
    }

    public List<JournalEntry> findAll() {
        return journalEntryRepository.findAll();
    }

    public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByDateRange(startDate, endDate);
    }

    public List<JournalEntryLine> getLedger(Account account, LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findLinesByAccount(account, startDate, endDate);
    }

    public void delete(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));

        if (entry.isPosted()) {
            throw new IllegalStateException("No se puede eliminar un asiento contabilizado");
        }

        journalEntryRepository.delete(entry);
    }
}
