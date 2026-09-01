package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class RaceAbilities {
    private RaceAbilities() {}
    private static final DustParticleOptions WINE = new DustParticleOptions(new Vector3f(.42f, .08f, .16f), 1f);

    public static void usePrimary(ServerPlayer p) {
        Race race = RaceState.race(p);
        long now = p.level().getGameTime();
        if (race == Race.NONE || now < RaceState.primaryReady(p)) return;
        int cooldown;
        switch (race) {
            case TIEFLING -> { infernalPulse(p); cooldown = 700; }
            case LYCANTHROPE -> { huntingHowl(p); cooldown = 900; }
            case DRAGONBORN -> { dragonBreath(p); cooldown = 600; }
            case HARPY -> { windGust(p); cooldown = 500; }
            default -> { return; }
        }
        RaceState.setPrimaryReady(p, now + cooldown);
        RaceGame.sync(p);
    }

    public static void useMobility(ServerPlayer p) {
        Race race = RaceState.race(p);
        long now = p.level().getGameTime();
        if (race == Race.NONE || now < RaceState.mobilityReady(p)) return;
        Vec3 look = p.getLookAngle();
        switch (race) {
            case TIEFLING -> p.setDeltaMovement(look.x * 1.15, Math.max(.18, look.y * .35), look.z * 1.15);
            case LYCANTHROPE -> p.setDeltaMovement(look.x * 1.35, .34, look.z * 1.35);
            case DRAGONBORN -> p.setDeltaMovement(look.x * 1.0, .12, look.z * 1.0);
            case HARPY -> {
                if (!p.onGround()) return;
                p.setDeltaMovement(look.x * .65, 1.0, look.z * .65);
            }
            default -> { return; }
        }
        p.hurtMarked = true;
        p.causeFoodExhaustion(1.0f);
        RaceState.setMobilityReady(p, now + (race == Race.HARPY ? 240 : 300));
        particles(p, 18);
        RaceGame.sync(p);
    }

    private static void infernalPulse(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 4.0)) {
            target.hurt(p.damageSources().playerAttack(p), 4f);
            target.igniteForSeconds(3);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        particles(p, 45);
    }

    private static void huntingHowl(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 24.0)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
        if (p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2f, .8f);
        p.causeFoodExhaustion(1f);
    }

    private static void dragonBreath(ServerPlayer p) {
        Vec3 from = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        DragonLineage lineage = RaceState.lineage(p);
        for (LivingEntity target : nearby(p, 8.0)) {
            Vec3 to = target.getEyePosition().subtract(from).normalize();
            if (look.dot(to) < .72) continue;
            target.hurt(p.damageSources().playerAttack(p), 6f);
            if (lineage == DragonLineage.FIRE) target.igniteForSeconds(4);
            if (lineage == DragonLineage.FROST) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            if (lineage == DragonLineage.VENOM) target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        }
        p.causeFoodExhaustion(1f);
        particles(p, 55);
    }

    private static void windGust(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        for (LivingEntity target : nearby(p, 7.0)) {
            Vec3 to = target.position().subtract(p.position()).normalize();
            if (look.dot(to) < .5) continue;
            target.push(look.x * 1.4, .35, look.z * 1.4);
            target.hurtMarked = true;
        }
        particles(p, 35);
    }

    private static java.util.List<LivingEntity> nearby(ServerPlayer p, double radius) {
        AABB box = p.getBoundingBox().inflate(radius);
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive());
    }

    private static void particles(ServerPlayer p, int count) {
        if (p.level() instanceof ServerLevel level)
            level.sendParticles(WINE, p.getX(), p.getY() + 1, p.getZ(), count, .7, .7, .7, .05);
    }
}
