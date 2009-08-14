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
import java.util.Collection;
import java.util.Arrays;

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

    public static String generateAggregate(Collection<BooleanExpression<AtomCategory>> exprs, String join) {
        return new CategoryQueryGenerator(exprs).generateAggregateSQL(join);
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

    String generateAggregateSQL(String join) {
        int firstTopLevelTerm = 0;
        StringBuilder builder = null;
        for (BooleanExpression<AtomCategory> expr : exprs) {
            if (builder == null) {
                String termSql = generateAggregate(expr, join);
                firstTopLevelTerm = term++;
                builder = new StringBuilder(
                        MessageFormat.format(
                                "SELECT term{0}.EntryId AS EntryId FROM\n", firstTopLevelTerm));
                builder.append("(").append(termSql).append(") term").append(firstTopLevelTerm).append("\n");
            } else {
                builder.append("INNER JOIN (\n")
                        .append(generateAggregate(expr, join))
                        .append(") term").append(term)
                        .append("\n")
                        .append("ON term").append(firstTopLevelTerm)
                        .append(".EntryId = term").append(term++)
                        .append(".EntryId\n");
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

    private String generateAggregate(BooleanExpression<AtomCategory> expr, String join) {
        return (expr instanceof BooleanTerm) ?
               generateAggregateAtom((BooleanTerm<AtomCategory>) expr, join)
               : (expr instanceof Conjunction) ?
                 generateAggregateAnd((Conjunction<AtomCategory>) expr, join)
                 : generateAggregateOr((Disjunction<AtomCategory>) expr, join);
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

    private String generateAggregateAnd(Conjunction<AtomCategory> expr, String join) {
        return MessageFormat.format(
                "(SELECT term{2}.EntryId FROM\n" +
                "({0}) term{2}\n" +
                "INNER JOIN\n" +
                "({1}) term{3}\n" +
                "ON term{2}.EntryId = term{3}.EntryId)",
                generateAggregate(expr.getLhs(), join),
                generateAggregate(expr.getRhs(), join),
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

    private String generateAggregateOr(Disjunction<AtomCategory> expr, String join) {
        return MessageFormat.format(
                "( {0} UNION {1} )",
                generateAggregate(expr.getLhs(), join),
                generateAggregate(expr.getRhs(), join)
        );
    }

    private String generateAtom(BooleanTerm<AtomCategory> expr) {
        return MessageFormat.format(
                "( SELECT EntryStoreId FROM EntryCategory WHERE Scheme = ''{0}'' and Term = ''{1}'' )",
                expr.getValue().getScheme(),
                expr.getValue().getTerm()
        );
    }

    private String generateAggregateAtom(BooleanTerm<AtomCategory> expr, String join) {
        return MessageFormat.format(
                "SELECT DISTINCT j.Term AS EntryId" +
                "  FROM EntryCategory j JOIN EntryCategory c" +
                "    ON j.Scheme = ''{0}''" +
                "   AND c.Scheme = ''{1}''" +
                "   AND c.Term = ''{2}''" +
                "   AND j.EntryStoreId = c.EntryStoreId",
                join,
                expr.getValue().getScheme(),
                expr.getValue().getTerm()
        );
    }
}
