package io.github.yasmramos.axia.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Represents a single line item within a journal entry.
 * 
 * <p>Each line affects one account with either a debit or credit amount
 * (typically one is zero). Multiple lines together form a complete
 * journal entry that must balance.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine extends BaseModel {

    /** Parent journal entry */
    @ManyToOne(optional = false)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    /** Account affected by this line */
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    /** Debit amount (zero if credit) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal debit = BigDecimal.ZERO;

    /** Credit amount (zero if debit) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal credit = BigDecimal.ZERO;

    /** Line description */
    @Column(length = 300)
    private String description;

    // ==================== Getters and Setters ====================

    /**
     * Gets the parent journal entry.
     * @return the journal entry
     */
    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    /**
     * Sets the parent journal entry.
     * @param journalEntry the journal entry
     */
    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    /**
     * Gets the account.
     * @return the account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Sets the account.
     * @param account the account
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Gets the debit amount.
     * @return the debit amount
     */
    public BigDecimal getDebit() {
        return debit;
    }

    /**
     * Sets the debit amount.
     * @param debit the debit amount
     */
    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    /**
     * Gets the credit amount.
     * @return the credit amount
     */
    public BigDecimal getCredit() {
        return credit;
    }

    /**
     * Sets the credit amount.
     * @param credit the credit amount
     */
    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    /**
     * Gets the line description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the line description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
