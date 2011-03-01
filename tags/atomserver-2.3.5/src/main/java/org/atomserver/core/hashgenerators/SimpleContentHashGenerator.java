/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.hashgenerators;

import org.atomserver.ContentHashGenerator;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Wrapper class for MD5 hashing function.
 */
public class SimpleContentHashGenerator implements ContentHashGenerator {

    public byte[] hashCode(String content) {
      return DigestUtils.md5(content);
    }
}
