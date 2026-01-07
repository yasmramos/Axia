package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.EmbeddedPostgresExtension;
import io.github.yasmramos.axia.model.Currency;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CurrencyService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CurrencyServiceTest {

    private static CurrencyService currencyService;

    @BeforeAll
    static void setUp() {
        currencyService = new CurrencyService();
        
        // Create test currencies
        Currency usd = new Currency();
        usd.setCode("USD");
        usd.setName("US Dollar");
        usd.setSymbol("$");
        usd.setExchangeRate(BigDecimal.ONE);
        usd.setBaseCurrency(true);
        usd.setActive(true);
        DB.save(usd);
        
        Currency eur = new Currency();
        eur.setCode("EUR");
        eur.setName("Euro");
        eur.setSymbol("€");
        eur.setExchangeRate(new BigDecimal("0.85"));
        eur.setBaseCurrency(false);
        eur.setActive(true);
        DB.save(eur);
        
        Currency gbp = new Currency();
        gbp.setCode("GBP");
        gbp.setName("British Pound");
        gbp.setSymbol("£");
        gbp.setExchangeRate(new BigDecimal("0.73"));
        gbp.setBaseCurrency(false);
        gbp.setActive(false);
        DB.save(gbp);
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(Currency.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should find currency by code")
    void testFindByCode() {
        Optional<Currency> usd = currencyService.findByCode("USD");
        
        assertTrue(usd.isPresent());
        assertEquals("US Dollar", usd.get().getName());
        assertEquals("$", usd.get().getSymbol());
    }

    @Test
    @Order(2)
    @DisplayName("Should get base currency")
    void testGetBaseCurrency() {
        Optional<Currency> base = currencyService.getBaseCurrency();
        
        assertTrue(base.isPresent());
        assertEquals("USD", base.get().getCode());
        assertTrue(base.get().isBaseCurrency());
    }

    @Test
    @Order(3)
    @DisplayName("Should find all active currencies")
    void testFindAllActive() {
        List<Currency> active = currencyService.findAllActive();
        
        assertFalse(active.isEmpty());
        assertTrue(active.stream().allMatch(Currency::isActive));
        assertTrue(active.stream().noneMatch(c -> "GBP".equals(c.getCode())));
    }

    @Test
    @Order(4)
    @DisplayName("Should update exchange rate")
    void testUpdateExchangeRate() {
        boolean updated = currencyService.updateExchangeRate("EUR", new BigDecimal("0.90"));
        
        assertTrue(updated);
        
        Optional<Currency> eur = currencyService.findByCode("EUR");
        assertTrue(eur.isPresent());
        assertEquals(0, new BigDecimal("0.90").compareTo(eur.get().getExchangeRate()));
    }

    @Test
    @Order(5)
    @DisplayName("Should return false for non-existent currency update")
    void testUpdateExchangeRateNotFound() {
        boolean updated = currencyService.updateExchangeRate("XYZ", new BigDecimal("1.5"));
        
        assertFalse(updated);
    }

    @Test
    @Order(6)
    @DisplayName("Should convert same currency")
    void testConvertSameCurrency() {
        BigDecimal amount = new BigDecimal("100");
        BigDecimal result = currencyService.convert(amount, "USD", "USD");
        
        assertEquals(amount, result);
    }

    @Test
    @Order(7)
    @DisplayName("Should throw exception for unknown currency")
    void testConvertUnknownCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
            currencyService.convert(new BigDecimal("100"), "USD", "XYZ")
        );
    }

    @Test
    @Order(8)
    @DisplayName("Should set new base currency")
    void testSetAsBaseCurrency() {
        currencyService.setAsBaseCurrency("EUR");
        
        Optional<Currency> eur = currencyService.findByCode("EUR");
        Optional<Currency> usd = currencyService.findByCode("USD");
        
        assertTrue(eur.isPresent());
        assertTrue(eur.get().isBaseCurrency());
        
        assertTrue(usd.isPresent());
        assertFalse(usd.get().isBaseCurrency());
    }

    @Test
    @Order(9)
    @DisplayName("Should save new currency")
    void testSave() {
        Currency jpy = new Currency();
        jpy.setCode("JPY");
        jpy.setName("Japanese Yen");
        jpy.setSymbol("¥");
        jpy.setExchangeRate(new BigDecimal("110"));
        jpy.setBaseCurrency(false);
        jpy.setActive(true);
        
        currencyService.save(jpy);
        
        Optional<Currency> saved = currencyService.findByCode("JPY");
        assertTrue(saved.isPresent());
        assertEquals("Japanese Yen", saved.get().getName());
    }

    @Test
    @Order(10)
    @DisplayName("Should find all currencies")
    void testFindAll() {
        List<Currency> all = currencyService.findAll();
        
        assertFalse(all.isEmpty());
        assertTrue(all.size() >= 3);
    }

    @Test
    @Order(11)
    @DisplayName("Should return empty for non-existent code")
    void testFindByCodeNotFound() {
        Optional<Currency> result = currencyService.findByCode("XXX");
        
        assertFalse(result.isPresent());
    }

    @Test
    @Order(12)
    @DisplayName("Should convert between different currencies")
    void testConvertDifferentCurrencies() {
        BigDecimal amount = new BigDecimal("100");
        // USD to EUR: 100 * (1/0.85) = 117.64 approx
        BigDecimal result = currencyService.convert(amount, "USD", "EUR");
        
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, new BigDecimal("117.65").compareTo(result.setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    @Order(13)
    @DisplayName("Should handle inactive currency conversion")
    void testConvertInactiveCurrency() {
        // GBP is inactive, should still work for conversion
        BigDecimal amount = new BigDecimal("50");
        BigDecimal result = currencyService.convert(amount, "USD", "GBP");
        
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @Order(14)
    @DisplayName("Should deactivate currency")
    void testDeactivate() {
        Currency jpy = currencyService.findByCode("JPY").orElseThrow();
        assertTrue(jpy.isActive());
        
        currencyService.deactivate("JPY");
        
        Optional<Currency> deactivated = currencyService.findByCode("JPY");
        assertTrue(deactivated.isPresent());
        assertFalse(deactivated.get().isActive());
    }

    @Test
    @Order(15)
    @DisplayName("Should not find inactive currencies in findAllActive")
    void testInactiveNotInActiveList() {
        List<Currency> active = currencyService.findAllActive();
        
        assertFalse(active.stream().anyMatch(c -> "GBP".equals(c.getCode())));
    }

    @Test
    @Order(16)
    @DisplayName("Should return currencies by symbol")
    void testFindBySymbol() {
        Optional<Currency> usd = currencyService.findBySymbol("$");
        
        assertTrue(usd.isPresent());
        assertEquals("USD", usd.get().getCode());
    }

    @Test
    @Order(17)
    @DisplayName("Should handle null exchange rate gracefully")
    void testNullExchangeRate() {
        Currency mxn = new Currency();
        mxn.setCode("MXN");
        mxn.setName("Mexican Peso");
        mxn.setSymbol("$");
        mxn.setExchangeRate(null);
        mxn.setBaseCurrency(false);
        mxn.setActive(true);
        
        currencyService.save(mxn);
        
        Optional<Currency> saved = currencyService.findByCode("MXN");
        assertTrue(saved.isPresent());
        assertNull(saved.get().getExchangeRate());
    }

    @Test
    @Order(18)
    @DisplayName("Should calculate exchange rate between two non-base currencies")
    void testExchangeRateBetweenNonBase() {
        BigDecimal rate = currencyService.getExchangeRate("EUR", "GBP");
        
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0);
        // EUR to GBP: 0.85 / 0.73 ≈ 1.16
        assertEquals(0, new BigDecimal("1.16").compareTo(rate.setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    @Order(19)
    @DisplayName("Should get all currencies including inactive")
    void testFindAllIncludesInactive() {
        List<Currency> all = currencyService.findAll();
        
        assertTrue(all.stream().anyMatch(c -> "GBP".equals(c.getCode())));
    }

    @Test
    @Order(20)
    @DisplayName("Should throw exception when setting non-existent as base")
    void testSetNonExistentAsBase() {
        assertThrows(IllegalArgumentException.class, () ->
            currencyService.setAsBaseCurrency("XYZ")
        );
    }
}
