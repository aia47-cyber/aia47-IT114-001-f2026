// UCID: aia47
// Date: 2026-11-02
/*
flip: pick heads or tails and display result
pm: find target client and send message only to them
shuffle: mix message and display 
*/
package M4.MCCS.Part1HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

import M4.MCCS.Part1HW.TextFX;
import M4.MCCS.Part1HW.TextFX.Color;
import M4.MCCS.Part1HW.Constants;

/**
 * Server-side handler for one connected client.
 * Owns one socket, its object streams, read loop, and resource cleanup.
 * Each client gets its own thread so multiple clients can be served
 * concurrently.
 */
public class ServerThread extends Thread {
    private final Socket client;
    private final long clientId = threadId(); // set before any logging so the id appears correctly
    private final Server server;
    private final Consumer<ServerThread> onInitializationComplete;
    private volatile ObjectOutputStream out; // field so sendToClient() can reach it after run() opens it
    private volatile boolean isRunning; // volatile so changes are visible across threads immediately

    /** Keep the accepted socket and the server callback for this one client. */
    protected ServerThread(Socket client, Server server,
            Consumer<ServerThread> onInitializationComplete) {
        this.client = Objects.requireNonNull(client, "Client socket is required");
        this.server = Objects.requireNonNull(server, "Server is required");
        this.onInitializationComplete = Objects.requireNonNull(onInitializationComplete, "Callback is required");
        info("ServerThread created");
    }

    /** Report whether this connection has completed setup and remains active. */
    public boolean isRunning() {
        return isRunning;
    }

    /** Return this handler's stable, process-local thread identifier. */
    public long getClientId() {
        return clientId;
    }

    /**
     * Build the temporary display label used before named clients are introduced.
     */
    public String getDisplayName() {
        return "User[" + clientId + "]";
    }

    /** Write a diagnostic labeled with this client's identity. */
    private void info(String message) {
        System.out.println(getDisplayName() + ": " + message);
    }

    /** Notify the server that the streams are ready for communication. */
    private void onInitialized() {
        onInitializationComplete.accept(this);
    }

    /** Notify the server after closing this client connection. */
    private void onDisconnected() {
        server.handleDisconnect(this);
    }

    /**
     * Routes incoming text to a command handler or broadcasts it as a chat message.
     */
    private void processPayload(String incoming) {
        if (!processCommand(incoming)) {
            server.handleMessage(this, incoming);
        }
    }

    /**
     * Interpret wire commands.
     * Commands start with COMMAND_TRIGGER and are comma-separated:
     * trigger,name,data
     * Example: [cmd],reverse,hello world
     *
     * @return true if a command was handled, false if plain text
     */
    private boolean processCommand(String message) {
        if (!message.startsWith(Constants.COMMAND_TRIGGER + ",")) {
            return false;
        }
        String[] parts = message.split(",", 3);
        if (parts.length < 2) {
            return false;
        }
        final String command = parts[1].trim();
        System.out.println(TextFX.colorize("Checking command: " + command, Color.YELLOW));
        switch (command) {
            case "disconnect":
                server.handleDisconnect(this);
                return true;
            case "users":
                server.handleGetUserList(this);
                return true;
            case "reverse":
                server.handleReverseText(this, parts.length > 2 ? parts[2] : "");
                return true;
            case "pm":
                server.handlePrivateMessage(this, parts.length > 2 ? parts[2] : "");
                return true;
            case "flip":
                server.handleFlip(this);
                return true;
            case "shuffle":
                server.handleShuffle(this, parts.length > 2 ? parts[2] : "");
                return true;
            default:
                return false;
        }
    }

    /**
     * Write a message; success reports a completed write, not a delivery receipt.
     *
     * @return true on success, false if the send failed (client likely
     *         disconnected)
     */
    protected synchronized boolean sendToClient(String payload) {
        if (!isRunning || out == null || client.isClosed()) {
            return false;
        }
        try {
            out.writeObject(payload);
            out.flush();
            out.reset(); // Release previously written objects from the stream's cache.
            return true;
        } catch (IOException e) {
            info("Send failed (most likely disconnected): " + e.getMessage());
            return false;
        }
    }

    /**
     * Signals this thread to stop. Sets isRunning to false, interrupts the thread,
     * and
     * closes the classic socket to release blocked platform-thread I/O (interrupt
     * alone
     * does not unblock a socket read), so the run() loop exits and the finally
     * block
     * handles cleanup. Call from outside this thread (e.g. from Server).
     */
    protected void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        if (Thread.currentThread() != this) {
            this.interrupt(); // unblocks readObject() if the thread is waiting on it
        }
        try {
            client.close();
        } catch (IOException e) {
            info("Could not close the socket: " + e.getMessage());
        }
    }

    /**
     * Initialize both object streams, receive this client's data, and always clean
     * up.
     */
    @Override
    public void run() {
        info("Thread starting");
        // Both endpoints send and flush their stream header before waiting for input.
        // ObjectOutputStream must be opened before ObjectInputStream on both sides.
        // Reversing the order causes a deadlock: both sides block waiting for the
        // other's stream header.
        try (Socket connection = client) {
            connection.setSoTimeout(Constants.SETUP_TIMEOUT_MS);
            try (ObjectOutputStream output = new ObjectOutputStream(connection.getOutputStream())) {
                out = output;
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(connection.getInputStream())) {
                    connection.setSoTimeout(0);
                    isRunning = true;
                    onInitialized(); // notify Server this thread is ready
                    while (isRunning) {
                        try {
                            Object received = input.readObject(); // blocks until client sends something
                            if (received instanceof String message) {
                                info(TextFX.colorize("Received from client: " + message, Color.CYAN));
                                processPayload(message);
                            } else {
                                info("Ignoring an invalid payload");
                            }
                        } catch (ClassNotFoundException e) {
                            info("Unknown message class: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                info("Thread interrupted during read, disconnect() was called");
            } else if (isRunning) {
                info("Connection ended: " + e.getMessage());
            } else {
                info("Connection closed during setup or cleanup");
            }
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
            // Notify the server after this handler closes its connection.
            onDisconnected();
        }
    }

    /** Close the socket and release this connection's resources. */
    private void cleanup() {
        info("ServerThread cleanup() start");
        try {
            client.close();
            info("Closed server-side socket");
        } catch (IOException e) {
            info("Client already closed");
        }
        info("ServerThread cleanup() end");
    }
}
