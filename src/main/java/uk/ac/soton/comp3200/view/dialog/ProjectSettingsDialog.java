package uk.ac.soton.comp3200.view.dialog;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;

public class ProjectSettingsDialog extends Dialog<Pair<Integer, Integer>> {

    private final TextField fpsField;
    private final TextField maxFramesField;

    public ProjectSettingsDialog(Stage owner, int currentFps, int currentMaxFrames) {
        initOwner(owner);
        setTitle("Project Settings");
        setHeaderText("Configure animation properties.");

        fpsField = createIntegerField(currentFps);
        maxFramesField = createIntegerField(currentMaxFrames);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("FPS (1-60):"), 0, 0);
        grid.add(fpsField, 1, 0);
        grid.add(new Label("Max Frames (>9):"), 0, 1);
        grid.add(maxFramesField, 1, 1);

        getDialogPane().setContent(grid);

        ButtonType okButtonType = ButtonType.OK;
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        Node okButton = getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        Runnable validationTask = () -> {
            boolean fpsValid = isInteger(fpsField.getText());
            boolean maxFramesValid = isInteger(maxFramesField.getText());
            okButton.setDisable(!fpsValid || !maxFramesValid);
        };

        fpsField.textProperty().addListener((obs, oldV, newV) -> validationTask.run());
        maxFramesField.textProperty().addListener((obs, oldV, newV) -> validationTask.run());
        validationTask.run();

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int fps = Integer.parseInt(fpsField.getText());
                    int maxFrames = Integer.parseInt(maxFramesField.getText());
                    return new Pair<>(fps, maxFrames);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
    }

    private TextField createIntegerField(int initialValue) {
        return new TextField(String.valueOf(initialValue));
    }

    private boolean isInteger(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}