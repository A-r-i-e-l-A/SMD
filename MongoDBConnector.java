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
                    continue;
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

        Placa[] placasArray = new Placa[placasList.size()];
        placasArray = placasList.toArray(placasArray);

        return placasArray;
    }

    public static void addPlaca(String placaID, Placa[] placaArray) {

        Document existingDocument = collection.find(eq("placaID", placaID)).first();

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

        JOptionPane.showMessageDialog(null, "PCB successfully uploaded.");
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

        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

        for (Document elementDoc : elementsList) {
            double posX = elementDoc.getDouble("posX");
            elementDoc.put("posX", posX * -1);
        }

        collection.updateOne(query, new Document("$set", new Document("elements", elementsList)));

        return true;
    }

    public static boolean chooseCenter(String placaID, String elementID) {

        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result == null) {
            System.out.println("PlacaID not found in the database: " + placaID);
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Document> elementsList = (List<Document>) result.get("elements");

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

        double offsetX = chosenElement.getDouble("posX");
        double offsetY = chosenElement.getDouble("posY");

        // Update the elements' posX and posY and round to three decimals
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

        FindIterable<Document> placasDocuments = collection.find();

        for (Document document : placasDocuments) {
            placasList.add(document);
        }

        return placasList;
    }

    public static List<Document> getPlacaElements(String placaID) {
        List<Document> elements = new ArrayList<>();

        Document query = new Document("placaID", placaID);
        Document result = collection.find(query).first();

        if (result != null) {
            @SuppressWarnings("unchecked")
            List<Document> elementsList = (List<Document>) result.get("elements");
            elements.addAll(elementsList);
        }

        return elements;
    }

    public static boolean placaExists(String placaID) {
        try (MongoClient mongoClient = new MongoClient(uri)) {
            Document query = new Document("placaID", placaID);
            long count = collection.countDocuments(query);

            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
