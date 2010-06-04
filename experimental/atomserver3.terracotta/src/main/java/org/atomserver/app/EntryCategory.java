package org.atomserver.app;

/**
 * represents an instance of a CategoryNode on an Entry.
 */
public class EntryCategory {
    private final CategoryNode category;
    private final String label;

    /**
     * construct a new EntryCategory with the given category and label.
     *
     * @param category the category to apply to an Entry
     * @param label    the label to apply to this Category, for this Entry
     */
    public EntryCategory(CategoryNode category, String label) {
        this.category = category;
        this.label = label;
    }

    /**
     * getter for category.
     *
     * @return category
     */
    public CategoryNode getCategory() {
        return category;
    }

    /**
     * getter for label.
     *
     * @return label
     */
    public String getLabel() {
        return label;
    }

    public int hashCode() {
        return category.hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj ||
               (this != null &&
                EntryCategory.class.equals(obj.getClass()) &&
                category.equals(((EntryCategory) obj).category));
    }
}
