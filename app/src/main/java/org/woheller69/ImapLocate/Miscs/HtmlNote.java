package org.woheller69.ImapLocate.Miscs;

import android.util.Log;

import androidx.annotation.NonNull;

import org.woheller69.ImapLocate.Data.OneNote;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;


public class HtmlNote {

    private static final String TAG = "IN_HtmlNote";
    private static final String ColorBgStr = "background-color:";
    private static final Pattern patternBodyBgColor = Pattern.compile("background-color:(.*?);", Pattern.MULTILINE);

    public final String text;
    // --Commented out by Inspection (11/26/16 11:50 PM):private final String position;
    @NonNull
    public final String color;

    public HtmlNote(String text,
                    @NonNull String color) {
        this.text = text;
        this.color = color;
    }

    @NonNull
    public static Message GetMessageFromNote(@NonNull OneNote note, String noteBody) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setHeader("X-Uniform-Type-Identifier", "com.apple.mail-note");
        UUID uuid = UUID.randomUUID();
        message.setHeader("X-Universally-Unique-Identifier", uuid.toString());
        message.setHeader("X-Mailer", Utilities.FullApplicationName);

/*
            <!DOCTYPE html>
            <html>
            <body style="background-color:khaki;">
            </body>
            </html>
*/
        Document doc = Jsoup.parse(noteBody, "utf-8");
        String bodyStyle = doc.select("body").attr("style");
        doc.outputSettings().prettyPrint(false);
        if (!note.GetBgColor().equals("none")) {
            Matcher matcherColor = HtmlNote.patternBodyBgColor.matcher(bodyStyle);
            String BgColorStr = "background-color:" + note.GetBgColor() + ";";
            if (matcherColor.find()) {
                bodyStyle = matcherColor.replaceFirst(BgColorStr);
            } else {
                bodyStyle = BgColorStr + bodyStyle;
            }

            doc.select("body").attr("style", bodyStyle);
        }
/*          body = body.replaceFirst("<p dir=ltr>", "<div>");
            body = body.replaceFirst("<p dir=\"ltr\">", "<div>");
            body = body.replaceAll("<p dir=ltr>", "<div><br></div><div>");
            body = body.replaceAll("<p dir=\"ltr\">", "<div><br></div><div>");
            body = body.replaceAll("</p>", "</div>");
            body = body.replaceAll("<br>\n", "</div><div>");
 */

        message.setText(doc.toString(), "utf-8", "html");
        message.setFlag(Flags.Flag.SEEN, true);

        return (message);
    }

    @NonNull
    public static HtmlNote GetNoteFromMessage(@NonNull Message message) {
        ContentType contentType;
        String stringres = "";
        //InputStream iis = null;
        //String charset;
        try {
            Log.d(TAG, "message :" + message);
            contentType = new ContentType(message.getContentType());
            String charset = contentType.getParameter("charset");
            InputStream iis = (InputStream) message.getContent();
            stringres = IOUtils.toString(iis, charset);
            iis.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "Exception GetNoteFromMessage:");
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }

        return new HtmlNote(
                getText(stringres),
                getColor(stringres));
    }

    private static String getText(@NonNull String stringres) {
        return stringres;
    }

    @NonNull
    private static String getColor(@NonNull String stringres) {
        Document doc = Jsoup.parse(stringres, "utf-8");
        String bodyStyle = doc.select("body").attr("style");
        Matcher matcherColor = patternBodyBgColor.matcher(bodyStyle);
        if (matcherColor.find()) {
            String colorName = matcherColor.group(1).toLowerCase(Locale.ROOT);
            return ((colorName.isEmpty()) || colorName.equals("null") || colorName.equals("transparent")) ? "none" : colorName;
        } else {
            return "none";
        }
    }

}
