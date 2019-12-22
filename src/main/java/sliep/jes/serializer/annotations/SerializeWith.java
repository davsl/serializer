package sliep.jes.serializer.annotations;

import org.jetbrains.annotations.NotNull;
import sliep.jes.reflection.JesConstructorsKt;
import sliep.jes.serializer.NonJesObjectException;
import sliep.jes.serializer.UserSerializer;

import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.HashMap;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerializeWith {
    Class<? extends UserSerializer<?, ?>> value();

    class Provider {
        private static HashMap<Class<?>, UserSerializer<Object, Object>> formats = new HashMap<>();

        @NotNull
        private static UserSerializer<Object, Object> get(@NotNull SerializeWith annotation) {
            Class<?> serialize = annotation.value();
            UserSerializer<Object, Object> result = formats.get(serialize);
            if (result != null) return result;
            try {
                //noinspection unchecked
                result = (UserSerializer<Object, Object>) JesConstructorsKt.constructor(serialize).newInstance();
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
            formats.put(serialize, result);
            return result;
        }

        @NotNull
        public static Object toJson(@NotNull SerializeWith annotation, @NotNull Object value) {
            try {
                return get(annotation).toJson(value);
            } catch (NonJesObjectException e) {
                throw new IllegalStateException(e);
            }
        }

        @NotNull
        public static Object fromJson(@NotNull SerializeWith annotation, @NotNull Object value, @NotNull Type type) {
            return get(annotation).fromJson(value, type);
        }
    }
}
