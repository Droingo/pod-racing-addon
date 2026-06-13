package net.droingo.podracing.client.binder;

import net.droingo.podracing.content.binder.EnergyBinderConnectionSnapshot;
import net.droingo.podracing.content.binder.EnergyBinderEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EnergyBinderClientState {
    private static List<EnergyBinderConnectionSnapshot> connections = List.of();
    private static EnergyBinderEndpoint selectedEndpoint;

    private EnergyBinderClientState() {
    }

    public static void replaceConnections(List<EnergyBinderConnectionSnapshot> syncedConnections) {
        connections = List.copyOf(syncedConnections);
    }

    public static List<EnergyBinderConnectionSnapshot> connections() {
        return new ArrayList<>(connections);
    }

    public static void setSelectedEndpoint(EnergyBinderEndpoint endpoint) {
        selectedEndpoint = endpoint;
    }

    public static void clearSelectedEndpoint() {
        selectedEndpoint = null;
    }

    public static Optional<EnergyBinderEndpoint> selectedEndpoint() {
        return Optional.ofNullable(selectedEndpoint);
    }

    public static void clear() {
        connections = List.of();
        selectedEndpoint = null;
    }
}