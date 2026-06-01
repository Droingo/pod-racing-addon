package net.droingo.aerowind.client;

import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.wind.WindField;
import net.droingo.aerowind.wind.WindSample;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AeroWind.MOD_ID, value = Dist.CLIENT)
public final class AeroWindClientEvents {
    private AeroWindClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        RandomSource random = minecraft.level.random;

        if (random.nextFloat() > 0.18F) {
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        WindSample wind = WindField.sampleClientVisual(minecraft.level, playerPos);
        Vec3 direction = wind.direction();

        double side = (random.nextDouble() - 0.5D) * 60.0D;
        double forward = -30.0D + random.nextDouble() * 20.0D;

        Vec3 sideways = new Vec3(-direction.z, 0.0D, direction.x).normalize();

        double x = playerPos.x + sideways.x * side + direction.x * forward;
        double y = playerPos.y + 10.0D + random.nextDouble() * 24.0D;
        double z = playerPos.z + sideways.z * side + direction.z * forward;

        /*
         * Particles now travel with the same visual wind direction instead of
         * being reversed from it.
         */
        Vec3 velocity = direction.scale(0.28D);

        minecraft.level.addParticle(
                ParticleTypes.CLOUD,
                x,
                y,
                z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }
}