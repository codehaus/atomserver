package org.atomserver.core.dbstore.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBTool {

    public static ApplicationContext getToolContext(ApplicationContext parentContext) {
        String[] configs = {
                "/org/atomserver/spring/dbAuxilApplicationContext.xml"
        };
        ClassPathXmlApplicationContext springFactory =
                new ClassPathXmlApplicationContext(configs, false, parentContext);
        springFactory.setClassLoader(
                new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();
        return springFactory;
    }

    public static ApplicationContext getToolContext() {
        String[] configs = {
                "/org/atomserver/spring/propertyConfigurerBeans.xml",
                "/org/atomserver/spring/logBeans.xml",
                "/org/atomserver/spring/storageBeans.xml",
                "/org/atomserver/spring/databaseBeans.xml",
                "/org/atomserver/spring/dbAuxilApplicationContext.xml"
        };
        ClassPathXmlApplicationContext springFactory =
                new ClassPathXmlApplicationContext(configs, false);
        springFactory.setClassLoader(
                new ConfigurationAwareClassLoader(springFactory.getClassLoader()));
        springFactory.refresh();
        return springFactory;
    }

}
