import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import org.bson.Document;

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
        super("PCB converter");

        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Creating and initializing buttons with specific font properties
        Font buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        txtToPlacaButton = new JButton("Upload from .txt");
        txtToPlacaButton.setFont(buttonFont);
        csvToPlacaButton = new JButton("Upload from .csv");
        csvToPlacaButton.setFont(buttonFont);
        placaToasqButton = new JButton("Export to .asq");
        placaToasqButton.setFont(buttonFont);
        placaTocsvButton = new JButton("Export to .csv");
        placaTocsvButton.setFont(buttonFont);
        choosecenterButton = new JButton("Center components");
        choosecenterButton.setFont(buttonFont);
        flipcomponentsButton = new JButton("Flip components");
        flipcomponentsButton.setFont(buttonFont);
        reloadDatabaseButton = new JButton("Reload");
        reloadDatabaseButton.setFont(buttonFont);

        // Creating panel to hold buttons
        JPanel panel = new JPanel();
        panel.add(txtToPlacaButton);
        panel.add(csvToPlacaButton);
        panel.add(placaToasqButton);
        panel.add(placaTocsvButton);
        panel.add(choosecenterButton);
        panel.add(flipcomponentsButton);
        panel.add(reloadDatabaseButton);
        panel.setBorder(new EmptyBorder(0, 100, 120, 100));
        add(panel, BorderLayout.NORTH);

        // Creating and initializing table with specific font properties
        Font tableFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        table = new JTable();
        table.setPreferredScrollableViewportSize(new Dimension(300, table.getRowHeight()));
        table.setFillsViewportHeight(true);
        table.setFont(tableFont);

        JTableHeader tableHeader = table.getTableHeader();
        Font headerFont = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        tableHeader.setFont(headerFont);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Adding action listeners to buttons
        reloadDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTableData();
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

        // Action listener for the "Upload from .txt" button
        txtToPlacaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // File chooser for selecting .txt file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select a TXT file");

                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text files", "txt");
                fileChooser.setFileFilter(filter);

                int userSelection = fileChooser.showOpenDialog(FileConverterGUI.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String fileName = selectedFile.getAbsolutePath();

                    // Creating Placa array from TXT file
                    placaArray = MongoDBConnector.createPlacaArrayFromTXT(fileName);

                    if (placaArray != null) {
                        String placasID = JOptionPane.showInputDialog(FileConverterGUI.this,
                                "Enter the name for the PCB:");
                        if (placasID != null && !placasID.isEmpty()) {
                            // Adding Placa to MongoDB
                            MongoDBConnector.addPlaca(placasID, placaArray);
                        } else {
                            JOptionPane.showMessageDialog(FileConverterGUI.this, "ERROR: Invalid name");
                            return; // Stop further execution
                        }
                    } else {
                        JOptionPane.showMessageDialog(FileConverterGUI.this,
                                "Error uploading information from TXT file.");
                    }
                }
            }
        });

        // Action listener for the "Upload from .csv" button
        csvToPlacaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // File chooser for selecting .csv file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select a CSV file");

                FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
                fileChooser.setFileFilter(filter);

                int userSelection = fileChooser.showOpenDialog(FileConverterGUI.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();

                    // Creating Placa array from CSV file
                    placaArray = MongoDBConnector.createPlacaArrayFromCSV(filePath);

                    if (placaArray != null) {
                        String placasID = JOptionPane.showInputDialog(FileConverterGUI.this,
                                "Enter the name for the PCB:");
                        if (placasID != null && !placasID.isEmpty()) {
                            // Adding Placa to MongoDB
                            MongoDBConnector.addPlaca(placasID, placaArray);
                        } else {
                            JOptionPane.showMessageDialog(FileConverterGUI.this, "ERROR: Invalid name");
                            return; // Stop further execution
                        }
                    } else {
                        JOptionPane.showMessageDialog(FileConverterGUI.this,
                                "Error uploading information from CSV file.");
                    }
                }
            }
        });

        // Action listener for the "Export to .asq" button
        placaToasqButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Prompt user to enter PCB name
                String placaID3 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the PCB name:");

                // Check if PCB exists
                if (!MongoDBConnector.placaExists(placaID3)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "ERROR: PCB with name '" + placaID3 + "' does not exist.");
                    return;
                }

                // Convert Placa data to ASQ format
                String asqContent = MongoDBConnector.placaArrayToASQ(placaID3);
                String outputFileName3 = JOptionPane.showInputDialog(FileConverterGUI.this,
                        "Enter the name of the output file (without extension):");
                writeToFile(asqContent, outputFileName3, ".asq");
            }
        });

        // Action listener for the "Export to .csv" button
        placaTocsvButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Prompt user to enter PCB name
                String placaID4 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the PCB name:");

                // Check if PCB exists
                if (!MongoDBConnector.placaExists(placaID4)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "ERROR: PCB with name '" + placaID4 + "' does not exist.");
                    return;
                }

                // Convert Placa data to CSV format
                String asqContent = MongoDBConnector.placaArrayToCSV(placaID4);
                String outputFileName4 = JOptionPane.showInputDialog(FileConverterGUI.this,
                        "Enter the name of the output file (without extension):");
                writeToFile(asqContent, outputFileName4, ".csv");
            }
        });

        // Action listener for the "Center components" button
        choosecenterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Prompt user to enter PCB name and element ID
                String placaID5 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the PCB name:");
                String elementID5 = JOptionPane.showInputDialog(FileConverterGUI.this,
                        "Enter the ComponentID of the new center:");

                // Check if PCB exists
                if (!MongoDBConnector.placaExists(placaID5)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "ERROR: PCB with name '" + placaID5 + "' does not exist.");
                    return;
                }

                // Choose new center for the PCB
                if (MongoDBConnector.chooseCenter(placaID5, elementID5)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "New center chosen successfully");
                } else {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "ERROR: Could not find the component. Please, use a valid ID.");
                }
            }
        });

        // Action listener for the "Flip components" button
        flipcomponentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Prompt user to enter PCB name
                String placasID6 = JOptionPane.showInputDialog(FileConverterGUI.this, "Enter the PCB name:");

                // Check if PCB exists
                if (!MongoDBConnector.placaExists(placasID6)) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this,
                            "ERROR: PCB with name '" + placasID6 + "' does not exist.");
                    return;
                }

                // Flip components of the PCB
                boolean flipSuccess = MongoDBConnector.flipComponents(placasID6);
                if (flipSuccess) {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "The components were flipped successfully.");
                } else {
                    JOptionPane.showMessageDialog(FileConverterGUI.this, "ERROR: Components could not be flipped.");
                }
            }
        });

        updateTableData();
    }

    // Method to write content to a file
    private static void writeToFile(String content, String fileName, String fileExtension) {
        // File chooser for selecting destination folder
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose the destination folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();

            String filePath = selectedDirectory.getAbsolutePath() + File.separator + fileName + fileExtension;

            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
                System.out.println(
                        "File '" + fileName + "' successfully created at " + selectedDirectory.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error saving the file: " + e.getMessage());
            }
        } else {
            System.out.println("Operation cancelled.");
        }
    }

    // Method to update table data from MongoDB
    private void updateTableData() {
        List<Document> placasList = MongoDBConnector.getAllPlacas();

        String[] columnNames = { "PCB name (ID)", "Number of components" };

        Object[][] rowData = new Object[placasList.size()][columnNames.length];
        for (int i = 0; i < placasList.size(); i++) {
            Document placaDoc = placasList.get(i);
            rowData[i][0] = placaDoc.getString("placaID");
            rowData[i][1] = placaDoc.getInteger("size");
        }

        DefaultTableModel model = new DefaultTableModel(rowData, columnNames);
        table.setModel(model);
    }

    // Method to display Placa elements in a dialog
    private void displayPlacaElements(String placaID) {
        List<Document> elements = MongoDBConnector.getPlacaElements(placaID);

        String[] columnNames = { "ID", "Type", "Outline", "PosX", "PosY", "Rotation", "Flip" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        for (Document element : elements) {
            String id = element.getString("ID");
            String type = element.getString("type");
            String outline = element.getString("outline");
            Double posX = element.getDouble("posX");
            Double posY = element.getDouble("posY");
            Double rotationDouble = element.getDouble("rotation");
            Integer rotation = rotationDouble != null ? rotationDouble.intValue() : null;
            Boolean flip = element.getBoolean("flip");

            model.addRow(new Object[] { id, type, outline, posX, posY, rotation, flip });
        }

        JTable table = new JTable(model);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(1).setPreferredWidth(200);
        columnModel.getColumn(2).setPreferredWidth(200);

        Font tableFont = table.getFont();
        table.setFont(new Font(tableFont.getName(), Font.PLAIN, 14));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setVerticalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, renderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, Math.min(table.getRowHeight() * (elements.size() + 1), 600)));

        JOptionPane.showMessageDialog(this, scrollPane, "PCB Components", JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileConverterGUI gui = new FileConverterGUI();
            gui.setVisible(true);
        });
    }
}