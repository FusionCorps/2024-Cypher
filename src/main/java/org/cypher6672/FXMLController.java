package org.cypher6672;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.Rating;
import org.cypher6672.ui.*;
import org.cypher6672.util.CopyImageToClipBoard;
import org.cypher6672.util.QRFuncs;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.*;

public class FXMLController {
    public enum Page {
        BEGIN, PREGAME, AUTON, TELEOP, ENDGAME, QUALITATIVE_NOTES, QR_CODE
    }

    private static Page currPage = Page.BEGIN;
    private static LinkedHashMap<String, String> info = new LinkedHashMap<>(); //stores user input data
    private static HashMap<String, Integer> toggleMap = new HashMap<>() {{
        putIfAbsent("driveStation", null);
        putIfAbsent("startLocation", null);
        putIfAbsent("climbPartners", null);
    }}; //stores toggle group values

    public static String eventCode = "CURIE"; // FW, DAL, TXCMP1 (mercury), CURIE

    private static StringBuilder data = new StringBuilder(); //used to build data output string in sendInfo()
    private static boolean startLocationImageFlipped = false; //for flipping starting location image
    private static char autonPickupGridColor = 'r'; //for flipping auton pickup grid
    private static boolean autonPickupGridFlipped = false; //for flipping auton pickup grid
    private static String prevMatchNum = "1"; //stores current matchNum, increments on reset
    private static String prevScouterName = null;
    private static Integer prevDriveStation = null;
    private static boolean preloadSelected = true;

    //======================FXML DATA FIELDS======================
    //data for each page, variables are named the same as corresponding fx:ids in fxml files for consistency

    //page 1 - pregame
    @FXML private LimitedTextField teamNum; //team number
    @FXML private LimitedTextField matchNum; //match number
    @FXML private Text teamNameText; //displays team name based on team number
    @FXML private ToggleGroup driveStation;
    @FXML private ToggleGroup startLocation;
    @FXML private CheckBox preload;
    //page 2
    @FXML private CheckBox mobility;
    public static ArrayList<Integer> autonPickups = new ArrayList<>();
    @FXML private GridPane autoPickupGridBlue;
    @FXML private GridPane autoPickupGridRed;
    @FXML private Tally autoAmp;
    @FXML private Tally autoSpeakerClose;
    @FXML private Tally autoSpeakerMid;
    //page 3
    @FXML private PlusMinusBox friendlyPickups;
    @FXML private PlusMinusBox neutralPickups;
    @FXML private PlusMinusBox oppPickups;
    @FXML private PlusMinusBox sourcePickups;
    @FXML private Tally teleopSpeakerClose;
    @FXML private Tally teleopSpeakerMid;
    @FXML private Tally teleopSpeakerFar;
    @FXML private Tally teleopAmp;
    //page 4
    @FXML private Tally teleopTrap;
    @FXML private CheckBox climb;
    @FXML private LimitedTextField climbTime;
    @FXML private ToggleGroup climbPartners;
    @FXML private CheckBox spotlight;
    //page 5
    @FXML private CheckBox shuttle;
    @FXML private Rating shooter;
    @FXML private Rating intake;
    @FXML private Rating speed;
    @FXML private Rating driver;
    @FXML private LimitedTextField scoutName;
    @FXML private TextArea comments;

    //page 6
    @FXML private ImageView imageBox;
    @FXML private Text reminderBox;

    private BufferedImage qrImage;
    @FXML private ImageView startLocationPNG; //starting location image
    @FXML private ImageView autoPickupPNG; //auton pickup grid image

    static Stage stage;

    // resource paths
    private static final String blueField = FXMLController.class.getResource("images/2024-field-blue.png").toString();
    private static final String redField = FXMLController.class.getResource("images/2024-field-red.png").toString();
    private static final String blueFieldReversed = FXMLController.class.getResource("images/2024-field-reversed-blue.png").toString();
    private static final String redFieldReversed = FXMLController.class.getResource("images/2024-field-reversed-red.png").toString();
    private static final String blueAutoPickup = FXMLController.class.getResource("images/autoPickupBlue.png").toString();
    private static final String redAutoPickup = FXMLController.class.getResource("images/autoPickupRed.png").toString();
    private static final String blueAutoPickupReversed = FXMLController.class.getResource("images/autoPickupBlueReversed.png").toString();
    private static final String redAutoPickupReversed = FXMLController.class.getResource("images/autoPickupRedReversed.png").toString();
    private static final String outputPath = "C:\\Users\\" + System.getProperty("user.name") + "\\Desktop\\Scouting" + eventCode;
    private static final String qrCodePath = outputPath + "\\qrcodes" + eventCode;
    private static final String txtPath = outputPath + "\\texts";
    private static final String matchDataPath = outputPath + "\\matchData" + eventCode + ".csv";
    private static final String schedulePath = outputPath + "\\schedule.csv";

    List<int[]> schedule;

    // general things to run when a page is loaded, regardless of forward or backward
    private void initGeneral() {
        switch (currPage) {
            case PREGAME -> {
                createSchedule();
                startLocationImageFlipped = autonPickupGridFlipped; // sync start location flip with auton pickup grid flip

                teamNum.textProperty().addListener((observable, oldValue, newValue) -> {
                    try {
                        BufferedReader csvReader = new BufferedReader(new InputStreamReader(
                                Objects.requireNonNull(this.getClass().getResourceAsStream("teamList.csv"))));
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

                // default driveStation based on its input in previous matches
                if (prevDriveStation != null) {
                    driveStation.selectToggle(driveStation.getToggles().get(prevDriveStation));
                }

                // display correct startLocation image based on flip and color (from driveStation)
                if (((String) driveStation.getSelectedToggle().getUserData()).charAt(0) == 'b') {
                    if (startLocationImageFlipped) {
                        startLocationPNG.setImage(new Image(blueFieldReversed));
                    } else {
                        startLocationPNG.setImage(new Image(blueField));
                    }
                } else {
                    if (startLocationImageFlipped) {
                        startLocationPNG.setImage(new Image(redFieldReversed));
                    } else {
                        startLocationPNG.setImage(new Image(redField));
                    }
                }

                // add listener to driveStation to update startLocation img
                driveStation.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        char alliance = newValue.getUserData().toString().charAt(0);
                        if (alliance == 'b') {
                            if (startLocationImageFlipped) {
                                startLocationPNG.setImage(new Image(blueFieldReversed));
                            } else {
                                startLocationPNG.setImage(new Image(blueField));
                            }
                        } else {
                            if (startLocationImageFlipped) {
                                startLocationPNG.setImage(new Image(redFieldReversed));
                            } else {
                                startLocationPNG.setImage(new Image(redField));
                            }
                        }
                    }
                });

                preload.selectedProperty().addListener((observable, oldValue, selected) -> preloadSelected = selected);


                driveStation.selectedToggleProperty().addListener(((observableValue, toggle, t1) -> dynamicUpdateTeamNum()));

                matchNum.textProperty().addListener((observable, oldValue, newValue) ->  {
                    if (!newValue.isBlank()) {
                        dynamicUpdateTeamNum();
                    }
                });
            }
            case AUTON -> {
                autonPickupGridFlipped = startLocationImageFlipped; // sync auton pickup grid flip with start location flip

                // show correct auton pickup PNG and grid based on alliance
                if (autonPickupGridColor == 'b') {
                    autoPickupGridBlue.setVisible(true);
                    autoPickupGridRed.setVisible(false);
                    autoPickupGridBlue.setDisable(false);
                    autoPickupGridRed.setDisable(true);
                    if (autonPickupGridFlipped) {
                        autoPickupPNG.setImage(new Image(blueAutoPickupReversed));
                        autoPickupGridBlue.rotateProperty().set(180);
                        autoPickupGridBlue.translateXProperty().set(-110);
                    } else {
                        autoPickupPNG.setImage(new Image(blueAutoPickup));
                        autoPickupGridBlue.rotateProperty().set(0);
                        autoPickupGridBlue.translateXProperty().set(0);
                    }
                } else {
                    autoPickupGridBlue.setVisible(false);
                    autoPickupGridRed.setVisible(true);
                    autoPickupGridBlue.setDisable(true);
                    autoPickupGridRed.setDisable(false);
                    if (autonPickupGridFlipped) {
                        autoPickupPNG.setImage(new Image(redAutoPickupReversed));
                        autoPickupGridRed.rotateProperty().set(180);
                        autoPickupGridRed.translateXProperty().set(110);
                    }
                    else {
                        autoPickupPNG.setImage(new Image(redAutoPickup));
                        autoPickupGridRed.rotateProperty().set(0);
                        autoPickupGridRed.translateXProperty().set(0);
                    }
                }
            }
            case ENDGAME -> {
                if (!climb.isSelected()) defaultDisableEndgameFields();

                // enable climb time estimate, partner selection, and spotlight if climb is checked
                climb.selectedProperty().addListener((observable, oldValue, selected) -> {
                    if (selected) {
                        climbTime.setDisable(false);
                        spotlight.setDisable(false);
                        ((RadioButton) climbPartners.getToggles().get(1)).setDisable(false);
                        ((RadioButton) climbPartners.getToggles().get(2)).setDisable(false);
                    } else {
                        spotlight.setSelected(false);
                        spotlight.setDisable(true);
                        climbTime.setText("0");
                        climbTime.setDisable(true);
                        climbPartners.selectToggle(climbPartners.getToggles().get(0));
                        ((RadioButton) climbPartners.getToggles().get(1)).setDisable(true);
                        ((RadioButton) climbPartners.getToggles().get(2)).setDisable(true);
                    }
                });
            }
            case QR_CODE -> {
                try {
                    sendInfo();
                } catch (Exception e) {
                    System.err.println("Error sending info to QR code");
                }
            }
        }
    }

    private void dynamicUpdateTeamNum() {
        int[] match = schedule.stream()
                .filter(row -> row[0] == Integer.parseInt(matchNum.getText()))
                .findFirst()
                .orElse(new int[7]);

        int column = switch (driveStation.getSelectedToggle().getUserData().toString()) {
            case "r1" -> 1;
            case "r2" -> 2;
            case "r3" -> 3;
            case "b1" -> 4;
            case "b2" -> 5;
            case "b3" -> 6;
            default -> throw new IllegalStateException("Unexpected value: " + driveStation.getSelectedToggle().getUserData().toString());
        };

        teamNum.setText(String.valueOf(match[column]));
    }

    //=============================METHODS FOR CONTROLLING APP LOGIC=============================
    // runs at loading of any scene, defaults null values and reloads previously entered data
    public void initialize() {
        initGeneral();
        reloadData(); // reloads data previously entered when reentering a page
        setDefaults(); // defaults fields that are blank on the page
    }

    private void setDefaults() {
        // defaults fields that are blank
        switch (currPage) {
            case PREGAME -> {
                preload.setSelected(preloadSelected);
                if (matchNum.getText().isBlank()) matchNum.setText(prevMatchNum);
            }
            case AUTON -> {
                autoAmp.initNull();
                autoSpeakerClose.initNull();
                autoSpeakerMid.initNull();
            }
            case TELEOP -> {
                friendlyPickups.initNull();
                neutralPickups.initNull();
                oppPickups.initNull();
                sourcePickups.initNull();
                teleopSpeakerClose.initNull();
                teleopSpeakerMid.initNull();
                teleopSpeakerFar.initNull();
                teleopAmp.initNull();
            }
            case ENDGAME -> {
                teleopTrap.initNull();
                if (climbTime.getText().isBlank()) climbTime.setText("0");
            }
            case QUALITATIVE_NOTES -> {
                if (prevScouterName != null && scoutName.getText().isBlank()) scoutName.setText(prevScouterName);
            }
        }
    }
    private void defaultDisableEndgameFields() {
        spotlight.setDisable(true);
        climbTime.setDisable(true);

        climbTime.setText("0");
        climbTime.setDisable(true);
        ((RadioButton) climbPartners.getToggles().get(1)).setDisable(true);
        ((RadioButton) climbPartners.getToggles().get(2)).setDisable(true);
    }

    /**
     * <p> {@code sendInfo} - formats and creates QR code for data, calls outputAll
     * <p> {@code collectData} - saves data entered on a page
     * <p> {@code reloadData} - reloads data previously entered when reentering a page
     * <p> {@code validateInput} - checks for certain data fields (types of characters allowed)
     * <p> {@code requiredFieldsAreOK} - displays alert boxes for unfulfilled required fields
     */

    //sends data to QR code creator and displays it on screen
    @FXML private void sendInfo() {
        data = new StringBuilder(); //clears data for new data to be appended

        //output string appended to data StringBuilder
        // correctly format data for QR code output
        for (String keyName : info.keySet()) {
            //get embedded alliance + start location value from driveStation
            if (keyName.equals("driveStation")) {
                var driveStation = info.get("driveStation");
                char alliance = driveStation.charAt(0);
                char startLocation = driveStation.charAt(1);

                data.append("alliance=").append(alliance).append("|");
                data.append("driveStation=").append(alliance).append(startLocation).append("|");
            }
            else data.append(keyName).append("=").append(info.get(keyName)).append("|");
        }

        // prune last '|' character
        data = data.delete(data.lastIndexOf("|"), data.length());

        String createdQRPath = "qrCode.png";

        //creates QR code and displays it on screen, runs outputAll() to save all data
        try {
            qrImage = QRFuncs.generateQRCode(data.toString(), createdQRPath, 320, 320);

            Image img = new Image("file:" + createdQRPath);
            imageBox.setImage(img);

            outputAll(Integer.parseInt(info.get("matchNum")),
                    Integer.parseInt(info.get("teamNum")),
                    info.get("scoutName"));
        } catch (Exception e) {
            AlertBox.display("Generation Error", "There was an error creating the QR code. Please check all inputs and try again.");
        }
    }

    //IMPORTANT: ALL collected data elements must be added to the info HashMap in this method,
    // with a SPECIFIC ORDER, so they can be correctly parsed
    private void collectData() {
        switch (currPage) {
            // pregame
            case PREGAME -> {
                collectDataTextField(teamNum, "teamNum");
                collectDataTextField(matchNum, "matchNum");
                collectDataToggleGroup(driveStation, "driveStation");
                collectDataToggleGroup(startLocation, "startLocation");
                collectDataCheckBox(preload, "preload");

                autonPickupGridColor = info.get("driveStation").charAt(0);
            }
            case AUTON -> {
                collectDataCheckBox(mobility, "mobility");
                collectDataArray(autonPickups, "autonPickups");
                collectDataTally(autoAmp, "autoAmp", "autoAmpMisses", true);
                collectDataTally(autoSpeakerClose, "autoSpeakerClose", "autoSpeakerCloseMisses", true);
                collectDataTally(autoSpeakerMid, "autoSpeakerMid", "autoSpeakerMidMisses", true);
            }
            case TELEOP -> {
                collectDataPlusMinusBox(friendlyPickups, "friendlyPickups");
                collectDataPlusMinusBox(neutralPickups, "neutralPickups");
                collectDataPlusMinusBox(oppPickups, "oppPickups");
                collectDataPlusMinusBox(sourcePickups, "sourcePickups");
                collectDataTally(teleopSpeakerClose, "teleopSpeakerClose", "teleopSpeakerCloseMisses", true);
                collectDataTally(teleopSpeakerMid, "teleopSpeakerMid", "teleopSpeakerMidMisses", true);
                collectDataTally(teleopSpeakerFar, "teleopSpeakerFar", "teleopSpeakerFarMisses", true);
                collectDataTally(teleopAmp, "teleopAmp", "teleopAmpMisses", true);
            }
            case ENDGAME -> {
                collectDataTally(teleopTrap, "teleopTrap", "teleopTrapMisses", true);
                collectDataCheckBox(climb, "climb");
                collectDataTextField(climbTime, "climbTime");
                collectDataToggleGroup(climbPartners, "climbPartners");
                collectDataCheckBox(spotlight, "spotlight");
            }
            case QUALITATIVE_NOTES -> {
                collectDataCheckBox(shuttle, "shuttle");
                collectDataRating(shooter, "shooter");
                collectDataRating(intake, "intake");
                collectDataRating(speed, "speed");
                collectDataRating(driver, "driver");
                collectDataTextField(scoutName, "scoutName");
                collectDataTextArea(comments, "comments");
            }
        }
    }

    //reloads data for a scene, called when loading scene in initialize() method
    private void reloadData() {
        switch (currPage) {
            case PREGAME -> {
                reloadDataTextField(teamNum, "teamNum");
                reloadDataTextField(matchNum, "matchNum");
                reloadDataToggleGroup(driveStation, "driveStation");
                reloadDataToggleGroup(startLocation, "startLocation");
                reloadDataCheckBox(preload, "preload");
            }
            case AUTON -> {
                reloadDataCheckBox(mobility, "mobility");
                reloadDataTally(autoAmp, "autoAmp", "autoAmpMisses", true);
                reloadDataTally(autoSpeakerClose, "autoSpeakerClose", "autoSpeakerCloseMisses", true);
                reloadDataTally(autoSpeakerMid, "autoSpeakerMid", "autoSpeakerMidMisses", true);
                if (autonPickupGridColor == 'b') {
                    reloadAutonPickupGrid(autoPickupGridBlue);
                } else {
                    reloadAutonPickupGrid(autoPickupGridRed);
                }
            }
            case TELEOP -> {
                reloadDataPlusMinusBox(friendlyPickups, "friendlyPickups");
                reloadDataPlusMinusBox(neutralPickups, "neutralPickups");
                reloadDataPlusMinusBox(oppPickups, "oppPickups");
                reloadDataPlusMinusBox(sourcePickups, "sourcePickups");
                reloadDataTally(teleopSpeakerClose, "teleopSpeakerClose", "teleopSpeakerCloseMisses", true);
                reloadDataTally(teleopSpeakerMid, "teleopSpeakerMid", "teleopSpeakerMidMisses", true);
                reloadDataTally(teleopSpeakerFar, "teleopSpeakerFar", "teleopSpeakerFarMisses", true);
                reloadDataTally(teleopAmp, "teleopAmp", "teleopAmpMisses", true);
            }
            case ENDGAME -> {
                reloadDataTally(teleopTrap, "teleopTrap", "teleopTrapMisses", true);
                reloadDataCheckBox(climb, "climb");
                reloadDataTextField(climbTime, "climbTime");
                reloadDataToggleGroup(climbPartners, "climbPartners");
                reloadDataCheckBox(spotlight, "spotlight");
            }
            case QUALITATIVE_NOTES -> {
                reloadDataCheckBox(shuttle, "shuttle");
                reloadDataRating(shooter, "shooter");
                reloadDataRating(intake, "intake");
                reloadDataRating(speed, "speed");
                reloadDataRating(driver, "driver");
                reloadDataTextField(scoutName, "scoutName");
                reloadDataTextArea(comments, "comments");
            }
            case QR_CODE -> {
                if (info.get("teamNum") != null)
                    reminderBox.setText(info.get("scoutName") + " - " + info.get("teamNum"));
            }
        }
    }

    //puts restrictions on certain LimitedTextFields
    @FXML private void validateInput(KeyEvent keyEvent) {
        Object src = keyEvent.getSource(); // element that got input
        if (src.equals(teamNum)) {
            teamNum.setRestrict("^(?:[1-9]\\d{0,4})?$"); // numerics only, no leading zeroes
            teamNum.setMaxLength(5);
        } else if (src.equals(matchNum)) {
            matchNum.setRestrict("^(?:[1-9]\\d{0,3})?$"); // no leading zeroes
            matchNum.setMaxLength(3);
        } else if (src.equals(scoutName)) {
            scoutName.setRestrict("[a-zA-Z ]");
            scoutName.setMaxLength(30);
        } else if (src.equals(climbTime)) {
            climbTime.setRestrict(("^(?:0|[1-9]\\d{0,2})?$"));
            climbTime.setMaxLength(3);
        } else if (src.equals(comments)) {
            // restrict '|' character from being entered in text area, and sets reasonable max length
            comments.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.contains("|")) {
                    comments.setText(oldValue);
                }
                if (comments.getText().length() > 500) {
                    comments.setText(oldValue);
                }
            });
        }
    }

    //validation for required fields
    private boolean requiredFieldsAreOK() {
        String warnings = "";
        //TODO: add more warnings based on issues with info
        try {
            if (info.get("teamNum").isBlank() || info.get("teamNum").matches("0+"))
                warnings += "Fix the team number (cannot contain only 0s or be blank). ";
            if (info.get("matchNum").isBlank() || info.get("matchNum").matches("0+"))
                warnings += "Fix the match number (cannot contain only 0s or be blank). ";
            if (info.get("scoutName").isBlank())
                warnings += "Fix the scout name (cannot be blank). ";


            System.out.println(warnings); // for debug purposes
            if (warnings.isBlank()) return true;
            else {
                AlertBox.display("xputs", warnings);
                return false;
            }
        }
        catch (Exception e) {
            AlertBox.display("Bad inputs", "There was an error with the inputs. Please check them and try again.");
            return false;
        }
    }

    /**
     *     saves output to QR Codes and text files on computer,
     *     copies in Desktop/Scouting and Documents/backupScouting of active user
     *     @param matchNum match number
     *     @param teamNum team number
     *     @param scoutName name of scout
     */
    private void outputAll(int matchNum, int teamNum, String scoutName) {
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
    
    /**
     * writes data to CSV on computer
     * @param data HashMap data to be written
     * @param outputCSVPath path to CSV file
     * @throws IOException file paths not found
     */
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

    /**
     * copies data (text or qr code) to clipboard
     * @param event button click source element
     */
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
    private void collectDataTextField(TextField textField, String key) {info.put(key, textField.getText());}
    private void collectDataArray(ArrayList<Integer> array, String key) {
        Collections.sort(array);
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
    private <T> void collectDataComboBox(ComboBox<T> comboBox, String key) {
        info.put(key, String.valueOf(comboBox.getValue()));
    }
    private void collectDataPlusMinusBox(PlusMinusBox plusMinusBox, String key) {
        info.put(key, plusMinusBox.getValueElement().getText());
    }
    private void collectDataTally(Tally tally, String firstKey, String secondKey, boolean sendMissesNotTotal) {
        info.put(firstKey, String.valueOf(tally.getNumerator().getValueElement().getText()));
        if (sendMissesNotTotal) {
            int scored = Integer.parseInt(tally.getNumerator().getValueElement().getText());
            int total = Integer.parseInt(tally.getDenominator().getValueElement().getText());
            info.put(secondKey, String.valueOf(total - scored));
        }
        else {
            info.put(secondKey, String.valueOf(tally.getDenominator().getValueElement().getText()));
        }
    }
    @FXML private void manipAutonPickupGrid(ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: green; -fx-border-color: black;");
            FXMLController.autonPickups.add(Integer.valueOf(btn.getUserData().toString()));
        } else if (btn.getStyle().contains("-fx-background-color: green;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            FXMLController.autonPickups.remove(Integer.valueOf(btn.getUserData().toString()));
        }
    }

    //used in reloadData() for specific types of data
    private void reloadDataCheckBox(CheckBox checkBox, String key) {
        checkBox.setSelected(Boolean.parseBoolean(info.get(key)));
    }
    private void reloadDataTextField(TextField textField, String key) {
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
    private void reloadDataPlusMinusBox(PlusMinusBox plusMinusBox, String key) {
        if (info.get(key) != null) plusMinusBox.getValueElement().setText(info.get(key));
    }

    private void reloadDataStringComboBox(ComboBox<String> comboBox, String key) {
        if (info.get(key) != null) comboBox.setValue(info.get(key));
    }
    private void reloadDataIntegerComboBox(ComboBox<Integer> comboBox, String key) {
        if (info.get(key) != null) comboBox.setValue(Integer.valueOf(info.get(key)));
    }
    private void reloadDataTally(Tally tally, String firstKey, String secondKey, boolean getMissesNotTotal) {
        if (info.get(firstKey) == null || info.get(secondKey) == null) return;
        tally.getNumerator().getValueElement().setText(info.get(firstKey));
        if (getMissesNotTotal) {
            int scored = Integer.parseInt(info.get(firstKey));
            int missed = Integer.parseInt(info.get(secondKey));
            tally.getDenominator().getValueElement().setText(String.valueOf(scored + missed));
        }
        else {
            tally.getDenominator().getValueElement().setText(info.get(secondKey));
        }
    }

    private void reloadAutonPickupGrid(GridPane grid) {
        int gridLength = grid.getChildren().size();
        for (int i = 0; i < gridLength; i++) {
            Button btn = (Button) grid.getChildren().get(i);
            if (autonPickups.contains(Integer.valueOf(btn.getUserData().toString())))
                btn.setStyle("-fx-background-color: green; -fx-border-color: black;");
        }
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

    //flips pregame start location image
    @FXML private void flipStartLocationImage(ActionEvent ignoredEvent) {
        char alliance = driveStation.getSelectedToggle().getUserData().toString().charAt(0);
        if (alliance == 'b') {
            if (startLocationImageFlipped) {
                startLocationImageFlipped = false;
                startLocationPNG.setImage(new Image(blueField));
            } else {
                startLocationImageFlipped = true;
                startLocationPNG.setImage(new Image(blueFieldReversed));
            }
        } else {
            if (startLocationImageFlipped) {
                startLocationImageFlipped = false;
                startLocationPNG.setImage(new Image(redField));
            } else {
                startLocationImageFlipped = true;
                startLocationPNG.setImage(new Image(redFieldReversed));
            }
        }
    }

    @FXML private void flipAutonPickupImage(ActionEvent ignoredEvent) {
        if (autonPickupGridFlipped && autonPickupGridColor == 'b') {
            autoPickupGridBlue.rotateProperty().set(0);
            autoPickupGridBlue.translateXProperty().set(0);
            autonPickupGridFlipped = false;
            autoPickupPNG.setImage(new Image(blueAutoPickup));
        }
        else if (autonPickupGridFlipped && autonPickupGridColor == 'r') {
            autoPickupGridRed.rotateProperty().set(0);
            autoPickupGridRed.translateXProperty().set(0);
            autonPickupGridFlipped = false;
            autoPickupPNG.setImage(new Image(redAutoPickup));
        }
        else if (!autonPickupGridFlipped && autonPickupGridColor == 'b') {
            autoPickupGridBlue.rotateProperty().set(180);
            autoPickupGridBlue.translateXProperty().set(-110); // fudge factored to look right
            autonPickupGridFlipped = true;
            autoPickupPNG.setImage(new Image(blueAutoPickupReversed));
        }
        else if (!autonPickupGridFlipped && autonPickupGridColor == 'r') {
            autoPickupGridRed.rotateProperty().set(180);
            autoPickupGridRed.translateXProperty().set(110); // fudge factored to look right
            autonPickupGridFlipped = true;
            autoPickupPNG.setImage(new Image(redAutoPickupReversed));
        }
    }

    /**
     * <p> {@code resetAll} - resets all forms of data storage and goes to first page
     * <p> {@code nextPage} - goes to next page
     * <p> {@code prevPage} - goes to previous page
     * <p> {@code setPage} - goes to specified page
     * <p> {@code letterbox} - resizes scene to fit screen
     */
    //implementations of setPage() for going to next and previous pages
    @FXML private void resetAll(Event ignoredEvent) throws IOException {
        // increments match number for next match
        try {
            prevMatchNum = String.valueOf(Integer.parseInt(info.get("matchNum")) + 1);
            prevScouterName = info.get("scoutName");
            prevDriveStation = toggleMap.get("driveStation");
        } catch (Exception e) {
            AlertBox.display("Error", "Error resetting data");
        }

        // reset data storage variables
        data = new StringBuilder();
        info = new LinkedHashMap<>();
        toggleMap = new HashMap<>();
        autonPickups = new ArrayList<>();

        // resets UI
        setPage(stage, Page.PREGAME);
    }
    @FXML private void nextPage(Event event) throws IOException {
        collectData();
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setPage(stage, Page.values()[currPage.ordinal() + 1]);
    }
    @FXML private void prevPage(Event event) throws IOException {
        //collects data from current page and goes to previous page
        collectData();
        if (currPage == Page.BEGIN) return;
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setPage(stage, Page.values()[currPage.ordinal() - 1]);
    }

    //changes page to the scene specified by sceneIndex
    public static void setPage(Stage stage, Page page) throws IOException {
        FXMLController.stage = stage;
        if (!stage.isShowing()) stage.show();

        currPage = page;
        FXMLLoader loader = new FXMLLoader(FXMLController.class.getResource("scenes/scene" + (currPage.ordinal()) + ".fxml"));
        //if next line causes errors, check syntax in all fxml files
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.sizeToScene();

        double reqHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight()-65;

        letterbox(scene, (Pane) scene.getRoot());
        stage.setWidth(reqHeight * 1.5);
        stage.setHeight(reqHeight);

        stage.centerOnScreen();
    }

    /**
     * Resizes the scene elements as the stage changes size.
     * ALways call this method after showing the stage, so that scene.getWidth() and scene.getHeight() are valid.
     */
    private static void letterbox(final Scene scene, final Pane contentPane) {
        final double initWidth  = scene.getWidth();
        final double initHeight = scene.getHeight();
        final double ratio      = initWidth / initHeight;

        SceneSizeChangeListener sizeListener = new SceneSizeChangeListener(scene, ratio, initHeight, initWidth, contentPane);
        scene.widthProperty().addListener(sizeListener);
        scene.heightProperty().addListener(sizeListener);
    }

    /**
     * Keybinds for the app
     * Keybinds:
     * Ctrl + Enter - go to next page
     * Ctrl + Backspace - go to previous page
     * Ctrl + R - reset all data and go to first page
     * Ctrl + 1-6 - go to specified page
     * Ctrl + Shift + E - minimize app
     *
     */
    @FXML private void runUserKeybinds(Event newEvent) throws IOException {
        KeyEvent event = (KeyEvent) newEvent;
        if (event.isControlDown()) {
            if (event.isShiftDown() && event.getCode() == KeyCode.E) {
                    stage.setIconified(true);
            }
            if (event.getCode() == KeyCode.ENTER && currPage != Page.QR_CODE) {
                nextPage(newEvent);
            } else if (event.getCode() == KeyCode.BACK_SPACE && currPage != Page.BEGIN) {
                prevPage(newEvent);
            } else if (event.getCode() == KeyCode.R) {
                resetAll(newEvent);
            } else if (event.getCode() == KeyCode.DIGIT1) {
                collectData();
                setPage(stage, Page.PREGAME);
            } else if (event.getCode() == KeyCode.DIGIT2) {
                collectData();
                setPage(stage, Page.AUTON);
            } else if (event.getCode() == KeyCode.DIGIT3) {
                collectData();
                setPage(stage, Page.TELEOP);
            } else if (event.getCode() == KeyCode.DIGIT4) {
                collectData();
                setPage(stage, Page.ENDGAME);
            } else if (event.getCode() == KeyCode.DIGIT5) {
                collectData();
                setPage(stage, Page.QUALITATIVE_NOTES);
            } else if (event.getCode() == KeyCode.DIGIT6) {
                collectData();
                setPage(stage, Page.QR_CODE);
            }
        }
    }

    @FXML public void generateQRCode(ActionEvent event) throws Exception {
        collectData();
        if (requiredFieldsAreOK()) {
            nextPage(event);
        }
    }

    @FXML public void openSchedule(ActionEvent ignoredEvent) throws IOException {
        new File(outputPath).mkdirs();
        boolean fileDidNotExist = new File(schedulePath).createNewFile();
        BufferedReader r = new BufferedReader(new FileReader(schedulePath));
        FileWriter wr;
        // if connected to internet, prefer schedule from TBA API; otherwise put a placeholder for manually entering schedule
        if (isInternetConnected()) {
            wr = new FileWriter(schedulePath);
            String matchUrl = "https://frc-api.firstinspires.org/v3.0/2024/schedule/" + eventCode + "?tournamentLevel=Qualification";
            HttpURLConnection con = (HttpURLConnection) new URL(matchUrl).openConnection();
            con.setRequestMethod("GET");
            final String auth = "thepianoman:40e1597d-89f6-4dc1-9c90-3359b87ea809";
            String encodedAuth = new String(Base64.getEncoder().encode(auth.getBytes()));
            con.setRequestProperty("Authorization", "Basic " + encodedAuth);
            con.setRequestProperty("Accept", "application/json");

            StringBuilder response = new StringBuilder();
            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                in.lines().forEach(response::append);
                in.close();
                con.disconnect();
                try {
                    JSONArray arr = new JSONObject(response.toString()).getJSONArray("Schedule");
                    for (Object obj : arr) {
                        JSONObject match = (JSONObject) obj;
                        JSONArray teams = match.getJSONArray("teams");
                        int matchNum = match.getInt("matchNumber");
                        int r1 = ((JSONObject) teams.get(0)).getInt("teamNumber");
                        int r2 = ((JSONObject) teams.get(1)).getInt("teamNumber");
                        int r3 = ((JSONObject) teams.get(2)).getInt("teamNumber");
                        int b1 = ((JSONObject) teams.get(3)).getInt("teamNumber");
                        int b2 = ((JSONObject) teams.get(4)).getInt("teamNumber");
                        int b3 = ((JSONObject) teams.get(5)).getInt("teamNumber");
                        wr.write(matchNum + "," + r1 + "," + r2 + "," + r3 + "," + b1 + "," + b2 + "," + b3 + "\n");
                    }
                } catch (IOException e) {
                    System.err.println("Error reading JSON");
                    }
                    wr.flush();
                    wr.close();
            }
            else {
                System.out.println("Failed to fetch data from the API");
            }
        }

        if (r.readLine() == null || r.readLine().isBlank()) {
                wr = new FileWriter(schedulePath);
                wr.write("[Match Number],[Red1],[Red2],[Red3],[Blue1],[Blue2],[Blue3]\n");
                wr.flush();
                wr.close();
        }
        try {
            Desktop.getDesktop().open(new File(schedulePath));
        } catch (Exception e) {
            System.err.println("Copy failure");
        }
    }

    private void createSchedule() {
        try (BufferedReader reader = new BufferedReader(new FileReader(schedulePath))) {
            schedule = reader.lines().parallel()
                    .map(line -> {
                        String[] strings = line.split(",");
                        int[] records = new int[7];
                        for (int i = 0; i < 7; i++)
                            records[i] = Integer.parseInt(strings[i]);
                        return records;
                    })
                    .toList();
        } catch (Exception e) {
            System.err.println("Failed to Create schedule");
        }
    }


    private boolean isInternetConnected() {
        try {
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}