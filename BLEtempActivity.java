package com.example.bletempget;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;
/*
 * This has been written to receive and send data from and to a BBC micro:bit
 * Several things need to be in place for this to work
 * the mac address of the micro:bit needs correcting to the one being used
 * the micro:bit must have Temperature Service activated
 * the android device must have Bluetooth LE and it must be turned on
 * the micro:bit should be paired/bonded to the android device
 * the Manifest must include permissions for bluetooth
 * e.g.
 *  <uses-permission android:name="android.permission.BLUETOOTH"/>
 *  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 *  <uses-feature android:name="android.hardware.bluetooth_le"  android:required="true" />
 *  
 * There aren't many error traps so it may crash or hang leaving you to work out why
 * Information about bluetooth on the micro:bit can be found at 
 * lancaster-university.github.io/microbit-docs/ble/profile
 * or
 * lancaster-university.github.io/microbit-docs/resources/bluetooth/bluetooth_profile.html 
 */

public class BLEtempActivity extends Activity {
	BluetoothManager btManager;
	BluetoothAdapter btAdapter;
	BluetoothGattService myService;
	BluetoothGattCharacteristic myStic;
	BluetoothGattCharacteristic myStic2;
	BluetoothDevice myDevice;
	BluetoothGatt myGatt;	
	BluetoothGattDescriptor myDesc;
	String myData;
	TextView myTextView;
	TextView myTextView2;
	int myStatus;
	int myTemp;
	int myState;
	int interval;
	int ms;
	int ls;	
	boolean first;
	byte[] myVal;
	UUID myUuidS = UUID.fromString("e95d6100-251d-470a-a062-fa1922dfa9a8");//temperature service
	UUID myUuidC = UUID.fromString("e95d9250-251d-470a-a062-fa1922dfa9a8");//temperature read
	UUID myUuidC2 = UUID.fromString("e95d1b25-251d-470a-a062-fa1922dfa9a8");//interval read or write
	UUID configD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bletemp);
		myTextView = (TextView) findViewById(R.id.textView1);		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			myTextView.setText("\nBlueTooth LE not supported\n");
			final Handler handler2 = new Handler(); 
			Timer t = new Timer(); 				   
			t.schedule(new TimerTask() { 
				public void run() { 
					handler2.post(new Runnable() { 
						public void run() {						
							finish();
							} }); 
				} 
			},  10000 ); 
		} else {
				
		btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();	
		if (!btAdapter.isEnabled()){
			myTextView.setText("\nBlueTooth not turned on\n");
		} else {
		//BluetoothDevice myDevice = btAdapter.getRemoteDevice("D0:1D:2E:AD:61:E8");
		//This needs changing to the Mac address of your micro:Bit V v V v V v V
		BluetoothDevice myDevice = btAdapter.getRemoteDevice("CA:B9:37:94:F4:52");
		try {
			myGatt = myDevice.connectGatt(this, true, myGattCallback);
			myData = myDevice.getName();
		} catch (IllegalArgumentException exception){
			Toast.makeText(getApplicationContext(), "Crashed by  " + exception, Toast.LENGTH_LONG).show();
		}
		myState = myDevice.getBondState();
		if (myState == BluetoothDevice.BOND_BONDED) {
		myTextView.append("\nBonded To " + myData +"\n");
		} else {
			myTextView.append("\nNot Bonded To " + myData +"\n");
		}
		}
		}
	}
	
	private Handler aHandler = new Handler(){
		@Override
		public void handleMessage (Message mess){
			BluetoothGattCharacteristic stic;
			switch (mess.what) {
			case 1: //discover services
				myGatt.discoverServices();
				break;
				
			case 2: //read characteristic
				stic = (BluetoothGattCharacteristic) mess.obj;
				if (myUuidC.equals(stic.getUuid())){
					myData = stic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0).toString();
				} else {
					myData = stic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0).toString();
				}
				myTextView.append("my handled SticRead data is " + myData + "\n");				
				break;
				
			case 3: //write descriptor				
				myDesc = myStic.getDescriptor(configD);				
				if (myDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
					myTextView.append("Handled Value was set\n");
				};	
				if (myGatt.writeDescriptor(myDesc)) {
					myTextView.append("Handled Descriptor written\n");	
				} else {
					myTextView.append("Handled Descriptor was not written\n");
					final Handler handler3 = new Handler(); 
					Timer t = new Timer(); 				   
					t.schedule(new TimerTask() { 
						public void run() { 
							handler3.post(new Runnable() { 
								public void run() {						
									tryAgain();
									} }); 
						} 
					},  5000 ); 
					
				}				
				break;
			
			case 4: //read the interval value
				if(myGatt.readCharacteristic(myStic2)){			
					myTextView.append("Temperature reading is sent every " + myStic2.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0)+ " milliseconds\n");				
			} else {				
				myTextView.append("myStic2 handled Value not read \n");				
			};
				break;
				
			case 5: //read descriptor value
				if(myGatt.readDescriptor(myDesc)) {
					myTextView.append("myDesc descriptor read \n");
				} else {
					myTextView.append("myDesc descriptor not read \n");
				};
				break;
				
			case 6:  //add text to textView1
				myTextView.append((CharSequence) mess.obj);
				if (myTextView.getLineCount()>30) myTextView.setText("Hello again world!\n");
					
			}
		}
	};
	
	private final BluetoothGattCallback myGattCallback = new BluetoothGattCallback(){
		public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
			
			switch (newState) {
			case 0:
				aHandler.sendMessage(Message.obtain(null,6,"device has been disconnected\n"));//add text to textView1
				
				break;
			case 2:
				aHandler.sendMessage(Message.obtain(null,6,"device is now connected\n"));//add text to textView1				
				aHandler.sendEmptyMessageDelayed(1, 1000); //discover services				
				break;
			default:
				aHandler.sendMessage(Message.obtain(null,6,"Unknown state\n"));//add text to textView1
				
				break;
			}
		}
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic stic) {
			
			aHandler.sendMessage(Message.obtain(null,6,"\nDevice Temperature is " + myStic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8,0) + "\n"));//add text to textView1
			aHandler.sendMessage(Message.obtain(null,2,stic)); //read characteristic					     
		} 
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {			
			
			myGatt = gatt;
			myService = gatt.getService(myUuidS);
			aHandler.sendMessage(Message.obtain(null,6,"device services have been discovered\n"));//add text to textView1			
			//if the service isn't available on the micro:Bit the app will hang
			if (myService != null){
				myStic = myService.getCharacteristic(myUuidC);
				myStic2 = myService.getCharacteristic(myUuidC2);
				} else {
					myTextView.append("myService = null\n");
				};
			
			byte[] sentData = new byte[2];
			interval = 30000;
			ls = interval%256;
			ms = (interval - ls)/256;
			sentData[0] = (byte) ls;
			sentData[1] = (byte) ms;
			
			if(myStic2.setValue(sentData)) {
				aHandler.sendMessage(Message.obtain(null,6,"Interval Value Set\n"));//add text to textView1						
					};

			if(myGatt.writeCharacteristic(myStic2)) {
				aHandler.sendMessage(Message.obtain(null,6,"Characteristic written\n"));//add text to textView1
				
			} else {
				aHandler.sendMessage(Message.obtain(null,6,"Characteristic not written\n"));//add text to textView1
				
			};
			
			if (myGatt.setCharacteristicNotification(myStic,true)){
				aHandler.sendMessage(Message.obtain(null,6,"myStic notifications enabled\n"));//add text to textView1
				
			} else {
				aHandler.sendMessage(Message.obtain(null,6,"myStic notifications not enabled\n"));//add text to textView1				
			}; 
			if (myGatt.setCharacteristicNotification(myStic2,true)){
				aHandler.sendMessage(Message.obtain(null,6,"myStic2 notifications enabled\n"));//add text to textView1				
			};
			
			aHandler.sendMessage(Message.obtain(null,6,"device id is " + myGatt.getDevice() + "\n"));//add text to textView1
			aHandler.sendEmptyMessage(3); //write descriptor
			aHandler.sendEmptyMessageDelayed(4, 2000); //read the interval value
			aHandler.sendEmptyMessageDelayed(5, 3000); //read descriptor value			
		}
		
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic stic, int status) {			
			aHandler.sendMessage(Message.obtain(null,2,stic)); //read characteristic						
		}
		
		@Override 
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { 			
			myStatus = status;	
			aHandler.sendMessage(Message.obtain(null,6,"SticWrite status is  " + myStatus + "\n"));//add text to textView1			
		} 

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			aHandler.sendMessage(Message.obtain(null,6,"Descriptor was written\n"));//add text to textView1				
		} 

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { 
			myStatus = status;
			final byte[] myVal = descriptor.getValue();	
			aHandler.sendMessage(Message.obtain(null,6,"Descriptor was read = " + myVal[0]+ " " + myVal[1] +" length = " + myVal.length +"\n"));//add text to textView1		
		} 
	};

	// end of myGattCallback  
	
public void tryAgain(){			
	myDesc = myStic.getDescriptor(configD);		
		if (myDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)){
			myTextView.append("Value was set\n");
		};	
		
		if (myGatt.writeDescriptor(myDesc)) {
					myTextView.append("Descriptor written at second attempt\n");	
				} else {
					myTextView.append("Descriptor was not written\n");
					final Handler handler2 = new Handler(); 
					Timer t = new Timer(); 				   
					t.schedule(new TimerTask() { 
						public void run() { 
							handler2.post(new Runnable() { 
								public void run() {						
									myGatt.writeDescriptor(myDesc);									
									myTextView.append("Timer Task was done\nWas Descriptor written at third attempt?\n");							
								} }); 
						} 
					},  8000 ); 
					
				};
				
		Toast.makeText(getApplicationContext(), "trying again",	Toast.LENGTH_LONG).show();
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (myGatt != null) {
			myGatt.disconnect();
			myGatt.close();
			myGatt = null;
		}
	}
}
/*micro:bit LED Service (e95dd91d-251d-470a-a062-fa1922dfa9a8)
 * Characteristics
- micro:bit LED Matrix State [R W] (e95d7b77-251d-470a-a062-fa1922dfa9a8)
- micro:bit LED Text [W] (e95d93ee-251d-470a-a062-fa1922dfa9a8)
- micro:bit Scrolling Delay [R W] (e95d0d2d-251d-470a-a062-fa1922dfa9a8)
Nordic UART Service (6e400001-b5a3-f393-e0a9-e50e24dcca9e)
- RX Characteristic [I] (6e400002-b5a3-f393-e0a9-e50e24dcca9e)
   			Client Characteristic Configuration (0x2902)
- TX Characteristic [W WNR] (6e400003-b5a3-f393-e0a9-e50e24dcca9e)
Temperature Service (e95d6100-251d-470a-a062-fa1922dfa9a8)
- Temperature [N R] (e95d9250-251d-470a-a062-fa1922dfa9a8)
Client Characteristic Configuration (0x2902)
Temperature Period [R W] (e95d1b25-251d-470a-a062-fa1922dfa9a8)
 */
