package io.github.yasmramos.axia.model;

import io.ebean.annotation.Index;

/**
 * Database index configuration for the Axia Accounting System.
 * 
 * <p>This file documents all database indices that should be created
 * for optimal query performance. Some indices are defined via annotations
 * on entity classes, while others may require DDL execution.
 * 
 * <p>Indices are organized by entity type and include:
 * <ul>
 *   <li>Primary key indices (automatic)</li>
 *   <li>Unique constraints</li>
 *   <li>Foreign key indices</li>
 *   <li>Query optimization indices</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public final class DatabaseIndices {

    private DatabaseIndices() {
        // Prevent instantiation
    }

    // ==================== Account Indices ====================

    /**
     * Account table indices:
     * - code: Unique index for account code lookups
     * - type: Index for filtering by account type
     * - parent_id: Foreign key index for hierarchical queries
     * - active: Index for filtering active accounts
     */
    public static final String ACCOUNT_CODE_INDEX = "idx_account_code";
    public static final String ACCOUNT_TYPE_INDEX = "idx_account_type";
    public static final String ACCOUNT_PARENT_INDEX = "idx_account_parent";
    public static final String ACCOUNT_ACTIVE_INDEX = "idx_account_active";
    public static final String ACCOUNT_CODE_TYPE_INDEX = "idx_account_code_type";

    // ==================== Journal Entry Indices ====================

    /**
     * Journal Entry table indices:
     * - entry_number: Unique index for entry lookups
     * - date: Index for date range queries
     * - posted: Index for filtering posted/unposted entries
     * - reference: Index for external reference searches
     */
    public static final String JE_NUMBER_INDEX = "idx_je_number";
    public static final String JE_DATE_INDEX = "idx_je_date";
    public static final String JE_POSTED_INDEX = "idx_je_posted";
    public static final String JE_REFERENCE_INDEX = "idx_je_reference";
    public static final String JE_DATE_POSTED_INDEX = "idx_je_date_posted";

    // ==================== Invoice Indices ====================

    /**
     * Invoice table indices:
     * - invoice_number: Unique index for invoice lookups
     * - type: Index for filtering sales/purchase invoices
     * - status: Index for filtering by status
     * - date: Index for date range queries
     * - customer_id: Foreign key index
     * - supplier_id: Foreign key index
     * - journal_entry_id: Foreign key index for posted invoices
     */
    public static final String INV_NUMBER_INDEX = "idx_invoice_number";
    public static final String INV_TYPE_INDEX = "idx_invoice_type";
    public static final String INV_STATUS_INDEX = "idx_invoice_status";
    public static final String INV_DATE_INDEX = "idx_invoice_date";
    public static final String INV_CUSTOMER_INDEX = "idx_invoice_customer";
    public static final String INV_SUPPLIER_INDEX = "idx_invoice_supplier";
    public static final String INV_DUE_DATE_INDEX = "idx_invoice_due_date";
    public static final String INV_TYPE_STATUS_INDEX = "idx_invoice_type_status";

    // ==================== Customer/Supplier Indices ====================

    /**
     * Customer table indices:
     * - code: Unique index for customer code lookups
     * - name: Index for name searches
     * - tax_id: Index for tax ID searches
     * - active: Index for filtering active customers
     */
    public static final String CUST_CODE_INDEX = "idx_customer_code";
    public static final String CUST_NAME_INDEX = "idx_customer_name";
    public static final String CUST_TAX_ID_INDEX = "idx_customer_tax_id";
    public static final String CUST_ACTIVE_INDEX = "idx_customer_active";
    public static final String CUST_EMAIL_INDEX = "idx_customer_email";

    /**
     * Supplier table indices (same pattern as customers):
     * - code: Unique index for supplier code lookups
     * - name: Index for name searches
     * - tax_id: Index for tax ID searches
     * - active: Index for filtering active suppliers
     */
    public static final String SUPP_CODE_INDEX = "idx_supplier_code";
    public static final String SUPP_NAME_INDEX = "idx_supplier_name";
    public static final String SUPP_TAX_ID_INDEX = "idx_supplier_tax_id";
    public static final String SUPP_ACTIVE_INDEX = "idx_supplier_active";
    public static final String SUPP_EMAIL_INDEX = "idx_supplier_email";

    // ==================== Fiscal Year Indices ====================

    /**
     * Fiscal Year table indices:
     * - year: Unique index for year lookups
     * - current: Index for finding current year
     * - closed: Index for filtering open/closed years
     */
    public static final String FY_YEAR_INDEX = "idx_fiscal_year_year";
    public static final String FY_CURRENT_INDEX = "idx_fiscal_year_current";
    public static final String FY_CLOSED_INDEX = "idx_fiscal_year_closed";
    public static final String FY_YEAR_CURRENT_INDEX = "idx_fiscal_year_year_current";

    // ==================== Journal Entry Line Indices ====================

    /**
     * Journal Entry Line table indices:
     * - journal_entry_id: Foreign key index
     * - account_id: Foreign key index for ledger queries
     * - debit: Index for debit amount filtering
     * - credit: Index for credit amount filtering
     */
    public static final String JEL_JE_INDEX = "idx_jel_journal_entry";
    public static final String JEL_ACCOUNT_INDEX = "idx_jel_account";
    public static final String JEL_ACCOUNT_DATE_INDEX = "idx_jel_account_date";

    // ==================== Audit Log Indices ====================

    /**
     * Audit Log table indices:
     * - entity_type: Index for filtering by entity type
     * - entity_id: Index for entity-specific logs
     * - user_id: Index for user-specific logs
     * - timestamp: Index for time-based queries
     * - action: Index for action type filtering
     */
    public static final String AUDIT_ENTITY_INDEX = "idx_audit_entity";
    public static final String AUDIT_ENTITY_ID_INDEX = "idx_audit_entity_id";
    public static final String AUDIT_USER_INDEX = "idx_audit_user";
    public static final String AUDIT_TIMESTAMP_INDEX = "idx_audit_timestamp";
    public static final String AUDIT_ACTION_INDEX = "idx_audit_action";
    public static final String AUDIT_ENTITY_TIMESTAMP_INDEX = "idx_audit_entity_timestamp";

    // ==================== DDL Generation Helper ====================

    /**
     * Generates DDL statements for creating all indices.
     * 
     * <p>This method can be used during development to generate
     * the CREATE INDEX statements for manual execution.
     * 
     * @return array of DDL statements
     */
    public static String[] generateCreateIndexStatements() {
        return new String[] {
            // Account indices
            "CREATE INDEX IF NOT EXISTS " + ACCOUNT_TYPE_INDEX + " ON accounts(type)",
            "CREATE INDEX IF NOT EXISTS " + ACCOUNT_PARENT_INDEX + " ON accounts(parent_id)",
            "CREATE INDEX IF NOT EXISTS " + ACCOUNT_ACTIVE_INDEX + " ON accounts(active)",
            
            // Journal Entry indices
            "CREATE INDEX IF NOT EXISTS " + JE_DATE_INDEX + " ON journal_entries(date)",
            "CREATE INDEX IF NOT EXISTS " + JE_POSTED_INDEX + " ON journal_entries(posted)",
            "CREATE INDEX IF NOT EXISTS " + JE_REFERENCE_INDEX + " ON journal_entries(reference)",
            
            // Invoice indices
            "CREATE INDEX IF NOT EXISTS " + INV_TYPE_INDEX + " ON invoices(type)",
            "CREATE INDEX IF NOT EXISTS " + INV_STATUS_INDEX + " ON invoices(status)",
            "CREATE INDEX IF NOT EXISTS " + INV_DATE_INDEX + " ON invoices(date)",
            "CREATE INDEX IF NOT EXISTS " + INV_CUSTOMER_INDEX + " ON invoices(customer_id)",
            "CREATE INDEX IF NOT EXISTS " + INV_SUPPLIER_INDEX + " ON invoices(supplier_id)",
            "CREATE INDEX IF NOT EXISTS " + INV_DUE_DATE_INDEX + " ON invoices(due_date)",
            
            // Customer/Supplier indices
            "CREATE INDEX IF NOT EXISTS " + CUST_NAME_INDEX + " ON customers(name)",
            "CREATE INDEX IF NOT EXISTS " + CUST_ACTIVE_INDEX + " ON customers(active)",
            "CREATE INDEX IF NOT EXISTS " + SUPP_NAME_INDEX + " ON suppliers(name)",
            "CREATE INDEX IF NOT EXISTS " + SUPP_ACTIVE_INDEX + " ON suppliers(active)",
            
            // Fiscal Year indices
            "CREATE INDEX IF NOT EXISTS " + FY_CURRENT_INDEX + " ON fiscal_years(current)",
            "CREATE INDEX IF NOT EXISTS " + FY_CLOSED_INDEX + " ON fiscal_years(closed)",
            
            // Journal Entry Line indices
            "CREATE INDEX IF NOT EXISTS " + JEL_ACCOUNT_INDEX + " ON journal_entry_lines(account_id)",
            
            // Audit Log indices
            "CREATE INDEX IF NOT EXISTS " + AUDIT_ENTITY_INDEX + " ON audit_logs(entity_type)",
            "CREATE INDEX IF NOT EXISTS " + AUDIT_TIMESTAMP_INDEX + " ON audit_logs(timestamp)",
            "CREATE INDEX IF NOT EXISTS " + AUDIT_ACTION_INDEX + " ON audit_logs(action)"
        };
    }

    /**
     * Generates DDL statements for dropping all indices.
     * 
     * @return array of DDL statements
     */
    public static String[] generateDropIndexStatements() {
        return new String[] {
            "DROP INDEX IF EXISTS " + ACCOUNT_TYPE_INDEX,
            "DROP INDEX IF EXISTS " + ACCOUNT_PARENT_INDEX,
            "DROP INDEX IF EXISTS " + ACCOUNT_ACTIVE_INDEX,
            "DROP INDEX IF EXISTS " + JE_DATE_INDEX,
            "DROP INDEX IF EXISTS " + JE_POSTED_INDEX,
            "DROP INDEX IF EXISTS " + JE_REFERENCE_INDEX,
            "DROP INDEX IF EXISTS " + INV_TYPE_INDEX,
            "DROP INDEX IF EXISTS " + INV_STATUS_INDEX,
            "DROP INDEX IF EXISTS " + INV_DATE_INDEX,
            "DROP INDEX IF EXISTS " + INV_CUSTOMER_INDEX,
            "DROP INDEX IF EXISTS " + INV_SUPPLIER_INDEX,
            "DROP INDEX IF EXISTS " + INV_DUE_DATE_INDEX,
            "DROP INDEX IF EXISTS " + CUST_NAME_INDEX,
            "DROP INDEX IF EXISTS " + CUST_ACTIVE_INDEX,
            "DROP INDEX IF EXISTS " + SUPP_NAME_INDEX,
            "DROP INDEX IF EXISTS " + SUPP_ACTIVE_INDEX,
            "DROP INDEX IF EXISTS " + FY_CURRENT_INDEX,
            "DROP INDEX IF EXISTS " + FY_CLOSED_INDEX,
            "DROP INDEX IF EXISTS " + JEL_ACCOUNT_INDEX,
            "DROP INDEX IF EXISTS " + AUDIT_ENTITY_INDEX,
            "DROP INDEX IF EXISTS " + AUDIT_TIMESTAMP_INDEX,
            "DROP INDEX IF EXISTS " + AUDIT_ACTION_INDEX
        };
    }
}
