package uk.ac.soton.comp3200.view.scene;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import uk.ac.soton.comp3200.view.window.GameWindow;

/**
 * A Base Scene used in the game. Handles common functionality between all scenes.
 */
public abstract class BaseScene {

    /**
     * The game window which the entire game will be displayed in.
     */
    protected final GameWindow gameWindow;

    /**
     * the root pane used to show the UI.
     */
    protected BorderPane root;

    /**
     * The actual scene of the window. Uses a node, width and height and background colour to set the scene.
     */
    protected Scene scene;

    /**
     * Create a new scene, passing in the GameWindow the scene will be displayed in
     * @param gameWindow the game window
     */
    public BaseScene(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }

    /**
     * Build the layout of the scene
     */
    public abstract void build();

    /**
     * Create a new JavaFX scene using the root contained within this scene
     * @return JavaFX scene
     */
    public Scene setScene() {
        Scene previous = gameWindow.getScene();
        Scene scene = new Scene(root, previous.getWidth(), previous.getHeight(), Color.BLACK);
        this.scene = scene;
        return scene;
    }

    /**
     * Get the JavaFX scene contained inside
     * @return JavaFX scene
     */
    public Scene getScene() {
        return this.scene;
    }

}
