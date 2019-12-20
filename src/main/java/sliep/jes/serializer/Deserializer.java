package sliep.jes.serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sliep.jes.reflection.JesUtilsKt;
import sliep.jes.serializer.annotations.JesDate;
import sliep.jes.serializer.annotations.SerializeWith;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public final class Deserializer {
    @NotNull
    public static Object objectValue(@NotNull Object jes, @NotNull Class<?> type) {
        if (type.isInstance(jes)) return jes;
        if (jes instanceof JSONArray) return objectValueArray((JSONArray) jes, type.getComponentType(), null);
        if (jes instanceof JSONObject) return objectValueObject((JSONObject) jes, type, null);
        if (type.isEnum()) return objectValueEnum(jes.toString(), type);
        if (jes instanceof Number) return objectValueNumber((Number) jes, type);
        if (jes instanceof String) return objectValueString((String) jes, type);
        return jes;
    }

    @NotNull
    public static Object objectValueArray(@NotNull JSONArray jes, @NotNull Class<?> componentType, @Nullable Object target) {
        Object result = target != null ? target : Array.newInstance(componentType, jes.length());
        if (result instanceof Object[])
            for (int i = 0; i < ((Object[]) result).length; i++)
                ((Object[]) result)[i] = objectValue(jes.get(i), componentType);
        else
            for (int i = 0; i < Array.getLength(result); i++)
                Array.set(result, i, objectValue(jes.get(i), componentType));
        return result;
    }

    @NotNull
    public static Object objectValueObject(@NotNull JSONObject jes, @NotNull Class<?> type, @Nullable Object target) {
        Object result = target != null ? target : JesUtilsKt.accessor.allocateInstance(type);
        for (String key : jes.keySet()) {
            if (jes.isNull(key)) continue;
            Field field = null;
            for (Field f : JesUtilsKt.accessor.fields(type))
                if ((f.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0 && f.getName().equals(key)) {
                    field = f;
                    break;
                }
            if (field == null) continue;
            try {
                field.set(result, valueFor(field, jes.get(key)));
            } catch (Throwable e) {
                throw new JSONException("Failed to deserialize field " + field.getDeclaringClass().getSimpleName() + "." + key + " of type " + field.getType().getName(), e);
            }
        }
        return result;
    }

    @NotNull
    public static Object objectValueEnum(@NotNull String jes, @NotNull Class<?> type) {
        if (ValueEnum.class.isAssignableFrom(type)) {
            int id = Integer.parseInt(jes);
            for (Object constant : type.getEnumConstants())
                if (((ValueEnum) constant).getValue() == id) return constant;
            throw new IllegalArgumentException("No enum constant for value: " + id + " in " + type);
        } else {
            for (Object constant : type.getEnumConstants())
                if (((Enum<?>) constant).name().equals(jes)) return constant;
            throw new IllegalArgumentException("No enum constant for name: " + jes + " in " + type);
        }
    }

    @NotNull
    private static Object objectValueNumber(@NotNull Number jes, @NotNull Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) return jes.intValue();
            if (type == float.class) return jes.floatValue();
            if (type == byte.class) return jes.byteValue();
            if (type == double.class) return jes.doubleValue();
            if (type == long.class) return jes.longValue();
            if (type == char.class) return (char) jes.intValue();
            if (type == boolean.class) return jes.intValue() != 0;
            if (type == short.class) return jes.shortValue();
        }
        if (type == Integer.class) return jes.intValue();
        if (type == Float.class) return jes.floatValue();
        if (type == Byte.class) return jes.byteValue();
        if (type == Double.class) return jes.doubleValue();
        if (type == Long.class) return jes.longValue();
        if (type == Character.class) return (char) jes.intValue();
        if (type == Boolean.class) return jes.intValue() != 0;
        if (type == Short.class) return jes.shortValue();
        return jes.toString();
    }

    @NotNull
    private static Object objectValueString(@NotNull String jes, @NotNull Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) return Integer.parseInt(jes);
            if (type == float.class) return Float.parseFloat(jes);
            if (type == byte.class) return Byte.parseByte(jes);
            if (type == double.class) return Double.parseDouble(jes);
            if (type == long.class) return Long.parseLong(jes);
            if (type == char.class) return jes.charAt(0);
            if (type == boolean.class) return Boolean.parseBoolean(jes);
            if (type == short.class) return Short.parseShort(jes);
        }
        if (type == Integer.class) return Integer.parseInt(jes);
        if (type == Float.class) return Float.parseFloat(jes);
        if (type == Byte.class) return Byte.parseByte(jes);
        if (type == Double.class) return Double.parseDouble(jes);
        if (type == Long.class) return Long.parseLong(jes);
        if (type == Character.class) return jes.charAt(0);
        if (type == Boolean.class) return Boolean.parseBoolean(jes);
        if (type == Short.class) return Short.parseShort(jes);
        if (JSONObject.class.isAssignableFrom(type)) return new JSONObject(jes);
        if (JSONArray.class.isAssignableFrom(type)) return new JSONArray(jes);
        if (type.getComponentType() != null) objectValueArray(new JSONArray(jes), type.getComponentType(), null);
        return objectValueObject(new JSONObject(jes), type, null);
    }

    @NotNull
    private static Object valueFor(@NotNull Field field, @NotNull Object value) {
        Class<?> type = field.getType();
        SerializeWith impl = field.getAnnotation(SerializeWith.class);
        if (impl != null) return SerializeWith.Provider.fromJson(impl, value, type);
        JesDate date = field.getAnnotation(JesDate.class);
        if (date != null) return JesDate.Provider.fromJson(date, value);
        if (List.class.isAssignableFrom(type)) {
            Class<?> componentType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            List<Object> result;
            if (type.isAssignableFrom(ArrayList.class)) result = new ArrayList<>();
            else if (type.isAssignableFrom(LinkedList.class)) result = new LinkedList<>();
            else throw new IllegalStateException("List type not supported: " + type);
            JSONArray jes = (JSONArray) value;
            for (int i = 0; i < jes.length(); i++) result.add(objectValue(jes.get(i), componentType));
            return result;
        }
        if (Map.class.isAssignableFrom(type)) {
            Class<?>[] componentType = (Class<?>[]) ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
            if (!String.class.isAssignableFrom(componentType[0]))
                throw new IllegalStateException("Map key type not supported: " + componentType[0]);
            Map<Object, Object> result;
            if (type.isAssignableFrom(LinkedHashMap.class)) result = new LinkedHashMap<>();
            else if (type.isAssignableFrom(HashMap.class)) result = new HashMap<>();
            else if (type.isAssignableFrom(TreeMap.class)) result = new TreeMap<>();
            else throw new IllegalStateException("Map type not supported: " + type);
            JSONObject jes = (JSONObject) value;
            for (String key : jes.keySet())
                result.put(key, objectValue(jes.get(key), componentType[1]));
            return result;
        }
        return objectValue(value, type);
    }
}
