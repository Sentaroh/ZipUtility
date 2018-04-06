package com.sentaroh.android.ZipUtility;

import static com.sentaroh.android.ZipUtility.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jConstants;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SafFile;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ZipFileListItem;
import com.sentaroh.android.Utilities.ZipUtil;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.FileSelectDialogFragment;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;
import com.sentaroh.android.Utilities.Widget.CustomTextView;

@SuppressWarnings("unused")
@SuppressLint("ClickableViewAccessibility")
public class TarFileManager {

	private GlobalParameters mGp=null;
	
	private FragmentManager mFragmentManager=null;
	private CommonDialog mCommonDlg=null;
	
	private Context mContext;
	private ActivityMain mActivity=null; 
	private String mLastMsgText="";

	private ArrayList<ZipFileListItem> mZipFileList=null;
	private ListView mTreeFilelistView=null;
	private CustomTreeFilelistAdapter mTreeFilelistAdapter=null;
	
	private Handler mUiHandler=null;
	private String mMainFilePath="";
	private String mMainPassword="";
	
	private Button mZipFileUp, mZipFileTop;
	private CustomTextView mZipFileCurrentDirectory; 
	private TextView mZipFileEmpty, mZipFileInfo;
	private Spinner mEncodingSpinner;
	private LinearLayout mMainDialogView=null;
	
	private CommonUtilities mUtil=null;
	
	private LinearLayout mMainView=null;
	private LinearLayout mDialogProgressSpinView=null;
	private TextView mDialogProgressSpinMsg1=null;
	private TextView mDialogProgressSpinMsg2=null;
	private Button mDialogProgressSpinCancel=null;

	private LinearLayout mDialogMessageView=null;
	private TextView mDialogMessageTitle=null;
	private TextView mDialogMessageBody=null;
	private Button mDialogMessageOk=null;
	private Button mDialogMessageClose=null;
	private Button mDialogMessageCancel=null;

	private LinearLayout mDialogConfirmView=null;
	private TextView mDialogConfirmMsg=null;
	private Button mDialogConfirmCancel=null;
	
	private Button mDialogConfirmYes=null;
	private Button mDialogConfirmYesAll=null;
	private Button mDialogConfirmNo=null;
	private Button mDialogConfirmNoAll=null;
	
	private LinearLayout mContextZipButton=null;

	private ImageButton mContextZipButtonExtract=null;
	private ImageButton mContextZipButtonAdd=null;
    private ImageButton mContextZipButtonDelete=null;
    private ImageButton mContextZipButtonSelectAll=null;
    private ImageButton mContextZipButtonUnselectAll=null;

	private LinearLayout mContextZipButtonExtractView=null;
	private LinearLayout mContextZipButtonAddView=null;
    private LinearLayout mContextZipButtonDeleteView=null;
    private LinearLayout mContextZipButtonSelectAllView=null;
    private LinearLayout mContextZipButtonUnselectAllView=null;

	public TarFileManager(GlobalParameters gp, ActivityMain a, FragmentManager fm, LinearLayout mv, String fp) {
		mGp=gp;
        mActivity=a;
		mCommonDlg=new CommonDialog(a, fm);
        mUiHandler=new Handler();
        mContext=a.getApplicationContext();
        mFragmentManager=fm;
        mMainFilePath=fp;
        mUtil=new CommonUtilities(mContext, "TarTab", gp);

        mZipFileNameEncodingDesired=mContext.getString(R.string.msgs_zip_parm_zip_encoding_auto);
        
        mMainView=mv;
        initViewWidget();

		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(final Context c, final Object[] o) {
			}
			@Override
			public void negativeResponse(Context c, final Object[] o) {
			}
		});
		createFileList(mMainFilePath,ntfy, "");

	};

	public void cleanupWorkDirectory() {
		final String work_dir=mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+WORK_DIRECTORY;
		File w_lf=new File(work_dir);
		File[] w_fl=w_lf.listFiles();
		if (w_fl!=null && w_fl.length>0) {
			for (File item:w_fl) item.delete();
		}
	};
	
	private void initViewWidget() {
		
		mContextZipButton=(LinearLayout)mMainView.findViewById(R.id.context_view_zip_file);
		
//		zipFileSpinner=(Spinner)mMainView.findViewById(R.id.zip_file_zip_file_spinner);
//		CommonUtilities.setSpinnerBackground(mActivity, mGp.zipFileSpinner, mGp.themeIsLight);
//		final CustomSpinnerAdapter adapter=
//				new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
////		mZipFileSpinner.setPrompt(mContext.getString(R.string.msgs_main_sync_profile_dlg_sync_folder_type_prompt));
//		mGp.zipFileSpinner.setAdapter(adapter);
		
		mEncodingSpinner=(Spinner)mMainView.findViewById(R.id.zip_file_encoding);
		CommonUtilities.setSpinnerBackground(mActivity, mEncodingSpinner, mGp.themeIsLight);
		final CustomSpinnerAdapter enc_adapter=
				new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
		enc_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		enc_adapter.add(mContext.getString(R.string.msgs_zip_parm_zip_encoding_auto));
		for(String item:ENCODING_NAME_LIST) enc_adapter.add(item);
		mEncodingSpinner.setAdapter(enc_adapter);
		mEncodingSpinner.setSelection(0);
		
		mMainDialogView=(LinearLayout)mMainView.findViewById(R.id.main_dialog_view);
        mTreeFilelistView=(ListView)mMainView.findViewById(R.id.zip_file_list);
        mZipFileEmpty=(TextView)mMainView.findViewById(R.id.zip_file_empty);
        mZipFileEmpty.setVisibility(TextView.GONE);
        mTreeFilelistView.setVisibility(ListView.VISIBLE);
        
        mZipFileInfo=(TextView)mMainView.findViewById(R.id.zip_file_info);
        
        mZipFileUp=(Button)mMainView.findViewById(R.id.zip_file_up_btn);
        if (mGp.themeIsLight) mZipFileUp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_dark, 0, 0, 0);
        else mZipFileUp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_up_light, 0, 0, 0);
        mZipFileTop=(Button)mMainView.findViewById(R.id.zip_file_top_btn);
        if (mGp.themeIsLight) mZipFileTop.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_dark, 0, 0, 0);
        else mZipFileTop.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_16_go_top_light, 0, 0, 0);

        mZipFileCurrentDirectory=(CustomTextView)mMainView.findViewById(R.id.zip_file_filepath);
        mZipFileCurrentDirectory.setTextColor(mGp.themeColorList.text_color_primary);


        mDialogProgressSpinView=(LinearLayout)mMainView.findViewById(R.id.main_dialog_progress_spin_view);
        mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
        mDialogProgressSpinMsg1=(TextView)mMainView.findViewById(R.id.main_dialog_progress_spin_syncprof);
        mDialogProgressSpinMsg2=(TextView)mMainView.findViewById(R.id.main_dialog_progress_spin_syncmsg);
        mDialogProgressSpinCancel=(Button)mMainView.findViewById(R.id.main_dialog_progress_spin_btn_cancel);

        mDialogMessageView=(LinearLayout)mMainView.findViewById(R.id.main_dialog_message_view);
        mDialogMessageView.setVisibility(LinearLayout.GONE);
        mDialogMessageTitle=(TextView)mMainView.findViewById(R.id.main_dialog_message_title);
        mDialogMessageBody=(TextView)mMainView.findViewById(R.id.main_dialog_message_body);
        mDialogMessageClose=(Button)mMainView.findViewById(R.id.main_dialog_message_close_btn);
        mDialogMessageCancel=(Button)mMainView.findViewById(R.id.main_dialog_message_cancel_btn);
        mDialogMessageOk=(Button)mMainView.findViewById(R.id.main_dialog_message_ok_btn);

        mDialogConfirmView=(LinearLayout)mMainView.findViewById(R.id.main_dialog_confirm_view);
        mDialogConfirmView.setVisibility(LinearLayout.GONE);
        mDialogConfirmMsg=(TextView)mMainView.findViewById(R.id.main_dialog_confirm_msg);
        mDialogConfirmCancel=(Button)mMainView.findViewById(R.id.main_dialog_confirm_sync_cancel);
        mDialogConfirmNo=(Button)mMainView.findViewById(R.id.copy_delete_confirm_no);
        mDialogConfirmNoAll=(Button)mMainView.findViewById(R.id.copy_delete_confirm_noall);
        mDialogConfirmYes=(Button)mMainView.findViewById(R.id.copy_delete_confirm_yes);
        mDialogConfirmYesAll=(Button)mMainView.findViewById(R.id.copy_delete_confirm_yesall);
        
    	mContextZipButtonExtract=(ImageButton)mMainView.findViewById(R.id.context_button_extract);
    	mContextZipButtonAdd=(ImageButton)mMainView.findViewById(R.id.context_button_add);
        mContextZipButtonDelete=(ImageButton)mMainView.findViewById(R.id.context_button_delete);
        mContextZipButtonSelectAll=(ImageButton)mMainView.findViewById(R.id.context_button_select_all);
        mContextZipButtonUnselectAll=(ImageButton)mMainView.findViewById(R.id.context_button_unselect_all);
        
    	mContextZipButtonExtractView=(LinearLayout)mMainView.findViewById(R.id.context_button_extract_view);
    	mContextZipButtonAddView=(LinearLayout)mMainView.findViewById(R.id.context_button_add_view);
        mContextZipButtonDeleteView=(LinearLayout)mMainView.findViewById(R.id.context_button_delete_view);
        mContextZipButtonSelectAllView=(LinearLayout)mMainView.findViewById(R.id.context_button_select_all_view);
        mContextZipButtonUnselectAllView=(LinearLayout)mMainView.findViewById(R.id.context_button_unselect_all_view);
        
        setZipContextButtonListener();
	};

	class SavedViewData implements Externalizable{
		private static final long serialVersionUID = 1L;
		public ArrayList<ZipFileListItem> zfl=new ArrayList<ZipFileListItem>();
		public ArrayList<TreeFilelistItem> tfl=new ArrayList<TreeFilelistItem>();
		public int tree_list_pos_x=0, tree_list_pos_y=0;
		public String curr_dir="", encoding_desired="", encoding_selected="";
		public int encoding_spinner_pos=0;
		
		public SavedViewData() {};
		
		@Override
		public void readExternal(ObjectInput input) throws IOException,
				ClassNotFoundException {
			int tc=input.readInt();
			zfl=new ArrayList<ZipFileListItem>(); 
			if (tc>0) for(int i=0;i<tc;i++) zfl.add((ZipFileListItem)input.readObject());
			tc=input.readInt();
			tfl=new ArrayList<TreeFilelistItem>(); 
			if (tc>0) for(int i=0;i<tc;i++) tfl.add((TreeFilelistItem)input.readObject());
			tree_list_pos_x=input.readInt();
			tree_list_pos_y=input.readInt();
			curr_dir=input.readUTF();
			encoding_desired=input.readUTF();
			encoding_selected=input.readUTF();
			encoding_spinner_pos=input.readInt();
		}
		@Override
		public void writeExternal(ObjectOutput output) throws IOException {
			output.writeInt(zfl.size());
			for(ZipFileListItem zi:zfl) output.writeObject(zi);
			output.writeInt(tfl.size());
			for(TreeFilelistItem ti:tfl) output.writeObject(ti);
			output.writeInt(tree_list_pos_x);
			output.writeInt(tree_list_pos_y);
			output.writeUTF(curr_dir);
			output.writeUTF(encoding_desired);
			output.writeUTF(encoding_selected);
			output.writeInt(encoding_spinner_pos);
		}
	};
	
	public String saveViewContents(Bundle outState) {
		SavedViewData sv=new SavedViewData();
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(bos);

			if (mZipFileList!=null) sv.zfl=mZipFileList;
			if (mTreeFilelistAdapter!=null) sv.tfl=mTreeFilelistAdapter.getDataList();
			if (mTreeFilelistView!=null) {
			    sv.tree_list_pos_x=mTreeFilelistView.getFirstVisiblePosition();
			    sv.tree_list_pos_y=mTreeFilelistView.getChildAt(0)==null?0:mTreeFilelistView.getChildAt(0).getTop();
			}
			sv.curr_dir=mZipFileCurrentDirectory.getText().toString();
			sv.encoding_desired=mZipFileNameEncodingDesired;
			sv.encoding_selected=mZipFileNameEncodingSelected;
			sv.encoding_spinner_pos=mEncodingSpinner.getSelectedItemPosition();
			sv.writeExternal(oos);
			oos.flush();
			oos.close();
			byte[] ba=bos.toByteArray();
			outState.putByteArray("saved_data", ba);
		} catch (IOException e) {
			e.printStackTrace();
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
		}
		return mMainFilePath;
	};

	public void restoreViewContents(String fp, Bundle inState) {
		byte[] ba=inState.getByteArray("saved_data");
		ByteArrayInputStream bis=new ByteArrayInputStream(ba);
		SavedViewData sv=new SavedViewData();
		sv.encoding_desired=mContext.getString(R.string.msgs_zip_parm_zip_encoding_auto);
		sv.encoding_selected=ENCODING_NAME_UTF8;
		try {
			ObjectInputStream ois=new ObjectInputStream(bis);
			sv.readExternal(ois);
			ois.close();
		} catch (StreamCorruptedException e) {
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
		} catch (IOException e) {
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
		} catch (ClassNotFoundException e) {
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
		}
		mMainFilePath=fp;
		mZipFileCurrentDirectory.setText(sv.curr_dir);
		mZipFileNameEncodingDesired=sv.encoding_desired;
		mZipFileNameEncodingSelected=sv.encoding_selected;
		mEncodingSpinner.setSelection(sv.encoding_spinner_pos);
		refreshFileList();
		mTreeFilelistView.setSelectionFromTop(sv.tree_list_pos_x, sv.tree_list_pos_y);
	};
	
	private String mFindKey="*";
	public void findItem() {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.search_file_dlg);
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.search_file_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.search_file_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		

		final CheckedTextView dlg_hidden = (CheckedTextView) dialog.findViewById(R.id.search_file_dlg_search_hidden_item);
		dlg_hidden.setVisibility(CheckedTextView.GONE);
		
		final CheckedTextView dlg_case_sensitive = (CheckedTextView) dialog.findViewById(R.id.search_file_dlg_search_case_sensitive);
		CommonUtilities.setCheckedTextView(dlg_case_sensitive);
		
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.search_file_dlg_msg);
		final Button btnOk = (Button) dialog.findViewById(R.id.search_file_dlg_ok_btn);
		final Button btnCancel = (Button) dialog.findViewById(R.id.search_file_dlg_cancel_btn);
		final EditText et_search_key=(EditText) dialog.findViewById(R.id.search_file_dlg_search_key);
		final ListView lv_searcgh_result=(ListView) dialog.findViewById(R.id.search_file_dlg_search_result);
		final AdapterSearchFileList tfa=new AdapterSearchFileList(mActivity);
		lv_searcgh_result.setAdapter(tfa);
		
		CommonDialog.setDlgBoxSizeLimit(dialog, true);
		
//		ll_option_view.setVisibility(LinearLayout.GONE);
//		dlg_option.setOnClickListener(new OnClickListener(){
//			@Override
//			public void onClick(View v) {
//				boolean isChecked=!dlg_option.isChecked();
//				dlg_option.setChecked(isChecked);
//				if (isChecked) ll_option_view.setVisibility(LinearLayout.VISIBLE);
//				else ll_option_view.setVisibility(LinearLayout.GONE);
//			}
//		});
		
		et_search_key.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()>0) btnOk.setEnabled(true);
				else btnOk.setEnabled(false);
			}
		});
		
		lv_searcgh_result.setOnItemClickListener(new OnItemClickListener(){
			@SuppressLint("DefaultLocale")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				TreeFilelistItem tfi=tfa.getItem(position);
				openSppecificDirectory(tfi.getPath());
				btnCancel.performClick();
//				String fid=CommonUtilities.getFileExtention(tfi.getName());
//				String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
//				invokeBrowser(tfi.getPath(), tfi.getName(), "");
////				Log.v("","mt="+mt);
//				if (mt!=null && mt.startsWith("application/zip")) {
//					btnCancel.performClick();
//				}
			}
		});

		et_search_key.setText(mFindKey);
		//OK button
//		btnOk.setEnabled(false);
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFindKey=et_search_key.getText().toString();
				final ArrayList<TreeFilelistItem>s_tfl=new ArrayList<TreeFilelistItem>();
				int flags = 0;//Pattern.CASE_INSENSITIVE;// | Pattern..MULTILINE;
				if (!dlg_case_sensitive.isChecked()) flags=Pattern.CASE_INSENSITIVE;
				final Pattern s_key=Pattern.compile("(" + MiscUtil.convertRegExp(mFindKey) + ")", flags);
				final ThreadCtrl tc=new ThreadCtrl();
				final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
						mContext.getString(R.string.msgs_search_file_dlg_searching), "",
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
						buildFileListBySearchKey(tc, psd, s_tfl, s_key);
						psd.dismissAllowingStateLoss();
						if (!tc.isEnabled()) {
							mCommonDlg.showCommonDialog(false, "W", 
									mContext.getString(R.string.msgs_search_file_dlg_search_cancelled), "", null);
						} else {
							tfa.setDataList(s_tfl);
							tfa.sort();
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									tfa.notifyDataSetChanged();
								}
							});
						}
					}
				};
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

	};
	
	private void buildFileListBySearchKey(final ThreadCtrl tc, ProgressSpinDialogFragment psd,
			ArrayList<TreeFilelistItem>s_tfl, Pattern s_key) {
		for(ZipFileListItem zfli:mZipFileList) {
			psd.updateMsgText(zfli.getFileName());
			if (!zfli.isDirectory()) {
				String fn=zfli.getFileName().lastIndexOf("/")>=0?zfli.getFileName().substring(zfli.getFileName().lastIndexOf("/")+1):zfli.getFileName();
				if (s_key.matcher(fn).matches()) {
					TreeFilelistItem tfli=createNewFileListItem(zfli);
					s_tfl.add(tfli);
				}
			}
		}
	};
	
	public void refreshFileList() {
		final String cdir=mZipFileCurrentDirectory.getText().toString();
		if (mMainFilePath.startsWith(mGp.externalRootDirectory)) {
			if (mGp.safMgr.getSdcardSafFile()==null) mActivity.startSdcardPicker();
		}
		ArrayList<TreeFilelistItem> p_tfl=null;
		if (mTreeFilelistAdapter!=null) p_tfl=mTreeFilelistAdapter.getDataList();
		if (cdir.length()>0) createFileList(mMainFilePath,null,cdir.substring(1));
		else createFileList(mMainFilePath,null,"");
		ArrayList<TreeFilelistItem> n_tfl=null;
		if (mTreeFilelistAdapter!=null) n_tfl=mTreeFilelistAdapter.getDataList();
		if (p_tfl!=null && n_tfl!=null) {
			for(TreeFilelistItem n_tfli:n_tfl) {
				for(TreeFilelistItem p_tfli:p_tfl) {
					if (n_tfli.getName().equals(p_tfli.getName())) {
						n_tfli.setChecked(p_tfli.isChecked());
						break;
					}
				}
			}
			mTreeFilelistAdapter.setDataList(n_tfl);
		}
	};
	
	private ArrayList<ZipFileListItem> buildTarFileList(String fp) {
		String ft=CommonUtilities.getFileExtention(fp);
		ArrayList<ZipFileListItem>tfl=new ArrayList<ZipFileListItem>();
		try {
			TarArchiveInputStream tais=null;
			if (ft.equals(ZipFileListItem.ZIP_TYPE_GZ) || ft.equals(ZipFileListItem.ZIP_TYPE_TGZ)) {
				FileInputStream fis=new FileInputStream(fp);
		        GzipCompressorInputStream  gis=new GzipCompressorInputStream( 
		        		new BufferedInputStream(fis, 1024*1024*8));
		        tais=new TarArchiveInputStream(gis);
			} else {
				FileInputStream fis=new FileInputStream(fp);
				tais = new TarArchiveInputStream(new BufferedInputStream(fis, 1024*1024*8));
			}
	        ArchiveEntry entry=null;
	        while( ( entry = tais.getNextEntry() ) != null ) {
	        	String tfp=entry.getName();
				String t_path="", t_name="";//, w_t_name="";
				String w_path="";
//				Log.v("","tfp="+tfp);
				ZipFileListItem zfli=null;
				if (entry.isDirectory()) {
					w_path=tfp.endsWith("/")?tfp.substring(0,tfp.length()-1):tfp;
					t_name=w_path.lastIndexOf("/")>=0?w_path.substring(w_path.lastIndexOf("/")+1):w_path;
					t_path=tfp.replace(t_name+"/", "");
					if (t_path.endsWith("/")) t_path=t_path.substring(0,t_path.length()-1);
//					Log.v("","t_name="+t_name+", w_path="+w_path+", t_path="+t_path);
					
					zfli=new ZipFileListItem(t_name, t_path,
							entry.isDirectory(), false, entry.getSize(), 
							entry.getLastModifiedDate().getTime(), 0, 0,
							true);
					tfl.add(zfli);
//					zfli.dump();
					String[] d_array=t_path.split("/");
					String p_dir="", c_dir="", sep="";					
					for(String d_item:d_array) {
						c_dir+=sep+d_item;
						boolean found=false;
						for(ZipFileListItem z_item:tfl) {
							if (z_item.getFileName().equals(c_dir)) {
								found=true;
								break;
							}
						}
						if (!found) {
							zfli=new ZipFileListItem(c_dir, p_dir,
									true, false, 0, 
									0, 0, 0,
									true);
							tfl.add(zfli);
//							zfli.dump();
						}
						p_dir+=sep+d_item;
						sep="/";
					}
				} else {
					w_path=tfp;
					t_name=w_path.lastIndexOf("/")>=0?w_path.substring(w_path.lastIndexOf("/")+1):w_path; 
					t_path=w_path.replace(t_name, "");
					if (t_path.endsWith("/")) t_path=t_path.substring(0,t_path.length()-1);
					zfli=new ZipFileListItem(t_name, t_path,
							entry.isDirectory(), false, entry.getSize(), 
							entry.getLastModifiedDate().getTime(), -1, 0,
							true);
					tfl.add(zfli);
				}
//				zfli.dump();
	        }
	        tais.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tfl;
	};
	
	private void createFileList(String fp, final NotifyEvent p_ntfy, String base) {
		Log.v("","fp="+fp+", base="+base);
		if (!fp.equals("")) {
			
			mZipFileList=buildTarFileList(fp);
//			for(ZipFileListItem zfli:mZipFileList) zfli.dump();
//			Log.v("","size="+mZipFileList.size());
			if (mZipFileList!=null && mZipFileList.size()>0) {
				ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, base);
				mTreeFilelistAdapter=new CustomTreeFilelistAdapter(mActivity, false, true);
//				Log.v("","size="+tfl.size());
				mTreeFilelistAdapter.setDataList(tfl);
				mTreeFilelistView.setAdapter(mTreeFilelistAdapter);
				mTreeFilelistView.setSelection(0);
				mZipFileCurrentDirectory.setText("/"+base);
				mZipFileCurrentDirectory.setVisibility(TextView.VISIBLE);
				setZipTreeFileListener();
//				mGp.zipFileSpinner.setVisibility(TextView.VISIBLE);
				mTreeFilelistView.setVisibility(ListView.VISIBLE);
				mZipFileEmpty.setVisibility(TextView.GONE);
				mContextZipButton.setVisibility(ListView.VISIBLE);
				mZipFileUp.setVisibility(Button.VISIBLE);
				mZipFileTop.setVisibility(Button.VISIBLE);
				if (base.equals("")) {
					setTopUpButtonEnabled(false);
//					mZipFileUp.setEnabled(false);
//					mZipFileTop.setEnabled(false);
				} else {
					setTopUpButtonEnabled(true);
//					mZipFileUp.setEnabled(true);
//					mZipFileTop.setEnabled(true);
				}
				
		    	mContextZipButtonExtractView.setVisibility(LinearLayout.VISIBLE);
		    	mContextZipButtonAddView.setVisibility(LinearLayout.VISIBLE);
		        mContextZipButtonDeleteView.setVisibility(LinearLayout.VISIBLE);
		        mContextZipButtonSelectAllView.setVisibility(LinearLayout.VISIBLE);
		        mContextZipButtonUnselectAllView.setVisibility(LinearLayout.VISIBLE);
				
			} else {
				mTreeFilelistView.setVisibility(ListView.GONE);
				mZipFileEmpty.setVisibility(TextView.VISIBLE);
				mZipFileEmpty.setText(R.string.msgs_zip_zip_folder_empty);
//				mContextZipButton.setVisibility(ListView.GONE);
				mZipFileCurrentDirectory.setVisibility(TextView.GONE);
				mZipFileUp.setVisibility(Button.GONE);
				mZipFileTop.setVisibility(Button.GONE);
//				mGp.zipFileSpinner.setVisibility(TextView.VISIBLE);
				
		    	mContextZipButtonExtractView.setVisibility(LinearLayout.GONE);
		    	mContextZipButtonAddView.setVisibility(LinearLayout.VISIBLE);
		        mContextZipButtonDeleteView.setVisibility(LinearLayout.GONE);
		        mContextZipButtonSelectAllView.setVisibility(LinearLayout.GONE);
		        mContextZipButtonUnselectAllView.setVisibility(LinearLayout.GONE);
			}
			File lf=new File(fp);
			String tfs=MiscUtil.convertFileSize(lf.length());
			String info=String.format(mContext.getString(R.string.msgs_zip_zip_file_info), 
					tfs, mZipFileList.size(), mZipFileNameEncodingSelected);
			mZipFileInfo.setText(info);
			mZipFileInfo.setVisibility(TextView.VISIBLE);
			
			mEncodingSpinner.setVisibility(Spinner.VISIBLE);
			if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
		} else {
			mTreeFilelistView.setVisibility(ListView.GONE);
			mZipFileEmpty.setVisibility(TextView.VISIBLE);
			mZipFileEmpty.setText(R.string.msgs_zip_folder_not_specified);
//			mContextZipButton.setVisibility(ListView.GONE);
			mZipFileCurrentDirectory.setVisibility(TextView.GONE);
			mZipFileUp.setVisibility(Button.GONE);
			mZipFileUp.setEnabled(false);
			mZipFileTop.setVisibility(Button.GONE);
//			mGp.zipFileSpinner.setVisibility(TextView.GONE);
			
	    	mContextZipButtonExtractView.setVisibility(LinearLayout.GONE);
	    	mContextZipButtonAddView.setVisibility(LinearLayout.GONE);
	        mContextZipButtonDeleteView.setVisibility(LinearLayout.GONE);
	        mContextZipButtonSelectAllView.setVisibility(LinearLayout.GONE);
	        mContextZipButtonUnselectAllView.setVisibility(LinearLayout.GONE);
	        
	        mEncodingSpinner.setVisibility(Spinner.GONE);
	        mZipFileInfo.setVisibility(TextView.GONE);
			if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
//			p_ntfy.notifyToListener(false,new Object[]{ 
//					getString(R.string.msgs_text_browser_file_file_name_not_specified)});
		}
	};

	private ArrayList<TreeFilelistItem> createTreeFileList(ArrayList<ZipFileListItem>zip_file_list, String target_dir) {
//		Log.v("","target_dir="+target_dir);
		ArrayList<TreeFilelistItem> tfl=new ArrayList<TreeFilelistItem>();
		for (ZipFileListItem zfli:mZipFileList) {
//			Log.v("","p="+zfli.getParentDirectory()+", t="+target_dir);
			if (zfli.getParentDirectory().equals(target_dir)) {
				TreeFilelistItem tfli=createNewFileListItem(zfli);
				tfl.add(tfli);
				if (tfli.isDirectory()) {
					int sub_dc=0;
					String sub_dir=tfli.getPath()+"/"+tfli.getName();
					if (sub_dir.startsWith("/")) sub_dir=sub_dir.substring(1);
					
					for (ZipFileListItem s_zfli:mZipFileList) {
						if (s_zfli.getParentDirectory().equals(sub_dir)) sub_dc++;
					}
//					Log.v("","dir="+sub_dir+", cnt="+sub_dc);
					tfli.setSubDirItemCount(sub_dc);
				}
				tfli.dump("");
			}
		}
		return tfl;
	};
	
	@SuppressLint("DefaultLocale")
	private TreeFilelistItem createNewFileListItem(ZipFileListItem zfli) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.getDefault());
		String tfs=MiscUtil.convertFileSize(zfli.getFileLength());
		TreeFilelistItem tfi=null;
		if (zfli.isDirectory()) {
			tfi=new TreeFilelistItem(zfli.getFileName(),
					sdf.format(zfli.getLastModifiedTime())+", ", true, 0, zfli.getLastModifiedTime(),
					false, true, true,
					false, zfli.getParentDirectory(),0);
			tfi.setZipEncrypted(false);
			tfi.setZipFileName(zfli.getPath());
		} else {
			tfi=new TreeFilelistItem(zfli.getFileName(),
					sdf.format(zfli.getLastModifiedTime())+","+tfs, false, zfli.getFileLength(), zfli.getLastModifiedTime(),
					false, true, true,
					false, zfli.getParentDirectory(),0);
			tfi.setZipEncrypted(zfli.isEncrypted());
			tfi.setZipFileName(zfli.getPath());
//			Log.v("","ft="+ft+", mt="+mt);
		}
		return tfi;
	};
	
    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
    	if (enabled) {
        	btn.postDelayed(new Runnable(){
    			@Override
    			public void run() {
    				btn.setEnabled(true);
    			}
        	}, 1000);
    	} else {
    		btn.setEnabled(false);
    	}
    };
    
	private void setZipContextButtonListener() {
        mContextZipButtonExtract.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					extractDlg(mTreeFilelistAdapter);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextZipButtonExtract,mContext.getString(R.string.msgs_zip_cont_label_extract));

        mContextZipButtonAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					addItemDlg();
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextZipButtonAdd,mContext.getString(R.string.msgs_zip_cont_label_add));
        
        mContextZipButtonDelete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					confirmDelete(mTreeFilelistAdapter);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextZipButtonDelete,mContext.getString(R.string.msgs_zip_cont_label_delete));
        
        mContextZipButtonSelectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled(mContextZipButtonSelectAll,false);
					ArrayList<TreeFilelistItem> tfl=mTreeFilelistAdapter.getDataList();
					for(TreeFilelistItem tfli:tfl) {
						tfli.setChecked(true);
					}
					mTreeFilelistAdapter.notifyDataSetChanged();
					setContextButtonEnabled(mContextZipButtonSelectAll,true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextZipButtonSelectAll,mContext.getString(R.string.msgs_zip_cont_label_select_all));
        
        mContextZipButtonUnselectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled(mContextZipButtonUnselectAll,false);
					mTreeFilelistAdapter.setAllItemUnchecked();
					mTreeFilelistAdapter.notifyDataSetChanged();
					setContextButtonEnabled(mContextZipButtonUnselectAll,true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextZipButtonUnselectAll,mContext.getString(R.string.msgs_zip_cont_label_unselect_all));
	};

	private void confirmCancel(final ThreadCtrl tc, final Button cancel) {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				tc.setDisabled();
				cancel.setEnabled(false);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mCommonDlg.showCommonDialog(true, "W", 
				mContext.getString(R.string.msgs_main_confirm_cancel), "", ntfy); 
	};

	@SuppressLint("DefaultLocale")
	private boolean addItemToZipFile(ThreadCtrl tc, ZipFile zf, ZipParameters zp, File add_item, 
			String base_dir, String root_zip) {
		mUtil.addDebugMsg(1, "I","addItemToZipFile entered, item="+add_item.getPath());
		if (!tc.isEnabled()) return false;
		boolean result=false;
		if (add_item.exists()) {
			if (add_item.isDirectory()) {
				ZipParameters n_zp=null;
				try {
					n_zp=(ZipParameters) zp.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				n_zp.setDefaultFolderPath(base_dir);
				n_zp.setRootFolderInZip(root_zip);
				try {
					zf.setRunInThread(false);
					zf.addFile(add_item, n_zp);
				} catch (ZipException e) {
					e.printStackTrace();
					tc.setThreadMessage(e.getMessage());
					return false;
				}
				File[] fl=add_item.listFiles();
				for(File child_item:fl) {
					if (!addItemToZipFile(tc, zf, zp, child_item, base_dir, root_zip)) {
						return false;
					}
				}
				result=true;
			} else {
				if (!tc.isEnabled()) return false;
				try {
					ZipParameters n_zp=null;
					try {
						n_zp=(ZipParameters) zp.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					if (mGp.settingNoCompressFileType.length()>0) {
						String[] no_comp_array=mGp.settingNoCompressFileType.split(";");
						if (no_comp_array!=null && no_comp_array.length>0) {
							for(String item:no_comp_array) {
//								Log.v("","item="+item+", path="+add_item.getName().toLowerCase());
								if (add_item.getName().toLowerCase().endsWith("."+item)) {
									n_zp.setCompressionMethod(Zip4jConstants.COMP_STORE);
									break;
								}
							}
						}
					}
					n_zp.setDefaultFolderPath(base_dir);
					n_zp.setRootFolderInZip(root_zip);
					
					zf.setRunInThread(true);
					zf.addFile(add_item, n_zp);
					while(zf.getProgressMonitor().getState()==ProgressMonitor.STATE_BUSY) {
						if (!tc.isEnabled()) {
							zf.getProgressMonitor().cancelAllTasks();
							while(zf.getProgressMonitor().getState()==ProgressMonitor.STATE_BUSY) {
								SystemClock.sleep(100);
							}
//							Log.v("","state="+zf.getProgressMonitor().getState());
							break;
						} else {
							putProgressMessage(
									String.format(mContext.getString(R.string.msgs_zip_add_file_adding),
											add_item.getPath(),zf.getProgressMonitor().getPercentDone()));
							SystemClock.sleep(100);
						}
					}
					if (tc.isEnabled()) {
						mUtil.addLogMsg("I", 
								String.format(mContext.getString(R.string.msgs_zip_add_file_added),add_item.getPath()));
						putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_add_file_added),add_item.getPath()));
						result=true;
					}
				} catch (ZipException e) {
					mUtil.addLogMsg("I", e.getMessage());
					CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
					tc.setThreadMessage(e.getMessage());
					return false;
				}
			}
		} else {
			mUtil.addDebugMsg(1, "I","addItemToZipFile item does not exists, item="+add_item.getPath());
		}
		mUtil.addDebugMsg(1, "I","addItemToZipFile exit, item="+add_item.getPath()+", result="+result);
		return result;
	};
	
	private void addItemDlg() {
		final Handler hndl=new Handler();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final String[] add_item=(String[])o;
				NotifyEvent ntfy_zip_parm=new NotifyEvent(mContext);
				ntfy_zip_parm.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, final Object[] o) {
						setUiDisabled();
						showDialogProgress();
						final Handler hndl=new Handler();
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
								boolean copy_back_required=false, result_work_copy=true;
								String w_zf_path="";
								if (mMainFilePath.startsWith(mGp.externalRootDirectory)) {
									w_zf_path=mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+WORK_DIRECTORY+"/"+"tmp.zip";
									result_work_copy=copyFileSdcardToInternal(tc, w_zf_path, mMainFilePath);
									copy_back_required=true;
								} else {
									w_zf_path=mMainFilePath;
									copy_back_required=false;
								}
								
								final String zf_path=w_zf_path;
								String wdir="";
								if (mZipFileCurrentDirectory.getText().toString().equals("") || 
										mZipFileCurrentDirectory.getText().toString().equals("/")) wdir="";
								else wdir=mZipFileCurrentDirectory.getText().toString().substring(1);
								final String cdir=wdir;
								if (result_work_copy) {
									ZipFile zf=createZipFile(zf_path, mZipFileNameEncodingSelected);
									
									@SuppressWarnings("rawtypes")
									List fhl=null;
									try {
										fhl=zf.getFileHeaders();
									} catch (ZipException e) {
									}
									if (fhl==null || fhl.size()==0) {
										File lf=zf.getFile();
										lf.delete();
										try {
											zf=new ZipFile(lf);
											zf.setFileNameCharset(mZipFileNameEncodingSelected);
										} catch (ZipException e) {
											e.printStackTrace();
										}
									}

									ZipParameters zp=(ZipParameters)o[0];
									ArrayList<File>add_file_list=new ArrayList<File>();
//									Log.v("","cdir="+cdir);
									String base_dir=add_item[0].startsWith(mGp.internalRootDirectory)?mGp.internalRootDirectory:mGp.externalRootDirectory;
									for(String item:add_item) {
										File lf=new File(item);
										if (!addItemToZipFile(tc, zf, zp, lf, base_dir, cdir))	{
											if (!tc.isEnabled()) {
												mCommonDlg.showCommonDialog(false, "W", 
														String.format(mContext.getString(R.string.msgs_zip_add_file_cancelled),
																item), "", null);
											} else {
												mUtil.addLogMsg("I", 
														String.format(mContext.getString(R.string.msgs_zip_add_file_failed),item));
												mCommonDlg.showCommonDialog(false, "E", 
														String.format(mContext.getString(R.string.msgs_zip_add_file_failed),item), 
														tc.getThreadMessage(), null);
											}
											break;
										}
									}
									
									if (copy_back_required) {
										copyFileInternalToSdcard(tc, zf_path, mMainFilePath);
									}
								}

								hndl.postDelayed(new Runnable(){
									@Override
									public void run() {
//										createZipFileList(zf_path, null, cdir);
										createFileList(mMainFilePath, null, cdir);
//										if (!mMainFilePath.equals(zf_path)) new File(zf_path).delete();
										setUiEnabled();
									}
								},500);
							}
						};
						th.start();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				getZipParmDlg(mUtil, mActivity, mGp, mZipFileNameEncodingSelected, "", mMainFilePath, ntfy_zip_parm);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		FileSelectDialogFragment fsdf=FileSelectDialogFragment.newInstance(
//				false, true, false, false, false, false, false, true, 
				mGp.internalRootDirectory, "", "", mContext.getString(R.string.msgs_zip_add_select_add_item));
		fsdf.setOptionDebug(false);
		fsdf.setOptionEnableCreate(true);
		fsdf.setOptionFileOnly(false);
		fsdf.setOptionHideMountPoint(false);
		fsdf.setOptionIncludeRoot(false);
		fsdf.setOptionSingleSelect(false);
		fsdf.setOptionLimitMountPoint(true);
		fsdf.setOptionSelectedFilePathWithMountPoint(true);
		fsdf.showDialog(mFragmentManager, fsdf, ntfy);
	};
	
//	private void buildAddFileList(ArrayList<File>add_file_list, String fpath) {
//		File lf=new File(fpath);
//		if (!lf.isDirectory()) {
//			add_file_list.add(new File(fpath));
//			mUtil.addDebugMsg(1,"I","buildAddFileList added path="+lf.getPath());
//		} else {
//			File[] fl_list=lf.listFiles();
//			if (fl_list!=null && fl_list.length>0) {
//				for(File sel_item:fl_list) {
//					if (sel_item.isFile()) {
//						add_file_list.add(sel_item);
//						mUtil.addDebugMsg(1,"I","buildAddFileList added path="+sel_item.getPath());
//					} else {
//						buildAddFileList(add_file_list, sel_item.getPath());
//					}
//				}
//			}
//			add_file_list.add(lf);
//			mUtil.addDebugMsg(1,"I","buildAddFileList added path="+lf.getPath());
//		}
//	};
	
	static public void getZipParmDlg(CommonUtilities mUtil, Activity mActivity, final GlobalParameters mGp, 
			final String selected_encoding, final String pswd, final String fp, final NotifyEvent p_ntfy) {
		boolean w_zip_encrypted=false, w_zip_empty=false;
		int zip_comp_method=0, zip_enc_method=0;
		final ZipFile zf=createZipFile(fp, selected_encoding);
		FileHeader w_fh=null;
		try {
			@SuppressWarnings("unchecked")
			List<FileHeader>fhl=zf.getFileHeaders();
			if (fhl.size()==0) w_zip_empty=true;
			else {
				if (zf.isEncrypted()) {
					for(FileHeader fh:fhl) {
						if (fh.isEncrypted()) {
							w_fh=fh;
							zip_comp_method=fh.getCompressionMethod();
							w_zip_encrypted=fh.isEncrypted();
							zip_enc_method=fh.getEncryptionMethod();
							break;
						}
					}
				} else {
					FileHeader fh=fhl.get(0);
					zip_comp_method=fh.getCompressionMethod();
					w_zip_encrypted=fh.isEncrypted();
					zip_enc_method=fh.getEncryptionMethod();
				}
			}
		} catch (ZipException e) {
//			e.printStackTrace();
			w_zip_empty=true;
		}
		final FileHeader zip_fh=w_fh;
		final boolean zip_file_empty=w_zip_empty;
		final boolean zip_encrypted=w_zip_encrypted;
		mUtil.addDebugMsg(1, "I", "getZipParm enc="+zip_encrypted+", comp_method="+zip_comp_method+", enc_method="+zip_enc_method);
		
		final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.zip_parm_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.zip_parm_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

    	LinearLayout title_view=(LinearLayout)dialog.findViewById(R.id.zip_parm_dlg_title_view);
    	title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
    	TextView dlg_title=(TextView)dialog.findViewById(R.id.zip_parm_dlg_title);
    	dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
    	
    	final TextView dlg_msg=(TextView)dialog.findViewById(R.id.zip_parm_dlg_msg);
    	dlg_msg.setVisibility(TextView.VISIBLE);
    	final EditText dlg_pswd=(EditText)dialog.findViewById(R.id.zip_parm_dlg_enc_password);
    	final EditText dlg_conf=(EditText)dialog.findViewById(R.id.zip_parm_dlg_enc_confirm);
    	
    	final Spinner dlg_comp_level=(Spinner)dialog.findViewById(R.id.zip_parm_dlg_comp_level);
    	
    	final RadioGroup dlg_rg_enc=(RadioGroup)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rg);
    	final RadioButton dlg_rb_none=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_none);
    	final RadioButton dlg_rb_std=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_standard);
    	final RadioButton dlg_rb_aes128=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_aes128);
    	final RadioButton dlg_rb_aes256=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_aes256);
    	
    	final Button dlg_cancel=(Button)dialog.findViewById(R.id.zip_parm_dlg_cancel_btn);
    	final Button dlg_ok=(Button)dialog.findViewById(R.id.zip_parm_dlg_ok_btn);

    	CommonDialog.setDlgBoxSizeLimit(dialog, false);
    	
    	setZipCompLevelSpinner(mGp, mActivity, dlg_comp_level);

    	if (!zip_file_empty && !zip_encrypted) {
    		dlg_rb_std.setEnabled(false);
    		dlg_rb_aes128.setEnabled(false);
    		dlg_rb_aes256.setEnabled(false);
    	}
    	dlg_rg_enc.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId==dlg_rb_none.getId()) {
		    		dlg_pswd.setVisibility(EditText.GONE);
		    		dlg_conf.setVisibility(EditText.GONE);
				} else if(checkedId==dlg_rb_std.getId()) {
		    		dlg_pswd.setVisibility(EditText.VISIBLE);
		    		dlg_conf.setVisibility(EditText.VISIBLE);
				} else if(checkedId==dlg_rb_aes128.getId()) {
		    		dlg_pswd.setVisibility(EditText.VISIBLE);
		    		dlg_conf.setVisibility(EditText.VISIBLE);
				} else if(checkedId==dlg_rb_aes256.getId()) {
		    		dlg_pswd.setVisibility(EditText.VISIBLE);
		    		dlg_conf.setVisibility(EditText.VISIBLE);
				}
				checkZipParmValidation(mGp, dialog, fp, zip_encrypted, zf, zip_fh);
			}
    	});
    	
		dlg_rb_none.setEnabled(true);
		dlg_rb_none.setChecked(true);
		dlg_pswd.setVisibility(EditText.GONE);
		dlg_conf.setVisibility(EditText.GONE);
    	
    	dlg_pswd.setText(pswd);
    	
    	checkZipParmValidation(mGp, dialog, fp, zip_encrypted, zf, zip_fh);
    	dlg_pswd.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				checkZipParmValidation(mGp, dialog, fp, zip_encrypted,  zf, zip_fh);
			}
    	});
    	dlg_conf.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				checkZipParmValidation(mGp, dialog, fp, zip_encrypted, zf, zip_fh);
			}
    	});
    	
		dlg_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				ZipParameters zp=new ZipParameters();
				String comp_level=dlg_comp_level.getSelectedItem().toString();
				int deflate_level=-1;
				if (comp_level.equals(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_fastest))) {
					zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
				} else if (comp_level.equals(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_fast))) {
					zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FAST);
				} else if (comp_level.equals(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_normal))) {
					zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
				} else if (comp_level.equals(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_maximum))) {
					zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_MAXIMUM);
				} else if (comp_level.equals(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_ultra))) {
					zp.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
				}
				String pswd=dlg_pswd.getText().toString();
				if (dlg_rb_none.isChecked()) {
					zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
				} else if (dlg_rb_std.isChecked()) {
					zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
					zp.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
					zp.setEncryptFiles(true);
					zp.setPassword(pswd);
				} else if (dlg_rb_aes128.isChecked()) {
					zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
					zp.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
					zp.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_128);
					zp.setEncryptFiles(true);
					zp.setPassword(pswd);
				} else if (dlg_rb_aes256.isChecked()) {
					zp.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
					zp.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
					zp.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
					zp.setEncryptFiles(true);
					zp.setPassword(pswd);
				}
				p_ntfy.notifyToListener(true, new Object[]{zp});
			}
		});
		
		dlg_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				p_ntfy.notifyToListener(false, null);
			}
		});
    	dialog.show();
	};

	private static void checkZipParmValidation(GlobalParameters mGp, Dialog dialog, String fp, boolean encrypted, ZipFile zf, FileHeader fh) {
    	final EditText dlg_pswd=(EditText)dialog.findViewById(R.id.zip_parm_dlg_enc_password);
    	final EditText dlg_conf=(EditText)dialog.findViewById(R.id.zip_parm_dlg_enc_confirm);
    	final TextView dlg_msg=(TextView)dialog.findViewById(R.id.zip_parm_dlg_msg);
    	
    	final Spinner dlg_comp_level=(Spinner)dialog.findViewById(R.id.zip_parm_dlg_comp_level);
    	
    	final RadioButton dlg_rb_none=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_none);
    	final RadioButton dlg_rb_std=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_standard);
    	final RadioButton dlg_rb_aes128=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_aes128);
    	final RadioButton dlg_rb_aes256=(RadioButton)dialog.findViewById(R.id.zip_parm_dlg_enc_type_rb_aes256);
    	final Button dlg_ok=(Button)dialog.findViewById(R.id.zip_parm_dlg_ok_btn);
    	
    	if (dlg_rb_none.isChecked()) {
			dlg_msg.setText("");
			dlg_ok.setEnabled(true);;
    	} else {
			if (dlg_pswd.getText().length()>0) {
				if (dlg_pswd.getText().toString().equals(dlg_conf.getText().toString())) {
					dlg_msg.setText("");
					dlg_ok.setEnabled(true);;
				} else {
					dlg_msg.setText(mGp.appContext.getString(R.string.msgs_zip_parm_confirm_pswd_unmatched));
					dlg_ok.setEnabled(false);;
				}
				dlg_conf.setEnabled(true);
			} else {
				dlg_ok.setEnabled(false);;
				dlg_msg.setText(mGp.appContext.getString(R.string.msgs_zip_parm_pswd_not_specified));
				dlg_conf.setEnabled(false);
			}
    	}
	};
	
	private static void setZipCompLevelSpinner(GlobalParameters mGp, Activity mActivity, Spinner spinner) {
		CommonUtilities.setSpinnerBackground(mActivity, spinner, mGp.themeIsLight);
		final CustomSpinnerAdapter adapter=
				new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
		adapter.add(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_fastest));
		adapter.add(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_fast));
		adapter.add(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_normal));
		adapter.add(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_maximum));
		adapter.add(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_ultra));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setPrompt(mGp.appContext.getString(R.string.msgs_zip_parm_zip_comp_level_promopt));
		spinner.setAdapter(adapter);
		
		spinner.setSelection(2);
	};
	
	private void extractDlg(final CustomTreeFilelistAdapter tfa) {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final String dest_path=((String)o[0]).endsWith("/")?((String)o[0]).substring(0,((String)o[0]).length()-1):((String)o[0]);
				final ArrayList<TreeFilelistItem> sel_file_list=buildSelectedFileist(tfa);
				NotifyEvent ntfy_confirm=new NotifyEvent(mContext);
				ntfy_confirm.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						try {
							ZipFile zf=createZipFile(mMainFilePath, mZipFileNameEncodingSelected);
							NotifyEvent ntfy_extract=new NotifyEvent(mContext);
							ntfy_extract.setListener(new NotifyEventListener(){
								@Override
								public void positiveResponse(Context c, Object[] o) {
									extractMultipleItem(sel_file_list, dest_path);
								}
								@Override
								public void negativeResponse(Context c, Object[] o) {
									mCommonDlg.showCommonDialog(false, "W", 
											mContext.getString(R.string.msgs_zip_extract_file_was_cancelled), "", null);
								}
							});
							if (zf.isEncrypted()) {
								mMainPassword="";
								obtainSelectedItemPassword(sel_file_list, zf, ntfy_extract);
							} else {
								ntfy_extract.notifyToListener(true, o);
							}
						} catch (ZipException e) {
							e.printStackTrace();
						}
						
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				String conf_list="";
				String sep="";
				for (TreeFilelistItem item:sel_file_list) {
					conf_list+=sep+item.getZipFileName();
					sep="\n";
				}
				mCommonDlg.showCommonDialog(true, "W", 
						String.format(mContext.getString(R.string.msgs_zip_extract_file_confirm_extract),dest_path), 
						conf_list, ntfy_confirm);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		FileSelectDialogFragment fsdf=FileSelectDialogFragment.newInstance(
//				false, true, false, false, true, false, true, true, 
				mGp.internalRootDirectory, "", "", mContext.getString(R.string.msgs_zip_extract_select_dest_directory));
		fsdf.setOptionDebug(false);
		fsdf.setOptionEnableCreate(true);
		fsdf.setOptionFileOnly(false);
		fsdf.setOptionHideMountPoint(false);
		fsdf.setOptionIncludeRoot(false);
		fsdf.setOptionSingleSelect(true);
		fsdf.setOptionLimitMountPoint(true);
		fsdf.showDialog(mFragmentManager, fsdf, ntfy);
	};
	
	private void obtainSelectedItemPassword(final ArrayList<TreeFilelistItem> sel_file_list, final ZipFile zf, final NotifyEvent p_ntfy) {
		TreeFilelistItem w_tfli=null;
		for(TreeFilelistItem item:sel_file_list) {
			if (!item.isChecked()){
				w_tfli=item;
				item.setChecked(true);
				break;
			}
		};
		if (w_tfli!=null) {
			final TreeFilelistItem sel_item=w_tfli;
//			Log.v("","size="+sel_file_list.size()+", name="+sel_item.getName()+", enc="+sel_item.isZipEncrypted()+", pswd="+sel_item.getZipPassword()+
//					", main pswd="+mMainPassword);
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
//					Log.v("","positive="+o[0]);
//					Log.v("","pos size="+sel_file_list.size());
					if (o!=null) {
						String pswd=(String)o[0];
						for (TreeFilelistItem s_tfli:sel_file_list) {
							if (s_tfli.getZipFileName().equals(sel_item.getZipFileName())) {
								s_tfli.setZipPassword(pswd);
//								Log.v("","o="+pswd+", p="+mMainPassword);
								break;
							}
						}
					}
					obtainSelectedItemPassword(sel_file_list, zf, p_ntfy);
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {
					p_ntfy.notifyToListener(false, null);
				}
			});
			
			if (sel_item.isZipEncrypted()) {
				if (sel_item.getZipPassword().isEmpty()) {
					try {
						FileHeader fh=zf.getFileHeader(sel_item.getZipFileName());
						if (mMainPassword.isEmpty()) {
							getZipPasswordDlg(zf, fh, ntfy);
						} else {
							if (isCorrectZipFilePassword(zf, fh, mMainPassword)) {
								ntfy.notifyToListener(true, new Object[]{mMainPassword});
							} else {
								getZipPasswordDlg(zf, fh, ntfy);
							}
						}
					} catch (ZipException e) {
						mUtil.addLogMsg("I", e.getMessage());
						CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
					}
				} else {
					ntfy.notifyToListener(true, null);
				}
			} else {
				ntfy.notifyToListener(true, null);
			}
		} else {
			p_ntfy.notifyToListener(true, null);
		}
	};
	
	private ArrayList<TreeFilelistItem> buildSelectedFileist(CustomTreeFilelistAdapter tfa) {
		ArrayList<TreeFilelistItem> sel_file_list=new ArrayList<TreeFilelistItem>();
		if (tfa.isDataItemIsSelected()) {
			for (TreeFilelistItem tfli:tfa.getDataList()) {
//				Log.v("","name="+tfli.getName()+", checked="+tfli.isChecked());
				if (tfli.isChecked()) {
					if (tfli.isDirectory()) {
						for(ZipFileListItem zfli:mZipFileList) {
//							Log.v("","tree="+tfli.getZipFileName()+", zip="+zfli.getPath());
							if (zfli.getPath().startsWith(tfli.getZipFileName())) {
								sel_file_list.add(createNewFileListItem(zfli));
							}
						}
					} else {
						for(ZipFileListItem zfli:mZipFileList) {
//							Log.v("","tf="+tfli.getZipFileName()+", zf="+zfli.getPath()+", zfdir="+zfli.isDirectory());
							if (!zfli.isDirectory() && tfli.getZipFileName().equals(zfli.getPath())) {
								sel_file_list.add(createNewFileListItem(zfli));
								Log.v("","aaded name="+zfli.getPath());
							}
						}
					}
				}
			}
		} else {
			for(ZipFileListItem zfli:mZipFileList) {
				sel_file_list.add(createNewFileListItem(zfli));
			}
		}
		return sel_file_list;
	};
	
	private void extractMultipleItem(final ArrayList<TreeFilelistItem>sel_file_list, final String dest_path) {
		mConfirmResponse=0;
		String conf_list="";
		String sep="";
		for (TreeFilelistItem item:sel_file_list) {
			conf_list+=sep+item.getZipFileName();
			sep="\n";
		}

		setUiDisabled();
		showDialogProgress();
		final Handler hndl=new Handler();
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
				try {
					mUtil.addDebugMsg(1, "I", "Extract started");
					ZipFile zf=createZipFile(mMainFilePath, mZipFileNameEncodingSelected);
					for(TreeFilelistItem extract_tfli:sel_file_list) {
						FileHeader extract_fh=zf.getFileHeader(extract_tfli.getZipFileName());
//						Log.v("","fn="+extract_tfli.getZipFileName());
						if (!extract_tfli.isDirectory()) {
							if (!tc.isEnabled()) break;
							String dir="", fn=extract_fh.getFileName();
							if (extract_fh.getFileName().lastIndexOf("/")>0) {
								dir=extract_fh.getFileName().substring(0,extract_fh.getFileName().lastIndexOf("/"));
								fn=extract_fh.getFileName().substring(extract_fh.getFileName().lastIndexOf("/")+1);
							}
							if (!extract_tfli.getZipPassword().isEmpty()) zf.setPassword(extract_tfli.getZipPassword());
//							Log.v("","dir="+dir+", fn="+fn+", dest="+dest_path);
							if (confirmReplace(tc, dest_path+"/"+dir, fn)) {
								if (extractSpecificFile(tc, zf, extract_fh.getFileName(), dest_path+"/"+dir, fn)) {
									putProgressMessage(
											String.format(mContext.getString(R.string.msgs_zip_extract_file_was_extracted), extract_fh.getFileName()));
									mUtil.addLogMsg("I", 
											String.format(mContext.getString(R.string.msgs_zip_extract_file_was_extracted), extract_fh.getFileName()));
								} else {
									if (tc.isEnabled()) {
										mCommonDlg.showCommonDialog(false, "E", 
												mContext.getString(R.string.msgs_zip_extract_file_was_failed), 
												tc.getThreadMessage(), null);
									} else {
										mCommonDlg.showCommonDialog(false, "W", 
												mContext.getString(R.string.msgs_zip_extract_file_was_cancelled), "", null);
									}
									break;
								}
							} else {
								//Reject replace request
								if (tc.isEnabled()) {
									putProgressMessage(
											mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced)+dest_path+"/"+dir+"/"+fn);
									mUtil.addLogMsg("I", 
											mContext.getString(R.string.msgs_zip_extract_file_was_not_replaced)+dest_path+"/"+dir+"/"+fn);
								} else {
									mCommonDlg.showCommonDialog(false, "W", 
											mContext.getString(R.string.msgs_zip_extract_file_was_cancelled), "", null);
									break;
								}
							}
						} else {
//							extract_fh.getFileName(), dest_path+"/"+dir, fn
							if (dest_path.startsWith(mGp.externalRootDirectory)) {
								SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(),
										dest_path+"/"+extract_tfli.getZipFileName(), true);
								sf.exists();
							} else {
								File lf=new File(dest_path+"/"+extract_tfli.getZipFileName());
								lf.mkdirs();
							}
						}
					}
					
					mUtil.addDebugMsg(1, "I", "Extract ended");
					
					hndl.postDelayed(new Runnable(){
						@Override
						public void run() {
							createFileList(mMainFilePath,null, "");
							setUiEnabled();
						}
					},10);
				} catch (ZipException e) {
					mUtil.addLogMsg("I", e.getMessage());
					CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
				}
			}
		};
		th.start();
	};
	
	static final private int CONFIRM_RESPONSE_CANCEL=-99; 
	static final private int CONFIRM_RESPONSE_YES=1;
	static final private int CONFIRM_RESPONSE_YESALL=2;
	static final private int CONFIRM_RESPONSE_NO=-1;
	static final private int CONFIRM_RESPONSE_NOALL=-2;
	private int mConfirmResponse=0;
	private boolean confirmReplace(final ThreadCtrl tc, final String dest_dir, final String dest_name) {
		final String w_path=dest_dir.endsWith("/")?dest_dir+dest_name:dest_dir+"/"+dest_name;

		File lf=new File(w_path);
//		Log.v("","name="+lf.getPath()+", exists="+lf.exists());
		if (lf.exists()) {
			if (mConfirmResponse!=CONFIRM_RESPONSE_YESALL && mConfirmResponse!=CONFIRM_RESPONSE_NOALL) {
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
						mDialogConfirmView.setVisibility(LinearLayout.VISIBLE);
						mDialogConfirmCancel.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
								mDialogConfirmView.setVisibility(LinearLayout.GONE);
								mConfirmResponse=CONFIRM_RESPONSE_CANCEL;
								tc.setDisabled();
								synchronized(tc) {tc.notify();}
							}
						});
						
						mDialogConfirmYes.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
								mDialogConfirmView.setVisibility(LinearLayout.GONE);
								mConfirmResponse=CONFIRM_RESPONSE_YES;
								synchronized(tc) {tc.notify();}
							}
						});
						mDialogConfirmYesAll.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
								mDialogConfirmView.setVisibility(LinearLayout.GONE);
								mConfirmResponse=CONFIRM_RESPONSE_YESALL;
								synchronized(tc) {tc.notify();}
							}
						});
						mDialogConfirmNo.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
								mDialogConfirmView.setVisibility(LinearLayout.GONE);
								mConfirmResponse=CONFIRM_RESPONSE_NO;
								synchronized(tc) {tc.notify();}
							}
						});
						mDialogConfirmNoAll.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View v) {
								mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
								mDialogConfirmView.setVisibility(LinearLayout.GONE);
								mConfirmResponse=CONFIRM_RESPONSE_NOALL; 
								synchronized(tc) {tc.notify();}
							}
						});
						mDialogConfirmMsg.setText(
								String.format(mContext.getString(R.string.msgs_zip_extract_file_confirm_replace_copy), w_path));
					}
				});
				
				synchronized(tc) {
					try {
						tc.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				boolean result=false;
				if (mConfirmResponse==CONFIRM_RESPONSE_CANCEL) {
				} else if (mConfirmResponse==CONFIRM_RESPONSE_YES) {
					result=true;
				} else if (mConfirmResponse==CONFIRM_RESPONSE_YESALL) {
					result=true;
				} else if (mConfirmResponse==CONFIRM_RESPONSE_NO) {
				} else if (mConfirmResponse==CONFIRM_RESPONSE_NOALL) {
				}
				return result;
			} else {
				boolean result=false;
				if (mConfirmResponse==CONFIRM_RESPONSE_YESALL) {
					result=true;
				}
				return result;
			}
		} else {
			return true;
		}
	};
	
	private void getZipPasswordDlg(final ZipFile zf, final FileHeader fh, final NotifyEvent p_ntfy) {
		final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.password_prompt_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.password_prompt_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

    	LinearLayout title_view=(LinearLayout)dialog.findViewById(R.id.password_prompt_dlg_title_view);
    	title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
    	TextView dlg_title=(TextView)dialog.findViewById(R.id.password_prompt_dlg_title);
    	dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
    	dlg_title.setText(R.string.msgs_zip_extract_zip_password_title);
    	final TextView dlg_msg=(TextView)dialog.findViewById(R.id.password_prompt_dlg_msg);
    	dlg_msg.setVisibility(TextView.VISIBLE);
    	final TextView dlg_filename=(TextView)dialog.findViewById(R.id.password_prompt_dlg_itemtext);
    	if (fh!=null) {
    		dlg_filename.setText(fh.getFileName());
    	} else {
    		dlg_filename.setText("");
    	}
    	
    	final EditText dlg_pswd=(EditText)dialog.findViewById(R.id.password_prompt_dlg_itemname);
    	
    	final Button dlg_cancel=(Button)dialog.findViewById(R.id.password_prompt_dlg_cancel_btn);
    	final Button dlg_ok=(Button)dialog.findViewById(R.id.password_prompt_dlg_ok_btn);
    	dlg_ok.setVisibility(Button.VISIBLE);
    	
    	dlg_pswd.setVisibility(EditText.VISIBLE);
    	
    	CommonDialog.setDlgBoxSizeLimit(dialog, true);
    	
    	dlg_pswd.setText(mMainPassword);
    	verifyZipPassword(zf, fh, mMainPassword, dlg_ok, dlg_msg);
    	dlg_pswd.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				verifyZipPassword(zf, fh, s.toString(), dlg_ok, dlg_msg);
			}
    	});
    	
		dlg_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				mMainPassword=dlg_pswd.getText().toString();
				p_ntfy.notifyToListener(true, new Object[]{mMainPassword});
			}
		});
		
		dlg_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				p_ntfy.notifyToListener(false, null);
			}
		});
    	dialog.show();
	};

	private static void verifyZipPassword(final ZipFile zf, final FileHeader fh, String pswd, Button dlg_ok, TextView dlg_msg) {
		if (pswd.length()>0) {
			if (isCorrectZipFilePassword(zf, fh, pswd)) {
				dlg_ok.setEnabled(true);
				dlg_msg.setText("");
			} else {
				dlg_ok.setEnabled(false);
				dlg_msg.setText(R.string.msgs_zip_extract_zip_password_wrong);
			}
		} else {
			dlg_ok.setEnabled(false);
			dlg_msg.setText(R.string.msgs_zip_extract_zip_password_not_specified);
		}
	}
	
	private static ZipFile createZipFile(String fp, String select_encoding) {
		ZipFile zf=null;
		File lf=new File(fp);
		boolean file_exists=lf.exists();
		try {
			zf = new ZipFile(fp);
			zf.setFileNameCharset(select_encoding);
		} catch (ZipException e) {
			e.printStackTrace();
		}
		return zf;
	};

	private static boolean isCorrectZipFilePassword(final ZipFile zf, final FileHeader fh, String pswd) {
		if (zf==null || fh==null) return true;
		boolean result=false;
		try {
			if (fh.isEncrypted()) {
				zf.setPassword(pswd);
				InputStream is=zf.getInputStream(fh);
				byte[] buff=new byte[512];
				int rc=is.read(buff);
				result=true;
			}
		} catch (ZipException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return result;
	};

	private void confirmDelete(final CustomTreeFilelistAdapter tfa) {
		String conf_list="";
		String sep="";
		final ArrayList<TreeFilelistItem> sel_file_list=buildSelectedFileist(tfa);
		for (TreeFilelistItem tfli:sel_file_list) {
//			Log.v("","sel name="+tfli.getName());
			conf_list+=sep+tfli.getZipFileName();
			sep="\n";
		}
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setUiDisabled();
				showDialogProgress();
				final Handler hndl=new Handler();
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
						boolean copy_back_required=false, result_work_copy=true;
						String w_zf_path="";
						if (mMainFilePath.startsWith(mGp.externalRootDirectory)) {
							w_zf_path=mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+WORK_DIRECTORY+"/"+"tmp.zip";
							result_work_copy=copyFileSdcardToInternal(tc, w_zf_path, mMainFilePath);
							copy_back_required=true;
						} else {
							w_zf_path=mMainFilePath;
							copy_back_required=false;
						}
						final String zf_path=w_zf_path;
						if (result_work_copy) {
							ZipFile zf=createZipFile(zf_path, mZipFileNameEncodingSelected);
//							ArrayList<ZipFileListItem> zfl=ZipUtil.buildZipFileList(zf_path, mZipFileNameEncodingSelected);
//							for(ZipFileListItem zfli:zfl) zfli.dump();
							for(final TreeFilelistItem del_item:sel_file_list) {
								if (!tc.isEnabled()) {
									mCommonDlg.showCommonDialog(false, "W", 
											String.format(mContext.getString(R.string.msgs_zip_delete_file_was_cancelled),
													del_item.getZipFileName()), "", null);
									break;
								}
								putProgressMessage(
										String.format(mContext.getString(R.string.msgs_zip_delete_file_is_deleting), del_item.getZipFileName()));
								try {
									if (del_item.isDirectory()) {
										try {
											zf.removeFile(del_item.getZipFileName()+"/");
										} catch(ZipException e) {
										}
									}
									else zf.removeFile(del_item.getZipFileName());
								} catch (ZipException e) {
									e.printStackTrace();
									tc.setThreadMessage(e.getMessage());
									mCommonDlg.showCommonDialog(false, "W", 
											String.format(mContext.getString(R.string.msgs_zip_delete_file_was_failed),
													del_item.getZipFileName()), e.getMessage(), null);
									copy_back_required=false;
									break;
								}
								putProgressMessage(
										String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), del_item.getZipFileName()));
								mUtil.addLogMsg("I", 
										String.format(mContext.getString(R.string.msgs_zip_delete_file_was_deleted), del_item.getZipFileName()));
							}
							
							if (copy_back_required) {
								copyFileInternalToSdcard(tc, zf_path, mMainFilePath);
							}
						}
						
						mUtil.addDebugMsg(1, "I", "Delete ended");
						
						hndl.postDelayed(new Runnable(){
							@Override
							public void run() {
								final String cdir=mZipFileCurrentDirectory.getText().toString();
//								createZipFileList(zf_path,null,cdir.substring(1));
								createFileList(mMainFilePath,null,cdir.substring(1));
//								if (!mMainFilePath.equals(zf_path)) new File(zf_path).delete();
								setUiEnabled();
							}
						},100);
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_zip_delete_confirm_delete),
				conf_list, ntfy);
	};

	private boolean copyFileInternalToSdcard(ThreadCtrl tc, String app_path, String sdcard_path) {
		long begin=System.currentTimeMillis();
		mUtil.addDebugMsg(1, "I", "copyFileInternalToSdcard copy stated");
		String msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_start);
		mUtil.addLogMsg("I", msg);
		putProgressMessage(msg);

		boolean result=false;
		SafFile sf=mGp.safMgr.getSdcardSafFile();
		SafFile out_sf=mGp.safMgr.getSafFileBySdcardPath(sf, sdcard_path, false);
		SafFile temp_sf=mGp.safMgr.getSafFileBySdcardPath(sf, sdcard_path+".tmp", false);
		File from=new File(app_path);
		try {
			OutputStream fos=mContext.getContentResolver().openOutputStream(temp_sf.getUri());
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*1024*8);
			FileInputStream fis=new FileInputStream(from);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*8);
			byte[] buff=new byte[1024*1024*1];
			int rc=bis.read(buff);
			while(rc>0) {
				if (!tc.isEnabled()) break;
				bos.write(buff, 0, rc);
				rc=bis.read(buff);
			}
			bis.close();
			bos.flush();
			bos.close();
			
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
				mUtil.addLogMsg("W", msg+"\n"+tc.getThreadMessage());
			}
		} else {
			msg=mContext.getString(R.string.msgs_zip_sdcard_create_zip_file_cancelled);
			mUtil.addLogMsg("W", msg);
		}
		putProgressMessage(msg);

		mUtil.addDebugMsg(1, "I", "copyFileInternalToSdcard copy ended, elapsed time="+(System.currentTimeMillis()-begin));
		return result;
	};

	private boolean copyFileSdcardToInternal(ThreadCtrl tc, String app_path, String sdcard_path) {
		long begin=System.currentTimeMillis();
		mUtil.addDebugMsg(1, "I", "copyFileSdcardToInternal copy started");
		String msg=mContext.getString(R.string.msgs_zip_sdcard_create_work_file_start);
		mUtil.addLogMsg("I", msg);
		putProgressMessage(msg);
		
		boolean result=false;
		File from=new File(sdcard_path);
		File to=new File(app_path);
		new File(to.getParent()).mkdirs();
		to.delete();
		try {
			FileOutputStream fos=new FileOutputStream(to);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*1024*8);
			FileInputStream fis=new FileInputStream(from);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*8);
			byte[] buff=new byte[1024*1024];
			int rc=bis.read(buff);
			while(rc>0) {
				if (!tc.isEnabled()) break;
				bos.write(buff, 0, rc);
				rc=bis.read(buff);
			}
			bis.close();
			bos.flush();
			bos.close();
			if (!tc.isEnabled()) to.delete();
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
				msg=mContext.getString(R.string.msgs_zip_sdcard_create_work_file_end);
				mUtil.addLogMsg("I", msg);
			} else {
				msg=mContext.getString(R.string.msgs_zip_sdcard_create_work_file_failed);
				mUtil.addLogMsg("E", msg+"\n"+tc.getThreadMessage());
			}
		} else {
			msg=mContext.getString(R.string.msgs_zip_sdcard_create_work_file_cancelled);
			mUtil.addLogMsg("W", msg);
		}
		putProgressMessage(msg);

		mUtil.addDebugMsg(1, "I", "copyFileSdcardToInternal copy ended, elapsed time="+(System.currentTimeMillis()-begin));
		return result;
	};

	private void putProgressMessage(final String msg) {
		mUiHandler.post(new Runnable(){
			@Override
			public void run() {
				mDialogProgressSpinMsg2.setText(msg);
			}
		});
	}
	
	final private void refreshOptionMenu() {
		mActivity.invalidateOptionsMenu();
	};

	private void setUiEnabled() {
		mActivity.setUiEnabled();
		hideDialog();
		refreshOptionMenu();
	};
	
	private void setUiDisabled() {
		mActivity.setUiDisabled();
		refreshOptionMenu();
	};
	
	private boolean isUiEnabled() {
		return mActivity.isUiEnabled();
	};

	private void showDialogProgress() {
		mDialogProgressSpinView.setVisibility(LinearLayout.VISIBLE);
	};
	
	private void hideDialog() {
		mDialogProgressSpinView.setVisibility(LinearLayout.GONE);
		mDialogConfirmView.setVisibility(LinearLayout.GONE);
	};
	
	private void openSppecificDirectory(String dir_name) {
		ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir_name);
		mTreeFilelistAdapter.setDataList(tfl);
		mZipFileCurrentDirectory.setText("/"+dir_name);
		if (tfl.size()>0) mTreeFilelistView.setSelection(0);
		setTopUpButtonEnabled(true);
	};

	private String mZipFileNameEncodingDesired="";
	private String mZipFileNameEncodingSelected=ENCODING_NAME_UTF8;
	private void setZipTreeFileListener() {
		mEncodingSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String sel_encoding=mEncodingSpinner.getSelectedItem().toString();
				if (!mZipFileNameEncodingDesired.equals(sel_encoding)) {
					mZipFileNameEncodingDesired=sel_encoding;
					refreshFileList();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
        ntfy_expand_close.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (!isUiEnabled()) return;
				int idx=(Integer)o[0];
	    		final int pos=mTreeFilelistAdapter.getItem(idx);
	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				if (tfi.isDirectory()){// && tfi.getSubDirItemCount()>0) {
					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
					ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir);
					mTreeFilelistAdapter.setDataList(tfl);
					mZipFileCurrentDirectory.setText("/"+dir);
					setTopUpButtonEnabled(true);
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
        });
        mTreeFilelistAdapter.setExpandCloseListener(ntfy_expand_close);
        
		mContextZipButtonDelete.setVisibility(ImageButton.INVISIBLE);
		NotifyEvent ntfy_cb=new NotifyEvent(mContext);
		ntfy_cb.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (!isUiEnabled()) return;
				mContextZipButtonDelete.setVisibility(ImageButton.VISIBLE);
				mContextZipButtonAdd.setVisibility(ImageButton.INVISIBLE);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				if (!isUiEnabled()) return;
				if (mTreeFilelistAdapter.isDataItemIsSelected()) {
					mContextZipButtonDelete.setVisibility(ImageButton.VISIBLE);
					mContextZipButtonAdd.setVisibility(ImageButton.INVISIBLE);
				} else {
					mContextZipButtonDelete.setVisibility(ImageButton.INVISIBLE);
					mContextZipButtonAdd.setVisibility(ImageButton.VISIBLE);
				}
			}
		});
		mTreeFilelistAdapter.setCbCheckListener(ntfy_cb);
        mTreeFilelistView.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		if (!isUiEnabled()) return;
	    		final int pos=mTreeFilelistAdapter.getItem(idx);
	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				if (!mTreeFilelistAdapter.isDataItemIsSelected() && tfi.isDirectory()) {
					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
					ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir);
					mTreeFilelistAdapter.setDataList(tfl);
					mZipFileCurrentDirectory.setText("/"+dir);
					if (tfl.size()>0) mTreeFilelistView.setSelection(0);
					setTopUpButtonEnabled(true);
				} else {
					if (mTreeFilelistAdapter.isDataItemIsSelected()) {
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
	    		final int pos=mTreeFilelistAdapter.getItem(position);
	    		final TreeFilelistItem tfi=mTreeFilelistAdapter.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return true;
				processContextMenu(tfi);
				return true;
			}
        });

        mZipFileTop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!isUiEnabled()) return;
				String dir="";
				ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir);
				mTreeFilelistAdapter.setDataList(tfl);
				mZipFileCurrentDirectory.setText("/");
				setTopUpButtonEnabled(false);
				mTreeFilelistView.setSelectionFromTop(0, 0);
			}
        });

        mZipFileUp.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!isUiEnabled()) return;
				String dir=mZipFileCurrentDirectory.getText().toString().substring(1);
				if (!dir.equals("")) {
					if (dir.lastIndexOf("/")>0) {
//						Log.v("","dir="+dir+", idx="+dir.lastIndexOf("/"));
						String n_dir=dir.substring(0,dir.lastIndexOf("/"));
						ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, n_dir);
						mTreeFilelistAdapter.setDataList(tfl);
						mZipFileCurrentDirectory.setText("/"+n_dir);
					} else {
//						Log.v("","s1");
						ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, "");
						mTreeFilelistAdapter.setDataList(tfl);
						mZipFileCurrentDirectory.setText("/");
						setTopUpButtonEnabled(false);
					}
					mTreeFilelistView.setSelectionFromTop(0, 0);
				}
			}
        });
	};
	
	public boolean isUpButtonEnabled() {
		return mZipFileUp.isEnabled(); 
	};
	public void performClickUpButton() {
		mZipFileUp.performClick(); 
	};

	private void setTopUpButtonEnabled(boolean p) {
		mZipFileUp.setEnabled(p);
		mZipFileTop.setEnabled(p);
		if (p) {
			mZipFileUp.setAlpha(1);
			mZipFileTop.setAlpha(1);
		} else {
			mZipFileUp.setAlpha(0.4f);
			mZipFileTop.setAlpha(0.4f);
		}
	};
	
	private void processContextMenu(final TreeFilelistItem tfi) {
        final CustomContextMenu mCcMenu = new CustomContextMenu(mContext.getResources(), mFragmentManager);
		String sel_list="",sep="";
		final CustomTreeFilelistAdapter tfa=new CustomTreeFilelistAdapter(mActivity, false, false, false);
		final ArrayList<TreeFilelistItem> n_tfl=new ArrayList<TreeFilelistItem>();
		int sel_count=0;
		if (mTreeFilelistAdapter.isDataItemIsSelected()) {
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

		if (sel_count==1) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_property)+"("+(n_tfl.get(0)).getName()+")",R.drawable.dialog_information)
		  		.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					ZipFile zf=createZipFile(mMainFilePath,mZipFileNameEncodingSelected);
					try {
						if (tfi.isDirectory()) {
							@SuppressWarnings("unchecked")
							List<FileHeader> fhl=zf.getFileHeaders();
							long item_cnt=-1, item_comp_size=0, item_uncomp_size=0;
							for (FileHeader fh:fhl) {
								if (fh.getFileName().startsWith(tfi.getZipFileName())) {
									item_cnt++;
									item_uncomp_size+=fh.getUncompressedSize();
									item_comp_size+=fh.getCompressedSize();
								}
							}
							String prop=String.format(mContext.getString(R.string.msgs_zip_zip_item_property_directory), 
									 item_cnt, item_comp_size, item_uncomp_size);
							mCommonDlg.showCommonDialog(false, "I", "/"+tfi.getZipFileName()+"/", prop, null);
							
						} else {
							FileHeader fh=zf.getFileHeader(tfi.getZipFileName());
							String comp_method="UNKNOWN:";
							if (fh.getCompressionMethod()==Zip4jConstants.COMP_DEFLATE) comp_method="DEFLATE"; 
							else if (fh.getCompressionMethod()==Zip4jConstants.COMP_STORE) comp_method="STORE";
							else if (fh.getCompressionMethod()==Zip4jConstants.COMP_AES_ENC) comp_method="AES";
							else comp_method+=fh.getCompressionMethod();
							long comp_size=fh.getCompressedSize();
//							long last_mod=fh.getLastModFileTime();
//							Log.v("","enc="+fh.getEncryptionMethod());
//							if (fh.getAesExtraDataRecord()!=null)
//								Log.v("","aes="+fh.getAesExtraDataRecord().getAesStrength());
							long uncomp_size=fh.getUncompressedSize();
							long last_mod=ZipUtil.dosToJavaTme(fh.getLastModFileTime());
							String prop=String.format(mContext.getString(R.string.msgs_zip_zip_item_property_file), 
									 StringUtil.convDateTimeTo_YearMonthDayHourMinSec(last_mod), 
									 comp_method, comp_size,uncomp_size, fh.isEncrypted());
							mCommonDlg.showCommonDialog(false, "I", "/"+fh.getFileName(), prop, null);
						}
					} catch (ZipException e) {
						e.printStackTrace();
						mCommonDlg.showCommonDialog(false, "E", n_tfl.get(0).getName(), 
								"ZipException\n"+e.getMessage(), null);
					}
					
				}
		  	});
		}

		if (tfi.isDirectory() && sel_count==1) {
			mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_open_directory)+
					"("+(n_tfl.get(0)).getName()+")",R.drawable.cc_folder)
	  			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					String dir=tfi.getPath().equals("")?tfi.getName():tfi.getPath()+"/"+tfi.getName();
					ArrayList<TreeFilelistItem> tfl=createTreeFileList(mZipFileList, dir);
					mTreeFilelistAdapter.setDataList(tfl);
					mZipFileCurrentDirectory.setText("/"+dir);
					if (tfl.size()>0) mTreeFilelistView.setSelection(0);
					setTopUpButtonEnabled(true);
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
		
		mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_extract)+"("+sel_list+")",R.drawable.context_button_extract)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				extractDlg(tfa);
			}
		});

		if (sel_count==1) {
			if (!tfi.isDirectory()) {
				mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_zip)+"("+(n_tfl.get(0)).getName()+")",R.drawable.context_button_archive)
			  		.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						invokeBrowser(tfi.getPath(), tfi.getName(), MIME_TYPE_ZIP);
					}
			  	});
				mCcMenu.addMenuItem(mContext.getString(R.string.msgs_main_local_file_ccmenu_force_text)+"("+(n_tfl.get(0)).getName()+")", R.drawable.cc_sheet)
		  			.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						invokeBrowser(tfi.getPath(), tfi.getName(), MIME_TYPE_TEXT);
					}
		  		});
			}
		}
		mCcMenu.createMenu();
	};

	private boolean extractSpecificFile(ThreadCtrl tc, ZipFile zf, String zip_file_name, 
			String dest_path, String dest_file_name) {
		if (dest_path.startsWith(mGp.internalRootDirectory)) return extractSpecificFileByInternal(tc, zf,zip_file_name, dest_path, dest_file_name);
		else return extractSpecificFileByExternal(tc, zf,zip_file_name, dest_path, dest_file_name);
	};

		
	private boolean extractSpecificFileByInternal(ThreadCtrl tc, ZipFile zf, String zip_file_name, 
			String dest_path, String dest_file_name) {
		boolean result=false;
		String w_path=dest_path.endsWith("/")?dest_path+dest_file_name:dest_path+"/"+dest_file_name;
		File to=new File(w_path);
		try {
			if (tc.isEnabled()) {
				FileHeader fh=zf.getFileHeader(zip_file_name);
				InputStream is=zf.getInputStream(fh);
				File lf=new File(dest_path);
				lf.mkdirs();
				FileOutputStream os=new FileOutputStream(to);
				long fsz=fh.getUncompressedSize();
				long frc=0;
				byte[] buff=new byte[1024*1024*4];
				int rc=is.read(buff);
				while(rc>0) {
					if (!tc.isEnabled()) break;
					os.write(buff,0,rc);
					frc+=rc;
					long progress=(frc*100)/(fsz);
					putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_extract_file_extracting),
							zip_file_name, progress));
					rc=is.read(buff);
				}
				os.flush();
				os.close();
				is.close();
				if (!tc.isEnabled()) to.delete();
				result=true;
			}
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
		mUtil.addDebugMsg(1,"I", 
				"extractSpecificFile result="+result+", zip file name="+zip_file_name+", dest="+dest_path+", dest file name="+dest_file_name);
		return result;
	};

	private boolean extractSpecificFileByExternal(ThreadCtrl tc, ZipFile zf, String zip_file_name, 
			String dest_path, String dest_file_name) {
		boolean result=false;
		try {
			if (tc.isEnabled()) {
				FileHeader fh=zf.getFileHeader(zip_file_name);
				InputStream is=zf.getInputStream(fh);
				
				String w_path=dest_path.endsWith("/")?dest_path+dest_file_name:dest_path+"/"+dest_file_name;
				SafFile root_sf=mGp.safMgr.getSdcardSafFile();
				SafFile out_dir_sf=mGp.safMgr.getSafFileBySdcardPath(root_sf, dest_path, true);
				SafFile out_file_sf=mGp.safMgr.getSafFileBySdcardPath(root_sf, w_path, false);
				OutputStream os=mContext.getContentResolver().openOutputStream(out_file_sf.getUri());
				
				long fsz=fh.getUncompressedSize();
				long frc=0;
				byte[] buff=new byte[1024*1024*4];
				int rc=is.read(buff);
				while(rc>0) {
					if (!tc.isEnabled()) break;
					os.write(buff,0,rc);
					frc+=rc;
					long progress=(frc*100)/(fsz);
					putProgressMessage(String.format(mContext.getString(R.string.msgs_zip_extract_file_extracting),
							zip_file_name, progress));
					rc=is.read(buff);
				}
				os.flush();
				os.close();
				is.close();
				if (!tc.isEnabled()) out_file_sf.delete();
				result=true;
			}
		} catch (ZipException e) {
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
			tc.setThreadMessage(e.getMessage());
		} catch (IOException e) {
			mUtil.addLogMsg("I", e.getMessage());
			CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
			tc.setThreadMessage(e.getMessage());
		}
		mUtil.addDebugMsg(1,"I", 
				"extractSpecificFile result="+result+", zip file name="+zip_file_name+", dest="+dest_path+", dest file name="+dest_file_name);
		return result;
	};

	private void invokeBrowser(final String p_dir, final String f_name, String mime_type) {
		final String work_dir=mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+WORK_DIRECTORY;
		String fid=CommonUtilities.getFileExtention(f_name);
		String w_mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		final String mt=mime_type.equals("")?w_mt:mime_type;
		final Handler hndl=new Handler();
		if (mt != null) {
			try {
				final ZipFile zf=createZipFile(mMainFilePath,mZipFileNameEncodingSelected);
				final String e_name=p_dir.equals("")?f_name:p_dir+"/"+f_name;
				FileHeader fh=zf.getFileHeader(e_name);
				NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
				ntfy_pswd.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						String pswd="";
						if (o!=null && o[0]!=null) pswd=(String)o[0];
						try {
							if (!pswd.equals("")) zf.setPassword(pswd);
							final ThreadCtrl tc=new ThreadCtrl();
							mDialogProgressSpinCancel.setEnabled(true);
							mDialogProgressSpinCancel.setOnClickListener(new OnClickListener(){
								@Override
								public void onClick(View v) {
									confirmCancel(tc,mDialogProgressSpinCancel);
								}
							});
							setUiDisabled();
							showDialogProgress();
							Thread th=new Thread(){
								@Override
								public void run() {
									putProgressMessage(
											String.format(mContext.getString(R.string.msgs_zip_specific_extract_file_extracting),f_name));
//									ZipFile e_zf=createZipFile(mMainFilePath);
									if (extractSpecificFile(tc, zf, e_name, work_dir, f_name)) {
										try {
											Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
											intent.setDataAndType(Uri.parse("file://"+work_dir+"/"+f_name), mt);
											mActivity.startActivity(intent);
										} catch(ActivityNotFoundException e) {
											mCommonDlg.showCommonDialog(false,"E", 
													String.format(mContext.getString(R.string.msgs_zip_specific_extract_file_viewer_not_found),f_name,mt),"",null);
											new File(work_dir+"/"+f_name).delete();
										}
									}
									hndl.post(new Runnable(){
										@Override
										public void run() {
											setUiEnabled();
										}
									});
								}
							};
							th.start();
						} catch (ZipException e) {
							mUtil.addLogMsg("I", e.getMessage());
							CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
							hndl.post(new Runnable(){
								@Override
								public void run() {
									setUiEnabled();
								}
							});
						}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				if (fh.isEncrypted()) {
					getZipPasswordDlg(zf, fh, ntfy_pswd);
				} else {
					ntfy_pswd.notifyToListener(true, null);
				}
			} catch (ZipException e) {
				mUtil.addLogMsg("I", e.getMessage());
				CommonUtilities.printStackTraceElement(mUtil, e.getStackTrace());
			}
		} else {
			mCommonDlg.showCommonDialog(false,"E", 
					String.format(mContext.getString(R.string.msgs_zip_specific_extract_mime_type_not_found),f_name),"",null);
		}

	};
}
