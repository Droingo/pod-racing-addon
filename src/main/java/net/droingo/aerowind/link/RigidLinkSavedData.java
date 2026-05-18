package net.droingo.aerowind.link;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RigidLinkSavedData extends SavedData {
    private static final String NAME = "aerowind_rigid_links";

    private final List<RigidLink> links = new ArrayList<>();

    public static RigidLinkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        RigidLinkSavedData::new,
                        RigidLinkSavedData::load,
                        null
                ),
                NAME
        );
    }

    public static RigidLinkSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        RigidLinkSavedData data = new RigidLinkSavedData();

        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag linkTag = list.getCompound(i);

            UUID subLevelA = linkTag.getUUID("SubLevelA");
            UUID subLevelB = linkTag.getUUID("SubLevelB");

            BlockPos posA = new BlockPos(
                    linkTag.getInt("AX"),
                    linkTag.getInt("AY"),
                    linkTag.getInt("AZ")
            );

            BlockPos posB = new BlockPos(
                    linkTag.getInt("BX"),
                    linkTag.getInt("BY"),
                    linkTag.getInt("BZ")
            );

            double targetLength = linkTag.getDouble("TargetLength");

            data.links.add(new RigidLink(subLevelA, posA, subLevelB, posB, targetLength));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();

        for (RigidLink link : links) {
            CompoundTag linkTag = new CompoundTag();

            linkTag.putUUID("SubLevelA", link.subLevelA());
            linkTag.putInt("AX", link.posA().getX());
            linkTag.putInt("AY", link.posA().getY());
            linkTag.putInt("AZ", link.posA().getZ());

            linkTag.putUUID("SubLevelB", link.subLevelB());
            linkTag.putInt("BX", link.posB().getX());
            linkTag.putInt("BY", link.posB().getY());
            linkTag.putInt("BZ", link.posB().getZ());

            linkTag.putDouble("TargetLength", link.targetLength());

            list.add(linkTag);
        }

        tag.put("Links", list);
        return tag;
    }



    public void addLink(UUID subLevelA, BlockPos posA, UUID subLevelB, BlockPos posB, double targetLength) {
        links.add(new RigidLink(subLevelA, posA, subLevelB, posB, targetLength));
        setDirty();
    }

    public void clearLinks() {
        links.clear();
        setDirty();
    }

    public int linkCount() {
        return links.size();
    }

    public List<RigidLink> links() {
        return List.copyOf(links);
    }

    public record RigidLink(
            UUID subLevelA,
            BlockPos posA,
            UUID subLevelB,
            BlockPos posB,
            double targetLength
    ) {
    }
}