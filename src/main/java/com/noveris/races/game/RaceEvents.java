package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NoverisRaces.MOD_ID)
public final class RaceEvents {
    private static final TagKey<net.minecraft.world.item.Item> SILVER_WEAPONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "silver_weapons"));
    private RaceEvents() {}

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            applyAttributes(p);
            RaceGame.sync(p);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RaceState.cancelTrialOnLogout(p);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer from && event.getEntity() instanceof ServerPlayer to) {
            RaceState.copyOnDeath(from, to);
            applyAttributes(to);
            RaceGame.sync(to);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        RaceState.tickTrial(p);
        Race race = RaceState.race(p);
        if (p.tickCount % 20 == 0) {
            applyAttributes(p);
            applyPassives(p, race);
            RaceGame.sync(p);
        }
        if (p.tickCount % 40 == 0) ambientParticles(p, race);
        if (race == Race.TIEFLING && p.isOnFire()) p.clearFire();
        if (race == Race.HARPY && p.getDeltaMovement().y < -0.12 && heavyArmorPieces(p) < 3)
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, false, false));
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Race race = RaceState.race(victim);
        if (race == Race.TIEFLING && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
            return;
        }
        if (race == Race.DRAGONBORN && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))
            event.setAmount(event.getAmount() * .88f);
        if (race == Race.DRAGONBORN) {
            DragonLineage lineage = RaceState.lineage(victim);
            if (lineage == DragonLineage.FIRE && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * .4f);
            if (lineage == DragonLineage.FROST && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.3f);
        }
        if (race == Race.HARPY) {
            if (event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * .2f);
            else if (!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount() * 1.12f);
        }
        if (race == Race.LYCANTHROPE && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker.getMainHandItem().is(SILVER_WEAPONS)) event.setAmount(event.getAmount() * 1.35f);
        RaceState.markCombat(victim);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) RaceState.markCombat(attacker);
    }

    @SubscribeEvent
    public static void healing(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.TIEFLING
                && p.getFoodData().getFoodLevel() >= 18 && event.getAmount() <= 1.0f)
            event.setAmount(event.getAmount() * .75f);
    }

    private static void applyAttributes(ServerPlayer p) {
        Race race = RaceState.race(p);
        var maxHealth = p.getAttribute(Attributes.MAX_HEALTH);
        var scale = p.getAttribute(Attributes.SCALE);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        var knockback = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (maxHealth != null) maxHealth.setBaseValue(race.maxHealth);
        if (scale != null) scale.setBaseValue(race.scale);
        if (speed != null) {
            double value = .1;
            if (race == Race.DRAGONBORN) value = .092;
            if (race == Race.HARPY) value = .112;
            if (race == Race.LYCANTHROPE && p.level().isNight()) value = .11;
            speed.setBaseValue(value);
        }
        if (knockback != null) knockback.setBaseValue(race == Race.DRAGONBORN ? .2 : 0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void applyPassives(ServerPlayer p, Race race) {
        switch (race) {
            case TIEFLING -> {
                if (p.level().getMaxLocalRawBrightness(p.blockPosition()) < 7)
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            }
            case LYCANTHROPE -> {
                if (p.level().isNight()) {
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
                    if (p.getFoodData().getFoodLevel() > 6 && p.tickCount % 160 == 0) p.heal(1f);
                    p.causeFoodExhaustion(.035f);
                    for (var target : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            p.getBoundingBox().inflate(8), e -> e.isAlive()))
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                } else if (p.level().canSeeSky(p.blockPosition())) {
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                }
            }
            case DRAGONBORN -> {
                p.causeFoodExhaustion(.025f);
                if (RaceState.lineage(p) == DragonLineage.VENOM) p.removeEffect(MobEffects.POISON);
            }
            case HARPY -> {
                boolean underground = p.getY() < 50 && !p.level().canSeeSky(p.blockPosition());
                if (underground) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                else if (p.getY() > 100 && p.level().canSeeSky(p.blockPosition()))
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                if (heavyArmorPieces(p) < 3) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
            }
            default -> {}
        }
    }

    private static int heavyArmorPieces(ServerPlayer p) {
        int count = 0;
        for (var stack : p.getArmorSlots()) if (!stack.isEmpty() && stack.getMaxDamage() >= 400) count++;
        return count;
    }

    private static void ambientParticles(ServerPlayer p, Race race) {
        if (!(p.level() instanceof ServerLevel level)) return;
        switch (race) {
            case TIEFLING -> {
                if (p.isInLava() || p.isOnFire())
                    level.sendParticles(ParticleTypes.SMALL_FLAME, p.getX(), p.getY() + .8, p.getZ(), 4, .28, .45, .28, .01);
            }
            case LYCANTHROPE -> {
                if (p.level().isNight() && p.isSprinting())
                    level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + .25, p.getZ(), 5, .3, .12, .3, .02);
            }
            case DRAGONBORN -> {
                if (p.getHealth() < p.getMaxHealth() * .4f)
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 3, .3, .5, .3, .01);
            }
            case HARPY -> {
                if (!p.onGround() && p.getDeltaMovement().y < 0)
                    level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + .2, p.getZ(), 4, .35, .08, .35, .01);
            }
            default -> { }
        }
    }
}
