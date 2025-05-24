package org.woheller69.ImapLocate.Miscs;

import androidx.annotation.NonNull;

public class ImapNotesResult {

    public final int returnCode;
    @NonNull
    public final String errorMessage;
    public final Long UIDValidity;

    public ImapNotesResult(int returnCode,
                           String errorMessage,
                           long UIDValidity) {
        this.returnCode = returnCode;
        this.errorMessage = errorMessage;
        this.UIDValidity = UIDValidity;
    }

}
