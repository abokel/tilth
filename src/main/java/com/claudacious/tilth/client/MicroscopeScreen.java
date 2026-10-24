package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The view down the microscope: a magnified image of each organism found on a plant sample (or healthy
 * tissue), beside its name, symptoms and control. Arrows page between organisms.
 */
public class MicroscopeScreen extends Screen {
    private static final int IMAGE = 128;
    private final CompoundTag sample;
    private final List<CompoundTag> organisms = new ArrayList<>();
    private int page;
    private Button prev;
    private Button next;

    public MicroscopeScreen(CompoundTag sample) {
        super(Component.literal("Microscope"));
        this.sample = sample;
        ListTag list = sample.getList("Afflictions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            organisms.add(list.getCompound(i));
        }
    }

    @Override
    protected void init() {
        int size = viewSize();
        int left = (width - size - 190) / 2;
        int y = (height + size) / 2 + 6;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left, y, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + size - 20, y, 20, 20).build());
        updateButtons();
    }

    private int viewSize() {
        return Math.min(256, Math.min(height - 60, width - 220));
    }

    private void turn(int d) {
        page = Math.max(0, Math.min(organisms.size() - 1, page + d));
        updateButtons();
    }

    private void updateButtons() {
        prev.visible = next.visible = organisms.size() > 1;
        prev.active = page > 0;
        next.active = page < organisms.size() - 1;
    }

    private ResourceLocation image(CompoundTag organism) {
        if (organism == null) {
            return Tilth.id("textures/gui/microscope/healthy.png");
        }
        ResourceLocation id = ResourceLocation.parse(organism.getString("Id"));
        ResourceLocation specific = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/microscope/" + id.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(specific).isPresent()) {
            return specific;
        }
        return Tilth.id("textures/gui/microscope/" + organism.getString("Category") + ".png");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int size = viewSize();
        int left = (width - size - 190) / 2;
        int top = (height - size) / 2 - 4;
        CompoundTag organism = organisms.isEmpty() ? null : organisms.get(page);
        g.fill(left - 4, top - 4, left + size + 4, top + size + 4, 0xFF1A1A1A);
        g.blit(image(organism), left, top, size, size, 0, 0, IMAGE, IMAGE, IMAGE, IMAGE);

        int x = left + size + 14;
        int y = top;
        int w = 176;
        g.drawString(font, Component.literal(sample.getString("Crop")).withStyle(ChatFormatting.WHITE), x, y, 0xFFFFFF);
        y += 10;
        y = wrap(g, sample.getString("Stage"), x, y, w, 0xAAAAAA) + 7;
        if (organism == null) {
            y = wrap(g, "Healthy tissue. Cells are turgid and full of chloroplasts; no hyphae, spores, bacteria or pests are visible.",
                    x, y, w, 0xA8E0A0);
            return;
        }
        g.drawString(font, Component.literal(organism.getString("Name")).withStyle(ChatFormatting.BOLD), x, y, 0xFFE070);
        y += 10;
        g.drawString(font, Component.literal(organism.getString("Sci")).withStyle(ChatFormatting.ITALIC), x, y, 0xCCCCCC);
        y += 10;
        String kind = organism.getString("Category");
        int pct = (int) Math.round(organism.getDouble("Severity") * 100);
        g.drawString(font, kind.substring(0, 1).toUpperCase() + kind.substring(1) + (organism.getBoolean("Beneficial") ? ", beneficial" : "")
                + ", " + pct + "% " + (organism.getBoolean("Beneficial") ? "established" : "severity"), x, y, 0xAAAAAA);
        y += 10;
        g.fill(x, y, x + w, y + 3, 0xFF404040);
        g.fill(x, y, x + w * Math.min(100, pct) / 100, y + 3, organism.getBoolean("Beneficial") ? 0xFF60C060 : 0xFFE05030);
        y += 9;
        y = section(g, "Symptoms", organism.getList("Symptoms", Tag.TAG_STRING), x, y, w);
        y = section(g, "Control", organism.getList("Control", Tag.TAG_STRING), x, y, w);
        if (!organism.getString("Notes").isEmpty()) {
            wrap(g, organism.getString("Notes"), x, y + 2, w, 0x909090);
        }
        if (organisms.size() > 1) {
            g.drawCenteredString(font, (page + 1) + " / " + organisms.size(), left + size / 2, top + size + 12, 0xFFFFFF);
        }
    }

    private int section(GuiGraphics g, String title, ListTag lines, int x, int y, int w) {
        if (lines.isEmpty()) {
            return y;
        }
        g.drawString(font, Component.literal(title).withStyle(ChatFormatting.UNDERLINE), x, y, 0xFFFFFF);
        y += 11;
        for (int i = 0; i < lines.size(); i++) {
            y = wrap(g, "- " + lines.getString(i), x, y, w, 0xD0D0D0);
        }
        return y + 4;
    }

    private int wrap(GuiGraphics g, String text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(Component.literal(text), w)) {
            g.drawString(font, line, x, y, color);
            y += 9;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
