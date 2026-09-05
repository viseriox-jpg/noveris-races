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
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
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
            case NEPHILIM -> { radiantBurst(p); cooldown = 300; }
            case VAMPIRE -> { if (!bloodDrain(p)) return; cooldown = 300; }
            case TIEFLING -> { infernalPulse(p); cooldown = 300; }
            case LYCANTHROPE -> { huntingHowl(p); cooldown = 300; }
            case DRAGONBORN -> { dragonBreath(p); cooldown = 300; }
            case HARPY -> { windGust(p); cooldown = 300; }
            case GOD -> { divineJudgment(p); cooldown = 200; }
            case NPC -> { guardCharge(p); cooldown = 240; }
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
        if (race == Race.NPC) return;
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
            case TIEFLING -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.15, Math.max(.18, look.y * .35), look.z * 1.15); }
            case LYCANTHROPE -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.35, .34, look.z * 1.35); }
            case DRAGONBORN -> { if (!p.onGround()) return; p.setDeltaMovement(look.x * 1.0, .12, look.z * 1.0); }
            case HARPY -> {
                if (!p.onGround()) return;
                p.setDeltaMovement(look.x * .65, 1.0, look.z * .65);
            }
            case GOD -> { celestialTeleport(p); }
            case NPC -> { return; }
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
        // As três cargas podem ser usadas continuamente. O único cooldown
        // permanece na recarga de 45s depois que todas forem consumidas.
        RaceState.customLong(p, "MobilityBurstReady", 0);
        long mobilityCooldown = 900;
        int heavyPieces = (int) RaceState.customLong(p, "HeavyArmorPieces");
        if ((race == Race.SATYR && heavyPieces >= 3) || (race == Race.HARPY && heavyPieces >= 4))
            mobilityCooldown *= 2;
        RaceState.setMobilityReady(p, charges == 0 ? now + mobilityCooldown : 0);
        p.displayClientMessage(Component.literal(charges > 0
                ? "Mobilidade: " + charges + "/3 cargas restantes."
                : "Mobilidade esgotada: recarga de " + (mobilityCooldown / 20) + " segundos."), true);
        switch (race) {
            case ELF -> { particles(p, external("irons_spellbooks:wisp", ParticleTypes.END_ROD), 28, .7, .04); particles(p, ParticleTypes.END_ROD, 10, .5, .03); particles(p, ParticleTypes.CRIT, 10, .5, .08); }
            case FAIRY -> { ParticleOptions fae = RaceState.fairyAffinity(p) == FairyAffinity.NATURE
                    ? external("hazennstuff:nature_slash_particle", ParticleTypes.HAPPY_VILLAGER)
                    : RaceState.fairyAffinity(p) == FairyAffinity.WATER
                    ? external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE)
                    : external("irons_spellbooks:shockwave", ParticleTypes.CLOUD); particles(p, fae, 24, .7, .06); particles(p, ParticleTypes.END_ROD, 4, .4, .025); }
            case SATYR -> { particles(p, external("hazennstuff:nature_slash_particle", ParticleTypes.COMPOSTER), 26, .75, .055); particles(p, ParticleTypes.END_ROD, 4, .4, .025); }
            case THALASSIAN -> particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 32, .78, .08);
            case NEPHILIM -> { particles(p, external("irons_spellbooks:heal", ParticleTypes.END_ROD), 24, .68, .04); particles(p, ParticleTypes.END_ROD, 5, .42, .025); }
            case VAMPIRE -> { particles(p, external("irons_spellbooks:blood", ParticleTypes.SMOKE), 28, .68, .05); particles(p, external("irons_spellbooks:siphon", ParticleTypes.DAMAGE_INDICATOR), 8, .45, .03); }
            case TIEFLING -> particles(p, external("irons_spellbooks:fire", ParticleTypes.FLAME), 22, .55, .12);
            case LYCANTHROPE -> { particles(p, ParticleTypes.POOF, 30, .8, .1); particles(p, ParticleTypes.CRIT, 16, .7, .12); particles(p, ParticleTypes.ASH, 10, .55, .03); }
            case DRAGONBORN -> particles(p, ParticleTypes.LARGE_SMOKE, 24, .68, .045);
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
                    target.hurt(p.damageSources().playerAttack(p), 4f);
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
                    target.hurt(p.damageSources().playerAttack(p), 4f);
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
                        target.hurt(p.damageSources().playerAttack(p), 4f);
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
    private static void radiantBurst(ServerPlayer p) {
        Vec3 origin = p.getEyePosition();
        Vec3 direction = p.getLookAngle().normalize();
        double range = 10.0;
        for (LivingEntity target : nearby(p, range)) {
            Vec3 center = target.position().add(0, target.getBbHeight() * .5, 0);
            Vec3 relative = center.subtract(origin);
            double alongRay = relative.dot(direction);
            if (alongRay < 0 || alongRay > range || !p.hasLineOfSight(target)) continue;
            double distanceFromRay = relative.subtract(direction.scale(alongRay)).length();
            if (distanceFromRay <= Math.max(.8, target.getBbWidth() * .7)) {
                target.hurt(p.damageSources().playerAttack(p), 4f);
                // Lentidão II visível por 5 segundos; o efeito é aplicado mesmo
                // quando o alvo bloqueia ou reduz o dano da rajada.
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, true, true, true));
            }
        }
        if (p.level() instanceof ServerLevel level) {
            for (int step = 1; step <= 20; step++) {
                Vec3 point = origin.add(direction.scale(step * (range / 20.0)));
                level.sendParticles(external("irons_spellbooks:heal", ParticleTypes.END_ROD), point.x, point.y, point.z, 3, .08, .08, .08, .01);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, .8f, 1.5f);
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

    private static void divineJudgment(ServerPlayer p) {
        Vec3 origin = p.getEyePosition(), direction = p.getLookAngle().normalize();
        for (LivingEntity target : nearby(p, 20.0)) {
            Vec3 center = target.position().add(0, target.getBbHeight() * .5, 0), relative = center.subtract(origin);
            double along = relative.dot(direction);
            if (along < 0 || along > 20 || !p.hasLineOfSight(target)) continue;
            if (relative.subtract(direction.scale(along)).length() <= Math.max(.8, target.getBbWidth() * .7)) {
                target.hurt(p.damageSources().playerAttack(p), 14f);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
        }
        if (p.level() instanceof ServerLevel level) {
            for (int step = 1; step <= 20; step++) {
                Vec3 point = origin.add(direction.scale(step));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 5, .12, .12, .12, .02);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, .8f, 1.15f);
    }

    private static void celestialTeleport(ServerPlayer p) {
        Vec3 origin = p.getEyePosition(), direction = p.getLookAngle().normalize();
        Vec3 hit = p.level().clip(new ClipContext(origin, origin.add(direction.scale(28)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
        BlockPos base = BlockPos.containing(hit).relative(net.minecraft.core.Direction.DOWN);
        for (int y = 2; y >= -1; y--) {
            BlockPos feet = base.above(y);
            var box = p.getDimensions(p.getPose()).makeBoundingBox(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
            if (p.level().noCollision(p, box)) {
                p.teleportTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
                particles(p, ParticleTypes.END_ROD, 28, .7, .08);
                p.level().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .8f, 1.2f);
                return;
            }
        }
        p.displayClientMessage(Component.literal("Não há espaço seguro para o teleporte."), true);
    }

    private static void guardCharge(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * 1.0, .22, look.z * 1.0);
        p.hurtMarked = true;
        for (LivingEntity target : nearby(p, 2.5)) {
            target.push(look.x * 1.25, .25, look.z * 1.25);
            target.hurtMarked = true;
            target.hurt(p.damageSources().playerAttack(p), 3f);
        }
        particles(p, ParticleTypes.CLOUD, 18, .55, .08);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, .8f, 1.1f);
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
                else if (RaceState.fairyAffinity(p) == FairyAffinity.AIR) particles(p, external("irons_spellbooks:shockwave", ParticleTypes.CLOUD), 8, .7, .04);
                else particles(p, external("hazennstuff:leaf_particle", ParticleTypes.HAPPY_VILLAGER), 8, .65, .03);
                particles(p, ParticleTypes.END_ROD, 2, .3, .02);
            }
            case VAMPIRE -> particles(p, external("irons_spellbooks:blood", ParticleTypes.DAMAGE_INDICATOR), 9, .48, .035);
            case DRAGONBORN -> {
                if (RaceState.lineage(p) == DragonLineage.FIRE) particles(p, external("irons_spellbooks:dragon_fire", ParticleTypes.FLAME), 8, .55, .03);
                else if (RaceState.lineage(p) == DragonLineage.FROST) { particles(p, external("irons_spellbooks:snowflake", ParticleTypes.SNOWFLAKE), 8, .55, .03); particles(p, ParticleTypes.SNOWFLAKE, 4, .4, .02); }
                else { particles(p, new DustParticleOptions(new Vector3f(.1f, .9f, .18f), 1.25f), 9, .52, .03); particles(p, external("irons_spellbooks:acid_bubble", ParticleTypes.BUBBLE), 3, .3, .02); }
            }
            case TIEFLING -> particles(p, external("irons_spellbooks:fiery_smoke", ParticleTypes.SMOKE), 7, .55, .03);
            case THALASSIAN -> particles(p, external("irons_spellbooks:tinted_bubble_pop", ParticleTypes.BUBBLE), 10, .6, .04);
            case ELF -> { particles(p, external("irons_spellbooks:wisp", ParticleTypes.END_ROD), 7, .5, .03); particles(p, ParticleTypes.END_ROD, 3, .3, .02); }
            case SATYR -> { particles(p, external("hazennstuff:leaf_particle", ParticleTypes.COMPOSTER), 9, .6, .04); particles(p, ParticleTypes.END_ROD, 2, .3, .02); }
            case HARPY -> { particles(p, ParticleTypes.CLOUD, 10, .7, .05); particles(p, ParticleTypes.GUST, 3, .35, .02); }
            default -> { }
        }
    }
}
