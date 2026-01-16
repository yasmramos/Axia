package io.github.yasmramos.axia.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Represents a fiscal year period.
 * 
 * <p>Fiscal years define the accounting periods for financial reporting.
 * Typically aligned with calendar years, but can be customized.
 * Once closed, no new transactions can be posted to that period.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Entity
@Table(name = "fiscal_years")
public class FiscalYear extends BaseModel {

    /** The year number (e.g., 2024) */
    @Column(nullable = false)
    private Integer year;

    /** Start date of the fiscal year */
    @Column(nullable = false)
    private LocalDate startDate;

    /** End date of the fiscal year */
    @Column(nullable = false)
    private LocalDate endDate;

    /** Whether the fiscal year has been closed */
    @Column(nullable = false)
    private boolean closed = false;

    /** Whether this is the currently active fiscal year */
    @Column(nullable = false)
    private boolean current = false;

    /** Optional description for the fiscal year */
    private String description;

    // ==================== Getters and Setters ====================

    /** @return the year number */
    public Integer getYear() {
        return year;
    }

    /** @param year the year number */
    public void setYear(Integer year) {
        this.year = year;
    }

    /** @return the start date */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** @param startDate the start date */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** @return the end date */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** @param endDate the end date */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** @return true if closed */
    public boolean isClosed() {
        return closed;
    }

    /** @param closed the closed status */
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    /** @return true if current */
    public boolean isCurrent() {
        return current;
    }

    /** @param current the current status */
    public void setCurrent(boolean current) {
        this.current = current;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description */
    public void setDescription(String description) {
        this.description = description;
    }
}
