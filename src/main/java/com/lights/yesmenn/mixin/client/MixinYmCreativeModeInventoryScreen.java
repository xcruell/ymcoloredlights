package com.lights.yesmenn.mixin.client;

import com.lights.yesmenn.ymtab.client.YmCreativeSections;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Sections in the shared YesMenn! tab: lays the items out in sections when the tab is filled, draws the banners over
 * the empty slot grid and makes the banner slots ignore clicks. Every YM mod applies this mixin, but it only acts in
 * the mod that leads the shared tab (in the others the tab is never "shared"). Helpers are {@link Unique} so the
 * copies of several mods do not clash.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinYmCreativeModeInventoryScreen {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void ym$layoutOnSelect(CreativeModeTab tab, CallbackInfo ci) {
        ym$layout();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void ym$layoutOnRefresh(Collection<?> items, CallbackInfo ci) {
        ym$layout();
    }

    @Unique
    private void ym$layout() {
        CreativeModeInventoryScreen.ItemPickerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        boolean shared = YmCreativeSections.isSharedTab(selectedTab);
        YmCreativeSections.layout(shared, menu.items);
        if (shared) {
            menu.scrollTo(scrollOffs);
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void ym$renderBanners(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (YmCreativeSections.isSharedTab(selectedTab)) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
            YmCreativeSections.render(graphics, screen.getGuiLeft(), screen.getGuiTop(), screen.getMenu().items.size(),
                    scrollOffs);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void ym$ignoreBannerClicks(@Nullable Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (slot != null && YmCreativeSections.isSharedTab(selectedTab)
                && YmCreativeSections.isBannerSlot(slot, ((CreativeModeInventoryScreen) (Object) this).getMenu().items.size(),
                scrollOffs)) {
            ci.cancel();
        }
    }
}
