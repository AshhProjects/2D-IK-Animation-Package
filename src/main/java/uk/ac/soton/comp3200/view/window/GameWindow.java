package uk.ac.soton.comp3200.view.window;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.animation.TimelineModel;
import uk.ac.soton.comp3200.model.figure.manager.FigureManager;
import uk.ac.soton.comp3200.model.io.export.ExportManager;
import uk.ac.soton.comp3200.controller.MenuController;
import uk.ac.soton.comp3200.view.scene.*;
import uk.ac.soton.comp3200.model.io.SaveManager;

/**
 * The GameWindow is the single window for the game where everything takes place. To move between screens in the game,
 * we simply change the scene.
 * <p>
 * The GameWindow has methods to launch each of the different parts of the game by switching scenes. You can add more
 * methods here to add more screens to the game.
 */
public class GameWindow {

    private static final Logger logger = LogManager.getLogger(GameWindow.class);
    private final int width;
    private final int height;
    private final Stage stage;
    private Scene scene;

    private SaveManager saveManager;

    public Stage getStage() { return stage; }

    /**
     * Create a new GameWindow attached to the given stage with the specified width and height
     * @param stage stage
     * @param width width
     * @param height height
     */
    public GameWindow(Stage stage, int width, int height) {
        this.width = width;
        this.height = height;
        logger.info("width {} height {}",width,height);

        this.stage = stage;

        //Setup window
        setupStage();

        //Setup default scene
        setupDefaultScene();

        //Go to menu
        startMenu();
    }

    /**
     * Display the main menu
     */
    public void startMenu() {

        FigureManager figureManager = new FigureManager();
        TimelineModel timelineModel = new TimelineModel();
        saveManager = new SaveManager(stage, figureManager, timelineModel);
        ExportManager exportManager = new ExportManager(null, timelineModel, null, stage);

        MenuController menuController = new MenuController(timelineModel, figureManager, saveManager, exportManager);

        MenuView menuView = new MenuView(this);

        menuView.setController(menuController);

        loadScene(menuView);

        menuController.setView(menuView);

        menuController.setCanvasForExportManager(menuView.getCanvas());
    }

    /**
     * Setup the default settings for the stage itself (the window), such as the title and minimum width and height.
     */
    public void setupStage() {
        stage.setTitle("2D Animation Software");
        stage.setMinWidth(width);
        stage.setMinHeight(height + 20);
    }

    /**
     * Load a given scene which extends BaseScene and switch over.
     * @param newScene new scene to load
     */
    public void loadScene(BaseScene newScene) {
        newScene.build();
        scene = newScene.setScene();
        stage.setScene(scene);
    }
    /**
     * Setup the default scene (an empty white scene) when no scene is loaded
     */
    public void setupDefaultScene() {
        this.scene = new Scene(new Pane(),width,height, Color.WHITE);
        stage.setScene(this.scene);
    }

    /**
     * Get the current scene being displayed
     * @return scene
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Get the width of the Game Window
     * @return width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the Game Window
     * @return height
     */
    public int getHeight() {
        return this.height;
    }
}
