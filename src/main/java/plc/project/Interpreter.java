package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
        scope.defineFunction("log", 1, args -> {
            if (!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected Type BigDecimal, received " + args.get(0).getValue().getClass().getName() + ".");
            }

            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();

            BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });

        scope.defineFunction("converter", 2, args -> {
            // decimal in this example is a base 10 number
            BigInteger decimal = requireType(BigInteger.class, Environment.create(args.get(0).getValue()));
            BigInteger base = requireType(BigInteger.class, Environment.create(args.get(1).getValue()));

            String number = new String();


            return Environment.create(number);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for(Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
    }

    @Override

    public Environment.PlcObject visit(Ast.Global ast) {
        // Grabbing the optional value to see whether there is something assigned to the variable or if it's just a declaration
        Optional optional = ast.getValue();
        if (optional.isPresent()) {
            if (optional.get() instanceof Ast.Expression.PlcList) {
                // If the optional is of type plcList, then it's a list, and we initialize a list instead of a variable
                List<Ast.Expression> expressionList = ((Ast.Expression.PlcList) optional.get()).getValues();
                // for loop to iterate through each literal in the list and add to resultList;
                List<Object> resultList = new ArrayList<>();
                for (int i = 0; i < expressionList.size(); ++i) {
                    resultList.add(visit(expressionList.get(i)).getValue());
                }
                System.out.println();
                scope.defineVariable(ast.getName(), ast.getMutable(), Environment.create(resultList));
            }
            else {
                // If the optional has a value and isn't a plcList, that means there is something assigned to the variable
                Object value = visit((Ast.Expression.Literal) optional.get()).getValue();
                scope.defineVariable(ast.getName(), ast.getMutable(), Environment.create(value));
            }
        }
        else {
            // If optional is empty, the variable is merely being declared
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        // TIPS: use the args ->
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);
                for(int i = 0; i < args.size(); i++) { // define arguments
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                for(Ast.Statement stmt : ast.getStatements()) { // evaluate statements
                    visit(stmt);
                }
            }
            catch(Return r) {
                return r.value;
            }
            finally {
                //restore scope
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    // DONE
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional optional = ast.getValue();
        Boolean present = optional.isPresent();
        String name = ast.getName();

        if (present) {
            Ast.Expression expression = (Ast.Expression) optional.get();
            scope.defineVariable(name, true ,visit(expression));
        }
        else {
            scope.defineVariable(name, true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        // Ensuring that receiver is Ast.Expression.Access
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("receiver needs to be of type Ast.Expression.Access");
        }
        String receiverName = ((Ast.Expression.Access) ast.getReceiver()).getName();
        // if else statements to branch based on whether there is an offset given or not (accounting for list assignment)
        if (((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()) {
            // Grabbing the offset expression and then type casting it into a BigInteger
            Ast.Expression offsetExpression = ((Ast.Expression.Access) ast.getReceiver()).getOffset().get();
            BigInteger offset = (BigInteger) visit(offsetExpression).getValue();
            // Grabbing the Object array from scope and then type casting it into a list
            Object listObject = scope.lookupVariable(receiverName).getValue().getValue();
            List<Object> list = (List<Object>) listObject;
            // Checking that offset given isn't negative or out of range
            if (offset.intValue() < -1 || offset.intValue() > list.size()) {
                throw new RuntimeException("Given offset is negative or out of range");
            }
            // Updating the list[offset] value
            list.set(offset.intValue(), visit(ast.getValue()).getValue());
        }
        else {
            // if no offset is given, simply change the variable to whatever the user wants
            scope.lookupVariable(receiverName).setValue(visit(ast.getValue()));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        // Checking that the condition evaluates to a Boolean
        scope = new Scope(scope);
        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        // Condition is true
        if (condition) {
            // Evaluate the then statements
            List<Ast.Statement> thenStatements = ast.getThenStatements();
            for (int i = 0; i < thenStatements.size(); ++i) {
                visit(thenStatements.get(i));
            }
        }
        else {
            // Evaluate the else statements
            List<Ast.Statement> elseStatements = ast.getElseStatements();
            for (int i = 0; i < elseStatements.size(); ++i) {
                visit(elseStatements.get(i));
            }
        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    // TODO: Ask if I am using visit case correctly
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        scope = new Scope(scope);
        List<Ast.Statement.Case> cases = ast.getCases();
        Object condition = visit(ast.getCondition()).getValue();
        // Loop through the different cases to see if the condition matches any of the cases
        int index = 0;
        for (int i = 0; i < cases.size(); ++i) {
            // using visit case to fetch the caseCondition value to compare to switch condition to know what case to evaluate
            Object caseCondition = visit(cases.get(i)).getValue();
            // if the caseCondition and the condition matches, then evaluate the statements associated with that case
            if (caseCondition.equals(condition)) {
                index = i;
                break;
            }
            // if at the last condition, it's the default case so that means no other cases match, and we evaluate the default case
            if (i == cases.size() - 1) {
                index = i;
                break;
            }
        }
        List<Ast.Statement> statements = cases.get(index).getStatements();
        // Using for loop to evaluate each of the statements associated with the case that's being looked at
        for (int i = 0; i < statements.size(); ++i) {
            visit(statements.get(i));
        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        return visit(ast.getValue().get());
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        // throw new UnsupportedOperationException(); //TODO (in lecture)
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    // Literal = Done
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        // if the literal is null, then return NIL
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        // else return the actual literal from the ast
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return(visit(ast.getExpression()));
    }

    @Override
    // TODO: Check the Rounding for Multiplication and Exponents
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String astOP = ast.getOperator();
        // Evaluating for && and ||
        if (astOP.equals("&&") || astOP.equals("||")) {
            Boolean LHS = requireType(Boolean.class, visit(ast.getLeft()));
            // if right
            if (astOP.equals("||")) {
                // if the right side is an access, it means that it's undefined and just return the left side
                if (ast.getRight() instanceof Ast.Expression.Access) {
                    return Environment.create(LHS);
                }
                if (LHS) {
                    return Environment.create(true);
                }
                Boolean RHS = requireType(Boolean.class, visit(ast.getRight()));
                if (RHS) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            }
            else {
                // Evaluating the &&
                if (ast.getRight() instanceof Ast.Expression.Access) {
                    return Environment.create(false);
                }
                if (!LHS) {
                    // if the LHS is false in an &&, immediately return false
                    return Environment.create(false);
                }
                Boolean RHS = requireType(Boolean.class, visit(ast.getRight()));
                if (!RHS) {
                    // if LHS is true but RHS is false, return false
                    return Environment.create(false);
                }
                else {
                    // both LHS and RHS are true, return true
                    return Environment.create(true);
                }
            }
        }
        // Evaluating for > and <
        else if (astOP.equals("<") || astOP.equals(">")) {

            Comparable LHS = requireType(Comparable.class, visit(ast.getLeft()));
            // Making sure RHS has the same type as LHS
            Comparable RHS = requireType(LHS.getClass(), visit(ast.getRight()));
            // Using compare to see what compareTo returns; if < 0, LHS < RHS, else if > 0, LHS > RHS
            int compare = LHS.compareTo(RHS);
            if (astOP.equals("<")) {
                if (compare < 0) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            }
            else {
                if (compare > 0) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create(false);
                }
            }
        }
        // Evaluating == and !=
        else if (astOP.equals("==")) {
            if (ast.getLeft().equals(ast.getRight())) {
                return Environment.create(true);
            }
            else {
                return Environment.create(false);
            }
        }
        else if (astOP.equals("!=")) {
            if (ast.getLeft().equals(ast.getRight())) {
                return Environment.create(false);
            }
            else {
                return Environment.create(true);
            }
        }
        // Evaluating the '+'
        else if (astOP.equals("+")) {
            if ((visit(ast.getLeft()).getValue() instanceof String) || (visit(ast.getRight()).getValue() instanceof String)) {
                // if either side is a string then it's concat operation
                String LHS = requireType(String.class, visit(ast.getLeft()));
                String RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                String result = LHS.concat(RHS);
                return Environment.create(result);
            }
            else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                // if left is bigDecimal, then make sure that right is also same type
                BigDecimal LHS = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                BigDecimal result = LHS.add(RHS);
                return Environment.create(result);
            }
            else if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                // if left is bigInteger, then make sure that right is also same type
                BigInteger LHS = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                BigInteger result = LHS.add(RHS);
                return Environment.create(result);
            }
            // otherwise, throw an exception
            else {
                throw new RuntimeException("Left Side is neither of type String, BigInteger, nor Decimal");
            }
        }
        // Evaluating the '-' and '*'
        else if (astOP.equals("-") || astOP.equals("*")) {
            if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                // if left is bigDecimal, then make sure that right is also same type
                BigDecimal LHS = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                BigDecimal result;
                if (astOP.equals("-")) {
                    result = LHS.subtract(RHS);
                }
                 // if not - it's *
                else {
                    // Making sure the multiplication stays at tenths after decimal
                    result = LHS.multiply(RHS);
                }
                return Environment.create(result);
            }
            else if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                // if left is bigInteger, then make sure that right is also same type
                BigInteger LHS = requireType(BigInteger.class, visit(ast.getLeft()));
                BigInteger RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                BigInteger result;
                if (astOP.equals("-")) {
                    result = LHS.subtract(RHS);
                }
                // if not - it's *
                else {
                    result = LHS.multiply(RHS);
                }
                return Environment.create(result);
            }
            else {
                throw new RuntimeException("Left Hand Side is Neither of Type BigInteger nor BigDecimal");
            }
        }
        // Evaluating the '/'
        else if (astOP.equals("/")) {
            if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                BigDecimal LHS = requireType(BigDecimal.class, visit(ast.getLeft()));
                BigDecimal RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                if (RHS.signum() == 0) {
                    throw new RuntimeException("Denominator cannot be zero!");
                }
                BigDecimal result = LHS.divide(RHS, RoundingMode.HALF_EVEN);
                return Environment.create(result);
            }
            else if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                BigInteger LHS = requireType(BigInteger.class, visit(ast.getRight()));
                BigInteger RHS = requireType(LHS.getClass(), visit(ast.getRight()));
                if (RHS.signum() == 0) {
                    throw new RuntimeException("Denominator cannot be zero!");
                }
                BigInteger result = LHS.divide(RHS);
                return Environment.create(result);
            }
            else {
                throw new RuntimeException("Left Hand Side is not of type BigInteger nor BigDecimal");
            }
        }
        // Evaluating the ^
        else if (astOP.equals("^")) {
            // Making sure that the exponent is BigInteger
            if (visit(ast.getRight()).getValue() instanceof BigInteger) {
                BigInteger exponentCounter = BigInteger.ZERO;
                BigInteger RHS = requireType(BigInteger.class, visit(ast.getRight()));
                Boolean negativeExponent = false;
                // If exponent is negative, make it positive but then divide 1/result at the end
                if (RHS.intValue() < 0) {
                    RHS = RHS.multiply(BigInteger.valueOf(-1));
                    negativeExponent = true;
                }
                // Making sure that LHS is either BigInteger or BigDecimal
                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    BigInteger LHS = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger result = BigInteger.ZERO;
                    while (!exponentCounter.equals(RHS.add(BigInteger.ONE))) {
                        if (exponentCounter.equals(BigInteger.ZERO)) {
                            result = result.add(BigInteger.ONE);
                            exponentCounter = exponentCounter.add(BigInteger.ONE);
                            continue;
                        }
                        exponentCounter = exponentCounter.add(BigInteger.ONE);
                        result = result.multiply(LHS);
                        System.out.println();
                    }
                    // Accounting for negative exponents
                    if (negativeExponent) {
                        BigDecimal decimalResult = BigDecimal.ONE.divide(BigDecimal.valueOf(result.doubleValue()));
                        return Environment.create(decimalResult);
                    }
                    return Environment.create(result);
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    BigDecimal LHS = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal result = new BigDecimal(0);
                    while (!exponentCounter.equals(RHS.add(BigInteger.ONE))) {
                        if (exponentCounter.equals(BigInteger.ZERO)) {
                            result = result.add(BigDecimal.ONE);
                            exponentCounter = exponentCounter.add(BigInteger.ONE);
                            continue;
                        }
                        exponentCounter = exponentCounter.add(BigInteger.ONE);
                        result = result.multiply(LHS);
                    }
                    // Accounting for negative exponents
                    if (negativeExponent) {
                        result = BigDecimal.ONE.divide(result);
                        return Environment.create(result);
                    }
                    return Environment.create(result);
                }
                // else the LHS is not of the right type so throw an exception
                else {
                    throw new RuntimeException("LHS is not of type BigInteger or BigDecimal");
                }
            }
            else {
                throw new RuntimeException("Exponent is not of type BigInteger");
            }
        }
        else {
            return Environment.NIL;
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        // Checking if it's a list access
        Environment.Variable listVariable = scope.lookupVariable(ast.getName());

        if (ast.getOffset().isPresent()) {
            List<Object> list = (List<Object>) listVariable.getValue().getValue();
            Optional offsetOptional = ast.getOffset();
            BigInteger offset = (BigInteger) visit((Ast.Expression.Literal) offsetOptional.get()).getValue();
            // Throw Exception if given offset is - or if > size of list
            if ((offset.intValue() < 0) || (offset.intValue() >= list.size())) {
                throw new RuntimeException("Given offset is out of range");
            }

            return Environment.create(list.get(offset.intValue()));
        }
        return listVariable.getValue();
    }

    @Override
    // TODO: Ask if the function print is actually supposed to print out
    // TODO: Ask about the different potential functions we have to account for
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.PlcObject> argsObject = new ArrayList<>();
        // If the function doesn't have any arguments, just return the name of the function
        if (function.getArity() == 0) {
            return Environment.create(function.getName());
        }
        // else evaluate the arguments
        else {
            for (int i = 0; i < args.size(); ++i) {
                // Converting the args from Ast.Expressions to PlcObject by visiting and then adding to argsObject list
                argsObject.add(visit(args.get(i)));
            }
            // Invoking the list of object arguments
            function.invoke(argsObject);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> result = new ArrayList<>();
        // Iterate through the list and obtain the literal values
        for (int i = 0; i < ast.getValues().size(); ++i) {
            result.add(visit(ast.getValues().get(i)).getValue());
        }
        return Environment.create(result);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
