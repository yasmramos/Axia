package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryLine;
import io.github.yasmramos.axia.repository.AccountRepository;
import io.github.yasmramos.axia.repository.JournalEntryRepository;
import io.ebean.Database;
import io.ebean.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Component
public class JournalEntryService {

    private static final Logger log = LoggerFactory.getLogger(JournalEntryService.class);

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final Database db;

    @Inject
    public JournalEntryService(JournalEntryRepository journalEntryRepository, 
                               AccountRepository accountRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
        this.db = DatabaseManager.getDatabase();
    }

    public JournalEntry create(LocalDate date, String description, String reference) {
        log.info("Creating journal entry: {} - {}", date, description);
        
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(journalEntryRepository.getNextEntryNumber());
        entry.setDate(date);
        entry.setDescription(description);
        entry.setReference(reference);
        entry.setPosted(false);

        journalEntryRepository.save(entry);
        log.info("Journal entry created: #{}", entry.getEntryNumber());
        return entry;
    }

    public JournalEntry addLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        log.debug("Adding line to entry #{}: account={}, debit={}, credit={}", 
                entry.getEntryNumber(), account.getCode(), debit, credit);
        
        if (entry.isPosted()) {
            log.error("Cannot add lines to posted entry #{}", entry.getEntryNumber());
            throw new IllegalStateException("Cannot add lines to a posted journal entry");
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
        log.info("Posting journal entry ID: {}", entryId);
        
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> {
                    log.error("Journal entry not found: {}", entryId);
                    return new IllegalArgumentException("Journal entry not found");
                });

        if (entry.isPosted()) {
            log.error("Entry #{} is already posted", entry.getEntryNumber());
            throw new IllegalStateException("Journal entry is already posted");
        }

        if (!entry.isBalanced()) {
            log.error("Entry #{} is not balanced. Debit: {}, Credit: {}", 
                    entry.getEntryNumber(), entry.getTotalDebit(), entry.getTotalCredit());
            throw new IllegalStateException("Journal entry is not balanced. Debit: " +
                    entry.getTotalDebit() + ", Credit: " + entry.getTotalCredit());
        }

        if (entry.getLines().isEmpty()) {
            log.error("Entry #{} has no lines", entry.getEntryNumber());
            throw new IllegalStateException("Journal entry has no lines");
        }

        try (Transaction txn = db.beginTransaction()) {
            log.debug("Updating account balances for entry #{}", entry.getEntryNumber());
            
            for (JournalEntryLine line : entry.getLines()) {
                Account account = line.getAccount();
                account.debit(line.getDebit());
                account.credit(line.getCredit());
                accountRepository.update(account);
                log.trace("Updated account {}: debit={}, credit={}", 
                        account.getCode(), line.getDebit(), line.getCredit());
            }

            entry.setPosted(true);
            journalEntryRepository.update(entry);

            txn.commit();
            log.info("Journal entry #{} posted successfully", entry.getEntryNumber());
        }

        return entry;
    }

    public void reverse(Long entryId, LocalDate reversalDate, String description) {
        log.info("Reversing journal entry ID: {}", entryId);
        
        JournalEntry original = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> {
                    log.error("Journal entry not found: {}", entryId);
                    return new IllegalArgumentException("Journal entry not found");
                });

        if (!original.isPosted()) {
            log.error("Cannot reverse unposted entry #{}", original.getEntryNumber());
            throw new IllegalStateException("Only posted entries can be reversed");
        }

        JournalEntry reversal = create(reversalDate, description != null ? description :
                "Reversal of entry #" + original.getEntryNumber(), "REV-" + original.getEntryNumber());

        for (JournalEntryLine line : original.getLines()) {
            addLine(reversal, line.getAccount(), line.getCredit(), line.getDebit(),
                    "Reversal: " + line.getDescription());
        }

        post(reversal.getId());
        log.info("Entry #{} reversed with new entry #{}", original.getEntryNumber(), reversal.getEntryNumber());
    }

    public Optional<JournalEntry> findById(Long id) {
        log.debug("Finding journal entry by ID: {}", id);
        return journalEntryRepository.findById(id);
    }

    public List<JournalEntry> findAll() {
        log.debug("Retrieving all journal entries");
        return journalEntryRepository.findAll();
    }

    public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Finding entries by date range: {} to {}", startDate, endDate);
        return journalEntryRepository.findByDateRange(startDate, endDate);
    }

    public List<JournalEntryLine> getLedger(Account account, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting ledger for account {} from {} to {}", account.getCode(), startDate, endDate);
        return journalEntryRepository.findLinesByAccount(account, startDate, endDate);
    }

    public void delete(Long id) {
        log.info("Deleting journal entry ID: {}", id);
        
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Journal entry not found: {}", id);
                    return new IllegalArgumentException("Journal entry not found");
                });

        if (entry.isPosted()) {
            log.error("Cannot delete posted entry #{}", entry.getEntryNumber());
            throw new IllegalStateException("Cannot delete a posted journal entry");
        }

        journalEntryRepository.delete(entry);
        log.info("Journal entry #{} deleted", entry.getEntryNumber());
    }
}
