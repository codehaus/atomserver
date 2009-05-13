package org.atomserver.domain;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Attribute;

@Root
@Namespace(reference = "http://atomserver.org/widgets/1.0", prefix = "widgets")
public class Widget {
    @Attribute
    private int id;
    @Element
    private String color;
    @Element
    private String name;

    public Widget() {
    }

    public Widget(int id, String color, String name) {
        this.id = id;
        this.color = color;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
