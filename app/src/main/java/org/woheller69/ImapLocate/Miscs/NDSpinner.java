package org.woheller69.ImapLocate.Miscs;

/**
 * Created by Deepak on 7/1/2015.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Spinner extension that calls onItemSelected even when the selection is the same as its previous value
 */
@SuppressLint("AppCompatCustomView")
public class NDSpinner extends Spinner {
    public boolean isDropDownMenuShown = false;
    public boolean initIsDone = false;

    public NDSpinner(Context context) {
        super(context);
    }

    public NDSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NDSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public void
    setSelection(int position, boolean animate) {
        boolean sameSelected = position == getSelectedItemPosition();
        super.setSelection(position, animate);
        initIsDone = true;
        if (sameSelected) {
            // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }

    @Override
    public boolean performClick() {
        this.isDropDownMenuShown = true; //Flag to indicate the spinner menu is shown
        return super.performClick();
    }

    public boolean isDropDownMenuShown() {
        return isDropDownMenuShown;
    }

    public void setDropDownMenuShown(boolean isDropDownMenuShown) {
        this.isDropDownMenuShown = isDropDownMenuShown;
    }

    @Override
    public void
    setSelection(int position) {
        boolean sameSelected = position == getSelectedItemPosition();
        super.setSelection(position);
        initIsDone = true;
        if (sameSelected) {
            // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

}
