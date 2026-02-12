package student;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class StudentClient {
    private static String SERVER_ADDRESS = "localhost";
    private static final int TCP_PORT = 8888;
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 9999;

    public static void main(String[] args) {
        // Allow server address to be specified as command line argument
        if (args.length > 0) {
            SERVER_ADDRESS = args[0];
        }
        
        System.out.println("Connecting to server at " + SERVER_ADDRESS + ":" + TCP_PORT);
        
        // Start a thread to listen for UDP multicast announcements
        new Thread(StudentClient::listenForAnnouncements).start();

        try (Socket socket = new Socket(SERVER_ADDRESS, TCP_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to LectureLink Server.");
            System.out.println("Commands: LOGIN <name>, SUBSCRIBE <course>");

            // Start a thread to read responses from the server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("Server: " + response);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                out.println(input);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void listenForAnnouncements() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            InetSocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.joinGroup(groupAddress, netIf);
            System.out.println("Joined multicast group for announcements.");
            System.out.println("Note: If you don't receive announcements, check your firewall settings.");

            byte[] buf = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("\n[ANNOUNCEMENT] " + received + "\n> ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
