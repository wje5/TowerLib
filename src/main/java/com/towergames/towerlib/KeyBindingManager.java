package com.towergames.towerlib;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeyBindingManager {
    public static int SHIFT = 1, CTRL = 2, ALT = 4;
    private final TowerGame game;
    private List<KeyBinding> keyBindings = new ArrayList<>();

    public KeyBindingManager(TowerGame game) {
        this.game = game;
    }

    public void registerKeyBinding(KeyBinding keyBinding) {
        keyBindings.add(keyBinding);
    }

    public void onKeyBoardInput(int key, int scancode, int action, int mods) {
        List<KeyBinding> l = keyBindings.stream().filter(e -> e.getKey() == key).collect(Collectors.toList());
        for (KeyBinding e : l) {
            switch (action) {
                case GLFW.GLFW_RELEASE:
                    e.setPressed(false);
                    break;
                case GLFW.GLFW_PRESS:
                    if (e.mods == mods || (!e.fullMatch && isModCover(mods, e.mods))) {
                        e.setPressed(true);
                    }
                    break;
                case GLFW.GLFW_REPEAT:
            }
        }
    }

    private boolean isModCover(int a, int b) {
        if (a == b) {
            return true;
        }
        if (((b & GLFW.GLFW_MOD_SHIFT) > 0 && (a & GLFW.GLFW_MOD_SHIFT) == 0)
                || ((b & GLFW.GLFW_MOD_CONTROL) > 0 && (a & GLFW.GLFW_MOD_CONTROL) == 0)
                || ((b & GLFW.GLFW_MOD_ALT) > 0 && (a & GLFW.GLFW_MOD_ALT) == 0)) {
            return false;
        }
        return true;
    }

    public List<KeyBinding> getKeyBindings() {
        return keyBindings;
    }

    public static class KeyBinding {
        private String name;
        private int key, mods;
        private boolean fullMatch, pressed;

        public KeyBinding(String name, int key, int mods, boolean fullMatch) {
            this.name = name;
            this.key = key;
            this.mods = mods;
            this.fullMatch = fullMatch;
        }

        public String getName() {
            return name;
        }

        public int getKey() {
            return key;
        }

        public int getMods() {
            return mods;
        }

        public boolean isFullMatch() {
            return fullMatch;
        }

        public void setKey(int key, int mods) {
            this.key = key;
            this.mods = mods;
        }

        public boolean isPressed() {
            return pressed;
        }

        public void setPressed(boolean pressed) {
            if (this.pressed != pressed) {
                this.pressed = pressed;
                if (pressed) {
                    onPress();
                } else {
                    onRelease();
                }
            }
        }

        protected void onPress() {

        }

        protected void onRelease() {

        }
    }
}
