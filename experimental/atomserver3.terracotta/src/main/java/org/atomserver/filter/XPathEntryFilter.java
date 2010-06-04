package org.atomserver.filter;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.parser.stax.FOMExtensibleElement;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathEntryFilter implements EntryFilter, NamespaceContext {
    private static final Logger log = Logger.getLogger(XPathEntryFilter.class);

    public XPathEntryFilter(ExtensibleElement configElement) {
        setScript(configElement.getSimpleExtension(AtomServerConstants.XPath.SCRIPT));
        // TODO : config other than with script
        onLoad();
    }

    public void filter(Entry entry, EntryFilterChain chain) {
        for (Action action : actions) {
            action.tag(entry,
                       new InputSource(new StringReader(entry.getContent())),
                       xPath.get());
        }
        chain.doChain(entry);
    }

    public void onLoad() {
        xPath = new ThreadLocal<XPath>() {
            protected XPath initialValue() {
                XPath xPath = XPathFactory.newInstance().newXPath();
                xPath.setNamespaceContext(XPathEntryFilter.this);
                return xPath;
            }
        };
    }

    private transient ThreadLocal<XPath> xPath;

    private BidirectionalMap<String, String> namespaceMap = null;
    private List<Action> actions = null;

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
        void tag(Entry entry, InputSource inputSource, XPath xPath);
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
        public void tag(Entry entry, InputSource inputSource, XPath xPath) {
            Iterator iterator = ((FOMExtensibleElement) entry).getChildElements();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (Category.class.isAssignableFrom(next.getClass())) {
                    iterator.remove();
                }
            }
        }
    }

    public static class DeleteSchemeAction extends AbstractAction {

        private String xpath;
        private String scheme;

        /**
         * {@inheritDoc}
         */
        public void tag(Entry entry,
                        InputSource inputSource,
                        XPath xPath) {
            Set<String> deleteSchemes = new HashSet<String>();
            if (xpath != null) {
                try {
                    NodeList nodeList;
                    if (log.isDebugEnabled()) {
                        log.debug("executing XPATH expression : " + xpath);
                    }
                    nodeList = (NodeList) xPath.evaluate(xpath, inputSource, XPathConstants.NODESET);
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

            Iterator iterator = ((FOMExtensibleElement) entry).getChildElements();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (Category.class.isAssignableFrom(next.getClass())) {
                    Category category = (Category) next;
                    if (deleteSchemes.contains(category.getScheme().toString())) {
                        iterator.remove();
                    }
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
        public void tag(Entry entry,
                        InputSource inputSource,
                        XPath xPath) {
            try {
                NodeList nodeList;
                if (log.isDebugEnabled()) {
                    log.debug("executing XPATH expression : " + xpath);
                }
                nodeList = (NodeList) xPath.evaluate(xpath, inputSource, XPathConstants.NODESET);

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
                    String[] replacements = values.toArray(new String[values.size()]);
                    String scheme = MessageFormat.format(this.scheme, replacements);
                    String term = MessageFormat.format(termPattern, replacements);
                    String label = labelPattern == null ? null :
                                   MessageFormat.format(labelPattern, replacements);
                    entry.addCategory(scheme, term, label);

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

    static class BidirectionalMap<K, V> extends HashMap<K, V> {
        private Map<V, K> reverse;

        public BidirectionalMap(Map<K, V> map) {
            this.putAll(map);
            reverse = new HashMap<V, K>(map.size());
            for (K key : map.keySet()) {
                reverse.put(map.get(key), key);
            }
        }

        public K reverseGet(V value) {
            return reverse.get(value);
        }
    }
}
