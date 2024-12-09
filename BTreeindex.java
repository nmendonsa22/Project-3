import java.io.*;
import java.util.*;

public class BTreeindex {
    private static final int BLOCK_SIZE = 512;
    private static final String MAGIC_IDENTIFIER = "4337PRJ3";
    private RandomAccessFile currentFile;
    private boolean isFileOpen = false;

    private long rootNodeBlockId = 0;
    private long nextAvailableBlockId = 1;

    public static void main(String[] args) {
        BTreeIndex bTreeIndex = new BTreeIndex();
        bTreeIndex.startInteractiveSession();
    }

    private void startInteractiveSession() {
        Scanner scanner = new Scanner(System.in);
        String userCommand;

        while (true) {
            System.out.println("\nEnter a command: ");
            userCommand = scanner.nextLine().toLowerCase();

            switch (userCommand) {
                case "create":
                    createNewIndexFile(scanner);
                    break;
                case "open":
                    openExistingIndexFile(scanner);
                    break;
                case "insert":
                    insertKeyValuePair(scanner);
                    break;
                case "search":
                    searchForKey(scanner);
                    break;
                case "print":
                    displayAllKeyValuePairs();
                    break;
                case "extract":
                    saveIndexToFile(scanner);
                    break;
                case "quit":
                    closeCurrentFile();
                    System.out.println("Exiting the program...");
                    return;
                default:
                    System.out.println("Invalid command. Please try again.");
            }
        }
    }

    private void createNewIndexFile(Scanner scanner) {
        if (isFileOpen) {
            System.out.println("A file is already open. Please close it before creating a new one.");
            return;
        }

        System.out.println("Enter the file name: ");
        String fileName = scanner.nextLine();

        try {
            File file = new File(fileName);
            if (file.exists()) {
                System.out.println("File already exists. Overwrite? (y/n)");
                String response = scanner.nextLine().toLowerCase();
                if (!response.equals("y")) return;
            }
            currentFile = new RandomAccessFile(file, "rw");
            writeFileHeader();
            isFileOpen = true;
            System.out.println("New index file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openExistingIndexFile(Scanner scanner) {
        if (isFileOpen) {
            System.out.println("A file is already open.");
            return;
        }

        System.out.println("Enter the file name: ");
        String fileName = scanner.nextLine();

        try {
            currentFile = new RandomAccessFile(fileName, "rw");
            if (!validateFileHeader()) {
                System.out.println("Invalid file format. Cannot open.");
                currentFile.close();
                return;
            }
            isFileOpen = true;
            System.out.println("Index file opened successfully.");
        } catch (IOException e) {
            System.out.println("File not found.");
        }
    }

    private boolean validateFileHeader() throws IOException {
        byte[] magicNumberBytes = new byte[8];
        currentFile.read(magicNumberBytes);
        String magicNumber = new String(magicNumberBytes);
        return magicNumber.equals(MAGIC_IDENTIFIER);
    }

    private void writeFileHeader() throws IOException {
        currentFile.setLength(0);
        byte[] magicBytes = MAGIC_IDENTIFIER.getBytes();
        currentFile.write(magicBytes);

        currentFile.writeLong(rootNodeBlockId);
        currentFile.writeLong(nextAvailableBlockId);

        byte[] emptySpace = new byte[BLOCK_SIZE - 8 - 8 - 8];
        currentFile.write(emptySpace);
    }

    private void insertKeyValuePair(Scanner scanner) {
        if (!isFileOpen) {
            System.out.println("No file is open.");
            return;
        }

        System.out.println("Enter the key (long): ");
        long key = scanner.nextLong();
        System.out.println("Enter the value (long): ");
        long value = scanner.nextLong();

        try {
            currentFile.seek(BLOCK_SIZE);
            long blockId = nextAvailableBlockId++;
            currentFile.writeLong(blockId);
            currentFile.writeLong(0); // Parent node id (root if 0)
            currentFile.writeLong(1); // Number of key-value pairs

            currentFile.writeLong(key);
            currentFile.writeLong(value);

            byte[] childPointers = new byte[160];
            currentFile.write(childPointers);

            System.out.println("Key-Value pair inserted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void searchForKey(Scanner scanner) {
        if (!isFileOpen) {
            System.out.println("No file is open.");
            return;
        }

        System.out.println("Enter key to search: ");
        long key = scanner.nextLong();

        try {
            currentFile.seek(BLOCK_SIZE);
            long blockId;
            while (currentFile.getFilePointer() < currentFile.length()) {
                blockId = currentFile.readLong();
                currentFile.readLong();
                long numEntries = currentFile.readLong();

                for (int i = 0; i < numEntries; i++) {
                    long storedKey = currentFile.readLong();
                    long storedValue = currentFile.readLong();

                    if (storedKey == key) {
                        System.out.println("Found: " + storedKey + " -> " + storedValue);
                        return;
                    }
                }
            }
            System.out.println("Key not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayAllKeyValuePairs() {
        if (!isFileOpen) {
            System.out.println("No file is open.");
            return;
        }

        try {
            currentFile.seek(BLOCK_SIZE);
            while (currentFile.getFilePointer() < currentFile.length()) {
                long blockId = currentFile.readLong();
                currentFile.readLong();
                long numEntries = currentFile.readLong();

                for (int i = 0; i < numEntries; i++) {
                    long key = currentFile.readLong();
                    long value = currentFile.readLong();
                    System.out.println(key + " -> " + value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIndexToFile(Scanner scanner) {
        if (!isFileOpen) {
            System.out.println("No file is open.");
            return;
        }

        System.out.println("Enter the filename to extract data to: ");
        String fileName = scanner.nextLine();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            currentFile.seek(BLOCK_SIZE);
            while (currentFile.getFilePointer() < currentFile.length()) {
                long blockId = currentFile.readLong();
                currentFile.readLong();
                long numEntries = currentFile.readLong();

                for (int i = 0; i < numEntries; i++) {
                    long key = currentFile.readLong();
                    long value = currentFile.readLong();
                    writer.write(key + "," + value);
                    writer.newLine();
                }
            }
            System.out.println("Data successfully extracted.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeCurrentFile() {
        if (isFileOpen) {
            try {
                currentFile.close();
                isFileOpen = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
