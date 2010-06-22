package org.atomserver.sharding;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@XmlRootElement
public class Distribution {
    private List<Selector> selectors = new ArrayList<Selector>();

    @XmlElement(name = "selector")
    public List<Selector> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
    }

    public CompiledDistribution compile() {
        return new CompiledDistribution();
    }

    public class CompiledDistribution {
        private List<CompiledSelector> compiledSelectors;

        private class CompiledSelector {
            Pattern pattern;
            String[] urls;

            private CompiledSelector(Pattern pattern, String[] urls) {
                this.pattern = pattern;
                this.urls = urls;
            }
        }

        public CompiledDistribution() {
            compiledSelectors = new ArrayList<CompiledSelector>(selectors.size());
            for (Selector selector : selectors) {
                compiledSelectors.add(new CompiledSelector(
                        Pattern.compile(selector.getRegex()),
                        selector.getUrls().toArray(new String[selector.getUrls().size()])));
            }
        }

        public String map(String key) {
            for (CompiledSelector compiledSelector : compiledSelectors) {
                final Matcher matcher = compiledSelector.pattern.matcher(key);
                if (matcher.matches()) {
                    return compiledSelector.urls.length == 1 ?
                            compiledSelector.urls[0] :
                            compiledSelector.urls[hash(matcher) % compiledSelector.urls.length];
                }
            }
            return null;
        }

        private int hash(Matcher matcher) {
            switch (matcher.groupCount()) {
                case 0:
                case 1:
                    return hashKey(matcher.group(matcher.groupCount()));
                default:
                    StringBuilder builder = new StringBuilder();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        builder.append(matcher.group(i));
                    }
                    return hashKey(builder.toString());
            }
        }
    }

    private static int hashKey(String key) {
        return Math.abs(key.hashCode());
    }
}
