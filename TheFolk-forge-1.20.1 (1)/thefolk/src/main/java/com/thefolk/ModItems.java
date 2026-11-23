package com.thefolk;

import com.thefolk.item.FolkConfigItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheFolkMod.MOD_ID);

    public static final RegistryObject<Item> FOLK_CONFIG = ITEMS.register("folkconfig",
            () -> new FolkConfigItem(new Item.Properties().stacksTo(1)));
}
