package com.thefolk;

import com.thefolk.entity.TheFolkEntity;
import com.thefolk.entity.TheThingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheFolkMod.MOD_ID);

    public static final RegistryObject<EntityType<TheFolkEntity>> THE_FOLK = ENTITIES.register("the_folk",
            () -> EntityType.Builder.<TheFolkEntity>of(TheFolkEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.9F)
                    .clientTrackingRange(10)
                    .build("the_folk"));

    public static final RegistryObject<EntityType<TheThingEntity>> THE_THING = ENTITIES.register("the_thing",
            () -> EntityType.Builder.<TheThingEntity>of(TheThingEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.2F)
                    .clientTrackingRange(16)
                    .build("the_thing"));
}
