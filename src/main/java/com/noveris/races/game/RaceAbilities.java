package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RaceAbilities {
    private RaceAbilities() {}
    public static void usePrimary(ServerPlayer p) {
        Race race = RaceState.race(p);
        long now = p.level().getGameTime();
        if (race == Race.NONE || now < RaceState.primaryReady(p)) return;
        int cooldown;
        switch (race) {
            case ELF -> { focusArcher(p); cooldown = 700; }
            case FAIRY -> { faeSense(p); cooldown = 800; }
            case SATYR -> { woodlandVigor(p); cooldown = 700; }
            case THALASSIAN -> { tidalGuard(p); cooldown = 700; }
            case NEPHILIM -> { supernaturalAegis(p); cooldown = 1000; }
            case VAMPIRE -> { bloodDrain(p); cooldown = 700; }
            case HALF_BLOOD -> { hybridHeritage(p); cooldown = 800; }
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
            case ELF -> { double power=p.level().getBiome(p.blockPosition()).is(BiomeTags.IS_FOREST)?1.25:1.0; p.setDeltaMovement(look.x * power, .2, look.z * power); }
            case FAIRY -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * .55, .72, look.z * .55); RaceState.customLong(p,"FaeLandingUntil",now+100); }
            case SATYR -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.15, .48, look.z * 1.15); }
            case THALASSIAN -> p.setDeltaMovement(look.x * (p.isInWater() ? 1.55 : .75), p.isInWater() ? look.y * 1.1 : .18, look.z * (p.isInWater() ? 1.55 : .75));
            case NEPHILIM -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * .7, .62, look.z * .7); }
            case VAMPIRE -> p.setDeltaMovement(look.x * 1.2, .12, look.z * 1.2);
            case HALF_BLOOD -> p.setDeltaMovement(look.x * .72, .18, look.z * .72);
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
        long mobilityCooldown = switch (race) { case FAIRY -> 280; case SATYR -> 320; case THALASSIAN -> p.isInWater() ? 240 : 360; case HARPY -> 240; default -> 300; };
        RaceState.setMobilityReady(p, now + mobilityCooldown);
        switch (race) {
            case ELF -> particles(p, ParticleTypes.HAPPY_VILLAGER, 18, .6, .03);
            case FAIRY -> particles(p, ParticleTypes.END_ROD, 20, .55, .04);
            case SATYR -> particles(p, ParticleTypes.COMPOSTER, 20, .65, .05);
            case THALASSIAN -> particles(p, ParticleTypes.BUBBLE, 24, .7, .08);
            case NEPHILIM -> particles(p, ParticleTypes.END_ROD, 18, .6, .03);
            case VAMPIRE -> particles(p, ParticleTypes.SMOKE, 22, .6, .04);
            case HALF_BLOOD -> particles(p, ParticleTypes.ENCHANTED_HIT, 18, .6, .04);
            case TIEFLING -> particles(p, ParticleTypes.FLAME, 22, .55, .12);
            case LYCANTHROPE -> particles(p, ParticleTypes.POOF, 24, .7, .08);
            case DRAGONBORN -> particles(p, ParticleTypes.LARGE_SMOKE, 20, .65, .04);
            case HARPY -> particles(p, ParticleTypes.CLOUD, 26, .8, .12);
            default -> { }
        }
        RaceGame.sync(p);
    }

    private static void focusArcher(ServerPlayer p) {
        RaceState.customLong(p, "ArcherFocusUntil", p.level().getGameTime() + 160);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        particles(p, ParticleTypes.HAPPY_VILLAGER, 28, .8, .04);
    }
    private static void faeSense(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 18)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        particles(p, ParticleTypes.END_ROD, 35, 1.1, .04);
    }
    private static void woodlandVigor(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        particles(p, ParticleTypes.COMPOSTER, 30, 1.0, .05);
    }
    private static void tidalGuard(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, p.isInWater() ? 1 : 0));
        p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 160, 0));
        particles(p, ParticleTypes.BUBBLE, 36, .9, .08);
    }
    private static void supernaturalAegis(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0));
        particles(p, ParticleTypes.END_ROD, 38, .9, .03);
    }
    private static void bloodDrain(ServerPlayer p) {
        LivingEntity target = nearby(p, 5).stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (target != null) { target.hurt(p.damageSources().playerAttack(p), 4f); p.heal(2f); }
        particles(p, ParticleTypes.DAMAGE_INDICATOR, 24, .8, .05);
    }
    private static void hybridHeritage(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        particles(p, ParticleTypes.ENCHANTED_HIT, 28, .8, .05);
    }

    private static void infernalPulse(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 4.0)) {
            target.hurt(p.damageSources().playerAttack(p), 4f);
            target.igniteForSeconds(3);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
        particles(p, ParticleTypes.FLAME, 42, 1.2, .08);
        particles(p, ParticleTypes.SMOKE, 18, 1.0, .04);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, .75f);
    }

    private static void huntingHowl(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 24.0)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
        if (p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2f, .8f);
        particles(p, ParticleTypes.POOF, 34, 1.5, .08);
        particles(p, ParticleTypes.CRIT, 22, 1.2, .12);
        p.causeFoodExhaustion(1f);
    }

    private static void dragonBreath(ServerPlayer p) {
        Vec3 from = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        DragonLineage lineage = RaceState.lineage(p);
        ParticleOptions breathParticle = lineage == DragonLineage.FIRE ? ParticleTypes.FLAME
                : lineage == DragonLineage.FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.WITCH;
        for (LivingEntity target : nearby(p, 8.0)) {
            Vec3 to = target.getEyePosition().subtract(from).normalize();
            if (look.dot(to) < .72) continue;
            target.hurt(p.damageSources().playerAttack(p), 6f);
            if (lineage == DragonLineage.FIRE) target.igniteForSeconds(4);
            if (lineage == DragonLineage.FROST) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            if (lineage == DragonLineage.VENOM) target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        }
        p.causeFoodExhaustion(1f);
        if (p.level() instanceof ServerLevel level) {
            for (int step = 1; step <= 8; step++) {
                Vec3 point = from.add(look.scale(step));
                level.sendParticles(breathParticle, point.x, point.y, point.z, 6, .22, .22, .22, .02);
            }
        }
        particles(p, ParticleTypes.SMOKE, 14, .5, .02);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0f, .9f);
    }

    private static void windGust(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        for (LivingEntity target : nearby(p, 7.0)) {
            Vec3 to = target.position().subtract(p.position()).normalize();
            if (look.dot(to) < .5) continue;
            target.push(look.x * 1.4, .35, look.z * 1.4);
            target.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            Vec3 origin = p.getEyePosition();
            for (int step = 1; step <= 6; step++) {
                Vec3 point = origin.add(look.scale(step));
                level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 7, .3, .25, .3, .05);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .9f, .65f);
    }

    private static java.util.List<LivingEntity> nearby(ServerPlayer p, double radius) {
        AABB box = p.getBoundingBox().inflate(radius);
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive());
    }

    private static void particles(ServerPlayer p, ParticleOptions particle, int count, double spread, double speed) {
        if (p.level() instanceof ServerLevel level)
            level.sendParticles(particle, p.getX(), p.getY() + 1, p.getZ(), count, spread, spread, spread, speed);
    }
}
