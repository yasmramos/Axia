package io.github.yasmramos.axia.service;

import io.ebean.DB;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing journal entry templates.
 * Enables quick creation of recurring entries.
 *
 * @author Yasmany Ramos Garcia
 */
public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    /**
     * Saves or updates a template.
     *
     * @param template the template to save
     */
    public void save(JournalEntryTemplate template) {
        logger.info("Saving template: {}", template.getName());
        DB.save(template);
    }

    /**
     * Finds a template by ID.
     *
     * @param id the template ID
     * @return optional containing the template if found
     */
    public Optional<JournalEntryTemplate> findById(Long id) {
        logger.debug("Finding template by id: {}", id);
        return Optional.ofNullable(DB.find(JournalEntryTemplate.class, id));
    }

    /**
     * Gets all active templates.
     *
     * @return list of active templates
     */
    public List<JournalEntryTemplate> findAllActive() {
        logger.debug("Finding all active templates");
        return DB.find(JournalEntryTemplate.class)
                .where()
                .eq("active", true)
                .orderBy().asc("name")
                .findList();
    }

    /**
     * Searches templates by name.
     *
     * @param name the name to search
     * @return list of matching templates
     */
    public List<JournalEntryTemplate> searchByName(String name) {
        logger.debug("Searching templates by name: {}", name);
        return DB.find(JournalEntryTemplate.class)
                .where()
                .ilike("name", "%" + name + "%")
                .eq("active", true)
                .findList();
    }

    /**
     * Creates journal entries from a template.
     *
     * @param templateId the template ID
     * @param entryDate the date for the entries
     * @param amount the amount (overrides default if provided)
     * @param description optional description override
     * @return list of created journal entries
     */
    public List<JournalEntry> createFromTemplate(Long templateId, LocalDate entryDate, 
                                                  BigDecimal amount, String description) {
        logger.info("Creating entries from template {} for date {}", templateId, entryDate);

        Optional<JournalEntryTemplate> template = findById(templateId);
        if (template.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }

        JournalEntryTemplate t = template.get();
        BigDecimal entryAmount = amount != null ? amount : t.getDefaultAmount();
        String entryDescription = description != null ? description : t.getDescription();

        List<JournalEntry> entries = new ArrayList<>();

        // Create debit entry
        if (t.getDebitAccount() != null) {
            JournalEntry debitEntry = new JournalEntry();
            debitEntry.setAccount(t.getDebitAccount());
            debitEntry.setEntryDate(entryDate);
            debitEntry.setDebitAmount(entryAmount);
            debitEntry.setDescription(entryDescription);
            debitEntry.setReference("TPL-" + templateId);
            entries.add(debitEntry);
        }

        // Create credit entry
        if (t.getCreditAccount() != null) {
            JournalEntry creditEntry = new JournalEntry();
            creditEntry.setAccount(t.getCreditAccount());
            creditEntry.setEntryDate(entryDate);
            creditEntry.setCreditAmount(entryAmount);
            creditEntry.setDescription(entryDescription);
            creditEntry.setReference("TPL-" + templateId);
            entries.add(creditEntry);
        }

        // Save entries
        for (JournalEntry entry : entries) {
            DB.save(entry);
        }

        logger.info("Created {} entries from template", entries.size());
        return entries;
    }

    /**
     * Deletes a template by ID.
     *
     * @param id the template ID
     * @return true if deleted
     */
    public boolean delete(Long id) {
        logger.info("Deleting template: {}", id);
        return DB.delete(JournalEntryTemplate.class, id);
    }

    /**
     * Deactivates a template.
     *
     * @param id the template ID
     * @return true if deactivated
     */
    public boolean deactivate(Long id) {
        logger.info("Deactivating template: {}", id);
        Optional<JournalEntryTemplate> template = findById(id);
        if (template.isPresent()) {
            template.get().setActive(false);
            DB.save(template.get());
            return true;
        }
        return false;
    }
}
