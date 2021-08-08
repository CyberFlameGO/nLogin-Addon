/*
 * This file is part of a NickUC project
 *
 * Copyright (c) NickUC <nickuc.com>
 * https://github.com/nickuc
 */

package com.nickuc.login.addon.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nickuc.login.addon.utils.SafeGenerator;
import com.nickuc.login.addon.utils.crypt.RSA;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.labymod.main.LabyMod;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Nullable;
import java.security.PublicKey;
import java.util.*;

@AllArgsConstructor
public class Credentials {

    @Getter
    private final String uuid;
    @Getter
    @Setter
    private String masterPassword;
    private final List<User> users;

    public synchronized User getUser() {
        return getUser(LabyMod.getInstance().getPlayerName());
    }

    public synchronized User getUser(String username) {
        for (User user : users) {
            if (user.username.equals(username)) return user;
        }
        User user = new User(username, new ArrayList<String>(), new HashMap<String, Server>());
        users.add(user);
        return user;
    }

    public synchronized JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid);
        json.addProperty("masterPassword", masterPassword);

        JsonArray usersJson = new JsonArray();
        for (User user : users) {
            usersJson.add(user.toJson());
        }

        json.add("users", usersJson);
        return json;
    }

    public static Credentials fromJson(JsonObject json) {
        String uuid;
        if (json.has("uuid")) {
            uuid = json.get("uuid").getAsString().trim();
        } else {
            uuid = UUID.randomUUID().toString();
        }

        String masterPassword;
        if (json.has("masterPassword")) {
            masterPassword = json.get("masterPassword").getAsString().trim();
        } else {
            masterPassword = "";
        }

        List<User> users = new ArrayList<User>();
        if (json.has("users")) {
            JsonArray usersJson = json.getAsJsonArray("users");
            for (int i = 0; i < usersJson.size(); i++) {
                JsonObject userJson = usersJson.get(i).getAsJsonObject();
                User user = User.fromJson(userJson);
                if (user != null) {
                    users.add(user);
                }
            }
        }
        return new Credentials(uuid, masterPassword, users);
    }

    public static class User {

        @Getter
        private final String username;
        private final List<String> cryptKeys;
        private final Map<String, Server> servers;

        public User(String username, List<String> cryptKeys, Map<String, Server> servers) {
            this.username = username;
            this.servers = servers;
            this.cryptKeys = cryptKeys;
            if (cryptKeys.isEmpty()) {
                cryptKeys.add(SafeGenerator.generateUserCryptKey());
            }
        }

        public synchronized List<String> getCryptKeys() {
            return cryptKeys;
        }

        public synchronized String getPrimaryCryptKey() {
            if (cryptKeys.isEmpty()) {
                cryptKeys.add(SafeGenerator.generateUserCryptKey());
            }
            return cryptKeys.get(0);
        }

        public synchronized void addCryptKey(String key) {
            if (!cryptKeys.contains(key)) cryptKeys.add(key);
        }

        public synchronized Server updateServer(String uuid, @Nullable PublicKey publicKey, String password) {
            Server server = getServer(uuid);
            if (server != null) {
                server.password = password;
                if (server.publicKey == null) {
                    server.publicKey = publicKey;
                }
            } else {
                servers.put(uuid, new Server(uuid, publicKey, password));
            }
            return server;
        }

        public synchronized void updateServer(Server server) {
            servers.put(server.uuid, server);
        }

        @Nullable
        public synchronized Server getServer(String uuid) {
            return servers.get(uuid);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);

            JsonArray cryptKeysJson = new JsonArray();
            for (String cryptKey : cryptKeys) {
                cryptKeysJson.add(new JsonPrimitive(cryptKey));
            }

            json.add("cryptKeys", cryptKeysJson);

            JsonArray serversJson = new JsonArray();
            for (Map.Entry<String, Server> entry : servers.entrySet()) {
                Server server = entry.getValue();
                serversJson.add(server.toJson());
            }

            json.add("servers", serversJson);
            return json;
        }

        @Nullable
        public static User fromJson(JsonObject json) {
            if (json.has("username") && json.has("cryptKeys") && json.has("servers")) {
                List<String> cryptKeys = new ArrayList<String>();
                JsonArray cryptKeysJson = json.getAsJsonArray("cryptKeys");
                for (int i = 0; i < cryptKeysJson.size(); i++) {
                    cryptKeys.add(cryptKeysJson.get(i).getAsJsonPrimitive().getAsString());
                }

                JsonArray serversJson = json.getAsJsonArray("servers");
                Map<String, Server> servers = new HashMap<String, Server>();
                User user = new User(json.get("username").getAsString(), cryptKeys, servers);
                for (int i = 0; i < serversJson.size(); i++) {
                    JsonObject serverJson = serversJson.get(i).getAsJsonObject();
                    Server server = Server.fromJson(serverJson);
                    if (server != null) {
                        servers.put(server.uuid, server);
                    }
                }
                return user;
            }
            return null;
        }

    }

    @AllArgsConstructor
    @Getter
    public static class Server {

        private final String uuid;
        @Nullable
        private PublicKey publicKey;
        private String password;

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid);
            if (publicKey != null) {
                json.addProperty("publicKey", Base64.encodeBase64String(publicKey.getEncoded()));
            }
            json.addProperty("password", password);
            return json;
        }

        @Nullable
        public static Server fromJson(JsonObject json) {
            if (json.has("uuid") && json.has("password")) {
                String uuid = json.get("uuid").getAsString();
                String password = json.get("password").getAsString();
                PublicKey publicKey = json.has("publicKey") ? RSA.getPublicKeyFromBytes(Base64.decodeBase64(json.get("publicKey").getAsString())) : null;
                return new Server(uuid, publicKey, password);
            }
            return null;
        }

    }

}
