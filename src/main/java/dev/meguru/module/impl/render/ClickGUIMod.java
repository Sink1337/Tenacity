package dev.meguru.module.impl.render;

import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.settings.impl.BooleanSetting;
import dev.meguru.module.settings.impl.ModeSetting;
import dev.meguru.module.settings.impl.NumberSetting;
import dev.meguru.ui.clickguis.dropdown.DropdownClickGUI;
import dev.meguru.ui.clickguis.modern.ModernClickGui;
import dev.meguru.utils.render.Theme;
import org.lwjgl.input.Keyboard;

public class ClickGUIMod extends Module {

    public static final ModeSetting clickguiMode = new ModeSetting("ClickGui", "Dropdown", "Dropdown", "Modern");
    public static final ModeSetting scrollMode = new ModeSetting("Scroll Mode", "Screen Height", "Screen Height", "Value");
    public static final BooleanSetting gradient = new BooleanSetting("Gradient", false);
    public static final BooleanSetting outlineAccent = new BooleanSetting("Outline Accent", true);
    public static final BooleanSetting transparent = new BooleanSetting("Transparent", false);
    public static final BooleanSetting walk = new BooleanSetting("Allow Movement", true);
    public static final NumberSetting clickHeight = new NumberSetting("Tab Height", 250, 500, 100, 1);
    public static final BooleanSetting rescale = new BooleanSetting("Rescale GUI", true);

    public static final DropdownClickGUI dropdownClickGui = new DropdownClickGUI();
    public static final ModernClickGui modernClickGui = new ModernClickGui();

    private int activeCategory = 0;
    private Category activeCategory2 = Category.COMBAT;

    public static int prevGuiScale;

    public ClickGUIMod() {
        super("ClickGUI", Category.RENDER, "Displays modules");
        clickHeight.addParent(scrollMode, selection -> selection.is("Value"));

        gradient.addParent(clickguiMode, selection -> selection.is("Dropdown") && !Theme.getCurrentTheme().isGradient());
        transparent.addParent(clickguiMode, selection -> selection.is("Dropdown"));
        outlineAccent.addParent(clickguiMode, selection -> selection.is("Dropdown"));
        scrollMode.addParent(clickguiMode, selection -> selection.is("Dropdown"));

        this.addSettings(clickguiMode, scrollMode, outlineAccent, gradient, transparent, walk, clickHeight, rescale);
        this.setKey(Keyboard.KEY_RSHIFT);
    }

    public void toggle() {
        this.onEnable();
    }

    public void onEnable() {
        if (rescale.isEnabled()) {
            prevGuiScale = mc.gameSettings.guiScale;
            mc.gameSettings.guiScale = 2;
        }
        switch (clickguiMode.getMode()) {
            case "Dropdown":
                mc.displayGuiScreen(dropdownClickGui);
                break;
            case "Modern":
                mc.displayGuiScreen(modernClickGui);
                break;

        }
    }


    public int getActiveCategoryy() {
        return activeCategory;
    }

    public Category getActiveCategory() {
        return activeCategory2;
    }

    public void setActiveCategory(int activeCategory) {
        this.activeCategory = activeCategory;
    }

    public void setActiveCategory(Category activeCategory) {
        this.activeCategory2 = activeCategory;
    }

}
