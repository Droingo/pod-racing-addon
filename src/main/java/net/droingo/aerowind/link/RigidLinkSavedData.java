package net.droingo.aerowind.link;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.joml.Vector3d;

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

            String linkKind = linkTag.getString("LinkKind");
            if (linkKind == null || linkKind.isBlank()) {
                linkKind = "rigid";
            }

            String label = linkTag.getString("Label");

            Vector3d anchorOffsetA = readVector(linkTag, "AnchorOffsetA");
            Vector3d anchorOffsetB = readVector(linkTag, "AnchorOffsetB");

            data.links.add(new RigidLink(
                    subLevelA,
                    posA,
                    subLevelB,
                    posB,
                    targetLength,
                    linkKind,
                    label,
                    anchorOffsetA,
                    anchorOffsetB
            ));
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

            linkTag.putString("LinkKind", link.linkKind());
            linkTag.putString("Label", link.label());

            writeVector(linkTag, "AnchorOffsetA", link.anchorOffsetA());
            writeVector(linkTag, "AnchorOffsetB", link.anchorOffsetB());

            list.add(linkTag);
        }

        tag.put("Links", list);
        return tag;
    }

    public void addLink(UUID subLevelA, BlockPos posA, UUID subLevelB, BlockPos posB, double targetLength) {
        RigidLink link = new RigidLink(
                subLevelA,
                posA,
                subLevelB,
                posB,
                targetLength,
                "rigid",
                "",
                new Vector3d(0.0D, 0.0D, 0.0D),
                new Vector3d(0.0D, 0.0D, 0.0D)
        );

        if (!links.contains(link)) {
            links.add(link);
            setDirty();
        }
    }

    public void addRagdollLink(
            UUID subLevelA,
            BlockPos posA,
            UUID subLevelB,
            BlockPos posB,
            String label,
            Vector3d anchorOffsetA,
            Vector3d anchorOffsetB
    ) {
        double targetLength = Math.sqrt(posA.distSqr(posB));

        RigidLink link = new RigidLink(
                subLevelA,
                posA,
                subLevelB,
                posB,
                targetLength,
                "ragdoll",
                label == null ? "" : label,
                new Vector3d(anchorOffsetA),
                new Vector3d(anchorOffsetB)
        );

        if (!links.contains(link)) {
            links.add(link);
            setDirty();
        }
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

    private static void writeVector(CompoundTag tag, String name, Vector3d vector) {
        CompoundTag vectorTag = new CompoundTag();
        vectorTag.putDouble("X", vector.x);
        vectorTag.putDouble("Y", vector.y);
        vectorTag.putDouble("Z", vector.z);
        tag.put(name, vectorTag);
    }

    private static Vector3d readVector(CompoundTag tag, String name) {
        if (!tag.contains(name, Tag.TAG_COMPOUND)) {
            return new Vector3d(0.0D, 0.0D, 0.0D);
        }

        CompoundTag vectorTag = tag.getCompound(name);

        return new Vector3d(
                vectorTag.getDouble("X"),
                vectorTag.getDouble("Y"),
                vectorTag.getDouble("Z")
        );
    }

    public record RigidLink(
            UUID subLevelA,
            BlockPos posA,
            UUID subLevelB,
            BlockPos posB,
            double targetLength,
            String linkKind,
            String label,
            Vector3d anchorOffsetA,
            Vector3d anchorOffsetB
    ) {
    }
}