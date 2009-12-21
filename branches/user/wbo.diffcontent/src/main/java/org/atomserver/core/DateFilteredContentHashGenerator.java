/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core;

import org.atomserver.ContentHashGenerator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ContentHashGenerator function which filters out <lastModified>...</lastModified>  and
 * returns an MD5 hash code.
 */
public class DateFilteredContentHashGenerator
        extends SimpleContentHashGenerator
        implements ContentHashGenerator {

    static final Pattern LAST_MODIFIED_REGX = Pattern.compile("<lastModified>[\\w\\s\\-:,\\.]*</lastModified>|<lastModified/>");

    public byte[] hashCode(String content) {
        Matcher matcher = LAST_MODIFIED_REGX.matcher(content);
        return super.hashCode(matcher.replaceAll("") );
    }
}
