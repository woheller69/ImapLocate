package com.Pau.ImapNotes2.Miscs;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.internet.ContentType;


public class HtmlNote {

    public static final String[] ColorHeader = {"SIO-Label"};
    private static final String TAG = "IN_HtmlNote";
    private static final Pattern patternColor = Pattern.compile("bgcolor=(.*?);", Pattern.MULTILINE);

    public final String text;
    // --Commented out by Inspection (11/26/16 11:50 PM):private final String position;
    @NonNull
    public final String color;

    public HtmlNote(String text,
                    @NonNull String color) {
        this.text = text;
        // this.position = position;
        this.color = color;
    }

    @Nullable
    public static HtmlNote GetNoteFromMessage(@NonNull Message message) {
        ContentType contentType = null;
        String stringres = "";
        //InputStream iis = null;
        String colorHeader = "BgNone";
        //String charset;
        try {
            Log.d(TAG, "message :" + message);
            contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            InputStream iis = (InputStream) message.getContent();
            stringres = IOUtils.toString(iis, charset);
            colorHeader = message.getMatchingHeaders(ColorHeader).nextElement().getValue();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "Exception GetNoteFromMessage:");
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }

        return new HtmlNote(
                getText(stringres),
                getColor(colorHeader));
    }

/*
    @Nullable
    private String GetHtmlFromMessage(@NonNull Message message) {
        ContentType contentType = null;
        String stringres = "";
        try {
            Log.d(TAG, "message :" + message);
            contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            InputStream iis = (InputStream) message.getContent();
            stringres = IOUtils.toString(iis, charset);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "Exception GetHtmlFromMessage:");
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
        if (contentType.match("text/x-stickynote")) {
            stringres = Sticky.ReadStickyNote(stringres).toString();
//        } else if (contentType.match("TEXT/HTML")) {
        } else if (contentType.match("TEXT/PLAIN")) {
            Spanned spanres = Html.fromHtml(stringres, Html.FROM_HTML_MODE_LEGACY);
            stringres = Html.toHtml(spanres, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else if (contentType.match("multipart/related")) {
// All next is a workaround
// All function need to be rewritten to handle correctly multipart and images
            // if (contentType.getParameter("type").equalsIgnoreCase("TEXT/HTML")) {          } else
            if (contentType.getParameter("type").equalsIgnoreCase("TEXT/PLAIN")) {
                Spanned spanres = Html.fromHtml(stringres, Html.FROM_HTML_MODE_LEGACY);
                stringres = Html.toHtml(spanres, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            }
            //} else if (contentType.getParameter("BOUNDARY") != null) {
        }
        return stringres;
    }


    private static String getPosition(String stringres) {

        Matcher matcherPosition = patternPosition.matcher(stringres);
        return matcherPosition.find() ?
                matcherPosition.group(1) :
                "";
    }*/

    private static String getText(@NonNull String stringres) {
        return stringres;
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
        return HtmlNote.position;
    }

    public String GetText() {
        return HtmlNote.text;
    }

    public Colors GetColor() {
        return HtmlNote.color;
    }

    public void SetText(String text) {
        HtmlNote.text = text;
    }

    public void SetPosition(String position) {
        HtmlNote.position = position;
    }

    public void SetColor(Colors color) {
        HtmlNote.color = color;
    }*/
}
