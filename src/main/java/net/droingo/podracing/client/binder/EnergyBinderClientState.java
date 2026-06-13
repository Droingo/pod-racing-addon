package net.droingo.podracing.client.binder;

import net.droingo.podracing.content.binder.EnergyBinderConnectionSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class EnergyBinderClientState {
    private static List<EnergyBinderConnectionSnapshot> connections = List.of();

    private EnergyBinderClientState() {
    }

    public static void replaceConnections(List<EnergyBinderConnectionSnapshot> syncedConnections) {
        connections = List.copyOf(syncedConnections);
    }

    public static List<EnergyBinderConnectionSnapshot> connections() {
        return new ArrayList<>(connections);
    }

    public static void clear() {
        connections = List.of();
    }
}