package com.sentaroh.android.ZipUtility;

import static com.sentaroh.android.ZipUtility.Constants.*;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.sentaroh.android.ZipUtility.Log.LogUtil;

public class ZipReceiver extends BroadcastReceiver{

	private static Context mContext =null;
	
	private static GlobalParameters mGp=null;
	
	private static LogUtil mLog=null;
	
	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context c, Intent intent) {
//		WakeLock wl=
//   	    		((PowerManager)c.getSystemService(Context.POWER_SERVICE))
//    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
//    				| PowerManager.ON_AFTER_RELEASE, "Receiver");
//		wl.acquire(1000);
		mContext=c;
		if (mGp==null) {
			mGp=new GlobalParameters();
			mGp.appContext=c;
		}
		mGp.loadSettingsParms(c);
		mGp.setLogParms(mGp);
		
		if (mLog==null) mLog=new LogUtil(c, "Receiver", mGp);
		
		String action=intent.getAction();
		if (action!=null) {
			if (action.equals(SERVICE_HEART_BEAT)) {
//				if (mGp.settingDebugLevel>=2) mLog.addDebugMsg(2,"I", "Receiver action="+action);
				Intent in=new Intent(mContext, ZipService.class);
				in.setAction(SERVICE_HEART_BEAT);
				if (intent.getExtras()!=null) in.putExtras(intent.getExtras());
				mContext.startService(in);
			} else {
				if (mGp.settingDebugLevel>=1) mLog.addDebugMsg(1,"I", "Receiver action="+action);
			}
		}
	};
	
}
