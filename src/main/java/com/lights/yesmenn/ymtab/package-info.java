/**
 * Shared "YesMenn!" creative tab with sections (Lights, Technicals, Decor, Other), used by all YM mods.
 * <p>
 * The mods do not depend on each other. Each one ships a copy of this package and takes part like this:
 * <ol>
 *     <li>Copy {@code ymtab} and {@code ymtab.client} into the mod (only the package name changes), plus the two
 *     mixins {@code MixinYmCreativeModeInventoryScreen} and {@code MixinYmAbstractContainerScreen} into its mixin
 *     package, listed under {@code "client"} in its mixin config.</li>
 *     <li>Mark the mod in {@code neoforge.mods.toml}:
 *     <pre>
 * [modproperties.${mod_id}]
 * yesmenn_tab = true</pre></li>
 *     <li>Call {@code YmCreativeTab.register(MODID, modEventBus, () -> new ItemStack(iconItem))} in the mod
 *     constructor (instead of registering an own creative tab).</li>
 *     <li>Put the mod's items into the section tags {@code data/yesmenn/tags/item/creative/<lights|technicals|decor|other>.json}
 *     ({@code "replace": false}). The order in the tag file is the order in the tab.</li>
 * </ol>
 * Of all loaded marked mods, the one with the alphabetically smallest mod ID leads: only it registers the tab and
 * draws the sections; the copies in the other mods stay inactive. Banners are
 * {@code assets/yesmenn/textures/gui/creative_sections/<section>.png} (ratio 9:1, e.g. 648x72) and the texts
 * {@code itemGroup.yesmenn} / {@code yesmenn.creative_section.<section>} in {@code assets/yesmenn/lang}; a missing
 * banner is drawn as a plain gradient. Keep the copies of this package in step when changing it.
 */
package com.lights.yesmenn.ymtab;
