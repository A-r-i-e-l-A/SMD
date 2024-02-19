import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;
import java.util.Scanner;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;

public class MongoDBConnector {

    private static final String connectionString = "mongodb://admin:admin@clustersaboristav2-shard-00-00.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-01.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-02.nmetz.mongodb.net:27017/?replicaSet=atlas-osxu20-shard-0&ssl=true&authSource=admin";
    private static final MongoClientURI uri = new MongoClientURI(connectionString);
    private static final MongoClient mongoClient = new MongoClient(uri);
    private static final MongoDatabase database = mongoClient.getDatabase("SMD");

    public static Placa[] createPlacaArrayFromTXT(String filename) {
        List<Placa> placasList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                if (lineCount == 1 || !br.ready()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length == 6 || parts.length == 7) {
                    String ID = parts[0];
                    String type = parts[1];
                    String outline = parts[2];
                    double posX = Double.parseDouble(parts[3].replace(',', '.'));
                    double posY = Double.parseDouble(parts[4].replace(',', '.'));
                    boolean flip = false;
                    double rotation = -1;
                    if (parts.length == 6) {
                        flip = false;
                        rotation = Double.parseDouble(parts[5]);
                    } else {
                        flip = true;
                        rotation = Double.parseDouble(parts[6]);
                    }
                    int files = 1;

                    Placa placa = new Placa(ID, type, outline, posX, posY, rotation, flip, files);
                    placasList.add(placa);
                } else {
                    System.err.println("Invalid line format: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Placa[] placasArray = new Placa[placasList.size()];
        placasArray = placasList.toArray(placasArray);

        return placasArray;
    }

    public static Placa[] createPlacaArrayFromCSV(String filename) {
        List<Placa> placasList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            String[] line;
            int lineCount = 0;
            while ((line = reader.readNext()) != null) {
                lineCount++;
                if (lineCount == 1) {
                    continue; // Skip header line
                }

                if (line.length == 7) {
                    // Remove double quotes from the first three values
                    String ID = line[0].replaceAll("^\"|\"$", "");
                    String type = line[1].replaceAll("^\"|\"$", "");
                    String outline = line[2].replaceAll("^\"|\"$", "");

                    // Replace commas with dots in posX, posY, and rotation
                    double posX = Double.parseDouble(line[3]);
                    double posY = Double.parseDouble(line[4]);
                    double rotation = Double.parseDouble(line[5]);

                    boolean flip = true; // Flip is always true for CSV entries
                    int files = 2; // Files is always 2 for CSV entries

                    Placa placa = new Placa(ID, type, outline, posX, posY, rotation, flip, files);
                    placasList.add(placa);
                } else {
                    System.err.println("Invalid line format: " + String.join(",", line));
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        // Convert ArrayList to array
        Placa[] placasArray = new Placa[placasList.size()];
        placasArray = placasList.toArray(placasArray);

        return placasArray;
    }

    public static void addPlaca(String placaID, Placa[] placaArray) {
        MongoCollection<Document> collection = database.getCollection("placas");

        Document existingDocument = collection.find(eq("placaID", placaID)).first();

        if (existingDocument != null) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("The Placa " + placaID + " already exists. Do you want to overwrite it? (Y/N)");
            String input = scanner.nextLine().trim().toUpperCase();
            scanner.close();

            if (!input.equals("Y")) {
                System.out.println("Operation canceled.");
                return;
            }

            collection.deleteOne(existingDocument);
        }

        Document elementsDoc = new Document();
        elementsDoc.append("placaID", placaID);

        List<Document> elementsList = new ArrayList<>();
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

        elementsDoc.append("elements", elementsList);
        elementsDoc.append("size", placaArray.length);

        collection.insertOne(elementsDoc);
    }

    public static String placaArrayToASQ(String placaID) {
        Document existingDocument = database.getCollection("placas").find(eq("placaID", placaID)).first();
        if (existingDocument == null) {
            System.out.println("placaID " + placaID + " does not exist.");
            return null;
        }

        List<Document> elementsList = existingDocument.getList("elements", Document.class);
        StringBuilder sb = new StringBuilder();

        for (Document elementDoc : elementsList) {
            sb.append("#").append(elementDoc.getString("ID")).append("#,");

            sb.append(elementDoc.getDouble("posX")).append(", ").append(elementDoc.getDouble("posY")).append(", ")
                    .append(elementDoc.getDouble("rotation").intValue()).append(",");

            sb.append("#PXY#,##,#").append(elementDoc.getString("type")).append(" ")
                    .append(elementDoc.getString("outline"))
                    .append(" #,1,T,#1#,0,F,#TAPE#,#X#,");

            sb.append("A#,##,##,F\n");
        }

        return sb.toString();
    }

    public static String placaArrayToCSV(String placaID) {
        System.out.println("Attempting to create CSV for placaID: " + placaID);

        MongoCollection<Document> collection = database.getCollection("placas");

        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result == null) {
            System.out.println("placaID not found in the database: " + placaID);
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Designator,NozzleNum,StackNum,Mid X,Mid Y,Rotation,Height,Speed,Vision,Pressure,Explanation\n");

        sb.append("\n");

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");
        for (Document elementDoc : elementsList) {
            sb.append(elementDoc.getString("ID")).append(",1/2,1,");

            String formattedPosX = String.format("%.6f", elementDoc.getDouble("posX"));
            sb.append(formattedPosX).append(",");

            String formattedPosY = String.format("%.6f", elementDoc.getDouble("posY"));
            sb.append(formattedPosY).append(",");

            int rotationIntegerPart = elementDoc.getDouble("rotation").intValue();
            String formattedRotation = String.format("%d.000000", rotationIntegerPart);
            sb.append(formattedRotation).append(",");

            sb.append("0,100,XX,TRUE,").append(elementDoc.getString("type")).append(" ")
                    .append(elementDoc.getString("outline")).append("\n");
        }

        return sb.toString();
    }

    public static boolean flipComponents(String placaID) {
        MongoCollection<Document> collection = database.getCollection("placas");

        // Find the document with the given placaID
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        // Retrieve the elements list from the document
        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        // Update the elements by multiplying their posX by -1
        for (Document elementDoc : elementsList) {
            double posX = elementDoc.getDouble("posX");
            elementDoc.put("posX", posX * -1);
        }

        // Update the document in the collection
        collection.updateOne(query, new Document("$set", new Document("elements", elementsList)));

        return true;
    }

    public static boolean chooseCenter(String placaID, String elementID) {
        MongoCollection<Document> collection = database.getCollection("placas");

        // Find the document with the given placaID
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        // Retrieve the elements list from the document
        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        // Find the element with the given elementID
        Document chosenElement = null;
        for (Document elementDoc : elementsList) {
            if (elementDoc.getString("ID").equals(elementID)) {
                chosenElement = elementDoc;
                break;
            }
        }

        if (chosenElement == null) {
            System.out.println("ElementID not found in the Placa: " + elementID);
            return false;
        }

        // Calculate the offset
        double offsetX = chosenElement.getDouble("posX");
        double offsetY = chosenElement.getDouble("posY");

        // Update the elements' posX and posY by subtracting the offset and rounding to
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

        collection.updateOne(query, new Document("$set", new Document("elements", elementsList)));

        return true;
    }
}
