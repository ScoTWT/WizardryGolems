package com.windanesz.wizardrygolems.entity.living;

import com.golems.main.ExtraGolems;
import com.golems.util.GolemNames;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Based on {@link com.golems.entity.EntityCraftingGolem} - Author: skyjay1
 * Author: WinDanesz
 */
public class EntityTNTGolemMinion extends EntityGolemBaseMinion {

	protected static final DataParameter<Boolean> DATA_IGNITED = EntityDataManager
			.<Boolean>createKey(com.golems.entity.EntityTNTGolem.class, DataSerializers.BOOLEAN);
	public static final String ALLOW_SPECIAL = "Allow Special: Explode";

	protected final int minExplosionRad;
	protected final int maxExplosionRad;
	protected final int fuseLen;
	/**
	 * Percent chance to explode while attacking a mob.
	 **/
	protected final int chanceToExplodeWhenAttacking;
	protected boolean allowedToExplode = false;

	protected boolean willExplode;
	protected int fuseTimer;

	public EntityTNTGolemMinion(final World world, final int minExplosionRange, final int maxExplosionRange,
			final int minFuseLength, final int randomExplosionChance) {
		super(world);
		this.minExplosionRad = minExplosionRange;
		this.maxExplosionRad = maxExplosionRange;
		this.fuseLen = minFuseLength;
		this.chanceToExplodeWhenAttacking = randomExplosionChance;
		this.resetIgnite();
	}

	/**
	 * Default constructor for TNT golem.
	 **/
	public EntityTNTGolemMinion(final World world) {
		this(world, 3, 6, 50, 10);
		this.setLootTableLoc(GolemNames.TNT_GOLEM);
		this.addHealItem(new ItemStack(Items.GUNPOWDER), 0.25D);
		this.addHealItem(new ItemStack(Blocks.SAND), 0.25D);
		this.allowedToExplode = getConfig(this).getBoolean(ALLOW_SPECIAL);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.26D);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(DATA_IGNITED, Boolean.valueOf(false));
	}

	@Override
	protected ResourceLocation applyTexture() {
		return makeTexture(ExtraGolems.MODID, GolemNames.TNT_GOLEM);
	}

	/**
	 * Called frequently so the entity can update its state every tick as required.
	 * For example, zombies and skeletons use this to react to sunlight and start to
	 * burn.
	 */
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (this.isBurning()) {
			this.ignite();
		}

		if (this.isWet() || (this.getAttackTarget() != null
				&& this.getDistanceSq(this.getAttackTarget()) > this.minExplosionRad * this.maxExplosionRad)) {
			this.resetIgnite();
		}

		if (this.isIgnited()) {
			this.motionX = this.motionZ = 0;
			this.fuseTimer--;
			if (this.world instanceof WorldServer) {
				for (int i = 0; i < 2; i++) {
					this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + 2.0D, this.posZ, 0.0D, 0.0D,
							0.0D);
					this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX + 0.75D,
							this.posY + 1.0D + rand.nextDouble() * 2, this.posZ + 0.75D, 0.5 * (0.5D - rand.nextDouble()), 0.0D,
							0.5 * (0.5D - rand.nextDouble()));
					this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX + 0.75D,
							this.posY + 1.0D + rand.nextDouble() * 2, this.posZ - 0.75D, 0.5 * (0.5D - rand.nextDouble()), 0.0D,
							0.5 * (0.5D - rand.nextDouble()));
					this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX - 0.75D,
							this.posY + 1.0D + rand.nextDouble() * 2, this.posZ + 0.75D, 0.5 * (0.5D - rand.nextDouble()), 0.0D,
							0.5 * (0.5D - rand.nextDouble()));
					this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX - 0.75D,
							this.posY + 1.0D + rand.nextDouble() * 2, this.posZ - 0.75D, 0.5 * (0.5D - rand.nextDouble()), 0.0D,
							0.5 * (0.5D - rand.nextDouble()));
				}
			}
			if (this.fuseTimer <= 0) {
				this.willExplode = true;
			}
		}

		if (this.willExplode) {
			this.explode();
		}
	}

	@Override
	public void onUpdate(){
		super.onUpdate();
		this.updateDelegate();
	}

	@Override
	public void onDeath(final DamageSource source) {
		super.onDeath(source);
		this.explode();
	}

	@Override
	public void onSuccessfulAttack(EntityLivingBase target) {
		if (!target.isDead && rand.nextInt(100) < this.chanceToExplodeWhenAttacking
				&& this.getDistanceSq(target) <= this.minExplosionRad * this.minExplosionRad) {
			this.ignite();
		}
	}

	@Override
	protected boolean processInteract(final EntityPlayer player, final EnumHand hand) {
		final ItemStack itemstack = player.getHeldItem(hand);
		if (!itemstack.isEmpty() && itemstack.getItem() == Items.FLINT_AND_STEEL) {
			this.world.playSound(player, this.posX, this.posY, this.posZ, SoundEvents.ITEM_FLINTANDSTEEL_USE,
					this.getSoundCategory(), 1.0F, this.rand.nextFloat() * 0.4F + 0.8F);
			player.swingArm(hand);

			if (!this.world.isRemote) {
				this.setFire(Math.floorDiv(this.fuseLen, 20));
				this.ignite();
				itemstack.damageItem(1, player);
			}
		}

		return super.processInteract(player, hand);
	}

	protected void resetFuse() {
		this.fuseTimer = this.fuseLen + rand.nextInt(Math.floorDiv(fuseLen, 2) + 1);
	}

	protected void setIgnited(final boolean toSet) {
		this.getDataManager().set(DATA_IGNITED, Boolean.valueOf(toSet));
	}

	protected boolean isIgnited() {
		return this.getDataManager().get(DATA_IGNITED).booleanValue();
	}

	protected void ignite() {
		if (!this.isIgnited()) {
			// update info
			this.setIgnited(true);
			this.resetFuse();
			// play sounds
			if (!this.isWet()) {
				this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 0.9F, rand.nextFloat());
			}
		}
	}

	protected void resetIgnite() {
		this.setIgnited(false);
		this.resetFuse();
		this.willExplode = false;
	}

	protected void explode() {
		if (this.allowedToExplode) {
			if (!this.world.isRemote) {
				final boolean flag = this.world.getGameRules().getBoolean("mobGriefing");
				final float range = this.maxExplosionRad > this.minExplosionRad
						? (minExplosionRad + rand.nextInt(maxExplosionRad - minExplosionRad))
						: this.minExplosionRad;
				this.world.createExplosion(this, this.posX, this.posY, this.posZ, range, flag);
				this.setDead();
			}
		} else {
			resetIgnite();
		}
	}

	@Override
	public SoundEvent getGolemSound() {
		return SoundEvents.BLOCK_GRAVEL_STEP;
	}
}
