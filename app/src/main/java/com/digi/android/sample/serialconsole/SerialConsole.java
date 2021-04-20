/*
 * Copyright (c) 2014-2021, Digi International Inc. <support@digi.com>
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

package com.digi.android.sample.serialconsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.TooManyListenersException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import com.digi.android.serial.ISerialPortEventListener;
import com.digi.android.serial.NoSuchPortException;
import com.digi.android.serial.PortInUseException;
import com.digi.android.serial.SerialPort;
import com.digi.android.serial.SerialPortEvent;
import com.digi.android.serial.SerialPortManager;
import com.digi.android.serial.UnsupportedCommOperationException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Serial Console sample application.
 *
 * <p>This example opens a bi-directional Serial Port connection with a set
 * of configurable parameters using the Serial Port API.</p>
 *
 * <p>For a complete description on the example, refer to the 'README.md' file
 * included in the example directory.</p>
 */
public class SerialConsole extends Activity implements ISerialPortEventListener {
	
	// Constants.
	private static final int DATA_RECEIVED = 0;
	private static final int BACKSPACE_RECEIVED = 1;
	private static final int UPDATE_STATUS_TEXT = 2;
	private static final int SHOW_TOAST = 3;
	private static final int DISABLE_SEND = 4;
	private static final int ENABLE_SEND = 5;
	private static final int TOGGLE_CONNECT_BUTTON = 6;
	private static final int PREFERENCES_ACTIVITY_ID = 1337;
	
	private static final String NEW_LINE = "\r\n";
	private static final String CONNECTION_STATUS = "connection_status";
	private static final String CONSOLE_TEXT = "console_text";
	private static final String TEXT_TO_SEND = "text_to_send";

	// Variables.

	// UI Elements.
	private TextView console;
	private TextView statusText;
	
	private EditText inputText;
	
	private ScrollView scroll;
	
	private Button sendButton;

	private ImageButton connectButton;
	
	// Variables.
	private int baudRate;
	private int dataBits;
	private int stopBits;
	private int parity;
	private int flowControl;
	
	private boolean connected = false;
	private boolean echoEnabled = false;
	
	private InputStream is;
	
	private OutputStream os;

	private SerialPortManager manager;

	private SerialPort serialPort;
	
	private String port;

	private final IncomingHandler handler = new IncomingHandler(this);

	/**
	 * Handler to manage UI calls from different threads.
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<SerialConsole> wActivity;

		IncomingHandler(SerialConsole activity) {
			wActivity = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			SerialConsole serialConsole = wActivity.get();

			if (serialConsole == null)
				return;

			switch (msg.what) {
				case DATA_RECEIVED:
					String message = serialConsole.processBuffer((byte[]) msg.obj);
					serialConsole.console.append(message);
					serialConsole.scroll.fullScroll(View.FOCUS_DOWN);
					break;
				case BACKSPACE_RECEIVED:
					String text = serialConsole.console.getText().toString();
					if (text.length() > 0)
						serialConsole.console.setText(text.substring(0, text.length() - 1));
					serialConsole.scroll.fullScroll(View.FOCUS_DOWN);
					break;
				case UPDATE_STATUS_TEXT:
					serialConsole.updateStatusMessage();
					break;
				case SHOW_TOAST:
					Toast.makeText(serialConsole.getBaseContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
					break;
				case DISABLE_SEND:
					serialConsole.sendButton.setEnabled(false);
					break;
				case ENABLE_SEND:
					serialConsole.sendButton.setEnabled(true);
					break;
				case TOGGLE_CONNECT_BUTTON:
					serialConsole.toggleConnectButton();
					break;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);

		manager = new SerialPortManager(this);
		
		initializeUI();
		if (savedInstanceState == null)
			openPreferencesActivity();
		else
			restoreStatus(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		closeConnection();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.menu_console, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Connect button status is lost after executing another activity,
		// restore it.
		handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
		int itemId = item.getItemId();
		if (itemId == R.id.menu_option_close_terminal) {
			finish();
			return true;
		} else if (itemId == R.id.menu_option_clear_terminal) {
			clearConsole();
			return true;
		} else if (itemId == R.id.menu_option_settings_terminal) {
			openPreferencesActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void serialEvent(SerialPortEvent ev) {
		String message = null;
		switch (ev.getEventType()) {
			case BI:
				message = "Break interrupt received.";
				break;
			case CD:
				closeConnection();
				message = "Carrier detect received.";
				break;
			case CTS:
				message = "CTS line activated.";
				break;
			case DSR:
				message = "DSR line activated.";
				break;
			case RI:
				message = "Ring indicator received.";
				break;
			case FE:
				message = "Received framing error.";
				break;
			case PE:
				message = "Received parity error.";
				break;
			case OE:
				closeConnection();
				message = "Connection Closed: buffer overrun error.";
				break;
			case DATA_AVAILABLE:
				try {
					if (is.available() > 0)
						readData();
				} catch (Exception e) {
					// This only happens with USB connections but just in case.
					closeConnection();
					message = "Connection Closed: Connection closed in the other side.";
				}
				break;
		}
		if (message != null) {
			Message msg = new Message();
			msg.what = SHOW_TOAST;
			msg.obj = message;
			handler.sendMessage(msg);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != PREFERENCES_ACTIVITY_ID)
			return;

		if (loadPreferences()) {
			if (connected)
				closeConnection();
			openConnection();
		}

		// Connect button status is lost after executing another activity,
		// restore it.
		handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
		handler.sendEmptyMessage(UPDATE_STATUS_TEXT);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(CONNECTION_STATUS, connected);
		outState.putString(CONSOLE_TEXT, console.getText().toString());
		outState.putString(TEXT_TO_SEND, inputText.getText().toString());
	}
	
	/**
	 * Initializes all graphic UI elements and sets the required listeners.
	 */
	private void initializeUI() {
		console = findViewById(R.id.console_terminal);
		statusText = findViewById(R.id.status_text);
		scroll = findViewById(R.id.scroll_console_terminal);
		inputText = findViewById(R.id.input_text);

		sendButton = findViewById(R.id.send_terminal_button);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				writeData();
			}
		});

		ImageButton settingsButton = findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				openPreferencesActivity();
			}
		});

		ImageButton clearConsoleButton = findViewById(R.id.clear_console_button);
		clearConsoleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				clearConsole();
			}
		});

		ImageButton clearInputButton = findViewById(R.id.clear_send_text_button);
		clearInputButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				clearInputText();
			}
		});

		connectButton = findViewById(R.id.connect_button);
		connectButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_UP:
						if (connected)
							closeConnection();
						else
							openConnection();
						handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
						break;
					case MotionEvent.ACTION_DOWN:
						connectButton.setPressed(true);
						break;
				}
				return true;
			}
		});
	}
	
	/**
	 * Initializes serial port.
	 * 
	 * @return {@code true} if success, {@code false} otherwise.
	 */
	private boolean initializeSerialPort() {
		String error = "Connection Error: ";
		try {
			serialPort = manager.openSerialPort(port, baudRate, dataBits, stopBits, parity, flowControl, 3000);
			serialPort.registerEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			serialPort.notifyOnCTS(true);
			serialPort.notifyOnDSR(true);
			serialPort.notifyOnRingIndicator(true);
			serialPort.notifyOnBreakInterrupt(true);
			serialPort.notifyOnCarrierDetect(true);
			serialPort.notifyOnFramingError(true);
			serialPort.notifyOnOverrunError(true);
			serialPort.notifyOnParityError(true);
			serialPort.setPortParameters(baudRate, dataBits, stopBits, parity, flowControl);
			is = serialPort.getInputStream();
			os = serialPort.getOutputStream();
			return true;
		} catch (NoSuchPortException e) {
			e.printStackTrace();
			error += "Port " + port + " does not exist.";
		} catch (PortInUseException e) {
			e.printStackTrace();
			error += "Port " + port + " is in use.";
		} catch (TooManyListenersException e) {
			e.printStackTrace();
			error += "Could not add serial port listener.";
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
			error += "Could not set serial port parameters.";
		} catch (IOException e) {
			e.printStackTrace();
			error += "Could not declare serial port streams.";
		}
		Message msg = new Message();
		msg.what = SHOW_TOAST;
		msg.obj = error;
		handler.sendMessage(msg);
		return false;
	}

	/**
	 * Reads data from the serial port and prints it in the console.
	 */
	private void readData() {
		try {
			int available = is.available();
			if (available > 0) {
				byte[] readBuffer = new byte[available];
				int numBytes = is.read(readBuffer, 0, available);
				if (numBytes <= 0)
					return;
				if (echoEnabled)
					sendEcho(readBuffer);
				Message message = new Message();
				message.obj = readBuffer;
				message.what = DATA_RECEIVED;
				handler.sendMessage(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends echo back to the serial port. Buffer is parsed looking
	 * for alone Carrier Return bytes in order to insert Line Feed bytes.
	 * 
	 * @param buffer Byte buffer to send using echo.
	 */
	private void sendEcho(byte[] buffer) {
		try {
			boolean previousWasCR = false;
			for (byte readByte:buffer) {
				if (os == null)
					break;
				// Insert Line Feed line for alone Carrier Return bytes.
				if (previousWasCR && readByte != (byte)10) {
					os.write((byte)10);
					previousWasCR = false;
				}
				os.write(readByte);
				if (readByte == (byte)13)
					previousWasCR = true;
				else if (readByte == (byte)10)
					previousWasCR = false;
			}
			// Check if last byte in buffer was Carrier Return
			// and if so append Line Feed.
			if (previousWasCR && os != null)
				os.write((byte)10);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Process the given byte array returning a readable string. Alone
	 * Carrier Return bytes are replaced by Carrier Return + Line Feed.
	 * 
	 * @param buffer Byte buffer to process.
	 *
	 * @return Resulting readable string.
	 */
	private String processBuffer(byte[] buffer) {
		// Basic processing for new line, carriage return, backspace,
		// tab and normal chars.
		StringBuilder sb = new StringBuilder();
		boolean previousWasCR = false;
		for (byte readByte:buffer) {
			// Insert Line Feed byte for alone Carrier Return bytes.
			if ((readByte != (byte)10) && previousWasCR) {
				sb.append("\n");
				previousWasCR = false;
			}
			if (readByte > 31) // Print readable characters.
				sb.append((char)readByte);
			else if (readByte == (byte)9) // Replace tab characters.
				sb.append("	");
			else if (readByte == (byte)10) { // Insert line feed.
				sb.append("\n");
				previousWasCR = false;
			} else if (readByte == (byte)13) { // Insert carrier return.
				sb.append("\r");
				previousWasCR = true;
			} else if (readByte == (byte)8) { // Perform backspace.
				if (sb.length() == 0)
					handler.sendEmptyMessage(BACKSPACE_RECEIVED);
				else
					sb.deleteCharAt(sb.length() - 1);
			}
		}
		// Check if last byte in buffer was Carrier Return and if so append
		// Line Feed.
		if (previousWasCR)
			sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Writes the data from the input text field to the serial port.
	 */
	public void writeData() {
		if (inputText.getText().toString().equals("") || serialPort == null || os == null)
			return;
		try {
			os.write((inputText.getText().toString().replace("\n", "\r\n") + NEW_LINE).getBytes());
			os.write((byte)13);
			clearInputText();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the serial console preferences and stores them in the application 
	 * instance. Returns true if connection settings have changed.
	 * 
	 * @return {@code true} if connection settings have changed,
	 *         {@code false} otherwise.
	 */
	private boolean loadPreferences() {
		boolean changed = false;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String newPort = preferences.getString(getString(R.string.serial_port_key), null);
		if (port == null || !port.equals(newPort)) {
			port = newPort;
			changed = true;
		}

		String sBaudRate = preferences.getString(getString(R.string.baud_rate_key), null);
		if (sBaudRate != null) {
			int newBaudRate = Integer.parseInt(sBaudRate);
			if (baudRate != newBaudRate) {
				baudRate = newBaudRate;
				changed = true;
			}
		}

		String sDataBits = preferences.getString(getString(R.string.data_bits_key), null);
		if (sDataBits != null) {
			int newDataBits = Integer.parseInt(sDataBits);
			if (dataBits != newDataBits) {
				dataBits = newDataBits;
				changed = true;
			}
		}

		String sStopBits = preferences.getString(getString(R.string.stop_bits_key), null);
		if (sStopBits != null) {
			int newStopBits = Integer.parseInt(sStopBits);
			if (stopBits != newStopBits) {
				stopBits = newStopBits;
				changed = true;
			}
		}

		String sParity = preferences.getString(getString(R.string.parity_key), null);
		if (sParity != null) {
			int newParity = Integer.parseInt(sParity);
			if (parity != newParity) {
				parity = newParity;
				changed = true;
			}
		}

		String sFlowControl = preferences.getString(getString(R.string.flow_control_key), null);
		if (sFlowControl != null) {
			int newFlowControl = Integer.parseInt(sFlowControl);
			if (flowControl != newFlowControl) {
				flowControl = newFlowControl;
				changed = true;
			}
		}

		echoEnabled = preferences.getBoolean(getString(R.string.echo_key), false);
		return changed;
	}

	/**
	 * Closes serial port connection.
	 */
	private void closeConnection() {
		connected = false;
		handler.sendEmptyMessage(DISABLE_SEND);
		handler.sendEmptyMessage(UPDATE_STATUS_TEXT);
		handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
		if (serialPort != null)
			serialPort.unregisterEventListener();
		if (serialPort != null)
			serialPort.close();
		try {
			if (os != null)
				os.close();
			if (is != null)
				is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		serialPort = null;
		os = null;
		is = null;
	}
	
	/**
	 * Opens serial port connection.
	 */
	private void openConnection() {
		if (!initializeSerialPort())
			return;
		clearConsole();
		connected = true;
		handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
		handler.sendEmptyMessage(UPDATE_STATUS_TEXT);
		handler.sendEmptyMessage(ENABLE_SEND);
	}

	/**
	 * Clears the contents of the serial console.
	 */
	public void clearConsole() {
		console.setText("");
	}
	
	/**
	 * Clears the text to be sent in the input text control.
	 */
	public void clearInputText() {
		inputText.setText("");
	}

	/**
	 * Starts the serial console preferences activity.
	 */
	public void openPreferencesActivity() {
		startActivityForResult(new Intent(this, Preferences.class), PREFERENCES_ACTIVITY_ID);
	}
	
	/**
	 * Updates serial console status text.
	 */
	private void updateStatusMessage() {
		String text;
		if (connected)
			text = "PORT OPEN @ ";
		else
			text = "PORT CLOSED @ ";
		text += port + " - PARAMS: "
				+ baudRate + ", "
				+ dataBits + ", "
				+ stopBits + ", "
				+ getParityString() + ", "
				+ getFlowControlString()
				+ " - Echo ";
		if (echoEnabled)
			text += "enabled";
		else
			text += "disabled";
		statusText.setText(text);
		if (connected)
			statusText.setTextColor(getResources().getColor(R.color.light_green));
		else
			statusText.setTextColor(getResources().getColor(R.color.light_red));
	}
	
	/**
	 * Toggles connect button changing its icon and background.
	 */
	private void toggleConnectButton() {
		if (connected) {
			connectButton.setImageResource(R.drawable.port_connected);
			connectButton.setPressed(true);
		} else {
			connectButton.setImageResource(R.drawable.port_disconnected);
			connectButton.setPressed(false);
		}
	}
	
	/**
	 * Restores the application status with the given saved values.
	 * 
	 * @param status Bundle containing application saved status.
	 */
	private void restoreStatus(Bundle status) {
		loadPreferences();
		connected = status.getBoolean(CONNECTION_STATUS);
		String consoleText = status.getString(CONSOLE_TEXT);
		String textToSend = status.getString(TEXT_TO_SEND);
		console.setText(consoleText);
		inputText.setText(textToSend);
		if (connected)
			openConnection();
		else
			handler.sendEmptyMessage(DISABLE_SEND);
		handler.sendEmptyMessage(UPDATE_STATUS_TEXT);
		handler.sendEmptyMessage(TOGGLE_CONNECT_BUTTON);
	}
	
	/**
	 * Retrieves the parity readable string based in its value.
	 * 
	 * @return String with readable parity value.
	 */
	private String getParityString() {
		String parityString = "";
		switch (parity) {
		case SerialPort.PARITY_EVEN:
			parityString = "Even";
			break;
		case SerialPort.PARITY_MARK:
			parityString = "Mark";
			break;
		case SerialPort.PARITY_NONE:
			parityString = "None";
			break;
		case SerialPort.PARITY_ODD:
			parityString = "Odd";
			break;
		case SerialPort.PARITY_SPACE:
			parityString = "Space";
			break;
		}
		return parityString;
	}
	
	/**
	 * Retrieves the flow control readable string based in its value.
	 * 
	 * @return String with readable flow control value.
	 */
	private String getFlowControlString() {
		String flowControlString = "";
		switch (flowControl) {
		case SerialPort.FLOWCONTROL_NONE:
			flowControlString = "None";
			break;
		case SerialPort.FLOWCONTROL_RTSCTS_IN:
			flowControlString = "RTS/CTS In";
			break;
		case SerialPort.FLOWCONTROL_RTSCTS_OUT:
			flowControlString = "RTS/CTS Out";
			break;
		case SerialPort.FLOWCONTROL_XONXOFF_IN:
			flowControlString = "XON/XOFF In";
			break;
		case SerialPort.FLOWCONTROL_XONXOFF_OUT:
			flowControlString = "XON/XOFF Out";
			break;
		}
		return flowControlString;
	}
}
