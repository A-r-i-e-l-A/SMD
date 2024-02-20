import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import org.bson.Document;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class FileConverterGUI extends JFrame {
    private JButton txtToPlacaButton;
    private JButton csvToPlacaButton;
    private JButton placaToasqButton;
    private JButton placaTocsvButton;
    private JButton choosecenterButton;
    private JButton flipcomponentsButton;
    private JButton reloadDatabaseButton;

    private JTable table;

    private Placa[] placaArray;

    public FileConverterGUI() {
        super("File Converter");

        // Configurar la ventana
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar la ventana

        // Crear componentes
        txtToPlacaButton = new JButton("Add txt to database");
        csvToPlacaButton = new JButton("Add csv to database");
        placaToasqButton = new JButton("Convert element to asq");
        placaTocsvButton = new JButton("Convert element to csv");
        choosecenterButton = new JButton("Choose center");
        flipcomponentsButton = new JButton("Flip components");
        reloadDatabaseButton = new JButton("Reload database");

        // Configurar diseño
        JPanel panel = new JPanel();
        panel.add(txtToPlacaButton);
        panel.add(csvToPlacaButton);
        panel.add(placaToasqButton);
        panel.add(placaTocsvButton);
        panel.add(choosecenterButton);
        panel.add(flipcomponentsButton);
        panel.add(reloadDatabaseButton);
        panel.setBorder(new EmptyBorder(0, 100, 100, 100));
        add(panel, BorderLayout.NORTH); // Add buttons panel to the NORTH

        table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        add(scrollPane, BorderLayout.CENTER);

        reloadDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTableData(); // Reload the information displayed on the table
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        String placaID = (String) table.getValueAt(selectedRow, 0);
                        displayPlacaElements(placaID);
                    }
                }
            }
        });

        // Agregar oyentes de eventos
        txtToPlacaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select a text file (.txt)");

                // Establecer el filtro para mostrar solo archivos de texto (.txt)
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text files", "txt");
                fileChooser.setFileFilter(filter);

                int userSelection = fileChooser.showOpenDialog(FileConverterGUI.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String fileName = selectedFile.getAbsolutePath();

                    // Crea el array de placas a partir del archivo TXT seleccionado
                    placaArray = MongoDBConnector.createPlacaArrayFromTXT(fileName);

                    if (placaArray != null) {
                        JOptionPane.showMessageDialog(FileConverterGUI.this, "Placas created from the file:");
                        for (int i = 0; i < Math.min(3, placaArray.length); i++) {
                            System.out.println(placaArray[i]);
                        }

                        // Solicita al usuario el nombre de este conjunto de Placas
                        String placasID = JOptionPane.showInputDialog(FileConverterGUI.this,
                                "Enter the name of this set of Placas:");
                        if (placasID != null && !placasID.isEmpty()) { // Verifica si se ingresó un nombre válido
                            MongoDBConnector.addPlaca(placasID, placaArray);
                        } else {
                            JOptionPane.showMessageDialog(FileConverterGUI.this, "Invalid Placas name.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(FileConverterGUI.this, "Error creating Placas from TXT file.");
                    }
                }
            }
        });

        csvToPlacaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Crear un cuadro de diálogo para que el usuario seleccione el archivo CSV
                JFileChooser fileChooser = new JFileChooser();

                // Establecer el título del cuadro de diálogo
                fileChooser.setDialogTitle("Select a CSV file");

                // Mostrar el cuadro de diálogo para seleccionar el archivo
                int userSelection = fileChooser.showOpenDialog(FileConverterGUI.this);

                // Verificar si el usuario ha seleccionado un archivo
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    // Obtener el archivo seleccionado por el usuario
                    File selectedFile = fileChooser.getSelectedFile();

                    // Obtener la ruta absoluta del archivo seleccionado
                    String filePath = selectedFile.getAbsolutePath();

                    // Verificar si el archivo seleccionado es un archivo CSV
                    if (filePath.toLowerCase().endsWith(".csv")) {
                        // Crear el array de placas a partir del archivo CSV seleccionado
                        placaArray = MongoDBConnector.createPlacaArrayFromCSV(filePath);

                        if (placaArray != null) {
                            // Mostrar un mensaje de éxito y las primeras 3 placas creadas
                            JOptionPane.showMessageDialog(FileConverterGUI.this, "Placas created from the file:");
                            for (int i = 0; i < Math.min(3, placaArray.length); i++) {
                                System.out.println(placaArray[i]);
                            }

                            // Solicitar al usuario el nombre de este conjunto de Placas
                            String placasID = JOptionPane.showInputDialog(FileConverterGUI.this,
                                    "Enter the name of this set of Placas:");
                            if (placasID != null && !placasID.isEmpty()) { // Verificar si se ingresó un nombre válido
                                MongoDBConnector.addPlaca(placasID, placaArray);
                            } else {
                                JOptionPane.showMessageDialog(FileConverterGUI.this, "Invalid Placas name..");
                            }
                        } else {
                            JOptionPane.showMessageDialog(FileConverterGUI.this,
                                    "Error creating Placas from CSV file.");
                        }
                    } else {
                        // Mostrar un mensaje de error si el archivo seleccionado no es un archivo CSV
                        JOptionPane.showMessageDialog(FileConverterGUI.this, "The selected file is not a CSV file.");
                    }
                }
            }
        });

        placaToasqButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String placaID3 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the placa ID:");
                String asqContent = MongoDBConnector.placaArrayToASQ(placaID3);
                if (asqContent == null) {
                    System.out.println("Placa with ID '" + placaID3
                            + "' does not exist. Please enter a valid PlacaID.");
                } else {
                    String outputFileName3 = JOptionPane.showInputDialog(FileConverterGUI.this,
                            "Enter the name of the output file:");
                    writeToFile(asqContent, outputFileName3, ".asq");
                }
            }
        });

        placaTocsvButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String placaID4 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the placa ID:");
                String asqContent = MongoDBConnector.placaArrayToCSV(placaID4);
                if (asqContent == null) {
                    System.out.println("Placa with ID '" + placaID4
                            + "' does not exist. Please enter a valid PlacaID.");
                } else {
                    String outputFileName4 = JOptionPane.showInputDialog(FileConverterGUI.this,
                            "Enter the name of the output file");
                    writeToFile(asqContent, outputFileName4, ".csv");
                }
            }
        });

        choosecenterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String placaID5 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the placa ID:");
                String elementID5 = JOptionPane.showInputDialog(FileConverterGUI.this,
                        "Enter the ElementID of the new center:");
                if (MongoDBConnector.chooseCenter(placaID5, elementID5)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "Successfully chosen center.");
                } else {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "Could not choose the center. Please review your entries.");
                }
            }
        });

        flipcomponentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String placasID6 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the placa ID:");
                boolean flipSuccess = MongoDBConnector.flipComponents(placasID6);
                if (flipSuccess) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "The components were flipped successfully.");
                } else {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "Components could not be flipped.");
                }
            }
        });

        updateTableData();
    }

    private static void writeToFile(String content, String fileName, String fileExtension) {
        // Crear un cuadro de diálogo para que el usuario seleccione el directorio de
        // destino
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccione el directorio de destino");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(null);

        // Verificar si el usuario seleccionó un directorio
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // Obtener el directorio seleccionado por el usuario
            File selectedDirectory = fileChooser.getSelectedFile();

            // Construir la ruta completa del archivo en el directorio seleccionado
            String filePath = selectedDirectory.getAbsolutePath() + File.separator + fileName + fileExtension;

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
                System.out.println(
                        "Archivo '" + fileName + "' creado exitosamente en " + selectedDirectory.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error al escribir el archivo: " + e.getMessage());
            }
        } else {
            System.out.println("Operación cancelada por el usuario.");
        }
    }

    // Method to update the table with data from MongoDB
    private void updateTableData() {
        // Retrieve data from MongoDB and populate a list of placas
        List<Document> placasList = MongoDBConnector.getAllPlacas();

        // Define table columns
        String[] columnNames = { "Placa ID", "Size" };

        // Prepare data for the table
        Object[][] rowData = new Object[placasList.size()][columnNames.length];
        for (int i = 0; i < placasList.size(); i++) {
            Document placaDoc = placasList.get(i);
            rowData[i][0] = placaDoc.getString("placaID");
            rowData[i][1] = placaDoc.getInteger("size");
        }

        // Set up the table model and update the table
        DefaultTableModel model = new DefaultTableModel(rowData, columnNames);
        table.setModel(model);
    }

    private void displayPlacaElements(String placaID) {
        // Retrieve the elements of the Placa with the given ID
        List<Document> elements = MongoDBConnector.getPlacaElements(placaID);

        // Create a table model with column names
        String[] columnNames = { "ID", "Type", "Outline", "PosX", "PosY", "Rotation", "Flip" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        // Populate the table model with Placa elements data
        for (Document element : elements) {
            // Exclude "files" from displaying
            String id = element.getString("ID");
            String type = element.getString("type");
            String outline = element.getString("outline");
            Double posX = element.getDouble("posX");
            Double posY = element.getDouble("posY");
            Double rotationDouble = element.getDouble("rotation");
            Integer rotation = rotationDouble != null ? rotationDouble.intValue() : null;
            Boolean flip = element.getBoolean("flip");

            // Add the row to the table model
            model.addRow(new Object[] { id, type, outline, posX, posY, rotation, flip });
        }

        // Create a table with the populated model
        JTable table = new JTable(model);

        // Set up scroll pane for the table
        JScrollPane scrollPane = new JScrollPane(table);

        // Show the table in a dialog
        JOptionPane.showMessageDialog(this, scrollPane, "Placa Elements", JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileConverterGUI gui = new FileConverterGUI();
            gui.setVisible(true);
        });
    }
}