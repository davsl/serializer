package sliep.jes.serializer.di;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public abstract class ModuleVariant<Key, AM extends DIModule> implements DIModule {
    @NotNull
    protected final HashMap<Key, AM> variants = new HashMap<>();
    @NotNull
    private AM delegate;
    @NotNull
    private Key variant;

    protected ModuleVariant() {
        variant = initializeVariants();
        delegate = variants.get(variant);
    }

    @NotNull
    public final Object resolve(@NotNull Object requester, @NotNull Class<?> dependency) throws Throwable {
        return delegate.resolve(requester, dependency);
    }

    @NotNull
    public Key getVariant() {
        return variant;
    }

    public final void setVariant(@NotNull Key variant) {
        this.variant = variant;
        this.delegate = variants.get(variant);
    }

    protected abstract Key initializeVariants();
}
