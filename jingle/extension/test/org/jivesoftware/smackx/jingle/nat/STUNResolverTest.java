package org.jivesoftware.smackx.jingle.nat;

import de.javawi.jstun.test.demo.ice.Candidate;
import de.javawi.jstun.test.demo.ice.ICENegociator;
import de.javawi.jstun.util.UtilityException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.*;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.PayloadType;

import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Test the STUN IP resolver.
 *
 * @author alvaro
 */
public class STUNResolverTest extends SmackTestCase {

    // Counter management

    public STUNResolverTest(final String arg) {
        super(arg);
    }

    private int counter;

    private final Object mutex = new Object();

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    private void incCounter() {
        synchronized (mutex) {
            counter++;
        }
    }

    private int valCounter() {
        int val;
        synchronized (mutex) {
            val = counter;
        }
        return val;
    }

    /**
     * Test for getPreferredCandidate()
     *
     * @throws Exception
     */
    public void testGetPreferredCandidate() throws Exception {
        int highestPref = 100;

        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 3, 2,
                "password", 3468, "username1", 1, "");
        TransportCandidate cand2 = new ICECandidate("192.168.5.1", 2, 10,
                "password", 3469, "username2", 15, "");
        TransportCandidate candH = new ICECandidate("192.168.2.11", 1, 2,
                "password", 3468, "usernameH", highestPref, "");
        TransportCandidate cand3 = new ICECandidate("192.168.2.10", 2, 10,
                "password", 3469, "username3", 2, "");
        TransportCandidate cand4 = new ICECandidate("192.168.4.1", 3, 2,
                "password", 3468, "username4", 78, "");

        STUNResolver stunResolver = new STUNResolver() {
        };
        stunResolver.addCandidate(cand1);
        stunResolver.addCandidate(cand2);
        stunResolver.addCandidate(candH);
        stunResolver.addCandidate(cand3);
        stunResolver.addCandidate(cand4);

        assertEquals(stunResolver.getPreferredCandidate(), candH);
    }

    /**
     * Test for getPreferredCandidate()
     *
     * @throws Exception
     */
    public void testGetPreferredCandidateICE() throws Exception {
        int highestPref = 100;

        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 3, 2,
                "password", 3468, "username1", 1, "");
        TransportCandidate cand2 = new ICECandidate("192.168.5.1", 2, 10,
                "password", 3469, "username2", 15, "");
        TransportCandidate candH = new ICECandidate("192.168.2.11", 1, 2,
                "password", 3468, "usernameH", highestPref, "");
        TransportCandidate cand3 = new ICECandidate("192.168.2.10", 2, 10,
                "password", 3469, "username3", 2, "");
        TransportCandidate cand4 = new ICECandidate("192.168.4.1", 3, 2,
                "password", 3468, "username4", 78, "");

        ICEResolver iceResolver = new ICEResolver(getConnection(0), "stun.xten.net", 3478) {
        };
        iceResolver.addCandidate(cand1);
        iceResolver.addCandidate(cand2);
        iceResolver.addCandidate(candH);
        iceResolver.addCandidate(cand3);
        iceResolver.addCandidate(cand4);

        assertEquals(iceResolver.getPreferredCandidate(), candH);
    }

    /**
     * Test priority generated by STUN lib
     *
     * @throws Exception
     */
    public void testICEPriority() throws Exception {

        String first = "";

        for (int i = 0; i < 100; i++) {

            ICENegociator cc = new ICENegociator((short) 1);
            // gather candidates
            cc.gatherCandidateAddresses();
            // priorize candidates
            cc.prioritizeCandidates();
            // get SortedCandidates

            for (Candidate candidate : cc.getSortedCandidates())
                try {
                    TransportCandidate transportCandidate = new ICECandidate(candidate.getAddress().getInetAddress().getHostAddress(), 1, candidate.getNetwork(), "1", candidate.getPort(), "1", candidate.getPriority(), "");
                    transportCandidate.setLocalIp(candidate.getBase().getAddress().getInetAddress().getHostAddress());
                    System.out.println("C: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress() + " p:" + candidate.getPriority());
                }
                catch (UtilityException e) {
                    e.printStackTrace();
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            Candidate candidate = cc.getSortedCandidates().get(0);
            String temp = "C: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress() + " p:" + candidate.getPriority();
            if (first.equals(""))
                first = temp;
            assertEquals(first, temp);
            first = temp;
        }
    }

    /**
     * Test for loadSTUNServers()
     *
     * @throws Exception
     */
    public void testLoadSTUNServers() throws Exception {
        STUNResolver stunResolver = new STUNResolver() {
        };
        ArrayList stunServers = stunResolver.loadSTUNServers();

        assertTrue(stunServers.size() > 0);
        System.out.println(stunServers.size() + " servers loaded");
    }

    public void testGetSTUNServer() {

        System.out.println(STUN.serviceAvailable(getConnection(0)));
        STUN stun = STUN.getSTUNServer(getConnection(0));
        System.out.println(stun.getHost() + ":" + stun.getPort());

    }

    /**
     * Test for resolve()
     *
     * @throws Exception
     */
    public void testResolve() throws Exception {

        final STUNResolver stunResolver = new STUNResolver() {
        };

        stunResolver.addListener(new TransportResolverListener.Resolver() {

            public void candidateAdded(final TransportCandidate cand) {
                incCounter();

                String addr = cand.getIp();
                int port = cand.getPort();

                System.out.println("Addr: " + addr + " port:" + port);

            }

            public void init() {
                System.out.println("Resolution started");
            }

            public void end() {
                System.out.println("Resolution finished");
            }
        });

        try {
            stunResolver.initialize();
            Thread.sleep(55000);
            assertTrue(valCounter() > 0);
            stunResolver.resolve();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Generate a list of payload types
     *
     * @return A testing list
     */
    private ArrayList getTestPayloads1() {
        ArrayList result = new ArrayList();

        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(36, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(45, "supercodec-4", 1, 98000));

        return result;
    }

    private ArrayList getTestPayloads2() {
        ArrayList result = new ArrayList();

        result.add(new PayloadType.Audio(27, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(32, "supercodec-4", 1, 98000));
        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));

        return result;
    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    public void testSTUNJingleSession() {

        resetCounter();

        try {
            TransportResolver tr1 = new STUNResolver() {
            };
            TransportResolver tr2 = new STUNResolver() {
            };

            // Explicit resolution
            tr1.resolve();
            tr2.resolve();

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from "
                            + request.getFrom() + ": accepting.");

                    // We accept the request
                    IncomingJingleSession session1;
                    try {
                        session1 = request.accept(getTestPayloads2());
                        session1.addListener(new JingleSessionListener() {
                            public void sessionClosed(String reason, JingleSession jingleSession) {
                            }

                            public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                            }

                            public void sessionDeclined(String reason, JingleSession jingleSession) {
                            }

                            public void sessionEstablished(PayloadType pt,
                                    TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                                incCounter();
                                System.out
                                        .println("Responder: the session is fully established.");
                                System.out.println("+ Payload Type: " + pt.getId());
                                System.out.println("+ Local IP/port: " + lc.getIp() + ":"
                                        + lc.getPort());
                                System.out.println("+ Remote IP/port: " + rc.getIp() + ":"
                                        + rc.getPort());
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }
                        });
                        session1.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                }

                public void sessionEstablished(PayloadType pt,
                        TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                    incCounter();
                    System.out.println("Initiator: the session is fully established.");
                    System.out.println("+ Payload Type: " + pt.getId());
                    System.out.println("+ Local IP/port: " + lc.getIp() + ":"
                            + lc.getPort());
                    System.out.println("+ Remote IP/port: " + rc.getIp() + ":"
                            + rc.getPort());
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });
            session0.start(null);

            Thread.sleep(60000);

            assertTrue(valCounter() == 2);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    public void testEcho() {

        TransportCandidate.Fixed c1 = new TransportCandidate.Fixed("localhost", 22222);
        TransportCandidate.Fixed c2 = new TransportCandidate.Fixed("localhost", 22444);

        try {
            c1.addCandidateEcho();
            c2.addCandidateEcho();

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            TransportCandidate.CandidateEcho ce1 = c1.getCandidateEcho();
            TransportCandidate.CandidateEcho ce2 = c2.getCandidateEcho();

            for (int i = 0; i < 10; i++) {
                assertTrue(ce1.test(InetAddress.getByName("localhost"), 22444, 100));
                System.out.println("Bind OK");
            }
            for (int i = 0; i < 10; i++) {
                assertTrue(ce2.test(InetAddress.getByName("localhost"), 22222, 100));
                System.out.println("Bind OK");
            }

        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    protected int getMaxConnections() {
        return 2;
    }
}
