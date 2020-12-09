# nLogin-Addon
Automatically creates a strong password when logging into servers with nLogin.

## How it works?
When entering a server, the addon will send a handshake message (see <a href="https://github.com/nickuc/nLogin-Addon/blob/master/src/main/java/com/nickuc/login/addon/model/request/ReadyRequest.java">ReadyRequest</a>) to the server.
<br><br>
After that, if the server is using nLogin, the server will send a message (see <a href="https://github.com/nickuc/nLogin-Addon/blob/master/src/main/java/com/nickuc/login/addon/model/response/ReadyResponse.java">ReadyResponse</a>).

#### If the user is not registered:
The addon will create a secure password and save to credentials.json file.

#### If the user is registered:
The addon will check if the password is registered and use it. If the password is not registered and the synchronization feature is active, the addon will try to decrypt the content using the saved cryptKeys (see <a href="https://github.com/nickuc/nLogin-Addon/blob/master/src/main/java/com/nickuc/login/addon/listeners/ServerMessage.java#L111">ServerMessage</a>).

## Synchronization
Password synchronization is performed with the zero knowledge technique. This means that the accessed server will not be able to know your passwords, since the whole encryption and decryption process is done on the client side.<br>
See more details in <a href="https://github.com/nickuc/nLogin-Addon/blob/master/src/main/java/com/nickuc/login/addon/sync/Synchronization.java#L38">Synchronization</a> class.

## Compiling
#### Requirements:
>- JDK 1.8

#### How to compile:
>- Clone the project with Git/Github
>- Run the command "gradle build" (Windows) or "./gradlew build" (Linux)