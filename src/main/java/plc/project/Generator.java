package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    // TODO: Remove the sout after writer.write before submitting!
    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
                // System.out.print(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        // System.out.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
            // System.out.print("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main {"
        print("public class Main {");
        newline(indent);
        newline(++indent);
        // Declare globals -> properties
        for (int i = 0; i < ast.getGlobals().size(); ++i) {
            print(ast.getGlobals().get(i));
            if (i < ast.getGlobals().size() - 1) {
                newline(indent);
            }
            else {
                newline(indent--);
            }
        }

        // Declare "public static void main(String[] args) {
        //              System.exit(new Main().main());
        //          }"
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent);
        newline(++indent);

        // Declare each of our functions -> methods
        // One of our functions -> methods is called main()!
        for (int i = 0; i < ast.getFunctions().size(); ++i) {
            print(ast.getFunctions().get(i));
            if (i < ast.getFunctions().size() - 1) {
                newline(indent);
            }
            else {
                newline(--indent);
            }
        }
        newline(indent);
        // print "}" to close the class Main
        print ("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        // Mutable
        if (ast.getValue().get() instanceof Ast.Expression.PlcList) {
            print(ast.getVariable().getType().getJvmName(), "[] ", ast.getName());
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
            print(";");
        }
        else if (ast.getMutable()) {
            print(ast.getVariable().getType().getJvmName(), " ", ast.getName());
            // if value is present, then = value
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
            print(";");
        }
        // Immutable
        else {
            print("final ", ast.getVariable().getType().getJvmName(), " ", ast.getName());
            // if value is present, then = value
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
            print(";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); ++i) {
            print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " ");
            print(ast.getParameters().get(i));
            // if not last parameter, print comma and space
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        print (") {");
        // if statements are empty then print the closing brace on the same line
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); ++i) {
                print (ast.getStatements().get(i));
                // if not last statement, then newline w/o subtracting indent
                if (i < ast.getStatements().size() - 1) {
                    newline(indent);
                }
                else {
                    newline(--indent);
                }
            }
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // printing the expression found in ast followed by a semicolon
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    // Done during lecture
    public Void visit(Ast.Statement.Declaration ast) {
        // write: TYPE variable_name
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        // is there an assigned value?
        if (ast.getValue().isPresent()) {
            // if so, write: = and the value
            print(" = ", ast.getValue().get());
        }
        // write: ;
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        // printing if
        print("if (", ast.getCondition(), ") {");
        newline(++indent);
        // print out all the then statements
        for (int i = 0; i < ast.getThenStatements().size(); ++i) {
            print(ast.getThenStatements().get(i));
            // if not last statement, then newline
            if (i < ast.getThenStatements().size() - 1) {
                newline(indent);
            }
        }
        newline(--indent);
        print("}");
        // if there is an else statement then print out the statements
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); ++i) {
                print(ast.getElseStatements().get(i));
                // if not last statement, then newline
                if (i < ast.getElseStatements().size() - 1) {
                    newline(indent);
                }
            }
            newline(--indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        // printing switch keyword
        print("switch (", ast.getCondition(), ") {");
        newline(++indent);
        for (int i = 0; i < ast.getCases().size(); ++i) {
            print(ast.getCases().get(i));
            // Accounting for the indenting issue after the default case
            if (i < ast.getCases().size() - 1) {
                newline(--indent);
            }
            else {
                indent -= 2;
                newline(indent);
            }
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        // printing case keyword
        if (ast.getValue().isPresent()) {
            // if the ast has a value, then it's not the default case
            print("case ", ast.getValue().get(), ":");
        }
        else {
            // else it's the default case
            print("default:");
        }
        newline(++indent);
        // printing all the case statements
        for (int i = 0; i < ast.getStatements().size(); ++i) {
            print(ast.getStatements().get(i));
            // if not last statement, then newline
            if (i < ast.getStatements().size() - 1) {
                newline(indent);
            }
        }

        return null;
    }

    @Override
    // Done during lecture
    public Void visit(Ast.Statement.While ast) {
        // print the while structure, including condition
        print("while (", ast.getCondition(),") {");
        // determine if there are statements to process
        if (!ast.getStatements().isEmpty()) {
            // set up the next line
            newline(++indent);
            // handle all statements in the while statement body
            for (int i = 0; i < ast.getStatements().size(); i++) {
                // check if newline and indent are needed
                if (i != 0) {
                    // set up the next line
                    newline(indent);
                }
                // print the next statement
                print(ast.getStatements().get(i));
            }
            // set up the next line
            newline(--indent);
        }
        // close the while
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        // Printing return
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        // Checking if the ast contains a string or a character, since those needs to be printed with "
        if (ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
            return null;
        }
        if (ast.getType().equals(Environment.Type.BOOLEAN)) {
            print(ast.getLiteral());
            return null;
        }
        if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("'", ast.getLiteral(), "'");
            return null;
        }
        // Printing the literal value found in the AST
        print(ast.getLiteral());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // Check the Binary Operator
        switch (ast.getOperator()) {
            case "&&":
                // Generate the AST left expression
                print(ast.getLeft());
                print(" && ");
                // Generate the AST right expression
                print(ast.getRight());
                break;
            case "||":
                print(ast.getLeft());
                print(" || ");
                // Generate the AST right expression
                print(ast.getRight());
                break;
            case "^":
                print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");
                break;
            default:
                print(ast.getLeft(), " ", ast.getOperator(), " ", ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        // if an offset is present, then a list is being accessed
        if (ast.getOffset().isPresent()) {
            print(ast.getVariable().getJvmName(), "[", ast.getOffset().get(), "]");
        }
        else {
            // else just print the JVM name of the variable stored in the AST
            print (ast.getVariable().getJvmName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // printing function name
        print(ast.getFunction().getJvmName());
        // printing a comma separated list of generated argument expressions surrounded by parenthesis
        print("(");
        for (int i = 0; i < ast.getArguments().size(); ++i) {
            // printing the current argument
            print(ast.getArguments().get(i));
            // if it's not the last argument, print a comma and space
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }
    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        // print brace
        print ("{");
        for (int i = 0; i < ast.getValues().size(); ++i) {
            // printing the current list value
            print(ast.getValues().get(i));
            // if not the last value, print comma and space
            if (i < ast.getValues().size() - 1) {
                print(", ");
            }
        }
        print("}");
        return null;
    }

}
