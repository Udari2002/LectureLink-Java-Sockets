package lecturer;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class LecturerClient {
    private static String SERVER_ADDRESS = "localhost";
    private static final int TCP_PORT = 8888;

    public static void main(String[] args) {
        // Allow server address to be specified as command line argument
        if (args.length > 0) {
            SERVER_ADDRESS = args[0];
        }
        
        System.out.println("Connecting to server at " + SERVER_ADDRESS + ":" + TCP_PORT);
        
        try (Socket socket = new Socket(SERVER_ADDRESS, TCP_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to LectureLink Server.");
            System.out.println("Commands: LOGIN <name>, ANNOUNCE <message>");

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
}
