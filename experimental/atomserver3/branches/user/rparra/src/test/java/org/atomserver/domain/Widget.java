package org.atomserver.domain;

import org.simpleframework.xml.*;

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

    @Override
    public boolean equals(Object o) {
        return (o != null && Widget.class.equals(o.getClass())) &&
               id == ((Widget)o).id &&
               color == null ? ((Widget)o).color == null : color.equals(((Widget)o).color) &&
               name == null ? ((Widget)o).name == null : name.equals(((Widget)o).name);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 8675309 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
