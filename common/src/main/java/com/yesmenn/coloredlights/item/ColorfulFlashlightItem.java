package com.yesmenn.coloredlights.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * Handheld colored spotlight. Use toggles the beam, sneak + use opens the light settings.
 */
public class ColorfulFlashlightItem extends Item {
    public ColorfulFlashlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                ClientHooks.openScreen(hand);
            }
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide) {
            boolean enabled = !FlashlightSettings.isEnabled(stack);
            FlashlightSettings.setEnabled(stack, enabled);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.35F, enabled ? 1.4F : 1.0F);
        }
        return InteractionResultHolder.consume(stack);
    }

    // Toggling or retuning the beam only changes custom data; keep the flashlight steady in the hand. Neither
    // method has @Override since the common code sees neither loader's item extension.

    /** NeoForge: {@code IItemExtension#shouldCauseReequipAnimation}. */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /** Fabric: {@code FabricItem#allowComponentsUpdateAnimation}, only asked while the item stays the same. */
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack,
                                                  ItemStack newStack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        FlashlightSettings settings = FlashlightSettings.read(stack);
        tooltipComponents.add(Component.translatable(FlashlightSettings.isEnabled(stack)
                        ? "tooltip.ymcoloredlights.colorful_flashlight.on"
                        : "tooltip.ymcoloredlights.colorful_flashlight.off")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                        "tooltip.ymcoloredlights.colorful_flashlight.config",
                        settings.range(),
                        String.format("#%06X", settings.color()),
                        settings.brightness(),
                        settings.angle())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.ymcoloredlights.colorful_flashlight.toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.ymcoloredlights.colorful_flashlight.configure")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // Resolved on first client use; dedicated servers never reach this holder.
    private static final class ClientHooks {
        private static final MethodHandle OPEN_SCREEN = find();

        private static MethodHandle find() {
            try {
                return MethodHandles.publicLookup().findStatic(
                        Class.forName("com.yesmenn.coloredlights.client.ColoredLightClientHooks"),
                        "openFlashlightScreen",
                        MethodType.methodType(void.class, InteractionHand.class));
            } catch (ClassNotFoundException | LinkageError ignored) {
                return null;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Missing flashlight screen hook", exception);
            }
        }

        private static void openScreen(InteractionHand hand) {
            if (OPEN_SCREEN == null) {
                return;
            }
            try {
                OPEN_SCREEN.invokeExact(hand);
            } catch (Throwable exception) {
                throw new IllegalStateException("Could not open flashlight screen.", exception);
            }
        }
    }
}
