package de.niendo.ImapNotes3;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.NavUtils;

import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import de.niendo.ImapNotes3.Data.OneNote;
import de.niendo.ImapNotes3.Miscs.EditorMenuAdapter;
import de.niendo.ImapNotes3.Miscs.HtmlNote;
import de.niendo.ImapNotes3.Miscs.NDSpinner;
import de.niendo.ImapNotes3.Miscs.StickyNote;
import de.niendo.ImapNotes3.Miscs.Utilities;
import de.niendo.ImapNotes3.Sync.SyncUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;


import javax.mail.Message;

import jp.wasabeef.richeditor.RichEditor;


public class NoteDetailActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //region Intent item names
    public static final String useSticky = "useSticky";
    public static final String selectedNote = "selectedNote";
    public static final String ActivityType = "ActivityType";
    public static final String ActivityTypeEdit = "ActivityTypeEdit";
    public static final String ActivityTypeAdd = "ActivityTypeAdd";
    public static final String ActivityTypeAddShare = "ActivityTypeAddShare";
    //private static final int DELETE_BUTTON = 3;
    private static final int EDIT_BUTTON = 6;
    // --Commented out by Inspection (11/26/16 11:52 PM):private final static int ROOT_AND_NEW = 3;
    private static final String TAG = "IN_NoteDetailActivity";
    private boolean usesticky;
    @NonNull
    private String bgColor = "none";
    //private int realColor = R.id.yellow;
    private String suid; // uid as string
    private RichEditor editText;
    //endregion

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setElevation(0); // or other
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getColor(R.color.ActionBgColor)));
        // Don't display keyboard when on note detail, only if user touches the screen
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        editText = findViewById(R.id.bodyView);

        Intent intent = getIntent();
        String stringres;
        Log.d(TAG, "Check_Action_Send");
        // Get intent, action and MIME type
        String action = intent.getAction();
        String ChangeNote = intent.getStringExtra(ActivityType);
        if (ChangeNote == null)
            ChangeNote = "";
        if (action == null)
            action = "";

        if (action.equals(Intent.ACTION_SEND) && !ChangeNote.equals(ActivityTypeAddShare)) {
            ImapNotes3.showAction(editText, R.string.insert_in_note, R.string.ok,
                    () -> {
                        if (!editText.hasFocus())
                            editText.focusEditor();
                        //editText.insertHTML(getSharedText(intent));
                        editText.loadUrl("javascript:RE.prepareInsert();");
                        editText.loadUrl("javascript:RE.insertHTML('" + getSharedText(intent) + "');");
                    });
        }
        if (ChangeNote.equals(ActivityTypeEdit)) {
            HashMap hm = (HashMap) intent.getSerializableExtra(selectedNote);
            usesticky = intent.getBooleanExtra(useSticky, false);

            if (hm != null) {
                suid = hm.get(OneNote.UID).toString();
                File rootDir = new File(ImapNotes3.GetRootDir(), hm.get(OneNote.ACCOUNT).toString());
                Message message = SyncUtils.ReadMailFromFileRootAndNew(suid, rootDir);
                //Log.d(TAG, "rootDir is null: " + (rootDir == null));
                Log.d(TAG, "rootDir: " + rootDir);
                if (message != null) {
                    if (usesticky) {
                        StickyNote stickyNote = StickyNote.GetStickyFromMessage(message);
                        stringres = stickyNote.text;
                        //String position = sticky.position;
                        bgColor = stickyNote.color;
                    } else {
                        HtmlNote htmlNote = HtmlNote.GetNoteFromMessage(message);
                        stringres = htmlNote.text;
                        bgColor = htmlNote.color;
                    }
                    SetupRichEditor();
                    editText.setHtml(stringres);
                } else {
                    // Entry can not opened..
                    ImapNotes3.ShowMessage(R.string.sync_wait_necessary, null, 3);
                    finish();
                    return;
                }
            } else { // Entry can not opened..
                ImapNotes3.ShowMessage(R.string.Invalid_Message, null, 3);
                finish();
                return;
            }
        } else if (ChangeNote.equals(ActivityTypeAdd)) {   // new entry
            SetupRichEditor();
        } else if (ChangeNote.equals(ActivityTypeAddShare)) {   // new Entry from Share
            SetupRichEditor();
            editText.setHtml(getSharedText(intent));
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

    private void SetupRichEditor() {
        // more functions, maybe use this editor...
        // https://github.com/Andrew-Chen-Wang/RichEditorView/blob/master/Sources/RichEditorView/Resources/editor/rich_editor.js
        // https://www.w3schools.com/howto/tryit.asp?filename=tryhow_js_collapsible_symbol
        editText.setPadding(10, 10, 10, 10);
        //    mEditor.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        editText.setPlaceholder(getString(R.string.placeholder));

/*
        mPreview = (TextView) findViewById(R.id.preview);
        mEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override public void onTextChange(String text) {
                mPreview.setText(text);
            }
        });

*/

        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[10], R.id.action_format, this));
        formatSpinner.setOnItemSelectedListener(this);

        NDSpinner insertSpinner = findViewById(R.id.action_insert);
        insertSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[4], R.id.action_insert, this));
        insertSpinner.setOnItemSelectedListener(this);

        NDSpinner headingSpinner = findViewById(R.id.action_heading);
        headingSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[8], R.id.action_heading, this));
        headingSpinner.setOnItemSelectedListener(this);

        NDSpinner alignmentSpinner = findViewById(R.id.action_alignment);
        alignmentSpinner.setAdapter(new EditorMenuAdapter(NoteDetailActivity.this, R.layout.editor_row, new String[6], R.id.action_alignment, this));
        alignmentSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.action_undo).setOnClickListener(v -> editText.undo());
        findViewById(R.id.action_redo).setOnClickListener(v -> editText.redo());

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
        NDSpinner spinner = (NDSpinner) parent;
        if ((view == null) || (!spinner.initIsDone)) return;
        switch (view.getId()) {
            case R.id.action_removeFormat:
                editText.removeFormat();
                break;
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
                editText.setTextColor(Utilities.getColorByName("html_WhiteSmoke", getApplicationContext()));
                break;
            case R.id.action_txt_color_grey:
                editText.setTextColor(Utilities.getColorByName("html_LightGray", getApplicationContext()));
                break;
            case R.id.action_txt_color_black:
                editText.setTextColor(Utilities.getColorByName("html_Black", getApplicationContext()));
                break;
            case R.id.action_txt_color_red:
                editText.setTextColor(Utilities.getColorByName("html_FireBrick", getApplicationContext()));
                break;
            case R.id.action_txt_color_green:
                editText.setTextColor(Utilities.getColorByName("html_Green", getApplicationContext()));
                break;
            case R.id.action_txt_color_yellow:
                editText.setTextColor(Utilities.getColorByName("html_Yellow", getApplicationContext()));
                break;
            case R.id.action_txt_color_brown:
                editText.setTextColor(Utilities.getColorByName("html_Brown", getApplicationContext()));
                break;
            case R.id.action_txt_color_blue:
                editText.setTextColor(Utilities.getColorByName("html_Blue", getApplicationContext()));
                break;
            case R.id.action_bg_color_white:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_WhiteSmoke", getApplicationContext()));
                break;
            case R.id.action_bg_color_grey:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_LightGray", getApplicationContext()));
                break;
            case R.id.action_bg_color_black:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_Black", getApplicationContext()));
                break;
            case R.id.action_bg_color_red:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_FireBrick", getApplicationContext()));
                break;
            case R.id.action_bg_color_green:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_Green", getApplicationContext()));
                break;
            case R.id.action_bg_color_yellow:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_Yellow", getApplicationContext()));
                break;
            case R.id.action_bg_color_brown:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_Brown", getApplicationContext()));
                break;
            case R.id.action_bg_color_blue:
                editText.setTextBackgroundColor(Utilities.getColorByName("html_Blue", getApplicationContext()));
                break;
            case R.id.action_font_size_1:
                editText.setFontSize(1);
                break;
            case R.id.action_font_size_2:
                editText.setFontSize(2);
                break;
            case R.id.action_font_size_3:
                editText.setFontSize(3);
                break;
            case R.id.action_font_size_4:
                editText.setFontSize(4);
                break;
            case R.id.action_font_size_5:
                editText.setFontSize(5);
                break;
            case R.id.action_font_size_6:
                editText.setFontSize(6);
                break;
            case R.id.action_font_size_7:
                editText.setFontSize(7);
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
                editText.insertImage("image_url", "dachshund");
                break;
            case R.id.action_insert_link:
                editText.insertLink("link_url", "wasabeef");
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

        // for color selection, it does not closes by itself
        NDSpinner formatSpinner = findViewById(R.id.action_format);
        formatSpinner.onDetachedFromWindow();

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // realColor is misnamed.  It is the ID of the radio button widget that chooses the background
    // colour.
    private void ResetColors() {
        editText.setEditorBackgroundColor(Utilities.getColorByName(bgColor, getApplicationContext()));
        editText.setEditorFontColor(getColor(R.color.EditorTxtColor));
        (findViewById(R.id.scrollView)).setBackgroundColor(Utilities.getColorByName(bgColor, getApplicationContext()));
        //invalidateOptionsMenu();
    }

    @SuppressLint("RestrictedApi")
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuBuilder m = (MenuBuilder) menu;
        m.setOptionalIconsVisible(true);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        //MenuItem item = menu.findItem(R.id.color);
        super.onPrepareOptionsMenu(menu);
        //depending on your conditions, either enable/disable
        //item.setVisible(usesticky);
        //menu.findItem(color.id).setChecked(true);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final Intent intent = new Intent();
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_note)
                        .setMessage(R.string.are_you_sure_you_wish_to_delete_the_note)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                            //Log.d(TAG,"We ask to delete Message #"+this.currentNote.get("number"));
                            intent.putExtra("DELETE_ITEM_NUM_IMAP", suid);
                            setResult(ListActivity.DELETE_BUTTON, intent);
                            finish();//finishing activity
                        })
                        .setNegativeButton(R.string.no, null).show();
                return true;
            case R.id.save:
                Save();
                finish();//finishing activity
                return true;
            case R.id.share:
                Share();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.none:
                item.setChecked(true);
                bgColor = "none";
                ResetColors();
                return true;
            case R.id.blue:
                item.setChecked(true);
                bgColor = "blue";
                ResetColors();
                return true;
            case R.id.white:
                item.setChecked(true);
                bgColor = "white";
                ResetColors();
                return true;
            case R.id.black:
                item.setChecked(true);
                bgColor = "black";
                ResetColors();
                return true;
            case R.id.yellow:
                item.setChecked(true);
                bgColor = "yellow";
                ResetColors();
                return true;
            case R.id.pink:
                item.setChecked(true);
                bgColor = "pink";
                ResetColors();
                return true;
            case R.id.green:
                item.setChecked(true);
                bgColor = "green";
                ResetColors();
                return true;
            case R.id.brown:
                item.setChecked(true);
                bgColor = "brown";
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
        intent.putExtra(ListActivity.EDIT_ITEM_NUM_IMAP, suid);
        Log.d(TAG, "Save html: " + editText.getHtml());
        intent.putExtra(ListActivity.EDIT_ITEM_TXT, editText.getHtml());
        intent.putExtra(ListActivity.EDIT_ITEM_COLOR, bgColor);
        setResult(NoteDetailActivity.EDIT_BUTTON, intent);
    }

    private void Share() {
        Log.d(TAG, "Share");
        Intent sendIntent = new Intent();
        String text = editText.getHtml();
        Spanned html = Html.fromHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        String title = html.toString().split("\n", 2)[0];

        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/html");
        sendIntent.putExtra(Intent.EXTRA_TEXT, html);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.shared_note_from) + Utilities.ApplicationName + ": " + title);

        String directory = getApplicationContext().getCacheDir().toString();
        File outfile = new File(directory, title.replaceAll("[#:/]", "") + ".html");
        Log.d(TAG, "Share Note: " + outfile);
        try (OutputStream str = new FileOutputStream(outfile)) {
            str.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            ImapNotes3.ShowMessage(R.string.share_file_error + e.toString(), editText, 2);
            e.printStackTrace();
        }

        Uri logUri =
                FileProvider.getUriForFile(
                        getApplicationContext(),
                        Utilities.PackageName, outfile);
        sendIntent.putExtra(Intent.EXTRA_STREAM, logUri);

        Intent shareIntent = Intent.createChooser(sendIntent, title);
        startActivity(shareIntent);
    }

    private String getSharedText(Intent intent) {
        String html = "";
        // Share: Receive Data as new message
        String strAction = intent.getAction();
        if ((strAction != null) && strAction.equals(Intent.ACTION_SEND)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String type = intent.getType();
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            Log.d(TAG, "Share 1");
            if (uri != null) {
                Log.d(TAG, "Share 2");
                BufferedInputStream bufferedInputStream = null;
                try {
                    bufferedInputStream =
                            new BufferedInputStream(getContentResolver().openInputStream(uri));
                    byte[] contents = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = bufferedInputStream.read(contents)) != -1) {
                        html += new String(contents, 0, bytesRead);
                    }
                    Log.d(TAG, "Share 3" + html);
                    bufferedInputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Share 4" + sharedText);
                if (subject != null) {
                    subject = "<b>" + subject + "</b><br>";
                } else subject = "";
                if (sharedText != null) {
                    if (type.equals("text/html")) {
                        editText.setHtml(subject + sharedText);
                    } else if (type.startsWith("text/")) {
                        html = Html.toHtml(new SpannedString(Html.fromHtml(subject + sharedText, Html.FROM_HTML_MODE_LEGACY)), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);

                    } else if (type.startsWith("image/")) {
                        // toDo
                    }
                }
            }

        }
        return html;
    }
}
