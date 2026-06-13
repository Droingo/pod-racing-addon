package net.droingo.podracing.content.binder;

import java.util.UUID;

public record EnergyBinderConnectionSnapshot(
        UUID id,
        EnergyBinderEndpoint endpointA,
        EnergyBinderEndpoint endpointB,
        double targetDistance,
        double stiffness,
        double damping,
        int color,
        boolean enabled
) {
    public static EnergyBinderConnectionSnapshot from(EnergyBinderConnection connection) {
        return new EnergyBinderConnectionSnapshot(
                connection.id(),
                connection.endpointA(),
                connection.endpointB(),
                connection.targetDistance(),
                connection.stiffness(),
                connection.damping(),
                connection.color(),
                connection.enabled()
        );
    }
}