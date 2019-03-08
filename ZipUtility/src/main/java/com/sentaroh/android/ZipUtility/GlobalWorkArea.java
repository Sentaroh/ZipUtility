package com.sentaroh.android.ZipUtility;

import android.content.Context;

public class GlobalWorkArea {
    static private GlobalParameters gp=null;
    static public GlobalParameters getGlobalParameters(Context c) {
        if (gp ==null) {
            gp =new GlobalParameters();
            gp.initGlobalParameter(c);
        } else {
            gp.refreshMediaDir(c);
        }
        return gp;
    }
}
