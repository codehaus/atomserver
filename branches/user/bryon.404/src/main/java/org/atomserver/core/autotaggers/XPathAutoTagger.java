/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.atomserver.core.autotaggers;

import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.utils.collections.BidirectionalMap;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XPathAutoTagger - EntryAutoTagger implementation that provides for tagging entries based on
 * matches against XPATH expressions.
 * <p/>
 * You can configure this bean in Spring in the straightforward manner - you can set "namespaceMap"
 * as a map from prefixes to namespace URIs, and the "actions" property to a list of the inner
 * classes on this class for each of the actions that can be performed.  They are:
 * <p/>
 * <dl>
 * <dt>delete all (<code>DeleteAllAction</code>)</dt>
 * <dd>clears all of the categories for the entry</dd>
 * <dt>delete scheme (<code>DeleteSchemeAction</code>)</dt>
 * <dd>clears all of the categories in the given scheme for the entry</dd>
 * <dt>match (<code>XPathMatchException</code>)</dt>
 * <dd>evaluate an XPATH expression against the Entry Contents, and write a category for each match
 * </dd>
 * </dl>
 * when an XPATH expression is matched, the TEXT CONTENT of the nodes matched (can be either
 * elements or attributes) are stored in a variable called $.  Then, the termPattern is evaluated
 * to set the category's term, and the optional labelPattern is evaluated to set the label.
 * <p/>
 * To make configuration easier, this class defines a little scripting language that can be set
 * via the "script" bean property.  Scripts are defined by the following grammar (nonterminals in
 * all caps, terminals in title casing, character and string literals enclosed in single quotes,
 * and parentheses, asterisks, and question marks for grouping, aggregation, and optional
 * components.
 * <pre>
 * <b>nonterminals:</b>
 * SCRIPT       ==>     STATEMENT (';'* STATEMENT ';'*)*
 * STATEMENT    ==>     NAMESPACE | DELETEALL | DELETESCHEME | MATCH
 * NAMESPACE    ==>     'namespace' Prefix '=' Uri
 * DELETEALL    ==>     'delete' 'all'
 * DELETESCHEME ==>     'delete' ('scheme')? {Scheme}
 * MATCH        ==>     'match' '"'Xpath'"' '{'Scheme'}' Termpattern ('['LabelPattern']')?
 * <b>terminals:</b>
 * Prefix       ==>     namespace prefix to use in XPATH expressions
 * Uri          ==>     namespace URIs
 * Xpath        ==>     the XPATH expression to match
 * Scheme       ==>     category schemes
 * Termpattern  ==>     the replacement pattern to use for generating category terms
 * Labelpattern ==>     the replacement pattern to use for generating category labels
 * </pre>
 * keywords (delete, match, namespace, etc.) are not case sensitive.  the quoted string for the
 * XPATH expression can contain double quotes if they are escaped with backslash (i.e. \").  A
 * script could look like:
 * <pre>
 * NAMESPACE widgets = http://schemas.foo.com/widgets/v1/rev0;
 * DELETE SCHEME {urn:foo.brands};
 * MATCH "//widgets:brand" {urn:foo.brands}$;
 * MATCH "//widgets:brand[@isMaster='true']" {urn:foo.brands}MASTER:$[Entry has master brand $]
 * </pre>
 * for example.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class XPathAutoTagger
        extends BaseAutoTagger
        implements NamespaceContext {

    private final ThreadLocal<XPath> xPath = new ThreadLocal<XPath>() {
        protected XPath initialValue() {
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(XPathAutoTagger.this);
            return xPath;
        }
    };

    private BidirectionalMap<String, String> namespaceMap = null;
    private List<Action> actions = null;

    /**
     * {@inheritDoc}
     */
    public void tag(EntryMetaData entry, String contentXML) {
        // this method selects the list of current categories for the entry from the DB, does all
        // modifications in memory, and then updates the DB as a batch at the end.  In the vast
        // majority of cases, these tags will not actually change as a result of auto-tagging, and
        // in all of those cases we get away with only doing a single select against the DB to
        // verify that nothing needs to change.

        // load the initial list of categories for the entry
        List<EntryCategory> initialState = getCategoriesHandler().selectEntryCategories(entry);
        for (EntryCategory entryCategory : initialState) {
            log.debug("TAG-INITIAL:" + entryCategory);
        }

        // make a COPY of that list as a Set -- this is the data structure we will modify
        Set<EntryCategory> categoryMods = new HashSet<EntryCategory>(initialState);
        log.debug("XPathAutoTagger.tag");
        // iterate through the actions and modify the category set as we go
        for (Action action : actions) {
            log.debug(":: action : " + action.getClass());
            action.tag(entry,
                       new InputSource(new StringReader(contentXML)),
                       categoryMods,
                       xPath.get());
        }

        List<EntryCategory> toDelete = new ArrayList<EntryCategory>();
        // for each category from the initial set...
        for (EntryCategory entryCategory : initialState) {            
            // if it's in the mods set, remove it -- we don't need to do anything for that category,
            // because it should end up in the DB, and its already there!
            if (categoryMods.contains(entryCategory)) {
                categoryMods.remove(entryCategory);
            } else {
                // otherwise, we need to delete it
                toDelete.add(entryCategory);
            }
        }
        // if there is anything left over in the mods set, it must be inserted.
        if (!categoryMods.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("autotagger performing " + categoryMods.size() + " inserts");
                for (EntryCategory entryCategory : categoryMods) {
                    log.debug("TAG-WRITE:" + entryCategory);
                }
            }
            getCategoriesHandler().insertEntryCategoryBatch(new ArrayList<EntryCategory>(categoryMods));
        }
        // delete anything that needs to be deleted
        if (!toDelete.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("autotagger performing " + toDelete.size() + " deletes");
            }
            getCategoriesHandler().deleteEntryCategoryBatch(toDelete);
        }
    }

    /**
     * Setter for property 'actions'.
     *
     * @param actions Value to set for property 'actions'.
     */
    public void setActions(List<Action> actions) {
        if (this.actions != null) {
            throw new RuntimeException("you cannot configure actions twice! " +
                                       "(did you specify both actions and script properties in Spring?");
        }
        this.actions = actions;
    }

    /**
     * Setter for property 'namespaceMap'.
     *
     * @param namespaceMap Value to set for property 'namespaceMap'.
     */
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        if (this.namespaceMap != null) {
            throw new RuntimeException("you cannot configure namespaces twice! " +
                                       "(did you specify both namespaces and script properties in Spring?");
        }
        this.namespaceMap = new BidirectionalMap<String, String>(namespaceMap);
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(String prefix) {
        return namespaceMap.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String namespaceURI) {
        return namespaceMap.reverseGet(namespaceURI);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getPrefixes(String namespaceURI) {
        return Collections.singleton(getPrefix(namespaceURI)).iterator();
    }


    public interface Action {
        void tag(EntryMetaData entry, InputSource inputSource, Set<EntryCategory> categoryMods, XPath xPath);
    }

    private static abstract class AbstractAction implements Action {

        protected List<String> subExpressions = new ArrayList<String>();

        protected String transformReplacements(String pattern) {
            StringBuffer buffer = new StringBuffer();
            Matcher matcher = XPATH_SUBEXPRESSION.matcher(pattern);
            while (matcher.find()) {
                this.subExpressions.add(matcher.group(1));
                matcher.appendReplacement(buffer, "{" + this.subExpressions.size() + "}");
            }
            matcher.appendTail(buffer);
            return buffer.toString().replace("$", "{0}");
        }
    }

    public static class DeleteAllAction implements Action {

        /**
         * {@inheritDoc}
         */
        public void tag(EntryMetaData entry, InputSource inputSource, Set<EntryCategory> categoryMods, XPath xPath) {
            categoryMods.clear();
        }
    }

    public static class DeleteSchemeAction extends AbstractAction {

        private String xpath;
        private String scheme;

        /**
         * {@inheritDoc}
         */
        public void tag(EntryMetaData entry,
                        InputSource inputSource,
                        Set<EntryCategory> categoryMods,
                        XPath xPath) {
            Set<String> deleteSchemes = new HashSet<String>();
            if (xpath != null) {
                try {
                    NodeList nodeList = null;
                    StopWatch stopWatch = new Log4JStopWatch();
                    try {
                        if (log.isDebugEnabled())
                            log.debug("executing XPATH expression : " + xpath);
                        nodeList = (NodeList) xPath.evaluate( xpath, inputSource, XPathConstants.NODESET);
                    } finally {
                        stopWatch.stop("XML.fine.xpath", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
                    }
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        List<String> values = new ArrayList<String>(this.subExpressions.size() + 1);
                        values.add(nodeList.item(i).getTextContent());
                        for (String subExpression : this.subExpressions) {
                            NodeList subValue =
                                    (NodeList) xPath.evaluate(subExpression,
                                                              nodeList.item(i),
                                                              XPathConstants.NODESET);
                            values.add(subValue.item(i).getTextContent());
                        }
                        String[] replacements = values.toArray(new String[values.size()]);
                        deleteSchemes.add(MessageFormat.format(scheme, replacements));
                    }
                } catch (XPathExpressionException e) {
                    log.error("unable to delete scheme - exception executing XPath expression", e);
                }
            } else {
                deleteSchemes.add(this.scheme);
            }

            Iterator<EntryCategory> iterator = categoryMods.iterator();
            while (iterator.hasNext()) {
                if (deleteSchemes.contains(iterator.next().getScheme())) {
                    iterator.remove();
                }
            }
        }

        /**
         * Setter for property 'scheme'.
         *
         * @param scheme Value to set for property 'scheme'.
         */
        public void setScheme(String scheme) {
            this.scheme = transformReplacements(scheme);
        }

        /**
         * Setter for property 'xpath'.
         *
         * @param xpath Value to set for property 'xpath'.
         */
        public void setXpath(String xpath) {
            this.xpath = xpath;
        }
    }

    public static class XPathMatchAction extends AbstractAction {

        private String xpath;
        private String scheme;
        private String termPattern;
        private String labelPattern;

        /**
         * {@inheritDoc}
         */
        public void tag(EntryMetaData entry,
                        InputSource inputSource,
                        Set<EntryCategory> categoryMods,
                        XPath xPath) {
            try {
                NodeList nodeList = null;
                StopWatch stopWatch = new Log4JStopWatch();
                try { 
                    if (log.isDebugEnabled()) 
                        log.debug("executing XPATH expression : " + xpath);
                    nodeList = (NodeList) xPath.evaluate( xpath, inputSource, XPathConstants.NODESET);
                } finally {
                    stopWatch.stop("XML.fine.xpath", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
                }

                for (int i = 0; i < nodeList.getLength(); i++) {
                    List<String> values = new ArrayList<String>(this.subExpressions.size() + 1);
                    values.add(nodeList.item(i).getTextContent());
                    for (String subExpression : this.subExpressions) {
                        NodeList subValue =
                                (NodeList) xPath.evaluate(subExpression,
                                                          nodeList.item(i),
                                                          XPathConstants.NODESET);
                        values.add(subValue.item(0).getTextContent());
                    }
                    EntryCategory category = new EntryCategory();
                    category.setEntryStoreId(entry.getEntryStoreId());
                    String[] replacements = values.toArray(new String[values.size()]);
                    category.setScheme(MessageFormat.format(scheme, replacements));
                    category.setTerm(MessageFormat.format(termPattern, replacements));
                    category.setLabel(labelPattern == null ? null :
                                      MessageFormat.format(labelPattern, replacements));
                    if (log.isDebugEnabled()) {
                        log.debug("creating category : " + category);
                    }
                    categoryMods.add(category);
                }
            } catch (XPathExpressionException e) {
                log.error("unable to complete tagging - exception executing XPath expression", e);
            }
        }


        /**
         * Setter for property 'xpath'.
         *
         * @param xpath Value to set for property 'xpath'.
         */
        public void setXpath(String xpath) {
            this.xpath = xpath;
        }

        /**
         * Setter for property 'scheme'.
         *
         * @param scheme Value to set for property 'scheme'.
         */
        public void setScheme(String scheme) {
            this.scheme = transformReplacements(scheme);
        }

        /**
         * Setter for property 'termPattern'.
         *
         * @param termPattern Value to set for property 'termPattern'.
         */
        public void setTermPattern(String termPattern) {
            this.termPattern = transformReplacements(termPattern);
        }

        /**
         * Setter for property 'labelPattern'.
         *
         * @param labelPattern Value to set for property 'labelPattern'.
         */
        public void setLabelPattern(String labelPattern) {
            this.labelPattern = labelPattern == null ? null : transformReplacements(labelPattern);
        }
    }

    private static final Pattern NAMESPACE = Pattern.compile(
            "namespace (\\w+)\\s*=\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_ALL = Pattern.compile(
            "delete\\s+all", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_SCHEME = Pattern.compile(
            "delete(?:\\s+scheme )?\\s*(?:\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\")?\\s*\\{([^\\}]*)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATCH_XPATH = Pattern.compile(
            "match\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"\\s*\\{([^\\}]*)\\}([^\\[]*)(?:\\[([^\\]]*)\\]|())",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern XPATH_SUBEXPRESSION = Pattern.compile(
            "\\$\\|([^\\|]+)\\|", Pattern.CASE_INSENSITIVE);

    /**
     * Setter for property 'script'.
     *
     * @param script Value to set for property 'script'.
     */
    public void setScript(String script) {
        Map<String, String> namespaceMap = new HashMap<String, String>();
        List<Action> actions = new ArrayList<Action>();

        String[] commands = script.split(";");
        for (String command : commands) {
            command = command.replaceAll("\\s+", " ").trim();
            if ("".equals(command)) {
                continue;
            }
            Matcher matcher = DELETE_ALL.matcher(command);
            if (matcher.matches()) {
                actions.add(new DeleteAllAction());
                log.debug("found DELETE ALL command");
            } else {
                matcher = DELETE_SCHEME.matcher(command);
                if (matcher.matches()) {
                    DeleteSchemeAction action = new DeleteSchemeAction();
                    if (matcher.groupCount() > 1) {
                        action.setXpath(matcher.group(1));
                        action.setScheme(matcher.group(2));
                    } else {
                        action.setScheme(matcher.group(1));
                    }
                    actions.add(action);
                    log.debug("found DELETE SCHEME command {" + action.scheme + "}");
                } else {
                    matcher = MATCH_XPATH.matcher(command);
                    if (matcher.matches()) {
                        XPathMatchAction action = new XPathMatchAction();
                        action.setXpath(matcher.group(1).replace("\\\"", "\""));
                        action.setScheme(matcher.group(2));
                        action.setTermPattern(matcher.group(3));
                        action.setLabelPattern(matcher.group(4));
                        actions.add(action);
                        log.debug("found MATCH XPATH command : \"" + action.xpath +
                                  "\" {" + action.scheme + "}" + action.termPattern + "[" +
                                  action.labelPattern + "]");
                    } else {
                        matcher = NAMESPACE.matcher(command);
                        if (matcher.matches()) {
                            namespaceMap.put(matcher.group(1), matcher.group(2));
                            log.debug("found NAMESPACE command : " + matcher.group(1) +
                                      "=" + matcher.group(2));
                        } else {
                            throw new RuntimeException("unknown command : " + command);
                        }
                    }
                }
            }
        }

        setNamespaceMap(namespaceMap);
        setActions(actions);
    }
}
