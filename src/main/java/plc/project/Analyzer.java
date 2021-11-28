package plc.project;

import jdk.nashorn.internal.codegen.types.Type;
import org.omg.SendingContext.RunTime;

import java.beans.Expression;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        // Visiting globals
        for (Ast.Global global: ast.getGlobals()) {
            visit(global);
        }
        // Visiting functions
        for (Ast.Function function: ast.getFunctions()) {
            visit(function);
        }
        requireAssignable(Environment.Type.INTEGER ,scope.lookupFunction("main", 0).getReturnType());

        // returning null;
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        // if the value of the global is present, visit it
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }
        if (!ast.getTypeName().isEmpty()) {
            String astType = ast.getTypeName();
            scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(astType), ast.getMutable(), Environment.NIL);

        }
        ast.setVariable(scope.lookupVariable(ast.getName()));
        // if the value of the global is present, check that its type is assignable to the global type
        if (ast.getValue().isPresent()) {
            requireAssignable(ast.getVariable().getType(), ast.getValue().get().getType());
        }
        // returns null
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        // Loop through the type names and make a list of corresponding Environment.Types
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < ast.getParameterTypeNames().size(); ++i) {
            String parameterType = ast.getParameterTypeNames().get(i);
            parameterTypes.add(Environment.getType(parameterType));
        }
        // if the return type is not given, it will be Nil
        Environment.Type returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        // Defining the function in scope
        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL);
        // Defining the function in ast
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        try {
            // Loop through all the statements, one of them will be return statements
            function = ast;
            scope = new Scope(scope);
            for (int i = 0; i < ast.getParameters().size(); ++i) {
                scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), true, Environment.NIL);
            }
            // Visiting all the function's statements inside a new scope containing variables for each parameter
            for (Ast.Statement stmt : ast.getStatements()) {
                visit (stmt);
            }
        }
        finally {
            function = null;
            scope = scope.getParent();
        }
        // returns null
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        // throws a runtime exception if the expression is not ast.expression.function
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("The expression is not an Ast.Expression.Function");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()) {
            // if the value of the declared variable is present, then visit it
            visit (ast.getValue().get());
        }
        if (ast.getTypeName().isPresent()) {
            String astType = ast.getTypeName().get();
            scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(astType), true, Environment.NIL);
        }
        else {
            // if there is no type present in the ast, then check for the value's type
            if (ast.getValue().isPresent()) {
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType() , true, Environment.NIL);
            }
        }
        // check that the value is assignable to the variable
        if (ast.getValue().isPresent()) {
            requireAssignable(scope.lookupVariable(ast.getName()).getType(), ast.getValue().get().getType());
        }
        // setting the ast variable
        ast.setVariable(scope.lookupVariable(ast.getName()));
        // Returning null
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // throw exception if the receiver is not access expression
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver is not an access expression");
        }
        visit(ast.getReceiver());
        // Checking whether the value is assignable to the receiver
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        // returning null
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        // Checking that the condition is of type Boolean
        visit(ast.getCondition());
        if (!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN))) {
            // if not equal boolean, throw Runtime Exception
            throw new RuntimeException("The condition if not of type Boolean");
        }
        // Check that the thenStatements list is not empty
        if (ast.getThenStatements().size() == 0) {
            // if it is empty, throw new exception
            throw new RuntimeException("List of thenStatements is empty!");
        }
        // visiting then statements inside a new scope for each one
        for (Ast.Statement stmt : ast.getThenStatements()) {
            scope = new Scope(scope);
            visit (stmt);
            scope = scope.getParent();
        }
        // visiting else statements inside a new scope for each one
        for (Ast.Statement stmt : ast.getElseStatements()) {
            scope = new Scope(scope);
            visit(stmt);
            scope = scope.getParent();
        }
        // returning null
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        // After visiting the Condition, visit each case inside a new scope for each case
        for (int i = 0; i < ast.getCases().size(); ++i) {
            // making sure that the final case statement has an empty value
            if (i == ast.getCases().size() - 1) {
                if (ast.getCases().get(i).getValue().isPresent()) {
                    throw new RuntimeException("Last case value is not empty");
                }
            }
            scope = new Scope(scope);
            visit (ast.getCases().get(i));
            // making sure that the condition type matches up with the case value type
            if (ast.getCases().get(i).getValue().isPresent()) {
                Environment.Type conditionType = ast.getCondition().getType();
                Environment.Type currentCaseType = ast.getCases().get(i).getValue().get().getType();
                requireAssignable(conditionType, currentCaseType);
            }
            scope = scope.getParent();
        }
        // returning null
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }
        // visit each statement of the case
        for (Ast.Statement stmt: ast.getStatements()) {
            visit (stmt);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit (stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(function.getFunction().getReturnType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral().equals(Environment.NIL)) {
           ast.setType(Environment.Type.NIL);
        }
        else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        // if the ast literal is a BigInteger, check that it isn't too big to be a Java int
        else if (ast.getLiteral() instanceof BigInteger) {
            // if the literal is larger than the max value of int, then throw exception
            if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("Literal is out of range of java int");
            }
            // else if the literal is smaller than the smallest value of int
            else if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Literal is out of range of java int");
            }
            else {
                ast.setType(Environment.Type.INTEGER);
            }
        }
        // else if the ast literal is a BigDecimal, make sure it isn't out of range of a Java double
        else if (ast.getLiteral() instanceof BigDecimal) {
            // check if greater than
            if (((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0) {
                throw new RuntimeException("Literal is out of range of java double");
            }
            // check if less than the smallest double
            else if (((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0) {
                throw new RuntimeException("Literal is out of range of java double");
            }
            else {
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        // Validating a group expression
        visit(ast.getExpression());
        // if the expression type is not Binary, throw an exception
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("The Group Expression is not a Binary Expression!");
        }
        // Setting the expression type to be the type of the contained expression
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // visit the left and right side to get them to have their matching types
        visit(ast.getLeft());
        visit(ast.getRight());
        // && and ||
        if (ast.getOperator().equals("&&") || ast.getOperator().equals("||")) {
            // check that both operands must be a boolean
            if (ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN)) {
                // then the result is also a boolean
                ast.setType(Environment.Type.BOOLEAN);
            }
            // else throw an exception
            else {
                throw new RuntimeException("Left Side and Right Side are not both Booleans");
            }
        }
        // < > == !=
        else if (ast.getOperator().equals(">") || ast.getOperator().equals("==") || ast.getOperator().equals("!=") || ast.getOperator().equals("<")) {
            // Check that both operands must be Comparable
            if ((!ast.getLeft().getType().equals(Environment.Type.COMPARABLE)) || (!ast.getRight().getType().equals(Environment.Type.COMPARABLE))) {
                // if either side is not a comparable, throw the exception
                throw new RuntimeException("Both operands are not of type Comparable");
            }
            // else all the conditions are met and the result is a Boolean
            ast.setType(Environment.Type.BOOLEAN);
        }
        // String concat or addition +
        else if (ast.getOperator().equals("+")) {
            // If either side of the binary operation is a String, then the result will be a String, and the other side can be anything
            if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            }
            // Otherwise, LHS must be Integer/Decimal and Both the RHS and Result will match the type
            else if (ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                // Checking that the right side is of type Integer
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                // The if both sides are integers, then the result will also be an integer
                ast.setType(Environment.Type.INTEGER);
            }
            else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                // Checking that the right side is also of type Decimal
                requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                // if both types matches up, then the result will also be a Decimal
                ast.setType(Environment.Type.DECIMAL);
            }
            // else, throw a runtime exception for having the wrong types
            else {
                throw new RuntimeException("Unsupported Binary Types");
            }
        }
        // - * /
        else if (ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")) {
            // LHS must be Integer or Decimal, and RHS and result will be the same as the LHS
            if (ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Invalid required operand types");
            }
        }
        // ^
        else if (ast.getOperator().equals("^")) {
            // Checking to make sure that the RHS is an integer
            requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
            // LHS must be Integer or Decimal, the result will the same type as the LHS
            if (ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
            }
            else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Invalid required operand types");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        // Checking whether the offset is there
        if (ast.getOffset().isPresent()) {
            // if there is an offset, make sure that its type is an Integer, otherwise throw exception
            visit(ast.getOffset().get());
            if (!ast.getOffset().get().getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset is not an Integer");
            }
        }
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        List<Ast.Expression> list = ast.getArguments();
        for (int i = 0; i < list.size(); ++i) {
            // visiting all the arguments of the functions
            visit(list.get(i));
            requireAssignable(ast.getFunction().getParameterTypes().get(i), list.get(i).getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for (int i = 0; i < ast.getValues().size(); ++i) {
            visit(ast.getValues().get(i));
        }
        // grabbing the type of the first element of the list to make sure that every element of the list is the same
        Environment.Type listType = ast.getValues().get(0).getType();
        // validating the list by iterating through and visiting each element
        for (int i = 0; i < ast.getValues().size(); ++i) {
            // checking that the expression is assignable to the list type
            requireAssignable(listType, ast.getValues().get(i).getType());
        }
        ast.setType(listType);
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        String targetName = target.getName();
        String typeName = type.getName();
        // if the two types don't match up, then check the Comparable and Any requirements
        if (!targetName.equals(typeName)) {
            // if the LHS is not an Any, then check if it's a Comparable
            if (!targetName.equals("Any")) {
                // If the LHS is a comparable, then check if the RHS is one of the excepted comparable types
                if (targetName.equals("Comparable")) {
                    // Checking whether it is of type Integer, Decimal, Character, or String
                    if (!(typeName.equals("Integer") || typeName.equals("Decimal") || typeName.equals("Character") || typeName.equals("String"))) {
                        throw new RuntimeException("Type and target type do not match!");
                    }
                }
                // else throw an exception
                else {
                    throw new RuntimeException("Type and target type do not match!");
                }
            }
        }
    }

}
