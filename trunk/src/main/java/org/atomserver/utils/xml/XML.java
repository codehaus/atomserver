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

package org.atomserver.utils.xml;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * XML - a simple class representing an XML element.
 *
 * this class is used to generate XML documents with a simple chaining Java API.  Since the
 * AtomServer has no need to parse XML, only to generate it, we prefer this very simple
 * API that adds no dependencies over the base JDK over one that forces clients to pull in
 * additional jars.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class XML {
    private String name;
    private List<Object> children = new ArrayList<Object>();
    private Map<String, String> attributes = new LinkedHashMap<String, String>();

    private XML(String name, String namespace) {
        this.name = name;
        if (namespace != null) {
            attr("xmlns", namespace);
        }
    }

    /**
     * create a new XML element with the given tag name.
     * @param name the name of the new element
     * @return the element
     */
    public static XML element(String name) {
        return new XML(name, null);
    }

    /**
     * create a new XML element with the given tag name and namespace.
     * @param name the name of the new element
     * @param namespace the namespace for the new element
     * @return the element
     */
    public static XML element(String name, String namespace) {
        return new XML(name, namespace);
    }

    /**
     * add the given XML element as the next child of this element.
     * @param element the element to add
     * @return returns "this", for chaining
     */
    public XML add(XML element) {
        children.add(element);
        return this;
    }

    /**
     * add the given text node as the next child of this element.
     * @param text the text to add
     * @return returns "this", for chaining
     */
    public XML add(String text) {
        children.add(text);
        return this;
    }

    /**
     * add the given text as a CDATA node, as the next child of this element.
     * @param text the text to add as a CDATA node
     * @return returns "this", for chaining
     */
    public XML addCdata(String text) {
        children.add(new StringBuilder("<![CDATA[").append(text).append("]]>"));
        return this;
    }

    /**
     * add the given attribute (name,value) as an XML attribute
     * @param name
     * @param value
     * @return
     */
    public XML attr(String name, String value) {
        attributes.put(name, value);
        return this;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(name);
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            builder.append(" ")
                    .append(attribute.getKey()).append("='")
                    .append(attribute.getValue()).append("'");
        }
        if (children == null || children.isEmpty()) {
            builder.append("/>");
        } else {
            builder.append(">");
            for (Object node : children) {
                builder.append(node.toString());
            }
            builder.append("</").append(name).append(">");
        }
        return builder.toString();
    }
}
