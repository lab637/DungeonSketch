package com.tbocek.android.combatmap.view.interaction;

import android.graphics.RectF;
import android.view.MotionEvent;

import com.tbocek.android.combatmap.view.CombatView;

/**
 * Created by tbocek on 10/29/14.
 */
public class DrawSelectionInteractionMode extends CombatViewInteractionMode {

    /**
     * Constructor.
     *
     * @param view The CombatView that this interaction mode manipulates.
     */
    public DrawSelectionInteractionMode(CombatView view) {
        super(view);

    }

    @Override
    public boolean onDown(final MotionEvent e) {
        super.onDown(e);
        getView().startSelection();
        return true;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (this.getNumberOfFingers() == 1) {
            RectF r = new RectF(
                    Math.min(e1.getX(), e2.getX()),
                    Math.min(e1.getY(), e2.getY()),
                    Math.max(e1.getX(), e2.getX()),
                    Math.max(e1.getY(), e2.getY()));
            getView().getSelection().setRectangle(
                    getData().getWorldSpaceTransformer().screenSpaceToWorldSpace(r));
            getView().refreshMap();
            return true;
        } else {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void onUp(final MotionEvent event) {
        getView().finalizeSelection();
    }
}
