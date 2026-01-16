package io.github.yasmramos.axia.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Template for recurring journal entries.
 * Allows quick creation of common accounting entries.
 *
 * @author Yasmany Ramos Garcia
 */
@Entity
@Table(name = "journal_entry_templates")
public class JournalEntryTemplate extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "debit_account_id")
    private Account debitAccount;

    @ManyToOne
    @JoinColumn(name = "credit_account_id")
    private Account creditAccount;

    @Column(precision = 15, scale = 2)
    private BigDecimal defaultAmount;

    @Column(nullable = false)
    private boolean active = true;

    @WhenCreated
    private Instant createdAt;

    @WhenModified
    private Instant updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Account getDebitAccount() { return debitAccount; }
    public void setDebitAccount(Account debitAccount) { this.debitAccount = debitAccount; }

    public Account getCreditAccount() { return creditAccount; }
    public void setCreditAccount(Account creditAccount) { this.creditAccount = creditAccount; }

    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
