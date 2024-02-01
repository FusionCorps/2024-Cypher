//TODO (long-term restructuring) EFFICIENCY/READABILITY/EASIER TO MODIFY (for new games): how to encapsulate/declare data fields in more efficient way (e.g. maybe hashmap for each field, like [Object:fx_id]?)

package org.cypher6672;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.Rating;
import org.cypher6672.ui.AlertBox;
import org.cypher6672.ui.LimitedTextField;
import org.cypher6672.ui.SceneSizeChangeListener;
import org.cypher6672.util.CopyImageToClipBoard;
import org.cypher6672.util.QRFuncs;

import javax.imageio.ImageIO;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

public class FXMLController {
    /**
     * scene0:begin
     * scene1:pregame
     * scene2:auton
     * scene3:teleop
     * scene4:endgame
     * scene5:qualitative notes
     * scene6:QR CODE
     */

    private static LinkedHashMap<String, String> info = new LinkedHashMap<>(); //stores user input data
    private static HashMap<String, Integer> toggleMap = new HashMap<>() {{
        putIfAbsent("driveStation", null);
        putIfAbsent("drivetrainType", null);
    }}; //stores toggle group values

    private static int sceneIndex = 0;  //used for changing pages
    private static StringBuilder data = new StringBuilder(); //used to build data output string in sendInfo()
    private static boolean isNextPageClicked = false; //used in initialize() to prevent data from being sent to info HashMap before user clicks next page
    private static String autonColor = "R"; //for changing color in auton pickup grid
    private static boolean PNGflipped = false; //for flipping starting location image
    private static String prevMatchNum = "1"; //stores current matchNum, increments on reset
    //TODO: save scouter name previously inputted,
    // maybe save list of previous scouter names in file, and use dropdown box

    //======================FXML DATA FIELDS======================
    //data for each page, variables are named the same as corresponding fx:ids in fxml files for consistency

    //page 1 - pregame
    @FXML private LimitedTextField teamNum; //team number
    @FXML private LimitedTextField matchNum; //match number
    @FXML private Text teamNameText; //displays team name based on team number
    @FXML private ToggleGroup driveStation;
    @FXML private CheckBox preload;
    //page 2
    //page 3
    //page 4
    //page 5


    private BufferedImage qrImage;
    @FXML private ImageView startLocationPNG; //starting location image

    //=============================METHODS FOR CONTROLLING APP LOGIC=============================
    //runs at loading of any scene, defaults null values and reloads previously entered data
    public void initialize() {
        if (sceneIndex == 1) {
            //handles team name display
            teamNum.setOnKeyTyped(event -> {
                try {
                    BufferedReader csvReader = new BufferedReader(new InputStreamReader(
                            this.getClass().getResourceAsStream("teamList.csv")));
                    String line;
                    while ((line = csvReader.readLine()) != null) {
                        String[] pair = line.split(",");
                        if (pair[0].equals(teamNum.getText())) {
                            teamNameText.setText("You are scouting: " + pair[1]);
                            break;
                        } else teamNameText.setText("This team isn't in the team database.");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        //TODO: default null values based on page number
        if (isNextPageClicked) {
            switch (sceneIndex) {
                case 1 -> {
                    if (matchNum.getText().isEmpty()) matchNum.setText(prevMatchNum);
                }
                case 2 -> {
                }
                case 3 -> {
                }
                default -> {
                }
            }
            reloadData();
        }
    }

    /**
     * <p> {@code resetAll} - resets all forms of data storage and goes to first page
     * <p> {@code nextPage} - goes to next page
     * <p> {@code prevPage} - goes to previous page
     * <p> {@code setPage} - general function for setting page number
     */
    //implementations of setPage() for going to next and previous pages
    @FXML private void resetAll(ActionEvent event) throws IOException {
        //TODO: set default match number

        // reset data storage variables
        data = new StringBuilder();
        info = new LinkedHashMap<>();

        // resets UI to scene1
        sceneIndex = 0;
        nextPage(event);
    }
    @FXML private void nextPage(ActionEvent event) throws IOException {
        collectData();
        isNextPageClicked = true;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setPage(stage, ++sceneIndex);
    }
    @FXML private void prevPage(ActionEvent event) throws IOException {
        //collects data from current page and goes to previous page
        collectData();
        if (sceneIndex > 0) sceneIndex--;
        isNextPageClicked = false;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setPage(stage, sceneIndex);
    }

    //changes page to the scene specified by sceneIndex
    public static void setPage(Stage stage, int page) throws IOException {
        sceneIndex = page;
        FXMLLoader loader = new FXMLLoader(FXMLController.class.getResource("scenes/scene" + (sceneIndex) + ".fxml"));
        //if next line causes errors, check syntax in all fxml files
        Scene scene = new Scene(loader.load());

        stage.setTitle("6672 Cypher Page " + (sceneIndex));
        stage.setScene(scene);
        stage.show();

        letterbox(scene, (Pane) scene.getRoot());
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
    }

    /**
     * <p> {@code sendInfo} - formats and creates QR code for data, calls outputAll
     * <p> {@code collectData} - saves data entered on a page
     * <p> {@code reloadData} - reloads data previously entered when reentering a page
     * <p> {@code validateInput} - checks for certain data fields (types of characters allowed)
     * <p> {@code requiredFieldsAreOK} - displays alert boxes for unfulfilled required fields
     */

    //sends data to QR code creator and displays it on screen
    @FXML private void sendInfo() throws Exception {
        if (requiredFieldsAreOK()) {
            data = new StringBuilder();

            //run certain checks to correctly format data; boolean checks,
            // and remove pieces scored in auton from being recorded in teleop

            //output string appended to data StringBuilder
            for (String keyName : info.keySet()) {
                //get embedded alliance value from driveStation
                if (keyName.equals("driveStation"))
                    data.append("alliance=").append(info.get("driveStation").charAt(0)).append("|");
                data.append(keyName).append("=").append(info.get(keyName)).append("|");
            }

            data = data.delete(data.lastIndexOf("|"), data.length());

            //creates QR code and displays it on screen, runs outputAll() to save all data
            qrImage = QRFuncs.generateQRCode(data.toString(), "qrcode.png");
            outputAll(Integer.parseInt(info.get("matchNum")),
                    Integer.parseInt(info.get("teamNum")),
                    info.get("scoutName"));
        }
    }

    //IMPORTANT: ALL collected data elements must be added to the info HashMap in this method,
    // with a SPECIFIC ORDER so they can be correctly parsed
    private void collectData() {
        switch (sceneIndex) {
            // pregame
            case 1 -> {
                collectDataTextField(teamNum, "teamNum");
                collectDataTextField(matchNum, "matchNum");
                collectDataToggleGroup(driveStation, "driveStation");
                //TODO: check if we need alliance/startLocation fields
                collectDataCheckBox(preload, "preload");
            }
            case 2 -> {

            }
            case 3 -> {

            }
        }
    }

    //reloads data for a scene, called when loading scene in initialize() method
    private void reloadData() {
        switch (sceneIndex) {
            //TODO: add cases for each page
            case 1 -> {
                reloadDataTextField(teamNum, "teamNum");
                reloadDataTextField(matchNum, "matchNum");
                reloadDataToggleGroup(driveStation, "driveStation");
                reloadDataCheckBox(preload, "preload");
            }
            case 2 -> {
            }
            case 3 -> {
            }
        }
    }

    //puts restrictions on certain LimitedTextFields
    @FXML private void validateInput(KeyEvent keyEvent) {
        Object src = keyEvent.getSource(); // element that got input
        //TODO: add more restrictions
        if (src.equals(teamNum)) {
            teamNum.setIntegerField();
            teamNum.setMaxLength(5);
        } else if (src.equals(matchNum)) {
            matchNum.setIntegerField();
            matchNum.setMaxLength(3);
        }
    }

    //validation for required fields
    private boolean requiredFieldsAreOK() {
        String warnings = "";
        //TODO: add more warnings based on issues with info

        if (info.get("teamNum").isBlank() || info.get("teamNum").matches("0+"))
            warnings += "Fix the team number (cannot contain only 0s or be blank). ";
        if (info.get("matchNum").isBlank() || info.get("matchNum").matches("0+"))
            warnings += "Fix the match number (cannot contain only 0s or be blank). ";

        System.out.println(warnings); // for debug purposes
        if (warnings.isBlank()) return true;
        else {
            AlertBox.display("Bad inputs", warnings);
            return false;
        }
    }

    /**
     * <p> {@code outputAll) - central function for outputting and saving data
     * <p> {@code writeToCSV} - writes data to CSV on computer
     * <p> {@code copyToClipBoard} - copies data to clipboard, mainly for debugging
     */

    //saves output to QR Codes and text files on computer, copies in Desktop/Scouting and Documents/backupScouting of active user
    private void outputAll(int matchNum, int teamNum, String scoutName) {
        String outputPath = "C:\\Users\\" + System.getProperty("user.name") + "\\Desktop\\Scouting";
        String qrCodePath = outputPath + "\\qrcodes";
        String txtPath = outputPath + "\\texts";
        String matchDataPath = outputPath + "\\matchData.csv";

        //FORMAT: Q[match number]-[team number]-[scouter name]
        String dataName = "Q"  + matchNum + "-"  + teamNum + "-" + scoutName;

        try {
            //creates Desktop directories (if they don't exist) to store text and QR code files
            new File(qrCodePath).mkdirs();
            new File(txtPath).mkdirs();
            new File(matchDataPath).createNewFile();

            //writes text file/qr code/CSV
            FileWriter writer = new FileWriter(txtPath + "\\" + dataName + ".txt");
            writer.write(data.toString());
            writer.close();
            ImageIO.write(qrImage, "png",
                    new File(qrCodePath + "\\" + dataName + ".png"));

            writeToCSV(info, matchDataPath);
        }
        catch (Exception e) {
            System.out.println("file not found");
        }

    }

    //helper function for outputAll() method, writes data to CSV file
    private void writeToCSV(LinkedHashMap<String, String> data, String outputCSVPath) throws IOException {
        File file = new File(outputCSVPath);
        FileWriter writer = new FileWriter(file, true);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        var keys = data.keySet();
        // if file is empty, write headers
        if (reader.lines().findAny().isEmpty()) {
            StringBuilder headers = new StringBuilder();
            for (int i = 0; i < keys.size(); i++)
                headers.append((keys.toArray())[i]).append(",");
            headers.deleteCharAt(headers.length() - 1); //remove last comma

            writer.write(headers + "\n");
        }

        //write data
        StringBuilder values = new StringBuilder();
        for (int j = 0; j < keys.size(); j++)
            values.append(data.get((keys.toArray())[j])).append(",");
        values.deleteCharAt(values.length() - 1); //remove last comma

        //if data is already in CSV, don't write it again
        //this is relevant if user tries to resubmit identical data
        if (reader.lines().noneMatch(line -> line.contains(values.toString()))) {
            writer.write(values + "\n");
        }

        writer.flush();
        writer.close();
        reader.close();
        }

    //copies either data text or QR code based on button source that was clicked, mainly emergency/debug methods
    @FXML private void copyToClipboard(ActionEvent event) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Button src = (Button) event.getSource();
        if (event.getSource().getClass().equals(Button.class)) {
            if ((src.getText().contains("Text")))
                clipboard.setContents(new StringSelection(data.toString()), null);
            else if (src.getText().contains("QR Code"))
                new CopyImageToClipBoard().copyImage(qrImage);
        }
    }

    //used in collectData() for specific types of data
    private void collectDataCheckBox(CheckBox checkBox, String key) {
        info.put(key, String.valueOf(checkBox.isSelected()));
    }
    private void collectDataTextField(LimitedTextField textField, String key) {info.put(key, textField.getText());}
    private void collectDataArray(ArrayList<Integer> array, String key) {
        info.put(key, array.toString());
    }
    private void collectDataRating(Rating rating, String key) {
        info.put(key, String.valueOf((int) rating.getRating()));
    }
    private void collectDataTextArea(TextArea textArea, String key) {
        info.put(key, textArea.getText());
    }
    private void collectDataToggleGroup(ToggleGroup toggleGroup, String key) {
        if (toggleGroup.getSelectedToggle() == null) return;
        Toggle selectedToggle = toggleGroup.getSelectedToggle();
        int index = toggleGroup.getToggles().indexOf(selectedToggle);
        String value = selectedToggle.getUserData().toString();
        info.put(key, value);
        toggleMap.put(key, index);
    }
    private void collectDataComboBox(ComboBox<String> comboBox, String key) {
        info.put(key, comboBox.getValue());
    }

    //used in reloadData() for specific types of data
    private void reloadDataCheckBox(CheckBox checkBox, String key) {
        checkBox.setSelected(Boolean.parseBoolean(info.get(key)));
    }
    private void reloadDataTextField(LimitedTextField textField, String key) {
        if (info.get(key) != null) textField.setText(info.get(key));
    }
    private void reloadDataRating(Rating rating, String key) {
        if (info.get(key) != null) rating.setRating(Double.parseDouble(info.get(key)));
    }
    private void reloadDataTextArea(TextArea textArea, String key) {
        textArea.setText(info.get(key));
    }
    private void reloadDataToggleGroup(ToggleGroup toggleGroup, String key) {
        if (toggleMap.get(key) != null) toggleGroup.selectToggle(toggleGroup.getToggles().get(toggleMap.get(key)));
    }
    private void reloadDataComboBox(ComboBox<String> comboBox, String key) {
        comboBox.setValue(info.get(key));
    }

    //displays confirmation popup before resetting app
    @FXML private void confirmReset(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset");
        alert.setHeaderText("Are you sure you want to reset the app?");
        alert.setContentText("This will clear all data and return to the start page. This cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) resetAll(event);
    }

    private static void letterbox(final Scene scene, final Pane contentPane) {
        final double initWidth  = scene.getWidth();
        final double initHeight = scene.getHeight();
        final double ratio      = initWidth / initHeight;

        SceneSizeChangeListener sizeListener = new SceneSizeChangeListener(scene, ratio, initHeight, initWidth, contentPane);
        scene.widthProperty().addListener(sizeListener);
        scene.heightProperty().addListener(sizeListener);
    }

    //flips pregame start location image
    @FXML private void flipImage(ActionEvent ignoredEvent) {
        if (PNGflipped) {
            startLocationPNG.setImage(new Image(getClass().getResource("images/2024-field.png").toString()));
            PNGflipped = false;
        } else {
            PNGflipped = true;
            startLocationPNG.setImage(new Image(getClass().getResource("images/start_locs_flipped.png").toString()));
        }
    }
}