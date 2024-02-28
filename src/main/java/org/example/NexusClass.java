package org.example;

import java.util.List;
import java.util.Map;

public class NexusClass implements NexusCallable {

    final String name;

    final NexusClass superclass;

    private final Map<String, NexusFunction> methods;

    NexusClass(String name, NexusClass superclass,
             Map<String, NexusFunction> methods) {
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }

    NexusFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        if (superclass != null) {
            return superclass.findMethod(name);
        }


        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter,
                       List<Object> arguments) {
        NexusInstance instance = new NexusInstance(this);
        NexusFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public int arity() {
        NexusFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
