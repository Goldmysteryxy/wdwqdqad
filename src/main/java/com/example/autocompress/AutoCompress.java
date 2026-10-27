package com.example.autocompress;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoCompress implements ModInitializer {
    public static final String MOD_ID = "autocompress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Check every 10 ticks (0.5s) - cheap, and feels instant after pickup.
    private static final int CHECK_INTERVAL = 10;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        CompressConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % CHECK_INTERVAL != 0) return;
            CompressConfig cfg = CompressConfig.get();
            if (!cfg.enabled) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                compress(player, cfg);
            }
        });

        LOGGER.info("Auto Compress loaded");
    }

    private static void compress(ServerPlayer player, CompressConfig cfg) {
        Inventory inv = player.getInventory();
        boolean changed = false;

        for (CompressConfig.Rule rule : cfg.rules) {
            if (!rule.enabled || rule.count < 2) continue;
            Item from = rule.fromItem();
            Item to = rule.toItem();
            if (from == null || to == null) continue;

            int total = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(from)) total += s.getCount();
            }

            int crafts = total / rule.count;
            if (crafts <= 0) continue;

            // Remove the inputs
            int toRemove = crafts * rule.count;
            for (int i = 0; i < inv.getContainerSize() && toRemove > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(from)) continue;
                int take = Math.min(s.getCount(), toRemove);
                s.shrink(take);
                toRemove -= take;
            }

            // Give the outputs. If the inventory is full, hand back the matching inputs instead.
            int maxStack = to.getDefaultMaxStackSize();
            while (crafts > 0) {
                int n = Math.min(crafts, maxStack);
                ItemStack out = new ItemStack(to, n);
                inv.add(out);
                if (!out.isEmpty()) {
                    giveBack(inv, from, out.getCount() * rule.count);
                }
                crafts -= n;
            }
            changed = true;
        }

        if (changed) inv.setChanged();
    }

    private static void giveBack(Inventory inv, Item item, int amount) {
        int max = item.getDefaultMaxStackSize();
        while (amount > 0) {
            int n = Math.min(amount, max);
            ItemStack back = new ItemStack(item, n);
            inv.add(back);
            if (!back.isEmpty()) break; // truly no room; extremely unlikely since we just freed slots
            amount -= n;
        }
    }
}
