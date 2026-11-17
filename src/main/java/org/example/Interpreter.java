package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();

    private final Map<Expr, Integer> locals = new HashMap<>();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new NexusCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("len", new NexusCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                Object arg = arguments.get(0);
                if (arg instanceof List) {
                    return (double)((List<Object>)arg).size();
                }
                if (arg instanceof String) {
                    return (double)((String)arg).length();
                }
                throw new RuntimeError(null,
                        "Argument to 'len' must be a list or string.");
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("append", new NexusCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                Object arg = arguments.get(0);
                if (!(arg instanceof List)) {
                    throw new RuntimeError(null,
                            "First argument to 'append' must be a list.");
                }
                ((List<Object>)arg).add(arguments.get(1));
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("remove", new NexusCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                Object arg = arguments.get(0);
                if (!(arg instanceof List)) {
                    throw new RuntimeError(null,
                            "First argument to 'remove' must be a list.");
                }
                List<Object> list = (List<Object>)arg;
                int index = checkIndex(null, arguments.get(1), list.size());
                return list.remove(index);
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Nexus.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof NexusInstance)) {
            throw new RuntimeError(expr.name,
                    "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((NexusInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        NexusClass superclass = (NexusClass)environment.getAt(
                distance, "super");
        NexusInstance object = (NexusInstance)environment.getAt(
                distance - 1, "this");
        NexusFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        if (object instanceof List) {
            List<Object> list = (List<Object>)object;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(stringify(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements,
                      Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof NexusClass)) {
                throw new RuntimeError(stmt.superclass.name,
                        "Superclass must be a class.");
            }
        }
        environment.define(stmt.name.lexeme, null);
        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }
        Map<String, NexusFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            NexusFunction function = new NexusFunction(method, environment,
                    method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        NexusClass klass = new NexusClass(stmt.name.lexeme,
                (NexusClass)superclass, methods);

        if (superclass != null) {
            environment = environment.enclosing;
        }
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        NexusFunction function = new NexusFunction(stmt, environment, false);        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                // Fall through to the increment, then re-check the condition.
            }
            if (stmt.increment != null) {
                execute(stmt.increment);
            }
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueException();
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof NexusCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        NexusCallable function = (NexusCallable)callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof NexusInstance) {
            return ((NexusInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitArrayExpr(Expr.Array expr) {
        List<Object> elements = new ArrayList<>();
        for (Expr element : expr.elements) {
            elements.add(evaluate(element));
        }
        return elements;
    }

    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        Object object = evaluate(expr.object);
        Object indexValue = evaluate(expr.index);

        if (object instanceof List) {
            List<Object> list = (List<Object>)object;
            int index = checkIndex(expr.bracket, indexValue, list.size());
            return list.get(index);
        }

        throw new RuntimeError(expr.bracket, "Only lists can be indexed.");
    }

    @Override
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof List)) {
            throw new RuntimeError(expr.bracket,
                    "Only lists can be indexed.");
        }

        Object indexValue = evaluate(expr.index);
        List<Object> list = (List<Object>)object;
        int index = checkIndex(expr.bracket, indexValue, list.size());
        Object value = evaluate(expr.value);
        list.set(index, value);
        return value;
    }

    private int checkIndex(Token bracket, Object indexValue, int size) {
        if (!(indexValue instanceof Double)) {
            throw new RuntimeError(bracket, "List index must be a number.");
        }
        double d = (double)indexValue;
        if (d != Math.floor(d) || Double.isInfinite(d)) {
            throw new RuntimeError(bracket,
                    "List index must be an integer.");
        }
        int index = (int)d;
        if (index < 0 || index >= size) {
            throw new RuntimeError(bracket, "List index out of range.");
        }
        return index;
    }

}