package com.sentaroh.android.ZipUtility.Log;

import static com.sentaroh.android.ZipUtility.Constants.APPLICATION_TAG;
import static com.sentaroh.android.ZipUtility.Log.LogConstants.*;
import android.content.Context;

import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.LogUtil.CommonLogReceiver;
import com.sentaroh.android.ZipUtility.GlobalParameters;

public class LogReceiver extends CommonLogReceiver{
	@Override
	public void setLogParms(Context c, CommonGlobalParms cgp) {
		
		GlobalParameters mgp=new GlobalParameters();
		mgp.loadSettingsParms(c);
		
		cgp.setDebugLevel(mgp.settingDebugLevel);
		cgp.setLogLimitSize(2*1024*1024);
		cgp.setLogMaxFileCount(mgp.settingLogMaxFileCount);
		cgp.setLogEnabled(mgp.settingLogOption);
		cgp.setLogDirName(mgp.settingLogMsgDir);
		cgp.setLogFileName(mgp.settingLogMsgFilename);
		cgp.setApplicationTag(APPLICATION_TAG);
		cgp.setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	};
}
