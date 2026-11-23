package com.thefolk;

import com.thefolk.entity.TheFolkEntity;
import com.thefolk.entity.TheThingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controla el ciclo de fases de The Folk, el modo del FolkConfig y cuándo aparecen The Folk y The Thing.
 * Todo lo editable está en el bloque CONFIGURACIÓN.
 */
@Mod.EventBusSubscriber(modid = TheFolkMod.MOD_ID)
public class FolkDirector {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_ERROR = 1;
    public static final int MODE_COMING = 2;

    // ================= CONFIGURACIÓN =================
    /** Días que dura cada fase [fase1, fase2, fase3, fase4] en modo Normal. */
    private static final int[] NORMAL_DAYS = {5, 8, 5, 0};
    /** Días por fase en modo ERROR y THE FOLK IS COMING (más rápido + fase 4). */
    private static final int[] ERROR_DAYS = {2, 4, 2, 2};

    /** Espera entre apariciones de The Folk, en ticks (20 ticks = 1 segundo). Índice = fase. */
    private static final int[] COOLDOWN_MIN = {0, 3000, 2400, 6000, 4800};
    private static final int[] COOLDOWN_MAX = {0, 6000, 6000, 12000, 9600};

    /** Distancia al jugador a la que aparece The Folk, en bloques. Índice = fase. */
    private static final int[] DIST_MIN = {0, 28, 22, 14, 14};
    private static final int[] DIST_MAX = {0, 45, 38, 24, 24};

    /** The Thing (solo modo THE FOLK IS COMING): espera entre apariciones y distancia. */
    private static final int THING_COOLDOWN_MIN = 3000;
    private static final int THING_COOLDOWN_MAX = 9000;
    private static final int THING_DIST_MIN = 12;
    private static final int THING_DIST_MAX = 22;
    // =================================================

    private static final long DAY_TICKS = 24000L;
    private static final Map<UUID, Long> NEXT_SPAWN = new HashMap<>();
    private static final Map<UUID, Long> THING_NEXT = new HashMap<>();

    // ---------- Modo ----------
    public static int currentMode(ServerLevel level) {
        return FolkModeData.get(level).getMode();
    }

    public static void setMode(ServerLevel level, int mode) {
        FolkModeData.get(level).setMode(mode);
        NEXT_SPAWN.clear();
        THING_NEXT.clear();
    }

    public static String modeName(int mode) {
        if (mode == MODE_ERROR) return "ERROR";
        if (mode == MODE_COMING) return "THE FOLK IS COMING";
        return "Normal";
    }

    // ---------- Fases ----------
    private static int[] durations(int mode) {
        return mode == MODE_NORMAL ? NORMAL_DAYS : ERROR_DAYS;
    }

    private static int cycleLength(int mode) {
        int sum = 0;
        for (int d : durations(mode)) sum += d;
        return sum;
    }

    public static long currentDay(ServerLevel level) {
        return level.getDayTime() / DAY_TICKS;
    }

    public static int phaseOfDay(long day, int mode) {
        int[] d = durations(mode);
        int pos = (int) (day % cycleLength(mode));
        for (int i = 0; i < d.length; i++) {
            if (pos < d[i]) return i + 1;
            pos -= d[i];
        }
        return TheFolkEntity.PHASE_WATCH;
    }

    public static int dayInCycle(long day, int mode) {
        return (int) (day % cycleLength(mode)) + 1;
    }

    // ---------- Tick ----------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;

        int mode = currentMode(level);
        tickFolk(player, level, mode);
        if (mode == MODE_COMING) {
            tickThing(player, level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NEXT_SPAWN.clear();
        THING_NEXT.clear();
    }

    private static void tickFolk(ServerPlayer player, ServerLevel level, int mode) {
        long now = level.getGameTime();
        Long next = NEXT_SPAWN.get(player.getUUID());
        if (next == null) {
            NEXT_SPAWN.put(player.getUUID(), now + 1200 + level.random.nextInt(2400));
            return;
        }
        if (now < next) return;

        int phase = phaseOfDay(currentDay(level), mode);
        if (phase < TheFolkEntity.PHASE_CHASE && !isDark(level, player)) return;

        if (trySpawn(player, phase, false)) {
            int span = COOLDOWN_MAX[phase] - COOLDOWN_MIN[phase] + 1;
            NEXT_SPAWN.put(player.getUUID(), now + COOLDOWN_MIN[phase] + level.random.nextInt(span));
        } else {
            NEXT_SPAWN.put(player.getUUID(), now + 200);
        }
    }

    private static void tickThing(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        Long next = THING_NEXT.get(player.getUUID());
        if (next == null) {
            THING_NEXT.put(player.getUUID(), now + 1200 + level.random.nextInt(2400));
            return;
        }
        if (now < next) return;

        if (trySpawnThing(player, false)) {
            int span = THING_COOLDOWN_MAX - THING_COOLDOWN_MIN + 1;
            THING_NEXT.put(player.getUUID(), now + THING_COOLDOWN_MIN + level.random.nextInt(span));
        } else {
            THING_NEXT.put(player.getUUID(), now + 200);
        }
    }

    private static boolean isDark(ServerLevel level, ServerPlayer player) {
        long t = level.getDayTime() % DAY_TICKS;
        boolean night = t >= 13000L && t <= 23000L;
        return night || level.getMaxLocalRawBrightness(player.blockPosition()) <= 4;
    }

    // ---------- Aparición de The Folk ----------
    /** Intenta hacer aparecer a The Folk cerca del jugador. force = ignora límites (para el comando). */
    public static boolean trySpawn(ServerPlayer player, int phase, boolean force) {
        ServerLevel level = player.serverLevel();
        if (!force && !level.getEntitiesOfClass(TheFolkEntity.class, player.getBoundingBox().inflate(128.0D)).isEmpty()) {
            return false;
        }

        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = DIST_MIN[phase] + rnd.nextDouble() * (DIST_MAX[phase] - DIST_MIN[phase]);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos spot = findStandingSpot(level, x, z, player.blockPosition().getY());
            if (spot == null) continue;

            boolean relax = force && attempt >= 25;
            if (!relax) {
                boolean visible = isVisible(level, player, eye, spot);
                // Fases 3 y 4 aparecen escondido; las otras aparecen a la vista.
                if (phase >= TheFolkEntity.PHASE_CHASE && visible) continue;
                if (phase < TheFolkEntity.PHASE_CHASE && !visible) continue;
            }

            TheFolkEntity folk = ModEntities.THE_FOLK.get().create(level);
            if (folk == null) return false;
            folk.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, rnd.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(folk)) continue;

            folk.setPhase(phase);
            level.addFreshEntity(folk);
            return true;
        }
        return false;
    }

    // ---------- Aparición de The Thing ----------
    public static boolean trySpawnThing(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();
        if (!force && !level.getEntitiesOfClass(TheThingEntity.class, player.getBoundingBox().inflate(128.0D)).isEmpty()) {
            return false;
        }

        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = THING_DIST_MIN + rnd.nextDouble() * (THING_DIST_MAX - THING_DIST_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos spot = findStandingSpot(level, x, z, player.blockPosition().getY());
            if (spot == null) continue;

            boolean relax = force && attempt >= 25;
            if (!relax && isVisible(level, player, eye, spot)) continue;   // aparece escondido

            TheThingEntity thing = ModEntities.THE_THING.get().create(level);
            if (thing == null) return false;
            thing.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, rnd.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(thing)) continue;

            thing.setCustomName(Component.literal("Im C0MING").withStyle(ChatFormatting.RED));
            thing.setCustomNameVisible(true);
            level.addFreshEntity(thing);

            // Texto rojo en pantalla
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("Im C0MING").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            return true;
        }
        return false;
    }

    private static boolean isVisible(ServerLevel level, ServerPlayer player, Vec3 eye, BlockPos spot) {
        Vec3 head = new Vec3(spot.getX() + 0.5D, spot.getY() + 2.0D, spot.getZ() + 0.5D);
        return level.clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static BlockPos findStandingSpot(ServerLevel level, int x, int z, int aroundY) {
        if (!level.hasChunkAt(new BlockPos(x, aroundY, z))) return null;
        int minY = Math.max(level.getMinBuildHeight() + 1, aroundY - 10);
        int maxY = Math.min(level.getMaxBuildHeight() - 4, aroundY + 10);
        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir()) {
                return pos;
            }
        }
        return null;
    }
}
