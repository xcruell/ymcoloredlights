package com.lights.yesmenn.ymtab;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The shared "YesMenn!" creative tab of all YM mods. See {@code package-info.java} for how a mod takes part.
 * <p>
 * Every YM mod ships this package unchanged (only the package name differs) and marks itself with
 * {@code yesmenn_tab = true} in its {@code neoforge.mods.toml} mod properties. Of all loaded marked mods, the one with
 * the alphabetically smallest mod ID is the leader: only it registers the tab and draws the sections, the others stay
 * passive. The tab is filled from the item tags {@code yesmenn:creative/<section>}, so a mod only adds tag files.
 */
public final class YmCreativeTab {
    /** Mod property marking a mod that takes part in the shared tab. */
    public static final String MARKER = "yesmenn_tab";
    /** Shared namespace of the section tags, banner textures and translations. */
    public static final String NAMESPACE = "yesmenn";

    /** Sections in display order. */
    public enum Section {
        LIGHTS("lights", 0xFFE7A93C),
        TECHNICALS("technicals", 0xFF5C8FD6),
        DECOR("decor", 0xFF6FB04E),
        OTHER("other", 0xFF9A9A9A);

        private final String id;
        private final int fallbackColor;
        private final TagKey<Item> tag;

        Section(String id, int fallbackColor) {
            this.id = id;
            this.fallbackColor = fallbackColor;
            this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NAMESPACE, "creative/" + id));
        }

        public String id() {
            return id;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        /** ARGB color of the plain banner drawn while no banner texture exists. */
        public int fallbackColor() {
            return fallbackColor;
        }

        public Component title() {
            return Component.translatable(NAMESPACE + ".creative_section." + id);
        }

        public ResourceLocation banner() {
            return ResourceLocation.fromNamespaceAndPath(NAMESPACE, "textures/gui/creative_sections/" + id + ".png");
        }

        /** First section whose tag holds the stack's item, or null. */
        @Nullable
        public static Section of(ItemStack stack) {
            for (Section section : values()) {
                if (stack.is(section.tag)) {
                    return section;
                }
            }
            return null;
        }
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab;
    private static Boolean leader;

    private YmCreativeTab() {
    }

    /**
     * Registers the shared tab if this mod is the leader. Call from the mod constructor.
     *
     * @param modId this mod's ID
     * @param icon  icon of the tab while this mod is the leader
     */
    public static void register(String modId, IEventBus modEventBus, Supplier<ItemStack> icon) {
        if (!isLeader(modId)) {
            return;
        }
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modId);
        tab = tabs.register("yesmenn", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + NAMESPACE))
                .icon(icon)
                .displayItems((parameters, output) -> fill(parameters.holders(), output))
                .build());
        tabs.register(modEventBus);
    }

    /** True if this mod draws the shared tab. */
    public static boolean isLeader(String modId) {
        if (leader == null) {
            String first = modId;
            for (IModInfo mod : ModList.get().getMods()) {
                if (isMarked(mod) && mod.getModId().compareTo(first) < 0) {
                    first = mod.getModId();
                }
            }
            leader = first.equals(modId);
        }
        return leader;
    }

    /** True for the shared tab while this mod is the leader. */
    public static boolean isTab(@Nullable CreativeModeTab creativeTab) {
        return creativeTab != null && tab != null && tab.isBound() && tab.get() == creativeTab;
    }

    private static void fill(HolderLookup.Provider holders, CreativeModeTab.Output output) {
        Optional<? extends HolderLookup.RegistryLookup<Item>> items = holders.lookup(Registries.ITEM);
        if (items.isEmpty()) {
            return;
        }
        // An item in several section tags shows up in the first one only (the tab refuses duplicates).
        Set<Item> added = new HashSet<>();
        Set<String> ymMods = markedMods();
        for (Section section : Section.values()) {
            Optional<HolderSet.Named<Item>> tagged = items.get().get(section.tag());
            if (tagged.isEmpty()) {
                continue;
            }
            for (Holder<Item> holder : tagged.get()) {
                if (added.add(holder.value())) {
                    output.accept(new ItemStack(holder));
                }
            }
        }
        // Safety net: items of YM mods that are in no section tag still show up (under Other, see the layout).
        items.get().listElements()
                .filter(holder -> ymMods.contains(holder.key().location().getNamespace()))
                .filter(holder -> added.add(holder.value()))
                .forEach(holder -> output.accept(new ItemStack(holder)));
    }

    /** Mod IDs of all loaded mods taking part in the shared tab. */
    private static Set<String> markedMods() {
        Set<String> mods = new HashSet<>();
        for (IModInfo mod : ModList.get().getMods()) {
            if (isMarked(mod)) {
                mods.add(mod.getModId());
            }
        }
        return mods;
    }

    private static boolean isMarked(IModInfo mod) {
        Object marker = mod.getModProperties().get(MARKER);
        return marker != null && !"false".equals(String.valueOf(marker));
    }

    /** Sections in display order, as a list. */
    public static List<Section> sections() {
        return List.of(Section.values());
    }
}
