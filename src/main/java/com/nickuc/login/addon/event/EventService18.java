/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.event;

import com.google.gson.JsonElement;
import com.nickuc.login.addon.nLoginAddon;
import net.labymod.api.EventManager;
import net.labymod.api.LabyModAPI;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.api.events.ServerMessageEvent;
import net.labymod.utils.Consumer;
import net.labymod.utils.ServerData;

import java.lang.reflect.Method;

public class EventService18 implements LMEventService {

    private static final Method GET_EVENT_MANAGER;

    static {
        try {
            GET_EVENT_MANAGER = LabyModAPI.class.getMethod("getEventManager");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("incompatible version!", e);
        }
    }

    @Override
    public void registerEvents(nLoginAddon addon) throws Exception {
        EventHandler eventHandler = addon.getEventHandler();
        EventManager eventManager = (EventManager) GET_EVENT_MANAGER.invoke(addon.getApi());
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
