/**
 * Copyright (c) 2014-2016, Digi International Inc. <support@digi.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.digi.android.serialconsole;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.gnu.io.CommPortIdentifier;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	// Constants.
	private final static int SERIAL_PORT_KEY = R.string.serial_port_key;
	private final static int BAUD_RATE_KEY = R.string.baud_rate_key;
	private final static int DATA_BITS_KEY = R.string.data_bits_key;
	private final static int STOP_BITS_KEY = R.string.stop_bits_key;
	private final static int PARITY_KEY = R.string.parity_key;
	private final static int FLOW_CONTROL_KEY = R.string.flow_control_key;
	
	// Variables.
	private static ArrayList<Integer> listPreferencesKeys = new ArrayList<Integer>();
	static {
		listPreferencesKeys.add(SERIAL_PORT_KEY);
		listPreferencesKeys.add(BAUD_RATE_KEY);
		listPreferencesKeys.add(DATA_BITS_KEY);
		listPreferencesKeys.add(STOP_BITS_KEY);
		listPreferencesKeys.add(PARITY_KEY);
		listPreferencesKeys.add(FLOW_CONTROL_KEY);
	}
	
	// Search for available ports and store them in static array.
	private static CharSequence[] portList;
	static {
		Enumeration<?> availablePorts = CommPortIdentifier.getPortIdentifiers();
		ArrayList<String> ports = new ArrayList<String>();
		while (availablePorts.hasMoreElements())
			ports.add(((CommPortIdentifier)availablePorts.nextElement()).getName());
		portList = new String[ports.size()];
		for (int i = 0; i< ports.size(); i++)
			portList[i] = ports.get(i);
	}
	
	private HashMap<String, ListPreference> listPreferences = new HashMap<String, ListPreference>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.preferences_dialog);

		setTitle(getString(R.string.serial_console_preferences));
		initializePreferences();

		Button closeButton = (Button)super.findViewById(R.id.close_button);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				closePressed();
			}
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		ListPreference listPref = listPreferences.get(key);
		if (listPref == null)
			return;
		listPref.setSummary(listPref.getEntry());
	}

	/**
	 * Initializes the preferences and summaries.
	 */
	private void initializePreferences() {
		SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		for (int prefKey:listPreferencesKeys) {
			ListPreference pref = (ListPreference)getPreferenceManager().findPreference(getString(prefKey));
			if (pref.getKey().equals(getString(SERIAL_PORT_KEY)))
				fillSerialPortPreference(pref);
			listPreferences.put(getString(prefKey), pref);  
			onSharedPreferenceChanged(sharedPrefs, getString(prefKey));  
		}
	}
	
	/**
	 * Handles what happens when close button is pressed.
	 */
	public void closePressed() {
		finish();
	}

	/**
	 * Fills serial port preference. Possible values are read from COM port
	 * identifiers and if setting has no default value or value they are also
	 * filled up.
	 * 
	 * @param pref Serial port preference.
	 */
	private void fillSerialPortPreference(ListPreference pref) {
		if (portList.length > 0)
			pref.setDefaultValue(portList[0]);
		if (pref.getValue() == null || pref.getValue().equals(""))
			pref.setValue(portList[0].toString());
		pref.setEntries(portList);
		pref.setEntryValues(portList);
	}
}
