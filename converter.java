import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class converter {
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
}
