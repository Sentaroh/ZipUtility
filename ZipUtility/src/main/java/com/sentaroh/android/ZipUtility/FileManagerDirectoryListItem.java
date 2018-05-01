package com.sentaroh.android.ZipUtility;

import java.io.Serializable;
import java.util.ArrayList;

class FileManagerDirectoryListItem implements Serializable {
	private static final long serialVersionUID = 1L;
	public String file_path="";
	public int pos_x=0, pos_y=0;
	public ArrayList<TreeFilelistItem> file_list=null;
}
