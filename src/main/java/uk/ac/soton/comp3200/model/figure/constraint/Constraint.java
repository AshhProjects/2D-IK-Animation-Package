package uk.ac.soton.comp3200.model.figure.constraint;

import uk.ac.soton.comp3200.model.figure.model.StickFigureModel;

public interface Constraint {
    /** Calculates and stores initial parameters (distance, offset) based on the model's current state. */
    void initialize(StickFigureModel model);

    /** Applies the constraint. May modify point positions in the model.
     * @param model The figure model.
     * @param recentlyMovedPointIndex The index of the point that was *just* moved (by user or another constraint),
     *                                 or -1 if applying initially or after keyframe load.
     * @return true if this constraint application resulted in moving a point, false otherwise.
     */
    boolean apply(StickFigureModel model, int recentlyMovedPointIndex);
}