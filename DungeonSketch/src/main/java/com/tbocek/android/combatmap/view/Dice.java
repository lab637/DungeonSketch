package com.tbocek.android.combatmap.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tbocek.android.combatmap.model.primitives.Units;
import com.tbocek.dungeonsketch.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by tbocek on 4/1/15.
 */
public class Dice extends LinearLayout {
    private static class Model {
        private Map<Integer, List<Integer>> mRolls = new HashMap<>();
        private Random rand = new Random();

        public void clear() {
            mRolls.clear();
        }

        public int getNumberOfRolls() {
            int n = 0;
            for (List<Integer> rollList: mRolls.values()) {
                n = Math.max(n, rollList.size());
            }
            return n;
        }

        public int getNumberOfRolls(int sides) {
            if (mRolls.containsKey(sides)) {
                return mRolls.get(sides).size();
            }
            return 0;
        }

        public int getRoll(int sides, int index) {
            if (mRolls.containsKey(sides)) {
                List<Integer> rolls = mRolls.get(sides);
                if (rolls.size() > index) {
                    return rolls.get(index);
                }
            }
            return 0;
        }

        public int getRollTotal(int sides) {
            int total = 0;
            if (mRolls.containsKey(sides)) {
                for (int roll: mRolls.get(sides)) {
                    total += roll;
                }
            }
            return total;
        }

        public void roll(int sides) {
            if (!mRolls.containsKey(sides)) {
                mRolls.put(sides, new ArrayList<Integer>());
            }
            mRolls.get(sides).add(rand.nextInt(sides) + 1);
        }
    }

    private class DieClickListener implements OnClickListener {
        int mSides;
        public DieClickListener(int sides) {
            mSides = sides;
        }
        @Override
        public void onClick(View v) {
            mModel.roll(mSides);
            updateFromModel();
        }
    }

    private static int SIDES[] = {4, 6, 8, 10, 12, 20, 100};

    Model mModel = new Model();

    List<LinearLayout> mDisplayColumns = new ArrayList<>();
    LinearLayout mControlColumn;

    private boolean mDarkBackground;

    private static final int TEXT_COLOR_LIGHT_BACKGROUND = Color.BLACK;
    private static final int TEXT_COLOR_DARK_BACKGROUND = Color.WHITE;


    /**
     * Constructor.
     *
     * @param context Context that this view uses.
     */
    public Dice(Context context) {
        this(context, null, 0);
    }

    public Dice(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Dice(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.dice, this);
        mControlColumn = (LinearLayout) this.findViewById(R.id.dice_control_column);

        findViewById(R.id.dice_d4).setOnClickListener(new DieClickListener(4));
        findViewById(R.id.dice_d6).setOnClickListener(new DieClickListener(6));
        findViewById(R.id.dice_d8).setOnClickListener(new DieClickListener(8));
        findViewById(R.id.dice_d10).setOnClickListener(new DieClickListener(10));
        findViewById(R.id.dice_d12).setOnClickListener(new DieClickListener(12));
        findViewById(R.id.dice_d20).setOnClickListener(new DieClickListener(20));
        findViewById(R.id.dice_d100).setOnClickListener(new DieClickListener(100));

        findViewById(R.id.dice_clear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mModel.clear();
                updateFromModel();
            }
        });
    }

    private void updateFromModel() {
        int rollCount = mModel.getNumberOfRolls();
        // If there are two or more rolls in any dice, add columns for the totals.
        if (rollCount < 2) {
            setColumnCount(rollCount);
        } else {
            setColumnCount(rollCount + 2);
        }

        for (int i = 0; i < SIDES.length; ++i) {
            for (int j = 0; j < rollCount; ++j) {
                int roll = mModel.getRoll(SIDES[i], j);
                setText(i, j, roll > 0 ? Integer.toString(roll) : "");
            }
        }

        if (rollCount >= 2) {
            for (int i = 0; i < SIDES.length; ++i) {
                int total = mModel.getRollTotal(SIDES[i]);
                setText(i, rollCount, mModel.getNumberOfRolls(SIDES[i]) > 1 && total > 0 ? "=" : "");
                setText(i, rollCount + 1, mModel.getNumberOfRolls(SIDES[i]) > 1 && total > 0 ? Integer.toString(total) : "");
            }
        }
    }

    private void setColumnCount(int columns) {
        this.removeAllViews();
        this.clearParent(mControlColumn);
        this.addView(mControlColumn);
        for (int i = 0; i < columns; ++i) {
            if (i < mDisplayColumns.size()) {
                this.clearParent(mDisplayColumns.get(i));
                this.addView(mDisplayColumns.get(i));
            } else {
                LinearLayout col = newDisplayColumn();
                this.addView(col);
                mDisplayColumns.add(col);
            }
        }
    }

    private LinearLayout newDisplayColumn() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        View exampleView = findViewById(R.id.dice_d4);

        for (int i = 0; i < mControlColumn.getChildCount(); ++i) {
            TextView tv = new TextView(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, exampleView.getHeight());
            tv.setLayoutParams(layoutParams);
            tv.setTextSize(24);
            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            tv.setPadding(0, exampleView.getPaddingTop(), (int) Units.dpToPx(16), exampleView.getPaddingBottom());
            setTextViewColor(tv);
            layout.addView(tv);
        }
        return layout;
    }

    private void setText(int row, int col, String text) {
        LinearLayout column = (LinearLayout)this.getChildAt(col + 1);
        TextView tv = (TextView) column.getChildAt(row);
        tv.setText(text);
    }

    private void clearParent(View child) {
        if (child != null && child.getParent() != null)
            ((ViewGroup)child.getParent()).removeView(child);
    }

    public void setIsBackgroundDark(boolean isDark) {
        mDarkBackground = isDark;

        for (LinearLayout col : mDisplayColumns) {
            for (int i = 0; i < col.getChildCount(); ++i) {
                TextView tv = (TextView) col.getChildAt(i);
                setTextViewColor(tv);
            }
        }
    }

    private void setTextViewColor(TextView tv) {
        tv.setTextColor(mDarkBackground ? TEXT_COLOR_DARK_BACKGROUND : TEXT_COLOR_LIGHT_BACKGROUND);
    }
}
