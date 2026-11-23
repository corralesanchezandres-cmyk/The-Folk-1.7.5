package com.thefolk.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Folk: un "dweller" con 3 fases.
 *  Fase 1: solo mira.
 *  Fase 2: se acerca caminando.
 *  Fase 3: persigue rapido, rompe bloques y ataca durante 1 minuto.
 */
public class TheFolkEntity extends Monster {
    public static final int PHASE_WATCH = 1;
    public static final int PHASE_WALK = 2;
    public static final int PHASE_CHASE = 3;
    public static final int PHASE_FRENZY = 4;   // solo en modo ERROR / THE FOLK IS COMING: velocidad x2

    // ================= CONFIGURACIÓN =================
    public static final int CHASE_TICKS = 20 * 60;          // la persecución dura 1 minuto
    private static final double SPEED_WATCH = 0.20D;
    private static final double SPEED_WALK = 0.17D;          // más lento que correr
    private static final double SPEED_CHASE = 0.34D;         // más rápido que el jugador corriendo
    private static final double ATTACK_DAMAGE = 6.0D;        // 3 corazones por golpe
    private static final double WATCH_VANISH_DISTANCE = 12.0D;
    // =================================================

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(TheFolkEntity.class, EntityDataSerializers.INT);

    private int life;
    private int closeTicks;
    private int attackCooldown;
    private boolean directMove;

    public TheFolkEntity(EntityType<? extends TheFolkEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED_WATCH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_WATCH);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        double speed;
        if (phase == PHASE_FRENZY) {
            speed = SPEED_CHASE * 2.0D;
        } else if (phase == PHASE_CHASE) {
            speed = SPEED_CHASE;
        } else if (phase == PHASE_WALK) {
            speed = SPEED_WALK;
        } else {
            speed = SPEED_WATCH;
        }
        AttributeInstance attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.setBaseValue(speed);
        }
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

        Player target = this.level().getNearestPlayer(this, 96.0D);
        if (target == null) {
            if (this.life > 200) {
                this.vanish(false);
            }
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dist = this.distanceTo(target);

        switch (this.getPhase()) {
            case PHASE_WATCH:
                this.tickWatch(dist);
                break;
            case PHASE_WALK:
                this.tickWalk(target, dist);
                break;
            default:
                this.tickChase(target);
                break;
        }
    }

    private void tickWatch(double dist) {
        this.getNavigation().stop();
        if (dist < WATCH_VANISH_DISTANCE || this.life > 2400) {
            this.vanish(true);
        }
    }

    private void tickWalk(Player target, double dist) {
        if (dist > 5.0D) {
            this.closeTicks = 0;
            if (this.life % 10 == 0 || this.getNavigation().isDone()) {
                this.getNavigation().moveTo(target, 1.0D);
            }
        } else {
            this.getNavigation().stop();
            if (++this.closeTicks > 60) {
                this.vanish(true);
                return;
            }
        }
        if (this.life > 6000) {
            this.vanish(true);
        }
    }

    private void tickChase(Player target) {
        if (this.life > CHASE_TICKS) {
            this.vanish(true);
            return;
        }

        if (this.life % 10 == 0) {
            Path path = this.getNavigation().createPath(target, 0);
            this.directMove = path == null || !path.canReach();
            if (!this.directMove) {
                this.getNavigation().moveTo(path, 1.0D);
            }
        }
        if (this.directMove) {
            // Sin camino posible: va en línea recta y rompe lo que se cruce.
            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
        }

        if (this.life % (this.getPhase() == PHASE_FRENZY ? 2 : 4) == 0) {
            this.breakBlocks(target);
        }

        if (this.attackCooldown <= 0
                && this.getBoundingBox().inflate(0.8D).intersects(target.getBoundingBox())) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(target);
            this.attackCooldown = 20;
        }
    }

    private void breakBlocks(Player target) {
        Level lvl = this.level();
        if (!(lvl instanceof ServerLevel)) return;
        if (!lvl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;

        Vec3 dir = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (dir.lengthSqr() > 0.0001D) {
            dir = dir.normalize();
        }

        AABB box = this.getBoundingBox().inflate(0.35D, 0.0D, 0.35D).move(dir.x * 0.6D, 0.0D, dir.z * 0.6D);
        if (target.getY() < this.getY() - 1.5D) {
            box = box.expandTowards(0.0D, -1.0D, 0.0D); // si el jugador está más abajo, rompe el suelo
        }

        int broken = 0;
        for (BlockPos p : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY + 0.1D), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockPos pos = p.immutable();
            BlockState state = lvl.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(lvl, pos) < 0.0F) continue;   // bedrock, etc.
            if (!state.getFluidState().isEmpty()) continue;
            lvl.destroyBlock(pos, false, this);
            if (++broken >= 6) break;
        }
    }

    private void vanish(boolean effects) {
        if (effects && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.4D, this.getZ(),
                    25, 0.3D, 0.8D, 0.3D, 0.02D);
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        this.discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        int phase = this.getPhase();
        if (phase >= PHASE_CHASE) return SoundEvents.WARDEN_NEARBY_CLOSEST;
        if (phase == PHASE_WALK) return SoundEvents.WARDEN_NEARBY_CLOSE;
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FolkPhase", this.getPhase());
        tag.putInt("FolkLife", this.life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int phase = tag.getInt("FolkPhase");
        this.setPhase(phase >= 1 && phase <= 4 ? phase : PHASE_WATCH);
        this.life = tag.getInt("FolkLife");
    }
}
