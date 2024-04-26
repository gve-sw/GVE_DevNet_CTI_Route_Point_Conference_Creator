package com.cisco.jtapi.monitorlines;

// Importing necessary classes for handling date and time
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

// Importing Java Telephony API and Cisco-specific extensions
import javax.telephony.*;
import com.cisco.jtapi.extensions.*;

// Importing libraries for environment variable management and web server functionality
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

/**
 * The monitorLines class sets up a server to monitor specific telephone lines
 * and communicates line status updates to connected clients via WebSockets.
 */
public class monitorLines {
    // Array of line Directory Numbers (DNs) to monitor
    public static String[] lineDNs = { "5016", "5017" };
    // Formatter for timestamping log messages
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS");

    /**
     * Helper method to log messages with a timestamp.
     * @param msg the message to log
     */
    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    // WebSocket context for sending messages to a connected web client
    public static WsContext theCTX;

    /**
     * Sets the WebSocket context when a client connects.
     * @param ctx the WebSocket context
     */
    public static void setContext(WsContext ctx) {
        theCTX = ctx;
    }

    /**
     * Returns the current WebSocket context.
     * @return the current WebSocket context
     */
    public static WsContext getContext() {
        return theCTX;
    }

    /**
     * Main method to setup the telephony monitoring and start the web server.
     * Handles initialization and configuration of the telephony environment,
     * sets up WebSocket endpoints, and maintains live status updates.
     */
    public static void main(String[] args) throws JtapiPeerUnavailableException, ResourceUnavailableException,
            MethodNotSupportedException, InvalidArgumentException,
            PrivilegeViolationException, InvalidPartyException, InvalidStateException, InterruptedException {

        // Create and start a Javalin server on port 7000
        Javalin app = Javalin.create().start(7000);

        // Load environment variables from a .env file
        Dotenv dotenv = Dotenv.load();

        // Logging the initialization of the JTAPI (Java Telephony API) peer
        log("Initializing Jtapi");
        CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory.getJtapiPeer(null);

        // Construct the provider string using environment variables
        String providerString = String.format("%s;login=%s;passwd=%s", dotenv.get("CUCM_ADDRESS"),
                dotenv.get("JTAPI_USERNAME"), dotenv.get("JTAPI_PASSWORD"));

        // Log the attempt to connect to the provider
        log("Connecting Provider: " + providerString);
        CiscoProvider provider = (CiscoProvider) peer.getProvider(providerString);

        // Await the provider to be fully in service
        log("Awaiting ProvInServiceEv...");
        
        // Initialize the handler with the provider to manage telephony events
        Handler handler = new Handler(provider);
        provider.addObserver(handler);
        handler.providerInService.waitTrue();

        // Set up WebSocket endpoint and define event handlers for connect and close
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                log("Websocket Connected with host: " + ctx.host());

                // Send a message to the client listing all monitored lines
                String result = "";
                for (int i = 0; i < lineDNs.length; i++) {
                    result += "#" + lineDNs[i];
                }
                ctx.send("Shared Lines: " + result);
                
                // Set the WebSocket context in both the handler and the monitorLines class
                handler.setContext(ctx);
                setContext(ctx);
            });
            ws.onClose(ctx -> {
                log("Websocket Disconnected!");
            });
        });

        // Infinite loop to keep the server running and send keep-alive messages
        while (true) {
            try {
                Thread.sleep(5000);
                log("Trying to send keep alive...");
                if (handler.getContext() != null) {
                    handler.getContext().send("KeepAlive");
                    log("Keep alive sent!...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
