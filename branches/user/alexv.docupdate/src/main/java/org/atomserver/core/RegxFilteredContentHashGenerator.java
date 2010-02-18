/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core;

import org.atomserver.ContentHashGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

/**
 * ContentHashGenerator function which filters out text patterns based on
 * the given regular expressions specified through spring config. e.g., the
 * following configuration will filter out <lastModified>date-time</lastModified>
 * pattern from the content String.
 * <code>
 *   <bean id="org.atomserver-ContentHashGenerator"
 *        class="org.atomserver.core.RegxFilteredContentHashGenerator"
 *        depends-on="org.atomserver-propertyConfigurer"
 *        lazy-init="true">
 *      <property name="regularExpressions">
 *         <list>
 *          <value>&lt;lastModified&gt;[\w\s\-\.:,]*&lt;/lastModified&gt;</value>
 *         </list>
 *      </property>
 *    </bean>
 *  </code>
 */
public class RegxFilteredContentHashGenerator
        extends SimpleContentHashGenerator
        implements ContentHashGenerator {

    private static final Log log = LogFactory.getLog(RegxFilteredContentHashGenerator.class);

    List<String>  regularExpressions = new ArrayList<String>(); // inject through spring
    List<Pattern> patterns = new ArrayList<Pattern>();          // regx compiled into patterns

    public byte[] hashCode(final String content) {
        return super.hashCode(getFilteredContent(content));
    }

    public List<String> getRegularExpressions() {
        return regularExpressions;
    }

    public void setRegularExpressions(List<String> regularExpressions) {
        this.regularExpressions = regularExpressions;
        patterns.clear();
        for(String regx: regularExpressions) {
            patterns.add(Pattern.compile(regx));
        }
    }

    // Make this public for debug
    public String getFilteredContent(final String content) {
        String filteredContent = content;
         for(Pattern pattern: patterns) {
            Matcher matcher = pattern.matcher(filteredContent);
            filteredContent = matcher.replaceAll("");
        }
        if(log.isDebugEnabled())  {
            log.debug("FilteredContent:"+filteredContent);
        }
        return filteredContent;
    }
}
