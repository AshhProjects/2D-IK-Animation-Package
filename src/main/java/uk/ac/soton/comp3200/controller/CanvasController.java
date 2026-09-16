package uk.ac.soton.comp3200.controller;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp3200.model.figure.manager.FigureManager;
import uk.ac.soton.comp3200.model.figure.model.PointData;
import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;
import uk.ac.soton.comp3200.view.common.ToolMode;
import uk.ac.soton.comp3200.view.scene.MenuView;

import java.util.List;

/**
 * Controller specifically for handling interactions with the main drawing Canvas.
 * Manages point dragging, figure translation (in MOVE mode), and canvas context menus.
 */
public class CanvasController {

    private static final Logger logger = LogManager.getLogger(CanvasController.class);

    private MenuView view;
    private Canvas canvas;
    private final FigureManager figureManager;
    private final MenuController menuController;

    private ToolMode currentToolMode = ToolMode.HAND;
    private boolean isDragging = false;
    private String draggedFigureId = null;
    private int draggedPointIndex = -1;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public CanvasController(FigureManager figureManager, MenuController menuController) {
        this.figureManager = figureManager;
        this.menuController = menuController;
    }

    public void setView(MenuView view) {
        this.view = view;
    }

    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        attachCanvasHandlers();
    }

    public void setCurrentToolMode(ToolMode toolMode) {
        this.currentToolMode = toolMode;
    }

    private void attachCanvasHandlers() {
        if (canvas == null) {
            logger.error("Cannot attach canvas handlers: Canvas is null.");
            return;
        }
        logger.debug("CanvasController attaching handlers to canvas.");
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        canvas.setFocusTraversable(true);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> canvas.requestFocus());
        canvas.addEventHandler(MouseEvent.MOUSE_ENTERED, this::handleMouseEnteredCanvas);
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExitedCanvas);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        canvas.setOnContextMenuRequested(this::handleContextMenuRequested);
    }

    public void handleMousePressed(MouseEvent event) {
        if (view == null || canvas == null) return;
        view.hideCanvasContextMenu();

        String clickedFigureId = null;
        int clickedPointIndex = -1;
        PointData clickedPointData = null;

        List<StickFigureModel> figures = figureManager.getAllFigures();
        for (int figIdx = figures.size() - 1; figIdx >= 0; figIdx--) {
            StickFigureModel currentFigure = figures.get(figIdx);
            for (int i = currentFigure.points.size() - 1; i >= 0; i--) {
                PointData pd = currentFigure.points.get(i);
                if (pd != null && pd.isDraggable && pd.isVisible && pd.shape != PointData.Shape.NONE && pd.isInside(event.getX(), event.getY())) {
                    clickedFigureId = currentFigure.id;
                    clickedPointIndex = i;
                    clickedPointData = pd;
                    break;
                }
            }
            if (clickedFigureId != null) break;
        }

        if (clickedPointData != null) {
            isDragging = true;
            draggedFigureId = clickedFigureId;
            draggedPointIndex = clickedPointIndex;

            figureManager.selectPoint(clickedFigureId, clickedPointIndex);
            menuController.handleFigureSelection(clickedFigureId);

            dragOffsetX = event.getX() - clickedPointData.position.x;
            dragOffsetY = event.getY() - clickedPointData.position.y;

            if (view != null) view.updateCursor(Cursor.CLOSED_HAND);

        } else {
            isDragging = false;
            draggedFigureId = null;
            draggedPointIndex = -1;
            if (figureManager.getSelectedFigureId() != null) {
                menuController.handleFigureSelection(null);
            }
            if (view != null) view.updateCursor(Cursor.DEFAULT);
        }

        menuController.requestCanvasRedraw();
    }

    public void handleMouseDragged(MouseEvent event) {
        if (view == null || !isDragging || draggedFigureId == null || draggedPointIndex < 0) {
            return;
        }

        StickFigureModel figure = figureManager.getFigureById(draggedFigureId);
        PointData point = (figure != null) ? figure.getPointData(draggedPointIndex) : null;
        if (figure == null || point == null) {
            isDragging = false;
            logger.warn("Drag aborted: Figure or Point became null during drag.");
            return;
        }

        double targetX = event.getX() - dragOffsetX;
        double targetY = event.getY() - dragOffsetY;
        double deltaX = targetX - point.position.x;
        double deltaY = targetY - point.position.y;

        if (currentToolMode == ToolMode.MOVE) {
            figureManager.translateFigure(draggedFigureId, deltaX, deltaY);
            point.position.x = targetX;
            point.position.y = targetY;
        } else {
            boolean isMiddle = figure.isMiddleJoint(draggedPointIndex);
            boolean isBottom = point.name != null && point.name.equals("BODY_BOTTOM");
            figureManager.moveSelectedPoint(targetX, targetY, isMiddle, isBottom, deltaX, deltaY);
        }

        menuController.requestCanvasRedraw();
    }

    public void handleMouseReleased(MouseEvent event) {
        if (view == null) return;

        if (isDragging) {
            if (currentToolMode == ToolMode.MOVE && draggedFigureId != null) {
                figureManager.finalizeFigureManipulation(draggedFigureId);
                menuController.requestCanvasRedraw();
            } else if (currentToolMode == ToolMode.HAND && draggedFigureId != null) {
                figureManager.finalizeFigureManipulation(draggedFigureId);
                menuController.requestCanvasRedraw();
            }
        }

        isDragging = false;
        draggedFigureId = null;
        draggedPointIndex = -1;

        handleMouseMoved(event);
    }

    public void handleMouseMoved(MouseEvent event) {
        if (view == null || isDragging) return;
        updateViewCursor();
    }

    public void handleMouseEnteredCanvas(MouseEvent event) {
        if(view == null || isDragging) return;
        updateViewCursor();
    }

    public void handleMouseExitedCanvas(MouseEvent event) {
        if(view == null || isDragging) return;
        view.updateCursor(Cursor.DEFAULT);
    }

    public void handleContextMenuRequested(ContextMenuEvent event) {
        if (view == null) return;

        String figureIdUnderCursor = null;
        StickFigureModel figureModel = null;
        List<StickFigureModel> figures = figureManager.getAllFigures();
        for (int figIdx = figures.size() - 1; figIdx >= 0; figIdx--) {
            StickFigureModel currentFigure = figures.get(figIdx);
            if (currentFigure.anyPointInside(event.getX(), event.getY())) {
                figureIdUnderCursor = currentFigure.id;
                figureModel = currentFigure;
                break;
            }
        }

        if (figureIdUnderCursor != null) {
            final String finalId = figureIdUnderCursor;
            view.getDeleteFigureMenuItem().setOnAction(e -> menuController.handleDeleteFigureClicked(finalId));
            view.showCanvasContextMenu(event, true, figureModel.id);
        } else {
            view.getDeleteFigureMenuItem().setOnAction(null);
            view.showCanvasContextMenu(event, false, null);
        }
    }

    public void updateViewCursor() {
        if (view == null || isDragging) return;

        Cursor finalCursor;

        if (currentToolMode == ToolMode.MOVE) {
            finalCursor = Cursor.MOVE;
        } else {
            finalCursor = Cursor.DEFAULT;
        }
        view.updateCursor(finalCursor);
    }
}