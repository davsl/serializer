package sliep.jes.serializer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import sliep.jes.reflection.JesUtilsKt;
import sliep.jes.serializer.annotations.JsonName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Serializer {
    static final int MODIFIER_STATIC_TRANSIENT = Modifier.TRANSIENT | Modifier.STATIC;
    static final int MODIFIER_ENUM = 16384;
    static final HashMap<Class<?>, UserSerializer> serializers = new HashMap<>();

    @NotNull
    public static Object jsonValue(@NotNull Object value) {
        Class<?> type = value.getClass();
        if (type.isPrimitive() || value instanceof String || value instanceof Number || type == Character.class || type == Boolean.class)
            return value;
        if (type.isArray()) {
            JSONArray result = new JSONArray();
            if (value instanceof Object[]) {
                for (Object element : ((Object[]) value)) result.put(element == null ? null : jsonValue(element));
            } else {
                for (int i = 0; i < Array.getLength(value); i++) result.put(Array.get(value, i));
            }
            return result;
        }
        if ((type.getModifiers() & MODIFIER_ENUM) != 0)
            if (value instanceof ValueEnum) return ((ValueEnum) value).getValue();
            else return ((Enum<?>) value).name();
        if (value instanceof Iterable<?>) {
            JSONArray result = new JSONArray();
            for (Object element : (Iterable<?>) value) result.put(element == null ? null : jsonValue(element));
            return result;
        }
        if (value instanceof Map<?, ?>) {
            JSONObject result = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object element = entry.getValue();
                result.put(entry.getKey().toString(), element == null ? null : jsonValue(element));
            }
            return result;
        }
        JSONObject result = new JSONObject();
        for (Field field : JesUtilsKt.accessor.fields(type))
            if ((field.getModifiers() & MODIFIER_STATIC_TRANSIENT) == 0) try {
                JsonName jsonName = field.getAnnotation(JsonName.class);
                Object fValue = field.get(value);
                if (fValue != null)
                    result.put(jsonName == null ? field.getName() : jsonName.value(), valueFor(field, fValue));
            } catch (IllegalAccessException ignored) {
            }
        return result;
    }

    @NotNull
    private static Object valueFor(@NotNull Field field, @NotNull Object value) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            UserSerializer serializer = serializers.get(annotation.annotationType());
            if (serializer != null) return serializer.toJson(annotation, value);
        }
        return jsonValue(value);
    }
}
