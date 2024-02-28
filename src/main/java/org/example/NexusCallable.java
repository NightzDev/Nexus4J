package org.example;

import java.util.List;

public interface NexusCallable {

    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);

}
