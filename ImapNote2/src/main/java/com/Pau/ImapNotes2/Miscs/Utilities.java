package com.Pau.ImapNotes2.Miscs;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.Pau.ImapNotes2.BuildConfig;

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

}
