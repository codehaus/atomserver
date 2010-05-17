package org.atomserver.categories;

import static java.lang.String.format;

public class CategoryQuery {
    public enum Type {
        AND, OR, NOT, SIMPLE
    }

    public final Type type;

    public static CategoryQuery valueOf(String queryString) throws CategoryQueryParseException {
        return queryString == null ? null : CategoryQueryParser.parse(queryString);
    }

    private CategoryQuery(Type type) {
        this.type = type;
    }

    private static abstract class BinaryQuery extends CategoryQuery {
        public final CategoryQuery left;
        public final CategoryQuery right;

        private BinaryQuery(Type type, CategoryQuery left, CategoryQuery right) {
            super(type);
            this.left = left;
            this.right = right;
        }

        public String toString() {
            return format("(%s %s %s)", type, left, right);
        }
    }

    public static class AndQuery extends BinaryQuery {
        public AndQuery(CategoryQuery left, CategoryQuery right) {
            super(Type.AND, left, right);
        }
    }

    public static class OrQuery extends BinaryQuery {
        public OrQuery(CategoryQuery left, CategoryQuery right) {
            super(Type.OR, left, right);
        }
    }

    public static class NotQuery extends CategoryQuery {
        public final CategoryQuery inner;

        public NotQuery(CategoryQuery inner) {
            super(Type.NOT);
            this.inner = inner;
        }

        public String toString() {
            return format("(NOT %s)", inner);
        }
    }

    public static class SimpleCategoryQuery extends CategoryQuery {
        public final String scheme;
        public final String term;

        public SimpleCategoryQuery(String scheme, String term) {
            super(Type.SIMPLE);
            this.scheme = scheme;
            this.term = term;
        }

        public String toString() {
            return format("{%s}%s", scheme, term);
        }
    }
}
