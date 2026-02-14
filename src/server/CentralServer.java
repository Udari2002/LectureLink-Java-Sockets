package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class CentralServer {
    private static final int TCP_PORT = 8888;
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 9999;

    // Keep track of active clients if needed, or just handle them per thread
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        System.out.println("LectureLink Server started on TCP port " + TCP_PORT);
        System.out.println("Multicast Group: " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // TRIM the input to remove leading/trailing spaces
                    inputLine = inputLine.trim();

                    if (inputLine.isEmpty()) {
                        continue; // Skip empty lines
                    }

                    System.out.println("Received from " + (clientName != null ? clientName : socket.getInetAddress()) + ": " + inputLine);

                    // Split by whitespace, limit to 2 parts
                    String[] parts = inputLine.split("\\s+", 2);
                    String command = parts[0].toUpperCase();

                    switch (command) {
                        case "LOGIN":
                            if (parts.length > 1) {
                                clientName = parts[1];
                                out.println("Welcome to LectureLink, " + clientName + "!");
                            } else {
                                out.println("Usage: LOGIN <name>");
                            }
                            break;
                        case "SUBSCRIBE":
                            if (parts.length > 1) {
                                String course = parts[1];
                                out.println("Subscribed to course: " + course);
                            } else {
                                out.println("Usage: SUBSCRIBE <course_code>");
                            }
                            break;
                        case "ANNOUNCE":
                            if (parts.length > 1) {
                                String message = parts[1];
                                broadcastUDP(message);
                                out.println("Announcement broadcasted via UDP.");
                            } else {
                                out.println("Usage: ANNOUNCE <message>");
                            }
                            break;
                        case "EXIT":
                            out.println("Goodbye!");
                            return; // Exit the loop to close connection
                        default:
                            out.println("Unknown command: '" + command + "'. Available: LOGIN, SUBSCRIBE, ANNOUNCE, EXIT");
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
            }
        }

        private void broadcastUDP(String message) {
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                String formattedMessage = "[ANNOUNCEMENT] " + (clientName != null ? clientName : "Server") + ": " + message;
                byte[] buf = formattedMessage.getBytes();
                
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                udpSocket.send(packet);
                System.out.println("Multicast sent: " + formattedMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
