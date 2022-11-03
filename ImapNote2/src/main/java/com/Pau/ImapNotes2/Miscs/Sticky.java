package com.Pau.ImapNotes2.Miscs;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.internet.ContentType;

public class Sticky {
    private static final String TAG = "IN_Sticky";
    private static final Pattern patternColor = Pattern.compile("^COLOR:(.*?)$", Pattern.MULTILINE);
    // --Commented out by Inspection (12/2/16 8:50 PM):private static final Pattern patternPosition = Pattern.compile("^POSITION:(.*?)$", Pattern.MULTILINE);
    private static final Pattern patternText = Pattern.compile("TEXT:(.*?)(END:|POSITION:)", Pattern.DOTALL);

    public final String text;
    // --Commented out by Inspection (11/26/16 11:50 PM):private final String position;
    @NonNull
    public final String color;

    public Sticky(String text,
                  @NonNull String color) {
        this.text = text;
        // this.position = position;
        this.color = color;
    }
/*
    @Nullable
    public static Sticky GetStickyFromMessage(@NonNull Message message) {
        ContentType contentType = null;
        String stringres = "";
        //InputStream iis = null;
        //Colors color = NONE;
        //String charset;
        try {
            Log.d(TAG, "message :" + message);
            contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            InputStream iis = (InputStream) message.getContent();
            stringres = IOUtils.toString(iis, charset);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "Exception GetStickyFromMessage:");
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
        return ReadStickyNote(stringres);
    }

*/
/*
    private static String getPosition(String stringres) {

        Matcher matcherPosition = patternPosition.matcher(stringres);
        return matcherPosition.find() ?
                matcherPosition.group(1) :
                "";
    }*/

    @Nullable
    public static Sticky GetStickyFromMessage(@NonNull Message message) {
        ContentType contentType = null;
        String stringres = "";
        //InputStream iis = null;
        //Colors color = NONE;
        //String charset;
        try {
            Log.d(TAG, "message :" + message);
            contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            InputStream iis = (InputStream) message.getContent();
            stringres = IOUtils.toString(iis, charset);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "Exception GetStickyFromMessage:");
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }

        Log.d(TAG, "contentType:" + contentType);
        Sticky sticky = null;
        if (contentType.match("text/x-stickynote")) {
            sticky = Sticky.ReadStickyNote(stringres);
        } else if (contentType.match("TEXT/HTML")) {
            sticky = ReadHtmlNote(stringres);
        } else if (contentType.match("TEXT/PLAIN")) {
            sticky = ReadPlainNote(stringres);
        } else if (contentType.match("multipart/related")) {
// All next is a workaround
// All function need to be rewritten to handle correctly multipart and images
            if (contentType.getParameter("type").equalsIgnoreCase("TEXT/HTML")) {
                sticky = ReadHtmlNote(stringres);
            } else if (contentType.getParameter("type").equalsIgnoreCase("TEXT/PLAIN")) {
                sticky = ReadPlainNote(stringres);
            }
        } else if (contentType.getParameter("BOUNDARY") != null) {
            sticky = ReadHtmlNote(stringres);
        }
        return sticky;
    }

    @NonNull
    private static Sticky ReadHtmlNote(String stringres) {
//        Log.d(TAG,"From server (html):"+stringres);
        Spanned spanres = Html.fromHtml(stringres, Html.FROM_HTML_MODE_LEGACY);
        stringres = Html.toHtml(spanres, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        stringres = stringres.replaceFirst("<p dir=ltr>", "");
        stringres = stringres.replaceFirst("<p dir=\"ltr\">", "");
        stringres = stringres.replaceAll("<p dir=ltr>", "<br>");
        stringres = stringres.replaceAll("<p dir=\"ltr\">", "<br>");
        stringres = stringres.replaceAll("</p>", "");

        return new Sticky(stringres, "BgNone");
    }

    @NonNull
    private static Sticky ReadPlainNote(String stringres) {
//        Log.d(TAG,"From server (plain):"+stringres);
        stringres = stringres.replaceAll("\n", "<br>");

        return new Sticky(stringres, "BgNone");
    }

    // List the colours together with the ids of the option widgets used to select them and the
    // RGB values used as the actual colours.  Doing this means that we do not need so much code
    // in switch statements, etc.

    @NonNull
    public static Sticky ReadStickyNote(@NonNull String stringres) {
        Log.d(TAG, "ReadStickyNote");

        return new Sticky(
                getText(stringres),
                getColor(stringres));
    }

    private static String getText(@NonNull String stringres) {
        Matcher matcherText = patternText.matcher(stringres);
        String text = "";
        if (matcherText.find()) {
            text = matcherText.group(1);
            // Kerio Connect puts CR+LF+space every 78 characters from line 2
            // first line seem to be smaller. We remove these characters
            text = text.replaceAll("\r\n ", "");
            // newline in Kerio is the string (not the character) "\n"
            text = text.replaceAll("\\\\n", "<br>");
        }
        return text;
    }

    @NonNull
    private static String getColor(@NonNull String stringres) {
        Matcher matcherColor = patternColor.matcher(stringres);
        if (matcherColor.find()) {
            String colorName = matcherColor.group(1);
            return ((colorName == null) || colorName.equals("null")) ? "BgNone" : colorName;
        } else {
            return "BgNone";
        }
    }


/*
    public String GetPosition() {
        return Sticky.position;
    }

    public String GetText() {
        return Sticky.text;
    }

    public Colors GetColor() {
        return Sticky.color;
    }

    public void SetText(String text) {
        Sticky.text = text;
    }

    public void SetPosition(String position) {
        Sticky.position = position;
    }

    public void SetColor(Colors color) {
        Sticky.color = color;
    }*/
}
