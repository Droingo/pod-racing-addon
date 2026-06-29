package net.droingo.podracing.client.pilot;

import com.mojang.blaze3d.platform.InputConstants;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.network.payload.UpdatePilotInputPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = PodRacingAddon.MOD_ID,
        value = Dist.CLIENT
)
public final class PodPilotClient {
    private static final float ROLL_TO_YAW_MIX = 0.35F;
    private static final String CATEGORY = "key.categories.pod_racing_addon";

    private static final KeyMapping TOGGLE_PILOT = new KeyMapping(
            "key.pod_racing_addon.toggle_pilot",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY
    );

    private static boolean pilotActive = false;
    private static BlockPos activeControlCorePos;

    private static float lastSentPitch = 999.0F;
    private static float lastSentRoll = 999.0F;
    private static float lastSentYaw = 999.0F;
    private static boolean lastSentActive = false;

    private static int sendCooldown = 0;
    private static int debugCooldown = 0;

    private PodPilotClient() {
    }

    public static void toggleFromControlBlock(BlockPos controlCorePos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (pilotActive && controlCorePos.equals(activeControlCorePos)) {
            setPilotActive(minecraft, false, activeControlCorePos, "Pod Control Core");
        } else {
            setPilotActive(minecraft, true, controlCorePos, "Pod Control Core");
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            pilotActive = false;
            activeControlCorePos = null;
            return;
        }

        while (TOGGLE_PILOT.consumeClick()) {
            if (pilotActive) {
                setPilotActive(minecraft, false, activeControlCorePos, "Pod pilot prototype");
            } else {
                setPilotActive(minecraft, true, null, "Pod pilot prototype");
            }
        }

        float roll = 0.0F;
        float pitch = 0.0F;
        float yaw = 0.0F;

        if (pilotActive) {
            if (minecraft.options.keyLeft.isDown()) {
                roll -= 1.0F;
            }

            if (minecraft.options.keyRight.isDown()) {
                roll += 1.0F;
            }

            if (minecraft.options.keyUp.isDown()) {
                pitch -= 1.0F;
            }

            if (minecraft.options.keyDown.isDown()) {
                pitch += 1.0F;
            }

            long window = minecraft.getWindow().getWindow();

            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS) {
            yaw += 1.0F;
        }

            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS) {
            yaw -= 1.0F;
        }
        }

        if (sendCooldown > 0) {
            sendCooldown--;
        }

        if (debugCooldown > 0) {
            debugCooldown--;
        }

        boolean changed = pilotActive != lastSentActive
                || Math.abs(pitch - lastSentPitch) > 0.001F
                || Math.abs(roll - lastSentRoll) > 0.001F
                || Math.abs(yaw - lastSentYaw) > 0.001F;

        if (changed || (pilotActive && sendCooldown <= 0)) {
            sendNow(minecraft, pitch, roll, yaw, activeControlCorePos);
        }

        if (pilotActive && debugCooldown <= 0 && Math.abs(yaw) > 0.001F) {
            minecraft.player.displayClientMessage(
                    Component.literal("Yaw input: " + yaw),
                    true
            );
            debugCooldown = 10;
        }
    }

    private static void setPilotActive(
            Minecraft minecraft,
            boolean active,
            BlockPos controlCorePos,
            String sourceName
    ) {
        pilotActive = active;

        if (active) {
            activeControlCorePos = controlCorePos;
        }

        minecraft.player.displayClientMessage(
                Component.literal(sourceName + ": " + (pilotActive ? "ON" : "OFF")),
                true
        );

        sendNow(minecraft, 0.0F, 0.0F, 0.0F, active ? activeControlCorePos : controlCorePos);

        if (!active) {
            activeControlCorePos = null;
        }
    }

    private static void sendNow(
            Minecraft minecraft,
            float pitch,
            float roll,
            float yaw,
            BlockPos controlCorePos
    ) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        float mixedYaw = clampAxis(yaw - roll * ROLL_TO_YAW_MIX);

        PacketDistributor.sendToServer(new UpdatePilotInputPayload(
                pilotActive,
                pitch,
                roll,
                yaw,
                controlCorePos != null,
                controlCorePos == null ? BlockPos.ZERO : controlCorePos
        ));

        lastSentActive = pilotActive;
        lastSentPitch = pitch;
        lastSentRoll = roll;
        lastSentYaw = mixedYaw;
        sendCooldown = 4;
    }

    @EventBusSubscriber(
            modid = PodRacingAddon.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_PILOT);
        }
    }

    private static float clampAxis(float value) {
        if (value < -1.0F) {
            return -1.0F;
        }

        if (value > 1.0F) {
            return 1.0F;
        }

        return value;
    }
}