// UCID: aia47
// Date: 2026-11-02
// Plan: Add /flip, /pm and /shuffle as commands. When the user types one, send it to the server like /reverse does.
package M4.MCCS.Part1HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import M4.MCCS.Part1HW.TextFX;
import M4.MCCS.Part1HW.TextFX.Color;
import M4.MCCS.Part1HW.Constants;

/**
 * Multi-client chat client using ObjectInputStream/ObjectOutputStream.
 * Terminal input stays on the main thread; one background thread receives
 * messages.
 */
public class Client {

    private volatile Socket server;
    private ObjectOutputStream out;
    private volatile boolean isRunning = true; // volatile so changes are visible across threads immediately
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    /**
     * Recognized client-side commands.
     */
    private enum Command {
        CONNECT("/connect"), DISCONNECT("/disconnect"), QUIT("/quit"),
        USERS("/users"), REVERSE("/reverse");

        private final String trigger;

        /** Store the slash command spelling without changing argument text. */
        Command(String trigger) {
            this.trigger = trigger;
        }

        /** Match a command token without requiring a particular letter case. */
        static Command fromText(String token) {
            for (Command command : values()) {
                if (command.trigger.equalsIgnoreCase(token)) {
                    return command;
                }
            }
            return null;
        }
    }

    public Client() {
        System.out.println("Client Created");
    }

    /** Check whether the current socket and output stream are available. */
    public synchronized boolean isConnected() {
        // Note: these check the client's end of the socket only; they don't detect
        // server-side failures
        return server != null && server.isConnected() && !server.isClosed()
                && !server.isInputShutdown() && !server.isOutputShutdown() && out != null;
    }

    /** Open streams in header-safe order and start a background receiver. */
    private synchronized boolean connect(String host, int port) {
        if (isConnected()) {
            System.out.println("Already connected; use /disconnect first");
            return false;
        }
        Socket candidate = new Socket();
        try {
            candidate.connect(new InetSocketAddress(host, port), Constants.SETUP_TIMEOUT_MS);
            candidate.setSoTimeout(Constants.SETUP_TIMEOUT_MS);
            // ObjectOutputStream must be created before ObjectInputStream on both sides to
            // avoid deadlock
            ObjectOutputStream output = new ObjectOutputStream(candidate.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(candidate.getInputStream());
            candidate.setSoTimeout(0);
            server = candidate;
            out = output;
            new Thread(() -> listenToServer(candidate, input)).start();
            System.out.println("Client connected");
            System.out.println("Connection opened");
            return true;
        } catch (IOException e) {
            System.out.println("Connection failed: " + e.getMessage());
            try {
                candidate.close();
            } catch (IOException closeError) {
                System.out.println("Could not close failed connection: " + closeError.getMessage());
            }
            return false;
        }
    }

    /** Check that a /connect command is shaped like localhost:port or ip:port. */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Handle a recognized slash command and leave ordinary chat text to the caller.
     *
     * @return true if the input was a recognized command, false if it is a plain
     *         message
     */
    private boolean processClientCommand(String text) throws IOException {
        String[] parts = text.strip().split("\\s+", 2);
        Command command = Command.fromText(parts[0]);
        if (command == null) {
            return false;
        }
        String arguments = parts.length == 2 ? parts[1] : "";
        switch (command) {
            case CONNECT:
                if (!isConnection(text.strip().replaceAll("\\s+", " "))) {
                    System.out.println("Invalid format. Use: /connect localhost:3000 or /connect 192.168.1.x:3000");
                    return true;
                }
                String[] endpoint = arguments.split(":", -1);
                if (endpoint.length != 2 || endpoint[0].isBlank()) {
                    System.out.println("Use /connect <host>:<port>");
                    return true;
                }
                try {
                    int port = Integer.parseInt(endpoint[1].trim());
                    if (port < 1 || port > 65535) {
                        System.out.println("Port must be between 1 and 65535");
                    } else {
                        connect(endpoint[0].trim(), port);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Port must be a whole number");
                }
                return true;
            case USERS:
                sendToServer(Constants.COMMAND_TRIGGER + ",users");
                return true;
            case REVERSE:
                sendReverse(arguments);
                return true;
            case DISCONNECT:
                sendDisconnect();
                return true;
            case QUIT:
                isRunning = false;
                closeServerConnection(server);
                System.out.println("Client terminated");
                return true;
            default:
                return false;
        }
    }

    /** Send ordinary chat text to the server. */
    private void sendMessage(String message) throws IOException {
        sendToServer(message);
    }

    /** Request server-side text reversal. */
    private void sendReverse(String message) throws IOException {
        sendToServer(Constants.COMMAND_TRIGGER + ",reverse," + message);
    }

    /**
     * Ask the server to close this connection while keeping keyboard input
     * available.
     */
    private void sendDisconnect() throws IOException {
        sendToServer(Constants.COMMAND_TRIGGER + ",disconnect");
    }

    /** Write through the current connection while preventing interleaved sends. */
    private synchronized void sendToServer(String payload) throws IOException {
        if (!isConnected()) {
            System.out.println("Not connected; use /connect <host>:<port> (hint: /connect localhost:3000)");
            return;
        }
        out.writeObject(payload);
        out.flush();
        out.reset();
    }

    /**
     * Runs in a background thread. Receive data for this socket until it closes
     * or reading fails.
     */
    private void listenToServer(Socket connection, ObjectInputStream input) {
        try (input) {
            while (isRunning && !connection.isClosed()) {
                Object received = input.readObject(); // blocking
                if (received instanceof String message) {
                    synchronized (this) {
                        if (server == connection && !connection.isClosed()) {
                            System.out.println(TextFX.colorize(message, Color.BLUE));
                        }
                    }
                } else {
                    System.out.println("Ignoring an invalid server payload");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!connection.isClosed()) {
                System.out.println("Connection ended: " + e.getMessage());
            }
        } finally {
            // A finished old receiver cannot clear a subsequently opened connection.
            closeServerConnection(connection);
            System.out.println("listenToServer thread stopped");
        }
    }

    /** Close this socket and clear shared fields only if it is still current. */
    private synchronized void closeServerConnection(Socket connection) {
        if (connection == null) {
            return;
        }
        if (server == connection && out != null) {
            try {
                System.out.println("Closing output stream");
                out.close();
            } catch (IOException e) {
                System.out.println("Could not close output stream: " + e.getMessage());
            }
        }
        try {
            System.out.println("Closing connection");
            connection.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            System.out.println("Could not close connection: " + e.getMessage());
        }
        if (server == connection) {
            server = null;
            out = null;
            System.out.println("Disconnected");
        }
    }

    /**
     * Read keyboard input until quit or end-of-input, then close the connection.
     */
    public void start() {
        System.out.println("Client starting");
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Waiting for input");
            while (isRunning && input.hasNextLine()) {
                try {
                    String text = input.nextLine();
                    if (!processClientCommand(text)) {
                        sendMessage(text);
                    }
                } catch (IOException e) {
                    System.out.println("Send failed: " + e.getMessage());
                    closeServerConnection(server);
                }
            }
        } finally {
            closeServerConnection(server);
            System.out.println("listenToInput thread stopped");
        }
    }

    /** Start the course's terminal client. */
    public static void main(String[] args) {
        new Client().start();
    }

}