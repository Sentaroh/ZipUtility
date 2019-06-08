package com.sentaroh.android.ZipUtility.Log;

import com.sentaroh.android.Utilities.LogUtil.CommonLogFileListDialogFragment;

import android.os.Bundle;

public class LogFileListDialogFragment extends CommonLogFileListDialogFragment{
    public static LogFileListDialogFragment newInstance(boolean retainInstance, String title, String send_msg, String enable_msg, String send_subject) {
        LogFileListDialogFragment frag = new LogFileListDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("retainInstance", retainInstance);
        bundle.putBoolean("showSaveButton", true);
        bundle.putString("title", title);
        bundle.putString("msgtext", send_msg);
        bundle.putString("enableMsg", enable_msg);
        bundle.putString("subject", send_subject);
        frag.setArguments(bundle);
        return frag;
    }

}
