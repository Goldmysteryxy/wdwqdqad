package com.example.autocompress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompressConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CompressConfig instance;

    public boolean enabled = true;
    public List<Rule> rules = new ArrayList<>();

    /** One compression rule: `count` of `from` become 1 of `to`. */
    public static class Rule {
        public String from;
        public String to;
        public int count = 9;
        public boolean enabled = true;

        private transient Item fromItem;
        private transient Item toItem;

        public Rule() {}

        public Rule(String from, String to, int count, boolean enabled) {
            this.from = from;
            this.to = to;
            this.count = count;
            this.enabled = enabled;
        }

        public Item fromItem() {
            if (fromItem == null) fromItem = lookup(from);
            return fromItem;
        }

        public Item toItem() {
            if (toItem == null) toItem = lookup(to);
            return toItem;
        }

        public String label() {
            return pretty(from) + " -> " + pretty(to);
        }

        private static Item lookup(String id) {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed == null) return null;
            return BuiltInRegistries.ITEM.getOptional(parsed).orElse(null);
        }

        private static String pretty(String id) {
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            StringBuilder sb = new StringBuilder();
            for (String part : path.split("_")) {
                if (part.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return sb.toString();
        }
    }

    private static CompressConfig defaults() {
        CompressConfig c = new CompressConfig();
        // Order matters: nuggets are processed before ingots so chains work in one pass.
        c.rules.add(new Rule("minecraft:gold_nugget", "minecraft:gold_ingot", 9, true));
        c.rules.add(new Rule("minecraft:gold_ingot", "minecraft:gold_block", 9, true));
        c.rules.add(new Rule("minecraft:iron_nugget", "minecraft:iron_ingot", 9, false));
        c.rules.add(new Rule("minecraft:iron_ingot", "minecraft:iron_block", 9, false));
        c.rules.add(new Rule("minecraft:copper_ingot", "minecraft:copper_block", 9, false));
        c.rules.add(new Rule("minecraft:diamond", "minecraft:diamond_block", 9, false));
        c.rules.add(new Rule("minecraft:emerald", "minecraft:emerald_block", 9, false));
        c.rules.add(new Rule("minecraft:redstone", "minecraft:redstone_block", 9, false));
        c.rules.add(new Rule("minecraft:lapis_lazuli", "minecraft:lapis_block", 9, false));
        c.rules.add(new Rule("minecraft:coal", "minecraft:coal_block", 9, false));
        return c;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("autocompress.json");
    }

    public static CompressConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                CompressConfig loaded = GSON.fromJson(Files.readString(p), CompressConfig.class);
                if (loaded != null && loaded.rules != null) {
                    instance = loaded;
                    return;
                }
            } catch (Exception e) {
                AutoCompress.LOGGER.error("Failed to read config, using defaults", e);
            }
        }
        instance = defaults();
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (IOException e) {
            AutoCompress.LOGGER.error("Failed to save config", e);
        }
    }
}
