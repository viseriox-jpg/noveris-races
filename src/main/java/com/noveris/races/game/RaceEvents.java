package com.noveris.races.game;

import com.noveris.races.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = NoverisRaces.MOD_ID)
public final class RaceEvents {
    private static final TagKey<net.minecraft.world.item.Item> SILVER_WEAPONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "silver_weapons"));
    private static final TagKey<net.minecraft.world.item.Item> IRON_WEAPONS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "iron_weapons"));
    private static final TagKey<net.minecraft.world.item.Item> HEAVY_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "heavy_armor"));
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
        if (RaceState.customLong(p, "MobilityChargeSystem") == 1
                && RaceState.customLong(p, "MobilityCharges") <= 0
                && RaceState.mobilityReady(p) > 0
                && p.level().getGameTime() >= RaceState.mobilityReady(p)) {
            RaceState.customLong(p, "MobilityCharges", 3);
            RaceState.setMobilityReady(p, 0);
            p.displayClientMessage(Component.literal("Mobilidade recarregada: 3/3 cargas."), true);
        }
        updateScale(p);
        handleHeavyArmorMobility(p, race);
        if (race == Race.FAIRY && ironArmorPieces(p) >= 4)
            p.removeEffect(MobEffects.REGENERATION);
        if (p.tickCount % 20 == 0) {
            applyAttributes(p);
            applyPassives(p, race);
            handleLycanTransformation(p, race);
            RaceGame.sync(p);
        }
        if (p.tickCount % 40 == 0) ambientParticles(p, race);
        RaceAbilities.tickVisuals(p);
        if (race == Race.TIEFLING && p.isOnFire()) p.clearFire();
        if (race == Race.THALASSIAN) tickHydration(p);
        else if (race == Race.HALF_BLOOD && (RaceState.ancestryA(p) == Race.THALASSIAN
                || RaceState.ancestryB(p) == Race.THALASSIAN)) tickHybridHydration(p);
        long aegisWeakness = RaceState.customLong(p, "AegisWeaknessAt");
        if (race == Race.NEPHILIM && aegisWeakness > 0 && p.level().getGameTime() >= aegisWeakness) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            RaceState.customLong(p, "AegisWeaknessAt", 0);
        }
        tickRacialHunger(p, race);
        if (race == Race.HARPY && p.getDeltaMovement().y < -0.12 && heavyArmorPieces(p) < 4)
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, false, false));
    }

    @SubscribeEvent
    public static void preventHeavyArmorMobilityEffects(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer p)
                || RaceState.customLong(p, "HeavyMobilityBlocked") != 1) return;
        Race race = RaceState.race(p);
        var effect = event.getEffectInstance().getEffect();
        int pieces = (int) RaceState.customLong(p, "HeavyArmorPieces");
        boolean blocked = race == Race.SATYR && (effect.equals(MobEffects.MOVEMENT_SPEED)
                || pieces >= 4 && effect.equals(MobEffects.JUMP));
        blocked |= race == Race.HARPY && (effect.equals(MobEffects.MOVEMENT_SPEED)
                || pieces >= 4 && (effect.equals(MobEffects.JUMP) || effect.equals(MobEffects.SLOW_FALLING)));
        // Quatro peças de ferro bloqueiam toda regeneração do Feérico.
        blocked |= race == Race.FAIRY && pieces >= 4 && effect.equals(MobEffects.REGENERATION);
        if (blocked) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        // O ataque normal do Vampiro pode recuperar uma pequena quantidade de vida
        // contra qualquer entidade viva, mas somente durante a noite.
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && RaceState.race(attacker) == Race.VAMPIRE
                && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && attacker.level().isNight()
                && attacker.level().getGameTime() >= RaceState.customLong(attacker, "VampireBiteReady")) {
            // Cura fixa de 0,5 coração para o feedback ser perceptível;
            // o intervalo de 1 segundo impede cura ilimitada por ataques rápidos.
            attacker.heal(1f);
            RaceState.customLong(attacker, "VampireBiteReady", attacker.level().getGameTime() + 20);
        }
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Race race = RaceState.race(victim);
        if (race == Race.HALF_BLOOD) applyHybridDamage(victim, event);
        if (race == Race.ELF && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity
                && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(event.getAmount() * 1.10f);
            if (event.getAmount() >= 6f) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
        if (race == Race.SATYR && event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * .5f);
        if (race == Race.FAIRY && event.getSource().is(DamageTypeTags.IS_FALL)
                && victim.level().getGameTime() < RaceState.customLong(victim, "FaeLandingUntil")) event.setAmount(event.getAmount() * .35f);
        if (race == Race.FAIRY && event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity fairyAttacker
                && isIronWeapon(fairyAttacker.getMainHandItem())) event.setAmount(event.getAmount() * 1.30f);
        if (race == Race.FAIRY && event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) event.setAmount(event.getAmount() * .8f);
        if (race == Race.THALASSIAN && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.6f);
        if (race == Race.THALASSIAN && victim.isInWater() && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount() * .92f);
        if (race == Race.NEPHILIM && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * .50f);
        if (race == Race.VAMPIRE && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.35f);
        if (race == Race.TIEFLING && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            long now = victim.level().getGameTime();
            if (now >= RaceState.customLong(victim, "InfernalRetaliationReady")) {
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
                RaceState.customLong(victim, "InfernalRetaliationReady", now + 200);
            }
            return;
        }
        if (race == Race.DRAGONBORN && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))
            event.setAmount(event.getAmount() * .88f);
        if (race == Race.DRAGONBORN) {
            DragonLineage lineage = RaceState.lineage(victim);
            if (lineage == DragonLineage.FIRE && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * .4f);
            if (lineage == DragonLineage.FIRE && event.getSource().is(DamageTypeTags.IS_FREEZING)) event.setAmount(event.getAmount() * 1.3f);
            if (lineage == DragonLineage.FROST && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.3f);
            if (lineage == DragonLineage.FROST && event.getSource().is(DamageTypeTags.IS_FREEZING)) event.setAmount(event.getAmount() * .4f);
            if (lineage == DragonLineage.VENOM && event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) event.setAmount(event.getAmount() * 1.25f);
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
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.NEPHILIM) event.setAmount(event.getAmount() * .8f);
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.HALF_BLOOD) {
            Race a = RaceState.ancestryA(p), b = RaceState.ancestryB(p);
            if (a == Race.NEPHILIM || b == Race.NEPHILIM) event.setAmount(event.getAmount() * .90f);
            if (a == Race.TIEFLING || b == Race.TIEFLING) event.setAmount(event.getAmount() * .88f);
        }
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.THALASSIAN
                && RaceState.customLong(p, "DryTicks") > 6000) event.setAmount(event.getAmount() * .6f);
        if (event.getEntity() instanceof ServerPlayer p && RaceState.race(p) == Race.FAIRY
                && p.level().dimensionType().ultraWarm()) event.setAmount(event.getAmount() * .7f);
    }

    @SubscribeEvent
    public static void drinkWater(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || RaceState.race(p) != Race.THALASSIAN
                || !event.getItem().is(Items.POTION)) return;
        var contents = event.getItem().get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.is(Potions.WATER)) return;
        long dry = Math.max(0, RaceState.customLong(p, "DryTicks") - 1920);
        RaceState.customLong(p, "DryTicks", dry);
        p.displayClientMessage(Component.literal("Hidratação restaurada em 20%."), true);
    }

    @SubscribeEvent
    public static void fairyPlantFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        boolean fairy = RaceState.race(p) == Race.FAIRY && RaceState.fairyAffinity(p) == FairyAffinity.NATURE;
        boolean hybridFairy = RaceState.race(p) == Race.HALF_BLOOD
                && (RaceState.ancestryA(p) == Race.FAIRY || RaceState.ancestryB(p) == Race.FAIRY);
        if ((fairy || hybridFairy) && isPlantFood(event.getItem())) {
            if (fairy && ironArmorPieces(p) >= 4) {
                p.displayClientMessage(Component.literal("O ferro bloqueia a regeneração feérica."), true);
                return;
            }
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, fairy ? 60 : 35, 0, false, true));
            p.displayClientMessage(Component.literal("Seiva Vital: regeneração fortalecida."), true);
            if (p.level() instanceof ServerLevel level)
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY()+1, p.getZ(), 12, .45, .6, .45, .03);
        }
    }

    private static boolean isPlantFood(ItemStack stack) {
        return stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.BREAD)
                || stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.POTATO)
                || stack.is(Items.BAKED_POTATO) || stack.is(Items.BEETROOT) || stack.is(Items.MELON_SLICE)
                || stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES) || stack.is(Items.CHORUS_FRUIT)
                || stack.is(Items.DRIED_KELP) || stack.is(Items.COOKIE) || stack.is(Items.PUMPKIN_PIE);
    }

    private static boolean isIronWeapon(ItemStack stack) {
        return stack.is(IRON_WEAPONS)
                || stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE)
                || stack.is(Items.IRON_PICKAXE) || stack.is(Items.IRON_SHOVEL)
                || stack.is(Items.IRON_HOE);
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Race race = RaceState.race(p);
        float speed = event.getNewSpeed();
        if (race == Race.THALASSIAN && p.isInWater()) speed *= 5f;
        event.setNewSpeed(speed);
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
            if (race == Race.HARPY)
                value = RaceState.customLong(p, "HeavyMobilityBlocked") == 1 ? .1 : .112;
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
                if (RaceState.visionEnabled(p) && isDark(p)) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
                if (isForest(p)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }
            case FAIRY -> {
                switch (RaceState.fairyAffinity(p)) {
                    case WATER -> {
                        if (p.isInWaterOrRain())
                            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
                    }
                    case AIR -> {
                        if (!p.onGround() && p.getDeltaMovement().y < -0.08)
                            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
                    }
                    default -> {
                        if (isNaturalGround(p) && !nearLava(p)) p.removeEffect(MobEffects.POISON);
                    }
                }
            }
            case SATYR -> {
                int heavy = heavyArmorPieces(p);
                if (isNaturalGround(p)) {
                    if (heavy < 3) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                    if (heavy < 4) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
                }
                else if (p.getY() < 50 && !p.level().canSeeSky(p.blockPosition())) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                if (isForest(p) && p.tickCount % 240 == 0 && !RaceState.inCombat(p)) p.heal(1f);
            }
            case THALASSIAN -> {
                p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false));
                if (p.isInWater()) { if (RaceState.visionEnabled(p) && isDark(p)) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false)); }
            }
            case HUMAN -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false));
            case NEPHILIM -> { if (p.getHealth() <= p.getMaxHealth() * .3f) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false)); }
            case VAMPIRE -> {
                if (RaceState.visionEnabled(p) && isDark(p))
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
                if (p.level().isNight()) { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false)); p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false)); }
            }
            case HALF_BLOOD -> applyHybridPassives(p);
            case TIEFLING -> {
                if (RaceState.visionEnabled(p) && isDark(p))
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
            }
            case LYCANTHROPE -> {
                if (p.level().isNight()) {
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
                    if (p.getFoodData().getFoodLevel() >= 6)
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                    else p.removeEffect(MobEffects.MOVEMENT_SPEED);
                    if (p.getFoodData().getFoodLevel() >= 6 && p.getHealth() < p.getMaxHealth()
                            && p.tickCount % 160 == 0) {
                        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
                        if (p.level() instanceof ServerLevel level)
                            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(),
                                    4, .35, .45, .35, .02);
                    }
                    p.causeFoodExhaustion(.035f);
                    for (var target : p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            p.getBoundingBox().inflate(6), e -> e.isAlive() && p.distanceToSqr(e) <= 36))
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
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
                int heavy = heavyArmorPieces(p);
                if (heavy < 3) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
                else if (heavy == 3) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
            }
            default -> {}
        }
    }

    private static int heavyArmorPieces(ServerPlayer p) {
        int count = 0;
        for (var stack : p.getArmorSlots()) if (isHeavyArmor(stack)) count++;
        return count;
    }

    private static int ironArmorPieces(ServerPlayer p) {
        int count = 0;
        for (var stack : p.getArmorSlots())
            if (stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
                    || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)) count++;
        return count;
    }

    private static boolean isHeavyArmor(ItemStack stack) {
        if (stack.is(HEAVY_ARMOR)) return true;
        if (!(stack.getItem() instanceof ArmorItem armor)) return false;

        var material = armor.getMaterial().value();
        int defense = material.defense().getOrDefault(armor.getType(), 0);
        int threshold = Integer.MAX_VALUE;
        if (armor.getType() == ArmorItem.Type.HELMET) threshold = 3;
        else if (armor.getType() == ArmorItem.Type.CHESTPLATE) threshold = 7;
        else if (armor.getType() == ArmorItem.Type.LEGGINGS) threshold = 6;
        else if (armor.getType() == ArmorItem.Type.BOOTS) threshold = 3;
        return defense >= threshold || material.toughness() >= 2.0f;
    }

    private static void handleHeavyArmorMobility(ServerPlayer p, Race race) {
        boolean susceptible = race == Race.SATYR || race == Race.HARPY;
        int pieces = susceptible ? heavyArmorPieces(p) : 0;
        boolean blocked = susceptible && pieces >= 3;
        boolean wasBlocked = RaceState.customLong(p, "HeavyMobilityBlocked") == 1;

        RaceState.customLong(p, "HeavyArmorPieces", pieces);
        if (blocked) {
            p.removeEffect(MobEffects.MOVEMENT_SPEED);
            if (pieces >= 4) {
                p.removeEffect(MobEffects.JUMP);
                if (race == Race.HARPY) p.removeEffect(MobEffects.SLOW_FALLING);
            }
        }

        if (blocked && (p.tickCount % 40 == 0 || !wasBlocked))
            p.displayClientMessage(Component.literal(pieces == 3
                    ? "⚠ Mobilidade racial parcialmente reduzida: armadura pesada (3/4 peças)"
                    : "⚠ Mobilidade racial severamente reduzida: armadura pesada (4/4 peças)"), true);
        else if (!blocked && wasBlocked)
            p.displayClientMessage(Component.literal("Mobilidade racial restaurada."), true);

        RaceState.customLong(p, "HeavyMobilityBlocked", blocked ? 1 : 0);
    }

    private static boolean isForest(ServerPlayer p) { return p.level().getBiome(p.blockPosition()).is(BiomeTags.IS_FOREST); }
    private static boolean isDark(ServerPlayer p) { return p.level().getRawBrightness(p.blockPosition(), p.level().getSkyDarken()) < 7; }

    private static boolean isNaturalGround(ServerPlayer p) {
        var state = p.level().getBlockState(p.blockPosition().below());
        return state.is(BlockTags.DIRT) || state.is(BlockTags.LEAVES) || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK);
    }
    private static boolean nearFlowers(ServerPlayer p) {
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-3,-1,-3), p.blockPosition().offset(3,2,3)))
            if (p.level().getBlockState(pos).is(BlockTags.FLOWERS)) return true;
        return false;
    }
    private static boolean nearLava(ServerPlayer p) {
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                p.blockPosition().offset(-5,-3,-5), p.blockPosition().offset(5,3,5)))
            if (p.level().getFluidState(pos).is(FluidTags.LAVA)) return true;
        return false;
    }
    private static void tickHydration(ServerPlayer p) {
        tickHydration(p, false);
    }
    private static void tickHybridHydration(ServerPlayer p) {
        tickHydration(p, true);
    }
    private static void tickHydration(ServerPlayer p, boolean hybrid) {
        final long maximum = 9600;
        long before = RaceState.customLong(p, "DryTicks");
        long dry;
        if (p.isInWater()) dry = Math.max(0, before - 32);
        else if (p.isInWaterOrRain()) dry = Math.max(0, before - 4);
        else dry = Math.min(maximum, before + (hybrid && p.tickCount % 2 != 0 ? 0 : 1));
        RaceState.customLong(p, "DryTicks", dry);
        if (dry >= 3600) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        if (dry >= 6000) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, false, false));
        if (dry >= maximum) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            if (p.tickCount % 200 == 0 && p.getHealth() > 2f) {
                p.hurt(p.damageSources().dryOut(), Math.min(2f, p.getHealth() - 2f));
                if (p.level() instanceof ServerLevel level)
                    level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1, p.getZ(), 12, .35, .5, .35, .03);
                p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_HURT,
                        net.minecraft.sounds.SoundSource.PLAYERS, .55f, 1.35f);
            }
        }
        int warning = dry >= maximum ? 3 : dry >= 7200 ? 2 : dry >= 4800 ? 1 : 0;
        int oldWarning = (int) RaceState.customLong(p, "HydrationWarning");
        if (warning > oldWarning) {
            String text = warning == 1 ? "Hidratação em 50%." : warning == 2
                    ? "⚠ Hidratação em 25%." : "⚠ Hidratação esgotada!";
            p.displayClientMessage(Component.literal(text), true);
        }
        RaceState.customLong(p, "HydrationWarning", warning);
    }
    private static void tickRacialHunger(ServerPlayer p, Race race) {
        int current=p.getFoodData().getFoodLevel();
        if(RaceState.customLong(p,"FoodTracked")==0){RaceState.customLong(p,"LastFood",current);RaceState.customLong(p,"FoodTracked",1);return;}
        int previous=(int)RaceState.customLong(p,"LastFood");
        if(race==Race.VAMPIRE&&current>previous){int gain=current-previous;current=previous+Math.max(1,(int)Math.ceil(gain*.7));p.getFoodData().setFoodLevel(current);}
        RaceState.customLong(p,"LastFood",current);
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
        if ((a==Race.ELF||b==Race.ELF) && RaceState.visionEnabled(p) && isDark(p)) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,80,0,false,false));
        if (a==Race.THALASSIAN||b==Race.THALASSIAN) p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,60,0,false,false));
        if ((a==Race.VAMPIRE||b==Race.VAMPIRE)&&p.level().isNight()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,0,false,false));
        if ((a==Race.SATYR||b==Race.SATYR)&&isNaturalGround(p)&&heavyArmorPieces(p)<4) p.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0,false,false));
        if (a==Race.HUMAN||b==Race.HUMAN) p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,40,0,false,false));
        if ((a==Race.NEPHILIM||b==Race.NEPHILIM)&&p.getHealth()<p.getMaxHealth()*.25f) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,40,0,false,false));
        if ((a==Race.LYCANTHROPE||b==Race.LYCANTHROPE)&&p.level().isNight()) {
            p.causeFoodExhaustion(.018f);
            if (p.getFoodData().getFoodLevel() >= 6)
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,0,false,false));
            else p.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
        if ((a==Race.HARPY||b==Race.HARPY)&&!p.onGround()&&p.getDeltaMovement().y<-.15) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,8,0,false,false));
    }
    private static void applyHybridDamage(ServerPlayer p, LivingIncomingDamageEvent event) {
        Race a=RaceState.ancestryA(p),b=RaceState.ancestryB(p);
        java.util.function.Predicate<Race> has=r->a==r||b==r;
        if (has.test(Race.TIEFLING)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*.55f);
        if (has.test(Race.NEPHILIM)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*.8f);
        if (has.test(Race.DRAGONBORN)&&!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount()*.94f);
        if (has.test(Race.DRAGONBORN)) {
            DragonLineage lineage = RaceState.lineage(p);
            if (lineage == DragonLineage.FIRE && event.getSource().is(DamageTypeTags.IS_FREEZING))
                event.setAmount(event.getAmount()*1.15f);
            if (lineage == DragonLineage.FROST && event.getSource().is(DamageTypeTags.IS_FIRE))
                event.setAmount(event.getAmount()*1.15f);
            if (lineage == DragonLineage.VENOM && event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO))
                event.setAmount(event.getAmount()*1.15f);
        }
        if (has.test(Race.ELF)&&event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity
                && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount()*1.05f);
        if (has.test(Race.THALASSIAN)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*1.15f);
        if (has.test(Race.VAMPIRE)&&event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount()*1.15f);
        if (has.test(Race.HARPY)&&event.getSource().is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount()*.6f);
        else if (has.test(Race.HARPY)&&!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))
            event.setAmount(event.getAmount()*1.06f);
        if (has.test(Race.FAIRY)&&event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && isIronWeapon(attacker.getMainHandItem())) event.setAmount(event.getAmount()*1.15f);
        if (has.test(Race.LYCANTHROPE)&&event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker.getMainHandItem().is(SILVER_WEAPONS)) event.setAmount(event.getAmount()*1.25f);
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
