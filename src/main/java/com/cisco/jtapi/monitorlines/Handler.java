package com.cisco.jtapi.monitorlines;

// Importing necessary libraries for telephony event handling and WebSocket communication
import javax.telephony.*;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.*;
import com.cisco.jtapi.extensions.*;
import com.cisco.cti.util.Condition;
import io.javalin.websocket.WsContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Handler class is responsible for observing and reacting to various
 * telephony events
 * such as provider status changes, terminal events, address changes, and call
 * control events.
 * It serves as a bridge between a Cisco telephony system and possibly a web
 * interface, allowing
 * real-time updates and control over telephony resources.
 */
public class Handler implements ProviderObserver, TerminalObserver, AddressObserver, CallControlCallObserver {
    // Cisco telephony components and synchronization primitives for monitoring the
    // state.
    private CiscoProvider provider;
    private CiscoAddress ctiRoutePointAddress;
    private CiscoTerminal ctiRoutePointTerminal;
    private final Map<String, Set<String>> activeConnections = new HashMap<>();

    // Conditions to synchronize and check the readiness of various telephony
    // components.
    public Condition providerInService = new Condition();
    public Condition ctiRoutePointAddressInService = new Condition(); // Tracks readiness of a specific route point
                                                                      // address
    public Condition ctiRoutePointTerminalInService = new Condition(); // Tracks readiness of a specific terminal
    public Condition fromTerminalInService = new Condition();
    public Condition fromAddressInService = new Condition();
    public Condition callActive = new Condition();

    // Context for WebSocket communication, possibly used to send telephony event
    // updates to a web client.
    public WsContext ctx;

    /**
     * Constructs a Handler for the specified CiscoProvider.
     * 
     * @param provider CiscoProvider instance to observe and manage.
     */
    public Handler(CiscoProvider provider) {
        this.provider = provider;
    }

    /**
     * Handles events related to the telephony provider, such as when the provider
     * becomes operational.
     * This method initializes observers for terminals and addresses once the
     * provider is ready.
     */
    @Override
    public void providerChangedEvent(ProvEv[] events) {
        for (ProvEv ev : events) {
            System.out.println("Received--> Provider/" + ev);
            if (ev.getID() == ProvInServiceEv.ID) {
                providerInService.set();

                // Ensuring all terminals and addresses under this provider are monitored upon
                // service availability.
                Terminal[] terminals = null;
                try {
                    terminals = provider.getTerminals();
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                }

                for (Terminal terminal : terminals) {
                    try {
                        System.out.println("Adding Observer to : " + terminal);
                        if (terminal instanceof CiscoRouteTerminal) {
                            CiscoRouteTerminal routeTerminal = (CiscoRouteTerminal) terminal;
                            routeTerminal.register(null, CiscoRouteTerminal.NO_MEDIA_REGISTRATION);
                            routeTerminal.addCallObserver(this);
                        } else {
                            terminal.addCallObserver(this);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Address[] addresses = null;
                try {
                    addresses = provider.getAddresses();
                } catch (ResourceUnavailableException e) {
                    e.printStackTrace();
                }
                for (Address address : addresses) {
                    try {
                        System.out.println("Adding Observer to : " + address);
                        address.addCallObserver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Reacts to terminal-related events, especially to determine when terminals are
     * operational.
     * Useful for initializing call monitoring on specific terminals.
     */
    @Override
    public void terminalChangedEvent(TermEv[] events) {
        for (TermEv ev : events) {
            if (ev instanceof CiscoTermInServiceEv) {
                if (ev.getTerminal().equals(ctiRoutePointTerminal)) {
                    ctiRoutePointTerminalInService.set();
                }
                fromTerminalInService.set();
            }
        }
    }

    /**
     * Monitors address status changes, ensuring that addresses are ready to handle
     * calls
     * and other related functionalities.
     */
    @Override
    public void addressChangedEvent(AddrEv[] events) {
        for (AddrEv ev : events) {
            if (ev instanceof CiscoAddrInServiceEv) {
                if (ev.getAddress().equals(ctiRoutePointAddress)) {
                    ctiRoutePointAddressInService.set();
                }
                fromAddressInService.set();
            }
        }
    }

    /**
     * Handles call-related events to manage ongoing calls, connecting, and
     * disconnecting calls,
     * based on the current state of connections and addresses involved in the call.
     */
    @Override
    public void callChangedEvent(CallEv[] events) {
        for (CallEv event : events) {
            System.out.println("Event Received: " + event);
            if (event instanceof CallCtlConnOfferedEv) {
                CallCtlConnOfferedEv offeredEvent = (CallCtlConnOfferedEv) event;
                Connection conn = offeredEvent.getConnection();
                Address address = conn.getAddress();
                String addressName = address.getName();
                System.out.println("Offered on address: " + addressName);

                // Check if the offered event is on the CTI Route Point DN 885016
                if (addressName.equals("885016")) {
                    System.out.println("Initiating conference call logic for DN 885016");

                    try {
                        initiateConference(conn.getCall());
                    } catch (InvalidArgumentException | PrivilegeViolationException | ResourceUnavailableException
                            | MethodNotSupportedException | InvalidStateException | InvalidPartyException
                            | InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void initiateConference(Call originalCall) throws InvalidArgumentException, PrivilegeViolationException,
            ResourceUnavailableException, MethodNotSupportedException, InvalidStateException, InvalidPartyException,
            InterruptedException {

        System.out.println("\n-------Initiating Conference call ------------------\n");

        // Retrieve terminals and addresses
        CiscoTerminal terminal5016 = (CiscoTerminal) provider.getTerminal("CSFAMCKENZIE");
        CiscoAddress address5016 = (CiscoAddress) provider.getAddress("5016");
        CiscoTerminal terminal5017 = (CiscoTerminal) provider.getTerminal("CSFAPEREZ");
        CiscoAddress address5017 = (CiscoAddress) provider.getAddress("5017");
        CiscoTerminal terminal885016 = (CiscoTerminal) provider.getTerminal("CTIRoutePoint88");
        CiscoAddress address885016 = (CiscoAddress) provider.getAddress("885016");

        System.out.println("Initial state of DN 5017: " + address5017.getState());

        // Disconnect original call from CTI route point
        Connection[] connections = originalCall.getConnections();
        if (connections != null) {
            for (Connection conn : connections) {
                if (conn.getAddress().getName().equals("885016")) {
                    conn.disconnect();
                    System.out.println("Disconnected the original call from the CTI route point.");
                }
            }
        }

        // Create and set up a new call for the conference
        CallControlCall newCall = (CallControlCall) provider.createCall();
        newCall.setConferenceEnable(true);
        System.out.println("Call Created Successfully");

        // Connect DN 5016 to DN 4030
        newCall.connect(terminal5016, address5016, "4030");
        System.out.println("Attempting to add DN 4030 to the call...");

        // Wait up to 10 seconds for DN 4030 to join the call
        boolean is4030Connected = false;
        for (int i = 0; i < 10 && !is4030Connected; i++) {
            Thread.sleep(1000); // Wait for 1 second
            Connection[] newConnections = newCall.getConnections();
            for (Connection conn : newConnections) {
                if (conn.getAddress().getName().equals("4030") && conn.getState() == Connection.CONNECTED) {
                    System.out.println("DN 4030 has successfully joined the call.");
                    is4030Connected = true;
                    break;
                }
            }
        }

        if (!is4030Connected) {
            System.out.println("DN 4030 has not joined the call within the expected time. Aborting merge operation.");
            return; // Exit if DN 4030 is not connected
        }

        // Proceed with merging DN 5017 if it has an existing call
        Connection[] connections5017 = address5017.getConnections();
        Call existingCall5017 = null;
        if (connections5017 != null && connections5017.length > 0) {
            existingCall5017 = connections5017[0].getCall();
        }

        if (existingCall5017 != null) {
            try {
                System.out.println("Preparing to merge DN 5017's call with DN 4030's call");
                newCall.conference(existingCall5017);
                System.out.println("Successfully merged DN 5017's call with DN 4030's call.");
            } catch (Exception e) {
                System.err.println("Failed to merge calls: " + e.getMessage());
                e.printStackTrace();
                // Attempt to connect DN 5017 directly if merge failed
                connectDN5017Directly(newCall, terminal5017, address5017);
            }
        } else {
            System.out.println("No existing call found for DN 5017 to merge.");
            connectDN5017Directly(newCall, terminal5017, address5017);
        }
    }

    private void connectDN5017Directly(CallControlCall newCall, Terminal terminal5017, Address address5017)
            throws MethodNotSupportedException, ResourceUnavailableException, InvalidStateException,
            InvalidArgumentException {
        try {
            newCall.connect(terminal5017, address5017, "4030");
            System.out.println("DN 5017 directly added to the new call with DN 4030.");
        } catch (Exception e) {
            System.err.println("Failed to connect DN 5017 to the new call: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the WebSocket context to enable real-time communication with web
     * clients,
     * potentially sending updates about telephony events and changes.
     */
    public void setContext(WsContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Retrieves the current WebSocket context, allowing interactions with web
     * clients
     * to manage telephony resources.
     */
    public WsContext getContext() {
        return this.ctx;
    }
}
