package org.atomserver.core;

import java.io.Serializable;

public class CategoryTuple implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6224169891988067571L;
	public final String scheme;
    public final String term;
    public final String label;

    public CategoryTuple(String scheme, String term, String label) {
        this.scheme = scheme;
        this.term = term;
        this.label = label;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CategoryTuple that = (CategoryTuple) o;

        return !(label != null ? !label.equals(that.label) : that.label != null) &&
                scheme.equals(that.scheme) &&
                term.equals(that.term);

    }

    public int hashCode() {
        int result = scheme.hashCode();
        result = 31 * result + term.hashCode();
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "CategoryTuple{" +
                "scheme='" + scheme + '\'' +
                ", term='" + term + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
