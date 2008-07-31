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
    private int term = 0;
    private final Collection<BooleanExpression<AtomCategory>> exprs;

    public static String generate(Collection<BooleanExpression<AtomCategory>> exprs) {
        return new CategoryQueryGenerator(exprs).generateSQL();
    }

    public static String generateAggregate(Collection<BooleanExpression<AtomCategory>> exprs) {
        return new CategoryQueryGenerator(exprs).generateAggregateSQL();
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
        int firstTopLevelTerm = 0;
        StringBuilder builder = null;
        for (BooleanExpression<AtomCategory> expr : exprs) {
            if (builder == null) {
                String termSql = generate(expr);
                firstTopLevelTerm = term++;
                builder = new StringBuilder(
                        MessageFormat.format(
                                "SELECT term{0}.EntryStoreId AS EntryStoreId FROM\n", firstTopLevelTerm));
                builder.append(termSql).append(" term").append(firstTopLevelTerm).append("\n");
            } else {
                builder.append("INNER JOIN\n")
                        .append(generate(expr))
                        .append(" term").append(term)
                        .append("\n")
                        .append("ON term").append(firstTopLevelTerm)
                        .append(".EntryStoreId = term").append(term++)
                        .append(".EntryStoreId\n");
            }
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
        return MessageFormat.format(
                "(SELECT term{2}.EntryStoreId FROM\n" +
                "{0} term{2}\n" +
                "INNER JOIN\n" +
                "{1} term{3}\n" +
                "ON term{2}.EntryStoreId = term{3}.EntryStoreId)",
                generate(expr.getLhs()),
                generate(expr.getRhs()),
                term++,
                term++
        );
    }

    private String generateOr(Disjunction<AtomCategory> expr) {
        return MessageFormat.format(
                "( {0} UNION {1} )",
                generate(expr.getLhs()),
                generate(expr.getRhs())
        );
    }

    private String generateAtom(BooleanTerm<AtomCategory> expr) {
        return MessageFormat.format(
                "( SELECT EntryStoreId FROM EntryCategory WHERE Scheme = ''{0}'' and Term = ''{1}'' )",
                expr.getValue().getScheme(),
                expr.getValue().getTerm()
        );
    }

    String generateAggregateSQL() {
        StringBuilder builder = new StringBuilder();
        for (BooleanExpression<AtomCategory> expr : exprs) {
            builder.append("\nAND\n").append(generateAggregate(expr));
        }
        return builder.toString();
    }

    private String generateAggregate(BooleanExpression<AtomCategory> expr) {
        return (expr instanceof BooleanTerm) ?
               generateAggregateAtom((BooleanTerm<AtomCategory>) expr)
               : (expr instanceof Conjunction) ?
                 generateAggregateAnd((Conjunction<AtomCategory>) expr)
                 : generateAggregateOr((Disjunction<AtomCategory>) expr);
    }

    private String generateAggregateAnd(Conjunction<AtomCategory> expr) {
        return MessageFormat.format("{0}\nAND\n{1}",
                                    generateAggregate(expr.getLhs()),
                                    generateAggregate(expr.getRhs())
        );
    }

    private String generateAggregateOr(Disjunction<AtomCategory> expr) {
        return MessageFormat.format("(\n{0}\nOR\n{1}\n)",
                                    generateAggregate(expr.getLhs()),
                                    generateAggregate(expr.getRhs())
        );
    }

    private String generateAggregateAtom(BooleanTerm<AtomCategory> expr) {
        return MessageFormat.format(
                "(SUM(CASE WHEN infoCategories.Scheme = ''{0}''\n" +
                "           AND infoCategories.Term = ''{1}'' THEN 1 ELSE 0 END) > 0)",
                expr.getValue().getScheme(),
                expr.getValue().getTerm()
        );
    }
}
