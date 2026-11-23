package com.thefolk.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * The Thing: aparece solo 20 segundos, es más rápida que The Folk, abre puertas,
 * hace ruidos saturados y hace 12 corazones de daño.
 */
public class TheThingEntity extends Monster {

    // ================= CONFIGURACIÓN =================
    public static final int LIFETIME_TICKS = 20 * 20;   // 20 segundos
    private static final double SPEED = 0.75D;           // The Folk va a 0.34 (0.68 en la fase 4)
    private static final double DAMAGE = 24.0D;          // 24 = 12 corazones
    private static final float NOISE_VOLUME = 5.0F;      // más volumen = se oye desde más lejos
    // =================================================

    private static final SoundEvent[] NOISES = {
            SoundEvents.WARDEN_ROAR,
            SoundEvents.ENDERMAN_SCREAM,
            SoundEvents.GHAST_SCREAM,
            SoundEvents.RAVAGER_ROAR,
            SoundEvents.ELDER_GUARDIAN_CURSE,
            SoundEvents.ENDER_DRAGON_GROWL
    };

    private int life;
    private int attackCooldown;
    private boolean directMove;

    public TheThingEntity(EntityType<? extends TheThingEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.xpReward = 0;
        this.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Sin goals de vanilla: toda la IA está en customServerAiStep().
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.life++;
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.life > LIFETIME_TICKS) {
            this.vanish();
            return;
        }

        if (this.life % 3 == 0) {
            this.playNoise();
        }

        Player target = this.level().getNearestPlayer(this, 96.0D);
        if (target == null) return;

        this.getLookControl().setLookAt(target, 60.0F, 60.0F);

        if (this.life % 5 == 0) {
            this.openDoorsNearby();
        }

        if (this.life % 10 == 0) {
            Path path = this.getNavigation().createPath(target, 0);
            this.directMove = path == null || !path.canReach();
            if (!this.directMove) {
                this.getNavigation().moveTo(path, 1.0D);
            }
        }
        if (this.directMove) {
            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
        }

        if (this.attackCooldown <= 0
                && this.getBoundingBox().inflate(0.8D).intersects(target.getBoundingBox())) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(target);
            this.attackCooldown = 10;
        }
    }

    /** Ruido saturado: varios gritos a todo volumen y tono bajo, superpuestos. */
    private void playNoise() {
        for (int i = 0; i < 2; i++) {
            SoundEvent sound = NOISES[this.random.nextInt(NOISES.length)];
            float pitch = 0.5F + this.random.nextFloat() * 0.6F;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    sound, SoundSource.HOSTILE, NOISE_VOLUME, pitch);
        }
    }

    /** Abre puertas, trampillas y portones que tenga cerca. */
    private void openDoorsNearby() {
        Level lvl = this.level();
        AABB box = this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D);
        for (BlockPos p : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY + 0.1D), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.minY + 1.1D), Mth.floor(box.maxZ))) {
            BlockPos pos = p.immutable();
            BlockState state = lvl.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door) {
                if (!door.isOpen(state)) {
                    door.setOpen(this, lvl, state, pos, true);
                }
            } else if (state.getBlock() instanceof FenceGateBlock) {
                if (!state.getValue(FenceGateBlock.OPEN)) {
                    lvl.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
                }
            } else if (state.getBlock() instanceof TrapDoorBlock) {
                if (!state.getValue(TrapDoorBlock.OPEN)) {
                    lvl.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, true), 10);
                }
            }
        }
    }

    private void vanish() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(),
                    30, 0.4D, 0.8D, 0.4D, 0.02D);
        }
        this.discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
