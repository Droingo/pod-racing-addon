package net.droingo.podracing.content.binder;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public record EnergyBinderConnectionSnapshot(
        UUID id,
        EnergyBinderEndpoint endpointA,
        EnergyBinderEndpoint endpointB,
        double targetDistance,
        double stiffness,
        double damping,
        int color,
        boolean enabled,
        boolean active
) {
    public static EnergyBinderConnectionSnapshot from(EnergyBinderConnection connection, Level level) {
        boolean active = connection.enabled()
                && (isEndpointPowered(level, connection.endpointA())
                || isEndpointPowered(level, connection.endpointB()));

        return new EnergyBinderConnectionSnapshot(
                connection.id(),
                connection.endpointA(),
                connection.endpointB(),
                connection.targetDistance(),
                connection.stiffness(),
                connection.damping(),
                connection.color(),
                connection.enabled(),
                active
        );
    }

    private static boolean isEndpointPowered(Level level, EnergyBinderEndpoint endpoint) {
        if (!endpoint.dimension().equals(level.dimension())) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(endpoint.pos());

        if (blockEntity instanceof BinderMountBlockEntity binderMount) {
            return binderMount.isPowered();
        }

        return level.hasNeighborSignal(endpoint.pos());
    }
}