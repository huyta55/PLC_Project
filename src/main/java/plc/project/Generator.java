package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main {"

        // Declare globals -> properties

        // Declare "public static void main(String[] args) {
        //              System.exit(new Main().main());
        //          }"

        // Declare each of our functions -> methods
        // One of our functions -> methods is called main()!
        // print "}" to close the class Main
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

}
