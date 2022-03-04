/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.bootstrap.versions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nickuc.login.addon.Constants;
import com.nickuc.login.addon.bootstrap.LMAddon;
import com.nickuc.login.addon.bootstrap.LabyModBootstrap;
import com.nickuc.login.addon.handler.EventHandler;
import com.nickuc.login.addon.lang.Lang;
import com.nickuc.login.addon.model.AddonSettings;
import com.nickuc.login.addon.model.Credentials;
import com.nickuc.login.addon.model.request.Request;
import com.nickuc.login.addon.nLoginAddon;
import io.netty.buffer.Unpooled;
import net.labymod.addon.AddonConfig;
import net.labymod.api.EventManager;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.api.events.ServerMessageEvent;
import net.labymod.core.LabyModCore;
import net.labymod.gui.elements.DropDownMenu;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.*;
import net.labymod.utils.Consumer;
import net.labymod.utils.Material;
import net.labymod.utils.ServerData;
import net.labymod.utils.manager.ConfigManager;
import net.minecraft.network.PacketBuffer;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

public class LMAddon189 extends LabyModBootstrap {

    private static final Field CONFIG_MANAGER_FIELD;

    static {
        Field configManagerField;
        try {
            configManagerField = LabyModAddon.class.getDeclaredField("configManager");
            configManagerField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            configManagerField = null;
        }
        CONFIG_MANAGER_FIELD = configManagerField;
    }

    private final nLoginAddon addon = new nLoginAddon(this, this);

    public LMAddon189(LMAddon addonInstance) {
        super(addonInstance);
    }

    @Override
    public void onEnable() {
        addon.onEnable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadConfig() {
        try {
            ConfigManager<AddonConfig> configManager = (ConfigManager<AddonConfig>) CONFIG_MANAGER_FIELD.get(getAddonInstance());
            File addonSettingsFolder = new File(configManager.getFile().getParentFile(), "nLogin-Addon");
            addon.loadConfig(getAddonInstance().getConfig(), addonSettingsFolder);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fillSettings(List<SettingsElement> settingsElements) {
        final AddonSettings addonSettings = addon.getSettings();
        final Credentials credentials = addon.getCredentials();

        final BooleanElement enabledElement = new BooleanElement(Lang.Message.ENABLED_NAME.toText(), new ControlElement.IconData(Material.LEVER), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                addonSettings.setEnabled(result);
                addon.markModified(false);
            }
        }, addonSettings.isEnabled());

        final BooleanElement debugElement = new BooleanElement(Lang.Message.DEBUG_NAME.toText(), new ControlElement.IconData(Material.COMMAND), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                addonSettings.setDebug(result);
                addon.markModified(false);
            }
        }, addonSettings.isDebug());
        debugElement.setDescriptionText(Lang.Message.DEBUG_DESCRIPTION.toText());

        final BooleanElement securityWarningsElement = new BooleanElement(Lang.Message.SECURITY_WARNINGS_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                addonSettings.setSecurityWarnings(result);
                addon.markModified(false);
            }
        }, addonSettings.isSecurityWarnings());
        securityWarningsElement.setDescriptionText(Lang.Message.SECURITY_WARNINGS_DESCRIPTION.toText());

        final BooleanElement saveLoginElement = new BooleanElement(Lang.Message.SAVE_LOGIN_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                addonSettings.setSaveLogin(result);
                addon.markModified(false);
            }
        }, addonSettings.isSaveLogin());
        saveLoginElement.setDescriptionText(Lang.Message.SAVE_LOGIN_DESCRIPTION.toText());

        final BooleanElement storePasswordElement = new BooleanElement(Lang.Message.SYNC_PASSWORDS_NAME.toText(), new ControlElement.IconData(Material.REDSTONE_COMPARATOR), new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) {
                addonSettings.setSyncPasswords(result);
                addon.markModified(false);
            }
        }, addonSettings.isSyncPasswords());
        storePasswordElement.setDescriptionText(Lang.Message.SYNC_PASSWORDS_DESCRIPTION.toText());

        final StringElement masterPasswordElement = new StringElement(Lang.Message.MASTER_PASSWORD_NAME.toText(), new ControlElement.IconData(Material.PAPER), credentials.getMasterPassword(), new Consumer<String>() {
            @Override
            public void accept(String password) {
                credentials.setMasterPassword(password.trim());
                addon.markModified(true);
            }
        });
        masterPasswordElement.setDescriptionText(Lang.Message.MASTER_PASSWORD_DESCRIPTION.toText());

        Lang.Type[] langTypes0 = Lang.Type.values();
        String[] langTypes = new String[langTypes0.length];
        for (int i = 0; i < langTypes0.length; i++) {
            langTypes[i] = langTypes0[i].name();
        }

        final DropDownMenu<String> langSelectorDropDownMenu = new DropDownMenu<String>("Language", 0, 0, 0, 0).fill(langTypes);
        DropDownElement<String> langSelectorDropDown = new DropDownElement<String>("Language", langSelectorDropDownMenu);
        langSelectorDropDownMenu.setSelected(addonSettings.getLanguage());

        langSelectorDropDown.setChangeListener(new Consumer<String>() {
            @Override
            public void accept(String type) {
                addonSettings.setLanguage(type);
                addon.markModified(false);
            }
        });

        /*
        langSelectorDropDownMenu.setEntryDrawer(new DropDownMenu.DropDownEntryDrawer() {
            @Override
            public void draw(Object object, int x, int y, String trimmedEntry) {
                String entry = object.toString().toLowerCase();
                LabyMod.getInstance().getDrawUtils().drawString(LanguageManager.translate(entry), x, y);
            }
        });
         */

        settingsElements.add(langSelectorDropDown);
        settingsElements.add(enabledElement);
        settingsElements.add(debugElement);
        settingsElements.add(securityWarningsElement);
        settingsElements.add(saveLoginElement);
        settingsElements.add(storePasswordElement);
        settingsElements.add(masterPasswordElement);
    }

    @Override
    public String getPlayerName() {
        return LabyMod.getInstance().getPlayerName();
    }

    @Override
    public boolean isConnected() {
        return LabyMod.getInstance().isInGame();
    }

    @Override
    public void sendRequest(Request request) {
        synchronized (Constants.LOCK) {
            JsonObject json = new JsonObject();
            json.addProperty("id", request.getId());
            request.write(json);
            //getApi().sendJsonMessageToServer(Constants.NLOGIN_SUBCHANNEL, json);

            String messageKey = Constants.NLOGIN_SUBCHANNEL;
            String message = json.toString();

            PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
            packetBuffer.writeString(messageKey);
            packetBuffer.writeString(message);
            System.out.println(Constants.PREFIX + messageKey + ": " + message);
            LabyModCore.getMinecraft().sendPluginMessage("labymod3:main", packetBuffer);
        }
    }

    @Override
    public void sendMessage(String message) {
        synchronized (Constants.LOCK) {
            LabyMod.getInstance().displayMessageInChat(message);
        }
    }

    @Override
    public void sendChatPacket(String message) {
        synchronized (Constants.LOCK) {
            LabyModCore.getMinecraft().getPlayer().sendChatMessage(message);
        }
    }

    @Override
    public void sendNotification(String message) {
        synchronized (Constants.LOCK) {
            LabyMod.getInstance().notifyMessageRaw(Constants.DEFAULT_TITLE, message);
        }
    }

    @Override
    public void registerEvents() {
        final EventHandler eventHandler = addon.getEventHandler();
        EventManager eventManager = getAddonInstance().getApi().getEventManager();
        eventManager.registerOnJoin(new Consumer<ServerData>() {
            @Override
            public void accept(ServerData serverData) {
                eventHandler.handleJoin();
            }
        });
        eventManager.registerOnQuit(new Consumer<ServerData>() {
            @Override
            public void accept(ServerData serverData) {
                eventHandler.handleQuit();
            }
        });
        eventManager.register(new MessageSendEvent() {
            @Override
            public boolean onSend(String message) {
                return eventHandler.handleChat(message);
            }
        });
        eventManager.register(new ServerMessageEvent() {
            @Override
            public void onServerMessage(String subChannel, JsonElement jsonElement) {
                eventHandler.handleServerMessage(subChannel, jsonElement);
            }
        });
        eventManager.register(new MessageReceiveEvent() {
            @Override
            public boolean onReceive(String formatted, String unformatted) {
                eventHandler.handleReceivedMessage(unformatted);
                return false;
            }
        });
    }
}
