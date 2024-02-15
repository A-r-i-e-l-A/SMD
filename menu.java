import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class menu {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int choice = -1;

        Placa[] placaArray = null;

        do {
            System.out.println("File Converter Menu:");
            System.out.println("1. Convert txt to Placa");
            System.out.println("2. Convert csv to Placa");
            System.out.println("3. Convert Placa to asq");
            System.out.println("4. Convert Placa to csv");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                }
            } else {
                scanner.nextLine();
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter the input file name (without extension): ");
                    String fileName1 = scanner.next() + ".txt";
                    placaArray = converter.createPlacaArrayFromTXT("./Files/" + fileName1);
                    if (placaArray != null) {
                        System.out.print("Enter the name for this Placa: ");
                        String placaID = scanner.next();
                        MongoDBConnector.addPlaca(placaID, placaArray);
                    } else {
                        System.out.println("Failed to add elements from file.");
                    }
                    break;
                case 2:
                    System.out.print("Enter the input file name (without extension): ");
                    String fileName2 = scanner.next() + ".csv";
                    placaArray = converter.createPlacaArrayFromCSV("./Files/" + fileName2);
                    if (placaArray != null) {
                        System.out.print("Enter the name for this Placa: ");
                        String placaID = scanner.next();
                        MongoDBConnector.addPlaca(placaID, placaArray);
                    } else {
                        System.out.println("Failed to add elements from file.");
                    }
                    break;

                case 3:
                    System.out.print("Enter the PlacaID: ");
                    String placaID3 = scanner.next();
                    String asqContent = MongoDBConnector.placaArrayToASQ(placaID3);
                    if (asqContent == null) {
                        System.out.println("Placa with ID '" + placaID3
                                + "' does not exist. Please enter a valid PlacaID.");
                    } else {
                        System.out.print("Enter the name of the output file: ");
                        String outputFileName3 = scanner.next();

                        writeToFile(asqContent, outputFileName3, ".asq");
                    }
                    break;
                case 4:
                    System.out.print("Enter the PlacaID: ");
                    String placaID4 = scanner.next();
                    String csvContent = MongoDBConnector.placaArrayToCSV(placaID4);
                    if (csvContent == null) {
                        System.out.println("Placa with ID '" + placaID4
                                + "' does not exist. Please enter a valid PlacaID.");
                    } else {
                        System.out.print("Enter the name of the output file: ");
                        String outputFileName4 = scanner.next();

                        writeToFile(csvContent, outputFileName4, ".csv");
                    }
                    break;

                case 0:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 0 and 4.");
            }
        } while (choice != 0 && scanner.hasNextLine());

        scanner.close();
    }

    private static void writeToFile(String content, String fileName, String fileExtension) {
        String filePath = "./Files/" + fileName + fileExtension;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
            System.out.println("File '" + fileName + "' created successfully.");
        } catch (IOException e) {
            System.err.println("Failed to write file: " + e.getMessage());
        }
    }

}
