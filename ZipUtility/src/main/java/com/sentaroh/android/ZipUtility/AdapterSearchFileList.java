package com.sentaroh.android.ZipUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.lingala.zip4j.util.Zip4jConstants;

import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdapterSearchFileList extends BaseAdapter {
	private Context mContext;
	private ArrayList<TreeFilelistItem>mDataItems=null;
	private ThemeColorList mThemeColorList;
	
	public AdapterSearchFileList(Context c) {
		mContext = c;
		mDataItems=new ArrayList<TreeFilelistItem>();
		initTextColor();
		
	};
	private void initTextColor() {
		mThemeColorList=ThemeUtil.getThemeColorList(mContext);
	}
	
	@Override
	public int getCount() {return mDataItems.size();}

	@Override
	public TreeFilelistItem getItem(int arg0) {return mDataItems.get(arg0);}

	@Override
	public long getItemId(int arg0) {return 0;}
	
	@Override
	public boolean isEnabled(int p) {
		return true;
	};

	public ArrayList<TreeFilelistItem> getDataList() {
		return mDataItems;
	};
	
	private boolean mSortAscendant=true;
	public void setSortAscendant() {
		mSortAscendant=true;
	};
	
	public void setSortDescendant() {
		mSortAscendant=false;
	};
	
	public boolean isSortAscendant() {
		return mSortAscendant;
	};

	public boolean isSortKeyName() {
		return mSortKey==SORT_KEY_NAME;
	};

	public boolean isSortKeySize() {
		return mSortKey==SORT_KEY_SIZE;
	};

	public boolean isSortKeyTime() {
		return mSortKey==SORT_KEY_TIME;
	};

	final static private int SORT_KEY_NAME=0;
	final static private int SORT_KEY_TIME=1;
	final static private int SORT_KEY_SIZE=2;
	private int mSortKey=SORT_KEY_NAME;
	public void setSortKeyName() {
		mSortKey=SORT_KEY_NAME;
	};

	public void setSortKeyTime() {
		mSortKey=SORT_KEY_TIME;
	};

	public void setSortKeySize() {
		mSortKey=SORT_KEY_SIZE;
	};
	
	public void setDataList(ArrayList<TreeFilelistItem> p) {
		mDataItems=p;
		sort();
	}
	
	public void sort() {
		Collections.sort(mDataItems, new Comparator<TreeFilelistItem>(){
			@Override
			public int compare(TreeFilelistItem lhs, TreeFilelistItem rhs) {
				String l_key="", r_key="";
				if (mSortKey==SORT_KEY_NAME) {
//					l_key=lhs.getName();
//					r_key=rhs.getName();
					l_key=lhs.getSortKeyName();
					r_key=rhs.getSortKeyName();
				} else if (mSortKey==SORT_KEY_TIME) {
					l_key=lhs.getSortKeyTime();
					r_key=rhs.getSortKeyTime();
				} else if (mSortKey==SORT_KEY_SIZE) {
					l_key=lhs.getSortKeySize();
					r_key=rhs.getSortKeySize();
				}
				if (mSortAscendant) return l_key.compareToIgnoreCase(r_key);
				else return r_key.compareToIgnoreCase(l_key);
			}
		});
	};

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	 	final ViewHolder holder;
	 	
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.search_file_result_list_item, null);
            holder=new ViewHolder();

            holder.result_view=(LinearLayout)v.findViewById(R.id.search_file_result_list_item_view);
        	holder.file_name=(TextView)v.findViewById(R.id.search_file_result_list_item_file_name);
        	holder.directory_name=(TextView)v.findViewById(R.id.search_file_result_list_item_directory_name);
        	holder.file_size=(TextView)v.findViewById(R.id.search_file_result_list_item_file_size);
        	holder.comp_method=(TextView)v.findViewById(R.id.search_file_result_list_item_comp_method);
        	holder.file_date=(TextView)v.findViewById(R.id.search_file_result_list_item_last_modified_date);
        	holder.file_time=(TextView)v.findViewById(R.id.search_file_result_list_item_last_modified_time);
        	if (mThemeColorList.theme_is_light) {
//    			holder.result_view.setBackgroundColor(mThemeColorList.dialog_msg_background_color);
        	}

//        	if (normal_text_color==-1) normal_text_color=holder.tv_name.getCurrentTextColor();
//        	Log.v("","n="+String.format("0x%08x",holder.tv_name.getCurrentTextColor()));
        	v.setTag(holder); 
        } else {
     	   holder= (ViewHolder)v.getTag();
        }
        v.setEnabled(true);
        final TreeFilelistItem o = mDataItems.get(position);
        if (o != null) {
        	holder.file_name.setText(o.getName());
        	holder.directory_name.setText(o.getPath());
        	String[] cap1 = new String[3];
        	holder.file_size.setText(String.format("%1$,3d", o.getLength())+ "Byte, ");
        	String comp_method="";
			if (o.getZipFileCompressionMethod()==Zip4jConstants.COMP_DEFLATE) comp_method="DEFLATE, "; 
			else if (o.getZipFileCompressionMethod()==Zip4jConstants.COMP_STORE) comp_method="STORE, ";
			else if (o.getZipFileCompressionMethod()==Zip4jConstants.COMP_AES_ENC) comp_method="AES, ";
			else comp_method+=o.getZipFileCompressionMethod()+", ";
			holder.comp_method.setText(comp_method);

        	holder.file_date.setText(o.getFileLastModDate());
        	holder.file_time.setText(o.getFileLastModTime());

        }
        return v;
	}

	class ViewHolder {
		public LinearLayout result_view;
		public TextView file_name, directory_name, file_size, file_time, file_date, comp_method;
	}
}
