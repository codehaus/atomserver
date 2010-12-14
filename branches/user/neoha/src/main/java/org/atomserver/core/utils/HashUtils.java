/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.utils;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * Hash related utility methods.
 */
public class HashUtils {

    public static String converToUUIDStandardFormat(byte [] hval) {
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < hval.length; i++) {
           if (i == 4 || i == 6 || i == 8 || i == 10) {
               bld.append("-");
           }
           String s = Integer.toHexString(hval[i] & 0xff);
           if (s.length() == 1) {
               bld.append("0"); // prefix 0 if only 1 digit.
           }
           bld.append(s);
        }
        return bld.toString().toUpperCase();
   }

   public static String convertToSimpleFormat(byte [] hval) {
       StringBuilder bld = new StringBuilder();
        for (int i = 0; i < hval.length; i++) {
           String s = Integer.toHexString(hval[i] & 0xff);
           if (s.length() == 1) {
               bld.append("0"); // prefix 0 if only 1 digit.
           }
           bld.append(s);
        }
        return bld.toString().toUpperCase();
   }

   public static String convertUUIDStandardToSimpleFormat(String hashVal) {
       if(hashVal == null) {
           return null;
       }
       return StringUtils.remove(hashVal,"-");
   }

   private final static String UUID_STD_FORMAT =
           "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
   private final static String PREFIX_Z32 = "00000000000000000000000000000000";

   public static String convertToUUIDStandardFormat(final String hashVal) {
       if(hashVal == null) {
           return null;
       }
       if(hashVal.length() == 36) {
           if(Pattern.matches(UUID_STD_FORMAT, hashVal)) {
               return hashVal.toUpperCase();
           }
       }

       String hash = hashVal;
       if(hashVal.length() < 32) {
           hash = PREFIX_Z32.substring(0, (32 - hash.length())) + hash;
       }

       if(hash.length() == 32) {
            StringBuilder builder = new StringBuilder();
            builder.append(hash.substring(0,8))
               .append("-")
               .append(hash.substring(8,12))
               .append("-")
               .append(hash.substring(12,16))
               .append("-")
               .append(hash.substring(16,20))
               .append("-")
               .append(hash.substring(20));
            return builder.toString().toUpperCase();
       }
       throw new IllegalArgumentException("The original hash value:" + hash + " cannot be converted to UUID standard format.");
   }

}
