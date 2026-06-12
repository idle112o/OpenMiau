package myau.module.modules.misc;

import myau.Myau;
import myau.module.Module;

import java.util.ArrayList;
import java.util.List;

public class Panic extends Module {
    public Panic() {
        super("Panic", false, false);
    }

    @Override
    public void onEnabled() {
        List<Module> modulesToDisable = new ArrayList<>();
        for (Module module : Myau.moduleManager.modules.values()) {
            if (module != this && module.isEnabled()) {
                modulesToDisable.add(module);
            }
        }
        for (Module module : modulesToDisable) {
            module.setEnabled(false);
        }
        this.setEnabled(false);
    }
}
