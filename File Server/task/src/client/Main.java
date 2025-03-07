package client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Scanner;
import java.io.*;
import java.net.*;

public class Main  {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 34522;

    // The first 2 File.separator lines are commented to pass the checks in
    // Hyperskill. To test this program locally we have to let those lines uncommented

    private static final String CLIENT_DIR =
            System.getProperty("user.dir")
            //        + File.separator + "File Server"
            //        + File.separator + "task"
                    + File.separator + "src"
                    + File.separator + "client"
                    + File.separator + "data"
                    + File.separator ;

    public static void main(String[] args) throws IOException {

        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output  = new DataOutputStream(socket.getOutputStream())
        ) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Enter action (1 - get a file, 2 - create a file, 3 - delete a file):");
            String actionInput = sc.nextLine();
            String action = "";
            String by = "";
            String file = "";
            String msg = "";

            switch (actionInput) {
                case "1":
                    action = "GET";
                    System.out.print("Do you want to get the file by name or by id (1 - name, 2 - id):");
                    by = sc.nextLine();
                    if (by.equals("1")) {
                        System.out.print("Enter the name of the file: ");
                        by = "BY_NAME";
                    } else {
                        System.out.print("Enter id: ");
                        by = "BY_ID";
                    }
                    file= sc.nextLine();
                    msg = action + " " + by + " " + file;
                    output.writeUTF(msg); // send a message to the server
                    System.out.println("The request was sent.");

                    break;
                case "2":
                    action = "PUT";
                    System.out.print("Enter name of the file:");
                    file = sc.nextLine();
                    System.out.print("Enter name of the file to be saved on server:");
                    String fileServer = sc.nextLine();
                    if (fileServer.isEmpty()) {
                        Timestamp ts = Timestamp.from(Instant.now());
                        fileServer = ts.getTime() + file;
                    }
                    msg = action + " " + file + " " + fileServer;

                    Path path = Paths.get(CLIENT_DIR + file);
                    byte[] fileBytes = Files.readAllBytes(path);
                    output.writeUTF(msg); // send a message to the server
                    System.out.println("The request was sent.");
                    output.writeInt(fileBytes.length);
                    output.write(fileBytes);
                    break;
                case "3":
                    action = "DELETE";
                    System.out.print("Do you want to delete the file by name or by id (1 - name, 2 - id):");
                    by = sc.nextLine();
                    if (by.equals("1")) {
                        System.out.print("Enter the name of the file: ");
                        by = "BY_NAME";
                    } else {
                        System.out.print("Enter id: ");
                        by = "BY_ID";
                    }
                    file = sc.nextLine();
                    msg = action + " " + by + " " + file;
                    output.writeUTF(msg);
                    break;
                case "exit":
                    action = "EXIT";
                    msg = action;
                    output.writeUTF(msg);
                    System.out.println("The request was sent.");
                    return;
                default:
                    System.out.println("Invalid action.");
            }

            if (!action.equals("EXIT")) {
                String receivedMsg = input.readUTF(); // read the reply from the server
                String[] words = receivedMsg.split(" ");
                //System.out.println("Received: " + receivedMsg);
                switch (actionInput) {
                    case "1":
                        if (words[0].equals("404")) {
                            System.out.println("The response says that this file is not found!");
                        } else {
                            int length = input.readInt();
                            byte[] fileBytes = new byte[length];
                            input.readFully(fileBytes, 0, fileBytes.length);
                            System.out.println("The file was downloaded! Specify a name for it: ");
                            String fileClient = sc.nextLine();
                            Path path = Paths.get(CLIENT_DIR + fileClient);
                            try {
                                Files.write(path, fileBytes);
                                System.out.println("File saved on the hard drive!");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                        break;
                    case "2":
                        if (words[0].equals("403")) {
                            System.out.println("The response says that creating the file was forbidden!");
                        } else {
                            System.out.println("Response says that file is saved! ID = " + words[1]);
                        }
                        break;
                    case "3":
                        if (words[0].equals("404")) {
                            System.out.println("The response says that the file was not found!");
                        } else {
                            System.out.println("The response says that the file was successfully deleted!");
                        }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}