package net.droingo.aerowind.item;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.AeroWindBlocks;
import net.droingo.aerowind.link.RigidLinkSavedData;
import net.droingo.aerowind.sable.SableWindAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

public class RigidLinkRodItem extends Item {
    private static final String FIRST_X = "FirstMountX";
    private static final String FIRST_Y = "FirstMountY";
    private static final String FIRST_Z = "FirstMountZ";

    public RigidLinkRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = serverLevel.getBlockState(clickedPos);

        if (!clickedState.is(AeroWindBlocks.RIGID_LINK_MOUNT.get())) {
            return InteractionResult.PASS;
        }

        CompoundTag tag = context.getItemInHand()
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();

        if (!tag.contains(FIRST_X)) {
            tag.putInt(FIRST_X, clickedPos.getX());
            tag.putInt(FIRST_Y, clickedPos.getY());
            tag.putInt(FIRST_Z, clickedPos.getZ());

            context.getItemInHand().set(
                    DataComponents.CUSTOM_DATA,
                    CustomData.of(tag)
            );

            AeroWind.LOGGER.info("Rigid Link Rod selected first mount at {}", clickedPos);
            return InteractionResult.CONSUME;
        }

        BlockPos firstPos = new BlockPos(
                tag.getInt(FIRST_X),
                tag.getInt(FIRST_Y),
                tag.getInt(FIRST_Z)
        );

        tag.remove(FIRST_X);
        tag.remove(FIRST_Y);
        tag.remove(FIRST_Z);

        context.getItemInHand().set(
                DataComponents.CUSTOM_DATA,
                CustomData.of(tag)
        );

        if (firstPos.equals(clickedPos)) {
            AeroWind.LOGGER.info("Rigid Link Rod cancelled: second mount was the same block at {}", clickedPos);
            return InteractionResult.CONSUME;
        }

        AeroWind.LOGGER.info("Rigid Link Rod linked mounts: {} -> {}", firstPos, clickedPos);

        ServerSubLevel firstSubLevel = SableWindAccess.findSubLevelAt(serverLevel, firstPos);
        ServerSubLevel secondSubLevel = SableWindAccess.findSubLevelAt(serverLevel, clickedPos);

        UUID firstSubLevelId = getSubLevelUuid(firstSubLevel);
        UUID secondSubLevelId = getSubLevelUuid(secondSubLevel);

        if (firstSubLevelId == null || secondSubLevelId == null || firstSubLevel == null || secondSubLevel == null) {
            AeroWind.LOGGER.warn(
                    "Rigid Link Rod failed to save link: missing Sable sublevel UUID. firstPos={}, firstSubLevel={}, secondPos={}, secondSubLevel={}",
                    firstPos,
                    firstSubLevelId,
                    clickedPos,
                    secondSubLevelId
            );
            return InteractionResult.FAIL;
        }

        double rawBlockDistance = Math.sqrt(firstPos.distSqr(clickedPos));

        Vector3d firstWorldCenter = mountCenterInWorld(firstSubLevel, firstPos);
        Vector3d secondWorldCenter = mountCenterInWorld(secondSubLevel, clickedPos);
        double transformedDistance = firstWorldCenter.distance(secondWorldCenter);

        AeroWind.LOGGER.info(
                "Rigid Link Rod distance debug: rawBlockDistance={}, transformedDistance={}, firstWorldCenter={}, secondWorldCenter={}",
                rawBlockDistance,
                transformedDistance,
                firstWorldCenter,
                secondWorldCenter
        );

        double targetLength = transformedDistance;

        RigidLinkSavedData savedData = RigidLinkSavedData.get(serverLevel);
        savedData.addLink(firstSubLevelId, firstPos, secondSubLevelId, clickedPos, targetLength);

        AeroWind.LOGGER.info(
                "Rigid Link Rod saved link: {} {} -> {} {}, targetLength={}, savedLinks={}",
                firstSubLevelId,
                firstPos,
                secondSubLevelId,
                clickedPos,
                targetLength,
                savedData.linkCount()
        );
        AeroWind.LOGGER.info(
                "Rigid Link local debug: firstLocal={}, secondLocal={}",
                mountCenterInLocal(firstSubLevel, firstPos),
                mountCenterInLocal(secondSubLevel, clickedPos)
        );

        AeroWind.LOGGER.info(
                "Rigid Link plot debug A: plot={}, boundingBox={}",
                firstSubLevel.getPlot(),
                firstSubLevel.getPlot().getBoundingBox()
        );

        AeroWind.LOGGER.info(
                "Rigid Link plot debug B: plot={}, boundingBox={}",
                secondSubLevel.getPlot(),
                secondSubLevel.getPlot().getBoundingBox()
        );

        return InteractionResult.CONSUME;
    }

    private static Vector3d mountCenterInLocal(ServerSubLevel subLevel, BlockPos mountPos) {
        var bounds = subLevel.getPlot().getBoundingBox();

        return new Vector3d(
                mountPos.getX() - bounds.minX() + 0.5D,
                mountPos.getY() - bounds.minY() + 0.5D,
                mountPos.getZ() - bounds.minZ() + 0.5D
        );
    }

    @Nullable
    private static UUID getSubLevelUuid(@Nullable ServerSubLevel subLevel) {
        if (subLevel == null) {
            return null;
        }

        return subLevel.getUniqueId();
    }

    private static Vector3d mountCenterInWorld(ServerSubLevel subLevel, BlockPos mountPos) {
        Vector3d worldCenter = new Vector3d(
                mountPos.getX() + 0.5D,
                mountPos.getY() + 0.5D,
                mountPos.getZ() + 0.5D
        );

        subLevel.logicalPose().transformPosition(worldCenter);

        return worldCenter;
    }
}