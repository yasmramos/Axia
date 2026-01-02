package io.github.yasmramos.axia.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Abstract base model providing common fields for all entities.
 * 
 * <p>Includes automatic tracking of:
 * <ul>
 *   <li>Primary key (id)</li>
 *   <li>Optimistic locking version</li>
 *   <li>Creation timestamp</li>
 *   <li>Last modification timestamp</li>
 * </ul>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@MappedSuperclass
public abstract class BaseModel extends Model {

    /** Unique identifier for the entity */
    @Id
    private Long id;

    /** Version number for optimistic locking */
    @Version
    private Long version;

    /** Timestamp when the entity was created */
    @WhenCreated
    private Instant createdAt;

    /** Timestamp when the entity was last modified */
    @WhenModified
    private Instant updatedAt;

    /**
     * Gets the unique identifier.
     * @return the entity ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     * @param id the entity ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the version number for optimistic locking.
     * @return the version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version number.
     * @param version the version number
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Gets the creation timestamp.
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last modification timestamp.
     * @return the last modification timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last modification timestamp.
     * @param updatedAt the last modification timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
