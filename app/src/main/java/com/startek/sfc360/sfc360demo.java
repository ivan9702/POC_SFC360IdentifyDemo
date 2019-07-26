package com.startek.sfc360;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.startek.fingerprint.library.FP;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static java.lang.Integer.parseInt;
import static android.content.ContentValues.TAG;
public class sfc360demo extends Activity {

    final int U_LEFT = -41;
    final int U_RIGHT = -42;
    final int U_UP = -43;
    final int U_DOWN = -44;
    final int U_POSITION_CHECK_MASK = 0x00002F00;
    final int U_POSITION_NO_FP = 0x00002000;
    final int U_POSITION_TOO_LOW = 0x00000100;
    final int U_POSITION_TOO_TOP = 0x00000200;
    final int U_POSITION_TOO_RIGHT = 0x00000400;
    final int U_POSITION_TOO_LEFT = 0x00000800;
    final int U_POSITION_TOO_LOW_RIGHT = (U_POSITION_TOO_LOW | U_POSITION_TOO_RIGHT);
    final int U_POSITION_TOO_LOW_LEFT = (U_POSITION_TOO_LOW | U_POSITION_TOO_LEFT);
    final int U_POSITION_TOO_TOP_RIGHT = (U_POSITION_TOO_TOP | U_POSITION_TOO_RIGHT);
    final int U_POSITION_TOO_TOP_LEFT = (U_POSITION_TOO_TOP | U_POSITION_TOO_LEFT);

    final int U_POSITION_OK = 0x00000000;

    final int U_DENSITY_CHECK_MASK = 0x000000E0;
    final int U_DENSITY_TOO_DARK = 0x00000020;
    final int U_DENSITY_TOO_LIGHT = 0x00000040;
    final int U_DENSITY_LITTLE_LIGHT = 0x00000060;
    final int U_DENSITY_AMBIGUOUS = 0x00000080;

    final int U_INSUFFICIENT_FP = -31;
    final int U_NOT_YET = -32;

    final int U_CLASS_A = 65;
    final int U_CLASS_B = 66;
    final int U_CLASS_C = 67;
    final int U_CLASS_D = 68;
    final int U_CLASS_E = 69;
    final int U_CLASS_R = 82;
    static EditText editText;

    ///////////////////////// SharedPreferences SAVE　IP ADDRESS ////////////////////
    SharedPreferences sharedata;
    SharedPreferences.Editor editor;
    static String ip;
    static String port;
    private byte[] piv = new byte[16];
    private byte[] eskey = new byte[256];
    public String UI_message;
    public int UI_Code;
    public String UI_Score;
    public static Boolean UI_HTTPS_Enable = false;
    public String UI_UserID;
    public String UI_FPID;
    static int StatusCode=0;
    int RecoveryColor=0;
    //////////////////////////////////////////////////////////////////
    /**
     * Called when the activity is first created.
     */
    private TextView apk_Ver;
    private TextView theMessage;
    private Button buttonConnect;

    private Button buttonEnroll;
    private Button buttonIdentify;
    private Button buttonDisC;
    private int connectrtn;
    private int rtn;
    private int rtn2;
    private ImageView myImage;

    byte[] bMapArray = new byte[1078 + (640 * 480)];
    byte[] bISOImgArray = new byte[32 + 14  + (256 * 360)];
    private byte[] minu_code1 = new byte[512];
    private byte[] minu_code2 = new byte[512];

    byte[] srno= new byte[16];
    byte[] pak = new byte[16];
    byte[] fwver = new byte[16];
    byte[] Key2= new byte[16];
    byte[] newKey= new byte[16];

    private EventHandler m_eventHandler;
    private Bitmap bMap;

    private int counter = 0;

    private static Context Context;

    //public static final int UPDATE_TEXT_VIEW=0x0001;
////////holing add for usb host
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbManager manager;
    private PendingIntent mPermissionIntent;
    private UsbDevice d;
    private UsbDeviceConnection conn;
    private UsbInterface usbIf;
    UsbEndpoint epIN;
    UsbEndpoint epOUT;
    UsbEndpoint ep2IN;

    static String APK_Ver = "STARTEK  BIOSERVER SFC360 20190723";
    String FIRST_LINE="";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            theMessage.setText("Device is found and try to connect");
                            connectreader();
                        }
                    } else {
                        //      Log.d(TAG, "permission denied for device " + device);
                        theMessage.setText("Device is found");
                    }
                }
            }
        }
    };

    private void connectreader() {
        // TODO Auto-generated method stub
        if(RecoveryColor !=0 )
            theMessage.setTextColor(RecoveryColor);
        usbIf = d.getInterface(0);
        Log.d("Device", "Interface:-" + String.valueOf(usbIf.getEndpointCount()));
        Log.d("Device", "Interface Count: " + Integer.toString(d.getInterfaceCount()));

        Log.d("USB", String.valueOf(usbIf.getEndpointCount()));

        //    final UsbEndpoint  usbEndpoint = usbInterface.getEndpoint(0);

        epIN = null;
        epOUT = null;
        ep2IN = null;

        theMessage.setText("num of ep" + usbIf.getEndpointCount());

        epOUT = usbIf.getEndpoint(0);
        epIN = usbIf.getEndpoint(1);
        ep2IN = usbIf.getEndpoint(2);

        //	 theMessage.setText("ep num "+ ep2IN.getEndpointNumber()+"packet size "+ ep2IN.getMaxPacketSize()+"dir "+ep2IN.getDirection());
        //	 theMessage.setText("ep num "+ epIN.getEndpointNumber()+"packet size "+ epIN.getMaxPacketSize()+"dir "+epIN.getDirection());
        theMessage.setText("ep num " + epOUT.getEndpointNumber() + "packet size " + epOUT.getMaxPacketSize() + "dir " + epOUT.getDirection());

        //	 theMessage.setText("manager.hasPermission()");
        if (manager.hasPermission(d) == false) {
            //    	 theMessage.setText("manager.hasPermission() false");
            return;

        }

        conn = manager.openDevice(d);

        if (conn.getFileDescriptor() == -1) {
            Log.d("Device", "Fails to open DeviceConnection");
        } else {

            Log.d("Device", "Opened DeviceConnection" + Integer.toString(conn.getFileDescriptor()));
        }

        if (conn.releaseInterface(usbIf)) {
            Log.d("USB", "Released OK");
        } else {
            Log.d("USB", "Released fails");
        }

        if (conn.claimInterface(usbIf, true)) {
            Log.d("USB", "Claim OK");
        } else {
            Log.d("USB", "Claim fails");
        }
        //     theMessage.setText("EEPROM_read");
        //     byte [] buf= new byte [48];
        //     eeprom_read(0,48,buf);
        theMessage.setText("Device fileDesc" + conn.getFileDescriptor());

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Context = getApplicationContext();
        //SetLibraryPath(Context.getFilesDir().getPath());
        FP.SetFPLibraryPath("/data/data/com.startek.sfc360/lib/");
        FP.InitialSDK();

        sharedata = getSharedPreferences("IP_ADDR", MODE_PRIVATE);
        editor = sharedata.edit();
        ip=sharedata.getString("ip","0");
        if(RecoveryColor !=0 )
            theMessage.setTextColor(RecoveryColor);

        if(ip.equals("0"))
        {
            LayoutInflater layoutInflater = LayoutInflater.from(sfc360demo.this);
            View promptView = layoutInflater.inflate(R.layout.text_inpu_password, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(sfc360demo.this);
            alertDialogBuilder.setView(promptView);

            editText = (EditText) promptView.findViewById(R.id.edittext);
            ip = editText.getHint().toString().substring(0,editText.getHint().toString().indexOf(':'));
            editor.putString("ip",ip);
            port=editText.getHint().toString().substring(editText.getHint().toString().indexOf(':')+1);
            editor.putString("port",port);
            editor.commit();
            Log.d("onCreate", "IP0: "+sharedata.getString("ip","0"));
            Log.d("onCreate", "PORT0: "+sharedata.getString("port","0"));

        }
        else
        {
            ip=sharedata.getString("ip","0");
            port=sharedata.getString("port","0");
            Log.d("onCreate", "IP1: "+sharedata.getString("ip","0"));
            Log.d("onCreate", "PORT1: "+sharedata.getString("port","0"));
        }

        theMessage = (TextView) findViewById(R.id.message);
        apk_Ver = (TextView) findViewById(R.id.apk_Ver);
        apk_Ver.setText(APK_Ver);

        buttonConnect = (Button) findViewById(R.id.connectB);

        buttonEnroll = (Button) findViewById(R.id.enrollB);


        buttonDisC = (Button) findViewById(R.id.discB);
        myImage = (ImageView) findViewById(R.id.test_image);
        buttonIdentify = (Button) findViewById(R.id.IdentifyB);
        //holing reserve for android.hardware.usb test
        //UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);


        // check for existing devices
        //PendingIntent mPermissionIntent;
        for (UsbDevice mdevice : manager.getDeviceList().values()) {

            int pid, vid;

            pid = mdevice.getProductId();
            vid = mdevice.getVendorId();

            if (((pid == 0x8360) && (vid == 0x0bca)) || ((pid == 0x8221) && (vid == 0x0bca)) || ((pid == 0x8225) && (vid == 0x0bca))) {

                apk_Ver.setText(APK_Ver + "    PID: 0x"+Integer.toHexString(pid));
                d = mdevice;

                manager.requestPermission(d, mPermissionIntent);

                break;

            }


        }


        theMessage.setText("Please check the USB Reader. is connected ..");
       // theMessage.setTextColor(Color.BLACK);
        ////Identify
        buttonIdentify.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (connectrtn == 0) {
                        m_eventHandler = new EventHandler(Looper.getMainLooper());
                        buttonIdentify.setEnabled(false);

                        new Thread() {
                            public void run() {
                                Message msg0 = new Message();
                                msg0.what = PublicData.TEXTVIEW_VERIFY_PLEASE_PRESS;
                                m_eventHandler.sendMessage(msg0);
                                Log.v("Device", "Press your finger");
                                counter++;
                                if ((counter % 15) == 0) {
                                    Log.v("Device", "Start GC");
                                    System.gc();
                                }

                                Log.v("Device", "Marcus: run");

                                while ((rtn = FP.Capture()) != 0) {
                                    Message msg2 = new Message();
                                    msg2.what = PublicData.SHOW_PIC;
                                    m_eventHandler.sendMessage(msg2);
                                    //if(counter >20)
                                    //    break;
                                    //counter++;
                                }

                                FP.SaveImageBMP("/storage/emulated/0/DCIM/Camera/fp_Identify_image.bmp");
                                //rtn = FP.GetTemplate(minu_code1);
                                rtn = FP.GetEncryptedTemplate(minu_code1,piv, eskey);

                                if (rtn == -2)
                                {
                                    Message msg2 = new Message();
                                    msg2.what = PublicData.STARTEK_SDK_EXPIRES;
                                    m_eventHandler.sendMessage(msg2);
                                    return;
                                }

                                try {
                                    do_Identify();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                Message msg2 = new Message();
                                msg2.what = PublicData.SHOW_PIC;
                                m_eventHandler.sendMessage(msg2);
                            }
                        }.start();
                    } else {
                        theMessage.setText("FP_ConnectCaptureDriver() failed!!");
                        theMessage.postInvalidate();
                        FP.DisconnectCaptureDriver();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        /////ori connect here

        //Connect
        buttonConnect.setOnClickListener(new Button.OnClickListener() {
            @Override

            public void onClick(View v) {
                if(RecoveryColor !=0 )
                    theMessage.setTextColor(RecoveryColor);

                Log.v("Device", "Marcus: Click");
                try {

                    if (conn.getFileDescriptor() == -1) {
                        connectreader();
                        theMessage.setText("try connect without file descripter" + conn.getFileDescriptor());
                        connectrtn = FP.ConnectCaptureDriver(conn, d);
                        Log.d("Device", "Fails to open DeviceConnection");
                    } else {
                        theMessage.setText("try connect with file descripter" + conn.getFileDescriptor());
                        connectrtn = FP.ConnectCaptureDriver(conn, d);
                        Log.d("Device", "Opened DeviceConnection" + Integer.toString(conn.getFileDescriptor()));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                FP.GetSerialNumber(srno);
                String strSN = new String(srno);
                //FP.GetPreAllocatedKey(pak);
                //String strPAK = new String(pak);
                //FP.GetFWVer(fwver);
                //String strFWVer = new String(fwver);
                //theMessage.setText("sn: " + strSN + " pak: " + strPAK +" fw ver: " + strFWVer);

                theMessage.setText("sn: " + strSN);
            }
        });

//        //Capture
//        buttonCapture.setOnClickListener(new Button.OnClickListener() {
//            @Override
//
//            public void onClick(View v) {
//
//                // Log.v("Device", "Marcus: Click");
//                try {
//
//                    if (connectrtn == 0) {
//                        m_eventHandler = new EventHandler(Looper.getMainLooper());
////					CaptureThread m_captureThread = new CaptureThread(m_eventHandler);
////					Thread m_capture = new Thread(m_captureThread);
////					m_capture.start();
//                        buttonCapture.setEnabled(false);
//
//                        new Thread() {
//                            public void run() {
//                                super.run();
//
//                                Message msg0 = new Message();
//                                msg0.what = PublicData.TEXTVIEW_CAPTURE_PLEASE_PRESS;
//                                m_eventHandler.sendMessage(msg0);
//								Log.v("Device", "Press your finger");
//                                counter++;
//                                if ((counter % 15) == 0) {
//                                    Log.v("Device", "Start GC");
//                                    System.gc();
//                                }
//
//                                Log.v("Device", "Marcus: run");
//                                // InitialSDK();
//                                // Log.v("FP Device", "Marcus: InitialSDK() OK");
//                                // PublicData.captureDone=false;
//								counter = 0;
//                                while ((rtn = FP.Capture()) != 0) {
//                                    Message msg2 = new Message();
//                                    msg2.what = PublicData.SHOW_PIC;
//                                    m_eventHandler.sendMessage(msg2);
//                                    Message msg3 = new Message();
//                                    msg3.what = PublicData.SHOW_NFIQ;
//                                    m_eventHandler.sendMessage(msg3);
//                                    if (counter > 50)
//                                        break;
//                                    counter++;
//                                    if (rtn == -2)    //capture fail with abnormal behavior disconnect or device error
//                                        break;
//                                }
//								Log.v("Device", "by Kevin rtn = " + rtn);
//								if (rtn == 0)
//								{
//								    Message msg1 = new Message();
//                                    msg1.what = PublicData.TEXTVIEW_SUCCESS;
//                                    m_eventHandler.sendMessage(msg1);
//                                    Log.v("Device", "Marcus: FP_Capture OK");
//                                    // FP.SaveImageBMP("/system/data/fp_image.bmp");
//                                    // FP.SaveImageBMP("/data/data/com.startek.sfc360/fp_image.bmp");
//                                    FP.SaveImageBMP("/storage/emulated/0/DCIM/Camera/FP_Capture.bmp");
//                                    FP.GetISOImageBuffer((byte)0,(byte)0,bISOImgArray);
//                                    Logger.d("ISO img  = " + Arrays.toString(bISOImgArray));
//
//                                    rtn = FP.GetTemplate(minu_code1);
//									if (rtn == -2)
//								    {
//								        Message msg2 = new Message();
//                                        msg2.what = PublicData.STARTEK_SDK_EXPIRES;
//                                        m_eventHandler.sendMessage(msg2);
//										return;
//								    }
//                                    FP.SaveISOminutia(minu_code1,"/storage/emulated/0/DCIM/Camera/fpcode.ist");
//                                    Log.v("Device", "Marcus: FP_SaveImageBMP OK");
//							        // try{Thread.sleep(100);}
//							        // catch(Exception e){}
//                                    Message msg2 = new Message();
//                                    msg2.what = PublicData.SHOW_PIC;
//                                    m_eventHandler.sendMessage(msg2);
//                                    // FP_LedOff();
//                                }
//								else
//								{
//								    Message msg1 = new Message();
//                                    msg1.what = PublicData.TEXTVIEW_TIMEOUT_CAPTURE_FAIL;
//                                    m_eventHandler.sendMessage(msg1);
//								}
//                            }
//                        }.start();
//                    } else {
//                        theMessage.setText("FP_ConnectCaptureDriver() failed!!");
//                        theMessage.postInvalidate();
//                        FP.DisconnectCaptureDriver();
//                        return;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        //Enroll ori
        buttonEnroll.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //		led_off();
                Log.v("Enroll", " setOnClickListener");
                if (connectrtn == 0) {
                    buttonEnroll.setEnabled(false);

                    m_eventHandler = new EventHandler(Looper.getMainLooper());

                    //let thread do main job
                    new Thread() {
                        public void run() {
                            super.run();

                            FP.CreateEnrollHandle();

                            Message msg0 = new Message();
                            msg0.what = PublicData.TEXTVIEW_ENROLL_PLEASE_PRESS;
                            m_eventHandler.sendMessage(msg0);

                            for (int i = 0; i < 6; i++) {
                                //theMessage.setText("Times: "+i);
                                SystemClock.sleep(500);
                                while ((rtn = FP.Capture()) != 0) {
                                    Message msg1 = new Message();
                                    msg1.what = PublicData.TEXTVIEW_PRESS_AGAIN;
                                    m_eventHandler.sendMessage(msg1);
                                    Message msg2 = new Message();
                                    msg2.what = PublicData.SHOW_PIC;
                                    m_eventHandler.sendMessage(msg2);
                                }
								
                                rtn = FP.GetTemplate(minu_code1);
                                if (rtn == -2)   
								{
								    Message msg2 = new Message();
                                    msg2.what = PublicData.STARTEK_SDK_EXPIRES;
                                    m_eventHandler.sendMessage(msg2);
									return;
								}
                                rtn = FP.ISOminutiaEnroll(minu_code1, minu_code2);

                                while (true) {
                                    rtn2 = FP.CheckBlank();

                                    Message msg2 = new Message();
                                    msg2.what = PublicData.TEXTVIEW_REMOVE_FINGER;
                                    m_eventHandler.sendMessage(msg2);

                                    if (rtn2 != -1)
                                        break;
                                    //theMessage.setText("remove your finger!!!");
                                }

                                if (rtn == U_CLASS_A || rtn == U_CLASS_B) {
                                    //FP.SaveISOminutia(minu_code2, "/system/data/fpcode.dat");
                                    //FP.SaveISOminutia(minu_code2, "/data/data/com.startek.sfc360/fpcode.dat");
                                    FP.SaveISOminutia(minu_code2, "/storage/emulated/0/DCIM/Camera/fpcode.dat");

                                    SystemClock.sleep(1000);
                                    Message msg3 = new Message();
                                    msg3.what = PublicData.TEXTVIEW_SUCCESS;
                                    m_eventHandler.sendMessage(msg3);

                                    break;
                                } else if (i == 5) {
                                    Message msg4 = new Message();
                                    msg4.what = PublicData.TEXTVIEW_TIMEOUT_CAPTURE_FAIL;
                                    m_eventHandler.sendMessage(msg4);
                                }
                                //showPic();
                            }

                            FP.DestroyEnrollHandle();
                        }
                    }.start();
                } else {
                    theMessage.setText("FP_ConnectCaptureDriver() failed!!");
                    FP.DisconnectCaptureDriver();
                    return;
                }

            }

        });




        buttonDisC.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                conn.close();
                FP.DisconnectCaptureDriver();
                theMessage.setText("FP_DisconnectCaptureDriver() Succeeded!!");
                theMessage.postInvalidate();

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUsbReceiver);
    }

    public  Context getContext() {
        return Context;
    }
    public void SettingimageClick(View view) {
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(sfc360demo.this);
        View promptView = layoutInflater.inflate(R.layout.text_inpu_password, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(sfc360demo.this);
        alertDialogBuilder.setView(promptView);

        editText = (EditText) promptView.findViewById(R.id.edittext);
        sharedata = getSharedPreferences("IP_ADDR", MODE_PRIVATE);
        editor = sharedata.edit();

        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //resultText.setText("Hello, " + editText.getText());


                        Log.d("Main Dialog", "Input IP: "+editText.getText());
                        ip = editText.getText().toString().substring(0,editText.getText().toString().indexOf(':'));
                        port = editText.getText().toString().substring(editText.getText().toString().indexOf(':')+1);
                        editor.putString("ip",ip);
                        editor.putString("port",port);
                        editor.commit();
                        Log.d("Main Dialog", "IP: "+sharedata.getString("ip","0"));
                        Log.d("Main Dialog", "PORT: "+sharedata.getString("port","0"));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                onResume();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void AlertDialog(String msg) {
        new AlertDialog.Builder(sfc360demo.this)
                .setTitle("Failure")
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })

                .show();
    }

    class showPic extends AsyncTask<String, Void, String> {
        //       private ImageView image;
        private Bitmap bMap = null;

        @Override
        protected String doInBackground(String... path) {
            tryGetStream();
            return null;
        }

        protected void onPostExecute(String a) {
            myImage.setImageBitmap(bMap);
            bMap = null;
            System.gc();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            myImage.postInvalidate();
            Log.v("Device", "Marcus: onProgressUpdate");
        }

        private void tryGetStream() {
            try {
                //buf = FP_GetImageBuffer);
                FP.GetImageBuffer(bMapArray);
                Logger.d(Arrays.toString(Arrays.copyOf(bMapArray, 512)));

                bMap = BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PublicData.TEXTVIEW_SUCCESS:

                    buttonEnroll.setEnabled(true);

                    theMessage.setText("Success.");
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_FAILURE:
                    theMessage.setText(UI_message);
                    theMessage.postInvalidate();
                    AlertDialog(UI_message);
                    buttonEnroll.setEnabled(true);
                    buttonIdentify.setEnabled(true);
                    break;
                case PublicData.TEXTVIEW_CAPTURE_PLEASE_PRESS:
                    theMessage.setText("Capture: Press your finger");
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_ENROLL_PLEASE_PRESS:
                    theMessage.setText("Enroll: Press your finger");
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_VERIFY_PLEASE_PRESS:
                    theMessage.setText("Identify: Press your finger");
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_SCORE:
                    theMessage.setText("matching score=" + (int) FP.Score());
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_FILE_EXIST:
                    theMessage.setText("Verify: File exist");
                    theMessage.postInvalidate();
                    break;
                case PublicData.TEXTVIEW_FILE_NOT_EXIST:
                    theMessage.setText("File not exist, please enroll first");
                    theMessage.postInvalidate();

                    break;
                case PublicData.TEXTVIEW_REMOVE_FINGER:
                    theMessage.setText("Please remove your finger");
                    theMessage.postInvalidate();
                    //new showPic().execute("/system/data/fp_image.bmp");
                    new showPic().execute("");

                    break;
                case PublicData.TEXTVIEW_PRESS_AGAIN:
                    theMessage.setText("Please press your finger again");
                    theMessage.postInvalidate();
                    //new showPic().execute("/system/data/fp_image.bmp");
                    new showPic().execute("");
                    break;
				case PublicData.TEXTVIEW_TIMEOUT_CAPTURE_FAIL:
                    theMessage.setText("Timeout or capture fail, please press capture button again");
                    theMessage.postInvalidate();
                    //new showPic().execute("/system/data/fp_image.bmp");
                    new showPic().execute("");
                    break;
				case PublicData.STARTEK_SDK_EXPIRES:
                    theMessage.setText("Startek SDK expires now.\nPlease contact Startek Engineering Inc..");
                    theMessage.postInvalidate();
                    //new showPic().execute("/system/data/fp_image.bmp");
                    new showPic().execute("");
                    break;
                case PublicData.SHOW_PIC:
                    new showPic().execute("");

                    buttonEnroll.setEnabled(true);

                    break;
                case PublicData.SHOW_NFIQ:
                    theMessage.setText("nfiq " + FP.GetNFIQ());
                    theMessage.postInvalidate();
                    break;

            }
            super.handleMessage(msg);
        }
    }

    ////  ========  SERVER  FUNCTION  =============  ////
    public static String ByteToHexString ( byte buf[] )
    {

        StringBuffer strbuf = new StringBuffer( buf.length * 2 );
        int i;

        for ( i = 0; i < buf.length; i++ )
        {

            if ( ( ( int ) buf[i] & 0xff ) < 0x10 )

                strbuf.append("0");

            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }

        return strbuf.toString();
    }


    public void do_Identify( ) throws JSONException {

        String str_EncryptedMinutiae = ByteToHexString(minu_code1);
        String str_EncryptedSessionKey = ByteToHexString(eskey);
        String str_piv = ByteToHexString(piv);

        String json_str = BuildJson_Identify(str_EncryptedMinutiae, str_EncryptedSessionKey, str_piv);
        String results = Srv_Identify(json_str);


       UI_message = ParseJsonString(results);
       Log.d("IVAN", "Srv_Identify return string " + results+ "score"+ UI_Score);

        if(UI_Code != 200){
            Message msg1 = new Message();
            msg1.what = PublicData.TEXTVIEW_FAILURE;
            m_eventHandler.sendMessage(msg1);
        }else{
            Message msg4 = new Message();
            msg4.what = PublicData.TEXTVIEW_SUCCESS;
            m_eventHandler.sendMessage(msg4);
        }
    }

    public static String BuildJson_Identify(String encMinutiae, String eSkey, String iv)
    {
        //put together as new serialize json string as server need
        json_srv_identify json_to_srv = new json_srv_identify();
        //String ret_str;
        //using (var ms = new MemoryStream())
        {
            //assign one json to another json
            json_to_srv.encMinutiae = encMinutiae;
            json_to_srv.eSkey = eSkey;
            json_to_srv.iv = iv;

        }
        Gson gson = new Gson();
        String ret = gson.toJson(json_to_srv);

        return ret;
    }

    public static class json_srv_identify {
        @SerializedName("encMinutiae")
        private String encMinutiae;
        public String get_encMinutiae() { return encMinutiae;}
        public void set_encMinutiae(String data) {this.encMinutiae = data;}

        @SerializedName("eSkey")
        private String eSkey;
        public String get_eSkey() { return eSkey; }
        public void set_eSkey(String data) { this.eSkey = data; }

        @SerializedName("iv")
        private String iv;
        public String get_iv() { return iv; }
        public void set_iv(String data_in) { this.iv = data_in; }
    }

    private static String Srv_Identify(String json_string)
    {
        Boolean https_en = UI_HTTPS_Enable;
        //String ip = UI_Srv_IP;
        //String port = UI_Srv_Port;
        String route = "/identify";
        Boolean ignore_https_ca = true;

        Log.d("IVAN", "Srv_Identify  Json string: " + json_string);

        String ret_str = PostJson2RedirectServer(https_en,ip, port, route, json_string, ignore_https_ca);

        return ret_str;
    }

    private static String PostJson2RedirectServer(boolean https_en,String SrvIp, String port, String route, String json_string, Boolean Ignore_CA)
    {
        String protocol = "";
        String ret_str = "";


        if(https_en == true)
        {
            protocol = "https://" + SrvIp + ":" + port;
        }
        else
        {
            protocol = "http://"+ SrvIp + ":" + port;
        }

        //HttpURLConnection connection = null;
        DataOutputStream wr;
        InputStream is;
        try
        {
            URL url = new URL(protocol + route);
            if(https_en == true)
            {
                if(Ignore_CA == true)   //if need to ignore CA (ex. self signed CA for HTTPS)
                {
                    TrustManager[] trustAllCerts = new TrustManager[] {
                            new X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                                }
                                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                    HostnameVerifier allHostsValid = new HostnameVerifier()
                    {
                        public boolean verify(String hostname, SSLSession session)
                        {
                            return true;
                        };
                    };

                    // Install the all-trusting host verifier
                    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

                }


                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", Integer.toString(json_string.length())); //?
                connection.setRequestProperty("User-agent","myapp");
                connection.setConnectTimeout(120000);
                connection.setReadTimeout(120000);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setDoInput(true);

                //Write out
                wr = new DataOutputStream (connection.getOutputStream ());
                wr.writeBytes (json_string);
                wr.flush ();
                wr.close ();
               // is = connection.getInputStream();
                is = connection.getErrorStream();
                if (is == null) {
                    is = connection.getInputStream();
                }
                StatusCode = connection.getResponseCode();
            }
            else
            {
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type","application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", Integer.toString(json_string.length())); //?
                connection.setRequestProperty("User-agent","myapp");
                connection.setConnectTimeout(120000);
                connection.setReadTimeout(120000);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                //Write out
                wr = new DataOutputStream (connection.getOutputStream ());
                wr.writeBytes (json_string);
                wr.flush ();
                wr.close ();
               // is = connection.getInputStream();
                is = connection.getErrorStream();
                if (is == null) {
                    is = connection.getInputStream();
                }
                StatusCode = connection.getResponseCode();
            }

            /*
            wr.writeBytes (json_string);
            wr.flush ();
            wr.close ();

            */
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null)
            {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            ret_str = response.toString();
            //return response.toString();


        }
        catch(SocketTimeoutException e)
        {
            System.out.println("TIMEOUT: " + e);
        }
        catch(Exception e)
        {
            System.out.println("ERROR: " + e);
        }
        finally
        {
            //if(connection != null)
            {
                //connection.disconnect();

            }
        }

        Log.d("IVAN", "res_str " + ret_str+ "Response Code:" +StatusCode );
        return ret_str;
    }

    public String ParseJsonString (String message) throws JSONException {


        if(StatusCode != 200)
        {
            RecoveryColor = theMessage.getCurrentTextColor();
           // theMessage.setText(message.toString());
            theMessage.setTextColor(Color.rgb(200,0,0));
            UI_Code=StatusCode;

            return message;
        }
        Log.d("IVAN", "RETURE:"+message);

        JSONObject json = new JSONObject(message);
        int code = json.getInt("code");
        String userID;
        String fpIndex;
        String score;
        String msg = json.getString("message");
        Log.d("IVAN","code  "+code);

        // UI_UserID="0";
        UI_FPID="0";
        UI_Code = code;
        switch(code)
        {
            case 20003:
            case 20004:
                JSONObject jsonObj = json.getJSONObject("data");
                if(code == 20004){
                    userID = jsonObj.getString("clientUserId");
                    UI_UserID = userID;
                }
                fpIndex = jsonObj.getString("fpIndex");
                score = jsonObj.getString("score");
                UI_Score = score;
                if(parseInt(score) <2000)
                    UI_UserID = "NO User";

                UI_FPID = fpIndex;

                Log.d(TAG,"code  "+code);
                Log.d(TAG,"score  "+UI_Score);
                Log.d(TAG,"fpIndex  "+UI_FPID);
                Log.d(TAG,"userID "+UI_UserID);
                break;




        }
        if(code >40000) {
            UI_FPID="0";
            UI_UserID="0";
            Message msg4 = new Message();
            msg4.what = PublicData.TEXTVIEW_FAILURE;
            m_eventHandler.sendMessage(msg4);
        }
        return msg;
    }
}
