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

package org.atomserver.core.dbstore.dao;

import junit.framework.TestCase;
import org.atomserver.AtomCategory;
import org.atomserver.utils.logic.BooleanTerm;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.Disjunction;
import org.atomserver.utils.logic.Conjunction;

import java.util.Arrays;

// TODO: make this test actually test something.
public class CategoryQueryGeneratorTest extends TestCase {

    public void testCategoryQueryGenerator() throws Exception {
        BooleanTerm<AtomCategory>[] cats = new BooleanTerm[] {
                new BooleanTerm<AtomCategory>("div2", new AtomCategory("divisible", "two")),
                new BooleanTerm<AtomCategory>("div3", new AtomCategory("divisible", "three")),
                new BooleanTerm<AtomCategory>("div5", new AtomCategory("divisible", "five")),
                new BooleanTerm<AtomCategory>("div7", new AtomCategory("divisible", "seven")),
                new BooleanTerm<AtomCategory>("div11", new AtomCategory("divisible", "eleven"))
        };

        String sql = null;

        sql = new CategoryQueryGenerator(
                Arrays.<BooleanExpression<AtomCategory>>asList(
                        cats[0]
                )).generateSQL();
        System.out.println("sql = " + sql);

        sql = new CategoryQueryGenerator(
                Arrays.<BooleanExpression<AtomCategory>>asList(
                        cats[0], cats[1], cats[2]
                )).generateSQL();
        System.out.println("sql = " + sql);

        sql = new CategoryQueryGenerator(
                Arrays.<BooleanExpression<AtomCategory>>asList(
                        cats[0], new Disjunction(cats[1], cats[2])
                )).generateSQL();
        System.out.println("sql = " + sql);

        sql = new CategoryQueryGenerator(
                Arrays.<BooleanExpression<AtomCategory>>asList(
                        new Conjunction(new Disjunction(cats[0], cats[3]),
                                        new Disjunction(cats[1], cats[2]))
                )).generateSQL();
        System.out.println("sql = " + sql);

        sql = new CategoryQueryGenerator(
                Arrays.<BooleanExpression<AtomCategory>>asList(
                        new Conjunction(new Disjunction(cats[0], cats[3]),
                                        new Disjunction(cats[1],
                                                        new Conjunction(new Disjunction(cats[0], cats[3]),
                                                                        new Disjunction(cats[1], cats[2])))),
                        new Conjunction(new Disjunction(cats[0], cats[3]),
                                        new Disjunction(cats[1], cats[2]))
                )).generateSQL();
        System.out.println("sql = " + sql);
    }
}
