package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
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
import org.joml.Vector3f;

public final class RaceAbilities {
    private RaceAbilities() {}
    public static void usePrimary(ServerPlayer p) {
        Race race = RaceState.race(p);
        long now = p.level().getGameTime();
        if (race == Race.NONE || now < RaceState.primaryReady(p)) return;
        int cooldown;
        switch (race) {
            case ELF -> { piercingShot(p); cooldown = 300; }
            case FAIRY -> { faeSense(p); cooldown = 300; }
            case SATYR -> { woodlandVigor(p); cooldown = 300; }
            case THALASSIAN -> { tidalGuard(p); cooldown = 300; }
            case NEPHILIM -> { supernaturalAegis(p); cooldown = 300; }
            case VAMPIRE -> { if (!bloodDrain(p)) return; cooldown = 300; }
            case HALF_BLOOD -> { hybridHeritage(p); cooldown = 300; }
            case TIEFLING -> { infernalPulse(p); cooldown = 300; }
            case LYCANTHROPE -> { huntingHowl(p); cooldown = 300; }
            case DRAGONBORN -> { dragonBreath(p); cooldown = 300; }
            case HARPY -> { windGust(p); cooldown = 300; }
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
            case ELF -> {
                if (!p.onGround()) return;
                Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();
                p.setDeltaMovement(-horizontal.x * 1.15, .42, -horizontal.z * 1.15);
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 0, false, false));
            }
            case FAIRY -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * .55, .72, look.z * .55); RaceState.customLong(p,"FaeLandingUntil",now+100); }
            case SATYR -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.15, .48, look.z * 1.15); }
            case THALASSIAN -> { if (!p.isInWater() && !p.onGround()) return; p.setDeltaMovement(look.x * (p.isInWater() ? 1.55 : .75), p.isInWater() ? look.y * 1.1 : .18, look.z * (p.isInWater() ? 1.55 : .75)); }
            case NEPHILIM -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * .7, .62, look.z * .7); }
            case VAMPIRE -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.2, .12, look.z * 1.2); }
            case HALF_BLOOD -> { if (!p.onGround()) return; hybridMobility(p, look); }
            case TIEFLING -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.15, Math.max(.18, look.y * .35), look.z * 1.15); }
            case LYCANTHROPE -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.35, .34, look.z * 1.35); }
            case DRAGONBORN -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.0, .12, look.z * 1.0); }
            case HARPY -> {
                if (!p.onGround()) return;
                p.setDeltaMovement(look.x * .65, 1.0, look.z * .65);
            }
            default -> { return; }
        }
        p.hurtMarked = true;
        p.causeFoodExhaustion(1.0f);
        long mobilityCooldown = switch (race) { case FAIRY -> 280; case THALASSIAN, HARPY -> 240; default -> 300; };
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

    private static void piercingShot(ServerPlayer p) {
        Vec3 origin = p.getEyePosition();
        Vec3 direction = p.getLookAngle().normalize();
        double range = 16.0;
        for (LivingEntity target : nearby(p, range)) {
            Vec3 center = target.position().add(0, target.getBbHeight() * .5, 0);
            Vec3 relative = center.subtract(origin);
            double alongRay = relative.dot(direction);
            if (alongRay < 0 || alongRay > range || !p.hasLineOfSight(target)) continue;
            double distanceFromRay = relative.subtract(direction.scale(alongRay)).length();
            if (distanceFromRay <= Math.max(.75, target.getBbWidth() * .65))
                target.hurt(p.damageSources().playerAttack(p), 5f);
        }
        if (p.level() instanceof ServerLevel level) {
            for (int step = 1; step <= 24; step++) {
                Vec3 point = origin.add(direction.scale(step * (range / 24.0)));
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, point.x, point.y, point.z, 2, .05, .05, .05, .01);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, 1.35f);
    }

    private static void faeSense(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 18)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        particles(p, ParticleTypes.END_ROD, 35, 1.1, .04);
    }
    private static void woodlandVigor(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        particles(p, ParticleTypes.COMPOSTER, 30, 1.0, .05);
    }
    private static void tidalGuard(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, p.isInWater() ? 1 : 0));
        if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 120, 0));
        particles(p, ParticleTypes.BUBBLE, 36, .9, .08);
    }
    private static void supernaturalAegis(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0));
        particles(p, ParticleTypes.END_ROD, 38, .9, .03);
    }
    private static boolean bloodDrain(ServerPlayer p) {
        if (!p.level().isNight()) {
            p.displayClientMessage(Component.literal("A Drenagem de Sangue só pode ser usada durante a noite."), true);
            return false;
        }
        LivingEntity target = nearby(p, 6).stream()
                .filter(p::hasLineOfSight)
                .min(java.util.Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (target == null) {
            p.displayClientMessage(Component.literal("Nenhum alvo visível para drenar."), true);
            return false;
        }
        float healthBefore = target.getHealth();
        if (!target.hurt(p.damageSources().playerAttack(p), 6f)) {
            p.displayClientMessage(Component.literal("A drenagem não conseguiu ferir o alvo."), true);
            return false;
        }
        float damageDealt = Math.max(0f, healthBefore - target.getHealth());
        float healthStolen = Math.min(6f, damageDealt);
        if (healthStolen > 0f && p.getHealth() < p.getMaxHealth()) {
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + healthStolen));
            p.displayClientMessage(Component.literal(String.format("Vida drenada: %.1f coração(ões)", healthStolen / 2f)), true);
        } else if (p.getHealth() >= p.getMaxHealth()) {
            p.displayClientMessage(Component.literal("Dano causado, mas sua vida já está cheia."), true);
        }
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY()+target.getBbHeight()*.6, target.getZ(), 18, .35, .45, .35, .05);
            Vec3 from=target.getEyePosition(),to=p.getEyePosition();
            for(int step=1;step<=8;step++){
                Vec3 point=from.lerp(to,step/8.0);
                level.sendParticles(ParticleTypes.DUST_PLUME,point.x,point.y,point.z,2,.08,.08,.08,.01);
            }
        }
        p.level().playSound(null,p.blockPosition(),SoundEvents.EVOKER_CAST_SPELL,SoundSource.PLAYERS,.8f,.65f);
        return true;
    }
    private static void hybridHeritage(ServerPlayer p) {
        applyHybridPower(p, RaceState.ancestryA(p));
        applyHybridPower(p, RaceState.ancestryB(p));
        particles(p, ParticleTypes.ENCHANTED_HIT, 28, .8, .05);
    }

    private static void applyHybridPower(ServerPlayer p, Race ancestry) {
        switch (ancestry) {
            case ELF -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
            case FAIRY -> p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            case SATYR -> { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0)); p.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 0)); }
            case THALASSIAN -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
            case HUMAN -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0));
            case NEPHILIM -> p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0));
            case VAMPIRE -> { if (p.level().isNight()) p.heal(2f); }
            case TIEFLING -> p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
            case LYCANTHROPE -> { if (p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0)); }
            case DRAGONBORN -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
            case HARPY -> p.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 0));
            default -> { }
        }
    }

    private static void hybridMobility(ServerPlayer p, Vec3 look) {
        Race a = RaceState.ancestryA(p), b = RaceState.ancestryB(p);
        double horizontal = (hybridHorizontal(a) + hybridHorizontal(b)) / 2.0;
        double vertical = (hybridVertical(a) + hybridVertical(b)) / 2.0;
        horizontal = Math.min(1.05, horizontal);
        vertical = Math.min(.62, vertical);
        p.setDeltaMovement(look.x * horizontal, vertical, look.z * horizontal);
    }
    private static double hybridHorizontal(Race race) { return switch (race) {
        case ELF -> .75; case FAIRY -> .5; case SATYR -> 1.0; case THALASSIAN -> .7;
        case HUMAN -> .65; case NEPHILIM -> .6; case VAMPIRE -> .95; case TIEFLING -> .95;
        case LYCANTHROPE -> 1.05; case DRAGONBORN -> .8; case HARPY -> .55; default -> .65;
    }; }
    private static double hybridVertical(Race race) { return switch (race) {
        case FAIRY -> .62; case SATYR -> .42; case NEPHILIM -> .52; case LYCANTHROPE -> .3;
        case HARPY -> .62; case ELF -> .34; default -> .18;
    }; }

    private static void infernalPulse(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 4.0)) {
            if (!p.hasLineOfSight(target)) continue;
            target.igniteForSeconds(3);
            target.hurt(p.damageSources().playerAttack(p), 4f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
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
                : lineage == DragonLineage.FROST ? ParticleTypes.SNOWFLAKE
                : new DustParticleOptions(new Vector3f(.18f, .90f, .25f), 1.2f);
        for (LivingEntity target : nearby(p, 8.0)) {
            Vec3 to = target.getEyePosition().subtract(from).normalize();
            if (look.dot(to) < .72 || !p.hasLineOfSight(target)) continue;
            if (lineage == DragonLineage.FIRE) target.igniteForSeconds(4);
            target.hurt(p.damageSources().playerAttack(p), 6f);
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
            if (look.dot(to) < .5 || !p.hasLineOfSight(target)) continue;
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
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive()
                && e.isAttackable() && !p.isAlliedTo(e)
                && p.distanceToSqr(e) <= radius * radius
                && (!(e instanceof net.minecraft.world.entity.player.Player other) || !other.isSpectator()));
    }

    private static void particles(ServerPlayer p, ParticleOptions particle, int count, double spread, double speed) {
        if (p.level() instanceof ServerLevel level)
            level.sendParticles(particle, p.getX(), p.getY() + 1, p.getZ(), count, spread, spread, spread, speed);
    }
}
