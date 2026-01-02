package io.github.yasmramos.axia.model;

import io.ebean.annotation.Index;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an account in the chart of accounts.
 * 
 * <p>Accounts are organized in a hierarchical structure where each account
 * can have a parent and multiple children. The account type determines
 * how debits and credits affect the balance.
 * 
 * <p>Balance behavior by account type:
 * <ul>
 *   <li>ASSET/EXPENSE: Debits increase, Credits decrease</li>
 *   <li>LIABILITY/EQUITY/INCOME: Credits increase, Debits decrease</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "accounts")
public class Account extends BaseModel {

    /** Unique account code (e.g., "1.1.01") */
    @Index(unique = true)
    @Column(nullable = false, length = 20)
    private String code;

    /** Account name */
    @Column(nullable = false, length = 200)
    private String name;

    /** Optional description */
    @Column(length = 500)
    private String description;

    /** Account classification type */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    /** Parent account for hierarchical structure */
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Account parent;

    /** Child accounts */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Account> children = new ArrayList<>();

    /** Current account balance */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Whether the account is active */
    @Column(nullable = false)
    private boolean active = true;

    /** Hierarchy level (1 = root) */
    @Column(nullable = false)
    private int level = 1;

    // ==================== Getters and Setters ====================

    /**
     * Gets the account code.
     * @return the unique account code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the account code.
     * @param code the unique account code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the account name.
     * @return the account name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the account name.
     * @param name the account name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the account description.
     * @return the description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the account description.
     * @param description the account description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the account type.
     * @return the account type
     */
    public AccountType getType() {
        return type;
    }

    /**
     * Sets the account type.
     * @param type the account type
     */
    public void setType(AccountType type) {
        this.type = type;
    }

    /**
     * Gets the parent account.
     * @return the parent account or null if root
     */
    public Account getParent() {
        return parent;
    }

    /**
     * Sets the parent account and updates the hierarchy level.
     * @param parent the parent account
     */
    public void setParent(Account parent) {
        this.parent = parent;
        this.level = parent != null ? parent.getLevel() + 1 : 1;
    }

    /**
     * Gets the child accounts.
     * @return list of child accounts
     */
    public List<Account> getChildren() {
        return children;
    }

    /**
     * Sets the child accounts.
     * @param children list of child accounts
     */
    public void setChildren(List<Account> children) {
        this.children = children;
    }

    /**
     * Gets the current balance.
     * @return the account balance
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Sets the account balance.
     * @param balance the new balance
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Checks if the account is active.
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active status.
     * @param active the active status
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets the hierarchy level.
     * @return the level (1 = root)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the hierarchy level.
     * @param level the hierarchy level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    // ==================== Business Methods ====================

    /**
     * Applies a debit to the account.
     * 
     * <p>For ASSET and EXPENSE accounts, debits increase the balance.
     * For LIABILITY, EQUITY, and INCOME accounts, debits decrease the balance.
     * 
     * @param amount the amount to debit
     */
    public void debit(BigDecimal amount) {
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            this.balance = this.balance.add(amount);
        } else {
            this.balance = this.balance.subtract(amount);
        }
    }

    /**
     * Applies a credit to the account.
     * 
     * <p>For ASSET and EXPENSE accounts, credits decrease the balance.
     * For LIABILITY, EQUITY, and INCOME accounts, credits increase the balance.
     * 
     * @param amount the amount to credit
     */
    public void credit(BigDecimal amount) {
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            this.balance = this.balance.subtract(amount);
        } else {
            this.balance = this.balance.add(amount);
        }
    }
}
