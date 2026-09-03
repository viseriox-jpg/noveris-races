package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
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
        if (race == Race.FAIRY && nearLava(p)) {
            p.displayClientMessage(Component.literal("A proximidade da lava impede sua magia feérica."), true);
            return;
        }
        int cooldown;
        switch (race) {
            case ELF -> { piercingShot(p); cooldown = 300; }
            case FAIRY -> { fairyPower(p); cooldown = 300; }
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
        RaceState.customLong(p, "RaceVfxUntil", now + 30);
        RaceState.setPrimaryReady(p, now + cooldown);
        RaceGame.sync(p);
    }

    public static void useMobility(ServerPlayer p) {
        Race race = RaceState.race(p);
        long now = p.level().getGameTime();
        if (race == Race.NONE) return;
        if (RaceState.customLong(p, "MobilityChargeSystem") == 0) {
            RaceState.customLong(p, "MobilityChargeSystem", 1);
            RaceState.customLong(p, "MobilityCharges", 3);
            RaceState.setMobilityReady(p, 0);
        }
        int charges = (int) RaceState.customLong(p, "MobilityCharges");
        if (charges <= 0) {
            if (now < RaceState.mobilityReady(p)) return;
            charges = 3;
            RaceState.customLong(p, "MobilityCharges", charges);
        }
        long burstReady = RaceState.customLong(p, "MobilityBurstReady");
        if (now < burstReady) {
            long tenths = (burstReady - now + 1) / 2;
            p.displayClientMessage(Component.literal("Aguarde " + (tenths / 10.0) + "s para usar outra carga."), true);
            return;
        }
        if (race == Race.FAIRY && nearLava(p)) {
            p.displayClientMessage(Component.literal("A proximidade da lava impede sua mobilidade feérica."), true);
            return;
        }
        Vec3 look = p.getLookAngle();
        switch (race) {
            case ELF -> {
                if (!p.onGround()) return;
                Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();
                p.setDeltaMovement(-horizontal.x * 1.15, .42, -horizontal.z * 1.15);
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 0, false, false));
            }
            case FAIRY -> {
                switch (RaceState.fairyAffinity(p)) {
                    case WATER -> {
                        if (!p.isInWater() && !p.onGround()) return;
                        double force = p.isInWater() ? 1.45 : .72;
                        p.setDeltaMovement(look.x * force, p.isInWater() ? look.y * 1.05 : .28, look.z * force);
                    }
                    case AIR -> {
                        if (!p.onGround()) return;
                        p.setDeltaMovement(look.x * .72, .78, look.z * .72);
                        RaceState.customLong(p,"FaeLandingUntil",now+100);
                    }
                    default -> {
                        if (!p.onGround()) return;
                        p.setDeltaMovement(look.x * .55, .66, look.z * .55);
                        RaceState.customLong(p,"FaeLandingUntil",now+100);
                    }
                }
            }
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
        // Mantém o rastro visível por alguns segundos, permitindo ver as camadas do efeito.
        RaceState.customLong(p, "RaceVfxUntil", now + 80);
        if (race == Race.LYCANTHROPE)
            p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 3));
        else p.causeFoodExhaustion(1.0f);
        charges--;
        RaceState.customLong(p, "MobilityCharges", charges);
        RaceState.customLong(p, "MobilityBurstReady", charges > 0 ? now + 60 : 0);
        long mobilityCooldown = 900;
        int heavyPieces = (int) RaceState.customLong(p, "HeavyArmorPieces");
        if ((race == Race.SATYR && heavyPieces >= 3) || (race == Race.HARPY && heavyPieces >= 4))
            mobilityCooldown *= 2;
        RaceState.setMobilityReady(p, charges == 0 ? now + mobilityCooldown : 0);
        p.displayClientMessage(Component.literal(charges > 0
                ? "Mobilidade: " + charges + "/3 cargas restantes."
                : "Mobilidade esgotada: recarga de " + (mobilityCooldown / 20) + " segundos."), true);
        switch (race) {
            case ELF -> { particles(p, external("irons_spellbooks:wisp", ParticleTypes.END_ROD), 28, .7, .04); particles(p, ParticleTypes.CRIT, 10, .5, .08); }
            case FAIRY -> particles(p, RaceState.fairyAffinity(p) == FairyAffinity.NATURE ? ParticleTypes.HAPPY_VILLAGER
                    : RaceState.fairyAffinity(p) == FairyAffinity.WATER ? ParticleTypes.SPLASH : ParticleTypes.CLOUD, 22, .65, .07);
            case SATYR -> { particles(p, external("hazennstuff:leaf_particle", ParticleTypes.COMPOSTER), 28, .75, .06); particles(p, ParticleTypes.FALLING_SPORE_BLOSSOM, 12, .55, .02); }
            case THALASSIAN -> { particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 34, .8, .1); particles(p, ParticleTypes.SPLASH, 16, .65, .06); }
            case NEPHILIM -> { particles(p, external("irons_spellbooks:cleanse", ParticleTypes.END_ROD), 26, .7, .04); particles(p, ParticleTypes.TOTEM_OF_UNDYING, 10, .5, .03); }
            case VAMPIRE -> { particles(p, external("irons_spellbooks:blood", ParticleTypes.SMOKE), 30, .7, .05); particles(p, ParticleTypes.DAMAGE_INDICATOR, 10, .5, .04); }
            case HALF_BLOOD -> { particles(p, ParticleTypes.ENCHANTED_HIT, 26, .7, .05); particles(p, ParticleTypes.END_ROD, 12, .45, .03); }
            case TIEFLING -> particles(p, external("irons_spellbooks:fire", ParticleTypes.FLAME), 22, .55, .12);
            case LYCANTHROPE -> { particles(p, ParticleTypes.POOF, 30, .8, .1); particles(p, ParticleTypes.CRIT, 16, .7, .12); }
            case DRAGONBORN -> { particles(p, ParticleTypes.LARGE_SMOKE, 26, .7, .05); particles(p, ParticleTypes.ELECTRIC_SPARK, 12, .55, .04); }
            case HARPY -> particles(p, external("irons_spellbooks:spark", ParticleTypes.CLOUD), 26, .8, .12);
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
            if (distanceFromRay <= Math.max(.75, target.getBbWidth() * .65)) {
                target.hurt(p.damageSources().playerAttack(p), 5f);
                particlesAt(p, external("irons_spellbooks:wisp", ParticleTypes.END_ROD), target.getX(), target.getY()+target.getBbHeight()*.5, target.getZ(), 10, .25, .35, .25, .03);
            }
        }
        if (p.level() instanceof ServerLevel level) {
            for (int step = 1; step <= 24; step++) {
                Vec3 point = origin.add(direction.scale(step * (range / 24.0)));
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, point.x, point.y, point.z, 2, .05, .05, .05, .01);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f, 1.35f);
    }

    private static void fairyPower(ServerPlayer p) {
        switch (RaceState.fairyAffinity(p)) {
            case WATER -> {
                Vec3 center = p.position();
                for (LivingEntity target : nearby(p, 4.5)) {
                    Vec3 away = target.position().subtract(center).normalize();
                    target.push(away.x * 1.15, .22, away.z * 1.15);
                    target.hurtMarked = true;
                }
                p.clearFire();
                cleanseOneHarmfulEffect(p);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
                particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.SPLASH), 42, 1.1, .12);
                particles(p, external("irons_spellbooks:acid_bubble", ParticleTypes.BUBBLE), 24, .8, .08);
                particles(p, ParticleTypes.FALLING_WATER, 20, .8, .04);
            }
            case AIR -> {
                Vec3 look = p.getLookAngle().normalize();
                for (LivingEntity target : nearby(p, 8)) {
                    Vec3 toward = target.position().subtract(p.position()).normalize();
                    if (toward.dot(look) < .25 || !p.hasLineOfSight(target)) continue;
                    target.push(toward.x * 1.15, .28, toward.z * 1.15);
                    target.hurtMarked = true;
                }
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
                particles(p, external("irons_spellbooks:spark", ParticleTypes.CLOUD), 46, 1.3, .16);
                particles(p, ParticleTypes.SWEEP_ATTACK, 8, .7, .02);
            }
            default -> {
                for (LivingEntity target : nearby(p, 5)) {
                    if (p.hasLineOfSight(target)) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    }
                }
                particles(p, external("irons_spellbooks:cleanse", ParticleTypes.HAPPY_VILLAGER), 38, 1.1, .08);
                particles(p, external("hazennstuff:leaf_particle", ParticleTypes.COMPOSTER), 26, .9, .05);
            }
        }
    }
    private static void woodlandVigor(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        particles(p, external("hazennstuff:nature_slash_particle", ParticleTypes.COMPOSTER), 30, 1.0, .05);
        particles(p, ParticleTypes.FALLING_SPORE_BLOSSOM, 18, .8, .02);
    }
    private static void tidalGuard(ServerPlayer p) {
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 36, .9, .08);
        particles(p, ParticleTypes.FALLING_WATER, 16, .8, .03);
    }
    private static void supernaturalAegis(ServerPlayer p) {
        cleanseOneHarmfulEffect(p);
        p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 2));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
        RaceState.customLong(p, "AegisWeaknessAt", p.level().getGameTime() + 200);
        particles(p, external("irons_spellbooks:cleanse", ParticleTypes.END_ROD), 28, .9, .03);
        particles(p, external("irons_spellbooks:absorption", ParticleTypes.END_ROD), 18, .6, .03);
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
            float hearts = healthStolen / 2f;
            String amount = Math.abs(hearts - Math.round(hearts)) < .01f
                    ? String.format(java.util.Locale.ROOT, "%.0f", hearts)
                    : String.format(java.util.Locale.ROOT, "%.1f", hearts);
            String unit = Math.abs(hearts - 1f) < .01f ? "coração" : "corações";
            p.displayClientMessage(Component.literal("Vida drenada: " + amount + " " + unit + "."), true);
        } else if (p.getHealth() >= p.getMaxHealth()) {
            p.displayClientMessage(Component.literal("Dano causado, mas sua vida já está cheia."), true);
        }
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(external("irons_spellbooks:blood", ParticleTypes.DAMAGE_INDICATOR), target.getX(), target.getY()+target.getBbHeight()*.6, target.getZ(), 18, .35, .45, .35, .05);
            level.sendParticles(external("irons_spellbooks:blood_ground", ParticleTypes.DAMAGE_INDICATOR), target.getX(), target.getY()+.05, target.getZ(), 6, .25, .02, .25, .01);
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
        particles(p, external("irons_spellbooks:fire", ParticleTypes.FLAME), 42, 1.2, .08);
        particles(p, external("irons_spellbooks:fiery_smoke", ParticleTypes.SMOKE), 28, 1.0, .04);
        particles(p, ParticleTypes.SOUL_FIRE_FLAME, 8, .55, .03);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, .75f);
    }

    private static void huntingHowl(ServerPlayer p) {
        for (LivingEntity target : nearby(p, 24.0)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
        if (p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2f, .8f);
        particles(p, ParticleTypes.POOF, 34, 1.5, .08);
        particles(p, ParticleTypes.CRIT, 22, 1.2, .12);
        p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 3));
    }

    private static boolean nearLava(ServerPlayer p) {
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                p.blockPosition().offset(-5, -3, -5), p.blockPosition().offset(5, 3, 5)))
            if (p.level().getFluidState(pos).is(FluidTags.LAVA)) return true;
        return false;
    }

    private static void dragonBreath(ServerPlayer p) {
        Vec3 from = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        DragonLineage lineage = RaceState.lineage(p);
        ParticleOptions breathParticle = lineage == DragonLineage.FIRE ? external("irons_spellbooks:dragon_fire", ParticleTypes.FLAME)
                : lineage == DragonLineage.FROST ? external("irons_spellbooks:snowflake", ParticleTypes.SNOWFLAKE)
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
        if (lineage == DragonLineage.FIRE) {
            particles(p, ParticleTypes.SMOKE, 24, .55, .03);
            p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0f, .9f);
        } else if (lineage == DragonLineage.FROST) {
            particles(p, external("irons_spellbooks:snow_dust", ParticleTypes.SNOWFLAKE), 28, .7, .04);
            particles(p, ParticleTypes.SNOWFLAKE, 18, .55, .02);
            p.level().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, .9f, 1.6f);
        } else {
            particles(p, external("irons_spellbooks:acid", ParticleTypes.SNEEZE), 28, .6, .04);
            particles(p, new DustParticleOptions(new Vector3f(.08f, .95f, .16f), 1.5f), 24, .7, .05);
            particles(p, ParticleTypes.BUBBLE, 10, .45, .03);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, .9f, .8f);
        }
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
                level.sendParticles(external("irons_spellbooks:spark", ParticleTypes.CLOUD), point.x, point.y, point.z, 7, .3, .25, .3, .05);
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

    private static void cleanseOneHarmfulEffect(ServerPlayer p) {
        for (MobEffectInstance effect : java.util.List.copyOf(p.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;
            p.removeEffect(effect.getEffect());
            p.displayClientMessage(Component.literal("Um efeito negativo foi purificado."), true);
            particles(p, ParticleTypes.TOTEM_OF_UNDYING, 14, .45, .02);
            return;
        }
    }

    private static void particles(ServerPlayer p, ParticleOptions particle, int count, double spread, double speed) {
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(particle, p.getX(), p.getY() + 1, p.getZ(), count, spread, spread, spread, speed);
            // Um brilho secundário dá profundidade ao efeito sem substituir a partícula temática.
            if (particle != ParticleTypes.FLAME && particle != ParticleTypes.SMALL_FLAME
                    && particle != ParticleTypes.SOUL_FIRE_FLAME && particle != ParticleTypes.SMOKE
                    && particle != ParticleTypes.LARGE_SMOKE)
                level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(),
                        Math.max(4, count / 4), spread * .72, spread * .72, spread * .72, Math.max(.01, speed * .55));
        }
    }

    private static void particlesAt(ServerPlayer p, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (p.level() instanceof ServerLevel level)
            level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
    }

    private static ParticleOptions external(String id, ParticleOptions fallback) {
        try {
            var type = BuiltInRegistries.PARTICLE_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
            return type instanceof SimpleParticleType simple ? simple : fallback;
        } catch (Exception ignored) { return fallback; }
    }

    public static void tickVisuals(ServerPlayer p) {
        long until = RaceState.customLong(p, "RaceVfxUntil");
        if (until <= p.level().getGameTime() || p.tickCount % 4 != 0) return;
        Race race = RaceState.race(p);
        switch (race) {
            case FAIRY -> {
                if (RaceState.fairyAffinity(p) == FairyAffinity.WATER) particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 8, .55, .03);
                else if (RaceState.fairyAffinity(p) == FairyAffinity.AIR) particles(p, ParticleTypes.CLOUD, 8, .7, .04);
                else particles(p, external("hazennstuff:leaf_particle", ParticleTypes.HAPPY_VILLAGER), 8, .65, .03);
            }
            case VAMPIRE -> particles(p, external("irons_spellbooks:blood", ParticleTypes.DAMAGE_INDICATOR), 7, .45, .03);
            case DRAGONBORN -> particles(p, RaceState.lineage(p) == DragonLineage.FIRE ? external("irons_spellbooks:dragon_fire", ParticleTypes.FLAME) : RaceState.lineage(p) == DragonLineage.FROST ? external("irons_spellbooks:snowflake", ParticleTypes.SNOWFLAKE) : new DustParticleOptions(new Vector3f(.1f, .9f, .18f), 1.25f), 8, .55, .03);
            case TIEFLING -> particles(p, external("irons_spellbooks:fiery_smoke", ParticleTypes.SMOKE), 7, .55, .03);
            case THALASSIAN -> particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 8, .55, .03);
            case ELF -> particles(p, external("irons_spellbooks:wisp", ParticleTypes.END_ROD), 7, .5, .03);
            case SATYR -> particles(p, external("hazennstuff:leaf_particle", ParticleTypes.COMPOSTER), 7, .55, .03);
            case HARPY -> particles(p, ParticleTypes.CLOUD, 8, .65, .04);
            default -> { }
        }
    }
}
