/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.event;

import com.nickuc.login.addon.nLoginAddon;
import lombok.RequiredArgsConstructor;
import net.labymod.api.LabyModAPI;
import net.labymod.api.event.EventService;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.events.client.chat.MessageReceiveEvent;
import net.labymod.api.event.events.client.chat.MessageSendEvent;
import net.labymod.api.event.events.network.ServerMessageEvent;
import net.labymod.api.event.events.network.server.ServerEvent;
import net.labymod.core.ChatComponent;
import net.labymod.core.LabyModCore;

import java.lang.reflect.Method;

public class EventService116 implements LMEventService {

    private static final Method GET_EVENT_MANAGER;

    static {
        try {
            GET_EVENT_MANAGER = LabyModAPI.class.getMethod("getEventService");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("incompatible version!", e);
        }
    }
    @Override
    public void registerEvents(nLoginAddon addon) throws Exception {
        EventHandler eventHandler = addon.getEventHandler();
        EventService eventService = (EventService) GET_EVENT_MANAGER.invoke(addon.getApi());
        eventService.registerListener(new EventListener(eventHandler));
    }

    @RequiredArgsConstructor
    private static class EventListener {

        private static final Method GET_COMPONENT;

        static {
            try {
                GET_COMPONENT = MessageReceiveEvent.class.getMethod("getComponent");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("incompatible version!", e);
            }
        }

        private final EventHandler eventHandler;

        @Subscribe
        public void onServerEvent(ServerEvent event) {
            eventHandler.handleJoin();
        }

        @Subscribe
        public void onMessageReceive(MessageReceiveEvent event) {
            try {
                Object component = GET_COMPONENT.invoke(event);
                ChatComponent chatComponent = LabyModCore.getCoreAdapter().getMinecraftImplementation().getChatComponent(component);
                eventHandler.handleReceivedMessage(chatComponent.getUnformattedText());
            } catch (ReflectiveOperationException e) {
                eventHandler.handleReceivedMessage("");
                e.printStackTrace();
            }
        }

        @Subscribe
        public void onMessageSend(MessageSendEvent event) {
            eventHandler.handleChat(event.getMessage());
        }

        @Subscribe
        public void onServerMessage(ServerMessageEvent event) {
            eventHandler.handleServerMessage(event.getMessageKey(), event.getServerMessage());
        }

    }

}
