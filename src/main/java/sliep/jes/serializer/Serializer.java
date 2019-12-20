package sliep.jes.serializer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import sliep.jes.reflection.JesUtilsKt;
import sliep.jes.serializer.annotations.JesDate;
import sliep.jes.serializer.annotations.JsonName;
import sliep.jes.serializer.annotations.SerializeWith;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public final class Serializer {
    @NotNull
    public static Object jsonValue(@NotNull Object value) throws NonJesObjectException {
        Class<?> type = value.getClass();
        if (type.isPrimitive() || value instanceof String || value instanceof Number || type == Character.class || type == Boolean.class)
            return value;
        if (type.isArray()) {
            JSONArray result = new JSONArray();
            if (value instanceof Object[]) {
                for (Object element : ((Object[]) value))
                    if (element != null) result.put(jsonValue(element));
            } else {
                for (int i = 0; i < Array.getLength(value); i++) result.put(Array.get(value, i));
            }
            return result;
        }
        if (value instanceof Iterable<?>) {
            JSONArray result = new JSONArray();
            for (Object item : (Iterable<?>) value) result.put(jsonValue(item));
            return result;
        }
        if (type.isEnum())
            if (value instanceof ValueEnum) return ((ValueEnum) value).getValue();
            else return ((Enum<?>) value).name();
        if (value instanceof JesObject) {
            JSONObject result = new JSONObject();
            for (Field field : JesUtilsKt.accessor.fields(type))
                if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                    JsonName jsonName = field.getAnnotation(JsonName.class);
                    String key = jsonName == null ? field.getName() : jsonName.value();
                    if (result.has(key)) throw new IllegalStateException("Duplicate declaration of key " + key);
                    try {
                        Object fValue = field.get(value);
                        if (fValue != null) result.put(key, valueFor(field, fValue));
                    } catch (IllegalAccessException ignored) {
                    }
                }
            return result;
        }
        if (value instanceof Map<?, ?>) {
            JSONObject result = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Object entryValue = entry.getValue();
                if (entryValue == null) continue;
                String key = entry.getKey().toString();
                if (result.has(key)) throw new IllegalStateException("Duplicate declaration of key " + key);
                result.put(key, jsonValue(entryValue));
            }
            return result;
        }
        throw new NonJesObjectException(type);
    }

    @NotNull
    private static Object valueFor(@NotNull Field field, @NotNull Object value) throws NonJesObjectException {
        SerializeWith impl = field.getAnnotation(SerializeWith.class);
        if (impl != null) return SerializeWith.Provider.toJson(impl, value);
        JesDate date = field.getAnnotation(JesDate.class);
        if (date != null) return JesDate.Provider.toJson(date, value);
        return jsonValue(value);
    }
}
