package me.petulikan1.zMachines.utils;

import me.petulikan1.zMachines.Loader;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

@SuppressWarnings("ALL")
public class PDC {
    private final PersistentDataContainer pdc;

    public <T extends PersistentDataHolder> PDC(T t) {
        Validator.notNull(t, "PersistentDataHolder cannot be null!");
        this.pdc = t.getPersistentDataContainer();
    }

    public PDC(PersistentDataContainer pdc) {
        Validator.notNull(pdc, "PersistentDataContainer cannot be null!");
        this.pdc = pdc;
    }

    public void setString(String key, String data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.STRING, data);
    }

    public void setBoolean(String key, boolean data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.BOOLEAN, data);
    }

    public void setByte(String key, byte data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE, data);
    }

    public void setByteArray(String key, byte[] data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE_ARRAY, data);
    }

    public void setInt(String key, int data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER, data);
    }

    public void setIntArray(String key, int[] data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER_ARRAY, data);
    }

    public void setLong(String key, long data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.LONG, data);
    }

    public void setLongArray(String key, long[] data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.LONG_ARRAY, data);
    }

    public void setDouble(String key, double data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.DOUBLE, data);
    }

    public void setFloat(String key, float data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.FLOAT, data);
    }

    public void setShort(String key, short data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.SHORT, data);
    }

    public void setTagContainer(String key, PersistentDataContainer data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER, data);
    }

    public void setTagContainerArray(String key, PersistentDataContainer[] data) {
        pdc.set(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER_ARRAY, data);
    }

    public String getString(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.STRING);
    }

    public boolean getBoolean(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.BOOLEAN);
    }

    public byte getByte(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE);
    }

    public byte[] getByteArray(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE_ARRAY);
    }

    public int getInt(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER);
    }

    public int[] getIntArray(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER_ARRAY);
    }

    public long getLong(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.LONG);
    }

    public long[] getLongArray(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.LONG_ARRAY);
    }

    public double getDouble(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.DOUBLE);
    }

    public float getFloat(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.FLOAT);
    }

    public short getShort(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.SHORT);
    }

    public PersistentDataContainer getTagContainer(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER);
    }

    public PersistentDataContainer[] getTagContainerArray(String key) {
        return pdc.get(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER_ARRAY);
    }

    public boolean has(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key));
    }

    public boolean hasString(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.STRING);
    }

    public boolean hasBoolean(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.BOOLEAN);
    }

    public boolean hasByte(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE);
    }

    public boolean hasByteArray(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.BYTE_ARRAY);
    }

    public boolean hasInt(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER);
    }

    public boolean hasIntArray(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.INTEGER_ARRAY);
    }

    public boolean hasLong(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.LONG);
    }

    public boolean hasLongArray(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.LONG_ARRAY);
    }

    public boolean hasDouble(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.DOUBLE);
    }

    public boolean hasFloat(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.FLOAT);
    }

    public boolean hasShort(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.SHORT);
    }

    public boolean hasTagContainer(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER);
    }

    public boolean hasTagContainerArray(String key) {
        return pdc.has(new NamespacedKey(Loader.main, key), PersistentDataType.TAG_CONTAINER_ARRAY);
    }

    public List<String> getKeys() {
        return pdc.getKeys().stream().map(NamespacedKey::getKey).toList();
    }

    public void remove(String key) {
        pdc.remove(new NamespacedKey(Loader.main, key));
    }

}
