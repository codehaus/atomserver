package org.atomserver.app;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a single atom category.
 * <p/>
 * note: CategoryNode does NOT include a "label" value, because a label can be specified for each
 * USAGE of a category.  Labels are modeled on EntryCategory, which is the Category X Entry space.
 */
public class CategoryNode implements Serializable {
    private final String scheme;
    private final String term;

    /**
     * construct a new CategoryNode with the given scheme/term.
     *
     * @param scheme the scheme for the CategoryNode
     * @param term   the term for the CategoryNode
     */
    CategoryNode(String scheme, String term) {
        this.scheme = scheme;
        this.term = term;
    }

    /**
     * getter for scheme.
     *
     * @return scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * getter for term.
     *
     * @return term
     */
    public String getTerm() {
        return term;
    }

    public int hashCode() {
        return getScheme().hashCode() + 8675309 * getTerm().hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj ||
               (obj != null && CategoryNode.class.equals(obj.getClass()) &&
                getScheme().equals(((CategoryNode) obj).getScheme()) &&
                getTerm().equals(((CategoryNode) obj).getTerm()));
    }

    public String toString() {
        return String.format("(%s)%s", scheme, term);
    }

    private static final Map<String, Map<String, CategoryNode>> CATEGORIES =
            new HashMap<String, Map<String, CategoryNode>>();

    private Object readResolve() throws ObjectStreamException {
        // TODO: log this
        return intern(this);
    }

    public static CategoryNode intern(CategoryNode categoryNode) {
        return category(categoryNode.scheme, categoryNode.term);
    }

    public static CategoryNode category(String scheme, String term) {
        Map<String, CategoryNode> schemeMap = CATEGORIES.get(scheme);
        if (schemeMap == null) {
            CATEGORIES.put(scheme, schemeMap = new HashMap<String, CategoryNode>());
        }
        CategoryNode categoryNode = schemeMap.get(term);
        if (categoryNode == null) {
            schemeMap.put(term, categoryNode = new CategoryNode(scheme, term));
        }
        return categoryNode;
    }

}
