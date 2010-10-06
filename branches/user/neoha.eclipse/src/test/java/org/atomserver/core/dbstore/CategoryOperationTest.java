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


package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.util.Constants;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.ext.category.CategoryOperation;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;

import java.io.StringWriter;
import java.io.StringReader;
import java.io.File;
import java.util.List;

/**
 */
public class CategoryOperationTest extends CRUDDBSTestCase {

        String expectedCategoryInsertXML = "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                                           "xmlns:catop=\"http://atomserver.org/namespaces/1.0/category\" " +
                                           "xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
                                           "<catop:category-op type=\"modify\" />" +
                                           "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo0\" term=\"testutils:0\" catop:modifyType=\"insert\" />" +
                                           "</categories>";
        String expectedCategoryUpdateXML = "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                                           "xmlns:catop=\"http://atomserver.org/namespaces/1.0/category\" " +
                                           "xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
                                           "<catop:category-op type=\"modify\" />" +
                                           "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo0\" term=\"testutils:A\" label=\"Some testutils A label\" catop:oldTerm=\"testutils:0\" catop:modifyType=\"update\" />" +
                                           "</categories>";
        String expectedCategoryDeleteXML = "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                                           "xmlns:catop=\"http://atomserver.org/namespaces/1.0/category\" " +
                                           "xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
                                           "<catop:category-op type=\"modify\" />" +
                                           "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo0\" term=\"testutils:A\" catop:modifyType=\"delete\" />" +
                                           "</categories>";

    public static Test suite() { return new TestSuite(CategoryOperationTest.class); }

    public void setUp() throws Exception {
        super.setUp();
        categoriesDAO.deleteAllRowsFromEntryCategories();
    }

    public void tearDown() throws Exception { super.tearDown(); }

    protected String getURLPath() { return "tags:widgets/acme/642.en.xml"; }

    protected File getEntryFile(int revision) throws Exception {
        return getEntryFile("widgets", "acme", "642", null, true, revision);
    }

    // --------------------
    //       tests
    //---------------------
    public void testCategoryOperationParse() throws Exception {

        Categories categories = createCategoriesWithOpToInsert(1);
        String insertXML = getCategoriesXML(categories);
        assertEquals(insertXML, expectedCategoryInsertXML);
        parseAndValidate(insertXML, CategoryOperation.INSERT);

        categories = createCategoriesWithOpToUpdate();
        String updateXML = getCategoriesXML(categories);
        System.out.println(" updateXML:" + updateXML);
        assertEquals(updateXML, expectedCategoryUpdateXML);
        parseAndValidate(updateXML, CategoryOperation.UPDATE);

        categories = createCategoriesWithOpToDelete();
        String delXML = getCategoriesXML(categories);
        assertEquals(delXML, expectedCategoryDeleteXML);
        parseAndValidate(delXML, CategoryOperation.DELETE);
    }

    public void testCategoryOperationDB() throws Exception {
        String urlPath = getURLPath();
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        // Create a standard APP Categories doc
        Categories categories = getFactory().newCategories();

        Category category = getFactory().newCategory();
        category.setScheme("urn:widgets/foo");
        category.setTerm("scooby");
        categories.addCategory(category);

        category = getFactory().newCategory();
        category.setScheme("urn:widgets/foo");
        category.setTerm("doo");
        categories.addCategory(category);

        StringWriter stringWriter = new StringWriter();
        categories.writeTo(stringWriter);
        String categoriesXML = stringWriter.toString();
        log.debug("Categories= " + categoriesXML);

        // create property 642.
        String realEntryURL = getServerURL() + "widgets/acme/642.en.xml";
        String realId = urlPath;
        insert(realId, realEntryURL);
        // add categories to it.
        String editURI = insert(id, (fullURL + "/1"), categoriesXML, false);

        // SELECT
        log.debug("********** Basic insert, delete, and update tests **********");

        editURI = select(fullURL, "urn:widgets/foo");

        categories = createCategoriesWithOpToInsert(1); // with CategoryOperation
        String catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.INSERT, 3);

        categories = createCategoriesWithOpToUpdate();
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.UPDATE, 3);

        categories = createCategoriesWithOpToDelete();
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.DELETE, 2);

        log.debug("********** Error document tests **********");

        // insert existing category
        categories = createCategoriesWithOpToInsert(1);
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, 200);
        log.debug("      Inserting Error XML\n" + catXML);
        update(id, editURI, catXML, true, 400);

        // inserting more than one category
        categories = createCategoriesWithOpToInsert(2);
        catXML = getCategoriesXML(categories);
        update(id, editURI, catXML, true, 400);

        // updating with no newTerm or newLabel attribute
        categories = createCategoriesWithOpToUpdateWithNoCurrTermError();
        catXML = getCategoriesXML(categories);
        log.debug("       Update with no new term or label\n" + catXML);
        update(id,editURI, catXML, true, 400);

        // updating non-existing category
        categories = createCategoriesWithOpToUpdateWithNonExistingCategoryError();
        catXML = getCategoriesXML(categories);
        log.debug("      Updating invalid Scheme Error XML\n" + catXML);
        update(id, editURI, catXML, true, 400);

        // updating a category with matching scheme but not term
        categories = createCategoriesWithOpToUpdateWithOCError();
        catXML = getCategoriesXML(categories);
        log.debug("      Updating OC Error XML\n" + catXML);
        update(id, editURI, catXML, true, 409);

        editURI = delete(editURI);

        // DELETE
        // Now delete the actual row for 642
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString("widgets", "acme", "642", LocaleUtils.toLocale("en")));
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
        System.out.println(" ABout to obliterate");
        System.out.println(" entry Target revision=" + entryTarget.getRevision());
        entriesDAO.obliterateEntry(entryTarget);
    }

    @SuppressWarnings("unused")
	private Category newCategory(String scheme, String term) {
        Category category = getFactory().newCategory();
        category.setScheme(scheme);
        category.setTerm(term);
        return category;
    }

    private Category newCategory(String scheme, String term, String label, String currTerm, String op) {
        Category category = getFactory().newCategory();
        category.setScheme(scheme);
        category.setTerm(term);
        if(label != null) {
            category.setLabel(label);
        }
        if(currTerm != null) {
            category.setAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_OLD_TERM, currTerm);
        }
        if(op != null) {
            category.setAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_MODIFYTYPE, op);
        }
        return category;
    }

    private Categories createCategoriesWithOpToInsert(int numCats) {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.MODIFY);
        categories.addExtension(op);
        for (int ii = 0; ii < numCats; ii++) {
            categories.addCategory(newCategory("urn:widgets/foo" + ii, "testutils:" + ii, null, null, CategoryOperation.INSERT));
        }
        return categories;
    }

    private Categories createCategoriesWithOpToUpdate() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.MODIFY);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo0", "testutils:A", "Some testutils A label", "testutils:0", CategoryOperation.UPDATE));
        return categories;
    }

    // update with no new term or label
    private Categories createCategoriesWithOpToUpdateWithNoCurrTermError() {
         Categories categories = getFactory().newCategories();
         CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
         op.setType(CategoryOperation.MODIFY);
         categories.addExtension(op);
         categories.addCategory(newCategory("urn:widgets/foo0", "testutils:B", null, null, CategoryOperation.UPDATE));
         return categories;
     }

    // update non-existing scheme
    private Categories createCategoriesWithOpToUpdateWithNonExistingCategoryError() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.MODIFY);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/fooXXX","testutils:A", null, "testutils:0", CategoryOperation.UPDATE));
        return categories;
    }

    // updating wrong scheme with wrong term
    private Categories createCategoriesWithOpToUpdateWithOCError() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.MODIFY);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo0", "testutils:B", null, "testutils:A",CategoryOperation.UPDATE));
        return categories;
    }

    private Categories createCategoriesWithOpToDelete() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.MODIFY);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo0", "testutils:A", null, null, CategoryOperation.DELETE));
        return categories;
    }

    private String getCategoriesXML(Categories categories) throws Exception {
        StringWriter stringWriter = new StringWriter();
        categories.writeTo(stringWriter);
        String categoriesXML = stringWriter.toString();
        return categoriesXML;
    }

    private void parseAndValidate(String categoriesXML, String modifyType) {
        Parser parser = serviceContext.getAbdera().getParser();
        Document<Categories> doc = parser.parse(new StringReader(categoriesXML));
        Categories categories = doc.getRoot();
        CategoryOperation catOp = categories.getExtension(AtomServerConstants.CATEGORY_OP);
        assertEquals(CategoryOperation.MODIFY, catOp.getType());

        String modType = categories.getCategories().get(0).getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_MODIFYTYPE);
        assertEquals(modifyType, modType);
        if (modifyType.equals(CategoryOperation.UPDATE)) {
            assertEquals(categories.getCategories().size(), 1);
            Category category = categories.getCategories().get(0);
            String oldTerm = category.getAttributeValue(AtomServerConstants.CATEGORY_OP_ATTR_OLD_TERM);
            assertEquals("testutils:0", oldTerm);
            assertEquals("urn:widgets/foo0", category.getScheme().toString());
            assertEquals("testutils:A", category.getTerm());
        }
    }

    protected String select(String fullURL, String xmlTestString) throws Exception {

        int expectedResult = 200;
        // SELECT
        ClientResponse response = clientGetWithFullURL(fullURL, expectedResult);

        IRI editLink = null;
        if (response.getStatus() != 200) {
            assertEquals(expectedResult, response.getStatus());
            if (response.getStatus() == 409) {
                Document<ExtensibleElement> doc = response.getDocument();
                ExtensibleElement error = doc.getRoot();
                log.debug("&&&&&&&&&&&&&& error = " + error);

                Link link = error.getExtension(Constants.LINK);
                log.debug("&&&&&&&&&&&&&& editLink = " + editLink);
                editLink = link.getResolvedHref();
            } else {
                return null;
            }
        } else {
            Document<Entry> doc = response.getDocument();
            Entry entryOut = doc.getRoot();
            editLink = entryOut.getEditLinkResolvedHref();

            log.debug("CONTENT:: " + entryOut.getContent());
            log.debug("xmlTestString:: " + xmlTestString);
            if (xmlTestString != null) {
                assertTrue(entryOut.getContent().indexOf(xmlTestString) != -1);
            }
        }
        assertNotNull("link rel='edit' must not be null", editLink);
        response.release();
        return editLink.toString();
    }

    protected Categories selectNoVerify(String fullURL) throws Exception {
        ClientResponse response = clientGetWithFullURL(fullURL, 200);
        assertEquals(200, response.getStatus());

        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        String contentXML = entry.getContent();
        log.debug("++++++++++++++++++++ contentXML = " + contentXML);

        Parser parser = abdera.getParser();
        Document<Categories> contentDoc = parser.parse(new StringReader(contentXML));
        assertNotNull(contentDoc);

        Categories categories = contentDoc.getRoot();
        response.release();
        return categories;
    }

    void validateCategoriesInDB(Categories categories, String fullURL, String opType, int catCount)
            throws Exception {

        List<Category> cats = categories.getCategories();
        Categories allCategories = selectNoVerify(fullURL);
        List<Category> allCats = allCategories.getCategories();
        assertEquals(catCount, allCats.size());
        if (CategoryOperation.INSERT.equals(opType)) {
            for (Category c : cats) {
                boolean found = false;
                for (Category c1 : allCats) {
                    if (c1.getScheme().equals(c.getScheme()) && c1.getTerm().equals(c.getTerm())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }
        } else if (CategoryOperation.UPDATE.equals(opType)) {
            for (int i = 1; i < cats.size(); i++) {
                Category c = cats.get(i);
                System.out.println(" c=" + c.getScheme() + ":" + c.getTerm());
                boolean found = false;
                for (Category c1 : allCats) {
                    System.out.println(" c1=" + c1.getScheme() + ":" + c1.getTerm());
                    if (c1.getScheme().equals(c.getScheme()) && c1.getTerm().equals(c.getTerm())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }
        } else if (CategoryOperation.DELETE.equals(opType)) {
            for (Category c : cats) {
                boolean found = false;
                for (Category c1 : allCats) {
                    if (c1.getScheme().equals(c.getScheme()) && c1.getTerm().equals(c.getTerm())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(!found);
            }
        }
    }
}