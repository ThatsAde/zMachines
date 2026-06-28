package me.petulikan1.zMachines.config.constructors;


import me.petulikan1.zMachines.config.loaders.DataLoader;

import javax.annotation.Nonnull;

public interface DataLoaderConstructor {

    @Nonnull
    DataLoader construct();

    @Nonnull
    String name();

    default boolean isConstructorOf(@Nonnull String type) {
        return name().equalsIgnoreCase(type);
    }
}