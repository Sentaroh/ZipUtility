package com.sentaroh.android.ZipUtility;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/ 

import static com.sentaroh.android.ZipUtility.Constants.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

import com.sentaroh.android.Utilities.ContentProviderUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.Widget.CustomTabContentView;
import com.sentaroh.android.Utilities.Widget.CustomViewPager;
import com.sentaroh.android.Utilities.Widget.CustomViewPagerAdapter;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.ZipUtility.Log.LogFileListDialogFragment;

@SuppressLint("NewApi")
public class ActivityMain extends AppCompatActivity {
	 
	private GlobalParameters mGp=null;
	
	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	private CommonDialog mCommonDlg=null;

	private FragmentManager mFragmentManager=null;
	
	private Context mContext;
	private ActivityMain mActivity;
	
	private CommonUtilities mUtil=null;

	private ActionBar mActionBar=null;

	private LocalFileManager mLocalFileMgr=null;
	private ZipFileManager mZipFileMgr=null;
	@SuppressWarnings("unused")
	private TarFileManager mTarFileMgr=null;

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if (mUtil!=null) {
	    	mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" Entered, " ,
	    			"New orientation="+newConfig.orientation+
	    			", New language=",newConfig.locale.getLanguage());
//	    	if (mLocalFileMgr!=null) mLocalFileMgr.refreshFileList();
	    }
	};

	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		mUtil.addDebugMsg(1, "I", "onSaveInstanceState entered");
		
		saveViewContents(outState);
	};  

	private void saveViewContents(Bundle outState) {
	};
	
	@Override  
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		mUtil.addDebugMsg(1, "I", "onRestoreInstanceState entered");
		restoreViewContents(savedState);
		mRestartStatus=2;
	};

	private void restoreViewContents(Bundle savedState) {
	};
	
	@Override
	protected void onNewIntent(Intent in) {
		super.onNewIntent(in);
		mUtil.addDebugMsg(1, "I", "onNewIntent entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==2) return;
		if (in!=null && in.getData()!=null) showZipFileByIntent(in);
	};

	private void showZipFileByIntent(Intent intent) {
		if (intent!=null && intent.getData()!=null) {
			mUtil.addDebugMsg(1,"I","showZipFileByIntent entered, "+"Uri="+intent.getData()+", type="+intent.getType());
			String cd=getCacheDir()+"/Attached/zip/";

//            String id=intent.getData().getPath().substring(intent.getData().getPath().toString().length()-3);
//            Log.v("","id="+id);
//            String sel = MediaStore.Images.Media._ID + "=?";
//            File tlf=null;
//            Cursor cursor = getContentResolver().query(intent.getData(),
//                    column, sel, new String[]{ id }, null);
//            int columnIndex = cursor.getColumnIndex(column[1]);
//            if (cursor.moveToFirst()) {
//                String path = cursor.getString(columnIndex);
//                tlf=new File(path);
//                Log.v("","name="+path);
//            }
//            cursor.close();
            String file_path=ContentProviderUtil.getFilePath(mContext, cd, intent.getData());
    		if (file_path!=null){// && file_path.endsWith(".zip")) {
                mLocalFileMgr.showLocalFileView(false);
                Handler hndl=new Handler();
                showZipFile(file_path);
                hndl.post(new Runnable(){
                    @Override
                    public void run() {
                        mLocalFileMgr.showLocalFileView(true);
                    }
                });
		        refreshOptionMenu();
    		}
		}
	};

	private void initViewWidget() {
        getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.main_screen);
        
        createTabView();
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        mContext=this;
        mActivity=this;
        mFragmentManager=getSupportFragmentManager();
        mRestartStatus=0;
       	mGp=GlobalWorkArea.getGlobalParameters(mContext);
        setTheme(mGp.applicationTheme);
        mGp.themeColorList=ThemeUtil.getThemeColorList(mActivity);
        super.onCreate(savedInstanceState);
        
		mActionBar=getSupportActionBar();
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setHomeButtonEnabled(false);

        mUtil=new CommonUtilities(mContext, "ZipActivity", mGp);
        
        mUtil.addDebugMsg(1, "I", "onCreate entered");

        mCommonDlg=new CommonDialog(mActivity, mFragmentManager);
        
        if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        initViewWidget();
        
    	mGp.copyCutList=new ArrayList<TreeFilelistItem>();
    	mGp.copyCutModeIsCut=false;

        mLocalFileMgr=new LocalFileManager(mGp, this, mFragmentManager, mLocalView);
        mLocalFileMgr.showLocalFileView(false);
//        mTarFileMgr=new TarFileManager(mGp, mActivity, mFragmentManager, mZipView, "");
        mZipFileMgr=new ZipFileManager(mGp, mActivity, mFragmentManager, mZipView);

        checkRequiredPermissions();
	};
    
	@Override
	public void onStart() {
		super.onStart();
		mUtil.addDebugMsg(1, "I", "onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		mUtil.addDebugMsg(1, "I", "onRestart entered");
	};

	@Override
	public void onResume() {
		super.onResume();
		mUtil.addDebugMsg(1, "I", "onResume entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==1) {
			if (isUiEnabled()) {
				mZipFileMgr.refreshFileList();
				mLocalFileMgr.refreshFileList();
			}
	        try {
				mSvcClient.aidlSetActivityInForeground();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else {
			CommonUtilities.cleanupWorkFile(mGp);
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
			        try {
						mSvcClient.aidlSetActivityInForeground();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
//			        String fp ="";
					if (mRestartStatus==0) {
				    	Intent in=getIntent();
						if (in!=null && in.getData()!=null) showZipFileByIntent(in);
						else mLocalFileMgr.showLocalFileView(true);
					} else if (mRestartStatus==2) {
						if (mGp.activityIsDestroyed) {
							mCommonDlg.showCommonDialog(false, "W",  
							getString(R.string.msgs_main_restart_by_destroyed),"",null);
						} else {
							mCommonDlg.showCommonDialog(false, "W",  
							getString(R.string.msgs_main_restart_by_killed),"",null);
						}
					}
					mRestartStatus=1;
					mGp.activityIsDestroyed=false;

				}
				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
				
			});
			openService(ntfy);
		}
	};
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mUtil.addDebugMsg(1, "I", "onLowMemory entered");
        // Application process is follow
		
	};
	
	@Override
	public void onPause() {
		super.onPause();
		mUtil.addDebugMsg(1, "I", "onPause entered");
        // Application process is follow
		
	};

	@Override
	public void onStop() {
		super.onStop();
		mUtil.addDebugMsg(1, "I", "onStop entered");
        // Application process is follow
		try {
			if (!isUiEnabled()) mSvcClient.aidlSetActivityInBackground();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUtil.addDebugMsg(1, "I", "onDestroy entered");
        // Application process is follow
		CommonUtilities.cleanupWorkFile(mGp);
		closeService();
		if (mTerminateApplication) {
			mGp.settingExitClean=true;
//			mZipFileMgr.cleanZipFileManager();
//			System.gc();
//			Handler hndl=new Handler();
//			hndl.postDelayed(new Runnable(){
//				@Override
//				public void run() {
//					android.os.Process.killProcess(android.os.Process.myPid());
//				}
//			}, 100);
		} else {
			mGp.activityIsDestroyed=true;
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (isUiEnabled()) {
					if (mMainTabHost.getCurrentTab()==0) {//Local tab
						if (mLocalFileMgr.isUpButtonEnabled()) {
							mLocalFileMgr.performClickUpButton();
							return true;
						}
					} else {//Zip folder
						if (mZipFileMgr.isUpButtonEnabled()) {
							mZipFileMgr.performClickUpButton();
							return true;
						} else {
//							mZipFileMgr.
						}
					}
					mTerminateApplication=true;
					confirmExit();
				} else {
					Intent in=new Intent();
					in.setAction(Intent.ACTION_MAIN);
					in.addCategory(Intent.CATEGORY_HOME);
					startActivity(in);
				}
				return true;
				// break;
			default:
				return super.onKeyDown(keyCode, event);
				// break;
		}
	};
	
	@SuppressLint("NewApi")
	public void refreshOptionMenu() {
		if (Build.VERSION.SDK_INT >= 11) invalidateOptionsMenu();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mUtil.addDebugMsg(1, "I", "onCreateOptionsMenu Entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mUtil.addDebugMsg(2, "I", "onPrepareOptionsMenu Entered");
        super.onPrepareOptionsMenu(menu);
        
        if (mMainTabHost.getCurrentTabTag().equals(mContext.getString(R.string.msgs_main_tab_name_local))) {
        	if (mLocalFileMgr!=null) {
        		if (mLocalFileMgr.isFileListSortAscendant()) menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_asc_gray);
        		else menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_dsc_gray);
        	} else {
        		menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_asc_gray);
        	}
        } else {
        	if (mZipFileMgr!=null) {
        		if (mZipFileMgr.isFileListSortAscendant()) menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_asc_gray);
        		else menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_dsc_gray);
        	} else {
        		menu.findItem(R.id.menu_top_sort).setIcon(R.drawable.ic_128_sort_asc_gray);
        	}
        }
        if (isUiEnabled()) {
        	menu.findItem(R.id.menu_top_find).setEnabled(true);
        	menu.findItem(R.id.menu_top_refresh).setEnabled(true);
        	menu.findItem(R.id.menu_top_sort).setEnabled(true);
        	menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
        	menu.findItem(R.id.menu_top_log_management).setEnabled(true);
        	menu.findItem(R.id.menu_top_about).setEnabled(true);
        	menu.findItem(R.id.menu_top_settings).setEnabled(true);
        } else {
        	menu.findItem(R.id.menu_top_find).setEnabled(false);
        	menu.findItem(R.id.menu_top_refresh).setEnabled(false);
        	menu.findItem(R.id.menu_top_sort).setEnabled(false);
        	menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
        	menu.findItem(R.id.menu_top_log_management).setEnabled(false);
        	menu.findItem(R.id.menu_top_about).setEnabled(false);
        	menu.findItem(R.id.menu_top_settings).setEnabled(false);
        }
        return true;
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { 
		mUtil.addDebugMsg(2, "I", "onOptionsItemSelected Entered");
		switch (item.getItemId()) {
			case android.R.id.home:
				return true;
			case R.id.menu_top_find:
				if (mMainTabHost.getCurrentTabTag().equals(mContext.getString(R.string.msgs_main_tab_name_local))) mLocalFileMgr.searchFile();
				else mZipFileMgr.searchFile();
				return true;
			case R.id.menu_top_refresh:
				if (mMainTabHost.getCurrentTabTag().equals(mContext.getString(R.string.msgs_main_tab_name_local))) mLocalFileMgr.refreshFileList();
				else mZipFileMgr.refreshFileList(true);
				return true;
			case R.id.menu_top_sort:
				if (mMainTabHost.getCurrentTabTag().equals(mContext.getString(R.string.msgs_main_tab_name_local))) mLocalFileMgr.sortFileList();
				else mZipFileMgr.sortFileList();
				return true;
			case R.id.menu_top_browse_log:
				invokeLogFileBrowser();
				return true;
			case R.id.menu_top_log_management:
				invokeLogManagement();
				return true;
			case R.id.menu_top_about:
//				Thread th=new Thread(){
//					@Override
//					public void run() {
//						try {
//							BufferedZipFile zf=new BufferedZipFile(new File("/sdcard/newZip.zip"),"UTF-8",true);
//							ZipParameters zp=new ZipParameters();
//							zf.removeItem(new String[]{"ZipUtility.log"});
//							zf.addItem("/sdcard/TextFileBrowser.zip", zp);
//							zf.addItem("/sdcard/TextFileBrowser/", zp);
//							zf.close();
//						} catch (ZipException e) {
//							e.printStackTrace();
//						}
//						
//					}
//				};
//				th.start();
				aboutApplicaion();
				return true;			
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;			
			case R.id.menu_top_quit:
				mGp.settingExitClean=true;
				confirmExit();
				return true;			
			case R.id.menu_top_kill:
				confirmKill();
				return true;			
		}

		return false;
	};
	
	private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE=1;
	@SuppressLint("NewApi")
	private void checkRequiredPermissions() {
		if (Build.VERSION.SDK_INT>=23) {
			mUtil.addDebugMsg(1, "I", "Prermission WriteExternalStorage="+checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)+
					", WakeLock="+checkSelfPermission(Manifest.permission.WAKE_LOCK)
					);
	        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
	        	NotifyEvent ntfy=new NotifyEvent(mContext);
	        	ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
			            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
			            		REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
			        	NotifyEvent ntfy_term=new NotifyEvent(mContext);
			        	ntfy_term.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c, Object[] o) {
					        	finish();
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {
							}
			        	});
			        	mCommonDlg.showCommonDialog(false, "W", 
			        			mContext.getString(R.string.msgs_main_permission_external_storage_title), 
			        			mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
					}
	        	});
	        	mCommonDlg.showCommonDialog(false, "W", 
	        			mContext.getString(R.string.msgs_main_permission_external_storage_title), 
	        			mContext.getString(R.string.msgs_main_permission_external_storage_request_msg), ntfy);
	        }
		}
	};

	private void showZipFile(final String fp) {
		if (!isUiEnabled()) return;
		mZipFileMgr.showZipFile(fp);
		mMainTabHost.setCurrentTabByTag(mContext.getString(R.string.msgs_main_tab_name_zip));
//		Handler hndl=new Handler();
//		hndl.postDelayed(new Runnable(){
//			@Override
//			public void run() {
//			}
//		},1);
	};
	
	private void invokeLogFileBrowser() {
		mUtil.addDebugMsg(1,"I","Invoke log file browser.");
		mUtil.flushLog();
		if (mGp.settingLogOption) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(Uri.parse("file://"+mUtil.getLogFilePath()), "text/plain");
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				mCommonDlg.showCommonDialog(false, "E", 
						mContext.getString(R.string.msgs_log_file_browse_app_can_not_found), e.getMessage(), null);
			}
		}
	};
	
	private void invokeSettingsActivity() {
		mUtil.addDebugMsg(1,"I","Invoke Settings.");
		Intent intent=null;
		intent = new Intent(this, ActivitySettings.class);
		startActivityForResult(intent,0);
	};

	private void invokeLogManagement() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mGp.setSettingOptionLogEnabled(mContext, (Boolean)o[0]);
//				loadSettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mUtil.flushLog();
		LogFileListDialogFragment lfm=
				LogFileListDialogFragment.newInstance(false, getString(R.string.msgs_log_management_title));
		lfm.showDialog(getSupportFragmentManager(), lfm, mGp, ntfy);
	};

	public boolean isApplicationTerminating() {return mTerminateApplication;}
	
	private void confirmExit() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mTerminateApplication=true;
				finish();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				mGp.settingExitClean=false;
			}
		});
		mCommonDlg.showCommonDialog(true, "W",
				mContext.getString(R.string.msgs_main_exit_confirm_msg), "", ntfy);
	};

	private void confirmKill() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				mGp.settingExitClean=false;
			}
		});
		mCommonDlg.showCommonDialog(true, "W",
				mContext.getString(R.string.msgs_main_kill_confirm_msg), "", ntfy);
	};
	
	private void aboutApplicaion() {
		mCommonDlg.showCommonDialog(false, "I", 
				getString(R.string.msgs_main_about_title), String.format(
				getString(R.string.msgs_main_about_content),getApplVersionName()), 
				null);
	};

	public String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mUtil.addDebugMsg(1, "I", "Return from settings");
		if (requestCode==0) applySettingParms();
		else if (requestCode == ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS) {
			mUtil.addDebugMsg(1,"I","Return from Storage Picker. id="+requestCode);
	        if (resultCode == Activity.RESULT_OK) {
	        	mUtil.addDebugMsg(1,"I","Intent="+data.getData().toString());
        		if (mGp.safMgr.isRootTreeUri(data.getData())) {
	        		mGp.safMgr.addSafFileFromUri(data.getData());
//	        		if (mSafSelectActivityNotify!=null) mSafSelectActivityNotify.notifyToListener(true, null);
	        	} else {
	        		NotifyEvent ntfy_retry=new NotifyEvent(mContext);
	        		ntfy_retry.setListener(new NotifyEventListener(){
						@Override
						public void positiveResponse(Context c, Object[] o) {
			        		NotifyEvent ntfy=new NotifyEvent(mContext);
			        		ntfy.setListener(new NotifyEventListener(){
								@Override
								public void positiveResponse(Context c, Object[] o) {
									startSdcardPicker();
								}
								@Override
								public void negativeResponse(Context c, Object[] o) {
									mCommonDlg.showCommonDialog(false, "W", 
											mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
											mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
											null);
								}
			        		});
			        		showSelectSdcardMsg(ntfy);
						}
						@Override
						public void negativeResponse(Context c, Object[] o) {
						}
	        		});
	        		mCommonDlg.showCommonDialog(true, "W", 
	        				mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg), "", ntfy_retry);
	        	}
	        } else {
	        	if (mGp.safMgr.getSdcardSafFile()==null) {
					mCommonDlg.showCommonDialog(false, "W", 
							mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
							mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
							null);
	        	}
	        }
		}
	};

	public void startSdcardPicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
	};
	
	public void showSelectSdcardMsg(final NotifyEvent ntfy) {
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.show_select_sdcard_dlg);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.show_select_sdcard_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.show_select_sdcard_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		final TextView dlg_msg=(TextView)dialog.findViewById(R.id.show_select_sdcard_dlg_msg);
		String msg="";
		if (Build.VERSION.SDK_INT>=23) msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api23);
		else if (Build.VERSION.SDK_INT>=21) msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api21);
		else msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api21);
		dlg_msg.setText(msg);
		
		final ImageView func_view=(ImageView)dialog.findViewById(R.id.show_select_sdcard_dlg_image);
		
		
		try {
			String fn="";
			if (Build.VERSION.SDK_INT>=23) fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api23);
			else if (Build.VERSION.SDK_INT>=21) fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api21);
			else fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api21);
		    InputStream is=mContext.getResources().getAssets().open(fn);
		    Bitmap bm = BitmapFactory.decodeStream(is);
		    func_view.setImageBitmap(bm);
		} catch (IOException e) {
		    /* ��O���� */
		}
		
		final Button btnOk = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_ok);
		final Button btnCancel = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_cancel);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		// OK�{�^���̎w��
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy.notifyToListener(true, null);
			}
		});
		// Cancel�{�^���̎w��
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy.notifyToListener(false, null);
			}
		});
		// Cancel���X�i�[�̎w��
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});

		dialog.show();

	}

	private void applySettingParms() {
		int prev_theme=mGp.applicationTheme;
		mGp.loadSettingsParms(mContext);
		mGp.setLogParms(mGp);
		mGp.refreshMediaDir(mContext);
		
        if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        
        if (prev_theme!=mGp.applicationTheme) {
        	mGp.applicationTheme=prev_theme;
        	mCommonDlg.showCommonDialog(false, "W", mContext.getString(R.string.msgs_main_theme_changed_msg), "", null);
        	mGp.settingExitClean=true;
        }

	};

	private boolean enableMainUi=true; 

	public void setUiEnabled() {
		enableMainUi=true;
		mMainTabHost.setEnabled(enableMainUi);
		mMainTabWidget.setEnabled(enableMainUi);
//		mMainViewPager.setEnabled(enableMainUi);
		mMainViewPager.setSwipeEnabled(enableMainUi);
		refreshOptionMenu();
		
		try {
		    if (mSvcClient!=null)
			    mSvcClient.aidlUpdateNotificationMessage(mContext.getString(R.string.msgs_main_notification_end_message));
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	};
	
	public void putNotificationMsg(String msg) {
		if (mSvcClient!=null) {
			try {
				mSvcClient.aidlUpdateNotificationMessage(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	public void setUiDisabled() {
		enableMainUi=false;
		mMainTabHost.setEnabled(enableMainUi);
		mMainTabWidget.setEnabled(enableMainUi);
//		mMainViewPager.setEnabled(enableMainUi);
		mMainViewPager.setSwipeEnabled(enableMainUi);
		refreshOptionMenu();
	};
	
	public boolean isUiEnabled() {
		return enableMainUi;
	};

	private TabHost mMainTabHost=null;
	private LinearLayout mLocalView;
	private LinearLayout mZipView;

	private CustomViewPager mMainViewPager;
	private CustomViewPagerAdapter mMainViewPagerAdapter;
	
	private TabWidget mMainTabWidget;

	@SuppressWarnings("deprecation")
	@SuppressLint("InflateParams")
	private void createTabView() {
		mMainTabHost=(TabHost)findViewById(android.R.id.tabhost);
		mMainTabHost.setup();
		mMainTabWidget = (TabWidget) findViewById(android.R.id.tabs);
		 
	    mMainTabWidget.setStripEnabled(false);  
	    mMainTabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);  
	    
		CustomTabContentView tabLocal = new CustomTabContentView(this,
				mContext.getString(R.string.msgs_main_tab_name_local));
		mMainTabHost.addTab(mMainTabHost.newTabSpec(
				mContext.getString(R.string.msgs_main_tab_name_local)).setIndicator(tabLocal).
				setContent(android.R.id.tabcontent));
		
		CustomTabContentView tabZip = new CustomTabContentView(this,
				mContext.getString(R.string.msgs_main_tab_name_zip));
		mMainTabHost.addTab(mMainTabHost.newTabSpec(mContext.getString(R.string.msgs_main_tab_name_zip)).
				setIndicator(tabZip).setContent(android.R.id.tabcontent));

		
		LinearLayout ll_main=(LinearLayout)findViewById(R.id.main_screen_view);
		ll_main.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLocalView=(LinearLayout)vi.inflate(R.layout.main_local_file,null);
		if (mGp.themeIsLight) mLocalView.setBackgroundColor(0xffc0c0c0);
		else mLocalView.setBackgroundColor(0xff303030);
		
		LinearLayout dv=(LinearLayout)mLocalView.findViewById(R.id.main_dialog_view);
		dv.setVisibility(LinearLayout.GONE);
		
		LinearLayout lv=(LinearLayout)mLocalView.findViewById(R.id.local_file_view);
		lv.setVisibility(LinearLayout.GONE);
		
//		mLocalView.setBackgroundColor(mGp.themeColorList.window_background_color_content);
		mZipView=(LinearLayout)vi.inflate(R.layout.main_zip_file,null);
		if (mGp.themeIsLight) mZipView.setBackgroundColor(0xffc0c0c0);
		else mZipView.setBackgroundColor(0xff303030);
//		mZipView.setBackgroundColor(mGp.themeColorList.window_background_color_content);

		mMainViewPager=(CustomViewPager)findViewById(R.id.main_screen_pager);
	    mMainViewPagerAdapter=new CustomViewPagerAdapter(this, 
	    		new View[]{mLocalView, mZipView});
	    
	    mMainViewPager.setBackgroundColor(mGp.themeColorList.window_background_color_content);
	    mMainViewPager.setAdapter(mMainViewPagerAdapter);
	    mMainViewPager.setOnPageChangeListener(new MainPageChangeListener());
		if (mRestartStatus==0) {
			mMainTabHost.setCurrentTabByTag(mContext.getString(R.string.msgs_main_tab_name_local));
			mMainViewPager.setCurrentItem(0);
		}
		mMainTabHost.setOnTabChangedListener(new MainOnTabChange());

		mGp.copyCutItemClear=(Button)findViewById(R.id.main_screen_copy_cut_clear_btn);
		mGp.copyCutItemInfo=(Button)findViewById(R.id.main_screen_copy_cut_item);
		
		mGp.copyCutItemInfo.setVisibility(TextView.GONE);
		mGp.copyCutItemClear.setVisibility(Button.GONE);

        mGp.copyCutItemClear.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mGp.copyCutList.clear();
				mGp.copyCutItemInfo.setVisibility(TextView.GONE);
				mGp.copyCutItemClear.setVisibility(Button.GONE);
				mLocalFileMgr.setContextButtonPasteEnabled(false);
				mZipFileMgr.setContextButtonPasteEnabled(false);
			}
        });
        mGp.copyCutItemInfo.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
//				String item_name=mGp.copyCutItemInfo.getText().toString();
				String c_list="", sep="";
				for(TreeFilelistItem tfli:mGp.copyCutList) {
					c_list+=sep+tfli.getPath()+"/"+tfli.getName();
					sep="\n";
				}
				String msg="";
				if (!mGp.copyCutModeIsCut) msg=mContext.getString(R.string.msgs_zip_cont_header_copy); 
				else msg=mContext.getString(R.string.msgs_zip_cont_header_cut);
				String from=mGp.copyCutType.equals(GlobalParameters.COPY_CUT_FROM_LOCAL)?"Local":"ZIP";
				mCommonDlg.showCommonDialog(false, "I", msg+from, c_list, null);
			}
        });
        

	};

	@SuppressWarnings("unused")
	private void setButtonColor(Button btn) {
//		if (Build.VERSION.SDK_INT<11) {
//			btn.setBackgroundColor(Color.DKGRAY);
//		}
	};
	
	private class MainOnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered. tab="+tabId);
			
//			mActionBar.setIcon(R.drawable.smbsync);
//			mActionBar.setHomeButtonEnabled(false);
//			mActionBar.setTitle(R.string.app_name);
			
			mMainViewPager.setCurrentItem(mMainTabHost.getCurrentTab());
			
			if (tabId.equals(mContext.getString(R.string.msgs_main_tab_name_local))) {
				mLocalFileMgr.refreshFileList();
			} else {
				mZipFileMgr.refreshFileList();
			}
			
			refreshOptionMenu();
		};
	};
	
	private class MainPageChangeListener implements ViewPager.OnPageChangeListener {  
	    @Override  
	    public void onPageSelected(int position) {
	    	mUtil.addDebugMsg(1,"I","onPageSelected entered, pos="+position);
	        mMainTabWidget.setCurrentTab(position);
	        mMainTabHost.setCurrentTab(position);
	    }  
	  
	    @Override  
	    public void onPageScrollStateChanged(int state) {  
//	    	mUtil.addDebugMsg(1,"I","onPageScrollStateChanged entered, state="+state);
	    }  
	  
	    @Override  
	    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//	    	mUtil.addDebugMsg(1, "I","onPageScrolled entered, pos="+position);
	    }  
	};  
	
	private static ISvcClient mSvcClient=null;
	private static ServiceConnection mSvcConnection=null;

	private void openService(final NotifyEvent p_ntfy) {
 		mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
        mSvcConnection = new ServiceConnection(){
    		public void onServiceConnected(ComponentName arg0, IBinder service) {
    	    	mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
    	    	mSvcClient=ISvcClient.Stub.asInterface(service);
   	    		p_ntfy.notifyToListener(true, null);
    		}
    		public void onServiceDisconnected(ComponentName name) {
    			mSvcConnection = null;
   				mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered");
//    	    	mSvcClient=null;
//    	    	synchronized(tcService) {
//        	    	tcService.notify();
//    	    	}
    		}
        };
    	
		Intent intmsg = new Intent(mContext, ZipService.class);
		intmsg.setAction("Bind");
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
	};

	private void closeService() {
    	
		mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" entered, conn="+mSvcConnection);

    	if (mSvcConnection!=null) {
//    		try {
//				if (mSvcClient!=null) mSvcClient.aidlStopService();
//			} catch (RemoteException e) {
//				e.printStackTrace();
//			}
    		mSvcClient=null;
    		unbindService(mSvcConnection);
	    	mSvcConnection=null;
//	    	Log.v("","close service");
    	}
//        Intent intent = new Intent(this, SyncService.class);
//        stopService(intent);
	};
	
//	final private void setCallbackListener() {
//		mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
//		try{
//			mSvcClient.setCallBack(mSvcCallbackStub);
//		} catch (RemoteException e){
//			e.printStackTrace();
//			mUtil.addDebugMsg(1,"E", "setCallbackListener error :"+e.toString());
//		}
//	};
//
//	final private void unsetCallbackListener() {
//		mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
//		if (mSvcClient!=null) {
//			try{
//				mSvcClient.removeCallBack(mSvcCallbackStub);
//			} catch (RemoteException e){
//				e.printStackTrace();
//				mUtil.addDebugMsg(1,"E", "unsetCallbackListener error :"+e.toString());
//			}
//		}
//	};

	@SuppressWarnings("unused")
	private ISvcCallback mSvcCallbackStub=new ISvcCallback.Stub() {
		@Override
		public void cbThreadStarted() throws RemoteException {
		}
		@Override
		public void cbThreadEnded() throws RemoteException {
		}
    };

}
