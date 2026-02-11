package server;

import java.io.*;
import java.net.*;

public class CentralServer {
    private static final int TCP_PORT = 8888;
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 9999;

    public static void main(String[] args) {
        System.out.println("Server started on port " + TCP_PORT);
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received: " + inputLine);
                    String[] parts = inputLine.split(" ", 2);
                    String command = parts[0];

                    if ("ANNOUNCE".equalsIgnoreCase(command) && parts.length > 1) {
                        String message = parts[1];
                        broadcastUDP(message);
                        out.println("Announcement sent via UDP Multicast.");
                    } else if ("LOGIN".equalsIgnoreCase(command)) {
                        out.println("Logged in. Welcome to LectureLink.");
                    } else {
                        out.println("Unknown command.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void broadcastUDP(String message) {
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                byte[] buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
                udpSocket.send(packet);
                System.out.println("Multicasted: " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
