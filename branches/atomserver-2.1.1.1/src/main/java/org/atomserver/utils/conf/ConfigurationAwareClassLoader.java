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
package org.atomserver.utils.conf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Enumeration;

/**
 * ConfigurationAwareClassLoader - a ClassLoader that is aware of the atomserver configuration
 * directories.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ConfigurationAwareClassLoader
        extends URLClassLoader {

    static private Log log = LogFactory.getLog(ConfigurationAwareClassLoader.class);

    private static Properties ENV = null;

    private static void loadEnv() {
        ClassLoader confClassLoader = new ConfigurationAwareClassLoader(
                ConfigurationAwareClassLoader.class.getClassLoader());

        ENV = new Properties();
        String envFile = "env/" + System.getProperty("atomserver.env") + ".properties";
        String msg = "Could not load the environment file: " + envFile;
        try {
            getENV().load(confClassLoader.getResourceAsStream(envFile));
        } catch (NullPointerException e) {
            msg = "NullPointerException:: " + msg;
            log.error(msg);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            msg = "IOException:: " + msg;
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    public URL getResource(String s) {
        log.trace("ConfigurationAwareClassLoader.getResource");
        URL resource = super.getResource(s);
        log.trace("found resource " + s + " at " + resource + " in " + this.toString());
        return resource;
    }

    public Enumeration<URL> getResources(String s) throws IOException {
        log.trace("ConfigurationAwareClassLoader.getResources");
        Enumeration<URL> resources = super.getResources(s);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            log.trace("found resource " + s + " at " + resource + " in " + this.toString());
        }
        return super.getResources(s);
    }

    public InputStream getResourceAsStream(String s) {
        log.trace("ConfigurationAwareClassLoader.getResourceAsStream");
        getResource(s);
        return super.getResourceAsStream(s);
    }

    /**
     * create a ClassLoader that delegates to the given classloader as its parent, and is aware of
     * the AtomServer configuration directories.
     *
     * @param parent the ClassLoader to use as the parent of the new ClassLoader
     */
    public ConfigurationAwareClassLoader(ClassLoader parent) {
        super(getConfigurationUrls(), parent);
    }

    /**
     * get the list of URLs to add to the classpath, based on the configuration directories.
     *
     * @return the list of URLs to add to the classpath.
     */
    private static URL[] getConfigurationUrls() {
        try {
            // get the two system properties
            String conf = System.getProperty("atomserver.conf.dir");
            String opsConf = System.getProperty("atomserver.ops.conf.dir");

            // Maven will send us an empty string, so deal with that here
            if (StringUtils.isEmpty(conf)) {
                conf = null;
            }
            if ( StringUtils.isEmpty(opsConf)) {
                opsConf = null;
            }

            log.debug("atomserver.conf.dir= " + conf);
            log.debug("atomserver.ops.conf.dir= " + opsConf);

            // build a list of URLs
            List<URL> urls = new ArrayList<URL>();

            // first, add the OPS conf dir to the classpath, if it exists
            if (opsConf != null) {
                final File opsConfDir = new File(opsConf);
                if (opsConfDir.exists() && opsConfDir.isDirectory()) {
                    urls.add(opsConfDir.toURL());
                    log.debug("added OPS conf dir [" + opsConf +
                              "] to the classpath.");
                } else {
                    log.warn("warning - atomserver OPS conf dir [" + opsConf +
                             "] does not exist, or is not a directory - " +
                             "NOT adding it to the classpath.");
                }
            } else {
                log.debug("no atomserver OPS conf dir specified.");
            }

            if (conf != null) {
                File confDir = new File(conf);
                if (confDir.exists() && confDir.isDirectory()) {
                    final File classesDir = new File(confDir, "classes/");
                    if (classesDir.exists() && classesDir.isDirectory()) {
                        urls.add(classesDir.toURL());
                        log.debug("added atomserver conf classes dir [" + classesDir +
                                  "] to the classpath.");
                    } else {
                        log.warn("warning - atomserver conf classes dir [" + opsConf +
                                 "] does not exist, or is not a directory - " +
                                 "NOT adding it to the classpath.");
                    }

                    File libDir = new File(confDir, "lib/");
                    if (libDir.exists() && libDir.isDirectory()) {

                        File[] jarFiles = libDir.listFiles(new FileFilter() {
                            public boolean accept(File file) {
                                return file.isFile() && file.getName().endsWith(".jar");
                            }
                        });

                        for (File jarFile : jarFiles) {
                            urls.add(jarFile.toURL());
                            log.debug("added jar file [" + jarFile +
                                      "] to the classpath.");
                        }

                    } else {
                        log.debug("no atomserver lib dir specified");
                    }

                } else {
                    log.warn("warning - atomserver conf dir [" + opsConf +
                             "] does not exist, or is not a directory - " +
                             "NOT adding it to the classpath.");
                }

            } else {
                log.debug("no atomserver conf dir specified.");
            }

            return urls.toArray(new URL[urls.size()]);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void invalidateENV() {
        ENV = null;
    }

    public static Properties getENV() {
        if (ENV == null) {
            loadEnv();
        }
        return ENV;
    }
}
