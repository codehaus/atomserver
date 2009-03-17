package org.atomserver.util;

import org.apache.log4j.Logger;

import java.util.Locale;
import java.util.Set;

// TODO: we should maybe throw an exception instead of returning null on invalid locales
// TODO: actually, we should maybe allow "invalid" locales altogether...
public class LocaleUtils {

    private static final Logger log = Logger.getLogger(LocaleUtils.class);

    static {
        loadValidLocales();
    }

    static void loadValidLocales() {
        if (log.isDebugEnabled()) {
            log.debug("LocaleUtils:: localeSet= " +
                      (Set<Locale>) org.apache.commons.lang.LocaleUtils.availableLocaleSet());
        }
    }

    public static Locale toLocale(String str) {
        if (str == null) {
            return null;
        }

        Locale locale = org.apache.commons.lang.LocaleUtils.toLocale(str);
        if (!org.apache.commons.lang.LocaleUtils.isAvailableLocale(locale)) {
            return null;
        }

        // We do NOT allow variants
        // TODO: but should we?
        if (!locale.getVariant().equals("")) {
            return null;
        }

        return locale;
    }
}