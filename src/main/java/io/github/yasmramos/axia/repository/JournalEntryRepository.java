package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryLine;
import io.ebean.Database;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class JournalEntryRepository {

    private final Database db;

    @Inject
    public JournalEntryRepository() {
        this.db = DatabaseManager.getDatabase();
    }

    public void save(JournalEntry entry) {
        db.save(entry);
    }

    public void update(JournalEntry entry) {
        db.update(entry);
    }

    public void delete(JournalEntry entry) {
        db.delete(entry);
    }

    public Optional<JournalEntry> findById(Long id) {
        return Optional.ofNullable(
                db.find(JournalEntry.class)
                        .fetch("lines")
                        .fetch("lines.account")
                        .where()
                        .idEq(id)
                        .findOne()
        );
    }

    public Optional<JournalEntry> findByEntryNumber(Integer entryNumber) {
        return db.find(JournalEntry.class)
                .fetch("lines")
                .fetch("lines.account")
                .where()
                .eq("entryNumber", entryNumber)
                .findOneOrEmpty();
    }

    public List<JournalEntry> findAll() {
        return db.find(JournalEntry.class)
                .fetch("lines")
                .orderBy("date desc, entryNumber desc")
                .findList();
    }

    public List<JournalEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return db.find(JournalEntry.class)
                .fetch("lines")
                .where()
                .ge("date", startDate)
                .le("date", endDate)
                .orderBy("date, entryNumber")
                .findList();
    }

    public List<JournalEntry> findPosted() {
        return db.find(JournalEntry.class)
                .where()
                .eq("posted", true)
                .orderBy("date, entryNumber")
                .findList();
    }

    public Integer getNextEntryNumber() {
        Integer max = db.find(JournalEntry.class)
                .select("max(entryNumber)")
                .findSingleAttribute();
        return (max != null ? max : 0) + 1;
    }

    public List<JournalEntryLine> findLinesByAccount(Account account, LocalDate startDate, LocalDate endDate) {
        return db.find(JournalEntryLine.class)
                .fetch("journalEntry")
                .where()
                .eq("account", account)
                .ge("journalEntry.date", startDate)
                .le("journalEntry.date", endDate)
                .eq("journalEntry.posted", true)
                .orderBy("journalEntry.date, journalEntry.entryNumber")
                .findList();
    }
}
