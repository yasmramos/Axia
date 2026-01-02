package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.ebean.DB;
import io.github.yasmramos.axia.model.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing currencies and exchange rates.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    /**
     * Saves or updates a currency.
     *
     * @param currency the currency to save
     */
    public void save(Currency currency) {
        logger.info("Saving currency: {}", currency.getCode());
        DB.save(currency);
    }

    /**
     * Finds a currency by its code.
     *
     * @param code the currency code (e.g., USD, EUR)
     * @return optional containing the currency if found
     */
    public Optional<Currency> findByCode(String code) {
        logger.debug("Finding currency by code: {}", code);
        return Optional.ofNullable(
                DB.find(Currency.class)
                        .where()
                        .eq("code", code)
                        .findOne()
        );
    }

    /**
     * Gets the base currency.
     *
     * @return optional containing the base currency
     */
    public Optional<Currency> getBaseCurrency() {
        logger.debug("Getting base currency");
        return Optional.ofNullable(
                DB.find(Currency.class)
                        .where()
                        .eq("baseCurrency", true)
                        .findOne()
        );
    }

    /**
     * Gets all active currencies.
     *
     * @return list of active currencies
     */
    public List<Currency> findAllActive() {
        logger.debug("Finding all active currencies");
        return DB.find(Currency.class)
                .where()
                .eq("active", true)
                .orderBy().asc("code")
                .findList();
    }

    /**
     * Updates the exchange rate for a currency.
     *
     * @param code the currency code
     * @param newRate the new exchange rate
     * @return true if updated successfully
     */
    public boolean updateExchangeRate(String code, BigDecimal newRate) {
        logger.info("Updating exchange rate for {}: {}", code, newRate);
        Optional<Currency> currency = findByCode(code);
        if (currency.isPresent()) {
            currency.get().setExchangeRate(newRate);
            DB.save(currency.get());
            return true;
        }
        return false;
    }

    /**
     * Converts an amount between currencies.
     *
     * @param amount the amount to convert
     * @param fromCode source currency code
     * @param toCode target currency code
     * @return converted amount
     */
    public BigDecimal convert(BigDecimal amount, String fromCode, String toCode) {
        logger.debug("Converting {} from {} to {}", amount, fromCode, toCode);

        if (fromCode.equals(toCode)) {
            return amount;
        }

        Optional<Currency> fromCurrency = findByCode(fromCode);
        Optional<Currency> toCurrency = findByCode(toCode);

        if (fromCurrency.isEmpty() || toCurrency.isEmpty()) {
            throw new IllegalArgumentException("Currency not found");
        }

        BigDecimal inBase = fromCurrency.get().toBaseCurrency(amount);
        return toCurrency.get().fromBaseCurrency(inBase);
    }

    /**
     * Sets a currency as the base currency.
     *
     * @param code the currency code to set as base
     */
    public void setAsBaseCurrency(String code) {
        logger.info("Setting {} as base currency", code);

        // Remove base flag from current base
        getBaseCurrency().ifPresent(c -> {
            c.setBaseCurrency(false);
            c.setExchangeRate(BigDecimal.ONE);
            DB.save(c);
        });

        // Set new base
        findByCode(code).ifPresent(c -> {
            c.setBaseCurrency(true);
            c.setExchangeRate(BigDecimal.ONE);
            DB.save(c);
        });
    }
}
