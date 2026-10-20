package com.carmod.entity;

import java.util.UUID;

import com.carmod.NukeIndex;
import com.carmod.registry.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NukeEntity extends Entity {
	private UUID owner;

	public NukeEntity(EntityType<? extends NukeEntity> type, Level level) {
		super(type, level);
		this.noPhysics = false;
		this.setNoGravity(true);
	}

	public UUID owner() {
		return this.owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			if (this.tickCount % 8 == 0) {
				this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.9, this.getZ(), 0.0, 0.02, 0.0);
			}
			return;
		}
		NukeIndex.add(this);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
			return;
		}
		NukeIndex.remove(this);
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.isRemoved() || !(source.getEntity() instanceof Player player) || player.isSpectator()) {
			return false;
		}
		if (!player.getAbilities().instabuild) {
			this.spawnAtLocation(level, new ItemStack(ModItems.NUKE));
		}
		this.discard();
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		input.getString("Owner").ifPresent(raw -> {
			try {
				this.owner = UUID.fromString(raw);
			} catch (IllegalArgumentException ignored) {
				this.owner = null;
			}
		});
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (this.owner != null) {
			output.putString("Owner", this.owner.toString());
		}
	}
}
