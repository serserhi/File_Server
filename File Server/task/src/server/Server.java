package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 34522;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10); // Adjust pool size as needed
    private static volatile Set<Integer> uniqueNumbersSet = generateNumbersSet();

    private static volatile boolean running = true; // Flag to control the server loop
    private static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {

        try  {
            System.out.println("Server started!");
            serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(ADDRESS));

            while (isRunning()) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.submit(new RequestThread(socket, uniqueNumbersSet));
                } catch (SocketException e) {
                    // This is expected when the server socket is closed for shutdown.
                    if (isRunning()) { // Only log if it wasn't an intentional shutdown
                        System.err.println("SocketException during accept: " + e.getMessage());
                    }
                    break; // Exit the loop
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }

            }

            executor.shutdown(); //Initiate an orderly shutdown
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Executor not terminated in 5 seconds.  Forcing shutdown.");
                    executor.shutdownNow(); // Forcefully shutdown
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();  //Re-interrupt the thread
            }

            System.out.println("Server stopped.");

        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT + ": " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static Set<Integer> generateNumbersSet() {
        Set<Integer> uniqueNumbersSet = new HashSet<>();
        Random r = new Random();
        while (uniqueNumbersSet.size() < 100) {
            uniqueNumbersSet.add(r.nextInt(100));
        }
        return uniqueNumbersSet;
    }

    //Synchronized getter and setter methods for the 'running' flag
    public static synchronized boolean isRunning() {
        return running;
    }

    public static synchronized void setRunning(boolean value) {
        running = value;
        if (!value && serverSocket != null) {
            try {
                serverSocket.close(); // Interrupt server.accept()
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}