package com.example.autocompress;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Auto Compress"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CompressConfig cfg = CompressConfig.get();
        int centerX = this.width / 2;

        StringWidget titleWidget = new StringWidget(this.title, this.font);
        titleWidget.setPosition(centerX - titleWidget.getWidth() / 2, 12);
        addRenderableWidget(titleWidget);

        // Master toggle
        addRenderableWidget(Button.builder(masterLabel(cfg), btn -> {
            cfg.enabled = !cfg.enabled;
            btn.setMessage(masterLabel(cfg));
        }).bounds(centerX - 100, 28, 200, 20).build());

        // Per-rule toggles in two columns
        int colWidth = 150;
        int startY = 58;
        for (int i = 0; i < cfg.rules.size(); i++) {
            CompressConfig.Rule rule = cfg.rules.get(i);
            int col = i % 2;
            int row = i / 2;
            int x = centerX - colWidth - 3 + col * (colWidth + 6);
            int y = startY + row * 24;

            addRenderableWidget(Button.builder(ruleLabel(rule), btn -> {
                rule.enabled = !rule.enabled;
                btn.setMessage(ruleLabel(rule));
            }).bounds(x, y, colWidth, 20).build());
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(centerX - 100, this.height - 28, 200, 20).build());
    }

    private static Component masterLabel(CompressConfig cfg) {
        return Component.literal("Auto Compress: " + (cfg.enabled ? "ON" : "OFF"));
    }

    private static Component ruleLabel(CompressConfig.Rule rule) {
        return Component.literal(rule.label() + ": " + (rule.enabled ? "ON" : "OFF"));
    }

    @Override
    public void onClose() {
        CompressConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}
