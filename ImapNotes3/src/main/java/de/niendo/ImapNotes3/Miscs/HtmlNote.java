package de.niendo.ImapNotes3.Miscs;

import android.util.Log;

import androidx.annotation.NonNull;

import de.niendo.ImapNotes3.BuildConfig;
import de.niendo.ImapNotes3.Data.OneNote;

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
        message.setHeader("X-Mailer", BuildConfig.APPLICATION_NAME + " " + BuildConfig.VERSION_NAME);

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
            stringres = StickyNote.ReadStickyNote(stringres).toString();
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



    /*  private void GetPart(@NonNull Part message) throws Exception {
          if (message.isMimeType("text/plain")) {
              Log.d(TAG, "+++ isMimeType text/plain (contentType):" + message.getContentType());
          } else if (message.isMimeType("multipart*//*")) {
            Log.d(TAG, "+++ isMimeType multipart*//* (contentType):" + message.getContentType());
            Object content = message.getContent();
            Multipart mp = (Multipart) content;
            int count = mp.getCount();
            for (int i = 0; i < count; i++) GetPart(mp.getBodyPart(i));
        } else if (message.isMimeType("message/rfc822")) {
            Log.d(TAG, "+++ isMimeType message/rfc822*//* (contentType):" + message.getContentType());
            GetPart((Part) message.getContent());
        } else if (message.isMimeType("image/jpeg")) {
            Log.d(TAG, "+++ isMimeType image/jpeg (contentType):" + message.getContentType());
        } else if (message.getContentType().contains("image/")) {
            Log.d(TAG, "+++ isMimeType image/jpeg (contentType):" + message.getContentType());
        } else {
            Object o = message.getContent();
            if (o instanceof String) {
                Log.d(TAG, "+++ instanceof String");
            } else if (o instanceof InputStream) {
                Log.d(TAG, "+++ instanceof InputStream");
            } else Log.d(TAG, "+++ instanceof ???");
        }
    }
*/

}
