package sliep.jes.serializer.di;

import org.jetbrains.annotations.NotNull;

public interface DIModule {
    @NotNull
    Object resolve(@NotNull Object requester, @NotNull Class<?> dependency) throws Throwable;
}
