package org.talend.tqldsel.tqltodsel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.maplang.el.parser.model.ELNode;
import org.talend.maplang.el.parser.model.ELNodeType;
import org.talend.tql.excp.TqlException;
import org.talend.tql.model.AllFields;
import org.talend.tql.model.AndExpression;
import org.talend.tql.model.ComparisonExpression;
import org.talend.tql.model.ComparisonOperator;
import org.talend.tql.model.Expression;
import org.talend.tql.model.FieldBetweenExpression;
import org.talend.tql.model.FieldCompliesPattern;
import org.talend.tql.model.FieldContainsExpression;
import org.talend.tql.model.FieldInExpression;
import org.talend.tql.model.FieldIsEmptyExpression;
import org.talend.tql.model.FieldIsInvalidExpression;
import org.talend.tql.model.FieldIsNullExpression;
import org.talend.tql.model.FieldIsValidExpression;
import org.talend.tql.model.FieldMatchesRegex;
import org.talend.tql.model.FieldReference;
import org.talend.tql.model.FieldWordCompliesPattern;
import org.talend.tql.model.LiteralValue;
import org.talend.tql.model.NotExpression;
import org.talend.tql.model.OrExpression;
import org.talend.tql.model.TqlElement;
import org.talend.tql.visitor.IASTVisitor;

/**
 * Abstract TQL to DSEL visitor
 */
abstract class AbstractTqlToDselVisitor implements IASTVisitor<ELNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTqlToDselVisitor.class);

    /**
     * Mapping from the field names to their corresponding type. Type can be a native type or a semantic type.
     */
    protected final Map<String, String> fieldToType;

    /**
     *
     * @param fieldToType a Map object used to get a type (native or semantic type) from a field name, this is a lightweight
     * representation of the schema
     */
    public AbstractTqlToDselVisitor(Map<String, String> fieldToType) {
        this.fieldToType = fieldToType;
    }

    @Override
    public ELNode visit(TqlElement elt) {
        throw new TqlException("Unsupported operation : visit(TqlElement elt)");
    }

    @Override
    public ELNode visit(ComparisonOperator elt) {
        LOGGER.debug("Inside Visit ComparisonOperator " + elt.toString());
        switch (elt.getOperator()) {
        case EQ:
            return new ELNode(ELNodeType.EQUAL, "==");
        case NEQ:
            return new ELNode(ELNodeType.NOT_EQUAL, "!=");
        case LT:
            return new ELNode(ELNodeType.LOWER_THAN, "<");
        case GT:
            return new ELNode(ELNodeType.GREATER_THAN, ">");
        case LET:
            return new ELNode(ELNodeType.LOWER_OR_EQUAL, "<=");
        case GET:
            return new ELNode(ELNodeType.GREATER_OR_EQUAL, ">=");
        default:
            throw new TqlException("Comparison operator " + elt.getOperator() + " not available in TQL");
        }
    }

    @Override
    public ELNode visit(LiteralValue elt) {
        LOGGER.debug("Inside Visit LiteralValue " + elt.toString());
        switch (elt.getLiteral()) {
        case INT:
            return new ELNode(ELNodeType.INTEGER_LITERAL, elt.getValue());
        case BOOLEAN:
            return new ELNode(ELNodeType.BOOLEAN_LITERAL, elt.getValue());
        case DECIMAL:
            return new ELNode(ELNodeType.DECIMAL_LITERAL, elt.getValue());
        case QUOTED_VALUE:
            return new ELNode(ELNodeType.STRING_LITERAL, elt.toQueryString());
        default:
            throw new TqlException("Literal value type " + elt.getLiteral() + " not available in TQL");
        }
    }

    @Override
    public ELNode visit(FieldReference elt) {
        LOGGER.debug("Inside Visit FieldReference " + elt.toString());
        return new ELNode(ELNodeType.HPATH, elt.getPath());
    }

    @Override
    public ELNode visit(Expression elt) {
        throw new TqlException("Unsupported operation : visit(Expession elt)");
    }

    @Override
    public ELNode visit(AndExpression elt) {
        LOGGER.debug("Inside Visit AndExpression " + elt.toString());
        final Expression[] expressions = elt.getExpressions();

        if (expressions.length == 0) {
            throw new TqlException("DSEL \"AND\" expression can't have zero sub-expressions");
        } else if (expressions.length == 1) {
            return expressions[0].accept(this);
        } else {
            ELNode andNode = new ELNode(ELNodeType.AND, "&&");
            for (Expression ex : expressions) {
                andNode.addChild(ex.accept(this));
            }
            return andNode;
        }
    }

    @Override
    public ELNode visit(OrExpression elt) {
        LOGGER.debug("Inside Visit OrExpression " + elt.toString());
        final Expression[] expressions = elt.getExpressions();

        if (expressions.length == 0) {
            throw new TqlException("DSEL \"OR\" expression can't have zero sub-expressions");
        } else if (expressions.length == 1) {
            return expressions[0].accept(this);
        } else {
            ELNode orNode = new ELNode(ELNodeType.OR, "||");
            for (Expression ex : expressions) {
                orNode.addChild(ex.accept(this));
            }
            return orNode;
        }
    }

    @Override
    public ELNode visit(ComparisonExpression elt) {
        LOGGER.debug("Inside Visit ComparisonExpression " + elt.toString());
        TqlElement field = elt.getField();
        ComparisonOperator operator = elt.getOperator();
        TqlElement valueOrField = elt.getValueOrField();
        ELNode fieldNode = field.accept(this);
        ELNode opNode = operator.accept(this);
        ELNode valueNode = valueOrField.accept(this);

        opNode.addChild(fieldNode);
        opNode.addChild(valueNode);

        return opNode;
    }

    @Override
    public ELNode visit(FieldInExpression elt) {
        LOGGER.debug("Inside Visit FieldInExpression " + elt.toString());

        List<ELNode> valueNodes = Arrays.stream(elt.getValues()).map(value -> value.accept(this)).collect(Collectors.toList());

        ELNode inNode = new ELNode(ELNodeType.FUNCTION_CALL, "in");
        inNode.addChild(elt.getField().accept(this));
        inNode.addChildren(valueNodes);

        return inNode;
    }

    @Override
    public abstract ELNode visit(FieldIsEmptyExpression elt);

    @Override
    public ELNode visit(FieldIsValidExpression elt) {
        LOGGER.debug("Inside Visit FieldIsValidExpression " + elt.toString());
        final TqlElement ex = elt.getField();

        ELNode isValidNode = new ELNode(ELNodeType.FUNCTION_CALL, "isValid");
        final ELNode node = ex.accept(this);
        isValidNode.addChild(node);
        final String validFieldType = fieldToType.get(node.getImage());
        if (validFieldType == null) {
            throw new TqlException(String.format("Cannot find the type of the field '%s'", node.getImage()));
        }
        isValidNode.addChild(new ELNode(ELNodeType.STRING_LITERAL, validFieldType));

        return isValidNode;
    }

    @Override
    public abstract ELNode visit(FieldIsInvalidExpression elt);

    @Override
    public ELNode visit(FieldIsNullExpression elt) {
        LOGGER.debug("Inside Visit FieldIsNullExpression " + elt.toString());
        final TqlElement ex = elt.getField();

        ELNode notNode = new ELNode(ELNodeType.FUNCTION_CALL,
                org.talend.maplang.el.interpreter.impl.function.builtin.IsNull.NAME);
        notNode.addChild(ex.accept(this));

        return notNode;
    }

    @Override
    public ELNode visit(FieldMatchesRegex elt) {
        LOGGER.debug("Inside Visit FieldMatchesRegex " + elt.toString());
        final TqlElement ex = elt.getField();

        ELNode regexNode = new ELNode(ELNodeType.FUNCTION_CALL,
                org.talend.maplang.el.interpreter.impl.function.builtin.Matches.NAME);
        regexNode.addChild(ex.accept(this));
        regexNode.addChild(new ELNode(ELNodeType.STRING_LITERAL, "'" + elt.getRegex() + "'"));

        return regexNode;
    }

    @Override
    public ELNode visit(FieldCompliesPattern elt) {
        LOGGER.debug("Inside Visit FieldCompliesPattern " + elt.toString());
        final TqlElement ex = elt.getField();

        ELNode fieldCompliesNode = new ELNode(ELNodeType.FUNCTION_CALL, "complies");
        fieldCompliesNode.addChild(ex.accept(this));
        fieldCompliesNode.addChild(new ELNode(ELNodeType.STRING_LITERAL, "'" + elt.getPattern() + "'"));

        return fieldCompliesNode;
    }

    @Override
    public ELNode visit(FieldWordCompliesPattern elt) {
        LOGGER.debug("Inside Visit FieldWordCompliesPattern " + elt.toString());
        final TqlElement ex = elt.getField();

        ELNode fieldWordCompliesNode = new ELNode(ELNodeType.FUNCTION_CALL, "wordComplies");
        fieldWordCompliesNode.addChild(ex.accept(this));
        fieldWordCompliesNode.addChild(new ELNode(ELNodeType.STRING_LITERAL, "'" + elt.getPattern() + "'"));

        return fieldWordCompliesNode;
    }

    @Override
    public ELNode visit(FieldBetweenExpression elt) {
        LOGGER.debug("Inside Visit FieldBetweenExpression " + elt.toString());
        final TqlElement ex = elt.getField();
        ELNode fieldBetweenNode = new ELNode(ELNodeType.FUNCTION_CALL,
                org.talend.maplang.el.interpreter.impl.function.builtin.Between.NAME);

        fieldBetweenNode.addChild(ex.accept(this));
        fieldBetweenNode.addChild(visit(elt.getLeft()));
        fieldBetweenNode.addChild(visit(elt.getRight()));

        return fieldBetweenNode;
    }

    @Override
    public ELNode visit(NotExpression elt) {
        LOGGER.debug("Inside Visit NotExpression " + elt.toString());
        final Expression ex = elt.getExpression();

        ELNode notNode = new ELNode(ELNodeType.NOT, "!");
        notNode.addChild(ex.accept(this));

        return notNode;
    }

    @Override
    public ELNode visit(FieldContainsExpression elt) {
        LOGGER.debug("Inside Visit FieldContainsExpression " + elt.toString());
        TqlElement ex = elt.getField();
        String expressionValue = elt.getValue();

        if (ex == null || expressionValue == null) {
            throw new TqlException("DSEL \"Contains\" expression should have two arguments");
        } else {
            ELNode containsNode = new ELNode(ELNodeType.FUNCTION_CALL,
                    org.talend.maplang.el.interpreter.impl.function.builtin.Contains.NAME);
            containsNode.addChild(ex.accept(this));
            containsNode.addChild(new ELNode(ELNodeType.STRING_LITERAL, "'" + expressionValue + "'"));
            if (!elt.isCaseSensitive()) {
                containsNode.addChild(new ELNode(ELNodeType.BOOLEAN_LITERAL, "false"));
            }
            return containsNode;
        }
    }

    @Override
    public ELNode visit(AllFields allFields) {
        throw new TqlException("Unsupported operation : visit(AllFields elt)");
    }
}
