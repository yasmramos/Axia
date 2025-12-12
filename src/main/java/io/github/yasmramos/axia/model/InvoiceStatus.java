package io.github.yasmramos.axia.model;

/**
 * Enumeration of invoice lifecycle statuses.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public enum InvoiceStatus {
    
    /** Draft - invoice is being prepared, can be modified */
    DRAFT,
    
    /** Posted - invoice has been recorded in the ledger */
    POSTED,
    
    /** Paid - invoice has been fully paid */
    PAID,
    
    /** Cancelled - invoice has been voided */
    CANCELLED
}
