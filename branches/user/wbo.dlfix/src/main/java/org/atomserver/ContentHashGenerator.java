/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver;

/**
 * Interface for hash generator function object.
 */
public interface ContentHashGenerator {

    byte [] hashCode(String content);
    
}
