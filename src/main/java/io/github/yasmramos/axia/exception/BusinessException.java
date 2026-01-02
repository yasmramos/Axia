package io.github.yasmramos.axia.exception;

/**
 * Exception thrown when a business rule is violated.
 * 
 * <p>This exception represents errors that occur due to violations
 * of business logic rules, such as attempting to delete an account
 * with children or posting an unbalanced journal entry.
 * 
 * <p>Common error codes:
 * <ul>
 *   <li>ACCOUNT_HAS_CHILDREN - Cannot delete account with child accounts</li>
 *   <li>ACCOUNT_HAS_BALANCE - Cannot delete account with non-zero balance</li>
 *   <li>ENTRY_UNBALANCED - Journal entry debits do not equal credits</li>
 *   <li>ENTRY_POSTED - Cannot modify a posted entry</li>
 *   <li>INVOICE_INVALID_STATE - Invoice cannot be modified in current state</li>
 *   <li>FISCAL_YEAR_CLOSED - Cannot modify a closed fiscal year</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class BusinessException extends AxiaException {

    /**
     * Error code for account with children.
     */
    public static final String ACCOUNT_HAS_CHILDREN = "ACCOUNT_HAS_CHILDREN";
    
    /**
     * Error code for account with balance.
     */
    public static final String ACCOUNT_HAS_BALANCE = "ACCOUNT_HAS_BALANCE";
    
    /**
     * Error code for unbalanced entry.
     */
    public static final String ENTRY_UNBALANCED = "ENTRY_UNBALANCED";
    
    /**
     * Error code for posted entry.
     */
    public static final String ENTRY_POSTED = "ENTRY_POSTED";
    
    /**
     * Error code for invalid invoice state.
     */
    public static final String INVOICE_INVALID_STATE = "INVOICE_INVALID_STATE";
    
    /**
     * Error code for closed fiscal year.
     */
    public static final String FISCAL_YEAR_CLOSED = "FISCAL_YEAR_CLOSED";
    
    /**
     * Error code for duplicate entity.
     */
    public static final String DUPLICATE_ENTITY = "DUPLICATE_ENTITY";
    
    /**
     * Error code for entity not found.
     */
    public static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";

    /**
     * Constructs a new BusinessException with the specified message and error code.
     * 
     * @param message the detail message
     * @param errorCode the business error code
     */
    public BusinessException(String message, String errorCode) {
        super(message, errorCode, "BUSINESS");
    }

    /**
     * Constructs a new BusinessException with a standard error code.
     * 
     * @param message the detail message
     * @param errorCode the standard error code
     */
    public BusinessException(String message, StandardErrorCode errorCode) {
        super(message, errorCode.code, "BUSINESS");
    }

    /**
     * Constructs a new BusinessException with a message and cause.
     * 
     * @param message the detail message
     * @param errorCode the error code
     * @param cause the cause
     */
    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    /**
     * Factory method for account has children error.
     * 
     * @param accountCode the account code
     * @return the exception instance
     */
    public static BusinessException accountHasChildren(String accountCode) {
        return new BusinessException(
            "Cannot delete account " + accountCode + " - it has child accounts",
            ACCOUNT_HAS_CHILDREN
        );
    }

    /**
     * Factory method for account has balance error.
     * 
     * @param accountCode the account code
     * @return the exception instance
     */
    public static BusinessException accountHasBalance(String accountCode) {
        return new BusinessException(
            "Cannot delete account " + accountCode + " - it has a non-zero balance",
            ACCOUNT_HAS_BALANCE
        );
    }

    /**
     * Factory method for unbalanced entry error.
     * 
     * @param debitTotal total debits
     * @param creditTotal total credits
     * @return the exception instance
     */
    public static BusinessException entryUnbalanced(java.math.BigDecimal debitTotal, 
                                                     java.math.BigDecimal creditTotal) {
        return new BusinessException(
            String.format("Journal entry is not balanced. Debits: %s, Credits: %s", 
                debitTotal, creditTotal),
            ENTRY_UNBALANCED
        );
    }

    /**
     * Factory method for posted entry error.
     * 
     * @param entryNumber the entry number
     * @return the exception instance
     */
    public static BusinessException entryPosted(Integer entryNumber) {
        return new BusinessException(
            "Journal entry #" + entryNumber + " is already posted and cannot be modified",
            ENTRY_POSTED
        );
    }

    /**
     * Factory method for entity not found error.
     * 
     * @param entityType the entity type name
     * @param id the entity id
     * @return the exception instance
     */
    public static BusinessException entityNotFound(String entityType, Long id) {
        return new BusinessException(
            entityType + " not found with id: " + id,
            ENTITY_NOT_FOUND
        );
    }

    /**
     * Factory method for duplicate entity error.
     * 
     * @param entityType the entity type name
     * @param field the field that is duplicated
     * @param value the duplicate value
     * @return the exception instance
     */
    public static BusinessException duplicateEntity(String entityType, String field, String value) {
        return new BusinessException(
            entityType + " already exists with " + field + ": " + value,
            DUPLICATE_ENTITY
        );
    }

    /**
     * Enum for standard business error codes.
     */
    public enum StandardErrorCode {
        ACCOUNT_HAS_CHILDREN("ACCOUNT_HAS_CHILDREN"),
        ACCOUNT_HAS_BALANCE("ACCOUNT_HAS_BALANCE"),
        ENTRY_UNBALANCED("ENTRY_UNBALANCED"),
        ENTRY_POSTED("ENTRY_POSTED"),
        INVOICE_INVALID_STATE("INVOICE_INVALID_STATE"),
        FISCAL_YEAR_CLOSED("FISCAL_YEAR_CLOSED"),
        DUPLICATE_ENTITY("DUPLICATE_ENTITY"),
        ENTITY_NOT_FOUND("ENTITY_NOT_FOUND");

        private final String code;

        StandardErrorCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
