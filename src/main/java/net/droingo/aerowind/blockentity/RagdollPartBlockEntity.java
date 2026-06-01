package net.droingo.aerowind.blockentity;

import net.droingo.aerowind.AeroWindBlockEntities;
import net.droingo.aerowind.block.RagdollPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RagdollPartBlockEntity extends BlockEntity {
    private RagdollPartBlock.PartShape partShape;
    private String ragdollRole = "";

    private String skinName = "";
    private String skinUuid = "";
    private String skinTextureValue = "";
    private String skinTextureSignature = "";

    public RagdollPartBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, RagdollPartBlock.PartShape.TORSO);
    }

    public RagdollPartBlockEntity(BlockPos pos, BlockState blockState, RagdollPartBlock.PartShape partShape) {
        super(AeroWindBlockEntities.RAGDOLL_PART.get(), pos, blockState);
        this.partShape = partShape;
    }

    public RagdollPartBlock.PartShape getPartShape() {
        return this.partShape;
    }

    public void setPartShape(RagdollPartBlock.PartShape partShape) {
        this.partShape = partShape;
        sync();
    }

    public String getRagdollRole() {
        return this.ragdollRole;
    }

    public void setRagdollRole(String ragdollRole) {
        this.ragdollRole = ragdollRole == null ? "" : ragdollRole.trim();
        sync();
    }

    public String getSkinName() {
        return this.skinName;
    }

    public String getSkinUuid() {
        return this.skinUuid;
    }

    public String getSkinTextureValue() {
        return this.skinTextureValue;
    }

    public String getSkinTextureSignature() {
        return this.skinTextureSignature;
    }

    public UUID getSkinUuidAsUuid() {
        if (this.skinUuid == null || this.skinUuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(this.skinUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void setSkin(String skinName, UUID uuid, String textureValue, String textureSignature) {
        this.skinName = skinName == null ? "" : skinName.trim();
        this.skinUuid = uuid == null ? "" : uuid.toString();
        this.skinTextureValue = textureValue == null ? "" : textureValue;
        this.skinTextureSignature = textureSignature == null ? "" : textureSignature;
        sync();
    }

    private void sync() {
        this.setChanged();

        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putString("PartShape", this.partShape.getSerializedName());
        tag.putString("RagdollRole", this.ragdollRole);

        tag.putString("SkinName", this.skinName);
        tag.putString("SkinUuid", this.skinUuid);
        tag.putString("SkinTextureValue", this.skinTextureValue);
        tag.putString("SkinTextureSignature", this.skinTextureSignature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        this.partShape = RagdollPartBlock.PartShape.byName(tag.getString("PartShape"));
        this.ragdollRole = tag.getString("RagdollRole");

        this.skinName = tag.getString("SkinName");
        this.skinUuid = tag.getString("SkinUuid");
        this.skinTextureValue = tag.getString("SkinTextureValue");
        this.skinTextureSignature = tag.getString("SkinTextureSignature");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        this.loadAdditional(packet.getTag(), registries);
    }
}