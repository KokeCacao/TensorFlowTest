package me.kokecacao.tensorflow.tensorflowtest;

import android.graphics.RectF;

/**
 * Created by Koke_Cacao on 2018/1/19.
 */

public class TFResult {

    private String label;
    private Float score;
    private RectF box;

    public TFResult(String label, Float score, RectF location) {
        this.label = label;
        this.score = score;
        this.box = location;
    }

    public String getLabel() {
        return label;
    }

    public Float getScore() {
        return score;
    }

    public RectF getBox() {
        return box;
    }
}
