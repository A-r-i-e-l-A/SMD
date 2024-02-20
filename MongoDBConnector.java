import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;
import java.util.Scanner;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;

import com.mongodb.client.FindIterable;

public class MongoDBConnector {

    private static final String connectionString = "mongodb://admin:admin@clustersaboristav2-shard-00-00.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-01.nmetz.mongodb.net:27017,clustersaboristav2-shard-00-02.nmetz.mongodb.net:27017/?replicaSet=atlas-osxu20-shard-0&ssl=true&authSource=admin";
    private static final MongoClientURI uri = new MongoClientURI(connectionString);
    private static final MongoClient mongoClient = new MongoClient(uri);
    private static final MongoDatabase database = mongoClient.getDatabase("SMD");

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

    public static List<Document> getAllPlacas() {
        List<Document> placasList = new ArrayList<>();

        // Get the collection
        MongoCollection<Document> collection = database.getCollection("placas");

        // Retrieve all documents from the collection
        FindIterable<Document> placasDocuments = collection.find();

        // Iterate over the documents and add them to the list
        for (Document document : placasDocuments) {
            placasList.add(document);
        }

        return placasList;
    }

    public static List<Document> getPlacaElements(String placaID) {
        // Initialize an empty list to store the elements
        List<Document> elements = new ArrayList<>();

        // Get the collection
        MongoCollection<Document> collection = database.getCollection("placas");

        // Find the document with the given placaID
        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        // Check if the document exists
        if (result != null) {
            // Retrieve the elements list from the document
            @SuppressWarnings("unchecked")
            List<Document> elementsList = (List<Document>) result.get("elements");

            // Add the elements to the list
            elements.addAll(elementsList);
        }

        // Return the list of elements
        return elements;
    }
}
