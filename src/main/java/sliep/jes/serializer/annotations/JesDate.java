package sliep.jes.serializer.annotations;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JesDate {
    String value();

    class Provider {
        private static HashMap<String, SimpleDateFormat> formats = new HashMap<>();

        @NotNull
        private static SimpleDateFormat get(@NotNull JesDate annotation) {
            String format = annotation.value();
            SimpleDateFormat result = formats.get(format);
            if (result != null) return result;
            result = new SimpleDateFormat(format);
            formats.put(format, result);
            return result;
        }

        @NotNull
        public static String toJson(@NotNull JesDate annotation, @NotNull Object value) {
            return get(annotation).format(value);
        }

        @NotNull
        public static Date fromJson(@NotNull JesDate annotation, @NotNull Object value) {
            try {
                return get(annotation).parse(value.toString());
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
