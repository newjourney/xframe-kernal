package dev.xframe.net.cmd;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import dev.xframe.inject.Configurator;
import dev.xframe.inject.Injection;

@Configurator
public class CommandBuilder {
    
    private Map<Predicate<Class<?>>, Function<Class<?>, Command>> builders = new HashMap<>(4);

    public void regist(Predicate<Class<?>> predicate, Function<Class<?>, Command> builder) {
        builders.put(predicate, builder);
    }
    
    public Command build(Class<?> clazz) {
        for (Map.Entry<Predicate<Class<?>>, Function<Class<?>, Command>> entry : builders.entrySet()) {
            if(entry.getKey().test(clazz)) {
                return entry.getValue().apply(clazz);
            }
        }
        return build0(clazz);
    }

    private Command build0(Class<?> clazz) {
        return (Command) Injection.makeInstanceAndInject(clazz);
    }
    
}
