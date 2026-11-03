// UCID: aia47
// Date: 2026-11-02
// Plan: When a flip, pm or shuffle command comes in, just pass it to the Server.
package M4.MCCS.Part1HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Accepts connections and selects the recipients of each message. */
public class Server {

    private int port = 3000;
    // thread-safe map; multiple ServerThreads may call Server methods concurrently
    private final ConcurrentHashMap<Long, ServerThread> connectedClients = new ConcurrentHashMap<>();
    private final List<ServerThread> disconnectedBuffer = new ArrayList<>();
    private volatile boolean isRunning = true;
    private volatile ServerSocket listener;

    /** Register resource cleanup for JVM shutdown. */
    public Server() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /** Accept connections and give each socket its own handler thread. */
    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            listener = serverSocket;
            System.out.println("Server listening on " + port);
            while (isRunning) {
                System.out.println("Waiting for next client");
                Socket client = serverSocket.accept(); // blocks until a client connects
                System.out.println("Client connected");
                // third arg is a callback; ServerThread calls it once streams are open and it
                // is ready
                new ServerThread(client, this, this::onServerThreadInitialized).start();
                // not added to connectedClients here; that happens inside the callback after
                // setup
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Server error: " + e.getMessage());
            }
        } finally {
            shutdown();
            System.out.println("Server socket closed");
        }
    }

    /**
     * Stop acceptance and close current connections without holding the registry
     * lock.
     */
    private void shutdown() {
        isRunning = false;
        try {
            if (listener != null) {
                listener.close();
            }
        } catch (IOException e) {
            System.out.println("Could not close listener: " + e.getMessage());
        }
        // Socket close does not wait for a handler's callback or hold the registry
        // lock.
        for (ServerThread client : connectedClients.values()) {
            client.disconnect();
        }
    }

    /**
     * Callback from ServerThread once streams are open and it is ready to
     * send/receive.
     * Registers the client (only after its object streams are ready) and announces
     * their arrival.
     */
    private synchronized void onServerThreadInitialized(ServerThread client) {
        if (!client.isRunning()) {
            return;
        }
        connectedClients.put(client.getClientId(), client);
        broadcast(null, client.getDisplayName() + " connected");
    }

    /** Close this connection and queue one leave event on its first removal. */
    private void removeClient(ServerThread client) {
        client.disconnect();
        if (connectedClients.remove(client.getClientId(), client)) {
            disconnectedBuffer.add(client);
        }
    }

    // handle* methods are the interface ServerThread uses to trigger Server actions

    /**
     * Called when a client requests to disconnect. Removes the client and sends
     * queued leave notices.
     */
    protected synchronized void handleDisconnect(ServerThread client) {
        removeClient(client);
        processDisconnectedBuffer();
    }

    /**
     * Unicast the current ready-client list to the requester only.
     * The requester's entry is tagged with "(you)".
     */
    protected synchronized void handleGetUserList(ServerThread requester) {
        StringBuilder list = new StringBuilder("Connected users:\n");
        for (ServerThread client : connectedClients.values()) {
            list.append(client.getDisplayName());
            if (client == requester) {
                list.append(" (you)");
            }
            list.append("\n");
        }
        if (!requester.sendToClient(list.toString().stripTrailing())) {
            removeClient(requester);
            processDisconnectedBuffer();
        }
    }

    /** Broadcast a received chat message, including to its sender. */
    protected synchronized void handleMessage(ServerThread sender, String text) {
        broadcast(sender, text);
    }

    /** Reverse the requested text before broadcasting it. */
    protected synchronized void handleReverseText(ServerThread sender, String text) {
        broadcast(sender, new StringBuilder(text).reverse().toString());
    }

    /**
     * Label a message and send it to every ready handler. Any client whose send
     * fails is removed.
     */
    private void broadcast(ServerThread sender, String message) {
        String label = sender == null ? "Server" : sender.getDisplayName();
        String formatted = label + ": " + message;
        sendOrDisconnect(recipient -> recipient.sendToClient(formatted));
    }

    /** Try each current recipient and queue failed connections for removal. */
    private void sendPass(Function<ServerThread, Boolean> sendAction) {
        for (ServerThread client : List.copyOf(connectedClients.values())) {
            if (!sendAction.apply(client)) {
                removeClient(client);
            }
        }
    }

    /** Complete a send pass before processing its disconnect notices. */
    private void sendOrDisconnect(Function<ServerThread, Boolean> sendAction) {
        sendPass(sendAction);
        processDisconnectedBuffer();
    }

    /** Drain leave notices without recursively broadcasting further failures. */
    private void processDisconnectedBuffer() {
        while (!disconnectedBuffer.isEmpty()) {
            ServerThread departed = disconnectedBuffer.remove(0);
            String notice = "Server: " + departed.getDisplayName() + " disconnected";
            System.out.println("Removed " + departed.getDisplayName());
            sendPass(recipient -> recipient.sendToClient(notice));
        }
    }

    /** Validate the optional port argument and start the server. */
    public static void main(String[] args) {
        System.out.println("Server Starting");
        int port = 3000; // default port
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            if (port < 1 || port > 65535) {
                System.out.println("Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Port must be a whole number");
            return;
        }
        new Server().start(port);
        System.out.println("Server Stopped");
    }

}