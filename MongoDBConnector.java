import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JOptionPane;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;

public class MongoDBConnector {
    // ignore this incredible security system (:
    private static final String connectionString = "mongodb://admin:admin@clustersaboristav2-shard-00-00.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-01.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-02.nmetz.mongodb.net:27017/?replicaSet=atlas-osxu20-shard-0&ssl=true&authSource=admin";
    private static final MongoClientURI uri = new MongoClientURI(connectionString);
    private static final MongoClient mongoClient = new MongoClient(uri);
    private static final MongoDatabase database = mongoClient.getDatabase("SMD");
    private static final MongoCollection<Document> collection = database.getCollection("placas");

    // Method to create a Placa array from a TXT file
    public static Placa[] createPlacaArrayFromTXT(String filename) {
        // List to store Placa objects
        List<Placa> placasList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (lineCount == 1 || !br.ready()) {
                    continue;
                }

                // Splitting the line into parts
                String[] parts = line.split("\\s+");
                if (parts.length == 6 || parts.length == 7) {
                    // Extracting values from parts and creating a Placa object
                    String ID = parts[0];
                    String type = parts[1];
                    String outline = parts[2];
                    double posX = Double.parseDouble(parts[3].replace(',', '.'));
                    double posY = Double.parseDouble(parts[4].replace(',', '.'));
                    boolean flip = parts.length == 7;
                    double rotation = Double.parseDouble(parts[parts.length - 1]);

                    Placa placa = new Placa(ID, type, outline, posX, posY, rotation, flip, 1);
                    placasList.add(placa);
                } else {
                    System.err.println("Invalid line format: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert List to array
        Placa[] placasArray = new Placa[placasList.size()];
        placasArray = placasList.toArray(placasArray);

        return placasArray;
    }

    // Method to create a Placa array from a CSV file
    public static Placa[] createPlacaArrayFromCSV(String filename) {
        // List to store Placa objects
        List<Placa> placasList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] line;
            int lineCount = 0;
            while ((line = reader.readNext()) != null) {
                lineCount++;
                if (lineCount == 1) {
                    continue;
                }

                if (line.length == 7) {
                    // Extracting values from CSV line
                    String ID = line[0].replaceAll("^\"|\"$", "");
                    String type = line[1].replaceAll("^\"|\"$", "");
                    String outline = line[2].replaceAll("^\"|\"$", "");
                    double posX = Double.parseDouble(line[3]);
                    double posY = Double.parseDouble(line[4]);
                    double rotation = Double.parseDouble(line[5]);

                    // Creating Placa object and adding it to the list
                    Placa placa = new Placa(ID, type, outline, posX, posY, rotation, true, 2);
                    placasList.add(placa);
                } else {
                    System.err.println("Invalid line format: " + String.join(",", line));
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        // Convert List to array
        Placa[] placasArray = new Placa[placasList.size()];
        placasArray = placasList.toArray(placasArray);

        return placasArray;
    }

    // Method to add a Placa to the MongoDB collection
    public static void addPlaca(String placaID, Placa[] placaArray) {
        // Check if the Placa already exists
        Document existingDocument = collection.find(eq("placaID", placaID)).first();

        // If it exists, prompt the user to confirm overwriting
        if (existingDocument != null) {
            int option = JOptionPane.showConfirmDialog(null,
                    "The PCB '" + placaID + "' already exists. Do you want to overwrite it?", "Duplicated PCB Name",
                    JOptionPane.YES_NO_OPTION);

            if (option != JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(null, "Operation cancelled.");
                return;
            }
            collection.deleteOne(existingDocument);
        }

        // Create a Document for the Placa and its elements
        Document elementsDoc = new Document();
        elementsDoc.append("placaID", placaID);
        List<Document> elementsList = new ArrayList<>();

        // Add each Placa in the array to the elements list
        for (Placa placa : placaArray) {
            Document elementDoc = new Document("ID", placa.getID())
                    .append("type", placa.getType())
                    .append("outline", placa.getOutline())
                    .append("posX", placa.getPosX())
                    .append("posY", placa.getPosY())
                    .append("rotation", placa.getRotation())
                    .append("flip", placa.isFlip())
                    .append("files", placa.getFiles());
            elementsList.add(elementDoc);
        }

        // Add elements list and size to the document
        elementsDoc.append("elements", elementsList);
        elementsDoc.append("size", placaArray.length);
        // Insert the document into the collection
        collection.insertOne(elementsDoc);

        JOptionPane.showMessageDialog(null, "PCB successfully uploaded.");
    }

    // Method to convert Placa array to ASQ format
    public static String placaArrayToASQ(String placaID) {
        // Retrieve Placa document from the database
        Document existingDocument = database.getCollection("placas").find(eq("placaID", placaID)).first();

        // Check if Placa document exists
        if (existingDocument == null) {
            System.out.println("placaID " + placaID + " does not exist.");
            return null;
        }

        // Retrieve elements list from the Placa document
        List<Document> elementsList = existingDocument.getList("elements", Document.class);
        StringBuilder sb = new StringBuilder();

        // Iterate through elements and append ASQ formatted strings to StringBuilder
        for (Document elementDoc : elementsList) {
            sb.append("#").append(elementDoc.getString("ID")).append("#,");
            sb.append(elementDoc.getDouble("posX")).append(", ").append(elementDoc.getDouble("posY")).append(", ")
                    .append(elementDoc.getDouble("rotation").intValue()).append(",");
            sb.append("#PXY#,##,#").append(elementDoc.getString("type")).append(" ")
                    .append(elementDoc.getString("outline")).append(" #,1,T,#1#,0,F,#TAPE#,#X#,");
            sb.append("A#,##,##,F\n");
        }

        return sb.toString();
    }

    // Method to convert Placa array to CSV format
    public static String placaArrayToCSV(String placaID) {
        // Print attempt message
        System.out.println("Attempting to create CSV for placaID: " + placaID);

        // Retrieve Placa document from the database
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        // Check if Placa document exists
        if (result == null) {
            System.out.println("placaID not found in the database: " + placaID);
            return null;
        }

        StringBuilder sb = new StringBuilder();

        // Append CSV header
        sb.append("Designator,NozzleNum,StackNum,Mid X,Mid Y,Rotation,Height,Speed,Vision,Pressure,Explanation\n");
        sb.append("\n");

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        // Iterate through elements and append CSV formatted strings to StringBuilder
        for (Document elementDoc : elementsList) {
            sb.append(elementDoc.getString("ID")).append(",1/2,1,");

            // Format posX, posY, and rotation
            String formattedPosX = String.format("%.6f", elementDoc.getDouble("posX"));
            sb.append(formattedPosX).append(",");

            String formattedPosY = String.format("%.6f", elementDoc.getDouble("posY"));
            sb.append(formattedPosY).append(",");

            int rotationIntegerPart = elementDoc.getDouble("rotation").intValue();
            String formattedRotation = String.format("%d.000000", rotationIntegerPart);
            sb.append(formattedRotation).append(",");

            // Append additional CSV data
            sb.append("0,100,XX,TRUE,").append(elementDoc.getString("type")).append(" ")
                    .append(elementDoc.getString("outline")).append("\n");
        }

        return sb.toString();
    }

    // Method to flip components of a Placa
    public static boolean flipComponents(String placaID) {
        // Retrieve Placa document from the database
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        // Check if Placa document exists
        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        // Iterate through elements and flip posX values
        for (Document elementDoc : elementsList) {
            double posX = elementDoc.getDouble("posX");
            elementDoc.put("posX", posX * -1);
        }

        // Update the Placa document with the flipped elements
        collection.updateOne(query, new Document("$set", new Document("elements", elementsList)));

        return true;
    }

    // Method to choose center of a Placa
    public static boolean chooseCenter(String placaID, String elementID) {
        // Retrieve Placa document from the database
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        // Check if Placa document exists
        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        Document chosenElement = null;
        // Find the element with the given ID
        for (Document elementDoc : elementsList) {
            if (elementDoc.getString("ID").equals(elementID)) {
                chosenElement = elementDoc;
                break;
            }
        }

        // Check if the chosen element exists
        if (chosenElement == null) {
            System.out.println("ElementID not found in the Placa: " + elementID);
            return false;
        }

        // Calculate offset based on the chosen element's position
        double offsetX = chosenElement.getDouble("posX");
        double offsetY = chosenElement.getDouble("posY");

        // Update elements' posX and posY with respect to the chosen center and round to
        // three decimals
        DecimalFormat df = new DecimalFormat("#.###");
        for (Document elementDoc : elementsList) {
            double posX = elementDoc.getDouble("posX");
            double posY = elementDoc.getDouble("posY");
            double roundedPosX = Double.parseDouble(df.format(posX - offsetX).replace(",", "."));
            double roundedPosY = Double.parseDouble(df.format(posY - offsetY).replace(",", "."));
            elementDoc.put("posX", roundedPosX);
            elementDoc.put("posY", roundedPosY);
        }

        // Update the Placa document with the adjusted elements
        collection.updateOne(query, new Document("$set", new Document("elements", elementsList)));
        return true;
    }

    // Method to retrieve all Placas from the database
    public static List<Document> getAllPlacas() {
        List<Document> placasList = new ArrayList<>();

        // Retrieve all Placas from the collection
        FindIterable<Document> placasDocuments = collection.find();

        // Add each Placa document to the list
        for (Document document : placasDocuments) {
            placasList.add(document);
        }

        return placasList;
    }

    public static List<Document> getPlacaElements(String placaID) {
        List<Document> elements = new ArrayList<>();

        // Querying the MongoDB collection for the Placa
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        // If the Placa is found, retrieve its elements
        if (result != null) {
            @SuppressWarnings("unchecked")
            List<Document> elementsList = (List<Document>) result.get("elements");
            elements.addAll(elementsList);
        }

        return elements;
    }

    public static boolean placaExists(String placaID) {
        try (MongoClient mongoClient = new MongoClient(uri)) {
            // Querying the MongoDB collection to count documents matching the PlacaID
            Document query = new Document("placaID", placaID);
            long count = collection.countDocuments(query);

            return count > 0;
        } catch (Exception e) {
            // Print stack trace in case of any exceptions
            e.printStackTrace();
            return false;
        }
    }
}
