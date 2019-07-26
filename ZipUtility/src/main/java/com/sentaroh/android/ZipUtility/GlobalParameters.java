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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.Button;

import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.SafManager;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.ZipUtility.Log.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerWriter;

import java.io.File;
import java.util.ArrayList;

import static com.sentaroh.android.ZipUtility.Constants.APPLICATION_TAG;
import static com.sentaroh.android.ZipUtility.Constants.LOG_FILE_NAME;

public class GlobalParameters extends CommonGlobalParms {
	public boolean debugEnabled=true;
	public boolean activityIsDestroyed=false;
	public boolean activityIsBackground=false;
	public int applicationTheme=-1;
	public ThemeColorList themeColorList=null;
	public boolean themeIsLight=false;

	public ISvcCallback callbackStub=null;

	public Context appContext=null;
	
	public boolean debuggable=false;

	public boolean externalStorageIsMounted=false;
	public boolean externalStorageAccessIsPermitted=false;
	
	final static public String STORAGE_STATUS_UNMOUNT="/unknown";
	public String internalRootDirectory=STORAGE_STATUS_UNMOUNT;
	public String externalRootDirectory=STORAGE_STATUS_UNMOUNT;
	public String appSpecificDirectory="/Android/data/com.sentaroh.android."+APPLICATION_TAG+"/files";
	public String applicationRootDirectory="/";
	public String applicationCacheDirectory="/";
	
	public SafManager safMgr=null;

	public ArrayList<TreeFilelistItem> copyCutList=new ArrayList<TreeFilelistItem>();
	public String copyCutFilePath="";
	public String copyCutCurrentDirectory="";
	public String copyCutEncoding="";
	public String copyCutType=COPY_CUT_FROM_LOCAL;
	public final static String COPY_CUT_FROM_LOCAL="L";
	public final static String COPY_CUT_FROM_ZIP="Z";
	public boolean copyCutModeIsCut=false;
	public Button copyCutItemClear=null;
	public Button copyCutItemInfo=null;
	
//	Settings parameter	    	
	public boolean settingExitClean=true;
	public int     settingDebugLevel=3;
	public boolean settingUseLightTheme=false;
	public int     settingLogMaxFileCount=10;		
	public String  settingLogMsgDir="", settingLogMsgFilename=LOG_FILE_NAME;
	public boolean settingLogOption=false;
	public boolean settingPutLogcatOption=false;
	
	public boolean settingFixDeviceOrientationToPortrait=false;
	
	public String  settingZipDefaultEncoding="UTF-8";
	public String  settingNoCompressFileType=DEFAULT_NOCOMPRESS_FILE_TYPE;
	static final public String DEFAULT_NOCOMPRESS_FILE_TYPE=
			"aac;avi;gif;ico;gz;jpe;jpeg;jpg;m3u;m4a;m4u;mov;movie;mp2;mp3;mpe;mpeg;mpg;mpga;ogg;png;qt;ra;ram;svg;tgz;wmv;"; 
	
	public Handler uiHandler=null;


	public GlobalParameters() {
//		Log.v("","constructed");
	};
	
//	@SuppressLint("Wakelock")
//	@Override
//	public void onCreate() {
////		Log.v("","onCreate dir="+getFilesDir().toString());
//		appContext=this.getApplicationContext();
//		uiHandler=new Handler();
//		debuggable=isDebuggable();
//
//		internalRootDirectory=Environment.getExternalStorageDirectory().toString();
//
//		applicationRootDirectory=getFilesDir().toString();
//		applicationCacheDirectory=getCacheDir().toString();
//
//		initSettingsParms(this);
//		loadSettingsParms(this);
//		setLogParms(this);
//
//		initStorageStatus(this);
//
//	};

    public void initGlobalParameter(Context c) {
//		Log.v("","onCreate dir="+getFilesDir().toString());
        appContext=c;
        uiHandler=new Handler();
        debuggable=isDebuggable();

        internalRootDirectory=Environment.getExternalStorageDirectory().toString();

        applicationRootDirectory=c.getFilesDir().toString();
        applicationCacheDirectory=c.getCacheDir().toString();

        final LogUtil slf4j_lu = new LogUtil(appContext, "SLF4J", this);
        Slf4jLogWriter slf4j_lw=new Slf4jLogWriter(slf4j_lu);
        slf4jLog.setWriter(slf4j_lw);


        initSettingsParms(c);
        loadSettingsParms(c);
        setLogParms(this);

        initStorageStatus(c);

    };
    class Slf4jLogWriter extends LoggerWriter {
        private LogUtil mLu =null;
        public Slf4jLogWriter(LogUtil lu) {
            mLu =lu;
        }
        @Override
        public void write(String msg) {
            mLu.addDebugMsg(1,"I", msg);
        }
    }

    public void clearParms() {
	};
	
	@SuppressLint("NewApi")
	public void initStorageStatus(Context c) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {  
    		externalStorageIsMounted=false;
    	} else  {  
    		externalStorageIsMounted=true;
    	}
		
		if (Build.VERSION.SDK_INT>=23) {
			externalStorageAccessIsPermitted=
					(c.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);
		} else {
			externalStorageAccessIsPermitted=true;
		}
		
		refreshMediaDir(c);
	};
	
	public void refreshMediaDir(Context c) {
        if (safMgr==null) {
            safMgr=new SafManager(c, settingDebugLevel>0);
        } else {
            safMgr.loadSafFile();
        }
//		File[] fl=ContextCompat.getExternalFilesDirs(c, null);
        File[] fl=c.getExternalFilesDirs(null);
        externalRootDirectory=STORAGE_STATUS_UNMOUNT;
		if (fl!=null) {
			for(File item:fl) {
				if (item!=null && !item.getPath().startsWith(internalRootDirectory) && safMgr.hasExternalMediaPath()) {
					externalRootDirectory=item.getPath().substring(0,item.getPath().indexOf("/Android"));
					break;
				}
			}
		}
	};
	
	public void setLogParms(GlobalParameters gp) {
		setDebugLevel(gp.settingDebugLevel);
		setLogcatEnabled(gp.settingPutLogcatOption);
		setLogLimitSize(2*1024*1024);
		setLogMaxFileCount(gp.settingLogMaxFileCount);
		setLogEnabled(gp.settingLogOption);
        setLogDirName(gp.appContext.getExternalFilesDir(null)+"/log/");
//		setLogDirName(gp.settingLogMsgDir);
		setLogFileName(gp.settingLogMsgFilename);
		setApplicationTag(APPLICATION_TAG);
	}
	
//	private int mTextColorForeground=0;
//	private int mTextColorBackground=0;
//	public void initTextColor(Context c) {
//    	TypedValue outValue = new TypedValue();
//    	c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
//    	mTextColorForeground=c.getResources().getColor(outValue.resourceId);
//    	c.getTheme().resolveAttribute(android.R.attr.colorBackground, outValue, true);
//    	mTextColorBackground=c.getResources().getColor(outValue.resourceId);
//    	Log.v("","f="+String.format("0x%08x", mTextColorForeground));
//    	Log.v("","b="+String.format("0x%08x", mTextColorBackground));
//	};

	public void initSettingsParms(Context c) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		if (prefs.getString(c.getString(R.string.settings_log_dir), "-1").equals("-1")) {
			Editor pe=prefs.edit();
			
			pe.putString(c.getString(R.string.settings_log_dir), internalRootDirectory+"/"+APPLICATION_TAG+"/");
			
			pe.putString(c.getString(R.string.settings_no_compress_file_type),
				"aac;ai;avi;gif;gz;jpe;jpeg;jpg;m3u;m4a;m4u;mov;movie;mp3;mp4;mpe;mpeg;mpg;mpga;pdf;png;psd;qt;ra;ram;svg;tgz;wmv;");

			pe.putBoolean(c.getString(R.string.settings_exit_clean), true);
			
			pe.commit();
		}
		if (!prefs.contains(c.getString(R.string.settings_zip_default_encoding))) {
//			Locale lc=Locale.getDefault();
			String enc="UTF-8";
//			if (lc.getCountry().equals("JP")) enc="SHIFT_JIS";
			prefs.edit().putString(c.getString(R.string.settings_zip_default_encoding), enc).commit();
		}

	};

	public void setSettingOptionLogEnabled(Context c, boolean enabled) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putBoolean(c.getString(R.string.settings_log_option), enabled).commit();
        this.settingLogOption=enabled;
        if (settingDebugLevel==0 && enabled) {
            prefs.edit().putString(c.getString(R.string.settings_log_level), "1").commit();
            settingDebugLevel=1;
        } else {
            if (!enabled) {
                settingDebugLevel=0;
                prefs.edit().putString(c.getString(R.string.settings_log_level), "0").commit();
            }
        }
        setLogParms(this);
    };

    private static Logger slf4jLog = LoggerFactory.getLogger(GlobalParameters.class);
	public void loadSettingsParms(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

		settingDebugLevel=Integer.parseInt(prefs.getString(c.getString(R.string.settings_log_level), "0")); 
		settingLogMaxFileCount=Integer.valueOf(prefs.getString(c.getString(R.string.settings_log_file_max_count), "10"));
		settingLogMsgDir=prefs.getString(c.getString(R.string.settings_log_dir),internalRootDirectory+"/"+APPLICATION_TAG+"/");
		settingLogOption=prefs.getBoolean(c.getString(R.string.settings_log_option), false); 
		settingPutLogcatOption=prefs.getBoolean(c.getString(R.string.settings_put_logcat_option), false);

		settingNoCompressFileType=prefs.getString(c.getString(R.string.settings_no_compress_file_type),DEFAULT_NOCOMPRESS_FILE_TYPE);

		themeIsLight=settingUseLightTheme=prefs.getBoolean(c.getString(R.string.settings_use_light_theme), false);
		if (settingUseLightTheme) {
			applicationTheme=R.style.MainLight;
//			dialogViewBackGroundColor=Color.argb(255, 50, 50, 50);//.BLACK;
		} else {
			applicationTheme=R.style.Main;
//			dialogViewBackGroundColor=Color.argb(255, 50, 50, 50);//.BLACK;
		}
//		if (Build.VERSION.SDK_INT>=21) dialogViewBackGroundColor=0xff333333;
		settingFixDeviceOrientationToPortrait=prefs.getBoolean(c.getString(R.string.settings_device_orientation_portrait),false);
		
		settingZipDefaultEncoding=prefs.getString(c.getString(R.string.settings_zip_default_encoding), "UTF-8");

		if (settingDebugLevel==1) {
            slf4jLog.setLogOption(true,true,true,false,true);
        } else if (settingDebugLevel==2) {
            slf4jLog.setLogOption(true,true,true,true,true);
        }
	};
	
	private boolean isDebuggable() {
		boolean result=false;
        PackageManager manager = appContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(appContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
        	result=false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
        	result=true;
//        Log.v("","debuggable="+result);
        return result;
    };
    
	@SuppressLint("NewApi")
	static private boolean isScreenOn(Context context, CommonUtilities util) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT>=23) {
        	util.addDebugMsg(1, "I", "isDeviceIdleMode()="+pm.isDeviceIdleMode()+
            		", isPowerSaveMode()="+pm.isPowerSaveMode()+", isInteractive()="+pm.isInteractive());
        } else {
        	util.addDebugMsg(1, "I", "isPowerSaveMode()="+pm.isPowerSaveMode()+", isInteractive()="+pm.isInteractive());
        }
        return pm.isInteractive();
    };
	
}

