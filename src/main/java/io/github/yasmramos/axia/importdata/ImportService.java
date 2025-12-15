package io.github.yasmramos.axia.importdata;

import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for importing accounting data from CSV files.
 *
 * @author Yasmany Ramos Garcia
 */
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccountService accountService;
    private final CustomerService customerService;
    private final SupplierService supplierService;

    public ImportService(AccountService accountService, CustomerService customerService, SupplierService supplierService) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.supplierService = supplierService;
    }

    /**
     * Imports accounts from a CSV file.
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importAccountsFromCsv(String filePath) throws IOException {
        logger.info("Importing accounts from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 3) {
                        Account account = new Account();
                        account.setCode(parts[0].trim());
                        account.setName(parts[1].trim());
                        account.setType(Account.AccountType.valueOf(parts[2].trim().toUpperCase()));
                        account.setActive(parts.length < 5 || Boolean.parseBoolean(parts[4].trim()));

                        accountService.save(account);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing account at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Accounts import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    /**
     * Imports customers from a CSV file.
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importCustomersFromCsv(String filePath) throws IOException {
        logger.info("Importing customers from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 1) {
                        Customer customer = new Customer();
                        customer.setName(parts[0].trim());
                        if (parts.length > 1) customer.setTaxId(parts[1].trim());
                        if (parts.length > 2) customer.setEmail(parts[2].trim());
                        if (parts.length > 3) customer.setPhone(parts[3].trim());
                        if (parts.length > 4) customer.setAddress(parts[4].trim());
                        customer.setActive(parts.length < 6 || Boolean.parseBoolean(parts[5].trim()));

                        customerService.save(customer);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing customer at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Customers import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    /**
     * Imports suppliers from a CSV file.
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importSuppliersFromCsv(String filePath) throws IOException {
        logger.info("Importing suppliers from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 1) {
                        Supplier supplier = new Supplier();
                        supplier.setName(parts[0].trim());
                        if (parts.length > 1) supplier.setTaxId(parts[1].trim());
                        if (parts.length > 2) supplier.setEmail(parts[2].trim());
                        if (parts.length > 3) supplier.setPhone(parts[3].trim());
                        if (parts.length > 4) supplier.setAddress(parts[4].trim());
                        supplier.setActive(parts.length < 6 || Boolean.parseBoolean(parts[5].trim()));

                        supplierService.save(supplier);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing supplier at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Suppliers import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }
}
