package uk.ac.soton.comp3200.view.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog showing progress during export operations
 */
public class ExportProgressDialog {
    private final Stage dialogStage;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Button actionButton;
    public final int minProgress;
    private final int maxProgress;
    private final Runnable cancelAction;

    /**
     * Creates a new export progress dialog
     *
     * @param parent The parent window
     * @param title The dialog title
     * @param minProgress The minimum progress value
     * @param maxProgress The maximum progress value
     */
    public ExportProgressDialog(Stage parent, String title, int minProgress, int maxProgress, Runnable cancelAction) {
        this.minProgress = minProgress;
        this.maxProgress = maxProgress;
        this.cancelAction = cancelAction;

        dialogStage = new Stage();
        dialogStage.initOwner(parent);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);

        statusLabel = new Label("Preparing export...");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        actionButton = new Button("Cancel");
        actionButton.setOnAction(e -> {
            if (actionButton.getText().equals("Cancel")) {
                statusLabel.setText("Cancelling...");
                actionButton.setDisable(true);
                if (this.cancelAction != null) {
                    this.cancelAction.run();
                }
            } else {
                dialogStage.close();
            }
        });

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(statusLabel, progressBar, actionButton);

        Scene scene = new Scene(root);
        dialogStage.setScene(scene);
    }

    /**
     * Shows the dialog
     */
    public void show() {
        dialogStage.show();
    }

    /**
     * Updates the progress display
     *
     * @param currentProgress The current progress value
     * @param message The status message to display
     */
    public void updateProgress(int currentProgress, String message) {
        Platform.runLater(() -> {
            if (!dialogStage.isShowing()) return;

            double progress = (double)(currentProgress - minProgress) / (maxProgress - minProgress);
            progressBar.setProgress(progress);
            statusLabel.setText(message);

            if (currentProgress >= maxProgress && !actionButton.getText().equals("Close")) {
                changeButtonToClose();
            }
        });
    }

    /**
     * Sets the dialog to show completion
     *
     * @param message The completion message
     */
    public void setComplete(String message) {
        Platform.runLater(() -> {
            if (!dialogStage.isShowing()) return;
            progressBar.setProgress(1.0);
            progressBar.setStyle("-fx-accent: green;");
            statusLabel.setText(message);
            changeButtonToClose();
        });
    }

    /**
     * Sets the dialog to show an error
     *
     * @param message The error message
     */
    public void setError(String message) {
        Platform.runLater(() -> {
            if (!dialogStage.isShowing()) return;
            progressBar.setStyle("-fx-accent: red;");
            statusLabel.setText(message);
            changeButtonToClose();
        });
    }

    /**
     * Changes the action button from "Cancel" to "Close"
     */
    private void changeButtonToClose() {
        Platform.runLater(() -> {
            actionButton.setText("Close");
            actionButton.setDisable(false);
        });
    }


    /**
     * Closes the dialog after a delay
     *
     * @param delayMs The delay in milliseconds
     */
    public void closeAfterDelay(int delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                Platform.runLater(() -> {
                    if (dialogStage.isShowing()) {
                        dialogStage.close();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Checks if the dialog is currently showing
     *
     * @return true if the dialog is showing
     */
    public boolean isShowing() {
        return dialogStage.isShowing();
    }

    /**
     * Gets the maximum progress value
     *
     * @return The maximum progress value
     */
    public int getMaxProgress() {
        return maxProgress;
    }
}