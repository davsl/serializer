package sliep.jes.serializer.di;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

public abstract class Module implements DIModule {
    @NotNull
    private final DependencyProvider[] methods;
    @NotNull
    private final String name;

    public Module() {
        Class<? extends Module> module = this.getClass();
        name = module.getSimpleName();
        Method[] declaredMethods = module.getDeclaredMethods();
        DependencyProvider[] methods = new DependencyProvider[declaredMethods.length];
        int i = 0;
        for (Method method : declaredMethods) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length > 1) continue;
            methods[i++] = new DependencyProvider(method, parameters);
        }
        Arrays.sort(methods, 0, i, ProviderComparator.INSTANCE);
        if (methods.length < i) {
            this.methods = new DependencyProvider[i];
            System.arraycopy(methods, 0, this.methods, 0, i);
        } else this.methods = methods;
    }

    @NotNull
    public final Object resolve(@NotNull Object requester, @NotNull Class<?> dependency) throws Throwable {
        for (DependencyProvider method : methods)
            if (dependency.isAssignableFrom(method.dependency)) try {
                if (method.requester == null) return method.method.invoke(this);
                if (method.requester.isInstance(requester)) return method.method.invoke(this, requester);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        throw new IllegalStateException("[" + name + "] Failed to resolve dependency '" + dependency.getName() + "' requested by " + requester.getClass());
    }

    private final static class DependencyProvider {
        @NotNull
        final Method method;
        @NotNull
        final Class<?> dependency;
        @Nullable
        final Class<?> requester;

        DependencyProvider(@NotNull Method method, @NotNull Class<?>[] parameters) {
            method.setAccessible(true);
            this.method = method;
            this.dependency = method.getReturnType();
            this.requester = parameters.length == 0 ? null : parameters[0];
        }

        @Override
        public String toString() {
            return method.getName() + "(" + (requester == null ? "" : requester.getName()) + ") => " + dependency.getName() + "";
        }
    }

    private final static class ProviderComparator implements Comparator<DependencyProvider> {
        private final static ProviderComparator INSTANCE = new ProviderComparator();

        private ProviderComparator() {
        }

        @Override
        public int compare(DependencyProvider m1, DependencyProvider m2) {
            if (m1.dependency != m2.dependency)
                if (m1.dependency.isAssignableFrom(m2.dependency)) return -1;
                else if (m2.dependency.isAssignableFrom(m1.dependency)) return 1;
                else return 0;
            else if (m1.requester != m2.requester)
                if (m2.requester == null) return -1;
                else if (m1.requester == null) return 1;
                else if (m2.requester.isAssignableFrom(m1.requester)) return -1;
                else if (m1.requester.isAssignableFrom(m2.requester)) return 1;
                else return 0;
            else throw new IllegalStateException("Duplicate provider declaration [" + m1 + "] and [" + m2 + "]");
        }
    }
}
