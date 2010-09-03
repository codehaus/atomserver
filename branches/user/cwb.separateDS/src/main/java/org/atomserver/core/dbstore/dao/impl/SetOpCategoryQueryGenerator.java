/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCategory;
import org.atomserver.utils.logic.BinaryOperator;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.BooleanTerm;
import org.atomserver.utils.logic.ExpressionType;

import java.util.Collection;

/**
 * SetOpCategoryQueryGenerator -- uses SQL Set operands (INTERSECT and UNION)
 * when constructing the SQL for Category queries.
 * <p/>
 * This produces SQL like that shown below;
 * <p/>
<pre>
 ===============
 ONE TERM

 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
 )

===============
TWO TERMS - AND

 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )

=====================
TWO TERMS - OR

 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     union
     select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
 )

=============
THREE TERMS - A && ( B || C )

 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         union all
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )

=============
THREE TERMS - A && B && C

 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         intersect
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )

=============
FOUR TERMS - (A || B) && (B || C)

 WHERE entry.EntryStoreId IN (
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.state' AND Y.Term = 'INTERVENE'
         union
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.state' AND Y.Term = 'MANUALLY_ACCEPTED'
     )
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         union
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )
</pre>
 */
public class SetOpCategoryQueryGenerator {
    private static final Log log = LogFactory.getLog(SetOpCategoryQueryGenerator.class);

    private final Collection<BooleanExpression<AtomCategory>> exprs;
    private int termCounter = 0;

    public static String generateCategorySearch(Collection<BooleanExpression<AtomCategory>> exprs) {
        return new SetOpCategoryQueryGenerator(exprs).generateCategorySearchSQL();
    }

    public SetOpCategoryQueryGenerator(Collection<BooleanExpression<AtomCategory>> exprs) {
        this.exprs = exprs;
    }

    String generateCategorySearchSQL() {
        StringBuilder builder = new StringBuilder();
        if ( containsOnlyTerms() ) {
            generateANDsOnly(builder);
        } else {
            for (BooleanExpression<AtomCategory> expr : exprs) {
                generateSQL(expr, builder) ;
            }
        }
        String sql = builder.toString();
        log.debug( "Generated SQL = \n" +  sql );
        return ( StringUtils.isEmpty(sql) ) ? "" : sql ;
    }

    private boolean containsOnlyTerms() {
        for (BooleanExpression<AtomCategory> expr : exprs) {
            if ( expr.getType() != ExpressionType.TERM ) return false;
        }
        return true;
    }

    private void generateANDsOnly(StringBuilder builder) {
        builder.append( "\n(" );
        int knt = 0;
        for (BooleanExpression<AtomCategory> expr : exprs) {
            generateTerm( (BooleanTerm<AtomCategory>)expr, builder );

            knt++;
            if ( knt < exprs.size() ){
                builder.append("\n INTERSECT ");
            }
        }          
        builder.append( "\n)" );
    }

    private void generateSQL(BooleanExpression<AtomCategory> expr, StringBuilder builder) {
        if ( expr.getType() == ExpressionType.TERM ) {
            generateTerm( (BooleanTerm<AtomCategory>)expr, builder );
        } else {
            generateExpr( (BinaryOperator<AtomCategory>)expr, builder );
        }
    }

    private void generateTerm( BooleanTerm<AtomCategory> term, StringBuilder builder ) {
        String alias = "X" + termCounter;
        termCounter++;

        builder.append( "\nSELECT ").append(alias).append(".EntryStoreId FROM EntryCategory ").append(alias);
        builder.append( " WHERE ").append(alias).append(".Scheme =" );
        builder.append( " '" ).append( term.getValue().getScheme() ).append( "'" );
        builder.append( " AND ").append(alias).append(".Term =");
        builder.append( " '" ).append( term.getValue().getTerm() ).append( "'" );
    }

    private void generateExpr(BinaryOperator<AtomCategory> operator, StringBuilder builder) {
        builder.append( "\n(" );
        generateSQL( operator.getLhs(), builder );
        generateSetOp( operator, builder );
        generateSQL( operator.getRhs(), builder );
        builder.append( "\n)" );
    }

    private void generateSetOp(BinaryOperator<AtomCategory> operator, StringBuilder builder) {
        switch( operator.getRawType() ) {
        case AND :
            builder.append("\n INTERSECT ");
            break;
        case OR :
            builder.append("\n UNION ");
            break;
        default :
            throw new IllegalArgumentException("Invalid Operator: " + operator.getRawType() );
        }
    }

}
