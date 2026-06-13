package net.droingo.podracing.content.binder;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EnergyBinderSavedData extends SavedData {
    private static final String DATA_NAME = "pod_racing_addon_energy_binders";
    private static final String TAG_CONNECTIONS = "connections";

    private final Map<UUID, EnergyBinderConnection> connections = new LinkedHashMap<>();

    public static EnergyBinderSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();

        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        EnergyBinderSavedData::new,
                        EnergyBinderSavedData::load
                ),
                DATA_NAME
        );
    }

    public Collection<EnergyBinderConnection> connections() {
        return connections.values();
    }

    public Optional<EnergyBinderConnection> findConnection(EnergyBinderEndpoint first, EnergyBinderEndpoint second) {
        return connections.values()
                .stream()
                .filter(connection -> connection.connects(first, second))
                .findFirst();
    }

    public EnergyBinderConnection addConnection(EnergyBinderEndpoint first, EnergyBinderEndpoint second) {
        Optional<EnergyBinderConnection> existing = findConnection(first, second);

        if (existing.isPresent()) {
            return existing.get();
        }

        EnergyBinderConnection connection = EnergyBinderConnection.create(first, second);
        connections.put(connection.id(), connection);
        setDirty();
        return connection;
    }

    public int removeConnectionsTouching(EnergyBinderEndpoint endpoint) {
        int before = connections.size();

        connections.entrySet().removeIf(entry -> entry.getValue().touches(endpoint));

        int removed = before - connections.size();

        if (removed > 0) {
            setDirty();
        }

        return removed;
    }

    public boolean removeConnection(UUID connectionId) {
        EnergyBinderConnection removed = connections.remove(connectionId);

        if (removed != null) {
            setDirty();
            return true;
        }

        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (EnergyBinderConnection connection : connections.values()) {
            list.add(connection.save());
        }

        tag.put(TAG_CONNECTIONS, list);
        return tag;
    }

    public static EnergyBinderSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EnergyBinderSavedData data = new EnergyBinderSavedData();

        ListTag list = tag.getList(TAG_CONNECTIONS, Tag.TAG_COMPOUND);

        for (int index = 0; index < list.size(); index++) {
            EnergyBinderConnection connection = EnergyBinderConnection.load(list.getCompound(index));
            data.connections.put(connection.id(), connection);
        }

        return data;
    }
}