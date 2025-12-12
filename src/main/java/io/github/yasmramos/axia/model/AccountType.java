package io.github.yasmramos.axia.model;

/**
 * Enumeration of account types following standard accounting classification.
 * 
 * <p>Account types determine the normal balance behavior:
 * <ul>
 *   <li>ASSET and EXPENSE accounts increase with debits</li>
 *   <li>LIABILITY, EQUITY, and INCOME accounts increase with credits</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public enum AccountType {
    
    /** Asset accounts - resources owned by the entity */
    ASSET,
    
    /** Liability accounts - obligations owed to others */
    LIABILITY,
    
    /** Equity accounts - owner's residual interest */
    EQUITY,
    
    /** Income accounts - revenue from operations */
    INCOME,
    
    /** Expense accounts - costs incurred in operations */
    EXPENSE
}
