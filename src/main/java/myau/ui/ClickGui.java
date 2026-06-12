
package myau.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.ClientInfo;
import myau.Myau;
import myau.config.online.OnlineConfigApplier;
import myau.config.online.OnlineConfigClient;
import myau.config.online.OnlineConfigEntry;
import myau.module.Module;
import myau.module.modules.*;
import myau.module.modules.combat.*;
import myau.module.modules.movement.*;
import myau.module.modules.render.*;
import myau.module.modules.player.*;
import myau.module.modules.misc.*;
import myau.module.modules.latency.*;
import myau.ui.components.CategoryComponent;
import myau.ui.components.OnlineConfigComponent;
import myau.ui.components.TargetPropertyComponent;
import myau.util.ChatUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClickGui extends GuiScreen {
    private static final Logger CLICK_GUI_LOGGER = Logger.getLogger(ClickGui.class.getName());
    private static final ExecutorService ONLINE_CONFIG_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "OpenMiau OnlConfigs");
        thread.setDaemon(true);
        return thread;
    });
    private static ClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;
    private final OnlineConfigClient onlineConfigClient = new OnlineConfigClient();
    private CategoryComponent onlineConfigCategory;

    public ClickGui() {
        instance = this;

        this.categoryList = new ArrayList<>();
        int topOffset = 5;

        for (Map.Entry<String, List<Module>> entry : Myau.moduleManager.getModulesByCategory().entrySet()) {
            CategoryComponent category = new CategoryComponent(entry.getKey(), entry.getValue());
            category.setY(topOffset);
            categoryList.add(category);
            topOffset += 20;
        }

        CategoryComponent targets = new CategoryComponent("Targets");
        targets.setY(topOffset);
        Targets targetSettings = (Targets) Myau.moduleManager.getModule(Targets.class);
        ArrayList<Component> targetComponents = new ArrayList<>();
        targetComponents.add(new TargetPropertyComponent(targetSettings.players, targets, 16));
        targetComponents.add(new TargetPropertyComponent(targetSettings.invisibles, targets, 28));
        targetComponents.add(new TargetPropertyComponent(targetSettings.bosses, targets, 40));
        targetComponents.add(new TargetPropertyComponent(targetSettings.mobs, targets, 52));
        targetComponents.add(new TargetPropertyComponent(targetSettings.animals, targets, 64));
        targetComponents.add(new TargetPropertyComponent(targetSettings.golems, targets, 76));
        targetComponents.add(new TargetPropertyComponent(targetSettings.silverfish, targets, 88));
        targetComponents.add(new TargetPropertyComponent(targetSettings.teams, targets, 100));
        targets.setComponents(targetComponents);
        categoryList.add(targets);
        topOffset += 20;

        this.onlineConfigCategory = new CategoryComponent("OnlConfigs");
        this.onlineConfigCategory.setY(topOffset);
        this.setOnlineConfigStatus("Loading...", "refreshing list", null);
        categoryList.add(this.onlineConfigCategory);

        loadPositions();
    }
    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
        this.refreshOnlineConfigs();
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        mc.fontRendererObj.drawStringWithShadow(ClientInfo.getClickGuiVersion(), 4,
                this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawStringWithShadow("dev, ksyz, idle", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT,
                new Color(60, 162, 253).getRGB());

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> btnCat = categoryList.iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y)
                            && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.mousePressed(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.setPin(!category.isPin());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : new ArrayList<>(category.getModules())) {
                c.mouseDown(x, y, mouseButton);
            }
        }

    }

    public void mouseReleased(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> iterator = categoryList.iterator();

        CategoryComponent categoryComponent;
        while (iterator.hasNext()) {
            categoryComponent = iterator.next();
            if (mouseButton == 0) {
                categoryComponent.mousePressed(false);
            }
        }

        iterator = categoryList.iterator();

        while (true) {
            do {
                do {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    categoryComponent = iterator.next();
                } while (!categoryComponent.isOpened());
            } while (categoryComponent.getModules().isEmpty());

            for (Component component : new ArrayList<>(categoryComponent.getModules())) {
                component.mouseReleased(x, y, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = categoryList.iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : new ArrayList<>(cat.getModules())) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void refreshOnlineConfigs() {
        this.setOnlineConfigStatus("Loading...", "fetching configs", null);
        ONLINE_CONFIG_EXECUTOR.execute(() -> {
            try {
                List<OnlineConfigEntry> entries = Collections
                        .unmodifiableList(new ArrayList<>(this.onlineConfigClient.list()));
                mc.addScheduledTask(() -> this.setOnlineConfigEntries(entries));
            } catch (Exception e) {
                mc.addScheduledTask(() -> this.setOnlineConfigStatus("Fetch failed", e.getMessage(), null));
            }
        });
    }

    private void setOnlineConfigEntries(List<OnlineConfigEntry> entries) {
        if (entries.isEmpty()) {
            this.setOnlineConfigStatus("No configs", "online list is empty", null);
            return;
        }
        ArrayList<Component> components = new ArrayList<>();
        int offset = 16;
        for (OnlineConfigEntry entry : entries) {
            String subtitle = "by " + entry.getAuthor() + " | " + safe(entry.setting_type);
            if (!entry.getVersion().isEmpty()) {
                subtitle += " • v" + entry.getVersion();
            }
            components.add(new OnlineConfigComponent(this.onlineConfigCategory, offset, entry.getName(), subtitle,
                    () -> this.loadOnlineConfig(entry)));
            offset += 24;
        }
        this.onlineConfigCategory.setComponents(components);
    }

    private void setOnlineConfigStatus(String title, String subtitle, Runnable action) {
        if (this.onlineConfigCategory == null)
            return;
        ArrayList<Component> components = new ArrayList<>();
        components.add(new OnlineConfigComponent(this.onlineConfigCategory, 16, title, subtitle, action));
        this.onlineConfigCategory.setComponents(components);
    }

    private void loadOnlineConfig(OnlineConfigEntry entry) {
        this.setOnlineConfigStatus("Loading " + entry.getName(), "please wait", null);
        ONLINE_CONFIG_EXECUTOR.execute(() -> {
            try {
                String json = this.onlineConfigClient.load(entry.getId());
                mc.addScheduledTask(() -> {
                    try {
                        int applied = new OnlineConfigApplier().apply(json);
                        ChatUtil.sendFormatted(
                                String.format("%sOnline config loaded (&a&o%s&r) &7- applied %d setting(s)&r",
                                        Myau.clientName, entry.getName(), applied));
                        this.refreshOnlineConfigs();
                    } catch (Exception e) {
                        ChatUtil.sendFormatted(
                                Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r");
                        this.refreshOnlineConfigs();
                    }
                });
            } catch (Exception e) {
                mc.addScheduledTask(() -> {
                    ChatUtil.sendFormatted(
                            Myau.clientName + "Failed to load online config: &c" + e.getMessage() + "&r");
                    this.refreshOnlineConfigs();
                });
            }
        });
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
        } catch (IOException e) {
            CLICK_GUI_LOGGER.log(Level.WARNING, "Failed to save ClickGui positions", e);
        }
    }

    private void loadPositions() {
        if (!configFile.exists())
            return;
        try (FileReader reader = new FileReader(configFile)) {
            com.google.gson.JsonElement element = new JsonParser().parse(reader);
            if (element == null || !element.isJsonObject()) {
                return;
            }
            JsonObject json = element.getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName()) && json.get(cat.getName()).isJsonObject()) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    if (pos.has("x") && pos.get("x").isJsonPrimitive()) {
                        cat.setX(pos.get("x").getAsInt());
                    }
                    if (pos.has("y") && pos.get("y").isJsonPrimitive()) {
                        cat.setY(pos.get("y").getAsInt());
                    }
                    if (pos.has("open") && pos.get("open").isJsonPrimitive()) {
                        cat.setOpened(pos.get("open").getAsBoolean());
                    }
                }
            }
        } catch (Exception e) {
            CLICK_GUI_LOGGER.log(Level.WARNING, "Failed to load ClickGui positions", e);
        }
    }
}
