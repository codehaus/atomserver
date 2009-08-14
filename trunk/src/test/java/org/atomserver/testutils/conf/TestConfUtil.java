package org.atomserver.testutils.conf;

import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

import java.io.File;

public class TestConfUtil {

    public static String UNCHANGED = "@@UNCHANGED@@";
    public static final String SYSPROP__CONF_DIR = "atomserver.conf.dir";
    public static final String SYSPROP__OPS_CONF_DIR = "atomserver.ops.conf.dir";
    public static final String SYSPROP__ENV = "atomserver.env";

    private static boolean reentrantFlag = false;
    private static String prevConfDir = null;
    private static String prevOpsConfDir = null;
    private static String prevEnv = null;


    public static void preSetup(String confDirName) throws Exception {
        preSetup(confDirName, UNCHANGED, UNCHANGED);
    }

    public static void preSetup(String confDirName,
                                String opsConfDirName) throws Exception {
        preSetup(confDirName, opsConfDirName, UNCHANGED);
    }

    public static void preSetup(String confDirName,
                                String opsConfDirName,
                                String env) throws Exception {
        if (reentrantFlag) {
            throw new RuntimeException("error - TestConfUtil is not reentrant - every call to " +
                                       "preSetup() must be matched with a call to " +
                                       "postTearDown() before it is called again!");
        }

        ConfigurationAwareClassLoader.invalidateENV();

        prevConfDir = System.getProperty(SYSPROP__CONF_DIR);
        prevOpsConfDir = System.getProperty(SYSPROP__OPS_CONF_DIR);
        prevEnv = System.getProperty(SYSPROP__ENV);


        if (!UNCHANGED.equals(confDirName)) {
            if (confDirName == null) {
                System.clearProperty(SYSPROP__CONF_DIR);
            } else {
                File confDir = new File(
                        TestConfUtil.class.getClassLoader().getResource(confDirName).toURI());
                System.setProperty(SYSPROP__CONF_DIR, confDir.getAbsolutePath());
            }
        }

        if (!UNCHANGED.equals(opsConfDirName)) {
            if (opsConfDirName == null) {
                System.clearProperty(SYSPROP__OPS_CONF_DIR);
            } else {
                File opsConfDir = new File(
                        TestConfUtil.class.getClassLoader().getResource(opsConfDirName).toURI());
                System.setProperty(SYSPROP__OPS_CONF_DIR, opsConfDir.getAbsolutePath());
            }
        }

        if (!UNCHANGED.equals(env)) {
            if (env == null) {
                System.clearProperty(SYSPROP__ENV);
            } else {
                System.setProperty(SYSPROP__ENV, env);
            }
        }

        reentrantFlag = true;
    }

    public static void postTearDown() throws Exception {

        if (prevConfDir == null) {
            System.clearProperty(SYSPROP__CONF_DIR);
        } else {
            System.setProperty(SYSPROP__CONF_DIR, prevConfDir);
        }
        if (prevOpsConfDir == null) {
            System.clearProperty(SYSPROP__OPS_CONF_DIR);
        } else {
            System.setProperty(SYSPROP__OPS_CONF_DIR, prevOpsConfDir);
        }
        if (prevEnv == null) {
            System.clearProperty(SYSPROP__ENV);
        } else {
            System.setProperty(SYSPROP__ENV, prevEnv);
        }
        ConfigurationAwareClassLoader.invalidateENV();

        reentrantFlag = false;
    }
}
