import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;

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

}
