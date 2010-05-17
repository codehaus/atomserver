package org.atomserver.spring;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.FactoryBean;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUriFactoryBean implements FactoryBean {

    private static final Pattern CONFIG_URI_PATTERN =
            Pattern.compile("atomserver:([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)((?:;[a-zA-Z_]\\w*=[^;]*)*)");

    private String className;
    private Map<String, String> propMap;

    public void setConfigUri(String configUri) {
        Matcher matcher = CONFIG_URI_PATTERN.matcher(configUri);
        if (!matcher.matches()) {
            throw new IllegalStateException("illegal config URI!");
        }
        className = matcher.group(1);
        String[] parts = matcher.group(2).split(";");


        propMap = new HashMap<String, String>();
        for (int i = 1; i < parts.length; i++) {
            String[] nameAndValue = parts[i].split("=", 2);
            if (nameAndValue.length > 1) {
                try {
                    String name = URLDecoder.decode(nameAndValue[0], "UTF-8");
                    String value = URLDecoder.decode(nameAndValue[1], "UTF-8");
                    propMap.put(name, value);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e); // TODO: handle
                }
            }
        }
    }

    public Object getObject() throws Exception {
        Class clazz = Class.forName(className);
        final BeanWrapper beanWrapper =
                PropertyAccessorFactory.forBeanPropertyAccess(clazz.newInstance());
        beanWrapper.setPropertyValues(propMap);
        final Object instance = beanWrapper.getWrappedInstance();

        for (Method method : instance.getClass().getMethods()) {
            if (method.getAnnotation(PostConstruct.class) != null) {
                method.invoke(instance);
            }
        }

        return instance;
    }

    public Class getObjectType() {
        return Object.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
