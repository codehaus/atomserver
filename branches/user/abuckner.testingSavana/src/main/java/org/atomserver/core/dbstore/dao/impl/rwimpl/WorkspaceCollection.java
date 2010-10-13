/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

/**
 *  WorkspaceCollection - used to pass results from the AtomCollection table
 */
public class WorkspaceCollection {
    String workspace;
    String collection;

    public String getWorkspace() { return workspace; }

    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getCollection() { return collection; }

    public void setCollection(String collection) { this.collection = collection; }
}