package me.petulikan1.zMachines.config.loaders;


import me.petulikan1.zMachines.config.Config;
import me.petulikan1.zMachines.config.StringContainer;
import me.petulikan1.zMachines.config.constructors.DataLoaderConstructor;
import me.petulikan1.zMachines.config.constructors.DataValue;
import me.petulikan1.zMachines.config.constructors.LoaderPriority;
import me.petulikan1.zMachines.utils.StreamUtils;
import me.petulikan1.zMachines.utils.Validator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public abstract class DataLoader implements Cloneable {

    // Data loaders hierarchy
    public static final Map<LoaderPriority, List<DataLoaderConstructor>> dataLoaders = new HashMap<>();

    // Do not modify!
    private static boolean anyLoaderWhichAllowFiles;

    static {
        for (LoaderPriority priority : LoaderPriority.values()) {
            DataLoader.dataLoaders.put(priority, new ArrayList<>());
        }

        // BUILT-IN LOADERS
        DataLoader.dataLoaders.get(LoaderPriority.LOW).add(new DataLoaderConstructor() {

            @Override
            public @Nonnull DataLoader construct() {
                return new ByteLoader();
            }

            @Override
            public @NotNull String name() {
                return "byte";
            }
        });
        DataLoader.dataLoaders.get(LoaderPriority.NORMAL).add(new DataLoaderConstructor() {

            @Override
            public @Nonnull DataLoader construct() {
                return new JsonLoader();
            }

            @Override
            public @NotNull String name() {
                return "json";
            }
        });
        DataLoader.dataLoaders.get(LoaderPriority.NORMAL).add(new DataLoaderConstructor() {

            @Override
            public @NotNull DataLoader construct() {
                return new TomlLoader();
            }

            @Override
            public @NotNull String name() {
                return "toml";
            }
        });
        DataLoader.dataLoaders.get(LoaderPriority.NORMAL).add(new DataLoaderConstructor() {

            @Override
            public @Nonnull DataLoader construct() {
                return new PropertiesLoader();
            }

            @Override
            public @NotNull String name() {
                return "properties";
            }
        });
        DataLoader.dataLoaders.get(LoaderPriority.HIGH).add(new DataLoaderConstructor() {

            @Override
            public @Nonnull DataLoader construct() {
                return new YamlLoader();
            }

            @Override
            public @NotNull String name() {
                return "yml";
            }
        });
        DataLoader.dataLoaders.get(LoaderPriority.HIGHEST).add(new DataLoaderConstructor() {

            @Override
            public @NotNull String name() {
                return "empty";
            }

            @Override
            public @Nonnull DataLoader construct() {
                return new EmptyLoader();
            }
        });
    }

    public static void register(@Nonnull LoaderPriority priority, @Nonnull DataLoaderConstructor constructor) {
        Validator.notNull(priority, "LoaderPriority");
        Validator.notNull(constructor, "DataLoaderConstructor");
        DataLoader.dataLoaders.get(priority).add(constructor);
        if (constructor.construct().loadingFromFile()) {
            anyLoaderWhichAllowFiles = true;
        }
    }

    public void unregister(@Nonnull DataLoaderConstructor constructor) {
        Validator.notNull(constructor, "DataLoaderConstructor");
        for (List<DataLoaderConstructor> entry : DataLoader.dataLoaders.values()) {
            if (entry.remove(constructor)) {
                break;
            }
        }
    }

    public abstract boolean loadingFromFile();

    @Nonnull
    public abstract Set<String> getPrimaryKeys();

    @Nonnull
    public abstract Map<String, DataValue> get();

    public abstract void set(@Nonnull String key, @Nonnull DataValue value);

    public abstract boolean remove(@Nonnull String key, boolean withSubKeys);

    public boolean remove(@Nonnull String key) {
        return remove(key, false);
    }

    @Nonnull
    public abstract Collection<String> getHeader();

    @Nonnull
    public abstract Collection<String> getFooter();

    @Nonnull
    public abstract Set<String> getKeys();

    public abstract void reset();

    public abstract void load(StringContainer container, @Nonnull List<int[]> input);

    public abstract void load(@Nullable String input);

    public abstract boolean supportsReadingLines();

    public void load(@Nonnull File file) {
        this.load(StreamUtils.fromStream(file));
    }

    public abstract boolean isLoaded();

    @Nullable
    public abstract DataValue get(@Nonnull String key);

    @Nonnull
    public abstract DataValue getOrCreate(@Nonnull String key);

    @Nonnull
    public String saveAsString(Config config, boolean markSaved) {
        Validator.notNull(config, "Config");
        return saveAsContainer(config, markSaved).toString();
    }

    @Nonnull
    public byte[] save(Config config, boolean markSaved) {
        Validator.notNull(config, "Config");
        return saveAsContainer(config, markSaved).getBytes();
    }

    @Nonnull
    public StringContainer saveAsContainer(Config config, boolean markSaved) {
        Validator.notNull(config, "Config");
        int size = config.getDataLoader().get().size();
        StringContainer builder = new StringContainer(size * 20);
        Iterator<CharSequence> itr = saveAsIterator(config, markSaved);
        while (itr != null && itr.hasNext()) {
            builder.append(itr.next());
        }
        return builder;
    }

    @Nullable
    public Iterator<CharSequence> saveAsIterator(@Nonnull Config config, boolean markSaved) {
        Validator.notNull(config, "Config");
        return null;
    }

    public boolean supportsIteratorMode() {
        return false;
    }

    @Nonnull
    public abstract String name();

    @Nonnull
    public abstract Set<Entry<String, DataValue>> entrySet();

    @Nonnull
    public abstract Set<String> keySet(@Nonnull String key, boolean subkeys);

    @Nonnull
    public abstract Iterator<String> keySetIterator(@Nonnull String key, boolean subkeys);

    @Override
    @Nonnull
    public abstract DataLoader clone();

    @Nonnull
    public static DataLoader findLoaderByName(@Nonnull String type) {
        Validator.notNull(type, "DataLoader Type Name");
        for (LoaderPriority priority : LoaderPriority.values()) {
            for (DataLoaderConstructor constructor : DataLoader.dataLoaders.get(priority)) {
                if (constructor.isConstructorOf(type)) {
                    return constructor.construct();
                }
            }
        }
        return new EmptyLoader();
    }

    @Nonnull
    public static DataLoader findLoaderFor(@Nonnull File input) {
        Validator.notNull(input, "Input File");
        if (!anyLoaderWhichAllowFiles) {
            return findLoaderFor(StreamUtils.fromStream(input));
        }
        if (input.length() > 0L) {
            String inputString = null;
            List<int[]> inputLines = null;
            StringContainer container = null;
            loadersLoop:
            for (LoaderPriority priority : LoaderPriority.values()) {
                for (DataLoaderConstructor constructor : DataLoader.dataLoaders.get(priority)) {
                    DataLoader loader = constructor.construct();
                    if (inputString == null && loader.loadingFromFile()) {
                        loader.load(input);
                    } else {
                        if (inputString == null) {
                            inputString = StreamUtils.fromStream(input);
                            if (inputString == null) {
                                break loadersLoop;
                            }
                        }
                        if (loader.supportsReadingLines()) {
                            if (container == null) {
                                container = new StringContainer(inputString, 0, 0);
                                inputLines = LoaderReadUtil.readLinesFromContainer(container);
                            }
                            loader.load(container, inputLines);
                        } else {
                            loader.load(inputString);
                        }
                    }
                    if (loader.isLoaded()) {
                        return loader;
                    }
                }
            }
        }
        EmptyLoader empty = new EmptyLoader();
        empty.load(input);
        return empty;
    }

    @Nonnull
    public static DataLoader findLoaderFor(@Nullable String inputString) {
        if (inputString != null && !inputString.isEmpty()) {
            for (LoaderPriority priority : LoaderPriority.values()) {
                for (DataLoaderConstructor constructor : DataLoader.dataLoaders.get(priority)) {
                    DataLoader loader = constructor.construct();
                    if (loader.supportsReadingLines()) {
                        StringContainer container = new StringContainer(inputString, 0, 0);
                        List<int[]> inputLines = LoaderReadUtil.readLinesFromContainer(container);
                        loader.load(container, inputLines);
                    } else {
                        loader.load(inputString);
                    }
                    if (loader.isLoaded()) {
                        return loader;
                    }
                }
            }
        }
        EmptyLoader empty = new EmptyLoader();
        empty.load(inputString);
        return empty;
    }
}
