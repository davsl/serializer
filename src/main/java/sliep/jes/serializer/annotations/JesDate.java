package sliep.jes.serializer.annotations;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import sliep.jes.serializer.UserSerializer;

import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JesDate {
    Provider INSTANCE = new Provider();

    String value();

    class Provider extends UserSerializer<JesDate, String, Date> {
        private static HashMap<String, SimpleDateFormat> formats = new HashMap<>();

        private Provider() {
            super(JesDate.class);
        }

        @NotNull
        @Override
        public String toJson(@NotNull JesDate annotation, @NotNull Date value) {
            return get(annotation).format(value);
        }

        @NotNull
        @Override
        public Date fromJson(@NotNull JesDate annotation, @NotNull String value, @NotNull Type type) throws JSONException {
            try {
                return get(annotation).parse(value);
            } catch (ParseException e) {
                throw new JSONException(e);
            }
        }

        @NotNull
        private static SimpleDateFormat get(@NotNull JesDate annotation) {
            String format = annotation.value();
            SimpleDateFormat result = formats.get(format);
            if (result != null) return result;
            result = new SimpleDateFormat(format);
            formats.put(format, result);
            return result;
        }
    }
}
