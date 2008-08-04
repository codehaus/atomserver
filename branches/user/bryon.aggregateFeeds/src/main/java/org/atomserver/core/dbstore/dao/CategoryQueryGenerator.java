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

import org.atomserver.AtomCategory;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.BooleanTerm;
import org.atomserver.utils.logic.Conjunction;
import org.atomserver.utils.logic.Disjunction;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class CategoryQueryGenerator {
    private final Collection<BooleanExpression<AtomCategory>> exprs;

    public static String generate(Collection<BooleanExpression<AtomCategory>> exprs) {
        return new CategoryQueryGenerator(exprs).generateSQL();
    }

    CategoryQueryGenerator(Collection<BooleanExpression<AtomCategory>> exprs) {
        if (exprs.size() == 1) {
            BooleanExpression<AtomCategory> singleExpr = exprs.iterator().next();
            if (singleExpr instanceof Conjunction) {
                BooleanExpression<AtomCategory> lhs =
                        ((Conjunction) singleExpr).getLhs();
                BooleanExpression<AtomCategory> rhs =
                        ((Conjunction) singleExpr).getRhs();
                this.exprs = Arrays.asList(lhs, rhs);
                return;
            }
        }
        this.exprs = exprs;
    }

    String generateSQL() {
        StringBuilder builder = null;
        for (BooleanExpression<AtomCategory> expr : exprs) {
            if (builder == null) {
                builder = new StringBuilder();
            } else {
                builder.append("\nAND\n");
            }
            builder.append(generate(expr));
        }
        return builder == null ? "" : builder.toString();
    }

    private String generate(BooleanExpression<AtomCategory> expr) {
        return (expr instanceof BooleanTerm) ?
               generateAtom((BooleanTerm<AtomCategory>) expr)
               : (expr instanceof Conjunction) ?
                 generateAnd((Conjunction<AtomCategory>) expr)
                 : generateOr((Disjunction<AtomCategory>) expr);
    }

    private String generateAnd(Conjunction<AtomCategory> expr) {
        return MessageFormat.format("{0}\nAND\n{1}",
                                    generate(expr.getLhs()),
                                    generate(expr.getRhs())
        );
    }

    private String generateOr(Disjunction<AtomCategory> expr) {
        return MessageFormat.format("(\n{0}\nOR\n{1}\n)",
                                    generate(expr.getLhs()),
                                    generate(expr.getRhs())
        );
    }

    private String generateAtom(BooleanTerm<AtomCategory> expr) {
        return MessageFormat.format(
                "(SUM(CASE WHEN searchCategories.Scheme = ''{0}''\n" +
                "           AND searchCategories.Term = ''{1}'' THEN 1 ELSE 0 END) > 0)",
                expr.getValue().getScheme(),
                expr.getValue().getTerm()
        );
    }
}
