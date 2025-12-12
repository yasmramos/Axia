# Axia Accounting System

A lightweight, offline-first accounting system built with Java and Ebean ORM. Designed for seamless integration with JavaFX desktop applications.

## Features

- **Chart of Accounts** - Hierarchical account structure with support for Assets, Liabilities, Equity, Income, and Expenses
- **Journal Entries** - Double-entry bookkeeping with automatic balance validation
- **General Ledger** - Complete transaction history per account
- **Invoicing** - Sales and purchase invoice management with automatic journal entry generation
- **Customer & Supplier Management** - Full CRUD operations with search capabilities
- **Financial Reports**
  - Balance Sheet
  - Income Statement
  - Trial Balance
  - General Ledger
  - Journal Book
- **Fiscal Year Management** - Multi-year support with period closing

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ |
| ORM | Ebean 15.x |
| Database | PostgreSQL |
| Build Tool | Maven |

## Project Structure

```
src/main/java/io/github/yasmramos/axia/
├── config/          # Database configuration
├── model/           # Entity classes
├── repository/      # Data access layer
└── service/         # Business logic layer
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

# Run initialization (creates default chart of accounts)
mvn exec:java -Dexec.mainClass="io.github.yasmramos.axia.AxiaApplication"
```

## Usage

### Initialize the System

```java
import io.github.yasmramos.axia.AxiaApplication;

// Initialize database and default data
AxiaApplication.initialize();
```

### Working with Accounts

```java
AccountService accountService = new AccountService();

// Create a new account
Account cash = accountService.create("1.1.01.01", "Petty Cash", AccountType.ASSET, parentAccount);

// Get chart of accounts
List<Account> accounts = accountService.getChartOfAccounts();
```

### Creating Journal Entries

```java
JournalEntryService journalService = new JournalEntryService();

// Create entry
JournalEntry entry = journalService.create(LocalDate.now(), "Office supplies purchase", "INV-001");

// Add lines (must balance)
journalService.addLine(entry, expenseAccount, new BigDecimal("100.00"), null, "Supplies");
journalService.addLine(entry, cashAccount, null, new BigDecimal("100.00"), "Cash payment");

// Post to ledger
journalService.post(entry.getId());
```

### Generating Reports

```java
ReportService reportService = new ReportService();

// Balance Sheet
Map<String, Object> balanceSheet = reportService.getBalanceSheet(LocalDate.now());

// Income Statement
Map<String, Object> incomeStatement = reportService.getIncomeStatement(startDate, endDate);

// Trial Balance
Map<String, Object> trialBalance = reportService.getTrialBalance(LocalDate.now());
```

### Invoice Management

```java
InvoiceService invoiceService = new InvoiceService();

// Create sales invoice
Invoice invoice = invoiceService.createSaleInvoice(customer, LocalDate.now(), dueDate);

// Add lines
invoiceService.addLine(invoice, "Consulting services", 
    new BigDecimal("10"), new BigDecimal("150.00"), new BigDecimal("16"), revenueAccount);

// Post invoice (creates journal entry automatically)
invoiceService.post(invoice.getId());
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

## Integration with JavaFX

This library is designed as a backend service layer. To integrate with JavaFX:

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

Yasmany Ramos García ([@yasmramos](https://github.com/yasmramos))
