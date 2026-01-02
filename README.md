# Axia Accounting System

A lightweight, offline-first accounting system built with Java, Ebean ORM, and JavaFX. Designed for small businesses and educational purposes.

[![CI](https://github.com/yasmramos/Axia/actions/workflows/ci.yml/badge.svg)](https://github.com/yasmramos/Axia/actions/workflows/ci.yml)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/yasmramos/Axia/actions)
![Java](https://img.shields.io/badge/Java-17+-orange)
![License](https://img.shields.io/badge/License-MIT-green)

## Features

### Core Accounting
- **Chart of Accounts** - Hierarchical account structure with support for Assets, Liabilities, Equity, Income, and Expenses
- **Journal Entries** - Double-entry bookkeeping with automatic balance validation
- **General Ledger** - Complete transaction history per account
- **Fiscal Year Management** - Multi-year support with period closing

### Business Operations
- **Invoicing** - Sales and purchase invoice management with automatic journal entry generation
- **Customer & Supplier Management** - Full CRUD operations with search capabilities
- **Multi-Currency Support** - Exchange rates and currency conversion

### Reports
- Balance Sheet
- Income Statement
- Trial Balance
- General Ledger
- Journal Book

### Additional Services
- **Dashboard** - Real-time KPIs and financial metrics
- **Audit Trail** - Complete logging of all entity changes
- **Data Export** - CSV export for accounts, customers, suppliers, invoices
- **Backup & Restore** - Full database backup to ZIP archives
- **Validation** - Business rule validation for accounting integrity

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| UI Framework | JavaFX 21 |
| ORM | Ebean 15.x |
| Database | PostgreSQL (Embedded for tests) |
| DI Framework | Veld |
| Build Tool | Maven |
| Testing | JUnit 5, Embedded PostgreSQL |

## Project Structure

```
src/main/java/io/github/yasmramos/axia/
├── config/          # Database configuration
├── model/           # Entity classes (Account, Invoice, JournalEntry, etc.)
├── repository/      # Data access layer
├── service/         # Business logic layer
├── validation/      # Accounting validators
├── audit/           # Audit trail service
├── backup/          # Backup & restore service
├── dashboard/       # Dashboard KPIs service
├── export/          # Data export service
├── importdata/      # Data import service
└── ui/controller/   # JavaFX controllers
```

## Getting Started

### Prerequisites

- Java 17 or higher
- PostgreSQL 12+
- Maven 3.8+

### Database Setup

```sql
CREATE DATABASE axia;
```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
datasource.db.username=your_username
datasource.db.password=your_password
datasource.db.url=jdbc:postgresql://localhost:5432/axia
```

### Build & Run

```bash
# Compile and enhance entities
mvn clean compile

# Run tests
mvn test

# Run the application
mvn exec:java -Dexec.mainClass="io.github.yasmramos.axia.AxiaApplication"
```

## Usage Examples

### Working with Accounts

```java
AccountService accountService = new AccountService(new AccountRepository());

// Initialize default chart of accounts
accountService.initializeDefaultAccounts();

// Create a new account
Account cash = accountService.create("1.1.01.01", "Petty Cash", AccountType.ASSET, parentAccount);

// Get chart of accounts
List<Account> accounts = accountService.getChartOfAccounts();
```

### Creating Journal Entries

```java
JournalEntryService journalService = new JournalEntryService(
    new JournalEntryRepository(), new AccountRepository());

// Create entry
JournalEntry entry = journalService.create(LocalDate.now(), "Office supplies purchase", "INV-001");

// Add lines (must balance)
journalService.addLine(entry, expenseAccount, new BigDecimal("100.00"), null, "Supplies");
journalService.addLine(entry, cashAccount, null, new BigDecimal("100.00"), "Cash payment");

// Post to ledger
journalService.post(entry.getId());
```

### Invoice Management

```java
InvoiceService invoiceService = new InvoiceService(
    new InvoiceRepository(), accountRepository, journalEntryService);

// Create sales invoice
Invoice invoice = invoiceService.createSaleInvoice(customer, LocalDate.now(), dueDate);

// Add lines
invoiceService.addLine(invoice, "Consulting services", 
    new BigDecimal("10"), new BigDecimal("150.00"), new BigDecimal("16"), revenueAccount);

// Post invoice (creates journal entry automatically)
invoiceService.post(invoice.getId());

// Mark as paid
invoiceService.markAsPaid(invoice.getId());
```

### Dashboard & Reports

```java
DashboardService dashboardService = new DashboardService();

// Get summary statistics
Map<String, Object> stats = dashboardService.getSummaryStats();

// Get financial KPIs
Map<String, BigDecimal> kpis = dashboardService.getFinancialKPIs(startDate, endDate);

// Reports
ReportService reportService = new ReportService(accountRepo, journalEntryRepo, invoiceRepo);
Map<String, Object> balanceSheet = reportService.getBalanceSheet(LocalDate.now());
Map<String, Object> incomeStatement = reportService.getIncomeStatement(startDate, endDate);
```

### Data Export

```java
ExportService exportService = new ExportService();

// Export to CSV
exportService.exportAccountsToCsv(accounts, "accounts.csv");
exportService.exportCustomersToCsv(customers, "customers.csv");
exportService.exportInvoicesToCsv(invoices, "invoices.csv");
```

### Backup & Restore

```java
BackupService backupService = new BackupService();

// Create backup
String backupPath = backupService.createBackup("/path/to/backups");

// List available backups
List<String> backups = backupService.listBackups("/path/to/backups");

// Restore from backup
backupService.restoreBackup(backupPath);
```

## Default Chart of Accounts

The system initializes with a standard chart of accounts:

| Code | Account | Type |
|------|---------|------|
| 1 | Assets | Asset |
| 1.1 | Current Assets | Asset |
| 1.1.01 | Cash | Asset |
| 1.1.02 | Banks | Asset |
| 1.1.03 | Accounts Receivable | Asset |
| 2 | Liabilities | Liability |
| 2.1.01 | Accounts Payable | Liability |
| 2.1.02 | Taxes Payable | Liability |
| 3 | Equity | Equity |
| 4 | Income | Income |
| 4.1.01 | Sales | Income |
| 5 | Expenses | Expense |

## Testing

The project includes comprehensive unit and integration tests using JUnit 5 and Embedded PostgreSQL:

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
```

**Current test coverage: 106 tests across 14 test classes**

| Test Class | Tests |
|------------|-------|
| AccountServiceTest | 9 |
| CustomerServiceTest | 8 |
| SupplierServiceTest | 7 |
| InvoiceServiceTest | 10 |
| JournalEntryServiceTest | 10 |
| FiscalYearServiceTest | 9 |
| ReportServiceTest | 5 |
| DashboardServiceTest | 6 |
| AuditServiceTest | 6 |
| CurrencyServiceTest | 9 |
| AccountingValidatorTest | 15 |
| ExportServiceTest | 6 |
| BackupServiceTest | 6 |

## Integration with JavaFX

This library includes a full JavaFX desktop application:

```java
public class MainApp extends Application {
    
    @Override
    public void init() {
        AxiaApplication.initialize();
    }
    
    @Override
    public void stop() {
        AxiaApplication.shutdown();
    }
}
```

## License

MIT License

## Author

Yasmany Ramos Garcia ([@yasmramos](https://github.com/yasmramos))
