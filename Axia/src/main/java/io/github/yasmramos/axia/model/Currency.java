package io.github.yasmramos.axia.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a currency with exchange rate support.
 *
 * @author Yasmany Ramos Garcia
 */
@Entity
@Table(name = "currencies")
public class Currency extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 5)
    private String symbol;

    @Column(precision = 18, scale = 8)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private boolean baseCurrency = false;

    @Column(nullable = false)
    private boolean active = true;

    @WhenCreated
    private Instant createdAt;

    @WhenModified
    private Instant updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public boolean isBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(boolean baseCurrency) { this.baseCurrency = baseCurrency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Converts an amount from this currency to the base currency.
     *
     * @param amount the amount to convert
     * @return converted amount in base currency
     */
    public BigDecimal toBaseCurrency(BigDecimal amount) {
        if (baseCurrency || exchangeRate == null) {
            return amount;
        }
        return amount.multiply(exchangeRate);
    }

    /**
     * Converts an amount from base currency to this currency.
     *
     * @param amount the amount in base currency
     * @return converted amount in this currency
     */
    public BigDecimal fromBaseCurrency(BigDecimal amount) {
        if (baseCurrency || exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP);
    }
}
