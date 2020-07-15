package io.github.trojan_gfw.igniter.common.utils;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;

public class AnimationUtils {
    /**
     * Perform an sway animation on view. The view would start swaying from 0° to (<code>maxSwayPeriodDuration / 2</code>°) clockwise.
     * Then it sways to the axisymmetric position (in respect to the vertical center axis of the view) and sways back.
     * The view will keep swaying till the <code>nextDegree</code> reaches 0.
     * Each time the sway amplitude is decreased by {@param swayDegreeDecrementStep}.
     *
     * @param view                    The view to be animated
     * @param maxSwayDegree           Maximum sway amplitude.
     * @param maxSwayPeriodDuration   The sway duration for maximum sway amplitude.
     * @param swayDegreeDecrementStep Step of sway amplitude decrement. Must be larger than 0.
     */
    public static void sway(View view, float maxSwayDegree, long maxSwayPeriodDuration, float swayDegreeDecrementStep) {
        if (Float.compare(swayDegreeDecrementStep, 0f) <= 0) {
            throw new IllegalArgumentException("swayDegreeDecrementStep must be >= 0!");
        }
        AnimationSet rotateSet = new AnimationSet(true);
        rotateSet.setInterpolator(new AccelerateDecelerateInterpolator());
        float lastDegree = maxSwayDegree / 2f;
        Animation prepareAnim = new RotateAnimation(0f, lastDegree, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        prepareAnim.setDuration(maxSwayPeriodDuration >> 1);
        rotateSet.addAnimation(prepareAnim);
        long startOffSet = maxSwayPeriodDuration >> 1;
        boolean run = true;
        while (run) {
            float nextDegree;
            if (lastDegree > 0f) {
                nextDegree = -lastDegree;
            } else {
                nextDegree = -lastDegree - swayDegreeDecrementStep;
                if (Float.compare(nextDegree, 0f) <= 0) {
                    run = false;
                }
            }
            double degreeDistance = Math.abs(lastDegree) + Math.abs(nextDegree);
            long duration = (long) ((degreeDistance / maxSwayDegree) * maxSwayPeriodDuration);
            Animation rotate = new RotateAnimation(lastDegree, nextDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(duration);
            rotate.setStartOffset(startOffSet);
            startOffSet += duration;
            lastDegree = nextDegree;
            rotateSet.addAnimation(rotate);
        }
        view.startAnimation(rotateSet);
    }
}
