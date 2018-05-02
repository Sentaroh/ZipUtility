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

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;

import com.sentaroh.android.Utilities.LocalMountPoint;

@SuppressWarnings("unused")
public class ActivitySettings extends PreferenceActivity{
	private static Context mContext=null;
	private static PreferenceFragment mPrefFrag=null;

	private static ActivitySettings mPrefActivity=null;

	private static GlobalParameters mGp=null;

	private CommonUtilities mUtil=null;

//	private GlobalParameters mGp=null;

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext=this;
		mGp=(GlobalParameters)getApplicationContext();
		setTheme(mGp.applicationTheme);
		super.onCreate(savedInstanceState);
		mPrefActivity=this;
		if (mUtil==null) mUtil=new CommonUtilities(this, "SettingsActivity", mGp);
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
		if (mGp.settingFixDeviceOrientationToPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void onStart(){
		super.onStart();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	@Override
	public void onResume(){
		super.onResume();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
//		setTitle(R.string.settings_main_title);
	};

	@Override
	public void onBuildHeaders(List<Header> target) {
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
		loadHeadersFromResource(R.xml.settings_frag, target);
	};

	@Override
	public boolean onIsMultiPane () {
		mContext=this;
		mGp=(GlobalParameters)getApplication();
//    	mPrefActivity=this;
		mUtil=new CommonUtilities(this, "SettingsActivity", mGp);
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
		return true;
	};

	@Override
	protected void onPause() {
		super.onPause();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	private static void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string) {
		if (!checkCompressSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
			if (!checkUiSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
				if (!checkLogSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
					if (!checkMiscSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
						checkOtherSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext);
	};

	private static boolean checkCompressSettings(CommonUtilities ut, Preference pref_key,
												 SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;

		if (key_string.equals(c.getString(R.string.settings_no_compress_file_type))) {
			isChecked=true;
			pref_key.setSummary(shared_pref.getString(key_string, ""));
		} else if (key_string.equals(c.getString(R.string.settings_zip_default_encoding))) {
			isChecked=true;
			pref_key.setSummary(shared_pref.getString(key_string, ""));
		}
		return isChecked;
	};

	private static boolean checkUiSettings(CommonUtilities ut, Preference pref_key,
										   SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;

		if (key_string.equals(c.getString(R.string.settings_use_light_theme))) {
			isChecked=true;
		} else if (key_string.equals(c.getString(R.string.settings_device_orientation_portrait))) {
			isChecked=true;
//			boolean orientation=shared_pref.getBoolean(key_string,false);
//            if (orientation) mPrefActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            else mPrefActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

		return isChecked;
	};

	private static boolean checkMiscSettings(CommonUtilities ut,
											 Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;

		if (key_string.equals(c.getString(R.string.settings_exit_clean))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, true)) {
				pref_key
						.setSummary(c.getString(R.string.settings_exit_clean_summary_ena));
			} else {
				pref_key
						.setSummary(c.getString(R.string.settings_exit_clean_summary_dis));
			}
		}

		return isChecked;
	};

	private static boolean checkLogSettings(CommonUtilities ut,
											Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;

		if (key_string.equals(c.getString(R.string.settings_log_option))) {
			isChecked=true;
		} else if (key_string.equals(c.getString(R.string.settings_put_logcat_option))) {
			isChecked=true;
		} else if (key_string.equals(c.getString(R.string.settings_log_level))) {
			isChecked=true;
			String[] wl_label= c.getResources().getStringArray(R.array.settings_log_level_list_entries);
			String sum_msg=wl_label[Integer.parseInt(shared_pref.getString(key_string, "0"))];
			pref_key.setSummary(sum_msg);
		} else if (key_string.equals(c.getString(R.string.settings_log_file_max_count))) {
			isChecked=true;
			pref_key.setSummary(String.format(c.getString(R.string.settings_log_file_max_count_summary),
					shared_pref.getString(key_string, "10")));
		}

		return isChecked;
	};

	private static boolean checkOtherSettings(CommonUtilities ut,
											  Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = true;
		if (pref_key!=null) {
			pref_key.setSummary(
					c.getString(R.string.settings_default_current_setting)+
							shared_pref.getString(key_string, "0"));
		} else {
			if (mGp.settingDebugLevel>0) ut.addDebugMsg(1, "I", "checkOtherSettings Key not found key="+key_string);
		}
		return isChecked;
	};

	public static class SettingsLog extends PreferenceFragment {
		private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
				new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
						checkSettingValue(mUtil, shared_pref, key_string);
					}
				};
		private CommonUtilities mUtil=null;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mPrefFrag=this;
			mUtil=new CommonUtilities(mContext, "SettingsLog", mGp);
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");

			addPreferencesFromResource(R.xml.settings_frag_log);

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

			if (!LocalMountPoint.isExternalStorageAvailable()) {
				findPreference(getString(R.string.settings_log_dir))
						.setEnabled(false);
			}

			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_option));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_put_logcat_option));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_file_max_count));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_dir));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_level));
		};

		@Override
		public void onStart() {
			super.onStart();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listenerAfterHc);
			getActivity().setTitle(R.string.settings_log_title);
		};
		@Override
		public void onStop() {
			super.onStop();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
		};
	};

	public static class SettingsMisc extends PreferenceFragment {
		private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
				new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
						checkSettingValue(mUtil, shared_pref, key_string);
					}
				};
		private CommonUtilities mUtil=null;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mPrefFrag=this;
			mUtil=new CommonUtilities(mContext, "SettingsMisc", mGp);
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");

			addPreferencesFromResource(R.xml.settings_frag_misc);

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

			shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean),true).commit();
			findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_exit_clean));
		};

		@Override
		public void onStart() {
			super.onStart();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listenerAfterHc);
			getActivity().setTitle(R.string.settings_misc_title);
		};
		@Override
		public void onStop() {
			super.onStop();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
		};
	};

	public static class SettingsCompress extends PreferenceFragment {
		private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
				new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
						checkSettingValue(mUtil, shared_pref, key_string);
					}
				};
		private CommonUtilities mUtil=null;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mPrefFrag=this;
			mUtil=new CommonUtilities(mContext, "SettingsCompress", mGp);
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");

			addPreferencesFromResource(R.xml.settings_frag_compress);

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_no_compress_file_type));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_zip_default_encoding));

		};

		@Override
		public void onStart() {
			super.onStart();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listenerAfterHc);
			getActivity().setTitle(R.string.settings_ui_title);
		};
		@Override
		public void onStop() {
			super.onStop();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
		};
	};

	public static class SettingsUi extends PreferenceFragment {
		private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =
				new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
						checkSettingValue(mUtil, shared_pref, key_string);
					}
				};
		private CommonUtilities mUtil=null;
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mPrefFrag=this;
			mUtil=new CommonUtilities(mContext, "SettingsUi", mGp);
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");

			addPreferencesFromResource(R.xml.settings_frag_ui);

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_use_light_theme));
			checkSettingValue(mUtil, shared_pref,getString(R.string.settings_device_orientation_portrait));

		};

		@Override
		public void onStart() {
			super.onStart();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listenerAfterHc);
			getActivity().setTitle(R.string.settings_ui_title);
		};
		@Override
		public void onStop() {
			super.onStop();
			if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);
		};
	};
}