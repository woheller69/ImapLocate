package org.woheller69.ImapLocate;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.net.Uri;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.woheller69.ImapLocate.Data.OneNote;
import org.woheller69.ImapLocate.Miscs.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An easy adapter to map static data to views defined in an XML file. You can specify the data
 * backing the list as an ArrayList of Maps. Each entry in the ArrayList corresponds to one row
 * in the list. The Maps contain the data for each row. You also specify an XML file that
 * defines the views used to display the row, and a mapping from keys in the Map to specific
 * views.
 * <p>
 * Binding data to views occurs in two phases. First, if a
 * {@link android.widget.SimpleAdapter.ViewBinder} is available,
 * {@link ViewBinder#setViewValue(android.view.View, Object, String)}
 * is invoked. If the returned value is true, binding has occurred.
 * If the returned value is false, the following views are then tried in order:
 * <ul>
 * <li> A view that implements Checkable (e.g. CheckBox).  The expected bind value is a boolean.
 * <li> TextView.  The expected bind value is a string and {@link #setViewText(TextView, String)}
 * is invoked.
 * <li> ImageView. The expected bind value is a resource id or a string and
 * {@link #setViewImage(ImageView, int)} or {@link #setViewImage(ImageView, String)} is invoked.
 * </ul>
 * If no appropriate binding can be found, an {@link IllegalStateException} is thrown.
 */
public class NotesListAdapter extends BaseAdapter implements Filterable {
    private final Context mContext;
    private final int[] mTo;
    private final String[] mFrom;
    private final String mBgColor;
    // --Commented out by Inspection (12/3/16 11:31 PM):private ViewBinder mViewBinder;

    private List<? extends Map<String, ?>> mData;

    private final int mResource;
    private final int mDropDownResource;
    @NonNull
    private final LayoutInflater mInflater;

    private SimpleFilter mFilter;
    private ArrayList<Map<String, ?>> mUnfilteredData;

    /**
     * Constructor
     *
     * @param context The context where the View associated with this SimpleAdapter is running
     * @param data    A List of Maps. Each entry in the List corresponds to one row in the list. The
     *                Maps contain the data for each row, and should include all the entries specified in
     *                "from"
     * @param from    A list of column names that will be added to the Map associated with each
     *                item.
     * @param to      The views that should display column in the "from" parameter. These should all be
     *                TextViews. The first N views in this list are given the values of the first N columns
     */
    NotesListAdapter(@NonNull Context context, List<? extends Map<String, ?>> data,
                     String[] from, int[] to, String bgColor) {
        mContext = context;
        mData = data;
        mResource = mDropDownResource = R.layout.note_element;
        mFrom = from;
        mTo = to;
        mBgColor = bgColor;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * After Closing the filter, reset all filter data
     */
    public void ResetFilterData(List<? extends Map<String, ?>> mData) {
        mFilter = null;
        mUnfilteredData = null;
        this.mData = mData;
    }

    /**
     * @see android.widget.Adapter#getCount()
     */
    public int getCount() {
        return mData.size();
    }

    /**
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int position) {
        return mData.get(position);
    }

    /**
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * @see android.widget.Adapter#getView(int, View, ViewGroup)
     */
    @Nullable
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    // TODO: this should never return null and the convertView argument should never be null.
    @Nullable
    private View createViewFromResource(int position, @Nullable View convertView,
                                        ViewGroup parent, int resource) {
        View v;
        if (convertView == null) {
            v = mInflater.inflate(resource, parent, false);
        } else {
            v = convertView;
        }

        bindView(position, v);

        return v;
    }

// --Commented out by Inspection START (12/2/16 9:22 PM):
//    /**
//     * <p>Sets the layout resource to create the drop down views.</p>
//     *
//     * @param resource the layout resource defining the drop down views
//     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
//     */
//    public void setDropDownViewResource(int resource) {
//        this.mDropDownResource = resource;
//    }
// --Commented out by Inspection STOP (12/2/16 9:22 PM)

    // TODO: Should never return null.
    @Nullable
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }

    private void bindView(int position, @NonNull View view) {
        final Map dataSet = mData.get(position);
        if (dataSet == null) {
            return;
        }

        //final ViewBinder binder = mViewBinder;
        final int[] to = mTo;
        final int count = to.length;


        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {
                final Object data = dataSet.get(mFrom[i]);
                String text = data == null ? "" : data.toString();
                String bgColor = dataSet.get(mBgColor).toString();
                int bgColorNr = Utilities.getColorByName(bgColor, mContext);

                boolean bound = false;
                //if (binder != null) {
                //    bound = binder.setViewValue(v, data, text);
                //}

                if (!bound) {
                    if (v instanceof Checkable) {
                        if (data instanceof Boolean) {
                            ((Checkable) v).setChecked((Boolean) data);
                        } else if (v instanceof TextView) {
                            // Note: keep the instanceof TextView check at the bottom of these
                            // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                            setViewText((TextView) v, text);
                        } else {
                            throw new IllegalStateException(v.getClass().getName() +
                                    " should be bound to a Boolean, not a " +
                                    (data == null ? "<unknown type>" : data.getClass()));
                        }
                    } else if (v instanceof TextView) {
                        // Note: keep the instanceof TextView check at the bottom of these
                        // ifs since a lot of views are TextViews (e.g. CheckBoxes).
                        setViewText((TextView) v, text);
                        setBgColor((TextView) v, bgColorNr);
                        setBgColor((RelativeLayout) view, bgColorNr);

                    } else if (v instanceof ImageView) {
                        if (data instanceof Integer) {
                            setViewImage((ImageView) v, (Integer) data);
                        } else {
                            setViewImage((ImageView) v, text);
                        }
                    } else {
                        throw new IllegalStateException(v.getClass().getName() + " is not a " +
                                " view that can be bounds by this SimpleAdapter");
                    }
                }
            }
        }
    }

// --Commented out by Inspection START (12/2/16 9:22 PM):
//    /**
//     * Returns the {@link ViewBinder} used to bind data to views.
//     *
//     * @return a ViewBinder or null if the binder does not exist
//     * @see #getViewBinder()
//     */
//    private ViewBinder getViewBinder() {
//        return mViewBinder;
//    }
// --Commented out by Inspection STOP (12/2/16 9:22 PM)

// --Commented out by Inspection START (12/2/16 9:13 PM):
//    /**
//     * Sets the binder used to bind data to views.
//     *
//     * @param viewBinder the binder used to bind data to views, can be null to
//     *                   remove the existing binder
//     * @see #getViewBinder()
//     */
//    public void setViewBinder(ViewBinder viewBinder) {
//        mViewBinder = viewBinder;
//    }
// --Commented out by Inspection STOP (12/2/16 9:13 PM)

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     * <p>
     * This method is called instead of {@link #setViewImage(ImageView, String)}
     * if the supplied data is an int or Integer.
     *
     * @param v     ImageView to receive an image
     * @param value the value retrieved from the data set
     * @see #setViewImage(ImageView, String)
     */
    private void setViewImage(@NonNull ImageView v, int value) {
        v.setImageResource(value);
    }

    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     * <p>
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     * <p>
     * This method is called instead of {@link #setViewImage(ImageView, int)}
     * if the supplied data is not an int or Integer.
     *
     * @param v     ImageView to receive an image
     * @param value the value retrieved from the data set
     * @see #setViewImage(ImageView, int)
     */
    private void setViewImage(@NonNull ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }

    /**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to a TextView.
     *
     * @param v    TextView to receive text
     * @param text the text to be set for the TextView
     */
    private void setViewText(@NonNull TextView v, String text) {
        v.setText(text);
    }

    private void setBgColor(@NonNull RelativeLayout v, @ColorInt int bgColor) {
        v.setBackgroundColor(bgColor);
    }

    private void setBgColor(@NonNull TextView v, @ColorInt int bgColor) {
        v.setBackgroundColor(bgColor);
    }


    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new SimpleFilter();
        }
        return mFilter;
    }

    /**
     * This class can be used by external clients of SimpleAdapter to bind
     * values to views.
     * <p>
     * You should use this class to bind values to views that are not
     * directly supported by SimpleAdapter or to change the way binding
     * occurs for views supported by SimpleAdapter.
     *
     * @see SimpleAdapter#setViewImage(ImageView, int)
     * @see SimpleAdapter#setViewImage(ImageView, String)
     * @see SimpleAdapter#setViewText(TextView, String)
     */
    interface ViewBinder {
        /**
         * Binds the specified data to the specified view.
         * <p>
         * When binding is handled by this ViewBinder, this method must return true.
         * If this method returns false, SimpleAdapter will attempts to handle
         * the binding on its own.
         *
         * @param view               the view to bind the data to
         * @param data               the data to bind to the view
         * @param textRepresentation a safe String representation of the supplied data:
         *                           it is either the result of data.toString() or an empty String but it
         *                           is never null
         * @return true if the data was bound to the view, false otherwise
         */
        boolean setViewValue(View view, Object data, String textRepresentation);
    }

    public static List<String> searchHTML(String filePath, String searchTerm, boolean useRegex) {
        List<String> matches = new ArrayList<>();

        // Open the HTML file
        File htmlFile = new File(filePath);
        if (!htmlFile.exists()) {
            // Return an empty list if the file doesn't exist
            return matches;
        }

        // Read the contents of the HTML file
        String html = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(htmlFile));
            String line = "";
            while ((line = br.readLine()) != null) {
                html += line + "\n";
            }
            br.close();
        } catch (IOException e) {
            // Return an empty list if there is an error reading the file
            return matches;
        }

        // Compile the regular expression pattern if necessary
        Pattern pattern = null;
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchTerm);
            } catch (PatternSyntaxException e) {
                // Return an empty list if the regular expression is invalid
                return matches;
            }
        } else {
            pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
        }
        // Use a Matcher to search the HTML for the search term
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return matches;
    }

    /**
     * <p>An array filters constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    private class SimpleFilter extends Filter {

        @NonNull
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mUnfilteredData == null) {
                mUnfilteredData = new ArrayList<>(mData);
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<Map<String, ?>> list = mUnfilteredData;
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = prefix.toString().toLowerCase(Locale.getDefault());

                ArrayList<Map<String, ?>> unfilteredValues = mUnfilteredData;
                int count = unfilteredValues.size();

                ArrayList<Map<String, ?>> newValues = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    Map<String, ?> h = unfilteredValues.get(i);
                    if (h != null) {

                        //String str = (String) h.get(OneNote.TITLE) + (String) h.get(OneNote.DATE);
                        File directory = new File(ImapNotes3.GetRootDir(), (String) h.get(OneNote.ACCOUNT));

                        String uid = (String) h.get(OneNote.UID);
                        if (uid.startsWith("-")) {
                            uid = uid.substring(1);
                            directory = new File(directory, "new");
                        }

                        File searchfile = new File(directory, uid);

                        List<String> matches = searchHTML(searchfile.toString(), prefixString, false);

                        if (!matches.isEmpty()) {
                            newValues.add(h);
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
            //noinspection unchecked
            mData = (List<Map<String, ?>>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

}