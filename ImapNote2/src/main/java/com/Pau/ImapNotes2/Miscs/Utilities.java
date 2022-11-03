package com.Pau.ImapNotes2.Miscs;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.Pau.ImapNotes2.BuildConfig;
import com.Pau.ImapNotes2.R;

/**
 * Created by kj on 2016-11-12 17:21.
 * <p>
 * Reduce repetition by providing static fields and methods for common operations.
 */
public final class Utilities {
    @NonNull
    public static final String PackageName = BuildConfig.APPLICATION_ID;
    @NonNull
    public static String internalDateFormatString = "yyyy-MM-dd HH:mm:ss";
    @NonNull
    public static SimpleDateFormat internalDateFormat = new SimpleDateFormat(internalDateFormatString, Locale.ROOT);

    /**
     * The notes have a time stamp associated with time and this is stored as a string on the
     * server so we must define a fixed format for it.
     */

    public static int getColorIdByName(String name) {
        int color = R.color.ListBgColor;

        if (name == null || name.isEmpty()) {
            name = "BgNone";
        }

        try {
            Class res = R.color.class;
            Field field = res.getField(name);
            color = field.getInt(null);


            //     color = context.getResources().getColor(field.getInt(null));
//            color = ContextCompat.getResources().getColor(field.getInt(null));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return color;
    }

    public static int getColorByName(String name, Context context) {
        return context.getResources().getColor(getColorIdByName(name), context.getTheme());
    }


}
