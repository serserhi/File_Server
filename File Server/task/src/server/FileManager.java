package server;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class FileManager {

    // The first 2 File.separator lines are commented to pass the checks in
    // Hyperskill. To test this program locally we have to let those lines uncommented

    private static final String ID_STORE_FILE =  System.getProperty("user.dir")
    //        + File.separator + "File Server"
    //        + File.separator + "task"
            + File.separator + "src"
            + File.separator + "server"
            + File.separator + "data"
            + File.separator + "file_ids.txt"; // File to store IDs
    private static final Object fileLock = new Object(); // Object to synchronize file access

    /*    public static void main(String[] args) {
            if (args.length == 0) {
                System.out.println("Usage: java FileIDGenerator <file1> <file2> ...");
                return;
            }

            Map<String, String> existingIds = loadExistingIds(); // Load existing IDs from file
            try {
                for (String filePath : args) {
                    File file = new File(filePath);

                    if (!file.exists()) {
                        System.err.println("File not found: " + filePath);
                        continue; // Skip to the next file
                    }

                    String fileId = existingIds.get(file.getAbsolutePath()); // Check if we already have an ID

                    if (fileId == null) {
                        fileId = generateUniqueId();
                        existingIds.put(file.getAbsolutePath(), fileId); // Store the new ID
                        System.out.println("Generated ID for " + filePath + ": " + fileId);
                    } else {
                        System.out.println("Existing ID found for " + filePath + ": " + fileId);
                    }
                }

                saveIdsToFile(existingIds); // Save all IDs to the storage file

            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
        } */

        // Function to generate a unique ID using UUID
        public static String generateUniqueId() {
            return UUID.randomUUID().toString();
        }

        // Load existing IDs from the storage file
        public static Map<String, String> loadExistingIds() {
            Map<String, String> ids = new HashMap<>();
            File idFile = new File(ID_STORE_FILE);

            if (!idFile.exists()) {
                return ids; // Return an empty map if the file doesn't exist
            }

            synchronized (fileLock) { // Synchronize file access
                try (BufferedReader reader = new BufferedReader(new FileReader(idFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("=", 2); // Split into filename and ID
                        if (parts.length == 2) {
                            ids.put(parts[0], parts[1]);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error loading existing IDs: " + e.getMessage());
                }
            }
            return ids;
        }

        // Save the filename and ID pairs to the storage file
        public static void saveIdsToFile(Map<String, String> ids) throws IOException {
            synchronized (fileLock) { // Synchronize file access
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ID_STORE_FILE))) {
                    for (Map.Entry<String, String> entry : ids.entrySet()) {
                        writer.write(entry.getKey() + "=" + entry.getValue());
                        writer.newLine();
                    }
                }
            }
        }
    }

