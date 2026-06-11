package myau.ui.components;

import myau.ui.Component;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineConfigComponent implements Component {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final CategoryComponent category;
    private final String title;
    private final String subtitle;
    private final Runnable clickAction;
    private int offsetY;

    public OnlineConfigComponent(CategoryComponent category, int offsetY, String title, String subtitle, Runnable clickAction) {
        this.category = category;
        this.offsetY = offsetY;
        this.title = title;
        this.subtitle = subtitle;
        this.clickAction = clickAction;
    }

    @Override
    public void draw(AtomicInteger offset) {
        int y = this.category.getY() + this.offsetY;
        boolean actionable = this.clickAction != null;
        int color = actionable ? Color.WHITE.getRGB() : new Color(150, 150, 150).getRGB();
        mc.fontRendererObj.drawStringWithShadow(this.title, this.category.getX() + 4, y + 3, color);
        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow(this.subtitle, this.category.getX() + 4, y + 13, new Color(125, 125, 125).getRGB());
        }
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (button == 0 && this.clickAction != null && this.isHovered(x, y)) {
            this.clickAction.run();
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 24;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    private boolean isHovered(int x, int y) {
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth()
                && y > this.category.getY() + this.offsetY && y < this.category.getY() + this.offsetY + this.getHeight();
    }
}
