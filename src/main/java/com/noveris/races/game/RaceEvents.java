package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
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
        updateScale(p);
        if (p.tickCount % 20 == 0) {
            applyAttributes(p);
            applyPassives(p, race);
            handleLycanTransformation(p, race);
            RaceGame.sync(p);
        }
        if (p.tickCount % 40 == 0) ambientParticles(p, race);
        if (race == Race.TIEFLING && p.isOnFire()) p.clearFire();
        if (race == Race.THALASSIAN) tickHydration(p);
        if (race == Race.HARPY && p.getDeltaMovement().y < -0.12 && heavyArmorPieces(p) < 3)
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, false, false));
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Race race = RaceState.race(victim);
        if (race == Race.HALF_BLOOD) applyHybridDamage(victim, event);
        if (race == Race.ELF && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount() * 1.12f);
        if (race == Race.ELF && event.getAmount() >= 6f && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity)
            event.setAmount(event.getAmount() * 1.10f);
        if (race == Race.SATYR && event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * .4f);
        if (race == Race.FAIRY && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity fairyAttacker
                && fairyAttacker.getMainHandItem().getItem().toString().contains("iron")) event.setAmount(event.getAmount() * 1.3f);
        if (race == Race.THALASSIAN && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.3f);
        if (race == Race.NEPHILIM && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * .65f);
        if (race == Race.VAMPIRE && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.35f);
        if (race == Race.REVENANT && event.getAmount() >= victim.getHealth()
                && victim.level().getGameTime() >= RaceState.customLong(victim, "DeathDefianceReady")) {
            event.setAmount(Math.max(0, victim.getHealth() - 1f));
            RaceState.customLong(victim, "DeathDefianceReady", victim.level().getGameTime() + 6000);
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
        }
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
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && RaceState.race(attacker) == Race.VAMPIRE
                && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) attacker.heal(Math.min(1f, event.getAmount() * .08f));
        if (event.getSource().getEntity() instanceof ServerPlayer archer && RaceState.race(archer) == Race.ELF
                && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount() * 1.12f);
        RaceState.markCombat(victim);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) RaceState.markCombat(attacker);
    }

    @SubscribeEvent
    public static void healing(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.TIEFLING
                && p.getFoodData().getFoodLevel() >= 18 && event.getAmount() <= 1.0f)
            event.setAmount(event.getAmount() * .75f);
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.NEPHILIM) event.setAmount(event.getAmount() * .8f);
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.REVENANT) event.setAmount(event.getAmount() * .6f);
    }

    private static void applyAttributes(ServerPlayer p) {
        Race race = RaceState.race(p);
        var maxHealth = p.getAttribute(Attributes.MAX_HEALTH);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        var knockback = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        var blockReach = p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityReach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (maxHealth != null) maxHealth.setBaseValue(race.maxHealth);
        if (speed != null) {
            double value = .1;
            if (race == Race.DRAGONBORN) value = .092;
            if (race == Race.HARPY) value = .112;
            if (race == Race.LYCANTHROPE && p.level().isNight()) value = .11;
            speed.setBaseValue(value);
        }
        if (knockback != null) knockback.setBaseValue(race == Race.DRAGONBORN ? .2 : 0);
        boolean small = RaceState.size(p) == RaceSize.SMALL;
        if (blockReach != null) blockReach.setBaseValue(small ? 4.275 : 4.5);
        if (entityReach != null) entityReach.setBaseValue(small ? 2.85 : 3.0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void updateScale(ServerPlayer p) {
        var attribute = p.getAttribute(Attributes.SCALE);
        if (attribute == null) return;
        double current = attribute.getBaseValue(), target = RaceState.effectiveScale(p);
        if (Math.abs(current - target) < .002) { attribute.setBaseValue(target); return; }
        if (target > current && !p.level().noCollision(p, p.getBoundingBox().expandTowards(0, .18, 0))) return;
        attribute.setBaseValue(current + Math.copySign(Math.min(.008, Math.abs(target - current)), target - current));
    }

    private static void applyPassives(ServerPlayer p, Race race) {
        switch (race) {
            case ELF -> {
                if (p.level().getMaxLocalRawBrightness(p.blockPosition()) < 7) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
                if (isNaturalGround(p)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }
            case FAIRY -> {
                if (nearFlowers(p) && p.tickCount % 100 == 0) p.heal(1f);
                if (p.level().dimensionType().ultraWarm()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            }
            case SATYR -> {
                if (isNaturalGround(p) && heavyArmorPieces(p) < 3) { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false)); }
                else if (p.getY() < 50 && !p.level().canSeeSky(p.blockPosition())) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            }
            case THALASSIAN -> {
                p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false));
                if (p.isInWater()) { p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false)); }
            }
            case HUMAN -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false));
            case NEPHILIM -> { if (p.getHealth() <= p.getMaxHealth() * .3f) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false)); }
            case VAMPIRE -> {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
                if (p.level().isNight()) { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false)); }
                else if (p.level().canSeeSky(p.blockPosition())) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            }
            case REVENANT -> p.removeEffect(MobEffects.POISON);
            case HALF_BLOOD -> applyHybridPassives(p);
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

    private static boolean isNaturalGround(ServerPlayer p) {
        var state = p.level().getBlockState(p.blockPosition().below());
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK);
    }
    private static boolean nearFlowers(ServerPlayer p) {
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-3,-1,-3), p.blockPosition().offset(3,2,3)))
            if (p.level().getBlockState(pos).is(BlockTags.FLOWERS)) return true;
        return false;
    }
    private static void tickHydration(ServerPlayer p) {
        long dry = p.isInWaterOrRain() ? 0 : RaceState.customLong(p, "DryTicks") + 1;
        RaceState.customLong(p, "DryTicks", dry);
        if (dry > 2400) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, dry > 6000 ? 1 : 0, false, false));
        if (dry > 6000) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
    }
    private static void handleLycanTransformation(ServerPlayer p, Race race) {
        boolean affected = race == Race.LYCANTHROPE || (race == Race.HALF_BLOOD && (RaceState.ancestryA(p) == Race.LYCANTHROPE || RaceState.ancestryB(p) == Race.LYCANTHROPE));
        if (!affected) { RaceState.customLong(p, "WasNight", p.level().isNight() ? 1 : 0); return; }
        long night = p.level().isNight() ? 1 : 0, before = RaceState.customLong(p, "WasNight");
        if (before != night && p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY()+1, p.getZ(), 28, .55, .8, .55, .04);
            level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY()+1, p.getZ(), 18, .5, .7, .5, .06);
            p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.WOLF_HOWL, net.minecraft.sounds.SoundSource.PLAYERS, 1.1f, night == 1 ? .65f : 1.05f);
        }
        RaceState.customLong(p, "WasNight", night);
    }
    private static void applyHybridPassives(ServerPlayer p) {
        Race a=RaceState.ancestryA(p),b=RaceState.ancestryB(p);
        if (a==Race.ELF||b==Race.ELF) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,80,0,false,false));
        if ((a==Race.FAIRY||b==Race.FAIRY)&&nearFlowers(p)&&p.tickCount%200==0) p.heal(1f);
        if (a==Race.THALASSIAN||b==Race.THALASSIAN) p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,60,0,false,false));
        if ((a==Race.VAMPIRE||b==Race.VAMPIRE)&&p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,0,false,false));
        if ((a==Race.SATYR||b==Race.SATYR)&&isNaturalGround(p)) p.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0,false,false));
        if (a==Race.HUMAN||b==Race.HUMAN) p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,40,0,false,false));
        if ((a==Race.NEPHILIM||b==Race.NEPHILIM)&&p.getHealth()<p.getMaxHealth()*.25f) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,40,0,false,false));
        if (a==Race.REVENANT||b==Race.REVENANT) p.removeEffect(MobEffects.POISON);
        if ((a==Race.LYCANTHROPE||b==Race.LYCANTHROPE)&&p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,0,false,false));
        if ((a==Race.HARPY||b==Race.HARPY)&&!p.onGround()&&p.getDeltaMovement().y<-.15) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,8,0,false,false));
    }
    private static void applyHybridDamage(ServerPlayer p, LivingIncomingDamageEvent event) {
        Race a=RaceState.ancestryA(p),b=RaceState.ancestryB(p);
        java.util.function.Predicate<Race> has=r->a==r||b==r;
        if (has.test(Race.TIEFLING)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*.55f);
        if (has.test(Race.NEPHILIM)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*.8f);
        if (has.test(Race.DRAGONBORN)&&!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount()*.94f);
        if (has.test(Race.ELF)&&!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount()*1.06f);
        if (has.test(Race.THALASSIAN)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*1.15f);
        if (has.test(Race.VAMPIRE)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*1.18f);
        if (has.test(Race.HARPY)&&event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount()*.6f);
        if (has.test(Race.FAIRY)&&event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker.getMainHandItem().getItem().toString().contains("iron")) event.setAmount(event.getAmount()*1.15f);
        if (has.test(Race.LYCANTHROPE)&&event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker.getMainHandItem().is(SILVER_WEAPONS)) event.setAmount(event.getAmount()*1.17f);
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
