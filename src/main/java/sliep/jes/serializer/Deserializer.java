package sliep.jes.serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sliep.jes.reflection.JesUtilsKt;
import sliep.jes.serializer.annotations.JsonName;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static sliep.jes.serializer.Serializer.MODIFIER_ENUM;
import static sliep.jes.serializer.Serializer.MODIFIER_STATIC_TRANSIENT;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Deserializer {

    @NotNull
    public static Object objectValue(@NotNull Object jes, @NotNull Type type) {
        if (jes instanceof JSONArray) return objectValueArray((JSONArray) jes, type, null);
        if (jes instanceof JSONObject) return objectValueObject((JSONObject) jes, type, null);
        if (type instanceof Class) return objectValueType(jes, (Class<?>) type);
        if (jes instanceof String) return objectValueString((String) jes, type);
        return jes;
    }

    @NotNull
    public static Object objectValueArray(@NotNull JSONArray jes, @NotNull Type arrayType, @Nullable Object target) {
        if (arrayType instanceof Class<?>) {
            if (JSONArray.class == arrayType) return jes;
            Class<?> componentType = ((Class<?>) arrayType).getComponentType();
            if (componentType == null)
                throw new IllegalStateException("Expected array type, found " + arrayType.getTypeName());
            if (componentType.isPrimitive()) {
                Object result = target != null ? target : Array.newInstance(componentType, jes.length());
                for (int i = 0; i < Array.getLength(result); i++)
                    Array.set(result, i, objectValuePrimitive(jes.get(i), componentType));
                return result;
            } else {
                Object[] result = (Object[]) (target != null ? target : Array.newInstance(componentType, jes.length()));
                for (int i = 0; i < result.length; i++)
                    result[i] = objectValue(jes.get(i), componentType);
                return result;
            }
        }
        if (arrayType instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) arrayType).getGenericComponentType();
            Object[] result = (Object[]) (target != null ? target : Array.newInstance((Class<?>) (componentType instanceof Class<?> ? componentType : ((ParameterizedType) componentType).getRawType()), jes.length()));
            for (int i = 0; i < result.length; i++)
                result[i] = objectValue(jes.get(i), componentType);
            return result;
        }
        if (arrayType instanceof ParameterizedType) {
            Class<?> type = (Class<?>) ((ParameterizedType) arrayType).getRawType();
            Type componentType = ((ParameterizedType) arrayType).getActualTypeArguments()[0];
            Collection<Object> result;
            if (target != null) result = (Collection<Object>) target;
            else if (List.class.isAssignableFrom(type))
                if (ArrayList.class == type || List.class == type) result = new ArrayList<>(jes.length());
                else if (LinkedList.class == type) result = new LinkedList<>();
                else if (Vector.class == type) result = new Vector<>(jes.length());
                else if (Stack.class == type) result = new Stack<>();
                else throw new IllegalStateException(
                            "Supported List types: [ArrayList, LinkedList, Vector, Stack], found " + arrayType.getTypeName());
            else if (Set.class.isAssignableFrom(type))
                if (HashSet.class == type || Set.class == type) result = new HashSet<>(jes.length());
                else if (LinkedHashSet.class == type) result = new LinkedHashSet<>(jes.length());
                else if (TreeSet.class == type) result = new TreeSet<>();
                else throw new IllegalStateException(
                            "Supported Set types: [HashSet, LinkedHashSet, TreeSet], found " + arrayType.getTypeName());
            else throw new IllegalStateException("Expected list or set type, found " + arrayType.getTypeName());
            for (int i = 0; i < jes.length(); i++) result.add(objectValue(jes.get(i), componentType));
            return result;
        }
        throw new IllegalStateException("Expected array or collection type, found " + arrayType.getTypeName());
    }

    @NotNull
    public static Object objectValueObject(@NotNull JSONObject jes, @NotNull Type genericType, @Nullable Object target) {
        if (genericType instanceof Class<?>) {
            if (JSONObject.class == genericType) return jes;
            Class<?> type = (Class<?>) genericType;
            Object result = target != null ? target : JesUtilsKt.accessor.allocateInstance(type);
            for (String key : jes.keySet()) {
                if (jes.isNull(key)) continue;
                Field field = null;
                for (Field tmp : JesUtilsKt.accessor.fields(type))
                    if ((tmp.getModifiers() & MODIFIER_STATIC_TRANSIENT) == 0) {
                        JsonName name = tmp.getAnnotation(JsonName.class);
                        if (key.equals(name == null ? tmp.getName() : name.value())) {
                            field = tmp;
                            break;
                        }
                    }
                if (field != null) try {
                    field.set(result, valueFor(field, jes.get(key)));
                } catch (Throwable e) {
                    throw new JSONException("Failed to deserialize field " + field.getDeclaringClass().getSimpleName() + "." + key + " of type " + field.getType().getName(), e);
                }
            }
            return result;
        }
        if (genericType instanceof ParameterizedType) {
            Class<?> type = (Class<?>) ((ParameterizedType) genericType).getRawType();
            Type componentType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
            Map<Object, Object> result;
            if (target != null) result = (Map<Object, Object>) target;
            else if (HashMap.class == type || Map.class == type) result = new HashMap<>(jes.length());
            else if (LinkedHashMap.class == type) result = new LinkedHashMap<>(jes.length());
            else if (TreeMap.class == type) result = new TreeMap<>();
            else throw new IllegalStateException(
                        "Supported Set types: [HashMap, LinkedHashMap, TreeMap], found " + genericType.getTypeName());
            for (String key : jes.keySet())
                result.put(key, objectValue(jes.get(key), componentType));
            return result;
        }
        throw new IllegalStateException("Expected object or map type, found " + genericType.getTypeName());
    }

    @NotNull
    public static Object objectValueType(@NotNull Object jes, @NotNull Class<?> type) {
        if (jes instanceof String) {
            if (type == String.class) return jes;
            if (type.isPrimitive()) {
                if (type == int.class) return Integer.parseInt((String) jes);
                if (type == boolean.class) return Boolean.parseBoolean((String) jes);
                if (type == float.class) return Float.parseFloat((String) jes);
                if (type == double.class) return Double.parseDouble((String) jes);
                if (type == long.class) return Long.parseLong((String) jes);
                if (type == char.class) return ((String) jes).charAt(0);
                if (type == short.class) return Short.parseShort((String) jes);
                if (type == byte.class) return Byte.parseByte((String) jes);
            }
            if ((type.getModifiers() & MODIFIER_ENUM) != 0) return objectValueEnum(jes, type);
            if (type == Integer.class) return Integer.parseInt((String) jes);
            if (type == Boolean.class) return Boolean.parseBoolean((String) jes);
            if (type == Float.class) return Float.parseFloat((String) jes);
            if (type == Double.class) return Double.parseDouble((String) jes);
            if (type == Long.class) return Long.parseLong((String) jes);
            if (type == Character.class) return ((String) jes).charAt(0);
            if (type == Short.class) return Short.parseShort((String) jes);
            if (type == Byte.class) return Byte.parseByte((String) jes);
            if (type == JSONObject.class) return new JSONObject((String) jes);
            if (type == JSONArray.class) return new JSONArray((String) jes);
            return objectValueString((String) jes, type);
        }
        if (type == String.class) return jes.toString();
        if (type.isPrimitive()) return objectValuePrimitive(jes, type);
        if ((type.getModifiers() & MODIFIER_ENUM) != 0)
            return objectValueEnum(jes instanceof Integer ? jes : ((Number) jes).intValue(), type);
        if (type == Integer.class)
            return jes instanceof Integer ? jes : ((Number) jes).intValue();
        if (type == Boolean.class)
            return jes instanceof Number ? ((Number) jes).intValue() != 0 : jes == Boolean.TRUE;
        if (type == Float.class)
            return jes instanceof Float ? jes : ((Number) jes).floatValue();
        if (type == Double.class)
            return jes instanceof Double ? jes : ((Number) jes).doubleValue();
        if (type == Long.class)
            return jes instanceof Long ? jes : ((Number) jes).longValue();
        if (type == Character.class)
            return jes instanceof Character ? jes : (char) ((Number) jes).intValue();
        if (type == Short.class)
            return ((Number) jes).shortValue();
        if (type == Byte.class)
            return ((Number) jes).byteValue();
        return jes;
    }

    @NotNull
    public static Object objectValueString(@NotNull String jes, @NotNull Type type) {
        try {
            return objectValueObject(new JSONObject(jes), type, null);
        } catch (JSONException ignored) {
        }
        try {
            return objectValueArray(new JSONArray(jes), type, null);
        } catch (JSONException ignored) {
        }
        throw new IllegalArgumentException("Failed to deserialize object from String: " + jes);
    }

    @NotNull
    private static Object objectValuePrimitive(@NotNull Object jes, @NotNull Class<?> type) {
        if (type == int.class)
            return jes instanceof Integer ? jes : ((Number) jes).intValue();
        if (type == boolean.class)
            return jes instanceof Number ? ((Number) jes).intValue() != 0 : jes == Boolean.TRUE;
        if (type == float.class)
            return jes instanceof Float ? jes : ((Number) jes).floatValue();
        if (type == double.class)
            return jes instanceof Double ? jes : ((Number) jes).doubleValue();
        if (type == long.class)
            return jes instanceof Long ? jes : ((Number) jes).longValue();
        if (type == char.class)
            return jes instanceof Character ? jes : (char) ((Number) jes).intValue();
        if (type == short.class)
            return ((Number) jes).shortValue();
        if (type == byte.class)
            return ((Number) jes).byteValue();
        throw new UnsupportedOperationException("Invalid primitive type " + type);
    }

    @NotNull
    private static Object objectValueEnum(@NotNull Object jes, @NotNull Class<?> type) {
        if (ValueEnum.class.isAssignableFrom(type)) {
            int id = jes instanceof Integer ? (int) jes : Integer.parseInt((String) jes);
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
    private static Object valueFor(@NotNull Field field, @NotNull Object value) {
        Type type = field.getGenericType();
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            UserSerializer serializer = Serializer.serializers.get(annotation.annotationType());
            if (serializer != null) return serializer.fromJson(annotation, value, type);
        }
        return objectValue(value, type);
    }
}
