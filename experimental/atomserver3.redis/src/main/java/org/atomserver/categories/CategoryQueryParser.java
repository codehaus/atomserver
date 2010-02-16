package org.atomserver.categories;

import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryQueryParser implements Iterator<CategoryQuery> {
    public static final Pattern CATEGORY_PATTERN = Pattern.compile("(?:\\(([^)]+)\\))?([^)]+)");

    private int pos;
    private String[] path;

    private CategoryQueryParser(String queryPath) {
        this.pos = 0;
        this.path = queryPath.split("/");
    }

    public boolean hasNext() {
        return pos < path.length;
    }

    public CategoryQuery next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException(
                    String.format("[%s] is ill-formed - there are not enough path components to " +
                                  "build a valid query",
                                  StringUtils.arrayToDelimitedString(path, "/")));
        }
        String component = path[pos++];
        Matcher matcher;
        if ("AND".equalsIgnoreCase(component)) {
            return new CategoryQuery.AndQuery(next(), next());
        } else if ("OR".equalsIgnoreCase(component)) {
            return new CategoryQuery.OrQuery(next(), next());
        } else if ("NOT".equalsIgnoreCase(component)) {
            return new CategoryQuery.NotQuery(next());
        } else if ((matcher = CATEGORY_PATTERN.matcher(component)).matches()) {
            return new CategoryQuery.SimpleCategoryQuery(matcher.group(1), matcher.group(2));
        } else {
            throw new NoSuchElementException(
                    String.format("[%s] is malformed - [%s] is not a valid category or a " +
                                  "boolean operator",
                                  StringUtils.arrayToDelimitedString(path, "/"),
                                  component));
        }
    }

    public void remove() { throw new UnsupportedOperationException(); }

    public static CategoryQuery parse(final String queryString)
            throws CategoryQueryParseException {
        CategoryQueryParser parser = new CategoryQueryParser(queryString);
        CategoryQuery query;
        try {
            query = parser.next();
            while (parser.hasNext()) {
                query = new CategoryQuery.AndQuery(query, parser.next());
            }
            return query;
        } catch (NoSuchElementException e) {
            throw new CategoryQueryParseException(e);
        }
    }
}