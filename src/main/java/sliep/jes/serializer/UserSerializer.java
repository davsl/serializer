package sliep.jes.serializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public abstract class UserSerializer<A extends Annotation, JV, OV> {
    public UserSerializer(Class<A> annotationType) {
        Serializer.serializers.put(annotationType, this);
    }

    @Nullable
    public abstract JV toJson(@NotNull A annotation, @NotNull OV value);

    @NotNull
    public abstract OV fromJson(@NotNull A annotation, @NotNull JV value, @NotNull Type type) throws JSONException;
}
