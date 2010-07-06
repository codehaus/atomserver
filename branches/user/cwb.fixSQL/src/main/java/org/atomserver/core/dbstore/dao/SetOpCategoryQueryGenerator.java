/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

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
 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.Workspace AS Workspace,
 entry.Collection AS Collection,
 entry.LanCode AS LanCode,
 entry.CountryCode AS CountryCode,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (

     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         union
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp




ZERO TERMS

SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp


 ONE TERM

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp

===============
TWO TERMS - AND

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp

=====================
TWO TERMS - OR

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     union
     select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp


=============
THREE TERMS - A && B || C

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         union
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp


=============
THREE TERMS - A && B && C

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
 WHERE entry.EntryStoreId IN (
     select X.EntryStoreId from EntryCategory X where X.Scheme = 'urn:inquiry.state' AND X.Term = 'INTERVENE'
     intersect
     (
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '3'
         intersect
         select Y.EntryStoreId from EntryCategory Y where Y.Scheme = 'urn:inquiry.stripes' AND Y.Term = '2'
     )
 )
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp


=============
FOUR TERMS - (A || B) && (B || C)

 SELECT
 TOP 21
 entry.EntryStoreId AS EntryStoreId,
 entry.EntryId AS EntryId,
 entry.UpdateDate AS UpdateDate,
 entry.CreateDate AS CreateDate,
 CAST(UpdateTimestamp AS BIGINT) AS UpdateTimestamp,
 entry.DeleteFlag AS DeleteFlag,
 entry.RevisionNum AS RevisionNum
 FROM EntryStore entry
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
 AND UpdateTimestamp > CAST(0 AS  TIMESTAMP  )
 AND ( DATEADD(s, -1 * 60, GETDATE()) >= UpdateDate )
 AND Workspace = 'inquiries'
 AND Collection='homeaway'
 ORDER BY entry.UpdateTimestamp


 *
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
        // TODO -- fix for multiple ANDs
        // TODO -- fix for single term
        for (BooleanExpression<AtomCategory> expr : exprs) {
            generate(expr, builder) ;
        }
        String sql = builder.toString();
        log.debug( "Generated SQL = \n" +  sql );
        return ( StringUtils.isEmpty(sql) ) ? "" : sql ;
    }

    private void generate(BooleanExpression<AtomCategory> expr, StringBuilder builder) {
        if ( expr.getType() == ExpressionType.TERM ) {
            generateTerm( (BooleanTerm<AtomCategory>)expr, builder );
        } else {
            generateExpr( (BinaryOperator<AtomCategory>)expr, builder );
        }
    }

    private void generateTerm( BooleanTerm<AtomCategory> term, StringBuilder builder ) {
        String alias = "X" + termCounter;
        termCounter++;

        builder.append( "\nSELECT ").append(alias).append(".EntryStoreId from EntryCategory ").append(alias);
        builder.append( " WHERE ").append(alias).append(".Scheme =" );
        builder.append( " '" ).append( term.getValue().getScheme() ).append( "'" );
        builder.append( " AND ").append(alias).append(".Term =");
        builder.append( " '" ).append( term.getValue().getTerm() ).append( "'" );
    }

    private void generateExpr(BinaryOperator<AtomCategory> operator, StringBuilder builder) {
        builder.append( "\n(" );
        generate( operator.getLhs(), builder );
        generateSetOp( operator, builder );
        generate( operator.getRhs(), builder );
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
