package me.petulikan1.zMachines.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import me.petulikan1.zMachines.API;
import me.petulikan1.zMachines.Loader;
import me.petulikan1.zMachines.database.adapters.ItemStackAdapter;
import me.petulikan1.zMachines.dataholders.LocationInternal;
import me.petulikan1.zMachines.dataholders.Machine;
import me.petulikan1.zMachines.dataholders.MachineInternalData;
import me.petulikan1.zMachines.dataholders.MachineType;
import me.petulikan1.zMachines.dataholders.impl.CraftingMachine;
import me.petulikan1.zMachines.dataholders.impl.GrowingMachine;
import me.petulikan1.zMachines.dataholders.impl.RubbleProcessor;
import me.petulikan1.zMachines.utils.Pair;
import me.petulikan1.zMachines.utils.SQLAPI;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("SqlResolve")
public class SQLiteStorageHandler {
    private static final String TABLE = "`zMachines_data`";

    private SQLAPI sql;

    public void initialize() throws Exception {
        sql = SQLAPI.create(SQLAPI.Type.SQLITE);
        if (!sql.isConnected()) {
            throw new RuntimeException("Failed to connect to SQLite database!");
        }
        sql.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (`id` integer NOT NULL PRIMARY KEY AUTOINCREMENT, `tier` integer NOT NULL, `location` text NOT NULL UNIQUE, `owner` text NOT NULL, `machine_type` text NOT NULL, `items` text NOT NULL, `internal_data` text NOT NULL)");
        // One-shot migration: pre-rewrite tiers were 0-indexed (0/1/2); new scheme is 1/2/3.
        // Gated on presence of any tier=0 row, which only exists in pre-rewrite DBs.
        try (ResultSet rs = sql.query("SELECT COUNT(*) AS n FROM " + TABLE + " WHERE `tier` = 0")) {
            if (rs != null && rs.next() && rs.getInt("n") > 0) {
                Loader.main.info("Migrating " + rs.getInt("n") + "+ machines from old tier numbering (0/1/2) to new (1/2/3)...");
                sql.execute("UPDATE " + TABLE + " SET `tier` = `tier` + 1");
            }
        }
    }


    private final ItemStackAdapter itemStackAdapter = new ItemStackAdapter();
    private final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

    public Machine createMachine(Location location, UUID owner, Pair<MachineType, String> mt, int tier) throws Exception {
        String locationData = serializeLocation(new LocationInternal(location), mt.getValue());
        String inventoryData = serializeInventory(new HashMap<>());
        String machineInternalData = serializeMachineData(MachineInternalData.defaultData());

        try (PreparedStatement ps = sql.getPreparedStatement("INSERT INTO " + TABLE + " (`location`, `owner`, `machine_type`, `items`, `internal_data`, `tier`) VALUES (?,?,?,?,?,?)")) {
            if (ps != null) {
                ps.setString(1, locationData);
                ps.setString(2, owner.toString());
                ps.setString(3, mt.getKey().name());
                ps.setString(4, inventoryData);
                ps.setString(5, machineInternalData);
                ps.setInt(6, tier);
                ps.executeUpdate();
            }
        }
        return loadMachine(locationData);
    }

    private String serializeMachineData(MachineInternalData data) {
        try {
            return gson.toJson(data, machineDataType);
        } catch (Exception e) {
            Loader.main.error("There was an error serializing machine data!");
            throw new RuntimeException(e);
        }
    }

    private MachineInternalData deserializeMachineData(String data) {
        try {
            return gson.fromJson(data, machineDataType);
        } catch (Exception e) {
            Loader.main.error("There was an error deserializing machine data!");
            throw new RuntimeException(e);
        }
    }

    private final Type stringMapType = new TypeToken<HashMap<String, String>>() {
    }.getType();
    private final Type itemStringMapType = new TypeToken<HashMap<Integer, String>>() {
    }.getType();
    private final Type machineDataType = new TypeToken<MachineInternalData>() {
    }.getType();


    private HashMap<String, HashMap<Integer, ItemStack>> deserializeInventory(String data) {

        HashMap<String, String> map;
        try {
            map = new HashMap<>(gson.fromJson(data, stringMapType));
        } catch (Exception e) {
            Loader.main.error("There was an error deserializing inventory!");
            throw new RuntimeException(e);
        }

        HashMap<String, HashMap<Integer, ItemStack>> mainMap = new HashMap<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            HashMap<Integer, String> itemStringMap = gson.fromJson(entry.getValue(), itemStringMapType);
            HashMap<Integer, ItemStack> itemMap = new HashMap<>();
            for (Map.Entry<Integer, String> itemEntry : itemStringMap.entrySet()) {
                try {
                    itemMap.put(itemEntry.getKey(), itemStackAdapter.deserialize(new JsonPrimitive(itemEntry.getValue()), null, null));
                } catch (Exception e) {
                    e.printStackTrace();
                    Loader.main.error("There was an error deserializing item! ItemData: " + itemEntry.getValue() + " | Error: " + e.getMessage());
                }
            }
            mainMap.put(entry.getKey(), itemMap);
        }
        return mainMap;
    }

    private String serializeInventory(HashMap<String, HashMap<Integer, ItemStack>> map) {
        String data;

        HashMap<String, String> newMap = new HashMap<>();
        for (Map.Entry<String, HashMap<Integer, ItemStack>> entry : map.entrySet()) {

            HashMap<Integer, String> itemMap = new HashMap<>();
            for (Map.Entry<Integer, ItemStack> itemEntry : entry.getValue().entrySet()) {
                if (itemEntry.getValue() == null)
                    continue;
                itemMap.put(itemEntry.getKey(), itemStackAdapter.serialize(itemEntry.getValue(), null, null).getAsString());
            }
            newMap.put(entry.getKey(), gson.toJson(itemMap, itemStringMapType));
        }
        try {
            data = gson.toJson(newMap, stringMapType);
        } catch (Exception e) {
            Loader.main.error("There was an error serializing inventory!");
            throw new RuntimeException(e);
        }
        return data;
    }

    private String serializeLocation(@NotNull LocationInternal location, String nexoID) {
        String data;
        try {
            location.setNexoID(nexoID);
            data = gson.toJson(location);
        } catch (Exception e) {
            Loader.main.error("There was an error serializing location!");
            throw new RuntimeException(e);
        }
        return data;
    }

    private LocationInternal deserializeLocation(String data) {
        LocationInternal loc;
        try {
            loc = gson.fromJson(data, LocationInternal.class);
        } catch (Exception e) {
            Loader.main.error("There was an error deserializing location!");
            throw new RuntimeException(e);
        }
        return loc;
    }

    public boolean deleteMachine(@NotNull Machine machine) throws Exception {
        try (PreparedStatement ps = sql.getPreparedStatement("DELETE FROM " + TABLE + " WHERE `location` = ? AND `id` = ?")) {
            ps.setString(1, serializeLocation(machine.getLocation(), machine.getNexoItemID()));
            ps.setInt(2, machine.getId());
            ps.executeUpdate();

            Loader.machines.remove(machine.getLocation());
            return true;
        }
    }

    @Nullable
    public Machine loadMachine(String data) throws Exception {
        try (PreparedStatement ps = sql.getPreparedStatement("SELECT * FROM " + TABLE + " WHERE `location` = ?")) {
            if (ps == null)
                return null;
            ps.setString(1, data);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs != null && rs.next()) {
                    int machineID = rs.getInt("id");
                    String location = rs.getString("location");
                    String owner = rs.getString("owner");
                    String machineType = rs.getString("machine_type");
                    String items = rs.getString("items");
                    String machineData = rs.getString("internal_data");
                    try {
                        UUID ownerUUID = UUID.fromString(owner);
                        LocationInternal locationInternal = deserializeLocation(location);
                        MachineType mt = MachineType.valueOf(machineType);
                        HashMap<String, HashMap<Integer, ItemStack>> inventoryItems = deserializeInventory(items);
                        MachineInternalData internalData = deserializeMachineData(machineData);

                        Machine machine = null;
                        if (mt == MachineType.RUBBLE_PROCESSOR) {
                            machine = new RubbleProcessor(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                        } else if (mt == MachineType.GROWING_MACHINE) {
                            machine = new GrowingMachine(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                        } else if (mt == MachineType.CRAFTING_MACHINE) {
                            machine = new CraftingMachine(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                        }
                        if (machine == null)
                            continue;
                        machine.setMachineInternalData(internalData);
                        machine.setInventoryItems(inventoryItems);
                        machine.setNexoItemID(locationInternal.getNexoID());
                        locationInternal.setNexoID(null);
                        Loader.machines.put(API.getLocationKeyed(locationInternal), machine);
                        return machine;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Loader.main.error("Failed to load machine: " + machineID + " | " + location);
                        return null;
                    }
                }
            }
        }
        return null;
    }


    public void updateMachine(Machine machine) throws Exception {
        String inventoryData = serializeInventory(machine.getInventoryItems());
        String locationData = serializeLocation(machine.getLocation(), machine.getNexoItemID());
        String internalData = serializeMachineData(machine.getMachineInternalData());
        try (PreparedStatement ps = sql.getPreparedStatement("UPDATE " + TABLE + " SET `items` = ?, `internal_data` = ? WHERE `id` = ? AND `location` = ?")) {
            if (ps != null) {
                ps.setString(1, inventoryData);
                ps.setString(2, internalData);
                ps.setInt(3, machine.getId());
                ps.setString(4, locationData);
                ps.executeUpdate();
            }
        }
    }

    public void loadData() throws Exception {
        int bad = 0;
        try (ResultSet rs = sql.query("SELECT  * from " + TABLE)) {
            while (rs != null && rs.next()) {
                int machineID = rs.getInt("id");
                String location = rs.getString("location");
                String owner = rs.getString("owner");
                String machineType = rs.getString("machine_type");
                String items = rs.getString("items");
                String internalData = rs.getString("internal_data");
                try {
                    UUID ownerUUID = UUID.fromString(owner);
                    LocationInternal locationInternal = deserializeLocation(location);
                    MachineType mt = MachineType.valueOf(machineType);
                    HashMap<String, HashMap<Integer, ItemStack>> inventoryItems = deserializeInventory(items);
                    MachineInternalData machineData = deserializeMachineData(internalData);
                    Machine machine = null;
                    if (mt == MachineType.RUBBLE_PROCESSOR) {
                        machine = new RubbleProcessor(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                    } else if (mt == MachineType.GROWING_MACHINE) {
                        machine = new GrowingMachine(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                    } else if (mt == MachineType.CRAFTING_MACHINE) {
                        machine = new CraftingMachine(locationInternal, machineID, ownerUUID, rs.getInt("tier"));
                    }
                    if (machine == null)
                        continue;
                    machine.setInventoryItems(inventoryItems);
                    machine.setMachineInternalData(machineData);
                    machine.setNexoItemID(locationInternal.getNexoID());
                    locationInternal.setNexoID(null);

                    Loader.machines.put(API.getLocationKeyed(locationInternal), machine);
                } catch (Exception e) {
                    e.printStackTrace();
                    bad++;
                    Loader.main.error("Failed to load machine: " + machineID + " | " + location);
                }
            }
        }
        Loader.main.info("Successfully loaded " + Loader.machines.size() + " machines!");
        if (bad != 0) {
            Loader.main.error("Failed to load " + bad + " machines!");
        }
    }

    public void close() {
        if (sql != null)
            sql.close();
    }

}
