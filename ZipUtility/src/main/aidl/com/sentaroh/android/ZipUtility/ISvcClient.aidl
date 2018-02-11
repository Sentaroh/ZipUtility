package com.sentaroh.android.ZipUtility;

import com.sentaroh.android.ZipUtility.ISvcCallback;

interface ISvcClient{
	
	void setCallBack(ISvcCallback callback);
	void removeCallBack(ISvcCallback callback);

	void aidlStopService() ;
	
	void aidlSetActivityInBackground() ;
	void aidlSetActivityInForeground() ;
	
	void aidlUpdateNotificationMessage(String msg_text) ;
}