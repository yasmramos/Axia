package io.github.yasmramos.axia.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a journal entry (accounting transaction) in the general journal.
 * 
 * <p>A journal entry consists of multiple lines, each affecting a different account.
 * The fundamental accounting equation requires that total debits equal total credits
 * for each entry (double-entry bookkeeping).
 * 
 * <p>Lifecycle:
 * <ol>
 *   <li>Create entry with header information</li>
 *   <li>Add debit and credit lines</li>
 *   <li>Post to update account balances</li>
 * </ol>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends BaseModel {

    /** Sequential entry number */
    @Column(nullable = false, unique = true)
    private Integer entryNumber;

    /** Transaction date */
    @Column(nullable = false)
    private LocalDate date;

    /** Entry description */
    @Column(length = 500)
    private String description;

    /** External reference (invoice number, check number, etc.) */
    @Column(length = 100)
    private String reference;

    /** Entry lines (debits and credits) */
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    /** Whether the entry has been posted to the ledger */
    @Column(nullable = false)
    private boolean posted = false;

    // ==================== Getters and Setters ====================

    /**
     * Gets the entry number.
     * @return the sequential entry number
     */
    public Integer getEntryNumber() {
        return entryNumber;
    }

    /**
     * Sets the entry number.
     * @param entryNumber the sequential entry number
     */
    public void setEntryNumber(Integer entryNumber) {
        this.entryNumber = entryNumber;
    }

    /**
     * Gets the transaction date.
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Sets the transaction date.
     * @param date the date
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Gets the description.
     * @return the entry description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description the entry description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the external reference.
     * @return the reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * Sets the external reference.
     * @param reference the reference
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Gets the entry lines.
     * @return list of journal entry lines
     */
    public List<JournalEntryLine> getLines() {
        return lines;
    }

    /**
     * Sets the entry lines.
     * @param lines list of journal entry lines
     */
    public void setLines(List<JournalEntryLine> lines) {
        this.lines = lines;
    }

    /**
     * Checks if the entry has been posted.
     * @return true if posted to the ledger
     */
    public boolean isPosted() {
        return posted;
    }

    /**
     * Sets the posted status.
     * @param posted the posted status
     */
    public void setPosted(boolean posted) {
        this.posted = posted;
    }

    // ==================== Business Methods ====================

    /**
     * Adds a line to this journal entry.
     * @param line the line to add
     */
    public void addLine(JournalEntryLine line) {
        line.setJournalEntry(this);
        this.lines.add(line);
    }

    /**
     * Calculates the total debit amount across all lines.
     * @return sum of all debit amounts
     */
    public BigDecimal getTotalDebit() {
        return lines.stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total credit amount across all lines.
     * @return sum of all credit amounts
     */
    public BigDecimal getTotalCredit() {
        return lines.stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks if the entry is balanced (debits equal credits).
     * @return true if the entry is balanced
     */
    public boolean isBalanced() {
        return getTotalDebit().compareTo(getTotalCredit()) == 0;
    }
}
