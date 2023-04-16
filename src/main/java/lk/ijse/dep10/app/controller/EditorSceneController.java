package lk.ijse.dep10.app.controller;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lk.ijse.dep10.app.util.SearchResult;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorSceneController {

    public boolean condition;
    public AnchorPane root;
    public MenuItem mnSaveAs;
    public TextField txtFind;
    public Button btnDown;
    public Button btnUp;
    public TextField txtReplace;
    public Button btnReplace;
    public Button btnReplaceAll;
    public Label lblNumOfSelection;
    public TextArea txtEditor;
    public MenuItem mnExport;
    JasperReport jasperReport;
    JasperDesign jasperDesign;
    @FXML
    private MenuItem mnAbout;

    @FXML
    private MenuItem mnClose;

    @FXML
    private MenuItem mnNew;

    @FXML
    private MenuItem mnOpen;

    @FXML
    private MenuItem mnPrint;

    @FXML
    private MenuItem mnSave;


    private boolean isSave = false;
    private File file = null;

    private String stageTitle = null;

    private ArrayList<SearchResult> searchResults = new ArrayList<>();
    private int pos = 0;


    public void initialize() {

        Platform.runLater(() -> {
            txtEditor.requestFocus();
            btnDown.setDisable(true);
            btnUp.setDisable(true);
            btnReplace.setDisable(true);
            btnReplaceAll.setDisable(true);
        });
        txtFind.textProperty().addListener((value, previous, current) -> {
            if (txtFind.getText().isEmpty() || txtFind.getText().isBlank()) {
                Platform.runLater(() -> {
                    lblNumOfSelection.setText("COUNT:");
                    btnDown.setDisable(true);
                    btnUp.setDisable(true);
                    btnReplace.setDisable(true);
                    btnReplaceAll.setDisable(true);
                });

                return;
            }
            Platform.runLater(() -> {
                btnDown.setDisable(false);
                btnDown.getStyleClass().add("linear-grad-to-top-right");
                btnUp.setDisable(false);
                btnReplace.setDisable(false);
                btnReplaceAll.setDisable(false);

            });

            findResultCount();


        });

        Platform.runLater(() -> {
            Stage stage = (Stage) btnDown.getScene().getWindow();

            stage.setOnCloseRequest(windowEvent -> {

                condition = isNotEdited();

                if (condition) return;
                Optional<ButtonType> optButtons = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to save before exit", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
                if (optButtons.isEmpty()) return;
                if (optButtons.get() == ButtonType.NO) {
                    stage.close();
                    return;
                }
                if (optButtons.get() == ButtonType.CANCEL) {
                    windowEvent.consume();
                    return;

                }

                ActionEvent event = new ActionEvent();

                try {
                    mnSaveOnAction(event);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        });

        try {
            jasperDesign = JRXmlLoader.load(this.getClass().getResourceAsStream("/report-for-print/print-text-editor.jrxml"));
            jasperReport = JasperCompileManager.compileReport(jasperDesign);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }

    }


    @FXML
    void mnAboutOnAction(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        stage.setTitle("Feather Pad");

        URL fxmlFile = this.getClass().getResource("/view/AboutScene.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlFile);
        AnchorPane root = fxmlLoader.load();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setWidth(600);
        stage.setHeight(450);
        stage.setResizable(false);

        stage.show();
        stage.centerOnScreen();
    }

    @FXML
    void mnCloseOnAction(ActionEvent event) throws IOException {


        if (isNotEdited()) {
            Stage stage = (Stage) txtEditor.getScene().getWindow();
            stage.close();
            return;
        }
        Optional<ButtonType> optButton = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save previous text?", ButtonType.YES, ButtonType.NO).showAndWait();
        if (optButton.isEmpty()) return;
        if (optButton.get() == ButtonType.NO) {
            Stage currentstage = (Stage) txtEditor.getScene().getWindow();
            currentstage.close();
            return;
        }
        ActionEvent event1 = new ActionEvent();
        mnSaveOnAction(event1);

    }

    @FXML
    void mnNewOnAction(ActionEvent event) throws IOException {

        if (isNotEdited()) {
            file = null;
            Stage stage = (Stage) txtEditor.getScene().getWindow();
            stageTitle = "Untiled Document";
            stage.setTitle("*" + stageTitle);
            txtEditor.setText("");
            return;
        }


        Optional<ButtonType> optButton = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save previous text?", ButtonType.YES, ButtonType.NO).showAndWait();

        if (optButton.isEmpty()) return;
        if (optButton.get() == ButtonType.NO) {
            Stage stage = (Stage) txtEditor.getScene().getWindow();
            stageTitle = "Untitled Document";
            stage.setTitle("*" + stageTitle);
            isSave = false;
            txtEditor.setText(null);

        } else {
            ActionEvent event1 = new ActionEvent();
            mnSaveOnAction(event1);

        }

    }

    @FXML
    void mnOpenOnAction(ActionEvent event) throws IOException {
        isSave = true;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open a text file");
        file = fileChooser.showOpenDialog(txtEditor.getScene().getWindow());


        if (file == null) {
            return;
        }

        FileInputStream is = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(is);

        try {
            String initialText = (String) ois.readObject();
            txtEditor.setText(initialText);
            Stage stage = (Stage) txtEditor.getScene().getWindow();
            stageTitle = file.getName();
            stage.setTitle(stageTitle);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

    @FXML
    void mnPrintOnAction(ActionEvent event) throws IOException {
        try {


            HashMap<String, Object> reportParam = new HashMap<>();
            JREmptyDataSource dataSource = new JREmptyDataSource(1);
            reportParam.put("text", txtEditor.getText());


            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParam, dataSource);


            JasperPrintManager.printReport(jasperPrint, true);


        } catch (JRException e) {
            throw new RuntimeException(e);
        }


    }

    @FXML
    void mnSaveOnAction(ActionEvent event) throws IOException {
        Stage stage = (Stage) txtEditor.getScene().getWindow();
        if (isSave) {
            FileOutputStream fos = new FileOutputStream(file, false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            String text = txtEditor.getText();
            oos.writeObject(text);


            stage.setTitle(file.getName());
            condition = false;
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the text file");
        file = fileChooser.showSaveDialog(txtEditor.getScene().getWindow());
        if (file == null) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(file, false);
        ObjectOutputStream oos = new ObjectOutputStream(fos);


        String text = txtEditor.getText();
        oos.writeObject(text);
        oos.close();

        isSave = true;
        condition = false;
        stageTitle = file.getName();
        stage.setTitle(stageTitle);

    }

    public void rootOnDragOver(DragEvent dragEvent) {

        dragEvent.acceptTransferModes(TransferMode.ANY);        //get drag files and accept


    }

    public void rootOnDragDropped(DragEvent dragEvent) {
        isSave = true;

        file = dragEvent.getDragboard().getFiles().get(0);      //take fist file of selected file set
        Stage stage = (Stage) txtEditor.getScene().getWindow();
        stageTitle = file.getName();
        stage.setTitle(stageTitle);


        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            String text = (String) ois.readObject();
            ois.close();
            txtEditor.setText(text);
            isSave = true;


        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void mnSaveAsOnAction(ActionEvent actionEvent) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save a text file");
        file = fileChooser.showSaveDialog(txtEditor.getScene().getWindow());
        if (file == null) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(file, false);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        String text = txtEditor.getText();
        oos.writeObject(text);
        oos.flush();
        oos.close();

        Stage stage = (Stage) txtEditor.getScene().getWindow();
        stageTitle = file.getName();
        stage.setTitle(stageTitle);

        isSave = true;


    }


    public boolean isNotEdited() {
        String currentText = txtEditor.getText();


        if ((file == null) && (currentText.isEmpty())) return true;
        if (file == null) return false;
        String previousText = null;

        try {
            FileInputStream is = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(is);
            previousText = (String) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


        return previousText.equals(currentText);
    }

    public void txtEditorOnKeyReleased(KeyEvent keyEvent) throws IOException {
        if (isNotEdited()) return;


        Stage stage = (Stage) txtEditor.getScene().getWindow();
        if (file == null && !(txtEditor.getText().isEmpty())) {
            stageTitle = "Untitled Document";
        } else {
            stageTitle = file.getName();

        }
        stage.setTitle("*" + stageTitle);

    }




    /*FInd and replace section*/


    private void findResultCount() {

        searchResults.clear();
        pos = 0;

        Pattern pattern = Pattern.compile(txtFind.getText());
        Matcher matcher = pattern.matcher(txtEditor.getText());

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            SearchResult result = new SearchResult(start, end);
            searchResults.add(result);
        }


        lblNumOfSelection.setText("COUNT: " + searchResults.size());


    }

    private void select() {
        if (searchResults.isEmpty()) return;
        SearchResult searchResult = searchResults.get(pos - 1);
        txtEditor.selectRange(searchResult.getStart(), searchResult.getEnd());
        lblNumOfSelection.setText("COUNT: " + (pos));
    }


    public void btnDownOnAction(ActionEvent actionEvent) {
        ++pos;
        if (pos == searchResults.size() + 1) {
            pos = -1;
            return;
        }
        if (pos == 0) ++pos;
        select();
    }

    public void btnUpOnAction(ActionEvent actionEvent) {
        --pos;

        if (pos <= 0) {
            pos = searchResults.size() + 1;
            return;
        }
        select();
    }


    public void btnReplaceOnAction(ActionEvent actionEvent) {


        if (txtReplace.getText().isEmpty()) return;
        String replacedText = txtEditor.getText();
        replacedText = replacedText.replaceFirst(txtFind.getText(), txtReplace.getText());

        txtEditor.setText(replacedText);
    }

    public void btnReplaceAllOnAction(ActionEvent actionEvent) {
        if (txtReplace.getText().isEmpty()) return;

        String replaceAllText = txtEditor.getText();
        replaceAllText = replaceAllText.replaceAll(txtFind.getText(), txtReplace.getText());
        txtEditor.setText(replaceAllText);

    }

    public void mnExportOnAction(ActionEvent actionEvent) {
        HashMap<String, Object> reportParam = new HashMap<>();
        JREmptyDataSource dataSource = new JREmptyDataSource(1);

        try {
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParam, dataSource);



        } catch (JRException e) {
            throw new RuntimeException(e);
        }


    }
}
