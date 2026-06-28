package me.petulikan1.zMachines.config;

import lombok.NonNull;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.config.constructors.DataValue;
import me.petulikan1.zMachines.config.json.Json;
import me.petulikan1.zMachines.config.loaders.DataLoader;
import me.petulikan1.zMachines.config.loaders.EmptyLoader;
import me.petulikan1.zMachines.config.merge.MergeSetting;
import me.petulikan1.zMachines.config.merge.MergeStandards;
import me.petulikan1.zMachines.utils.StreamUtils;
import me.petulikan1.zMachines.utils.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.Map.Entry;

public class Config {
    protected DataLoader loader;
    protected File file;
    protected transient boolean isSaving; // LOCK
    protected boolean requireSave;


    protected int updaterTask;

    public static Config loadFromInput(@NonNull InputStream input) {
        return new Config().reload(StreamUtils.fromStream(input));
    }

    public static Config loadFromInput(@NonNull InputStream input, @NonNull String outputFile, @NonNull MergeSetting... settings) {
        return Config.loadFromInput(input, new File(outputFile), settings);
    }

    public static Config loadFromInput(@NonNull InputStream input, @NonNull File outputFile, @NonNull MergeSetting... settings) {
        Config config = new Config(outputFile);
        config.merge(new Config().reload(StreamUtils.fromStream(input)), settings);
        return config;
    }

    public static Config createConfig(String name, String jar_file_name, Loader main) {
        Config original_data = new Config().reload(StreamUtils.fromStream(main.getClass().getClassLoader().getResourceAsStream(jar_file_name)));
        boolean mustSave = !(new File("plugins/" + main.getName() + "/" + name)).exists();
        Config c = new Config("plugins/" + main.getName() + "/" + name);
        if (c.merge(original_data, MergeStandards.DEFAULT) || mustSave) {
            c.save();
        }
        return c;
    }

    public static Config loadFromPlugin(@NonNull Class<?> mainClass, @NonNull String pathToFile, @NonNull File outputFile, @NonNull MergeSetting... settings) {
        return Config.loadFromInput(mainClass.getClassLoader().getResourceAsStream(pathToFile), outputFile, settings);
    }

    public static Config loadFromPlugin(@NonNull Class<?> mainClass, @NonNull String pathToFile, @NonNull String outputFile, @NonNull MergeSetting... settings) {
        return Config.loadFromInput(mainClass.getClassLoader().getResourceAsStream(pathToFile), new File(outputFile), settings);
    }

    public static Config loadFromInput(@NonNull InputStream input, @NonNull String outputFile) {
        return loadFromInput(input, outputFile, MergeStandards.DEFAULT);
    }

    public static Config loadFromInput(@NonNull InputStream input, @NonNull File outputFile) {
        return loadFromInput(input, outputFile, MergeStandards.DEFAULT);
    }

    public static Config loadFromPlugin(@NonNull Class<?> mainClass, @NonNull String pathToFile, @NonNull File outputFile) {
        return loadFromPlugin(mainClass, pathToFile, outputFile, MergeStandards.DEFAULT);
    }

    public static Config loadFromPlugin(@NonNull Class<?> mainClass, @NonNull String pathToFile, @NonNull String outputFile) {
        return loadFromPlugin(mainClass, pathToFile, outputFile, MergeStandards.DEFAULT);
    }

    public static Config loadFromFile(@NonNull File file) {
        return new Config(file);
    }

    public static Config loadFromFile(@NonNull String filePath) {
        return new Config(filePath);
    }

    public static Config loadFromString(@NonNull String input) {
        return new Config().reload(input);
    }

    public Config() {
        loader = new EmptyLoader();
    }

    public Config(@NonNull DataLoader dataLoader) {
        loader = dataLoader;
        requireSave = true;
    }

    public Config(@NonNull String filePath) {
        this(filePath, true);
    }

    public Config(@NonNull String filePath, boolean load) {
        file = new File(filePath.charAt(0) == '/' ? filePath.substring(1) : filePath);
        if (load) {
            loader = DataLoader.findLoaderFor(file); // get & load
        } else {
            loader = new EmptyLoader();
        }
    }

    public Config(@NonNull File file) {
        this(file, true);
    }

    public Config(@NonNull File file, boolean load) {
        this.file = file;
        if (load) {
            loader = DataLoader.findLoaderFor(file);
        } else {
            loader = new EmptyLoader();
        }
    }


    public Config(@NonNull Config data) {
        file = data.file;
        loader = data.getDataLoader().clone();
    }

    public boolean isModified() {
        return requireSave;
    }

    public void markModified() {
        requireSave = true;
    }

    public void markNonModified() {
        requireSave = false;
    }

    public boolean exists(@NonNull String key) {
        return isKey(key);
    }

    public boolean existsKey(@NonNull String key) {
        return getDataLoader().get().containsKey(key);
    }

    public Config setFile(@Nullable File file) {
        if (Objects.equals(file, this.file)) {
            return this;
        }
        markModified();
        this.file = file;
        return this;
    }

    public Config setFile(@Nullable String filePath) {
        return setFile(filePath == null ? null : new File(filePath));
    }

    public boolean setIfAbsent(@NonNull String key, @NonNull Object value) {
        return setIfAbsent(key, value, null);
    }

    public boolean setIfAbsent(@NonNull String key, @NonNull Object value, @Nullable List<String> comments) {
        if (!existsKey(key)) {
            DataValue val = getDataLoader().getOrCreate(key);
            val.value = value;
            val.comments = comments;
            val.modified = true;
            markModified();
            return true;
        }
        return false;
    }

    public Config set(@NonNull String key, @Nullable Object value) {
        if (value == null) {
            if (getDataLoader().remove(key)) {
                markModified();
            }
            return this;
        }
        DataValue val = getDataLoader().get(key);
        if (val == null) {
            getDataLoader().set(key, val = DataValue.of(value));
            val.modified = true;
            markModified();
        } else if (val.value == null || !val.value.equals(value)) {
            val.value = value;
            val.writtenValue = null;
            val.modified = true;
            markModified();
        }
        return this;
    }

    public Config remove(@NonNull String key) {
        if (getDataLoader().remove(key, true)) {
            markModified();
        }
        return this;
    }

    @Nullable
    public List<String> getComments(@NonNull String key) {
        DataValue val = getDataLoader().get(key);
        if (val != null) {
            return val.comments;
        }
        return null;
    }

    public Config setComments(@NonNull String key, @Nullable List<String> value) {
        if (value == null || value.isEmpty()) {
            DataValue val = getDataLoader().get(key);
            if (val != null && val.comments != null && !val.comments.isEmpty()) {
                val.comments = null;
                val.modified = true;
                markModified();
            }
            return this;
        }
        DataValue val = getDataLoader().getOrCreate(key);
        if (val.comments == null || !value.containsAll(val.comments)) {
            val.comments = value;
            val.modified = true;
            markModified();
        }
        return this;
    }

    @Nullable
    public String getCommentAfterValue(@NonNull String key) {
        DataValue val = getDataLoader().getOrCreate(key);
        if (val != null) {
            return val.commentAfterValue;
        }
        return null;
    }

    public Config setCommentAfterValue(@NonNull String key, @Nullable String comment) {
        if (comment == null || comment.isEmpty()) {
            DataValue val = getDataLoader().get(key);
            if (val == null || val.commentAfterValue == null) {
                return this;
            }
            val.commentAfterValue = null;
            val.modified = true;
            markModified();
            return this;
        }
        DataValue val = getDataLoader().getOrCreate(key);
        if (!comment.equals(val.commentAfterValue)) {
            val.commentAfterValue = comment;
            val.modified = true;
            markModified();
        }
        return this;
    }

    @Nullable
    public File getFile() {
        return file;
    }

    public Config setHeader(@Nullable Collection<String> lines) {
        getDataLoader().getHeader().clear();
        if (lines != null) {
            getDataLoader().getHeader().addAll(lines);
        }
        markModified();
        return this;
    }

    public Config setFooter(@Nullable Collection<String> lines) {
        getDataLoader().getFooter().clear();
        if (lines != null) {
            getDataLoader().getFooter().addAll(lines);
        }
        markModified();
        return this;
    }

    @NotNull
    public Collection<String> getHeader() {
        return getDataLoader().getHeader();
    }

    @NotNull
    public Collection<String> getFooter() {
        return getDataLoader().getFooter();
    }

    public Config reload(@Nullable String input) {
        loader = DataLoader.findLoaderFor(input);
        markModified();
        return this;
    }

    public Config reload() {
        return this.reload(getFile());
    }

    public Config reload(@NonNull File file) {
        clear();
        loader = DataLoader.findLoaderFor(file);
        markNonModified();
        return this;
    }

    @Nullable
    public Object get(@NonNull String key) {
        return get(key, null);
    }

    @Nullable
    public Object get(@NonNull String key, @Nullable Object defaultValue) {
        DataValue val = getDataLoader().get(key);
        if (val == null || val.value == null) {
            return defaultValue;
        }
        return val.value;
    }

    @Nullable
    public <E> E getAs(@NonNull String key, @NonNull Class<? extends E> clazz) {
        return getAs(key, clazz, null);
    }

    @Nullable
    public <E> E getAs(@NonNull String key, @NonNull Class<? extends E> clazz, @Nullable E defaultValue) {
        try {
            if (clazz == String.class || clazz == CharSequence.class) {
                return clazz.cast(getString(key));
            }
        } catch (Exception ignored) {
        }
        try {
            return clazz.cast(get(key, defaultValue));
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Contract(value = "_ -> _")
    public String getString(@NonNull String key) {
        return getString(key, null);
    }

    public String getIfEmpty(@NonNull String key, @Nullable String def) {
        DataValue val = getDataLoader().get(key);
        if (val == null || val.value == null)
            return def;
        if (val.writtenValue != null)
            return val.writtenValue.isEmpty() ? def : val.writtenValue;
        return val.value instanceof String str ? str.isEmpty() ? def : str : val.value + "";
    }

    public String getString(@NonNull String key, @Nullable String defaultValue) {
        DataValue val = getDataLoader().get(key);
        if (val == null || val.value == null) {
            return defaultValue;
        }
        if (val.writtenValue != null) {
            return val.writtenValue;
        }
        return val.value instanceof String ? (String) val.value : val.value + "";
    }

    public boolean isJson(@NonNull String key) {
        DataValue val = getDataLoader().get(key);
        if (val == null || val.value == null) {
            return false;
        }
        if (val.writtenValue != null && val.writtenValue.length() > 1) {
            char firstChar = val.writtenValue.charAt(0);
            char lastChar = val.writtenValue.charAt(val.writtenValue.length() - 1);
            return firstChar == '[' && lastChar == ']' || firstChar == '{' && lastChar == '}';
        }
        return false;
    }

    public int getInt(@NonNull String key) {
        return getInt(key, 0);
    }

    public int getInt(@NonNull String key, int defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return StringUtils.getInt(getString(key));
    }

    public double getDouble(@NonNull String key) {
        return getDouble(key, 0);
    }

    public double getDouble(@NonNull String key, double defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return StringUtils.getDouble(getString(key));
    }

    public long getLong(@NonNull String key) {
        return getLong(key, 0);
    }

    public long getLong(@NonNull String key, long defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return StringUtils.getLong(getString(key));
    }

    public float getFloat(@NonNull String key) {
        return getFloat(key, 0);
    }

    public float getFloat(@NonNull String key, float defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return StringUtils.getFloat(getString(key));
    }

    public byte getByte(@NonNull String key) {
        return getByte(key, (byte) 0);
    }

    public byte getByte(@NonNull String key, byte defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }
        return StringUtils.getByte(getString(key));
    }

    public short getShort(@NonNull String key) {
        return getShort(key, (short) 0);
    }

    public short getShort(@NonNull String key, short defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        return StringUtils.getShort(getString(key));
    }

    public boolean getBoolean(@NonNull String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return StringUtils.getBoolean(getString(key));
    }

    @Nullable
    public Collection<Object> getList(@NonNull String key) {
        return getList(key, null);
    }

    @Nullable
    public Collection<Object> getList(@NonNull String key, @Nullable Collection<Object> defaultValue) {
        Object value = get(key);
        if (!(value instanceof Collection)) {
            return defaultValue;
        }
        return new ArrayList<>((Collection<?>) value);
    }

    @NotNull
    public <E> List<E> getListAs(@NonNull String key, @NonNull Class<? extends E> clazz) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<E> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            try {
                list.add(o == null ? null : clazz.cast(o));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    @NotNull
    public List<String> getStringList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            if (o != null) {
                list.add("" + o);
            } else {
                list.add(null);
            }
        }
        return list;
    }

    @NotNull
    public List<Boolean> getBooleanList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Boolean> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o != null && (o instanceof Boolean ? (Boolean) o : StringUtils.getBoolean(o.toString())));
        }
        return list;
    }

    @NotNull
    public List<Integer> getIntegerList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0 : o instanceof Number ? ((Number) o).intValue() : StringUtils.getInt(o.toString()));
        }
        return list;
    }

    @NotNull
    public List<Double> getDoubleList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Double> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0.0 : o instanceof Number ? ((Number) o).doubleValue() : StringUtils.getDouble(o.toString()));
        }
        return list;
    }

    @NotNull
    public List<Short> getShortList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Short> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0 : o instanceof Number ? ((Number) o).shortValue() : StringUtils.getShort(o.toString()));
        }
        return list;
    }

    @NotNull
    public List<Byte> getByteList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Byte> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0 : o instanceof Number ? ((Number) o).byteValue() : StringUtils.getByte(o.toString()));
        }
        return list;
    }

    @NotNull
    public List<Float> getFloatList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Float> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0 : o instanceof Number ? ((Number) o).floatValue() : StringUtils.getFloat(o.toString()));
        }
        return list;
    }

    @NotNull
    public List<Long> getLongList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            list.add(o == null ? 0 : StringUtils.getLong(o.toString()));
        }
        return list;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <K, V> List<Map<K, V>> getMapList(@NonNull String key) {
        Collection<Object> collection = getList(key, Collections.emptyList());
        if (collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<K, V>> list = new ArrayList<>(collection.size());
        for (Object o : collection) {
            if (o == null) {
                list.add(null);
            } else if (o instanceof Map) {
                list.add((Map<K, V>) o);
            } else {
                Object re = Json.reader().read(o.toString());
                list.add(re instanceof Map ? (Map<K, V>) re : null);
            }
        }
        return list;
    }

    public synchronized boolean isSaving() {
        return isSaving;
    }

    public Config save(@NonNull DataType type) {
        return save(type.name());
    }

    public Config save(@NonNull String dataTypeName) {
        if (file == null || isSaving() || !isModified()) {
            return this;
        }
        isSaving = true;
        if (!file.exists()) {
            File folder = file.getParentFile();
            if (folder != null) {
                folder.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (Exception e) {
                isSaving = false;
                e.printStackTrace();
                return this;
            }
        }
        DataLoader writer;
        if (getDataLoader().name().equalsIgnoreCase(dataTypeName)) {
            writer = getDataLoader();
        } else {
            writer = DataLoader.findLoaderByName(dataTypeName);
        }
        if (writer != null) {
            if (writer.supportsIteratorMode()) {
                Iterator<CharSequence> iterator = writer.saveAsIterator(this, true);
                try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    while (iterator.hasNext()) {
                        CharSequence next = iterator.next();
                        channel.write(ByteBuffer
                                .wrap(next instanceof StringContainer ? ((StringContainer) next).getBytes() : next instanceof String ? ((String) next).getBytes() : next.toString().getBytes()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    channel.write(ByteBuffer.wrap(writer.save(this, true)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        markNonModified();
        isSaving = false;
        return this;
    }

    public void save() {
        if ("empty".equals(getDataLoader().name())) {
            this.save("yml");
        } else {
            save(getDataLoader().name());
        }
    }

    @NotNull
    public Set<String> getKeys() {
        return getDataLoader().getPrimaryKeys();
    }

    @NotNull
    public Set<String> getKeys(boolean subkeys) {
        return subkeys ? getDataLoader().getKeys() : getKeys();
    }

    @NotNull
    public Set<String> getKeys(@NonNull String key) {
        return this.getKeys(key, false);
    }

    @NotNull
    public Iterator<String> getIteratorKeys(@NonNull String key) {
        return this.getIteratorKeys(key, false);
    }

    public boolean isKey(@NonNull String key) {
        for (String section : getDataLoader().getKeys()) {
            if (section.startsWith(key)) {
                if (section.length() == key.length() || section.charAt(key.length()) == '.') {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public Set<String> getKeys(@NonNull String key, boolean subkeys) {
        return getDataLoader().keySet(key, subkeys);
    }

    @NotNull
    public Iterator<String> getIteratorKeys(@NonNull String key, boolean subkeys) {
        return getDataLoader().keySetIterator(key, subkeys);
    }

    @NotNull
    @Override
    public String toString() {
        return toString("empty".equals(getDataLoader().name()) ? DataType.BYTE.name() : getDataLoader().name(), false);
    }

    @NotNull
    public String toString(@NonNull String dataTypeName) {
        return toString(dataTypeName, false);
    }

    @NotNull
    public String toString(@NonNull DataType type) {
        return toString(type.name(), false);
    }

    @NotNull
    public String toString(@NonNull String dataTypeName, boolean markSaved) {
        if (getDataLoader().name().equalsIgnoreCase(dataTypeName)) {
            return getDataLoader().saveAsString(this, markSaved);
        }
        DataLoader loader = DataLoader.findLoaderByName(dataTypeName);
        if (loader != null) {
            return getDataLoader().saveAsString(this, markSaved);
        }
        return null;
    }

    public byte[] toByteArray(@NonNull String dataTypeName) {
        return toByteArray(dataTypeName, false);
    }


    public byte[] toByteArray(@NonNull DataType type) {
        return toByteArray(type.name(), false);
    }


    public byte[] toByteArray(@NonNull DataType type, boolean markSaved) {
        return toByteArray(type.name(), markSaved);
    }

    public byte[] toByteArray(@NonNull String dataTypeName, boolean markSaved) {
        if (getDataLoader().name().equalsIgnoreCase(dataTypeName)) {
            return getDataLoader().save(this, markSaved);
        }
        DataLoader loader = DataLoader.findLoaderByName(dataTypeName);
        if (loader != null) {
            return loader.save(this, markSaved);
        }
        return null;
    }

    public Config clear() {
        getDataLoader().getPrimaryKeys().clear();
        getDataLoader().get().clear();
        getDataLoader().getHeader().clear();
        getDataLoader().getFooter().clear();
        return this;
    }

    public Config reset() {
        getDataLoader().reset();
        return this;
    }

    public boolean merge(@NonNull Config merge) {
        return merge(merge, MergeStandards.DEFAULT);
    }

    public boolean merge(@NonNull Config merge, @NonNull MergeSetting... settings) {
        for (MergeSetting setting : settings) {
            if (setting.merge(this, merge)) {
                markModified();
            }
        }
        return isModified();
    }

    @NotNull
    public DataLoader getDataLoader() {
        return loader;
    }

    public boolean isAutoUpdating() {
        return updaterTask != 0;
    }

    public void processAutoUpdate() {
        DataLoader read = DataLoader.findLoaderFor(file);
        Iterator<Entry<String, DataValue>> iterator = read.entrySet().iterator();


        while (iterator.hasNext()) {
            Entry<String, DataValue> key = iterator.next();
            DataValue val = getDataLoader().getOrCreate(key.getKey());
            if (val.modified) {
                continue;
            }
            val.value = key.getValue().value;
            val.writtenValue = key.getValue().writtenValue;
            val.comments = key.getValue().comments;
            val.commentAfterValue = key.getValue().commentAfterValue;
        }

        iterator = getDataLoader().entrySet().iterator();


        Set<String> sectionsToRemove = null;

        while (iterator.hasNext()) {
            Entry<String, DataValue> key = iterator.next();
            if (key.getValue().modified) {
                continue;
            }
            if (read.get(key.getKey()) == null) {
                if (sectionsToRemove == null) {
                    sectionsToRemove = new HashSet<>();
                }
                sectionsToRemove.add(key.getKey());
            }
        }

        if (sectionsToRemove != null) {
            for (String section : sectionsToRemove) {
                getDataLoader().remove(section);
            }
        }
    }
}