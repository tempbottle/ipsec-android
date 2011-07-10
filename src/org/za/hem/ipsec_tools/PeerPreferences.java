package org.za.hem.ipsec_tools;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.lamerman.FileDialog;

// Order "public protected private static final transient volatile"

/**
 * @author mikael
 * IPsec peer preference activity
 */
public class PeerPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String PEER_ID = "PEER_ID";
	public static final String EXTRA_ID = "org.za.hem.ipsec_tools.ID";
	
	static final String TEMPLATE_PREFERENCE = "templatePref";
	static final String ID_PREFERENCE = "idPref";
	static final String NAME_PREFERENCE = "namePref";
	private static final String SDCARD_ROOT = "/sdcard";
	
	// FIXME
	private static final int REQUEST_SAVE = 1;
	private static final int REQUEST_LOAD = 2;
	
	private int mID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mID = getIntent().getIntExtra(EXTRA_ID, -1);
		if (mID < 0) {
			setResult(RESULT_FIRST_USER);
			finish();
		}
		
		PreferenceManager manager = getPreferenceManager();
		manager.setSharedPreferencesName(getSharedPreferencesName(this, mID));
		addPreferencesFromResource(R.xml.peer_preferences);

		// Get the template preference
		final Activity activity = this;
		
		// FIXME remove
		SharedPreferences idPreference = getSharedPreferences(
				getSharedPreferencesName(this, mID), Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = idPreference.edit();
		editor.putInt(ID_PREFERENCE, mID);
		editor.commit();

		Preference customPref = findPreference(TEMPLATE_PREFERENCE);
		
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(activity.getBaseContext(),
						FileDialog.class);
				intent.putExtra(FileDialog.START_PATH, SDCARD_ROOT);
				activity.startActivityForResult(intent, REQUEST_SAVE);
				return true;
			}
		});
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, final Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == REQUEST_SAVE) {
				System.out.println("Saving...");
			} else if (requestCode == REQUEST_LOAD) {
				System.out.println("Loading...");
			}
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);

			SharedPreferences templatePreference = getSharedPreferences(
					getSharedPreferencesName(this, mID), Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = templatePreference.edit();
			editor.putString(TEMPLATE_PREFERENCE, filePath);
			editor.commit();
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Logger.getLogger(PeerPreferences.class.getName()).log(
					Level.WARNING, "file not selected");
	    }

	}
	
	@Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        
        Map<String, ?> map = sharedPreferences.getAll();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext()){
        	String key = iter.next();
    		Preference pref= getPreferenceScreen().findPreference(key);
    		Object val = map.get(key);
    		UpdateSummary(pref, key, val);
        }
        
        // Set up a listener whenever a key changes            
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
	
	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes            
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref= getPreferenceScreen().findPreference(key);
		Map<String, ?> map = sharedPreferences.getAll();
		Object val = map.get(key);
		UpdateSummary(pref, key, val);
	}
    
    private void UpdateSummary(Preference pref, String key, Object val) {
    	if (pref instanceof EditTextPreference) {
			pref.setSummary("Text " + val);
		} else if (pref instanceof CheckBoxPreference) {
			pref.setSummary("Check " + val);
		} else if (pref instanceof ListPreference) {
			pref.setSummary("List " + val);
		} else if (key.equals(TEMPLATE_PREFERENCE)) {
			pref.setSummary("File " + val);
		}
    }
	
	public static String getSharedPreferencesName(Context context, int id) {
	    return context.getPackageName() + "_peer_" + id;
	}
}