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


package org.atomserver.utils.logic;

import junit.framework.TestCase;
import org.atomserver.utils.logic.ExpressionType;
import org.atomserver.utils.logic.Disjunction;
import org.atomserver.utils.logic.Conjunction;
import org.atomserver.utils.logic.BooleanTerm;

public class BooleanLogicTest extends TestCase {
    public void testBooleanLogic() throws Exception {
        BooleanTerm<String> a = new BooleanTerm<String>("A", "the letter a");
        BooleanTerm<String> b = new BooleanTerm<String>("B", "the letter b");
        BooleanTerm<String> c = new BooleanTerm<String>("C", "the letter c");
        BooleanTerm<String> d = new BooleanTerm<String>("D", "the letter d");
        BooleanTerm<String> e = new BooleanTerm<String>("E", "the letter e");

        Conjunction<String> c_and_d = new Conjunction<String>(c, d);
        Conjunction<String> a_and_b = new Conjunction<String>(a, b);
        Disjunction<String> c_or_d = new Disjunction<String>(c, d);
        Disjunction<String> a_or_b = new Disjunction<String>(a, b);

        assertEquals(ExpressionType.AND, a_and_b.getType());
        assertEquals(ExpressionType.OR, a_or_b.getType());

        assertEquals(ExpressionType.AND, new Conjunction<String>(c_and_d, a_and_b).getType());
        assertEquals(ExpressionType.MIXED, new Disjunction<String>(c_and_d, a_and_b).getType());
        assertEquals(ExpressionType.MIXED, new Conjunction<String>(c_or_d, a_or_b).getType());
        assertEquals(ExpressionType.OR, new Disjunction<String>(c_or_d, a_or_b).getType());
        assertEquals(ExpressionType.MIXED, new Disjunction<String>(c_and_d, a_or_b).getType());
        assertEquals(ExpressionType.MIXED, new Conjunction<String>(c_and_d, a_or_b).getType());
        assertEquals(ExpressionType.OR, new Disjunction<String>(c_or_d, e).getType());
        assertEquals(ExpressionType.MIXED, new Disjunction<String>(c_and_d, e).getType());
        assertEquals(ExpressionType.MIXED, new Conjunction<String>(c_or_d, e).getType());
        assertEquals(ExpressionType.AND, new Conjunction<String>(c_and_d, e).getType());
        assertEquals(ExpressionType.AND,
                     new Conjunction<String>(new Conjunction<String>(c_and_d, a_and_b), e).getType());
        assertEquals(ExpressionType.MIXED,
                     new Disjunction<String>(new Disjunction<String>(c_and_d, a_and_b), e).getType());
    }
}
