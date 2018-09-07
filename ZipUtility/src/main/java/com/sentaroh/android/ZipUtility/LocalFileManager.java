package com.sentaroh.android.ZipUtility;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.sentaroh.android.Utilities.BufferedZipFile;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SafFile;
import com.sentaroh.android.Utilities.SafManager;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities.Widget.CustomTextView;
import com.sentaroh.android.Utilities.ZipUtil;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.sentaroh.android.ZipUtility.Constants.APPLICATION_TAG;
import static com.sentaroh.android.ZipUtility.Constants.APP_SPECIFIC_DIRECTORY;
import static com.sentaroh.android.ZipUtility.Constants.ENCODING_NAME_UTF8;
import static com.sentaroh.android.ZipUtility.Constants.IO_AREA_SIZE;
import static com.sentaroh.android.ZipUtility.Constants.MIME_TYPE_TEXT;
import static com.sentaroh.android.ZipUtility.Constants.MIME_TYPE_ZIP;
import static com.sentaroh.android.ZipUtility.Constants.WORK_DIRECTORY;

public class LocalFileManager {
    private GlobalParameters mGp = null;

    private FragmentManager mFragmentManager = null;
    private CommonDialog mCommonDlg = null;

    private Context mContext;
    private ActivityMain mActivity = null;

    private ListView mTreeFilelistView = null;
    private CustomTreeFilelistAdapter mTreeFilelistAdapter = null;
    private TextView mLocalViewMsg = null;

    private View mMainView = null;
    private Handler mUiHandler = null;
    private String mMainFilePath = "";

    private Button mFileListUp, mFileListTop;
    private CustomTextView mCurrentDirectory;
    private TextView mFileEmpty;
    private LinearLayout mMainDialogView = null;

    private CommonUtilities mUtil = null;
//	public void setOptions(boolean debug, int mln) {
//		mGlblParms.debugEnabled=debug;
//	}

    public LocalFileManager(GlobalParameters gp, ActivityMain a, FragmentManager fm, LinearLayout mv) {
        mGp = gp;
        mActivity = a;
        mCommonDlg = new CommonDialog(a, fm);
        mUiHandler = new Handler();
        mContext = a.getApplicationContext();

        mFragmentManager = fm;
        mMainFilePath = mGp.internalRootDirectory;
        mUtil = new CommonUtilities(mContext, "LocalFolder", gp, mCommonDlg);

        mMainView = mv;
        initViewWidget();
        mTreeFilelistAdapter = new CustomTreeFilelistAdapter(mActivity, false, true);

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(final Context c, final Object[] o) {
//				if (!mGp.externalRootDirectory.equals(GlobalParameters.STORAGE_STATUS_UNMOUNT)) {
//					if (mGp.safMgr.getSdcardRootSafFile()==null) mActivity.startSdcardPicker();
//				}
            }

            @Override
            public void negativeResponse(Context c, final Object[] o) {
            }
        });
        createFileList(mMainFilePath, ntfy);

    }

    public void reInitView() {
        ArrayList<TreeFilelistItem> fl = mTreeFilelistAdapter.getDataList();
        int v_pos_fv = 0, v_pos_top = 0;
        v_pos_fv = mTreeFilelistView.getFirstVisiblePosition();
        if (mTreeFilelistView.getChildAt(0) != null)
            v_pos_top = mTreeFilelistView.getChildAt(0).getTop();

        mTreeFilelistAdapter = new CustomTreeFilelistAdapter(mActivity, false, true);

        mTreeFilelistAdapter.setDataList(fl);
        mTreeFilelistView.setAdapter(mTreeFilelistAdapter);
        mTreeFilelistView.setSelectionFromTop(v_pos_fv, v_pos_top);
        mTreeFilelistAdapter.notifyDataSetChanged();
    }

    public boolean isFileListSortAscendant() {
        if (mTreeFilelistAdapter != null) return mTreeFilelistAdapter.isSortAscendant();
        else return true;
    }

    public void refreshFileList() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        String w_curr_dir = "";
        if (mCurrentDirectory.getText().toString().equals("/")) {
            w_curr_dir = mLocalStorage.getSelectedItem().toString();
        } else {
            w_curr_dir = mLocalStorage.getSelectedItem().toString() + mCurrentDirectory.getText().toString();
        }
        final String curr_dir = w_curr_dir;

        int pos_x = mTreeFilelistView.getFirstVisiblePosition();
        int pos_y = mTreeFilelistView.getChildAt(0) == null ? 0 : mTreeFilelistView.getChildAt(0).getTop();
        ArrayList<TreeFilelistItem> prev_fl = mTreeFilelistAdapter.getDataList();
        boolean prev_selected = mTreeFilelistAdapter.isItemSelected();

        createFileList(curr_dir, null);

        if (prev_selected) {
            ArrayList<TreeFilelistItem> new_fl = mTreeFilelistAdapter.getDataList();
            for (TreeFilelistItem prev_item : prev_fl) {
                if (prev_item.isChecked()) {
                    for (TreeFilelistItem new_item : new_fl) {
                        if (prev_item.getName().equals(new_item.getName())) {
                            new_item.setChecked(true);
                            break;
                        }
                    }
                }
            }
            mTreeFilelistAdapter.notifyDataSetChanged();
        }

        mTreeFilelistView.setSelectionFromTop(pos_x, pos_y);

    }

    ;

    private Spinner mLocalStorage;

    private LinearLayout mDialogProgressSpinView = null;
    private TextView mDialogProgressSpinMsg1 = null;
    private TextView mDialogProgressSpinMsg2 = null;
    private Button mDialogProgressSpinCancel = null;

    private LinearLayout mDialogMessageView = null;
    private TextView mDialogMessageTitle = null;
    private TextView mDialogMessageBody = null;
    private Button mDialogMessageOk = null;
    private Button mDialogMessageClose = null;
    private Button mDialogMessageCancel = null;

    private LinearLayout mDialogConfirmView = null;
    private TextView mDialogConfirmMsg = null;
    private Button mDialogConfirmCancel = null;

    private Button mDialogConfirmYes = null;
    private Button mDialogConfirmYesAll = null;
    private Button mDialogConfirmNo = null;
    private Button mDialogConfirmNoAll = null;

    private LinearLayout mContextButton = null;

    private ImageButton mContextButtonPaste = null;
    private LinearLayout mContextButtonPasteView = null;
    private ImageButton mContextButtonCopy = null;
    private LinearLayout mContextButtonCopyView = null;
    private ImageButton mContextButtonCut = null;
    private LinearLayout mContextButtonCutView = null;

    private LinearLayout mContextButtonCreateView = null;
    private ImageButton mContextButtonCreate = null;
    private LinearLayout mContextButtonShareView = null;
    private ImageButton mContextButtonShare = null;

    private LinearLayout mContextButtonRenameView = null;
    private ImageButton mContextButtonRename = null;

    private ImageButton mContextButtonArchive = null;
    private LinearLayout mContextButtonArchiveView = null;

    private ImageButton mContextButtonDelete = null;
    private LinearLayout mContextButtonDeleteView = null;
    private ImageButton mContextButtonSelectAll = null;
    private LinearLayout mContextButtonSelectAllView = null;
    private ImageButton mContextButtonUnselectAll = null;
    private LinearLayout mContextButtonUnselectAllView = null;

    public void showLocalFileView(boolean show) {
        LinearLayout lv = (LinearLayout) mMainView.findViewById(R.id.local_file_view);
        if (show) lv.setVisibility(LinearLayout.VISIBLE);
        else lv.setVisibility(LinearLayout.INVISIBLE);
    }

    private void initViewWidget() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mContextButton = (LinearLayout) mMainView.findViewById(R.id.context_view_local_file);

        LinearLayout lv = (LinearLayout) mMainView.findViewById(R.id.local_file_view);
        lv.setVisibility(LinearLayout.VISIBLE);

        mMainDialogView = (LinearLayout) mMainView.findViewById(R.id.main_dialog_view);
        mMainDialogView.setVisibility(LinearLayout.VISIBLE);
        mTreeFilelistView = (ListView) mMainView.findViewById(R.id.local_file_list);
        mFileEmpty = (TextView) mMainView.findViewById(R.id.local_file_empty);
        mFileEmpty.setVisibility(TextView.GONE);
        mTreeFilelistView.setVisibility(ListView.VISIBLE);

        mLocalViewMsg = (TextView) mMainView.findViewById(R.id.local_file_msg);

        mLocalStorage = (Spinner) mMainView.findViewById(R.id.local_file_storage_spinner);
        CommonUtilities.setSpinnerBackground(mActivity, mLocalStorage, mGp.themeIsLight);
        final CustomSpinnerAdapter stg_adapter =
                new CustomSpinnerAdapter(mActivity, android.R.layout.simple_spinner_item);
        stg_adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        stg_adapter.add(mGp.internalRootDirectory);
        if (!mGp.externalRootDirectory.equals(GlobalParameters.STORAGE_STATUS_UNMOUNT))
            stg_adapter.add(mGp.externalRootDirectory);
//		if (!mGp.safMgr.getUsbFileSystemDirectory().equals(SafFileManager.UNKNOWN_USB_FS_DIRECTORY))
//			stg_adapter.add(mGp.safMgr.getUsbFileSystemDirectory());
        mLocalStorage.setAdapter(stg_adapter);
        mLocalStorage.setSelection(0, false);
        if (stg_adapter.getCount() > 1) mLocalStorage.setEnabled(true);
        else mLocalStorage.setEnabled(false);

        mFileListUp = (Button) mMainView.findViewById(R.id.local_file_up_btn);
        if (mGp.themeIsLight)
            mFileListUp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_dark, 0, 0, 0);
        else
            mFileListUp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_light, 0, 0, 0);
        mFileListTop = (Button) mMainView.findViewById(R.id.local_file_top_btn);
        if (mGp.themeIsLight)
            mFileListTop.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_dark, 0, 0, 0);
        else
            mFileListTop.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_light, 0, 0, 0);
        mCurrentDirectory = (CustomTextView) mMainView.findViewById(R.id.local_file_filepath);
        mCurrentDirectory.setTextColor(mGp.themeColorList.text_color_primary);

        mDialogProgressSpinView = (LinearLayout) mMainView.findViewById(R.id.main_dialog_progress_spin_view);
        mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
        mDialogProgressSpinMsg1 = (TextView) mMainView.findViewById(R.id.main_dialog_progress_spin_syncprof);
        mDialogProgressSpinMsg2 = (TextView) mMainView.findViewById(R.id.main_dialog_progress_spin_syncmsg);
        mDialogProgressSpinCancel = (Button) mMainView.findViewById(R.id.main_dialog_progress_spin_btn_cancel);

        mDialogMessageView = (LinearLayout) mMainView.findViewById(R.id.main_dialog_message_view);
        mDialogMessageView.setVisibility(LinearLayout.GONE);
        mDialogMessageTitle = (TextView) mMainView.findViewById(R.id.main_dialog_message_title);
        mDialogMessageBody = (TextView) mMainView.findViewById(R.id.main_dialog_message_body);
        mDialogMessageClose = (Button) mMainView.findViewById(R.id.main_dialog_message_close_btn);
        mDialogMessageCancel = (Button) mMainView.findViewById(R.id.main_dialog_message_cancel_btn);
        mDialogMessageOk = (Button) mMainView.findViewById(R.id.main_dialog_message_ok_btn);

        mDialogConfirmView = (LinearLayout) mMainView.findViewById(R.id.main_dialog_confirm_view);
        mDialogConfirmView.setVisibility(LinearLayout.GONE);
        mDialogConfirmMsg = (TextView) mMainView.findViewById(R.id.main_dialog_confirm_msg);
        mDialogConfirmCancel = (Button) mMainView.findViewById(R.id.main_dialog_confirm_sync_cancel);
        mDialogConfirmNo = (Button) mMainView.findViewById(R.id.copy_delete_confirm_no);
        mDialogConfirmNoAll = (Button) mMainView.findViewById(R.id.copy_delete_confirm_noall);
        mDialogConfirmYes = (Button) mMainView.findViewById(R.id.copy_delete_confirm_yes);
        mDialogConfirmYesAll = (Button) mMainView.findViewById(R.id.copy_delete_confirm_yesall);

        mContextButtonCreate = (ImageButton) mMainView.findViewById(R.id.context_button_clear);
        mContextButtonCreateView = (LinearLayout) mMainView.findViewById(R.id.context_button_clear_view);
        mContextButtonShare = (ImageButton) mMainView.findViewById(R.id.context_button_share);
        mContextButtonShareView = (LinearLayout) mMainView.findViewById(R.id.context_button_share_view);
        mContextButtonRename = (ImageButton) mMainView.findViewById(R.id.context_button_rename);
        mContextButtonRenameView = (LinearLayout) mMainView.findViewById(R.id.context_button_rename_view);
        mContextButtonPaste = (ImageButton) mMainView.findViewById(R.id.context_button_paste);
        mContextButtonPasteView = (LinearLayout) mMainView.findViewById(R.id.context_button_paste_view);
        mContextButtonCopy = (ImageButton) mMainView.findViewById(R.id.context_button_copy);
        mContextButtonCopyView = (LinearLayout) mMainView.findViewById(R.id.context_button_copy_view);
        mContextButtonCut = (ImageButton) mMainView.findViewById(R.id.context_button_cut);
        mContextButtonCutView = (LinearLayout) mMainView.findViewById(R.id.context_button_cut_view);

        mContextButtonArchive = (ImageButton) mMainView.findViewById(R.id.context_button_archive);
        mContextButtonArchiveView = (LinearLayout) mMainView.findViewById(R.id.context_button_archive_view);

        mContextButtonDelete = (ImageButton) mMainView.findViewById(R.id.context_button_delete);
        mContextButtonDeleteView = (LinearLayout) mMainView.findViewById(R.id.context_button_delete_view);
        mContextButtonSelectAll = (ImageButton) mMainView.findViewById(R.id.context_button_select_all);
        mContextButtonSelectAllView = (LinearLayout) mMainView.findViewById(R.id.context_button_select_all_view);
        mContextButtonUnselectAll = (ImageButton) mMainView.findViewById(R.id.context_button_unselect_all);
        mContextButtonUnselectAllView = (LinearLayout) mMainView.findViewById(R.id.context_button_unselect_all_view);
        mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);

        setContextButtonListener();
    }

    ;

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, enabled=" + enabled);
        if (enabled) {
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            }, 1000);
        } else {
            btn.setEnabled(false);
        }
    }

    ;

    private void setContextButtonSelectUnselectVisibility() {
        if (mTreeFilelistAdapter.getCount() > 0) {
            if (mTreeFilelistAdapter.isAllItemSelected())
                mContextButtonSelectAllView.setVisibility(ImageButton.INVISIBLE);
            else mContextButtonSelectAllView.setVisibility(ImageButton.VISIBLE);
            if (mTreeFilelistAdapter.getSelectedItemCount() == 0)
                mContextButtonUnselectAllView.setVisibility(ImageButton.INVISIBLE);
            else mContextButtonUnselectAllView.setVisibility(ImageButton.VISIBLE);
        } else {
            mContextButtonSelectAllView.setVisibility(ImageButton.INVISIBLE);
            mContextButtonUnselectAllView.setVisibility(ImageButton.INVISIBLE);
        }
    }


    private void setContextButtonListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mContextButtonCreate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonCreate, false);
                    createItem(mTreeFilelistAdapter, mLocalStorage.getSelectedItem().toString() +
                            mCurrentDirectory.getText());
                    setContextButtonEnabled(mContextButtonCreate, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonCreate, mContext.getString(R.string.msgs_zip_cont_label_create));

        mContextButtonShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonShare, false);
                    if (mTreeFilelistAdapter.isItemSelected()) shareItem(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonShare, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonShare, mContext.getString(R.string.msgs_zip_cont_label_share));

        mContextButtonRename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonRename, false);
                    if (mTreeFilelistAdapter.isItemSelected()) renameItem(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonRename, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonRename, mContext.getString(R.string.msgs_zip_cont_label_rename));

        mContextButtonArchive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonArchive, false);
                    if (mTreeFilelistAdapter.isItemSelected()) prepareZipSelectedItem(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonArchive, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonArchive, mContext.getString(R.string.msgs_zip_cont_label_paste));

        mContextButtonPaste.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonPaste, false);
                    pasteItem();
                    setContextButtonEnabled(mContextButtonPaste, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonPaste, mContext.getString(R.string.msgs_zip_cont_label_paste));

        mContextButtonCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonCopy, false);
                    if (mTreeFilelistAdapter.isItemSelected()) copyItem(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonCopy, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonCopy, mContext.getString(R.string.msgs_zip_cont_label_copy));

        mContextButtonCut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonCut, false);
                    if (mTreeFilelistAdapter.isItemSelected()) cutItem(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonCut, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonCut, mContext.getString(R.string.msgs_zip_cont_label_cut));

        mContextButtonDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonDelete, false);
                    if (mTreeFilelistAdapter.isItemSelected()) confirmDelete(mTreeFilelistAdapter);
                    setContextButtonEnabled(mContextButtonDelete, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonDelete, mContext.getString(R.string.msgs_zip_cont_label_delete));

        mContextButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonSelectAll, false);
                    ArrayList<TreeFilelistItem> tfl = mTreeFilelistAdapter.getDataList();
                    for (TreeFilelistItem tfli : tfl) {
                        tfli.setChecked(true);
                    }
                    mTreeFilelistAdapter.notifyDataSetChanged();
                    setContextButtonEnabled(mContextButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonSelectAll, mContext.getString(R.string.msgs_zip_cont_label_select_all));

        mContextButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextButtonUnselectAll, false);
                    mTreeFilelistAdapter.setAllItemUnchecked();
                    mTreeFilelistAdapter.notifyDataSetChanged();
                    setContextButtonEnabled(mContextButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextButtonUnselectAll, mContext.getString(R.string.msgs_zip_cont_label_unselect_all));
    }

    static private void setCheckedTextView(final CheckedTextView ctv) {
        ctv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctv.toggle();
            }
        });
    }

    public void sortFileList() {
        CommonUtilities.sortFileList(mActivity, mGp, mTreeFilelistAdapter, null);
    }

    final static public void getAllPictureFileInDirectory(ArrayList<File> fl, File lf, boolean process_sub_directories) {
        if (lf.exists()) {
            if (lf.isDirectory()) {
                File[] cfl = lf.listFiles();
                if (cfl != null && cfl.length > 0) {
                    for (File cf : cfl) {
                        if (cf.isDirectory()) {
                            if (process_sub_directories)
                                getAllPictureFileInDirectory(fl, cf, process_sub_directories);
                        } else {
                            fl.add(cf);
                        }
                    }
                }
            } else {
                fl.add(lf);
            }
        }
    }

    private void shareItem(CustomTreeFilelistAdapter tfa) {
        ArrayList<TreeFilelistItem> tfl = tfa.getDataList();
        ArrayList<String> fpl = new ArrayList<String>();
        for (int i = 0; i < tfl.size(); i++) {
            if (tfl.get(i).isChecked()) {
                if (!tfl.get(i).isDirectory()) {
                    fpl.add(tfl.get(i).getPath() + "/" + tfl.get(i).getName());
                } else {
                    ArrayList<File> fl = new ArrayList<File>();
                    File cf = new File(tfl.get(i).getPath() + "/" + tfl.get(i).getName());
                    getAllPictureFileInDirectory(fl, cf, true);

                    for (File item : fl) {
                        fpl.add(item.getAbsolutePath());
                    }
                }
            }
        }
        if (fpl.size() >= 100) {
            mCommonDlg.showCommonDialog(false, "E",
                    mContext.getString(R.string.msgs_local_file_share_file_max_file_count_reached), "", null);
        } else {
            if (fpl.size() > 1) {
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*"); /* This example is sharing jpeg images. */

                ArrayList<Uri> files = new ArrayList<Uri>();

                for (String path : fpl) {
                    File file = new File(path);
                    Uri uri = null;
                    if (Build.VERSION.SDK_INT >= 24)
                        uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(path));
                    else Uri.fromFile(file);
                    files.add(uri);
                }
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    mCommonDlg.showCommonDialog(false, "E", "startActivity() failed at shareItem() for multiple item. message=" + e.getMessage(), "", null);
                }
            } else if (fpl.size() == 1) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                File lf = new File(fpl.get(0));
                Uri uri = null;
                if (Build.VERSION.SDK_INT >= 26)
                    uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", lf);
                else uri = Uri.parse("file://" + fpl.get(0));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setType("image/*");
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    mCommonDlg.showCommonDialog(false, "E", "startActivity() failed at shareItem() for songle item. message=" + e.getMessage(), "", null);
                }
            } else {
                mCommonDlg.showCommonDialog(false, "E",
                        mContext.getString(R.string.msgs_local_file_share_file_no_file_selected), "", null);
            }
        }
    }

    ;

    private String mFindKey = "*";
    private AdapterSearchFileList mAdapterSearchFileList = null;
    private int mSearchListPositionX = 0;
    private int mSearchListPositionY = 0;
    private String mSearchRootDir = "";

    public void searchFile() {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.setContentView(R.layout.search_file_dlg);
        final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.search_file_dlg_view);
        dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.search_file_dlg_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
        final TextView dlg_title = (TextView) dialog.findViewById(R.id.search_file_dlg_title);
        dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

        final ImageButton ib_sort = (ImageButton) dialog.findViewById(R.id.search_file_dlg_sort_btn);
        ib_sort.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
//		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.search_file_dlg_msg);
        final CheckedTextView dlg_hidden = (CheckedTextView) dialog.findViewById(R.id.search_file_dlg_search_hidden_item);
        CommonUtilities.setCheckedTextView(dlg_hidden);
        final CheckedTextView dlg_case_sensitive = (CheckedTextView) dialog.findViewById(R.id.search_file_dlg_search_case_sensitive);
        CommonUtilities.setCheckedTextView(dlg_case_sensitive);

        final Button btnOk = (Button) dialog.findViewById(R.id.search_file_dlg_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.search_file_dlg_cancel_btn);
        final EditText et_search_key = (EditText) dialog.findViewById(R.id.search_file_dlg_search_key);
        final ListView lv_search_result = (ListView) dialog.findViewById(R.id.search_file_dlg_search_result);

        final TextView searcgh_info = (TextView) dialog.findViewById(R.id.search_file_dlg_search_info);

        if (mAdapterSearchFileList == null) {
            mAdapterSearchFileList = new AdapterSearchFileList(mActivity);
            lv_search_result.setAdapter(mAdapterSearchFileList);
        } else {
            if (!mMainFilePath.equals(mSearchRootDir)) {
                mAdapterSearchFileList = new AdapterSearchFileList(mActivity);
                lv_search_result.setAdapter(mAdapterSearchFileList);
            } else {
                lv_search_result.setAdapter(mAdapterSearchFileList);
                lv_search_result.setSelectionFromTop(mSearchListPositionX, mSearchListPositionY);
                if (mAdapterSearchFileList.isSortAscendant())
                    ib_sort.setImageResource(R.drawable.ic_128_sort_asc_gray);
                else ib_sort.setImageResource(R.drawable.ic_128_sort_dsc_gray);
                long s_size = 0;
                for (TreeFilelistItem tfi : mAdapterSearchFileList.getDataList())
                    s_size += tfi.getLength();
                String msg = mContext.getString(R.string.msgs_search_file_dlg_search_result);
                searcgh_info.setText(String.format(msg, mAdapterSearchFileList.getDataList().size(), s_size));
            }
        }

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        ib_sort.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final CustomTreeFilelistAdapter tfa = new CustomTreeFilelistAdapter(mActivity, false, false);
                NotifyEvent ntfy_sort = new NotifyEvent(mContext);
                ntfy_sort.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        if (tfa.isSortAscendant()) mAdapterSearchFileList.setSortAscendant();
                        else mAdapterSearchFileList.setSortDescendant();
                        if (tfa.isSortKeyName()) mAdapterSearchFileList.setSortKeyName();
                        else if (tfa.isSortKeySize()) mAdapterSearchFileList.setSortKeySize();
                        else if (tfa.isSortKeyTime()) mAdapterSearchFileList.setSortKeyTime();
                        mAdapterSearchFileList.sort();
                        mAdapterSearchFileList.notifyDataSetChanged();
                        if (mAdapterSearchFileList.isSortAscendant())
                            ib_sort.setImageResource(R.drawable.ic_128_sort_asc_gray);
                        else ib_sort.setImageResource(R.drawable.ic_128_sort_dsc_gray);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                if (mAdapterSearchFileList.isSortAscendant()) tfa.setSortAscendant();
                else tfa.setSortDescendant();
                if (mAdapterSearchFileList.isSortKeyName()) tfa.setSortKeyName();
                else if (mAdapterSearchFileList.isSortKeySize()) tfa.setSortKeySize();
                else if (mAdapterSearchFileList.isSortKeyTime()) tfa.setSortKeyTime();
                CommonUtilities.sortFileList(mActivity, mGp, tfa, ntfy_sort);
            }
        });

        et_search_key.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) btnOk.setEnabled(true);
                else btnOk.setEnabled(false);
            }
        });

        lv_search_result.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TreeFilelistItem tfi = mAdapterSearchFileList.getItem(position);
                openSppecificDirectory(tfi.getPath(), tfi.getName());
                mSearchListPositionX = lv_search_result.getFirstVisiblePosition();
                mSearchListPositionY = lv_search_result.getChildAt(0) == null ? 0 : lv_search_result.getChildAt(0).getTop();
                btnCancel.performClick();

//				String fid=CommonUtilities.getFileExtention(tfi.getName());
//				String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
//				invokeBrowser(tfi.getPath(), tfi.getName(), "");
//
//				if (mt!=null && mt.startsWith("application/zip")) {
//					btnCancel.performClick();
//				}
            }
        });

        lv_search_result.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final CustomTreeFilelistAdapter n_tfa = new CustomTreeFilelistAdapter(mActivity, false, false, false);
                ArrayList<TreeFilelistItem> n_tfl = new ArrayList<TreeFilelistItem>();
                final TreeFilelistItem n_tfli = mAdapterSearchFileList.getItem(position).clone();
                n_tfli.setChecked(true);
                n_tfl.add(n_tfli);
                n_tfa.setDataList(n_tfl);

                final CustomContextMenu mCcMenu = new CustomContextMenu(mContext.getResources(), mFragmentManager);

                mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_top), R.drawable.context_button_top)
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                lv_search_result.setSelection(0);
                            }
                        });
                mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_bottom), R.drawable.context_button_bottom)
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                lv_search_result.setSelection(lv_search_result.getCount());
                            }
                        });

                mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_open_file) + "(" + (n_tfl.get(0)).getName() + ")")
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                invokeBrowser(n_tfli.getPath(), n_tfli.getName(), "");
                                btnCancel.performClick();
                            }
                        });
                mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_zip) + "(" + (n_tfl.get(0)).getName() + ")", R.drawable.ic_32_file_zip)
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                invokeBrowser(n_tfli.getPath(), n_tfli.getName(), MIME_TYPE_ZIP);
                                btnCancel.performClick();
                            }
                        });
                mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_text) + "(" + (n_tfl.get(0)).getName() + ")", R.drawable.cc_sheet)
                        .setOnClickListener(new CustomContextMenuOnClickListener() {
                            @Override
                            public void onClick(CharSequence menuTitle) {
                                invokeBrowser(n_tfli.getPath(), n_tfli.getName(), MIME_TYPE_TEXT);
                                btnCancel.performClick();
                            }
                        });
                mCcMenu.createMenu();

                mSearchListPositionX = lv_search_result.getFirstVisiblePosition();
                mSearchListPositionY = lv_search_result.getChildAt(0) == null ? 0 : lv_search_result.getChildAt(0).getTop();

                return true;
            }
        });

        //OK button
        et_search_key.setText(mFindKey);
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mFindKey = et_search_key.getText().toString();
                final ArrayList<TreeFilelistItem> s_tfl = new ArrayList<TreeFilelistItem>();
                int flags = 0;//Pattern.CASE_INSENSITIVE;// | Pattern..MULTILINE;
                if (!dlg_case_sensitive.isChecked()) flags = Pattern.CASE_INSENSITIVE;
                final Pattern s_key = Pattern.compile("(" + MiscUtil.convertRegExp(mFindKey) + ")", flags);
                final ThreadCtrl tc = new ThreadCtrl();
                final ProgressSpinDialogFragment psd = ProgressSpinDialogFragment.newInstance(
                        mContext.getString(R.string.msgs_search_file_dlg_searching), "",
                        mContext.getString(R.string.msgs_common_dialog_cancel),
                        mContext.getString(R.string.msgs_common_dialog_canceling));

                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        tc.setDisabled();
                        if (!tc.isEnabled()) psd.dismissAllowingStateLoss();
                    }
                });
                psd.showDialog(mFragmentManager, psd, ntfy, true);
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        buildFileListBySearchKey(tc, dlg_hidden.isChecked(), psd, s_tfl, s_key,
                                new File(mLocalStorage.getSelectedItem().toString()));
                        psd.dismissAllowingStateLoss();
                        if (!tc.isEnabled()) {
                            mCommonDlg.showCommonDialog(false, "W",
                                    mContext.getString(R.string.msgs_search_file_dlg_search_cancelled), "", null);
                        } else {
                            mAdapterSearchFileList.setDataList(s_tfl);
                            mSearchRootDir = mLocalStorage.getSelectedItem().toString();
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    long s_size = 0;
                                    for (TreeFilelistItem tfi : mAdapterSearchFileList.getDataList())
                                        s_size += tfi.getLength();
                                    String msg = mContext.getString(R.string.msgs_search_file_dlg_search_result);
                                    searcgh_info.setText(String.format(msg, mAdapterSearchFileList.getDataList().size(), s_size));
                                    mAdapterSearchFileList.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                };
                th.setPriority(Thread.MIN_PRIORITY);
                th.start();
            }
        });
        // CANCEL�{�^���̎w��
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    ;

    private void buildFileListBySearchKey(final ThreadCtrl tc, boolean search_hidden_item,
                                          ProgressSpinDialogFragment psd,
                                          ArrayList<TreeFilelistItem> s_tfl, Pattern s_key, File s_file) {
//		mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
        boolean ignore = false;
        if (s_file.isHidden()) {
            if (search_hidden_item) {
                ignore = false;
            } else {
                ignore = true;
            }
        } else {
            ignore = false;
        }
//		Log.v("","path="+s_file.getPath()+", ignore="+ignore);
        if (!ignore) {
            if (s_file.isDirectory()) {
                psd.updateMsgText(s_file.getPath());
                File[] fl = s_file.listFiles();
                if (fl != null) {
                    for (File item : fl) {
                        if (!tc.isEnabled()) break;
                        buildFileListBySearchKey(tc, search_hidden_item, psd, s_tfl, s_key, item);
                    }
                }
                if (!tc.isEnabled()) return;
            } else {
                if (!tc.isEnabled()) return;
//				Log.v("","s_key="+s_key.toString()+", fn="+s_file.getName().toLowerCase()+
//						", result="+s_key.matcher(s_file.getName().toLowerCase()).matches());
                if (s_key.matcher(s_file.getName()).matches()) {
                    TreeFilelistItem tfli = createNewFileListItem(s_file);
                    s_tfl.add(tfli);
                }
            }
        }
    }

    ;

    private void createItem(CustomTreeFilelistAdapter tfa, final String c_dir) {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);
        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
        final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
        dlg_title.setText(mContext.getString(R.string.msgs_file_select_edit_dlg_create));
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etDir = (EditText) dialog.findViewById(R.id.single_item_input_dir);
        final CheckedTextView create_type = (CheckedTextView) dialog.findViewById(R.id.single_item_input_type);
        create_type.setVisibility(CheckedTextView.VISIBLE);
        setCheckedTextView(create_type);
        create_type.setText(mContext.getString(R.string.msgs_file_select_edit_dlg_dir_create_file));
        create_type.setChecked(false);
        final String t_dir = c_dir.endsWith("/") ? c_dir : c_dir + "/";
        dlg_cmp.setText(mContext.getString(R.string.msgs_file_select_edit_parent_directory) + t_dir);
        CommonDialog.setDlgBoxSizeCompact(dialog);
        etDir.setText("");
        btnOk.setEnabled(false);
        etDir.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    File lf = new File(t_dir + s.toString());
//					Log.v("","fp="+lf.getPath());
                    if (lf.exists()) {
                        btnOk.setEnabled(false);
                        dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
                    } else {
                        btnOk.setEnabled(true);
                        dlg_msg.setText("");
                    }
                }
            }
        });

        //OK button
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//				NotifyEvent
                final String creat_dir = etDir.getText().toString();
                final String n_path = t_dir + creat_dir;
//				Log.v("","create_dir="+creat_dir+", n_path="+n_path);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        File lf = new File(n_path);
                        boolean rc_create = false;
                        if (create_type.isChecked()) {
                            if (t_dir.startsWith(mGp.safMgr.getSdcardRootPath())) {
                                SafFile sf = mUtil.createSafFile(n_path, false);
                                if (sf==null) return;
                                rc_create = sf.exists();
                            } else {
                                try {
                                    rc_create = lf.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            if (t_dir.startsWith(mGp.safMgr.getSdcardRootPath())) {
                                SafFile sf = mUtil.createSafFile(n_path, true);
                                if (sf==null) return;
                                rc_create = sf.exists();
                            } else {
                                rc_create = lf.mkdirs();
                            }
                        }
                        if (!rc_create) {
                            dlg_msg.setText(String.format(
                                    mContext.getString(R.string.msgs_file_select_edit_dlg_dir_not_created),
                                    etDir.getText()));
                            return;
                        }
                        refreshFileList();
                        dialog.dismiss();
                        mCommonDlg.showCommonDialog(false, "I",
                                String.format(mContext.getString(R.string.msgs_file_select_edit_dlg_dir_created), n_path), "", null);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                if (create_type.isChecked())
                    mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_file_select_edit_confirm_create_file), n_path, ntfy);
                else
                    mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_file_select_edit_confirm_create_directory), n_path, ntfy);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();

    }

    ;

    private void renameItem(CustomTreeFilelistAdapter tfa) {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);
        final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
        final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
        dlg_title.setText(mContext.getString(R.string.msgs_zip_local_file_rename_title));
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etDir = (EditText) dialog.findViewById(R.id.single_item_input_dir);
        dlg_cmp.setVisibility(TextView.GONE);

        TreeFilelistItem w_tfli = null;
        for (TreeFilelistItem item : tfa.getDataList()) {
            if (item.isChecked()) {
                w_tfli = item;
                break;
            }
        }
        final TreeFilelistItem tfli = w_tfli;

        CommonDialog.setDlgBoxSizeCompact(dialog);
        btnOk.setEnabled(false);
        etDir.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    File lf = new File(tfli.getPath() + "/" + s.toString());
//					Log.v("","fp="+lf.getPath());
                    if (lf.exists()) {
                        btnOk.setEnabled(false);
                        dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
                    } else {
                        btnOk.setEnabled(true);
                        dlg_msg.setText("");
                    }
                }
            }

        });
        etDir.setText(tfli.getName());

        //OK button
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String new_name = tfli.getPath() + "/" + etDir.getText().toString();
                final String current_name = tfli.getPath() + "/" + tfli.getName();
//				NotifyEvent
//				Log.v("","new name="+new_name+", current name="+current_name);
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {

                        setUiDisabled();
                        showDialogProgress();
                        final ThreadCtrl tc = new ThreadCtrl();
                        mDialogProgressSpinCancel.setEnabled(true);
                        mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
                        mDialogProgressSpinCancel.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                confirmCancel(tc, mDialogProgressSpinCancel);
                            }
                        });
                        Thread th = new Thread() {
                            @Override
                            public void run() {
                                mUtil.addDebugMsg(1, "I", "Rename started");
                                boolean rc_create = false;
                                if (!tfli.isDirectory()) {
                                    if (current_name.startsWith(mGp.safMgr.getSdcardRootPath())) {
                                        try {
                                            SafFile sf = mUtil.createSafFile(current_name, false);
                                            if (sf!=null) {
                                                sf.renameTo(etDir.getText().toString());
                                                rc_create = sf.exists();
                                            }
                                        } catch (Exception e) {
                                            mUtil.addLogMsg("E", "Saf file error");
                                            mUtil.addLogMsg("E", mGp.safMgr.getMessages());
                                            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                                        }
                                    } else {
                                        File n_file = new File(new_name);
                                        File c_file = new File(current_name);
                                        rc_create = c_file.renameTo(n_file);
                                    }
                                    if (rc_create) {
                                        CommonUtilities.scanMediaFile(mGp, mUtil, current_name);
                                        CommonUtilities.scanMediaFile(mGp, mUtil, new_name);
                                        putProgressMessage("Media file scanned :" + new_name);
                                    }
                                } else {
                                    ArrayList<File> fl = new ArrayList<File>();
                                    CommonUtilities.getAllFileInDirectory(mGp, mUtil, fl, new File(current_name), true);
                                    if (current_name.startsWith(mGp.safMgr.getSdcardRootPath())) {
                                        try {
                                            SafFile sf = mUtil.createSafFile(current_name, true);
                                            if (sf!=null) {
                                                sf.renameTo(etDir.getText().toString());
                                                rc_create = sf.exists();
                                            } else {
                                                return;
                                            }
                                        } catch (Exception e) {
                                            mUtil.addLogMsg("E", "Saf directory error");
                                            mUtil.addLogMsg("E", mGp.safMgr.getMessages());
                                            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                                        }
                                    } else {
                                        File n_file = new File(new_name);
                                        File c_file = new File(current_name);
                                        rc_create = c_file.renameTo(n_file);
                                    }
                                    if (rc_create) {
                                        for (File item : fl) {
                                            CommonUtilities.scanMediaFile(mGp, mUtil, item.getPath());
                                            putProgressMessage("Media store removed : " + item.getPath());
                                        }
                                        fl.clear();
                                        CommonUtilities.getAllFileInDirectory(mGp, mUtil, fl, new File(new_name), true);
                                        for (File item : fl) {
                                            CommonUtilities.scanMediaFile(mGp, mUtil, item.getAbsolutePath());
                                            putProgressMessage("Media file scanned : " + item.getAbsolutePath());
                                        }
                                    }
                                }
                                if (!rc_create) {
                                    mUiHandler.post(new Runnable(){
                                        @Override
                                        public void run() {
                                            mCommonDlg.showCommonDialog(false, "I",
                                                    String.format(mContext.getString(R.string.msgs_zip_local_file_rename_failed), new_name),
                                                    "", null);
                                            setUiEnabled();
                                        }
                                    });
                                    return;
                                }
                                mUtil.addDebugMsg(1, "I", "Rename ended");

//								final String cdir=mLocalFileCurrentDirectory.getText().toString();
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCommonDlg.showCommonDialog(false, "I",
                                                String.format(mContext.getString(R.string.msgs_zip_local_file_rename_completed), new_name), "", null);
                                        mGp.copyCutList.clear();
                                        mGp.copyCutType = GlobalParameters.COPY_CUT_FROM_LOCAL;
                                        mGp.copyCutItemInfo.setVisibility(TextView.GONE);
                                        mGp.copyCutItemClear.setVisibility(Button.GONE);
                                        mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
                                        refreshFileList();
                                        setUiEnabled();
                                    }
                                });
                            }
                        };
                        th.start();
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_zip_local_file_rename_confirm_title),
                        current_name, ntfy);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    private void copyItem(CustomTreeFilelistAdapter tfa) {
        if (tfa.isItemSelected()) {
            mGp.copyCutModeIsCut = false;
            mGp.copyCutList.clear();
            mGp.copyCutType = GlobalParameters.COPY_CUT_FROM_LOCAL;
            mGp.copyCutFilePath = mMainFilePath;
            mGp.copyCutCurrentDirectory = mCurrentDirectory.getText().equals("/") ? "" : (mCurrentDirectory.getText().length() == 0 ? "" : mCurrentDirectory.getText().toString().substring(1));
            String c_list = "", sep = "";
            for (TreeFilelistItem tfl : tfa.getDataList()) {
                if (tfl.isChecked()) {
                    mGp.copyCutList.add(tfl);
                    c_list += sep + tfl.getPath().replace(mLocalStorage.getSelectedItem().toString(), "") + "/" + tfl.getName();
                    sep = ", ";
                    tfl.setChecked(false);
                }
            }
            tfa.notifyDataSetChanged();
            String from = mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_LOCAL) ? "Local" : "ZIP";
            mGp.copyCutItemInfo.setText(mContext.getString(R.string.msgs_zip_cont_header_copy) + " " + from + ":" + c_list);
            mGp.copyCutItemInfo.setVisibility(TextView.VISIBLE);
            mGp.copyCutItemClear.setVisibility(Button.VISIBLE);
            mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
            mNotifyCheckBoxChanged.notifyToListener(false, null);
        }
    }

    private void cutItem(CustomTreeFilelistAdapter tfa) {
        if (tfa.isItemSelected()) {
            mGp.copyCutModeIsCut = true;
            mGp.copyCutList.clear();
            mGp.copyCutType = GlobalParameters.COPY_CUT_FROM_LOCAL;
            mGp.copyCutFilePath = mMainFilePath;
//			mGp.copyCutCurrentDirectory=mCurrentDirectory.getText().equals("/")?"":mCurrentDirectory.getText().substring(1);
            mGp.copyCutCurrentDirectory = mCurrentDirectory.getText().equals("/") ? "" : (mCurrentDirectory.getText().length() == 0 ? "" : mCurrentDirectory.getText().toString().substring(1));
            String c_list = "", sep = "";
            for (TreeFilelistItem tfl : tfa.getDataList()) {
                if (tfl.isChecked()) {
                    mGp.copyCutList.add(tfl);
                    c_list += sep + tfl.getPath().replace(mLocalStorage.getSelectedItem().toString(), "") + "/" + tfl.getName();
                    sep = ", ";
                    tfl.setChecked(false);
                }
            }
            tfa.notifyDataSetChanged();
            String from = mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_LOCAL) ? "Local" : "ZIP";
            mGp.copyCutItemInfo.setText(mContext.getString(R.string.msgs_zip_cont_header_cut) + " " + from + ":" + c_list);
            mGp.copyCutItemInfo.setVisibility(TextView.VISIBLE);
            mGp.copyCutItemClear.setVisibility(Button.VISIBLE);
            mNotifyCheckBoxChanged.notifyToListener(false, null);
        }
        mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
    }

    ;

    private boolean isCopyCutDestValid(String fp) {
        boolean enabled = true;
        if (mGp.copyCutList.size() > 0) {
            if (mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_ZIP)) enabled = true;
            else {
                String to_pref = fp.startsWith(mGp.internalRootDirectory) ? mGp.internalRootDirectory : mGp.externalRootDirectory;
                String w_curr_dir = fp.replace(to_pref, "");
                String curr_dir = w_curr_dir.equals("") ? "/" : w_curr_dir;
                for (TreeFilelistItem s_item : mGp.copyCutList) {
                    if (!s_item.getPath().startsWith(to_pref)) {
                        enabled = true;
                        break;
                    }
//					String sel_path=s_item.isDirectory()?s_item.getPath()+"/"+s_item.getName():s_item.getPath();
                    String sel_path = s_item.getPath();
                    String pref = sel_path.startsWith(mGp.internalRootDirectory) ? mGp.internalRootDirectory : mGp.externalRootDirectory;
                    String w_s_item = sel_path.replace(pref, "").equals("") ? "/" : sel_path.replace(pref, "");
//					Log.v("","w_s_item="+w_s_item);
                    String[] item_array = w_s_item.equals("") ? new String[]{"/"} : w_s_item.substring(1).split("/");
                    String[] cdir_array = curr_dir.equals("/") ? new String[]{""} : curr_dir.split("/");
//					Log.v("","item num="+item_array.length+", cdir num="+cdir_array.length);
                    if (item_array.length > 1) {
                        if (cdir_array.length != 0) {
                            if (w_s_item.equals(curr_dir)) enabled = false;
                            if (s_item.isDirectory()) {
                                if (curr_dir.startsWith(w_s_item)) enabled = false;
                            } else {
                            }
                        }
                    } else {
//						Log.v("","s_path="+(w_s_item+s_item.getName()));
                        if (s_item.isDirectory()) {
                            if (w_s_item.equals(curr_dir)) enabled = false;
                            else {
                                if (curr_dir.startsWith((w_s_item + s_item.getName())))
                                    enabled = false;
                            }
                        } else {
                            if (w_s_item.equals(curr_dir)) enabled = false;
                        }
                    }
//					if (enabled) Log.v("","enabled  name="+w_s_item+", c="+curr_dir);
//					else Log.v("","disabled name="+w_s_item+", c="+curr_dir);
                }
            }
        } else {
            enabled = false;
        }
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " exit, enabled=" + enabled);
        return enabled;
    }

    ;

    private void confirmCancel(final ThreadCtrl tc, final Button cancel) {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                tc.setDisabled();
                cancel.setEnabled(false);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mCommonDlg.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_main_confirm_cancel), "", ntfy);
    }

    ;

    private void confirmMove() {
        if (mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_LOCAL)) confirmMoveFromLocal();
        else if (mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_ZIP)) confirmMoveFromZip();
    }

    ;

    private void confirmMoveFromZip() {
        final String to_dir = mLocalStorage.getSelectedItem().toString() + mCurrentDirectory.getText().toString();
        String w_conf_list = "";
        String sep = "";
        for (TreeFilelistItem item : mGp.copyCutList) {
            w_conf_list += sep + item.getZipFileName();
            sep = "\n";
        }
        final String conf_list = w_conf_list;
        NotifyEvent ntfy_confirm = new NotifyEvent(mContext);
        ntfy_confirm.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                prepareExtractMultipleItem(to_dir, conf_list, true);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mCommonDlg.showCommonDialog(true, "W",
                String.format(mContext.getString(R.string.msgs_zip_move_file_confirm), to_dir),
                conf_list, ntfy_confirm);
    }

    ;

    private void confirmMoveFromLocal() {
        final String to_dir = mLocalStorage.getSelectedItem().toString() + mCurrentDirectory.getText().toString();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setUiDisabled();
                showDialogProgress();
                final ThreadCtrl tc = new ThreadCtrl();
                mDialogProgressSpinCancel.setEnabled(true);
                mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
                mDialogProgressSpinCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmCancel(tc, mDialogProgressSpinCancel);
                    }
                });
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        mUtil.addDebugMsg(1, "I", "Move started");
                        String moved_item = "", moved_sep = "";
                        boolean process_aborted = false;
                        for (TreeFilelistItem tfl : mGp.copyCutList) {
//							if (!tc.isEnabled()) {
//								String msg=mContext.getString(R.string.msgs_zip_local_file_move_cancelled);
//								mUtil.addLogMsg("I", msg);
//								mCommonDlg.showCommonDialog(false, "W", msg, "", null);
//								process_aborted=true;
//								break;
//							}
                            File from_file = new File(tfl.getPath() + "/" + tfl.getName());
//							File to_file=new File(to_dir+"/"+tfl.getName());
                            boolean rc = moveFile(tc, from_file, (to_dir + "/" + tfl.getName()).replace("//", "/"));
//							boolean rc=from_file.renameTo(to_file);
                            if (rc) {
                                moved_item += moved_sep + from_file;
                                moved_sep = ", ";
                            } else {
                                process_aborted = true;
                                if (tc.isEnabled()) {
                                    String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_failed), tfl.getName());
                                    mUtil.addLogMsg("I", msg);
                                    mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                                    break;
                                }
                            }
                        }
                        if (!process_aborted) {
                            mCommonDlg.showCommonDialog(false, "I",
                                    mContext.getString(R.string.msgs_zip_local_file_move_completed), moved_item, null);
                        }
                        mUtil.addDebugMsg(1, "I", "Move ended");
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mGp.copyCutList.clear();
                                mGp.copyCutType = GlobalParameters.COPY_CUT_FROM_LOCAL;
                                mGp.copyCutItemInfo.setVisibility(TextView.GONE);
                                mGp.copyCutItemClear.setVisibility(Button.GONE);
                                mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
                                refreshFileList();
                                setUiEnabled();
                            }
                        });
                    }
                };
                th.start();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String c_list = "", sep = "";
        for (TreeFilelistItem tfl : mGp.copyCutList) {
            c_list += sep + tfl.getName();
            sep = ",";
        }
        mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_zip_local_file_move_confirm_title), c_list, ntfy);
    }

    private boolean moveFile(ThreadCtrl tc, File from_file, String to_path) {
        if (from_file.getPath().startsWith(mGp.internalRootDirectory)) {
            if (to_path.startsWith(mGp.internalRootDirectory))
                return moveFileInternalToInternal(tc, from_file, to_path);
            else return moveFileInternalToExternal(tc, from_file, to_path);
        } else {
            if (to_path.startsWith(mGp.internalRootDirectory))
                return moveFileExternalToInternal(tc, from_file, to_path);
            else return moveFileExternalToExternal(tc, from_file, to_path);
        }
    }

    private boolean moveFileInternalToInternal(ThreadCtrl tc, File from_file, String to_path) {
        if (!tc.isEnabled()) return false;
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " from=" + from_file.getPath() + ", to=" + to_path);
        boolean result = false;

        if (from_file.isDirectory()) {
//			Log.v("","mkdirs="+to_path);
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = moveFileInternalToInternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
                if (result) from_file.delete();
            } else {
                from_file.delete();
                result = true;
            }
        } else {
            if (confirmReplace(tc, to_path)) {
                if (!tc.isEnabled()) {
                    String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                    mUtil.addLogMsg("I", msg);
                    mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                    result = false;
                } else {
                    File to = new File(to_path);
                    File df = new File(to.getParent());
                    df.mkdirs();
                    to.delete();
                    result = from_file.renameTo(to);
                    MediaScannerConnection.scanFile(mContext, new String[]{to_path, from_file.getAbsolutePath()}, null, null);
                    String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moved), to_path);
                    mUtil.addLogMsg("I", msg);
                    putProgressMessage(msg);
                }
            } else {
                //Reject replace request
                if (tc.isEnabled()) {
                    putProgressMessage(
                            mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                    mUtil.addLogMsg("I",
                            mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                    result = true;
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    private boolean moveFileInternalToExternal(ThreadCtrl tc, File from_file, String to_path) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " from=" + from_file.getPath() + ", to=" + to_path);
        boolean result = false;
        if (from_file.isDirectory()) {
//			Log.v("","mkdirs="+to_path);
            if (!tc.isEnabled()) {
                String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                mUtil.addLogMsg("I", msg);
                mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                return false;
            }
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = moveFileInternalToExternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
                if (result) from_file.delete();
            } else {
                from_file.delete();
                result = true;
            }
            SafFile out_sf = mUtil.createSafFile(to_path, true);
            if (out_sf==null) {
                return false;
            }
        } else {
            try {
                if (confirmReplace(tc, to_path)) {
                    SafFile out_sf = mUtil.createSafFile(to_path, false);
                    if (out_sf==null) {
                        return false;
                    }

                    OutputStream fos = mContext.getContentResolver().openOutputStream(out_sf.getUri());
//					BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*8);
                    byte[] buff = new byte[IO_AREA_SIZE];
                    FileInputStream fis = new FileInputStream(from_file);
//					BufferedInputStream bis=new BufferedInputStream(fis, 1024*1024*8);
                    int rc = fis.read(buff);
                    long file_size = from_file.length();
                    long progress = 0, tot_rc = 0;
                    while (rc > 0) {
                        if (!tc.isEnabled()) {
                            fos.flush();
                            fos.close();
                            fis.close();
                            out_sf.delete();
                            break;
                        }
                        fos.write(buff, 0, rc);
                        tot_rc += rc;
                        progress = ((tot_rc * 100) / file_size);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moving), to_path, progress);
                        putProgressMessage(msg);
                        rc = fis.read(buff);
                    }
                    if (!tc.isEnabled()) {
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                        mUtil.addLogMsg("I", msg);
                        mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                        result = false;
                    } else {
                        result = true;
                        fos.flush();
                        fos.close();
                        fis.close();
                        from_file.delete();
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moved), to_path);
                        mUtil.addLogMsg("I", msg);
                        putProgressMessage(msg);
                    }
                } else {
                    //Reject replace request
                    if (tc.isEnabled()) {
                        putProgressMessage(
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        mUtil.addLogMsg("I",
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } catch (FileNotFoundException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            } catch (IOException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            }
        }
        return result;
    }

    ;

    private boolean moveFileExternalToInternal(ThreadCtrl tc, File from_file, String to_path) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " from=" + from_file.getPath() + ", to=" + to_path);
        boolean result = false;
        if (from_file.isDirectory()) {
//			Log.v("","mkdirs="+to_path);
            if (!tc.isEnabled()) {
                String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                mUtil.addLogMsg("I", msg);
                mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                return false;
            }
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = moveFileExternalToInternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
                if (result) {
                    SafFile out_sf = mUtil.createSafFile(from_file.getPath(), true);
                    if (out_sf==null) {
                        return false;
                    }

                    out_sf.delete();
                }
            } else {
                SafFile in_sf = mUtil.createSafFile(from_file.getPath(), true);
                if (in_sf==null) {
                    return false;
                }
                in_sf.delete();
                result = true;
            }
            File lf = new File(to_path);
            lf.mkdirs();
//			SafFile root_sf=mGp.safMgr.getSdcardSafFile();
//			SafFile out_sf=mGp.safMgr.getSafFileBySdcardPath(root_sf, to_path, true);
        } else {
            try {
                if (confirmReplace(tc, to_path)) {
                    File out_file = new File(to_path);
                    File d_lf = new File(out_file.getParent());
                    d_lf.mkdirs();
//					Log.v("","dir="+d_lf.getPath()+", file="+lf.getPath());
                    FileOutputStream fos = new FileOutputStream(out_file);
//					BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*8);
                    byte[] buff = new byte[IO_AREA_SIZE];
                    FileInputStream fis = new FileInputStream(from_file);
//					BufferedInputStream bis=new BufferedInputStream(fis, 1024*1024*8);
                    int rc = fis.read(buff);
                    long file_size = from_file.length();
                    long in_last_mod = from_file.lastModified();
                    long progress = 0, tot_rc = 0;
                    while (rc > 0) {
                        if (!tc.isEnabled()) {
                            fos.flush();
                            fos.close();
                            fis.close();
                            out_file.delete();
                            break;
                        }
                        fos.write(buff, 0, rc);
                        tot_rc += rc;
                        progress = ((tot_rc * 100) / file_size);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moving), to_path, progress);
                        putProgressMessage(msg);
                        rc = fis.read(buff);
                    }
                    if (!tc.isEnabled()) {
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                        mUtil.addLogMsg("I", msg);
                        mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                        result = false;
                    } else {
                        result = true;
                        fos.flush();
                        fos.close();
                        fis.close();
                        SafFile del_sf = mUtil.createSafFile(from_file.getPath(), false);
                        if (del_sf==null) {
                            return false;
                        }
                        del_sf.delete();
                        out_file.setLastModified(in_last_mod);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moved), to_path);
                        mUtil.addLogMsg("I", msg);
                        putProgressMessage(msg);
                    }
                } else {
                    //Reject replace request
                    if (tc.isEnabled()) {
                        putProgressMessage(
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        mUtil.addLogMsg("I",
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } catch (FileNotFoundException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            } catch (IOException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            }
        }
        return result;
    }

    ;

    private boolean moveFileExternalToExternal(ThreadCtrl tc, File from_file, String to_path) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " from=" + from_file.getPath() + ", to=" + to_path);
        boolean result = false;
        if (from_file.isDirectory()) {
            if (!tc.isEnabled()) {
                String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                mUtil.addLogMsg("I", msg);
                mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                return false;
            }
//			Log.v("","mkdirs="+to_path);
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = moveFileExternalToExternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
                if (result) {
                    SafFile out_sf = mUtil.createSafFile(from_file.getPath(), true);
                    if (out_sf==null) {
                        return false;
                    }
                    out_sf.delete();
                }
            } else {
                SafFile out_sf = mUtil.createSafFile(from_file.getPath(), true);
                if (out_sf==null) {
                    return false;
                }
                out_sf.delete();
                result = true;
            }
            SafFile out_sf = mUtil.createSafFile(to_path, true);
            if (out_sf==null) {
                return false;
            }
        } else {
            try {
                if (confirmReplace(tc, to_path)) {
                    SafFile out_sf = mUtil.createSafFile(to_path, false);
                    if (out_sf==null) {
                        return false;
                    }
                    OutputStream fos = mContext.getContentResolver().openOutputStream(out_sf.getUri());
//					BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*8);
                    byte[] buff = new byte[IO_AREA_SIZE];
                    FileInputStream fis = new FileInputStream(from_file);
//					BufferedInputStream bis=new BufferedInputStream(fis, 1024*1024*8);
                    int rc = fis.read(buff);
                    long file_size = from_file.length();
                    long progress = 0, tot_rc = 0;
                    while (rc > 0) {
                        if (!tc.isEnabled()) {
                            fos.flush();
                            fos.close();
                            fis.close();
                            out_sf.delete();
                            break;
                        }
                        fos.write(buff, 0, rc);
                        tot_rc += rc;
                        progress = ((tot_rc * 100) / file_size);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moving), to_path, progress);
                        putProgressMessage(msg);
                        rc = fis.read(buff);
                    }
                    if (!tc.isEnabled()) {
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_cancelled), to_path);
                        mUtil.addLogMsg("I", msg);
                        mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                        result = false;
                    } else {
                        result = true;
                        fos.flush();
                        fos.close();
                        fis.close();
                        SafFile in_sf = mUtil.createSafFile(from_file.getPath(), false);
                        if (in_sf==null) {
                            return false;
                        }
                        in_sf.delete();
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_move_moved), to_path);
                        mUtil.addLogMsg("I", msg);
                        putProgressMessage(msg);
                    }
                } else {
                    //Reject replace request
                    if (tc.isEnabled()) {
                        putProgressMessage(
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        mUtil.addLogMsg("I",
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } catch (FileNotFoundException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            } catch (IOException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            }
        }
        return result;
    }

    private void pasteItem() {
        if (mGp.copyCutList.size() > 0) {
            if (mGp.copyCutModeIsCut) confirmMove();
            else confirmCopy();
        }
    }

    private void confirmCopy() {
        if (mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_LOCAL)) confirmCopyFromLocal();
        else if (mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_ZIP)) confirmCopyFromZip();
    }

    private void confirmCopyFromZip() {
        final String to_dir = mLocalStorage.getSelectedItem().toString() + mCurrentDirectory.getText().toString();
        String w_conf_list = "";
        String sep = "";
        for (TreeFilelistItem item : mGp.copyCutList) {
            w_conf_list += sep + item.getZipFileName();
            sep = "\n";
        }
        final String conf_list = w_conf_list;
        NotifyEvent ntfy_confirm = new NotifyEvent(mContext);
        ntfy_confirm.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                prepareExtractMultipleItem(to_dir, conf_list, false);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mCommonDlg.showCommonDialog(true, "W",
                String.format(mContext.getString(R.string.msgs_zip_extract_file_confirm_extract), to_dir),
                conf_list, ntfy_confirm);
    }

    ;

    private void prepareExtractMultipleItem(final String dest_path, final String conf_list,
                                            final boolean move_mode) {
        mConfirmResponse = 0;

        setUiDisabled();
        showDialogProgress();
        final ThreadCtrl tc = new ThreadCtrl();
        mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
        mDialogProgressSpinCancel.setEnabled(true);
        mDialogProgressSpinCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmCancel(tc, mDialogProgressSpinCancel);
            }
        });
        Thread th = new Thread() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                mUtil.addDebugMsg(1, "I", "Extract started");
                putProgressMessage(mContext.getString(R.string.msgs_zip_extract_file_started));

                final ZipFile zf = ZipFileManager.createZipFile(mGp.copyCutFilePath, mGp.copyCutEncoding);
                ArrayList<FileHeader> zf_fhl = null;
                try {
                    zf_fhl = (ArrayList<FileHeader>) zf.getFileHeaders();
                } catch (ZipException e) {
                    e.printStackTrace();
                }
                ArrayList<FileHeader> sel_fhl = new ArrayList<FileHeader>();
                for (FileHeader fh_item : zf_fhl) {
                    for (TreeFilelistItem sel_tfli : mGp.copyCutList) {
                        if (sel_tfli.isDirectory()) {
                            if (fh_item.getFileName().startsWith(sel_tfli.getZipFileName() + "/")) {
                                sel_fhl.add(fh_item);
                                break;
                            }
                        } else {
                            if (sel_tfli.getZipFileName().equals(fh_item.getFileName())) {
                                sel_fhl.add(fh_item);
                                break;
                            }
                        }
                    }
                }
                ArrayList<FileHeader> ext_fhl = new ArrayList<FileHeader>();
                extractMultipleItem(tc, dest_path, zf, sel_fhl, ext_fhl, conf_list, move_mode);
                mUtil.addDebugMsg(1, "I", "Extract exited");
            }
        };
        th.setName("extract");
        th.start();
    }

    ;

    private boolean isExtractEnded(final ThreadCtrl tc, final String dest_path, final ZipFile zf,
                                   final ArrayList<FileHeader> selected_fh_list, final ArrayList<FileHeader> extracted_fh_list,
                                   final String conf_list, final boolean move_mode) {
        boolean error = false;
        if ((selected_fh_list.size() == 0)) {
            mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " Extract ended");
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (move_mode) mGp.copyCutItemClear.performClick();
                    refreshFileList();
                }
            });
            if (tc.isEnabled()) {
                if (!move_mode) {
                    mCommonDlg.showCommonDialog(false, "I",
                            mContext.getString(R.string.msgs_zip_extract_file_completed), conf_list, null);
                } else {
                    BufferedZipFile bzf = new BufferedZipFile(zf.getFile(), mGp.copyCutEncoding, mGp.settingDebugLevel > 1);
                    String msg = mContext.getString(R.string.msgs_zip_delete_file_was_deleted);
                    try {
                        for (FileHeader fh : extracted_fh_list) {
                            if (!tc.isEnabled()) {
                                mCommonDlg.showCommonDialog(false, "I",
                                        String.format(mContext.getString(R.string.msgs_zip_delete_file_was_cancelled), fh.getFileName()),
                                        "", null);
                                break;
                            }
                            bzf.removeItem(fh);
                            putProgressMessage(String.format(msg, fh.getFileName()));
                        }
                        if (tc.isEnabled()) bzf.close();
                    } catch (ZipException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        String e_msg = "Exception occured";
                        mUtil.addLogMsg("E", e_msg + ", " + e.getMessage());
                        mCommonDlg.showCommonDialog(false, "E", e_msg, e.getMessage(), null);
                        error = true;
                    }
                    if (!error && tc.isEnabled()) mCommonDlg.showCommonDialog(false, "I",
                            mContext.getString(R.string.msgs_zip_move_file_completed), conf_list, null);
                }
            }
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (move_mode) mGp.copyCutItemClear.performClick();
                    setUiEnabled();
                    hideDialog();
                }
            });
            return true;
        }
        return false;
    }

    ;

    private String mMainPassword = "";

    private boolean extractMultipleItem(final ThreadCtrl tc, final String dest_path, final ZipFile zf,
                                        final ArrayList<FileHeader> selected_fh_list, final ArrayList<FileHeader> extracted_fh_list,
                                        final String conf_list, final boolean move_mode) {

        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered, size=" + selected_fh_list.size());
        try {
            while (selected_fh_list.size() > 0) {
                final FileHeader fh_item = selected_fh_list.get(0);
                selected_fh_list.remove(0);
                extracted_fh_list.add(fh_item);
                if (fh_item.isDirectory()) {
                    if (dest_path.startsWith(mGp.externalRootDirectory)) {
                        String fp=dest_path + "/" + fh_item.getFileName().replace(mGp.copyCutCurrentDirectory, "");
                        SafFile sf = mUtil.createSafFile(fp, true);
                        if (sf==null) {
                            return false;
                        }
                        sf.exists();
                    } else {
                        File lf = new File(dest_path + "/" + fh_item.getFileName().replace(mGp.copyCutCurrentDirectory, ""));
                        lf.mkdirs();
                    }
                } else {
//					if (!tc.isEnabled()) return true;
                    final NotifyEvent ntfy_pswd = new NotifyEvent(mContext);
                    ntfy_pswd.setListener(new NotifyEventListener() {
                        @Override
                        public void positiveResponse(Context c, Object[] o) {
                            mMainPassword = (String) o[0];
                            extractSelectedItem(tc, dest_path, zf, selected_fh_list, extracted_fh_list, fh_item,
                                    conf_list, true, move_mode);
                        }

                        @Override
                        public void negativeResponse(Context c, Object[] o) {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setUiEnabled();
                                    hideDialog();
                                }
                            });
                        }
                    });

                    if (fh_item.isEncrypted()) {
                        if (!mMainPassword.isEmpty()) {
                            zf.setPassword(mMainPassword);
                            if (!ZipFileManager.isCorrectZipFilePassword(zf, fh_item, mMainPassword)) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ZipFileManager.getZipPasswordDlg(mActivity, mGp, mMainPassword, zf, fh_item,
                                                ntfy_pswd, true);
                                    }
                                });
                                break;
                            } else {
                                boolean rc = extractSelectedItem(tc, dest_path, zf, selected_fh_list, extracted_fh_list, fh_item,
                                        conf_list, false, move_mode);
                                if (!rc) break;
                            }
                        } else {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ZipFileManager.getZipPasswordDlg(mActivity, mGp, mMainPassword, zf, fh_item,
                                            ntfy_pswd, true);
                                }
                            });
                            break;
                        }
                    } else {
                        boolean rc = extractSelectedItem(tc, dest_path, zf, selected_fh_list, extracted_fh_list, fh_item,
                                conf_list, false, move_mode);
                        if (!rc) break;
                    }
                }
                isExtractEnded(tc, dest_path, zf, selected_fh_list, extracted_fh_list, conf_list, move_mode);
                if (!tc.isEnabled()) break;
            }
        } catch (ZipException e) {
            mUtil.addLogMsg("I", e.getMessage());
            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
            return false;
        }
        return true;
    }

    private boolean extractSelectedItem(final ThreadCtrl tc, final String dest_path, final ZipFile zf,
                                        final ArrayList<FileHeader> selected_fh_list, final ArrayList<FileHeader> extracted_fh_list,
                                        FileHeader fh_item, String conf_list, boolean call_child, final boolean move_mode) {
        String dir = "", fn = fh_item.getFileName();
        boolean result = true;
        if (fh_item.getFileName().lastIndexOf("/") > 0) {
            dir = fh_item.getFileName().substring(0, fh_item.getFileName().lastIndexOf("/")).replace(mGp.copyCutCurrentDirectory, "");
            fn = fh_item.getFileName().substring(fh_item.getFileName().lastIndexOf("/") + 1);
        }
//		Log.v("","dir="+dir+", fn="+fn+", cd="+mGp.copyCutCurrentFilePath);
        if (confirmReplace(tc, dest_path + dir + "/" + fn)) {
            if (extractSpecificFile(tc, zf, fh_item.getFileName(), dest_path + dir, fn)) {
                if (tc.isEnabled()) {
                    putProgressMessage(
                            String.format(mContext.getString(R.string.msgs_zip_extract_file_was_extracted), fh_item.getFileName()));
                    mUtil.addLogMsg("I",
                            String.format(mContext.getString(R.string.msgs_zip_extract_file_was_extracted), fh_item.getFileName()));
                    if (call_child && !isExtractEnded(tc, dest_path, zf, selected_fh_list, extracted_fh_list, conf_list, move_mode))
                        extractMultipleItem(tc, dest_path, zf, selected_fh_list, extracted_fh_list, conf_list, move_mode);
                } else {
                    result = false;
//					mCommonDlg.showCommonDialog(false, "W",
//							mContext.getString(R.string.msgs_zip_extract_file_was_cancelled), "", null);
                    refreshFileList();
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setUiEnabled();
                            hideDialog();
                        }
                    });
                }
            } else {
                result = false;
                mCommonDlg.showCommonDialog(false, "E",
                        mContext.getString(R.string.msgs_zip_extract_file_was_failed), tc.getThreadMessage(), null);
                refreshFileList();
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setUiEnabled();
                        hideDialog();
                    }
                });
            }
        } else {
            //Reject replace request
            if (tc.isEnabled()) {
                putProgressMessage(
                        mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + dest_path + "/" + dir + "/" + fn);
                mUtil.addLogMsg("I",
                        mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + dest_path + "/" + dir + "/" + fn);
                if (call_child && !isExtractEnded(tc, dest_path, zf, selected_fh_list, extracted_fh_list, conf_list, move_mode))
                    extractMultipleItem(tc, dest_path, zf, selected_fh_list, extracted_fh_list, conf_list, move_mode);
            }
        }
        if (!tc.isEnabled()) {
            result = false;
            mCommonDlg.showCommonDialog(false, "W",
                    mContext.getString(R.string.msgs_zip_extract_file_was_cancelled), "", null);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    setUiEnabled();
                    hideDialog();
                }
            });
        }
        return result;
    }

    private boolean extractSpecificFile(ThreadCtrl tc, ZipFile zf, String zip_file_name,
                                        String dest_path, String dest_file_name) {
        if (dest_path.startsWith(mGp.internalRootDirectory))
            return extractSpecificFileByInternal(tc, zf, zip_file_name, dest_path, dest_file_name);
        else return extractSpecificFileByExternal(tc, zf, zip_file_name, dest_path, dest_file_name);
    }

    private boolean extractSpecificFileByInternal(ThreadCtrl tc, ZipFile zf, String zip_file_name,
                                                  String dest_path, String dest_file_name) {
        mUtil.addDebugMsg(1, "I",
                "extractSpecificFile entered, zip file name=" + zip_file_name + ", dest=" + dest_path + ", dest file name=" + dest_file_name);
        boolean result = false;
        String w_path = dest_path.endsWith("/") ? dest_path + dest_file_name : dest_path + "/" + dest_file_name;
        File to = new File(w_path);
        try {
            if (tc.isEnabled()) {
                FileHeader fh = zf.getFileHeader(zip_file_name);
                InputStream is = zf.getInputStream(fh);
                File lf = new File(dest_path);
                lf.mkdirs();
                String temp_path = mGp.internalRootDirectory + "/" + APP_SPECIFIC_DIRECTORY + "/files/temp_file.tmp";
                File temp_to_file = new File(temp_path);
                FileOutputStream os = new FileOutputStream(temp_to_file);
                long fsz = fh.getUncompressedSize();
                long frc = 0;
                byte[] buff = new byte[IO_AREA_SIZE];
                int rc = is.read(buff);
                while (rc > 0) {
                    if (!tc.isEnabled()) break;
                    os.write(buff, 0, rc);
                    frc += rc;
                    long progress = (frc * 100) / (fsz);
                    putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_extract_file_extracting),
                            zip_file_name, progress));
                    rc = is.read(buff);
                }
                os.flush();
                os.close();
                is.close();
                temp_to_file.setLastModified(ZipUtil.dosToJavaTme(fh.getLastModFileTime()));
                if (to.exists()) to.delete();
                temp_to_file.renameTo(to);
                if (!tc.isEnabled()) to.delete();
                else {
                    CommonUtilities.scanMediaFile(mGp, mUtil, to.getAbsolutePath());
                }
            }
            result = true;
        } catch (ZipException e) {
            mUtil.addLogMsg("I", e.getMessage());
            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
            tc.setThreadMessage(e.getMessage());
            to.delete();
        } catch (IOException e) {
            mUtil.addLogMsg("I", e.getMessage());
            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
            tc.setThreadMessage(e.getMessage());
            to.delete();
        }
        mUtil.addDebugMsg(1, "I",
                "extractSpecificFile result=" + result + ", zip file name=" + zip_file_name + ", dest=" + dest_path + ", dest file name=" + dest_file_name);
        return result;
    }

    ;

    private boolean extractSpecificFileByExternal(ThreadCtrl tc, ZipFile zf, String zip_file_name,
                                                  String dest_path, String dest_file_name) {
        boolean result = false;
        try {
            if (tc.isEnabled()) {
                FileHeader fh = zf.getFileHeader(zip_file_name);
                InputStream is = zf.getInputStream(fh);

                String w_path = dest_path.endsWith("/") ? dest_path + dest_file_name : dest_path + "/" + dest_file_name;
                SafFile out_dir_sf = mGp.safMgr.createSdcardItem(dest_path, true);
                if (out_dir_sf == null) {
                    String e_msg = mGp.safMgr.getMessages();
                    mUtil.addLogMsg("E", "SafFile create error:dir=" + out_dir_sf + e_msg);
                    mCommonDlg.showCommonDialog(false, "E", "SafFile creation :dir=" + out_dir_sf, e_msg, null);
                    return false;
                }

                out_dir_sf.exists();
                SafFile out_file_sf = mGp.safMgr.createSdcardItem(w_path, false);
                if (out_file_sf == null) {
                    String e_msg = mGp.safMgr.getMessages();
                    mUtil.addLogMsg("E", "SafFile create error:fp=" + out_file_sf + e_msg);
                    mCommonDlg.showCommonDialog(false, "E", "SafFile creation :fp=" + out_file_sf, e_msg, null);
                    return false;
                }
                OutputStream os = mContext.getContentResolver().openOutputStream(out_file_sf.getUri());

                long fsz = fh.getUncompressedSize();
                long frc = 0;
                byte[] buff = new byte[IO_AREA_SIZE];
                int rc = is.read(buff);
                while (rc > 0) {
                    if (!tc.isEnabled()) break;
                    os.write(buff, 0, rc);
                    frc += rc;
                    long progress = (frc * 100) / (fsz);
                    putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_extract_file_extracting),
                            zip_file_name, progress));
                    rc = is.read(buff);
                }
                os.flush();
                os.close();
                is.close();
                if (!tc.isEnabled()) out_file_sf.delete();
                else CommonUtilities.scanMediaFile(mGp, mUtil, w_path);
            }
            result = true;
        } catch (ZipException e) {
            mUtil.addLogMsg("I", e.getMessage());
            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
            tc.setThreadMessage(e.getMessage());
        } catch (IOException e) {
            mUtil.addLogMsg("I", e.getMessage());
            CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
            tc.setThreadMessage(e.getMessage());
        }
        mUtil.addDebugMsg(1, "I",
                "extractSpecificFile result=" + result + ", zip file name=" + zip_file_name + ", dest=" + dest_path + ", dest file name=" + dest_file_name);
        return result;
    }

    ;

    private void confirmCopyFromLocal() {
        final String to_dir = mLocalStorage.getSelectedItem().toString() + mCurrentDirectory.getText().toString();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setUiDisabled();
                showDialogProgress();
                final ThreadCtrl tc = new ThreadCtrl();
                mDialogProgressSpinCancel.setEnabled(true);
                mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
                mDialogProgressSpinCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmCancel(tc, mDialogProgressSpinCancel);
                    }
                });
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        mUtil.addDebugMsg(1, "I", "Copy started");
                        String copied_item = "", copied_sep = "";
                        boolean process_aborted = false;
                        for (TreeFilelistItem tfl : mGp.copyCutList) {
                            File from_file = new File(tfl.getPath() + "/" + tfl.getName());
                            boolean rc = copyFile(tc, from_file, to_dir + "/" + tfl.getName());
                            if (rc) {
                                String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_copy_copied), tfl.getName());
                                mUtil.addLogMsg("I", msg);
                                putProgressMessage(msg);
                                copied_item += copied_sep + from_file;
                                CommonUtilities.scanMediaFile(mGp, mUtil, to_dir + "/" + tfl.getName());
                            } else {
                                if (!tc.isEnabled()) {
                                    String msg = mContext.getString(R.string.msgs_zip_local_file_copy_cancelled);
                                    mUtil.addLogMsg("I", msg);
                                    mCommonDlg.showCommonDialog(false, "W", msg, "", null);
                                    process_aborted = true;
                                    break;
                                } else {
                                    String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_copy_failed), tfl.getName());
                                    mUtil.addLogMsg("I", msg);
                                    mCommonDlg.showCommonDialog(false, "W", msg, tc.getThreadMessage(), null);
                                    process_aborted = true;
                                    break;
                                }
                            }
                        }
                        if (!process_aborted) {
//							putDialogMessage(false, "I",
//								mContext.getString(R.string.msgs_zip_local_file_copy_completed), copied_item, null);
                            mCommonDlg.showCommonDialog(false, "I",
                                    mContext.getString(R.string.msgs_zip_local_file_copy_completed), copied_item, null);
                        }
                        mUtil.addDebugMsg(1, "I", "Copy ended");
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshFileList();
                                setUiEnabled();
                            }
                        });
                    }
                };
                th.start();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String c_list = "", sep = "";
        for (TreeFilelistItem tfl : mGp.copyCutList) {
//			if(tfl.isChecked()) {
//			}
            c_list += sep + tfl.getName();
            sep = ",";
        }
        mCommonDlg.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_zip_local_file_copy_confirm_title), c_list, ntfy);
    }

    ;

    static final public int CONFIRM_RESPONSE_CANCEL = -99;
    static final public int CONFIRM_RESPONSE_YES = 1;
    static final public int CONFIRM_RESPONSE_YESALL = 2;
    static final public int CONFIRM_RESPONSE_NO = -1;
    static final public int CONFIRM_RESPONSE_NOALL = -2;
    private int mConfirmResponse = 0;

    private boolean confirmReplace(final ThreadCtrl tc, final String dest_path) {
        File lf = new File(dest_path);
//		Log.v("","name="+lf.getPath()+", exists="+lf.exists());
        if (lf.exists()) {
            if (mConfirmResponse != CONFIRM_RESPONSE_YESALL && mConfirmResponse != CONFIRM_RESPONSE_NOALL) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
                        mDialogConfirmView.setVisibility(LinearLayout.VISIBLE);
                        mDialogConfirmCancel.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
                                mDialogConfirmView.setVisibility(LinearLayout.GONE);
                                mConfirmResponse = CONFIRM_RESPONSE_CANCEL;
                                tc.setDisabled();
                                synchronized (tc) {
                                    tc.notify();
                                }
                            }
                        });

                        mDialogConfirmYes.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
                                mDialogConfirmView.setVisibility(LinearLayout.GONE);
                                mConfirmResponse = CONFIRM_RESPONSE_YES;
                                synchronized (tc) {
                                    tc.notify();
                                }
                            }
                        });
                        mDialogConfirmYesAll.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
                                mDialogConfirmView.setVisibility(LinearLayout.GONE);
                                mConfirmResponse = CONFIRM_RESPONSE_YESALL;
                                synchronized (tc) {
                                    tc.notify();
                                }
                            }
                        });
                        mDialogConfirmNo.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
                                mDialogConfirmView.setVisibility(LinearLayout.GONE);
                                mConfirmResponse = CONFIRM_RESPONSE_NO;
                                synchronized (tc) {
                                    tc.notify();
                                }
                            }
                        });
                        mDialogConfirmNoAll.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
                                mDialogConfirmView.setVisibility(LinearLayout.GONE);
                                mConfirmResponse = CONFIRM_RESPONSE_NOALL;
                                synchronized (tc) {
                                    tc.notify();
                                }
                            }
                        });
                        mDialogConfirmMsg.setText(
                                String.format(mContext.getString(R.string.msgs_zip_extract_file_confirm_replace_copy), dest_path));
                    }
                });

                synchronized (tc) {
                    try {
                        tc.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                boolean result = false;
                if (mConfirmResponse == CONFIRM_RESPONSE_CANCEL) {
                } else if (mConfirmResponse == CONFIRM_RESPONSE_YES) {
                    result = true;
                } else if (mConfirmResponse == CONFIRM_RESPONSE_YESALL) {
                    result = true;
                } else if (mConfirmResponse == CONFIRM_RESPONSE_NO) {
                } else if (mConfirmResponse == CONFIRM_RESPONSE_NOALL) {
                }
                return result;
            } else {
                boolean result = false;
                if (mConfirmResponse == CONFIRM_RESPONSE_YESALL) {
                    result = true;
                }
                return result;
            }
        } else {
            return true;
        }
    }

    private boolean copyFile(ThreadCtrl tc, File from_file, String to_path) {
        if (to_path.startsWith(mGp.internalRootDirectory))
            return copyFileInternal(tc, from_file, to_path);
        else return copyFileExternal(tc, from_file, to_path);
    }

    private boolean copyFileInternal(ThreadCtrl tc, File from_file, String to_path) {
        mUtil.addDebugMsg(1, "I", "copyFileInternal from=" + from_file + ", to=" + to_path);
        if (!tc.isEnabled()) return false;
        boolean result = false;
        if (from_file.isDirectory()) {
            File to_file = new File(to_path);
            to_file.mkdirs();
//			Log.v("","mkdirs="+to_path);
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = copyFileInternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
            } else {
                result = true;
            }
        } else {
            try {
                if (confirmReplace(tc, to_path)) {
                    String temp_path = mGp.internalRootDirectory + "/" + APP_SPECIFIC_DIRECTORY + "/files/temp_file.tmp";
                    FileOutputStream fos = new FileOutputStream(temp_path);
//					BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*8);
                    byte[] buff = new byte[IO_AREA_SIZE];
                    FileInputStream fis = new FileInputStream(from_file);
//					BufferedInputStream bis=new BufferedInputStream(fis, 1024*1024*8);
                    int rc = fis.read(buff);
                    long file_size = from_file.length();
                    long progress = 0, tot_rc = 0;
                    while (rc > 0) {
                        if (!tc.isEnabled()) break;
                        fos.write(buff, 0, rc);
                        tot_rc += rc;
                        progress = ((tot_rc * 100) / file_size);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_copy_copying), to_path, progress);
                        putProgressMessage(msg);
                        rc = fis.read(buff);
                    }
                    result = true;
                    fos.flush();
                    fos.close();
                    fis.close();
                    (new File(temp_path)).setLastModified(from_file.lastModified());
                    if ((new File(to_path)).exists()) (new File(to_path)).delete();
                    (new File(temp_path)).renameTo((new File(to_path)));
                } else {
                    //Reject replace request
                    if (tc.isEnabled()) {
                        putProgressMessage(
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        mUtil.addLogMsg("I",
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } catch (FileNotFoundException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            } catch (IOException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            }
        }
        return result;
    }

    ;

    private boolean copyFileExternal(ThreadCtrl tc, File from_file, String to_path) {
        mUtil.addDebugMsg(1, "I", "copyFileExternal from=" + from_file + ", to=" + to_path);
        if (!tc.isEnabled()) return false;
        boolean result = false;
        if (from_file.isDirectory()) {
            File[] fl = from_file.listFiles();
            if (fl != null && fl.length > 0) {
                for (File item : fl) {
                    result = copyFileExternal(tc, item, to_path + "/" + item.getName());
                    if (!result) break;
                    result = true;
                }
            } else {
                result = true;
            }
            SafFile out_sf = mUtil.createSafFile(to_path, true);
            if (out_sf==null) return false;
        } else {
            try {
                if (confirmReplace(tc, to_path)) {
                    SafFile out_sf = mUtil.createSafFile(to_path, false);
                    if (out_sf==null) return false;
                    OutputStream fos = mContext.getContentResolver().openOutputStream(out_sf.getUri());
//					BufferedOutputStream bos=new BufferedOutputStream(fos, 1024*1024*8);
                    byte[] buff = new byte[IO_AREA_SIZE];
                    FileInputStream fis = new FileInputStream(from_file);
//					BufferedInputStream bis=new BufferedInputStream(fis, 1024*1024*8);
                    int rc = fis.read(buff);
                    long file_size = from_file.length();
                    long progress = 0, tot_rc = 0;
                    while (rc > 0) {
                        if (!tc.isEnabled()) break;
                        fos.write(buff, 0, rc);
                        tot_rc += rc;
                        progress = ((tot_rc * 100) / file_size);
                        String msg = String.format(mContext.getString(R.string.msgs_zip_local_file_copy_copying), to_path, progress);
                        putProgressMessage(msg);
                        rc = fis.read(buff);
                    }
                    result = true;
                    fos.flush();
                    fos.close();
                    fis.close();
                } else {
                    //Reject replace request
                    if (tc.isEnabled()) {
                        putProgressMessage(
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        mUtil.addLogMsg("I",
                                mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced) + to_path);
                        result = true;
                    } else {
                        result = false;
                    }
                }
            } catch (FileNotFoundException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            } catch (IOException e) {
                mUtil.addLogMsg("I", e.getMessage());
                CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
                tc.setThreadMessage(e.getMessage());
            }
        }
        return result;
    }

    private boolean deleteLocalItem(ThreadCtrl tc, String fp) {
        if (!tc.isEnabled()) return false;
        boolean result = true;
        File lf = new File(fp);
        if (lf.exists()) {
            if (lf.isDirectory()) {
                File[] file_list = lf.listFiles();
                if (file_list != null) {
                    for (File item : file_list) {
                        if (item.isDirectory()) deleteLocalItem(tc, item.getPath());
                        else {
                            if (item.getPath().startsWith(mGp.externalRootDirectory)) {
                                SafFile del_sf = mUtil.createSafFile(item.getPath(), true);
                                if (del_sf==null) {
                                    return false;
                                }
                                result = del_sf.delete();
                            } else {
                                result = item.delete();
                            }
                            if (result) {
                                putProgressMessage(
                                        String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted),
                                                item.getPath()));
                                mUtil.addLogMsg("I",
                                        String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted),
                                                item.getPath()));
                                CommonUtilities.scanMediaFile(mGp, mUtil, item.getPath());
                            }
                        }
                        if (!result) break;
                    }
                }
                if (result) {
                    if (fp.startsWith(mGp.externalRootDirectory)) {
                        SafFile del_sf = mUtil.createSafFile(fp, true);
                        if (del_sf==null) {
                            return false;
                        }
                        result = del_sf.delete();
                    } else {
                        result = lf.delete();
                    }
                    if (result) {
                        putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), lf.getPath()));
                        mUtil.addLogMsg("I", String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), lf.getPath()));
                    }
                }
            } else {
                if (fp.startsWith(mGp.externalRootDirectory)) {
                    SafFile del_sf = mUtil.createSafFile(fp, false);
                    if (del_sf==null) {
                        return false;
                    }
                    result = del_sf.delete();
                } else {
                    result = lf.delete();
                }
                if (result) {
                    putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), lf.getPath()));
                    mUtil.addLogMsg("I", String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), lf.getPath()));
                    CommonUtilities.scanMediaFile(mGp, mUtil, lf.getPath());
                }

            }
        }
        return result;
    }

    private void confirmScan(final CustomTreeFilelistAdapter tfa) {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setUiDisabled();
				showDialogProgress();
				final ThreadCtrl tc=new ThreadCtrl();
				mDialogProgressSpinCancel.setEnabled(true);
				mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
				mDialogProgressSpinCancel.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						confirmCancel(tc,mDialogProgressSpinCancel);
					}
				});
				putProgressMessage(mContext.getString(R.string.msgs_local_file_scan_scan_please_wait));
				Thread th=new Thread(){
					@Override
					public void run() {
						mUtil.addDebugMsg(1, "I", "Scan started");
//						String deleted_item="", deleted_sep="";
						boolean process_abrted=false;
						for (TreeFilelistItem tfli:tfa.getDataList()) {
							if (tfli.isChecked()) {
								CommonUtilities.scanLocalFile(mGp, mUtil, tc, new File(tfli.getPath()+"/"+tfli.getName()));
								if (!tc.isEnabled()) {
									String msg=String.format(mContext.getString(R.string.msgs_local_file_scan_scan_was_cancelled),tfli.getName());
									mCommonDlg.showCommonDialog(false, "W", msg, "", null);
									process_abrted=true;
									break;
								}
							}
						}
						if (!process_abrted) {
							mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_local_file_scan_scan_was_completed), "", null);
						}
						mUtil.addDebugMsg(1, "I", "Scan ended");

						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								setUiEnabled();
							}
						});
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_local_file_scan_confirm_scan),
				"", ntfy);
	};
	
	private void confirmDelete(final CustomTreeFilelistAdapter tfa) {
//		String conf_list="";
//		String sep="";
//		StringBuilder sb=new StringBuilder(1024*1024);
//		for (TreeFilelistItem tfli:tfa.getDataList()) {
//			if (tfli.isChecked()) {
//				sb.append(sep).append(tfli.getPath()+"/"+tfli.getName());
//				sep="\n";
//			}
//		}
//		conf_list=sb.toString();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setUiDisabled();
				showDialogProgress();
				final ThreadCtrl tc=new ThreadCtrl();
				mDialogProgressSpinCancel.setEnabled(true);
				mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
				mDialogProgressSpinCancel.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						confirmCancel(tc,mDialogProgressSpinCancel);
					}
				});
				Thread th=new Thread(){
					@Override
					public void run() {
						mUtil.addDebugMsg(1, "I", "Delete started");
//						String deleted_item="", deleted_sep="";
						boolean process_abrted=false;
						for (TreeFilelistItem tfli:tfa.getDataList()) {
							if (tfli.isChecked()) {
								if (!deleteLocalItem(tc, tfli.getPath()+"/"+tfli.getName())) {
									if (!tc.isEnabled()) {
										String msg=String.format(mContext.getString(R.string.msgs_zip_delete_file_was_cancelled),tfli.getName());
										mCommonDlg.showCommonDialog(false, "W", msg, "", null);
									} else {
										String msg=String.format(mContext.getString(R.string.msgs_zip_delete_file_was_failed),tfli.getName());
										mCommonDlg.showCommonDialog(false, "W", msg, "", null);
									}
									process_abrted=true;
									break;
								} else {
//									deleted_item+=deleted_sep+tfli.getPath()+"/"+tfli.getName();
//									deleted_sep=", ";
								}
							}
						}
						if (!process_abrted) {
							mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_zip_delete_file_completed), "", null);
						}
						mUtil.addDebugMsg(1, "I", "Delete ended");

//						final String cdir=mLocalFileCurrentDirectory.getText().toString();
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								mGp.copyCutList.clear();
								mGp.copyCutType=GlobalParameters.COPY_CUT_FROM_LOCAL;
								mGp.copyCutItemInfo.setVisibility(TextView.GONE);
								mGp.copyCutItemClear.setVisibility(Button.GONE);
								mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
                                mTreeFilelistAdapter.setAllItemUnchecked();
								refreshFileList();
								setUiEnabled();
							}
						});
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_zip_delete_confirm_delete),
				"", ntfy);
	};

	private void putProgressMessage(final String msg) {
		mUiHandler.post(new Runnable(){
			@Override
			public void run() {
				mDialogProgressSpinMsg2.setText(msg);
			}
		});
	};

	@SuppressWarnings("unused")
	private void putDialogMessage(final boolean negative, final String msg_type, final String msg_title, 
			final String msg_body, final NotifyEvent ntfy) {
		mUiHandler.post(new Runnable(){
			@Override
			public void run() {
				setUiDisabled();
				mDialogMessageView.setVisibility(LinearLayout.VISIBLE);
				if (negative) {
					mDialogMessageOk.setVisibility(Button.VISIBLE);
					mDialogMessageCancel.setVisibility(Button.VISIBLE);
					mDialogMessageClose.setVisibility(Button.GONE);
				} else {
					mDialogMessageOk.setVisibility(Button.GONE);
					mDialogMessageCancel.setVisibility(Button.GONE);
					mDialogMessageClose.setVisibility(Button.VISIBLE);
				}
				mDialogMessageTitle.setText(msg_title);
				if (!msg_title.equals("")) {
					mDialogMessageTitle.setVisibility(TextView.VISIBLE);
				} else {
					mDialogMessageTitle.setVisibility(TextView.GONE);
				}
				mDialogMessageBody.setText(msg_body);
				if (!msg_title.equals("")) {
					mDialogMessageBody.setVisibility(TextView.VISIBLE);
				} else {
					mDialogMessageBody.setVisibility(TextView.GONE);
				}
				
				mDialogMessageOk.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if (ntfy!=null) ntfy.notifyToListener(true, null);
						mDialogMessageView.setVisibility(LinearLayout.GONE);
						setUiEnabled();
					}
				});
				mDialogMessageCancel.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if (ntfy!=null) ntfy.notifyToListener(false, null);
						mDialogMessageView.setVisibility(LinearLayout.GONE);
						setUiEnabled();
					}
				});
				mDialogMessageClose.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						mDialogMessageView.setVisibility(LinearLayout.GONE);
						setUiEnabled();
					}
				});
			}
		});
	};

	final private void refreshOptionMenu() {
		mActivity.invalidateOptionsMenu();
	};

	private void setUiEnabled() {
		mActivity.setUiEnabled();
		mTreeFilelistAdapter.setCheckBoxEnabled(isUiEnabled());
		mTreeFilelistAdapter.notifyDataSetChanged();
		hideDialog();
		refreshOptionMenu();
//		Thread.dumpStack();
	};
	
	private void setUiDisabled() {
		mActivity.setUiDisabled();
		mTreeFilelistAdapter.setCheckBoxEnabled(false);
		mTreeFilelistAdapter.notifyDataSetChanged();
		refreshOptionMenu();
//		Thread.dumpStack();
	};



	public void setContextButtonPasteEnabled(boolean enabled) {
		if (enabled) setContextButtonViewVisibility(mContextButtonPasteView);
		else mContextButtonPasteView.setVisibility(LinearLayout.INVISIBLE);
	};
	
	private boolean isUiEnabled() {
		return mActivity.isUiEnabled();
	};

	private void showDialogProgress() {
		mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
//		Thread.dumpStack();
	};
	
	private void hideDialog() {
		mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
		mDialogConfirmView.setVisibility(LinearLayout.GONE);
//		Thread.dumpStack();
	};
	
	class SavedViewContent {
		public int pos_x=0, pos_y=0;
		public String curr_dir="/";
		public ArrayList<TreeFilelistItem> tree_list=null;
		public String main_file_path="";
		
		public boolean sort_ascendant=true;
		public boolean sort_key_name=true;
		public boolean sort_key_size=false;
		public boolean sort_key_time=false;
	};
	
	private SavedViewContent mInternalViewContent=new SavedViewContent(), mExternalViewContent=new SavedViewContent();
	
	private ArrayList<FileManagerDirectoryListItem> mDirectoryList=new ArrayList<FileManagerDirectoryListItem>();

	private void openSppecificDirectory(String dir_name, String file_name) {
		String curr_dir=mLocalStorage.getSelectedItem().toString()+mCurrentDirectory.getText().toString();
		if (mCurrentDirectory.getText().toString().equals("/")) curr_dir=mLocalStorage.getSelectedItem().toString();
		FileManagerDirectoryListItem dli=CommonUtilities.getDirectoryItem(mDirectoryList, curr_dir);
		if (dli==null) {
			dli=new FileManagerDirectoryListItem();
			dli.file_path=curr_dir;
			mDirectoryList.add(dli);
		}
		dli.file_list=mTreeFilelistAdapter.getDataList();
		dli.pos_x=mTreeFilelistView.getFirstVisiblePosition();
		dli.pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
		
		ArrayList<TreeFilelistItem> tfl=createTreeFileList(dir_name);
		mTreeFilelistAdapter.setDataList(tfl);
		if (tfl.size()>0) mTreeFilelistView.setSelection(0);
        setCurrentDirectoryText(dir_name.replace(mLocalStorage.getSelectedItem().toString(), ""));
		setTopUpButtonEnabled(true);
		
		if (mTreeFilelistAdapter.getCount()==0) {
        	mTreeFilelistView.setVisibility(ListView.GONE);
	        mFileEmpty.setVisibility(TextView.VISIBLE);
	        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
        } else {
	        mFileEmpty.setVisibility(TextView.GONE);
	        mTreeFilelistView.setVisibility(ListView.VISIBLE);
	        
			int sel_pos=0;
			if (tfl.size()>0) {
				if (!file_name.equals("")) {
					for(int i=0;i<tfl.size();i++) {
						TreeFilelistItem tfli=tfl.get(i);
						if (tfli.getName().equals(file_name)) {
							sel_pos=i;
//							tfli.setChecked(false);
							break;
						}
					}
				}
				mTreeFilelistView.setSelection(sel_pos);
			}

        }
		if (isCopyCutDestValid(dir_name)) {
            setContextButtonViewVisibility(mContextButtonPasteView);
		} else {
			mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
		}
		mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
	};
	
    private void setContextButtonShareVisibility() {
    	if (mTreeFilelistAdapter.isItemSelected()) {
   			mContextButtonShareView.setVisibility(ImageButton.VISIBLE);
    	} else {
    		mContextButtonShareView.setVisibility(ImageButton.INVISIBLE);
    	}
    };

	private NotifyEvent mNotifyCheckBoxChanged=null;
	private void setTreeFileListener() {
		mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
		mLocalStorage.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String n_fp=mLocalStorage.getSelectedItem().toString();
				if (mMainFilePath.startsWith(mGp.internalRootDirectory)) {
					mInternalViewContent.tree_list=mTreeFilelistAdapter.getDataList();
					mInternalViewContent.pos_x=mTreeFilelistView.getFirstVisiblePosition();
					mInternalViewContent.pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
					mInternalViewContent.curr_dir=mCurrentDirectory.getText().toString();
					mInternalViewContent.sort_ascendant=mTreeFilelistAdapter.isSortAscendant();
					mInternalViewContent.sort_key_name=mTreeFilelistAdapter.isSortKeyName();
					mInternalViewContent.sort_key_size=mTreeFilelistAdapter.isSortKeySize();
					mInternalViewContent.sort_key_time=mTreeFilelistAdapter.isSortKeyTime();
				} else {
					mExternalViewContent.tree_list=mTreeFilelistAdapter.getDataList();
					mExternalViewContent.pos_x=mTreeFilelistView.getFirstVisiblePosition();
					mExternalViewContent.pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
					mExternalViewContent.curr_dir=mCurrentDirectory.getText().toString();
					mExternalViewContent.sort_ascendant=mTreeFilelistAdapter.isSortAscendant();
					mExternalViewContent.sort_key_name=mTreeFilelistAdapter.isSortKeyName();
					mExternalViewContent.sort_key_size=mTreeFilelistAdapter.isSortKeySize();
					mExternalViewContent.sort_key_time=mTreeFilelistAdapter.isSortKeyTime();
                }
				mMainFilePath=n_fp;
				ArrayList<TreeFilelistItem> prev_tfl=null;
				String w_curr_dir="";
				int pos_x=0, pos_y=0;
				boolean sort_asc=true, sort_name=true, sort_size=false, sort_time=false;
				if (mMainFilePath.startsWith(mGp.internalRootDirectory)) {
					prev_tfl=mInternalViewContent.tree_list;
					pos_x=mInternalViewContent.pos_x;
					pos_y=mInternalViewContent.pos_y;
					w_curr_dir=mInternalViewContent.curr_dir;
					sort_asc=mInternalViewContent.sort_ascendant;
					sort_name=mInternalViewContent.sort_key_name;
					sort_size=mInternalViewContent.sort_key_size;
					sort_time=mInternalViewContent.sort_key_time;
				} else {
					if (mExternalViewContent.tree_list==null) {
						createFileList(mMainFilePath, null);
						prev_tfl=mTreeFilelistAdapter.getDataList();
					} else {
						prev_tfl=mExternalViewContent.tree_list;
						pos_x=mExternalViewContent.pos_x;
						pos_y=mExternalViewContent.pos_y;
						w_curr_dir=mExternalViewContent.curr_dir;
						sort_asc=mExternalViewContent.sort_ascendant;
						sort_name=mExternalViewContent.sort_key_name;
						sort_size=mExternalViewContent.sort_key_size;
						sort_time=mExternalViewContent.sort_key_time;
					}
				}

                setSdcardGrantMsg();

                String curr_dir="";
			    if (w_curr_dir.equals("/")) {
			    	curr_dir=mMainFilePath;
			    } else {
			    	curr_dir=mMainFilePath+w_curr_dir;
			    }

			    if (sort_asc) mTreeFilelistAdapter.setSortAscendant();
			    else mTreeFilelistAdapter.setSortDescendant();
			    if (sort_name) mTreeFilelistAdapter.setSortKeyName();
			    else if (sort_size) mTreeFilelistAdapter.setSortKeySize();
			    else if (sort_time) mTreeFilelistAdapter.setSortKeyTime();
			    
				createFileList(curr_dir, null);
				
				mTreeFilelistView.setSelectionFromTop(pos_x, pos_y);
				
				for(TreeFilelistItem prev_item:prev_tfl) {
					for(TreeFilelistItem new_item:mTreeFilelistAdapter.getDataList()) {
						if (prev_item.getPath().equals(new_item.getPath()) && prev_item.getName().equals(new_item.getName())) {
							new_item.setChecked(prev_item.isChecked());
						}
					}
				}
				if (mTreeFilelistAdapter.isItemSelected()) {
					mContextButtonCopyView.setVisibility(ImageButton.VISIBLE);
                    setContextButtonViewVisibility(mContextButtonCutView);
				}
				if (mGp.copyCutList.size()>0) {
					mGp.copyCutItemInfo.setVisibility(TextView.VISIBLE);
					mGp.copyCutItemClear.setVisibility(Button.VISIBLE);
					if (isCopyCutDestValid(curr_dir))
                        setContextButtonViewVisibility(mContextButtonPasteView);
				}
				mActivity.refreshOptionMenu();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
//        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
//        ntfy_expand_close.setListener(new NotifyEventListener(){
//			@Override
//			public void positiveResponse(Context c, Object[] o) {
//				if (!isUiEnabled()) return;
//				int idx=(Integer)o[0];
//	    		final int pos=mTreeFilelistAdapter.getItem(idx);
//	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getDataItem(pos);
//				if (tfi.getName().startsWith("---")) return;
//				if (tfi.isDir()){// && tfi.getSubDirItemCount()>0) {
////					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
////					ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir);
////					mTreeFilelistAdapter.setDataList(tfl);
////					mZipFileCurrentDirectory.setText("/"+dir);
//					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
//					ArrayList<TreeFilelistItem> tfl=createTreeFileList(dir);
//					mTreeFilelistAdapter.setDataList(tfl);
//					mTreeFilelistAdapter.sort();
//					mLocalFileCurrentDirectory.setText(dir);
//					setTopUpButtonEnabled(true);
//					
//					if (mTreeFilelistAdapter.getCount()==0) {
//			        	mTreeFilelistView.setVisibility(ListView.GONE);
//				        mLocalFileEmpty.setVisibility(TextView.VISIBLE);
//				        mLocalFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
//			        } else {
//				        mLocalFileEmpty.setVisibility(TextView.GONE);
//				        mTreeFilelistView.setVisibility(ListView.VISIBLE);
//			        }
//				}
//			}
//			@Override
//			public void negativeResponse(Context c, Object[] o) {
//			}
//        });
//        mTreeFilelistAdapter.setExpandCloseListener(ntfy_expand_close);
        
		if (mGp.safMgr.isSdcardMounted()) mContextButtonCreateView.setVisibility(ImageButton.VISIBLE);
		mContextButtonShareView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonRenameView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonDeleteView.setVisibility(ImageButton.INVISIBLE);
		mContextButtonArchiveView.setVisibility(ImageButton.INVISIBLE);
		mNotifyCheckBoxChanged=new NotifyEvent(mContext);
		mNotifyCheckBoxChanged.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setContextButtonShareVisibility();
				mContextButtonCreateView.setVisibility(ImageButton.INVISIBLE);
				if (mTreeFilelistAdapter.getSelectedItemCount()==1) setContextButtonViewVisibility(mContextButtonRenameView);
				else mContextButtonRenameView.setVisibility(ImageButton.INVISIBLE);
                setContextButtonViewVisibility(mContextButtonDeleteView);
				mContextButtonArchiveView.setVisibility(ImageButton.VISIBLE);
				mContextButtonCopyView.setVisibility(ImageButton.VISIBLE);
                setContextButtonViewVisibility(mContextButtonCutView);
				mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
				setContextButtonSelectUnselectVisibility();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				if (mTreeFilelistAdapter.isItemSelected()) {
                    setContextButtonViewVisibility(mContextButtonCreateView);
					setContextButtonShareVisibility();
					if (mTreeFilelistAdapter.getSelectedItemCount()==1) {
                        setContextButtonViewVisibility(mContextButtonRenameView);
					} else {
						mContextButtonRenameView.setVisibility(ImageButton.INVISIBLE);
					}
					mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
                    setContextButtonViewVisibility(mContextButtonDeleteView);
					mContextButtonArchiveView.setVisibility(ImageButton.VISIBLE);
					mContextButtonCopyView.setVisibility(ImageButton.VISIBLE);
                    setContextButtonViewVisibility(mContextButtonCutView);
				} else {
					String dir=mLocalStorage.getSelectedItem().toString()+mCurrentDirectory.getText();
					if (isCopyCutDestValid(dir)) {
                        setContextButtonViewVisibility(mContextButtonPasteView);
					} else {
						mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
					}
                    setContextButtonViewVisibility(mContextButtonCreateView);
					setContextButtonShareVisibility();
					mContextButtonRenameView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonDeleteView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonArchiveView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
				}
				setContextButtonSelectUnselectVisibility();
			}
		});
		mTreeFilelistAdapter.setCbCheckListener(mNotifyCheckBoxChanged);
        mTreeFilelistView.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		if (!isUiEnabled()) return;
//	    		final int pos=mTreeFilelistAdapter.getItem(idx);
	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getItem(idx);
				if (tfi.getName().startsWith("---")) return;
				if (!mTreeFilelistAdapter.isItemSelected() && tfi.isDirectory()) {
					String curr_dir=mLocalStorage.getSelectedItem().toString()+mCurrentDirectory.getText().toString();
					if (mCurrentDirectory.getText().toString().equals("/")) curr_dir=mLocalStorage.getSelectedItem().toString();
					FileManagerDirectoryListItem dli=CommonUtilities.getDirectoryItem(mDirectoryList, curr_dir);
					if (dli==null) {
						dli=new FileManagerDirectoryListItem();
						dli.file_path=curr_dir;
						mDirectoryList.add(dli);
					}
					dli.file_list=mTreeFilelistAdapter.getDataList();
					dli.pos_x=mTreeFilelistView.getFirstVisiblePosition();
					dli.pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
					
					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
					ArrayList<TreeFilelistItem> tfl=createTreeFileList(dir);
					mTreeFilelistAdapter.setDataList(tfl);
					if (tfl.size()>0) {
						mTreeFilelistView.setSelection(0);
					}
					setContextButtonSelectUnselectVisibility();
                    setCurrentDirectoryText(dir.replace(mLocalStorage.getSelectedItem().toString(), ""));
					setTopUpButtonEnabled(true);
					
					if (mTreeFilelistAdapter.getCount()==0) {
			        	mTreeFilelistView.setVisibility(ListView.GONE);
				        mFileEmpty.setVisibility(TextView.VISIBLE);
				        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
			        } else {
				        mFileEmpty.setVisibility(TextView.GONE);
				        mTreeFilelistView.setVisibility(ListView.VISIBLE);
			        }
					if (isCopyCutDestValid(dir)) {
                        setContextButtonViewVisibility(mContextButtonPasteView);
					} else {
						mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
					}
					mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
				} else {
					if (mTreeFilelistAdapter.isItemSelected()) {
						tfi.setChecked(!tfi.isChecked());
						mTreeFilelistAdapter.notifyDataSetChanged();
					} else {
						invokeBrowser(tfi.getPath(), tfi.getName(), "");
					}
				}
			}
        });

        mTreeFilelistView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        		if (!isUiEnabled()) return true;
//	    		final int pos=mTreeFilelistAdapter.getItem(position);
	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getItem(position);
				if (tfi.getName().startsWith("---")) return true;
				showContextMenu(tfi);
				return true;
			}
        });
        mFileListTop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!isUiEnabled()) return;
				FileManagerDirectoryListItem p_dli=CommonUtilities.getDirectoryItem(mDirectoryList, mMainFilePath);
				CommonUtilities.clearDirectoryItem(mDirectoryList, mMainFilePath);
				String dir="";
				ArrayList<TreeFilelistItem> tfl=createTreeFileList(mMainFilePath+"/"+dir);
				mTreeFilelistAdapter.setDataList(tfl);
				mCurrentDirectory.setText("/");
				if (tfl.size()>0) mTreeFilelistView.setSelection(0);
				setTopUpButtonEnabled(false);
				if (mTreeFilelistAdapter.getCount()==0) {
		        	mTreeFilelistView.setVisibility(ListView.GONE);
			        mFileEmpty.setVisibility(TextView.VISIBLE);
			        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
		        } else {
			        mFileEmpty.setVisibility(TextView.GONE);
			        mTreeFilelistView.setVisibility(ListView.VISIBLE);
			        mTreeFilelistView.setSelectionFromTop(p_dli.pos_x, p_dli.pos_y);
		        }
				if (isCopyCutDestValid(mMainFilePath)) {
                    setContextButtonViewVisibility(mContextButtonPasteView);
				} else {
					mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
				}
				mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
				mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
			}
        });

        mFileListUp.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!isUiEnabled()) return;
				String dir=mLocalStorage.getSelectedItem().toString()+mCurrentDirectory.getText().toString();
//				Log.v("","dir="+dir+", idx="+dir.lastIndexOf("/"));
				if (!dir.equals("")) {
					if (dir.lastIndexOf("/")>0) {
						FileManagerDirectoryListItem dli=CommonUtilities.getDirectoryItem(mDirectoryList, dir);
						CommonUtilities.removeDirectoryItem(mDirectoryList, dli);
						String n_dir=dir.substring(0,dir.lastIndexOf("/"));
						FileManagerDirectoryListItem p_dli=CommonUtilities.getDirectoryItem(mDirectoryList, n_dir);
//						Log.v("","n_dir="+dir);
						if (!mMainFilePath.equals(n_dir)) {
							ArrayList<TreeFilelistItem> tfl=createTreeFileList(n_dir);
							mTreeFilelistAdapter.setDataList(tfl);
                            setCurrentDirectoryText(n_dir.replace(mLocalStorage.getSelectedItem().toString(), ""));
							if (tfl.size()>0) {
//								mTreeFilelistView.setSelection(0);
								if (p_dli!=null) mTreeFilelistView.setSelectionFromTop(p_dli.pos_x, p_dli.pos_y);
							}
						} else {
							ArrayList<TreeFilelistItem> tfl=createTreeFileList(n_dir);
							mTreeFilelistAdapter.setDataList(tfl);
							mCurrentDirectory.setText("/");
							if (tfl.size()>0) {
//								mTreeFilelistView.setSelection(0);
								if (p_dli!=null) mTreeFilelistView.setSelectionFromTop(p_dli.pos_x, p_dli.pos_y);
							}
							setTopUpButtonEnabled(false);
						}
				        if (mTreeFilelistAdapter.getCount()==0) {
				        	mTreeFilelistView.setVisibility(ListView.GONE);
					        mFileEmpty.setVisibility(TextView.VISIBLE);
					        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
				        } else {
					        mFileEmpty.setVisibility(TextView.GONE);
					        mTreeFilelistView.setVisibility(ListView.VISIBLE);
				        }
						if (isCopyCutDestValid(n_dir)) {
                            setContextButtonViewVisibility(mContextButtonPasteView);
						} else {
							mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
						}
						mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
						mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
					}
				}
			}
        });
	}
	
	public boolean isUpButtonEnabled() {
		return mFileListUp.isEnabled(); 
	}

	public void setSdcardGrantMsg() {
        if (mMainFilePath.startsWith(mGp.internalRootDirectory)) {
            mLocalViewMsg.setVisibility(TextView.GONE);
        } else {
            if (!mGp.safMgr.isSdcardMounted()) {
                mLocalViewMsg.setVisibility(TextView.VISIBLE);
                mLocalViewMsg.setText(mContext.getString(R.string.msgs_main_external_sdcard_select_required_can_not_write));
            } else {
                mLocalViewMsg.setVisibility(TextView.GONE);
            }
        }
    }

    public void setUsbGrantMsg() {
	    if (!mGp.safMgr.getUsbRootPath().equals(SafManager.UNKNOWN_USB_DIRECTORY)) {

        }
    }

    private void setContextButtonViewVisibility(LinearLayout cbv) {
        if (mMainFilePath.startsWith(mGp.internalRootDirectory)) {
            cbv.setVisibility(LinearLayout.VISIBLE);
        } else {
            if (mGp.safMgr.isSdcardMounted()) cbv.setVisibility(LinearLayout.VISIBLE);
            else cbv.setVisibility(LinearLayout.INVISIBLE);
        }
    }

    public void performClickUpButton() {
		mFileListUp.setSoundEffectsEnabled(false);
		mFileListUp.performClick(); 
		mFileListUp.setSoundEffectsEnabled(true);
	}

	private void setTopUpButtonEnabled(boolean p) {
		mFileListUp.setEnabled(p);
		mFileListTop.setEnabled(p);
		if (p) {
			mFileListUp.setAlpha(1);
			mFileListTop.setAlpha(1);
		} else {
			mFileListUp.setAlpha(0.4f);
			mFileListTop.setAlpha(0.4f);
		}
	};
	
	private int mInfoDirectoryCount=0;
	private int mInfoFileCount=0;
	private long mInfoFileSize=0;
	private void getDirectorySize(File file) {
		if (file.isDirectory()) {
			mInfoDirectoryCount++;
			File[] cl=file.listFiles();
			if (cl!=null) {
				for(File cf:cl) {
					getDirectorySize(cf);
				}
			}
		} else {
			mInfoFileCount++;
			mInfoFileSize+=file.length();
		}
	};

	private void showContextMenu(final TreeFilelistItem tfi) {
        final CustomContextMenu mCcMenu = new CustomContextMenu(mContext.getResources(), mFragmentManager);
		String sel_list="",sep="";
		final CustomTreeFilelistAdapter tfa=new CustomTreeFilelistAdapter(mActivity, false, false, false);
		ArrayList<TreeFilelistItem> n_tfl=new ArrayList<TreeFilelistItem>();
		int sel_count=0;
		if (mTreeFilelistAdapter.isItemSelected()) {
			for(TreeFilelistItem s_tfi:mTreeFilelistAdapter.getDataList()) {
				if (s_tfi.isChecked()) {
					n_tfl.add(s_tfi.clone());
					sel_list+=sep+s_tfi.getName();
					sep=",";
					sel_count++;
				}
			}
		} else {
			TreeFilelistItem n_tfi=tfi.clone();
			n_tfi.setChecked(true);
			n_tfl.add(n_tfi);
			sel_list=n_tfi.getName();
			sel_count++;
		}
		tfa.setDataList(n_tfl);

        if (!tfi.isChecked()) {
            mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_select) + "(" + sel_list + ")", R.drawable.menu_active)
                    .setOnClickListener(new CustomContextMenuOnClickListener() {
                        @Override
                        public void onClick(CharSequence menuTitle) {
                            tfi.setChecked(!tfi.isChecked());
                            mTreeFilelistAdapter.notifyDataSetChanged();
                        }
                    });
        }

        mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_top),R.drawable.context_button_top)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                mTreeFilelistView.setSelection(0);
            }
        });

        mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_bottom),R.drawable.context_button_bottom)
                .setOnClickListener(new CustomContextMenuOnClickListener() {
            @Override
            public void onClick(CharSequence menuTitle) {
                mTreeFilelistView.setSelection(mTreeFilelistAdapter.getCount()-1);
            }
        });

        if (tfi.isDirectory() && sel_count==1) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_property)+
					"("+(n_tfl.get(0)).getName()+")",R.drawable.dialog_information)
	  			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					final ThreadCtrl tc=new ThreadCtrl();
					final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
							mContext.getString(R.string.msgs_search_file_dlg_searching), 
							mContext.getString(R.string.msgs_search_file_dlg_sort_wait),
							mContext.getString(R.string.msgs_common_dialog_cancel), 
							mContext.getString(R.string.msgs_common_dialog_canceling));
					
					NotifyEvent ntfy=new NotifyEvent(mContext);
					ntfy.setListener(new NotifyEventListener(){
						@Override
						public void positiveResponse(Context c, Object[] o) {}
						@Override
						public void negativeResponse(Context c, Object[] o) {
							tc.setDisabled();
							if (!tc.isEnabled()) psd.dismissAllowingStateLoss();
						}
					});
					psd.showDialog(mFragmentManager, psd, ntfy,true);
					Thread th=new Thread(){
						@Override
						public void run() {
							File lf=new File(tfi.getPath()+"/"+tfi.getName());
							mInfoDirectoryCount=-1;
							mInfoFileCount=0;
							mInfoFileSize=0;
							getDirectorySize(lf);
							psd.dismissAllowingStateLoss();
							String msg=mContext.getString(R.string.msgs_local_file_item_property_directory);
							mCommonDlg.showCommonDialog(false, "I", tfi.getName(), 
									String.format(msg, mInfoDirectoryCount, mInfoFileCount, mInfoFileSize), null);
						}
					};
					th.start();
				}
	  		});

			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_open_directory)+
					"("+(n_tfl.get(0)).getName()+")",R.drawable.cc_folder)
	  			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					String curr_dir=mLocalStorage.getSelectedItem().toString()+mCurrentDirectory.getText().toString();
					if (mCurrentDirectory.getText().toString().equals("/")) curr_dir=mLocalStorage.getSelectedItem().toString();
					FileManagerDirectoryListItem dli=CommonUtilities.getDirectoryItem(mDirectoryList, curr_dir);
					if (dli==null) {
						dli=new FileManagerDirectoryListItem();
						dli.file_path=curr_dir;
						mDirectoryList.add(dli);
					}
					dli.file_list=mTreeFilelistAdapter.getDataList();
					dli.pos_x=mTreeFilelistView.getFirstVisiblePosition();
					dli.pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
					
					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
					ArrayList<TreeFilelistItem> tfl=createTreeFileList(dir);
					mTreeFilelistAdapter.setDataList(tfl);
					if (tfl.size()>0) mTreeFilelistView.setSelection(0);
                    setCurrentDirectoryText(dir.replace(mLocalStorage.getSelectedItem().toString(), ""));
					mContextButtonArchiveView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonDeleteView.setVisibility(ImageButton.INVISIBLE);
					setTopUpButtonEnabled(true);
					
					if (mTreeFilelistAdapter.getCount()==0) {
			        	mTreeFilelistView.setVisibility(ListView.GONE);
				        mFileEmpty.setVisibility(TextView.VISIBLE);
				        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
			        } else {
				        mFileEmpty.setVisibility(TextView.GONE);
				        mTreeFilelistView.setVisibility(ListView.VISIBLE);
			        }
					if (isCopyCutDestValid(dir)) {
                        setContextButtonViewVisibility(mContextButtonPasteView);
					} else {
						mContextButtonPasteView.setVisibility(ImageButton.INVISIBLE);
					}
					mContextButtonCopyView.setVisibility(ImageButton.INVISIBLE);
					mContextButtonCutView.setVisibility(ImageButton.INVISIBLE);
				}
	  		});
		}
		if (sel_count==1) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_rename)+"("+sel_list+")",R.drawable.context_button_rename)
		  		.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					renameItem(tfa);
				}
		  	});
		}
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_delete)+"("+sel_list+")",R.drawable.context_button_trash)
	  		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				confirmDelete(tfa);
			}
	  	});
		
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_scan_media_store)+"("+sel_list+")", R.drawable.context_button_media_file_scan)
	  		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				confirmScan(tfa);
			}
	  	});
		
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_compress)+"("+sel_list+")",R.drawable.context_button_archive)
	  		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				prepareZipSelectedItem(tfa);
			}
	  	});
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_copy)+"("+sel_list+")",R.drawable.context_button_copy)
	  		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				copyItem(tfa);
			}
	  	});
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_cut)+"("+sel_list+")",R.drawable.context_button_cut)
	  		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				cutItem(tfa);
			}
	  	});
		if (mContextButtonPasteView.getVisibility()==LinearLayout.VISIBLE) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_paste),R.drawable.context_button_paste)
		  		.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					pasteItem();
				}
		  	});
		}
		if (!tfi.isDirectory() && sel_count==1) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_zip)+"("+(n_tfl.get(0)).getName()+")",R.drawable.context_button_archive)
		  		.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					invokeBrowser(tfi.getPath(), tfi.getName(), MIME_TYPE_ZIP);
				}
		  	});
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_text)+"("+(n_tfl.get(0)).getName()+")",R.drawable.cc_sheet)
	  			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					invokeBrowser(tfi.getPath(), tfi.getName(), MIME_TYPE_TEXT);
				}
	  		});
		}
		mCcMenu.createMenu();
	};
	
	private boolean copyFileInternalToSdcard(ThreadCtrl tc, String app_path, String sdcard_path) {
		long begin=System.currentTimeMillis();
		mUtil.addDebugMsg(1, "I", "copyFileInternalToSdcard copy stated");
		String msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_start);
		mUtil.addLogMsg("I", msg);
		putProgressMessage(msg);

		boolean result=false;
		SafFile out_sf=mUtil.createSafFile(sdcard_path, false);
		if (out_sf==null) {
            return false;
        }
		SafFile temp_sf=mUtil.createSafFile(sdcard_path+".tmp", false);
        if (temp_sf==null) {
            return false;
        }
		File from=new File(app_path);
		try {
			OutputStream fos=mContext.getContentResolver().openOutputStream(temp_sf.getUri());
			FileInputStream fis=new FileInputStream(from);
			byte[] buff=new byte[IO_AREA_SIZE];
			int rc=fis.read(buff);
			while(rc>0) {
				if (!tc.isEnabled()) break;
				fos.write(buff, 0, rc);
				rc=fis.read(buff);
			}
			fis.close();
			fos.flush();
			fos.close();
			if (tc.isEnabled()) {
				String o_name=out_sf.getName();
				out_sf.delete();
				temp_sf.renameTo(o_name);
				FileInputStream t_fis=new FileInputStream(sdcard_path);
				rc=t_fis.read(buff, 0, 1024);
				t_fis.close();
				from.delete();
			} else {
				temp_sf.delete();
			}
			result=tc.isEnabled();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			tc.setThreadMessage(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			tc.setThreadMessage(e.getMessage());
		}
		if (tc.isEnabled()) {
			if (result) {
				msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_end);
				mUtil.addLogMsg("I", msg);
			} else {
				msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_failed);
				mUtil.addLogMsg("E", msg+"\n"+tc.getThreadMessage());
			}
		} else {
			msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_cancelled);
			mUtil.addLogMsg("W", msg);
		}
		putProgressMessage(msg);

		mUtil.addDebugMsg(1, "I", "copyFileInternalToSdcard copy ended, elapsed time="+(System.currentTimeMillis()-begin));
		return result;
	};

	private void prepareZipSelectedItem(final CustomTreeFilelistAdapter tfa) {
		String conf_list="", sep="", w_out_fn="", w_out_dir="";
		for(TreeFilelistItem tfi:tfa.getDataList()) {
			if (tfi.isChecked()) {
				conf_list+=sep+tfi.getName();
				sep=",";
				if (w_out_fn.equals("")) {
					w_out_fn=tfi.getName()+".zip";
					w_out_dir=tfi.getPath();
				}
			}
		}
		final String out_fn=w_out_fn;
		final String out_dir=w_out_dir.replace(mLocalStorage.getSelectedItem().toString(), "");
		NotifyEvent ntfy_confirm=new NotifyEvent(mContext);
		ntfy_confirm.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				NotifyEvent ntfy_select_dest=new NotifyEvent(mContext);
				ntfy_select_dest.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						final File out_fl=new File((String)o[0]);
						NotifyEvent ntfy_create=new NotifyEvent(mContext);
						ntfy_create.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c,Object[] o) {
								NotifyEvent ntfy_zip=new NotifyEvent(mContext);
								ntfy_zip.setListener(new NotifyEventListener(){
									@Override
									public void positiveResponse(Context c, Object[] o) {
										String dest_dir="", dest_name="";
										if (out_fl.getPath().startsWith(mGp.externalRootDirectory)) {
											dest_dir=mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+WORK_DIRECTORY;
											File tlf=new File(dest_dir);
											tlf.mkdirs();
											dest_name="tmp.zip";
										} else {
											dest_dir=out_fl.getParent();
											dest_name=out_fl.getName();
										}
										ZipParameters zp=(ZipParameters)o[0];
										zp.setCompressFileExtentionExcludeList(mGp.settingNoCompressFileType);
										ArrayList<String> sel_list=new ArrayList<String>();
										for(TreeFilelistItem tfi:tfa.getDataList()) {
											if (tfi.isChecked()) {
												sel_list.add(tfi.getPath()+"/"+tfi.getName());
											}
										}
										String[] add_item=new String[sel_list.size()];
										for(int i=0;i<sel_list.size();i++) {
											add_item[i]=sel_list.get(i);
										}
										zipSelectedItem(zp, add_item, dest_dir, dest_name, out_fl);
									}
									@Override
									public void negativeResponse(Context c, Object[] o) {
									}
									
								});
								if (out_fl.getPath().startsWith(mGp.externalRootDirectory)) {
									SafFile out_sf=mUtil.createSafFile(out_fl.getPath(), true);
									if (out_sf!=null) out_sf.delete();
									else {
									    return;
                                    }
								} else {
									if (out_fl.isFile()) out_fl.delete();
								}
								ZipFileManager.getZipParmDlg(mUtil, mActivity, mGp, ENCODING_NAME_UTF8, "", 
										out_fl.getPath(), ntfy_zip);
							}
							@Override
							public void negativeResponse(Context c,Object[] o) {
							}
						});
						if (out_fl.exists()) {
							mCommonDlg.showCommonDialog(true, "W", 
									String.format(mContext.getString(R.string.msgs_zip_create_new_zip_file_already_exists),out_fl.getName()),
									"", ntfy_create);
						} else {
							ntfy_create.notifyToListener(true, null);
						}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				String sep=out_dir.equals("")?"/":"";  
				mCommonDlg.fileOnlySelectWithCreateLimitMP(mLocalStorage.getSelectedItem().toString(),
						out_dir, sep+out_fn, 
						mContext.getString(R.string.msgs_zip_create_new_zip_file_select_dest), ntfy_select_dest);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
			
		});
		mCommonDlg.showCommonDialog(true, "W", 
				mContext.getString(R.string.msgs_zip_create_new_zip_file_confirm), conf_list, ntfy_confirm);
	};
	
	private void zipSelectedItem(final ZipParameters zp, final String[] add_item, 
			final String dest_dir, final String dest_name, final File out_fl) {
		mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered, dest_dir="+dest_dir+
				", dest_file="+dest_name);
		setUiDisabled();
		showDialogProgress();
		final ThreadCtrl tc=new ThreadCtrl();
		mDialogProgressSpinMsg1.setVisibility(TextView.GONE);
		mDialogProgressSpinCancel.setEnabled(true);
		mDialogProgressSpinCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				confirmCancel(tc,mDialogProgressSpinCancel);
			}
		});
		Thread th=new Thread(){
			@Override
			public void run() {
				long b_time=System.currentTimeMillis();
				BufferedZipFile bzf=null;
				File lf=new File(dest_dir+"/"+dest_name);
				bzf=new BufferedZipFile(lf, ZipUtil.DEFAULT_ZIP_FILENAME_ENCODING, false);
				boolean copy_back_required=true;
				boolean process_aborted=false;
				String base_dir=out_fl.getPath().startsWith(mGp.externalRootDirectory)?mGp.externalRootDirectory:mGp.internalRootDirectory;
				String added_item="", added_sep="";
				putProgressMessage(mContext.getString(R.string.msgs_local_file_add_file_begin));
				
				zp.setDefaultFolderPath(base_dir);
				zp.setRootFolderInZip("");

				File processed_file=null;
				try {
					for(String item:add_item) {
						File in_file=new File(item);
						ArrayList<File>sel_list=new ArrayList<File>();
						ZipFileManager.getAllItemInLocalDirectory(sel_list, in_file);
						for(File sel_item:sel_list) {
							processed_file=sel_item;
							bzf.addItem(sel_item, zp);
							String msg="";
							if (!tc.isEnabled()) {
								msg=String.format(mContext.getString(R.string.msgs_local_file_add_file_cancelled),item);
								mUtil.addLogMsg("W", msg);
								mCommonDlg.showCommonDialog(false, "W",msg, "", null);
								copy_back_required=false;
								process_aborted=true;
								break;
							} else {
								mUtil.addLogMsg("I", 
										String.format(mContext.getString(R.string.msgs_local_file_add_file_added),sel_item.getPath()));
								putProgressMessage(String.format(mContext.getString(R.string.msgs_local_file_add_file_added),sel_item.getPath()));
							}
						}
						if (process_aborted) break;
						added_item+=added_sep+item;
						added_sep=", ";
					}
					bzf.close();
				} catch (ZipException e) {
					e.printStackTrace();
					String msg=String.format(mContext.getString(R.string.msgs_local_file_add_file_failed),processed_file.getPath());
					mUtil.addLogMsg("E", msg);
					mCommonDlg.showCommonDialog(false, "E",msg, e.getMessage(), null);
					copy_back_required=false;
					process_aborted=true;
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg=String.format(mContext.getString(R.string.msgs_local_file_add_file_failed),processed_file.getPath());
                    mUtil.addLogMsg("E", msg);
                    mCommonDlg.showCommonDialog(false, "E",msg, e.getMessage(), null);
                    copy_back_required=false;
                    process_aborted=true;
				}

				mUtil.addDebugMsg(1, "I", "zipSelectedItem elapsed time="+(System.currentTimeMillis()-b_time));
				if (copy_back_required && out_fl.getPath().startsWith(mGp.externalRootDirectory)) {
					copyFileInternalToSdcard(tc, dest_dir+"/"+dest_name, out_fl.getPath());	
					CommonUtilities.scanMediaFile(mGp, mUtil, out_fl.getPath());
				}

				if (!process_aborted) {
					mCommonDlg.showCommonDialog(false, "I", 
							mContext.getString(R.string.msgs_local_file_add_file_completed), added_item, null);
				}
                mUiHandler.postDelayed(new Runnable(){
					@Override
					public void run() {
						refreshFileList();
						setUiEnabled();
					}
				},100);
			}
		};
		th.start();
	};

	@SuppressLint("DefaultLocale")
	private boolean invokeBrowser(final String p_dir, final String f_name, String mime_type) {
		boolean result=false;
		String fid=CommonUtilities.getFileExtention(f_name);
		String mt="";
		if (!mime_type.equals("")) mt=mime_type;
		else if (fid.equals("log")) mt=MIME_TYPE_TEXT;
		else if (fid.equals("gz")) mt=MIME_TYPE_ZIP;
		else if (fid.equals("zip")) mt=MIME_TYPE_ZIP;
		else mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
			
		if (mt != null) {
			try {
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT>=24) {
                    intent.setDataAndType(Uri.parse("file://"+p_dir+"/"+f_name), mt);
//                    Uri uri=FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(p_dir+"/"+f_name));
//                    intent.setDataAndType(uri, mt);
//                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    intent.setDataAndType(Uri.parse("file://"+p_dir+"/"+f_name), mt);
                }
                intent.setDataAndType(Uri.parse("file://"+p_dir+"/"+f_name), mt);
                mActivity.startActivity(intent);
				result=true;
			} catch(ActivityNotFoundException e) {
				mCommonDlg.showCommonDialog(false,"E", 
						String.format(mContext.getString(R.string.msgs_zip_specific_extract_file_viewer_not_found),f_name,mt),"",null);
			}
		} else {
			mCommonDlg.showCommonDialog(false,"E", 
					String.format(mContext.getString(R.string.msgs_zip_specific_extract_mime_type_not_found),f_name),"",null);
		}
		return result;
	};
	
	private void createFileList(String fp, final NotifyEvent p_ntfy) {
		if (!fp.equals("")) {
//			File lf=new File(fp);
			mTreeFilelistView.setVisibility(ListView.VISIBLE);
			mContextButton.setVisibility(ListView.VISIBLE);
			mFileListUp.setVisibility(Button.VISIBLE);
			mFileListTop.setVisibility(Button.VISIBLE);
			if (fp.equals(mMainFilePath)) {
				setTopUpButtonEnabled(false);
			} else {
				setTopUpButtonEnabled(true);
			}

			ArrayList<TreeFilelistItem> tfl=createTreeFileList(fp);
			if (mTreeFilelistAdapter==null) mTreeFilelistAdapter=new CustomTreeFilelistAdapter(mActivity, false, true);
			mTreeFilelistAdapter.setDataList(tfl);
			mTreeFilelistAdapter.setCheckBoxEnabled(isUiEnabled());
			mTreeFilelistAdapter.notifyDataSetChanged();
			mTreeFilelistView.setAdapter(mTreeFilelistAdapter);
			if (fp.equals(mLocalStorage.getSelectedItem().toString())) mCurrentDirectory.setText("/");
			else setCurrentDirectoryText(fp.replace(mLocalStorage.getSelectedItem().toString(), ""));
			mCurrentDirectory.setVisibility(TextView.VISIBLE);
			mTreeFilelistView.setSelectionFromTop(0, 0);
			setTreeFileListener();
	        
	        if (tfl.size()==0) {
		        mContextButtonSelectAllView.setVisibility(LinearLayout.INVISIBLE);
		        mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
	        	mTreeFilelistView.setVisibility(ListView.GONE);
		        mFileEmpty.setVisibility(TextView.VISIBLE);
		        mFileEmpty.setText(R.string.msgs_zip_local_folder_empty);
	        } else {
		        mContextButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
		        mContextButtonUnselectAllView.setVisibility(LinearLayout.INVISIBLE);
                setContextButtonViewVisibility(mContextButtonCreateView);
                mFileEmpty.setVisibility(TextView.GONE);
		        mTreeFilelistView.setVisibility(ListView.VISIBLE);
	        }
	        
			if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
		} else {
			if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
//			p_ntfy.notifyToListener(false,new Object[]{ 
//					getString(R.string.msgs_text_browser_file_file_name_not_specified)});
		}
	};

	private void setCurrentDirectoryText(String cd) {

	    mCurrentDirectory.setText(cd.equals("")?"/":cd);
    }

	private ArrayList<TreeFilelistItem> createTreeFileList(String target_dir) {
		ArrayList<TreeFilelistItem> tfl=new ArrayList<TreeFilelistItem>();
		File[] fl=(new File(target_dir)).listFiles();
		if (fl!=null) {
			for (File item:fl) {
				TreeFilelistItem tfli=createNewFileListItem(item);
				tfl.add(tfli);
				if (tfli.isDirectory()) {
					File[] sub_fl=(new File(item.getPath())).listFiles();
					if (sub_fl!=null) tfli.setSubDirItemCount(sub_fl.length);
				}
			}
		}
		return tfl;
	};
	
	@SuppressLint("DefaultLocale")
	private TreeFilelistItem createNewFileListItem(final File item) {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault());
		if (item.isDirectory()) {

            final TreeFilelistItem tfi=new TreeFilelistItem(item.getName(),
					true, -1, item.lastModified(),
					false, item.canRead(), item.canWrite(),
					item.isHidden(), item.getParent(),0);
            Thread th=new Thread(){
                @Override
                public void run() {
                    long dir_size=getAllFileSizeInDirectory(item,true);
                    tfi.setLength(dir_size);
                    mUiHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            if (item.getName().equals(".PGMS")) {
                                mUtil.addLogMsg("I", ".PGMS size="+tfi.getLength());
                            }
                            mTreeFilelistAdapter.notifyDataSetChanged();
                        }
                    }) ;
                }
            };
            th.start();
            return tfi;
		} else {
            TreeFilelistItem tfi=new TreeFilelistItem(item.getName(),
					false, item.length(), item.lastModified(),
					false, item.canRead(), item.canWrite(),
					item.isHidden(), item.getParent(),0);
            return tfi;
		}
	};

    final static public long getAllFileSizeInDirectory(File sd, boolean process_sub_directories) {
        long dir_size=0;
        if (sd.exists()) {
            if (sd.isDirectory()) {
                File[] cfl=sd.listFiles();
                if (cfl!=null && cfl.length>0) {
                    for(File cf:cfl) {
                        if (cf.isDirectory()) {
                            if (process_sub_directories)
                                dir_size+=getAllFileSizeInDirectory(cf, process_sub_directories);
                        } else {
                            dir_size+=cf.length();
                        }
                    }
                }
            } else {
                dir_size+=sd.length();
            }
        }
        return dir_size;
    };


}
