package com.brilliafy.magicstorage.client;

import com.brilliafy.magicstorage.network.NetworkHandler;
import com.brilliafy.magicstorage.network.OpenRemoteKeyMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    public static KeyBinding keyOpenRemote;

    public static void initKeyBindings() {
        keyOpenRemote = new KeyBinding("key.magicstorage.open_remote", Keyboard.KEY_LMENU, "key.category.magicstorage");
        ClientRegistry.registerKeyBinding(keyOpenRemote);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyOpenRemote != null) {
            int keyCode = keyOpenRemote.getKeyCode();
            boolean isPressed = keyOpenRemote.isPressed();
            if (!isPressed && keyCode != 0 && org.lwjgl.input.Keyboard.getEventKey() == keyCode && org.lwjgl.input.Keyboard.getEventKeyState()) {
                isPressed = true;
            }
            if (isPressed && Minecraft.getMinecraft().currentScreen == null) {
                NetworkHandler.INSTANCE.sendToServer(new OpenRemoteKeyMessage());
            }
        }
    }
}
