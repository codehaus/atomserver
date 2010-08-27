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
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
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
                                       "xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
                                       "xmlns:ascat=\"http://atomserver.org/namespaces/1.0/category\">" +
                                       "<ascat:category-op type=\"insert-cat\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo0\" term=\"testutils:0\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo1\" term=\"testutils:1\" />" +
                                       "</categories>";
    String expectedCategoryUpdateXML = "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                                       "xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
                                       "xmlns:ascat=\"http://atomserver.org/namespaces/1.0/category\">" +
                                       "<ascat:category-op type=\"update-cat\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo1\" term=\"testutils:1\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo1\" term=\"testutils:A\" />" +
                                       "</categories>";
    String expectedCategoryDeleteXML = "<categories xmlns=\"http://www.w3.org/2007/app\" " +
                                       "xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
                                       "xmlns:ascat=\"http://atomserver.org/namespaces/1.0/category\">" +
                                       "<ascat:category-op type=\"delete-cat\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo1\" term=\"testutils:A\" />" +
                                       "<category xmlns=\"http://www.w3.org/2005/Atom\" scheme=\"urn:widgets/foo0\" term=\"testutils:0\" />" +
                                       "</categories>";

    public static Test suite() { return new TestSuite(CategoryOperationTest.class); }

    public void setUp() throws Exception {
        super.setUp();
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
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

        Categories categories = createCategoriesWithOpToInsert();
        String insertXML = getCategoriesXML(categories);
        assertEquals(insertXML, expectedCategoryInsertXML);
        parseAndValidate(insertXML, CategoryOperation.INSERT);

        categories = createCategoriesWithOpToUpdate();
        String updateXML = getCategoriesXML(categories);
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
        System.out.println("Categories= " + categoriesXML);

        // create property 642.
        String realEntryURL = getServerURL() + "widgets/acme/642.en.xml";
        String realId = urlPath;
        String realEditURI = insert(realId, realEntryURL);
        // add categories to it.
        String editURI = insert(id, (fullURL + "/1"), categoriesXML, false);

        // SELECT
        log.debug("********** Basic insert, delete, and update tests **********");

        editURI = select(fullURL, "urn:widgets/foo");

        categories = createCategoriesWithOpToInsert(); // with CategoryOperation
        String catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.INSERT, 4);

        categories = createCategoriesWithOpToUpdate();
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.UPDATE, 4);

        categories = createCategoriesWithOpToDelete();
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, -1);
        validateCategoriesInDB(categories, fullURL, CategoryOperation.DELETE, 2);

        log.debug("********** Error document tests **********");

        categories = createCategoriesWithOpToInsert();
        catXML = getCategoriesXML(categories);
        editURI = update(id, editURI, catXML, true, 200);
        log.debug("      Inserting Error XML\n" + catXML);
        update(id, editURI, catXML, true, 400);

        categories = createCategoriesWithOpToUpdateError();
        catXML = getCategoriesXML(categories);
        log.debug("      Updating invalid Scheme Error XML\n" + catXML);
        update(id, editURI, catXML, true, 400);

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

    private Category newCategory(String scheme, String term) {
        Category category = getFactory().newCategory();
        category.setScheme(scheme);
        category.setTerm(term);
        return category;
    }

    private Categories createCategoriesWithOpToInsert() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.INSERT);
        categories.addExtension(op);
        int numCats = 2;
        for (int ii = 0; ii < numCats; ii++) {
            categories.addCategory(newCategory("urn:widgets/foo" + ii, "testutils:" + ii));
        }
        return categories;
    }

    private Categories createCategoriesWithOpToUpdate() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.UPDATE);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:1"));
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:A"));
        return categories;
    }

    // update non-existing scheme
    private Categories createCategoriesWithOpToUpdateError() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.UPDATE);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/fooXXX", "testutils:1"));
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:A"));
        return categories;
    }

    // updating wrong scheme with wrong term
    private Categories createCategoriesWithOpToUpdateWithOCError() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.UPDATE);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:B"));
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:A"));
        return categories;
    }

    private Categories createCategoriesWithOpToDelete() {
        Categories categories = getFactory().newCategories();
        CategoryOperation op = getFactory().newExtensionElement(AtomServerConstants.CATEGORY_OP);
        op.setType(CategoryOperation.DELETE);
        categories.addExtension(op);
        categories.addCategory(newCategory("urn:widgets/foo1", "testutils:A"));
        categories.addCategory(newCategory("urn:widgets/foo0", "testutils:0"));
        return categories;
    }

    private String getCategoriesXML(Categories categories) throws Exception {
        StringWriter stringWriter = new StringWriter();
        categories.writeTo(stringWriter);
        String categoriesXML = stringWriter.toString();
        return categoriesXML;
    }

    private void parseAndValidate(String categoriesXML, String opType) {
        Parser parser = serviceContext.getAbdera().getParser();
        Document<Categories> doc = parser.parse(new StringReader(categoriesXML));
        Categories categories = doc.getRoot();
        CategoryOperation catOp = categories.getExtension(AtomServerConstants.CATEGORY_OP);
        assertEquals(opType, catOp.getType());

        if (opType.equals(CategoryOperation.UPDATE)) {
            assertEquals(categories.getCategories().size(), 2);
            Category category1 = categories.getCategories().get(0);
            Category category2 = categories.getCategories().get(1);
            assertTrue(category1.getScheme().equals(category2.getScheme()));
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
                boolean found = false;
                for (Category c1 : allCats) {
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