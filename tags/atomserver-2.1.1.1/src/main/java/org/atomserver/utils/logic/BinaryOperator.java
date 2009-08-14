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

import java.util.Set;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BinaryOperator<T> implements BooleanExpression<T> {
    private ExpressionType type;
    private BooleanExpression<T> lhs;
    private BooleanExpression<T> rhs;

    protected BinaryOperator(ExpressionType type, BooleanExpression<T> lhs, BooleanExpression<T> rhs) {
        this.type = type;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public BooleanExpression<T> getLhs() {
        return lhs;
    }

    public BooleanExpression<T> getRhs() {
        return rhs;
    }

    public ExpressionType getType() {
        switch (lhs.getType()) {
        case MIXED:
            return ExpressionType.MIXED;
        case AND:
            switch (rhs.getType()) {
            case MIXED:
            case OR:
                return ExpressionType.MIXED;
            case AND:
            case TERM:
                return (type == ExpressionType.AND ? ExpressionType.AND : ExpressionType.MIXED);
            }
        case OR:
            switch (rhs.getType()) {
            case MIXED:
            case AND:
                return ExpressionType.MIXED;
            case OR:
            case TERM:
                return (type == ExpressionType.OR ? ExpressionType.OR : ExpressionType.MIXED);
            }
        case TERM:
            switch (rhs.getType()) {
            case MIXED:
                return ExpressionType.MIXED;
            case AND:
                return (type == ExpressionType.AND ? ExpressionType.AND : ExpressionType.MIXED);
            case OR:
                return (type == ExpressionType.OR ? ExpressionType.OR : ExpressionType.MIXED);
            case TERM:
                return type;
            }
        default:
            return type;
        }
    }

    public void buildTermSet(Set<BooleanTerm<? extends T>> terms) {
        lhs.buildTermSet(terms);
        rhs.buildTermSet(terms);
    }
}
