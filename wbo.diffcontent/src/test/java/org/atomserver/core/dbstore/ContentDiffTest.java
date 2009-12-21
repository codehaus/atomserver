package org.atomserver.core.dbstore;

import org.apache.abdera.model.Base;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.utils.AtomDate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ContentDiffTest extends DBSTestCase {

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates");
        super.setUp();

        entryCategoriesDAO.deleteAllEntryCategories("lalas");
        entryCategoriesDAO.deleteAllEntryCategories("cuckoos");
        entryCategoriesDAO.deleteAllEntryCategories("aloos");

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("lalas"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("cuckoos"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("aloos"));


    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testContentDiff() throws Exception {

        // Add new entries
        for (int i = 3006; i < 3018; i++) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i,0), true, "0");
            if (i % 2 == 0) {
                modifyEntry("cuckoos", "my", entryId, null, cuckooXml(i,0), true, "0");
            }
            if (i % 3 == 0) {
                modifyEntry("aloos", "my", entryId, null, alooXml(i,0), true, "0");
            }
        }

        // Update with the same contents and validate
        List<String> entryTimestamps1 = getTimestampsForEntries();
        // Update with same content - the revision should not be updated and the hash code returned
        for (int i = 3006; i < 3018; i++) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i,0), false, "1", true, true, "false");
            if (i % 2 == 0) {
                modifyEntry("cuckoos", "my", entryId, null, cuckooXml(i,0), false, "1", true, true, "false");
            }
            if (i % 3 == 0) {
                modifyEntry("aloos", "my", entryId, null, alooXml(i,0), false, "1", true, true, "false");
            }
        }

        // Make sure timestamps have not changed
        List<String> entryTimestamps2 = getTimestampsForEntries();
        assertTimestampsEqual(entryTimestamps1,entryTimestamps2);

        // Update with modified content - the revision should be updated and the hash code returned.
         for (int i = 3006; i < 3018; i++) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i,1), false, "1", true, true, "true");
            if (i % 2 == 0) {
                modifyEntry("cuckoos", "my", entryId, null, cuckooXml(i,1), false, "1", true, true, "true");
            }
            if (i % 3 == 0) {
                modifyEntry("aloos", "my", entryId, null, alooXml(i,1), false, "1", true, true, "true");
            }
        }

        // make sure timestamps have been changed
        List<String> entryTimestamps3 = getTimestampsForEntries();
        assertTimestampsNotEqual(entryTimestamps1, entryTimestamps3);

    }

    private List<String> getTimestampsForEntries() {
        Date zeroDate = new Date(0L);
        List<String> entryTimestamps = new ArrayList<String>();
        List<EntryMetaData> entries = entriesDao.selectEntriesByLastModified("lalas","my",zeroDate);
        for(EntryMetaData em: entries) {
            entryTimestamps.add( "lalas" + em.getEntryId() + em.getUpdateTimestamp());
        }

        entries = entriesDao.selectEntriesByLastModified("cuckoos","my",zeroDate);
        for(EntryMetaData em: entries) {
            entryTimestamps.add( "cuckoos" + em.getEntryId() + em.getUpdateTimestamp());
        }

        entries = entriesDao.selectEntriesByLastModified("aloos","my",zeroDate);
        for(EntryMetaData em: entries) {
            entryTimestamps.add( "aloos" + em.getEntryId() + em.getUpdateTimestamp());
        }
        return entryTimestamps;
    }

    private void assertTimestampsEqual(List<String> ts1, List<String> ts2) {
        assertEquals(ts1.size(), ts2.size());
        for(int i = 0; i < ts1.size(); i++) {
            assertEquals(ts1.get(i), ts2.get(i));
        }
    }

    private void assertTimestampsNotEqual(List<String> ts1, List<String> ts2) {
        assertEquals(ts1.size(), ts2.size());
        for(int i = 0; i < ts1.size(); i++) {
            assertFalse(ts1.get(i).equals(ts2.get(i)));
        }
    }

    private static String lalaXml(int id, int contentId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<lala xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        if(contentId == 0) {
            stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        } else if(contentId == 1) {
            stringBuilder.append("<group>").append(id % 2 == 0 ? "evenx" : "oddx").append("</group>");
        }
        stringBuilder.append("</lala>");
        return stringBuilder.toString();
    }

    private static String cuckooXml(int id, int contentId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<cuckoo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        if(contentId == 0) {
            stringBuilder.append("<group>").append(id % 3 == 0 ? "red" : "blue").append("</group>");
        } else if(contentId == 1) {
            stringBuilder.append("<group>").append(id % 3 == 0 ? "redx" : "bluex").append("</group>");            
        }
        stringBuilder.append("</cuckoo>");
        return stringBuilder.toString();
    }

    private static String alooXml(int id, int contentId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<aloo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        stringBuilder.append("<lala>").append(id + 2).append("</lala>");
        if(contentId == 0) {
            stringBuilder.append("<group>").append(id % 5 == contentId ? "heavy" : "light").append("</group>");
        } else if(contentId == 1) {
            stringBuilder.append("<group>").append(id % 5 == contentId ? "heavyx" : "lightx").append("</group>");
        }
        stringBuilder.append("</aloo>");
        return stringBuilder.toString();
    }
}