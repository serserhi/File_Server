package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

public class RequestThread implements Runnable {
    private final Socket socket;
    private Map<String, String> existingIds = FileManager.loadExistingIds();
    private Set<Integer> uniqueNumbersSet;// Reference to the Server instance

    // The first 2 File.separator lines are commented to pass the checks in
    // Hyperskill. To test this program locally we have to let those lines uncommented

    private static final String SERVER_DIR =
            System.getProperty("user.dir")
    //        + File.separator + "File Server"
    //        + File.separator + "task"
            + File.separator + "src"
            + File.separator + "server"
            + File.separator + "data"
            + File.separator ;

    public RequestThread(Socket socket, Set<Integer> uniqueNumbersSet) throws IOException { //PUT
        this.socket = socket;
        this.uniqueNumbersSet = uniqueNumbersSet;
    }

    @Override
    public void run() {

        try (
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            String msg = input.readUTF();
            if (!msg.equals("EXIT")) {
                String[] words = msg.split(" ");
                processRequest(words, input, output);
            } else {
                Server.setRunning(false); // Use the setter method
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                socket.close(); // Close the socket
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }


    private void processRequest(String[] words, DataInputStream input, DataOutputStream output ) throws IOException {

        switch (words[0]) {
            case "PUT":

                File file = new File(SERVER_DIR + words[2]);
                Path filePath = Paths.get(SERVER_DIR + words[2]);

                int length = input.readInt();
                byte[] fileBytes = new byte[length];
                input.readFully(fileBytes, 0, fileBytes.length);

                if (!file.exists()) {
                        try {
                            Files.write(filePath, fileBytes);
                            //String fileId = FileManager.generateUniqueId();
                            List<Integer> uniqueNumbersList = new ArrayList<>(uniqueNumbersSet);
                            Integer fileIDint = uniqueNumbersList.get(0);
                            uniqueNumbersSet.remove(fileIDint);
                            existingIds.put(fileIDint.toString(), file.getAbsolutePath()); // Store the new ID
                            FileManager.saveIdsToFile(existingIds);
                            output.writeUTF("200" + " " + fileIDint.toString());
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                } else {
                    output.writeUTF("403");
                    break;
                }
                break;

            case "GET":
                if (words[1].equals("BY_NAME")) {
                    file = new File(SERVER_DIR + words[2]);
                    if (!file.exists()) {
                        output.writeUTF("404");
                        break;
                    }
                } else {
                    if (existingIds.containsKey(words[2])) {
                        file = new File(existingIds.get(words[2]));
                    } else {
                        output.writeUTF("404");
                        break;
                    }
                }
                try {
                    output.writeUTF("200 ");
                    fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                    output.writeInt(fileBytes.length);
                    output.write(fileBytes);
                } catch (IOException e) {
                    output.writeUTF("404");
                }
                break;

            case "DELETE":
                String filename = null;

                if (words[1].equals("BY_NAME")) {
                    file = new File(SERVER_DIR + words[2]);
                } else {
                    if (existingIds.containsKey(words[2])) {
                        file = new File(existingIds.get(words[2]));
                    } else {
                        output.writeUTF("404");
                        break;
                    }
                }

                if (file.delete()) {
                    try {
                        String idToRemove = null;
                        for(Map.Entry<String, String> entry : existingIds.entrySet()){
                            if(entry.getValue().equals(file.getAbsolutePath())){
                                idToRemove = entry.getKey();
                                break;
                            }
                        }
                        if(idToRemove != null){
                            existingIds.remove(idToRemove);
                        } else {
                            output.writeUTF("404");
                        }
                        FileManager.saveIdsToFile(existingIds);
                        output.writeUTF("200");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        output.writeUTF("404");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        }



    }
}