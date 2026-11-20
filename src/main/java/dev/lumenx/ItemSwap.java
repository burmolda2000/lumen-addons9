package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Item swap: по клавише берёт выбранный предмет из ВСЕГО инвентаря (или только хотбара)
 * и меняет его местами с рукой. Повторное нажатие возвращает всё назад.
 */
public final class ItemSwap {
    static final String[] NAMES = {
            "Totem of Undying", "Golden apple", "Enchanted golden apple", "Ender pearl",
            "Firework rocket", "End crystal", "Experience bottle", "Shield", "Water bucket"
    };
    static final Item[] ITEMS = {
            Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.ENDER_PEARL,
            Items.FIREWORK_ROCKET, Items.END_CRYSTAL, Items.EXPERIENCE_BOTTLE, Items.SHIELD, Items.WATER_BUCKET
    };

    public static final Module M = new Module("Item swap",
            "Swap items from inventory or hotbar with a key", Category.UTILITY, true, true);
    static final Setting SCAN   = M.add(Setting.mode("scan", "Scan", 0, "Inventory", "Hotbar"));
    static final Setting TARGET = M.add(Setting.mode("target", "Swap into", 0, "Offhand", "Main hand"));
    static final Setting ITEM   = M.add(Setting.mode("item", "Item", 0, NAMES));
    static final Setting BACK   = M.add(Setting.bool("swap_back", "Press again to swap back", true));

    public static KeyBinding KEY;

    /** Контейнерный слот, откуда взяли предмет (для возврата), либо -1. */
    private static int backSlot = -1;

    static void init() {
        KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenx.item_swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                LumenAddons.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (KEY.wasPressed()) trigger(mc);
        });
    }

    private static void trigger(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || !M.enabled) return;
        if (mc.currentScreen != null) return;

        PlayerInventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        boolean offhand = TARGET.mode == 0;
        Item want = ITEMS[Math.max(0, Math.min(ITEMS.length - 1, ITEM.mode))];
        ItemStack held = offhand ? p.getOffHandStack() : p.getMainHandStack();
        int button = offhand ? 40 : sel;           // 40 = слот второй руки для SWAP
        boolean alreadyThere = held.isOf(want);

        int from;
        if (alreadyThere) {
            if (!BACK.bool || backSlot < 0) { say(p, "already in hand"); return; }
            from = backSlot;
        } else {
            from = find(inv, want, offhand ? -1 : sel);
            if (from < 0) { say(p, "not found"); return; }
        }

        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, from, button,
                SlotActionType.SWAP, p);
        backSlot = alreadyThere ? -1 : from;
    }

    /** Возвращает номер слота в PlayerScreenHandler: хотбар 36-44, инвентарь 9-35. */
    private static int find(PlayerInventory inv, Item want, int skipHotbar) {
        for (int i = 0; i < 9; i++) {
            if (i == skipHotbar) continue;
            if (inv.getStack(i).isOf(want)) return 36 + i;
        }
        if (SCAN.mode == 0) {
            for (int i = 9; i < 36; i++) {
                if (inv.getStack(i).isOf(want)) return i;
            }
        }
        return -1;
    }

    private static void say(ClientPlayerEntity p, String msg) {
        p.sendMessage(Text.literal("Item swap: " + msg), true);
    }
}
