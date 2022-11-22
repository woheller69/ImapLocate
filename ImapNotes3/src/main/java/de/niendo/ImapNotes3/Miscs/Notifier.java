package de.niendo.ImapNotes3.Miscs;

import android.content.Context;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class Notifier {
    public static void Show(@StringRes int resId,
                            Context context,
                            int durationSeconds
    ) {
        Show(context.getResources().getString(resId), context, durationSeconds);
    }

    public static void Show(String message,
                            Context context,
                            int durationSeconds

    ) {
        final Toast tag = Toast.makeText(context, message, Toast.LENGTH_LONG);

        tag.show();
        new CountDownTimer((long) durationSeconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                tag.show();
            }

            public void onFinish() {
                tag.show();
            }
        }.start();
    }
}
