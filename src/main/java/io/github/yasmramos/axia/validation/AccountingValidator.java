package io.github.yasmramos.axia.validation;

import io.github.yasmramos.axia.model.FiscalYear;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.service.FiscalYearService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates accounting business rules.
 * Ensures data integrity and compliance with accounting standards.
 *
 * @author Yasmany Ramos Garcia
 */
public class AccountingValidator {

    private static final Logger logger = LoggerFactory.getLogger(AccountingValidator.class);

    private final FiscalYearService fiscalYearService;

    public AccountingValidator(FiscalYearService fiscalYearService) {
        this.fiscalYearService = fiscalYearService;
    }

    /**
     * Validates that a journal entry has balanced debits and credits.
     *
     * @param entry the journal entry to validate
     * @return validation result with any errors
     */
    public ValidationResult validateBalancedEntry(JournalEntry entry) {
        logger.debug("Validating balance for journal entry: {}", entry.getId());
        List<String> errors = new ArrayList<>();

        BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
        BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;

        if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
            errors.add("Entry must have either a debit or credit amount");
        }

        if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
            errors.add("Entry cannot have both debit and credit amounts");
        }

        if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Amounts cannot be negative");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates that a date falls within an open fiscal year.
     *
     * @param date the date to validate
     * @return validation result with any errors
     */
    public ValidationResult validateDateInOpenPeriod(LocalDate date) {
        logger.debug("Validating date in open period: {}", date);
        List<String> errors = new ArrayList<>();

        Optional<FiscalYear> fiscalYear = fiscalYearService.findByDate(date);

        if (fiscalYear.isEmpty()) {
            errors.add("No fiscal year found for date: " + date);
        } else if (fiscalYear.get().isClosed()) {
            errors.add("Fiscal year is closed for date: " + date);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates that an account code follows the standard format.
     *
     * @param code the account code to validate
     * @return validation result with any errors
     */
    public ValidationResult validateAccountCode(String code) {
        logger.debug("Validating account code: {}", code);
        List<String> errors = new ArrayList<>();

        if (code == null || code.isBlank()) {
            errors.add("Account code cannot be empty");
        } else if (!code.matches("^[0-9]{1,10}$")) {
            errors.add("Account code must be numeric (1-10 digits)");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates a tax identification number format.
     *
     * @param taxId the tax ID to validate
     * @return validation result with any errors
     */
    public ValidationResult validateTaxId(String taxId) {
        logger.debug("Validating tax ID: {}", taxId);
        List<String> errors = new ArrayList<>();

        if (taxId != null && !taxId.isBlank()) {
            if (taxId.length() < 5 || taxId.length() > 20) {
                errors.add("Tax ID must be between 5 and 20 characters");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validates invoice data completeness.
     *
     * @param customerId customer ID (can be null for supplier invoices)
     * @param supplierId supplier ID (can be null for customer invoices)
     * @param amount invoice amount
     * @return validation result with any errors
     */
    public ValidationResult validateInvoice(Long customerId, Long supplierId, BigDecimal amount) {
        logger.debug("Validating invoice - Customer: {}, Supplier: {}, Amount: {}", customerId, supplierId, amount);
        List<String> errors = new ArrayList<>();

        if (customerId == null && supplierId == null) {
            errors.add("Invoice must have either a customer or supplier");
        }

        if (customerId != null && supplierId != null) {
            errors.add("Invoice cannot have both customer and supplier");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Invoice amount must be greater than zero");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
