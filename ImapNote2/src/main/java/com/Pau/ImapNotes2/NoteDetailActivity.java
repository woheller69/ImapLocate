package com.Pau.ImapNotes2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;


import com.Pau.ImapNotes2.Miscs.EditorMenuAdapter;
import com.Pau.ImapNotes2.Miscs.NDSpinner;
import com.Pau.ImapNotes2.Miscs.Sticky;
import com.Pau.ImapNotes2.Miscs.Notifier;
import com.Pau.ImapNotes2.Sync.SyncUtils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;


import javax.mail.Message;
import javax.mail.internet.ContentType;

import jp.wasabeef.richeditor.RichEditor;


public class NoteDetailActivity extends Activity implements AdapterView.OnItemSelectedListener {

    //region Intent item names
    public static final String useSticky = "useSticky";
    public static final String selectedNote = "selectedNote";
    public static final String ActivityType = "ActivityType";
    public static final String ActivityTypeEdit = "ActivityTypeEdit";
    public static final String ActivityTypeAdd = "ActivityTypeAdd";
    //private static final int DELETE_BUTTON = 3;
    private static final int EDIT_BUTTON = 6;
    // --Commented out by Inspection (11/26/16 11:52 PM):private final static int ROOT_AND_NEW = 3;
    private static final String TAG = "IN_NoteDetailActivity";
    private boolean usesticky;
    @NonNull
    private Colors color = Colors.YELLOW;
    //private int realColor = R.id.yellow;
    private String suid; // uid as string
    private RichEditor editText;
    //endregion

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        // Don't display keyboard when on note detail, only if user touches the screen
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        Bundle extras = getIntent().getExtras();
        Sticky sticky;
        String stringres;
        String ChangeNote = extras.getString(ActivityType);
        if (ChangeNote.equals(ActivityTypeEdit)) {
            HashMap hm = (HashMap) extras.getSerializable(selectedNote);
            usesticky = extras.getBoolean(useSticky);

            if (hm != null) {
                suid = hm.get("uid").toString();
                if (Integer.parseInt(suid) > 0) {
                    File rootDir = new File(getApplicationContext().getFilesDir(),
                            Listactivity.imapNotes2Account.accountName);
                    Message message = SyncUtils.ReadMailFromFileRootAndNew(suid, rootDir);
                    //Log.d(TAG, "rootDir is null: " + (rootDir == null));
                    Log.d(TAG, "rootDir: " + rootDir);
                    if (message != null) {
                        if (usesticky) {
                            sticky = GetStickyFromMessage(message);
                            stringres = sticky.text;
                            //String position = sticky.position;
                            color = sticky.color;
                        } else {
                            stringres = GetHtmlFromMessage(message);
                        }
                        editText = findViewById(R.id.bodyView);
                        SetupRichEditor(editText);
                        editText.setHtml(stringres);
                    } else {
                        // Entry can not opened..
                        Notifier.Show(R.string.sync_necessary, getApplicationContext(), 1);
                        finish();
                        return;
                    }
                } else {
                    // Entry can not opened..
                    Notifier.Show(R.string.Waiting_for_sync, getApplicationContext(), 1);
                    finish();
                    return;
                }
            } else { // Entry can not opened..
                Notifier.Show(R.string.Invalid_Message, getApplicationContext(), 1);
                finish();
                return;
            }
        } else if (ChangeNote.equals(ActivityTypeAdd)) {   // neuer Eintrag
            color = Colors.YELLOW;
            editText = findViewById(R.id.bodyView);
            SetupRichEditor(editText);
        }

/*        // TODO: Watch for changes so that we can auto save.
        // See http://stackoverflow.com/questions/7117209/how-to-know-key-presses-in-edittext#14251047
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //here is your code
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Work in progess
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Work in progess
            }

        });
        */
        ResetColors();
    }

    private void SetupRichEditor(@NonNull final RichEditor mEditor) {
        //mEditor = (RichEditor) findViewById(R.id.editor);
        //mEditor.setEditorHeight(200);
        //mEditor.setEditorFontSize(22);
        //mEditor.setEditorFontColor(Color.RED);

        //mEditor.setEditorBackgroundColor(Color.BLUE);
        //mEditor.setBackgroundColor(Color.BLUE);
        //mEditor.setBackgroundResource(R.drawable.bg);
        mEditor.setPadding(10, 10, 10, 10);
        //    mEditor.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        mEditor.setPlaceholder("Insert text here...");

/*
        mPreview = (TextView) findViewById(R.id.preview);
        mEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override public void onTextChange(String text) {
                mPreview.setText(text);
            }
        });

*/

        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[6], R.id.action_format));
        formatSpinner.setOnItemSelectedListener(this);

        NDSpinner insertSpinner = findViewById(R.id.action_insert);
        insertSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[7], R.id.action_insert));
        insertSpinner.setOnItemSelectedListener(this);

        NDSpinner headingSpinner = findViewById(R.id.action_heading);
        headingSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_heading));
        headingSpinner.setOnItemSelectedListener(this);

        NDSpinner txtColorSpinner = findViewById(R.id.action_txt_color);
        txtColorSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_txt_color));
        txtColorSpinner.setOnItemSelectedListener(this);

        NDSpinner bgColorSpinner = findViewById(R.id.action_bg_color);
        bgColorSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_bg_color));
        bgColorSpinner.setOnItemSelectedListener(this);

        NDSpinner alignmentSpinner = findViewById(R.id.action_alignment);
        alignmentSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[6], R.id.action_alignment));
        alignmentSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.action_undo).setOnClickListener(v -> mEditor.undo());
        findViewById(R.id.action_redo).setOnClickListener(v -> mEditor.redo());

    }

/*
    // TODO: delete this?
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        //Boolean isClicked = true;
    }
*/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (view.getId()) {
            case R.id.action_bold:
                editText.setBold();
                break;
            case R.id.action_italic:
                editText.setItalic();
                break;
            case R.id.action_subscript:
                editText.setSubscript();
                break;
            case R.id.action_superscript:
                editText.setSuperscript();
                break;
            case R.id.action_strikethrough:
                editText.setStrikeThrough();
                break;
            case R.id.action_underline:
                editText.setUnderline();
                break;
            case R.id.action_heading1:
                editText.setHeading(1);
                break;
            case R.id.action_heading2:
                editText.setHeading(2);
                break;
            case R.id.action_heading3:
                editText.setHeading(3);
                break;
            case R.id.action_heading4:
                editText.setHeading(4);
                break;
            case R.id.action_heading5:
                editText.setHeading(5);
                break;
            case R.id.action_heading6:
                editText.setHeading(6);
                break;
            case R.id.action_txt_color_white:
                editText.setTextColor(Color.WHITE);
                break;
            case R.id.action_txt_color_grey:
                editText.setTextColor(Color.GRAY);
                break;
            case R.id.action_txt_color_black:
                editText.setTextColor(Color.BLACK);
                break;
            case R.id.action_txt_color_red:
                editText.setTextColor(Color.RED);
                break;
            case R.id.action_txt_color_green:
                editText.setTextColor(Color.GREEN);
                break;
            case R.id.action_txt_color_yellow:
                editText.setTextColor(Color.YELLOW);
                break;
            case R.id.action_txt_color_brown:
                editText.setTextColor(Color.MAGENTA);
                break;
            case R.id.action_txt_color_blue:
                editText.setTextColor(Color.BLUE);
                break;
            case R.id.action_bg_color_white:
                editText.setTextBackgroundColor(Color.WHITE);
                break;
            case R.id.action_bg_color_grey:
                editText.setTextBackgroundColor(Color.GRAY);
                break;
            case R.id.action_bg_color_black:
                editText.setTextBackgroundColor(Color.BLACK);
                break;
            case R.id.action_bg_color_red:
                editText.setTextBackgroundColor(Color.RED);
                break;
            case R.id.action_bg_color_green:
                editText.setTextBackgroundColor(Color.GREEN);
                break;
            case R.id.action_bg_color_yellow:
                editText.setTextBackgroundColor(Color.YELLOW);
                break;
            case R.id.action_bg_color_brown:
                editText.setTextBackgroundColor(Color.MAGENTA);
                break;
            case R.id.action_bg_color_blue:
                editText.setTextBackgroundColor(Color.BLUE);
                break;
            case R.id.action_indent:
                editText.setIndent();
                break;
            case R.id.action_outdent:
                editText.setOutdent();
                break;
            case R.id.action_align_left:
                editText.setAlignLeft();
                break;
            case R.id.action_align_center:
                editText.setAlignCenter();
                break;
            case R.id.action_align_right:
                editText.setAlignRight();
                break;
            case R.id.action_blockquote:
                editText.setBlockquote();
                break;
            case R.id.action_insert_bullets:
                editText.setBullets();
                break;
            case R.id.action_insert_numbers:
                editText.setNumbers();
                break;
            case R.id.action_insert_image:
                editText.insertImage("http://www.1honeywan.com/dachshund/image/7.21/7.21_3_thumb.JPG",
                        "dachshund");
                break;
            case R.id.action_insert_link:
                editText.insertLink("https://github.com/wasabeef", "wasabeef");
                break;
            case R.id.action_insert_checkbox:
                editText.insertTodo();
                break;
            case R.id.action_insert_star:
                editText.loadUrl("javascript:RE.prepareInsert();");
                editText.loadUrl("javascript:RE.insertHTML('&#11088;');");
                break;
            case R.id.action_insert_question:
                editText.loadUrl("javascript:RE.prepareInsert();");
                editText.loadUrl("javascript:RE.insertHTML('&#10067;');");
                break;
            case R.id.action_insert_exclamation:
                editText.loadUrl("javascript:RE.prepareInsert();");
                editText.loadUrl("javascript:RE.insertHTML('&#10071;');");
                break;
            case R.id.action_insert_hline:
                editText.loadUrl("javascript:RE.prepareInsert();");
                editText.loadUrl("javascript:RE.insertHTML('<hr>');");
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // realColor is misnamed.  It is the ID of the radio button widget that chooses the background
    // colour.
    private void ResetColors() {
/*
        EditText bodyView = (EditText) findViewById(R.id.bodyView);
        bodyView.setBackgroundColor(Color.TRANSPARENT);
        bodyView.setTextColor(Color.BLACK);
*/
        (findViewById(R.id.scrollView)).setBackgroundColor(color.colorCode);
        //realColor = color.id;
        invalidateOptionsMenu();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.color);
        super.onPrepareOptionsMenu(menu);
        //depending on your conditions, either enable/disable
        item.setVisible(usesticky);
        Log.d(TAG, "color.id: " + color.id);
        Log.d(TAG, "mfi: " + (menu.findItem(color.id) == null));
        if (BuildConfig.DEBUG && (color == null)) {
            throw new AssertionError("color is null");
        }

        menu.findItem(color.id).setChecked(true);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Intent intent = new Intent();
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setTitle("Delete note")
                        .setMessage("Are you sure you wish to delete the note?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            //Log.d(TAG,"We ask to delete Message #"+this.currentNote.get("number"));
                            intent.putExtra("DELETE_ITEM_NUM_IMAP", suid);
                            setResult(Listactivity.DELETE_BUTTON, intent);
                            finish();//finishing activity
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            case R.id.save:
                Save();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.blue:
            case R.id.white:
            case R.id.yellow:
            case R.id.pink:
            case R.id.green:
                item.setChecked(true);
                color = Colors.fromId(itemId);
                ResetColors();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Note that this function does not save the note to permanent storage it just passes it back to
     * the calling activity to be saved in whatever fashion it that activity wishes.
     */
    private void Save() {
        Log.d(TAG, "Save");
        Intent intent = new Intent();
        intent.putExtra(Listactivity.EDIT_ITEM_NUM_IMAP, suid);
        Log.d(TAG, "Save html: " + ((RichEditor) findViewById(R.id.bodyView)).getHtml());
        intent.putExtra(Listactivity.EDIT_ITEM_TXT,
                ((RichEditor) findViewById(R.id.bodyView)).getHtml());
        if (!usesticky) {
            Log.d(TAG, "not sticky so set color to none");
            color = Colors.NONE;
        }
        intent.putExtra(Listactivity.EDIT_ITEM_COLOR, color);
        setResult(NoteDetailActivity.EDIT_BUTTON, intent);
        finish();//finishing activity

    }

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
            stringres = SyncUtils.ReadStickyNote(stringres).toString();
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

    @Nullable
    private Sticky GetStickyFromMessage(@NonNull Message message) {
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
            sticky = SyncUtils.ReadStickyNote(stringres);
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
    @NonNull
    private Sticky ReadHtmlNote(String stringres) {
//        Log.d(TAG,"From server (html):"+stringres);
        Spanned spanres = Html.fromHtml(stringres, Html.FROM_HTML_MODE_LEGACY);
        stringres = Html.toHtml(spanres, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        stringres = stringres.replaceFirst("<p dir=ltr>", "");
        stringres = stringres.replaceFirst("<p dir=\"ltr\">", "");
        stringres = stringres.replaceAll("<p dir=ltr>", "<br>");
        stringres = stringres.replaceAll("<p dir=\"ltr\">", "<br>");
        stringres = stringres.replaceAll("</p>", "");

        return new Sticky(stringres, Colors.NONE);
    }

    @NonNull
    private Sticky ReadPlainNote(String stringres) {
//        Log.d(TAG,"From server (plain):"+stringres);
        stringres = stringres.replaceAll("\n", "<br>");

        return new Sticky(stringres, Colors.NONE);
    }

    // List the colours together with the ids of the option widgets used to select them and the
    // RGB values used as the actual colours.  Doing this means that we do not need so much code
    // in switch statements, etc.
    public enum Colors {
        BLUE(R.id.blue, 0xFFA6CAFD),
        WHITE(R.id.white, 0xFFFFFFFF),
        YELLOW(R.id.yellow, 0xFFFFFFCC),
        PINK(R.id.pink, 0xFFFFCCCC),
        GREEN(R.id.green, 0xFFCCFFCC),
        NONE(R.id.white, 0xFFFFFFFF);

        public final int id;
        public final int colorCode;

        Colors(int id,
               int colorCode) {
            this.id = id;
            this.colorCode = colorCode;
        }

        @NonNull
        public static Colors fromId(int id) {

            for (Colors color : Colors.values()) {
                if (color.id == id)
                    return color;
            }
            throw new IllegalArgumentException("id not found in Colors: " + id);
        }


    }

// --Commented out by Inspection START (12/2/16 8:50 PM):
//    private void WriteMailToFile(@NonNull String suid, @NonNull Message message) {
//        String directory = getApplicationContext().getFilesDir() + "/" +
//                Listactivity.imapNotes2Account.accountName;
//        try {
//            File outfile = new File(directory, suid);
//            OutputStream str = new FileOutputStream(outfile);
//            message.writeTo(str);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
// --Commented out by Inspection STOP (12/2/16 8:50 PM)


}
