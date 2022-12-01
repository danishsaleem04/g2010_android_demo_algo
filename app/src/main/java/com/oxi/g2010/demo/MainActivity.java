package com.oxi.g2010.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.oxi.g2010.demo.utils.FileUtils;
import com.oxi.g2010.demo.utils.ImageUtils;
import com.oxi.sdk.JG_FprDev;
import com.oxi.sdk.OxiErrorCode;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.support.v7.widget.RecyclerView;

public class MainActivity extends Activity {
    int IMAGE_WIDTH = 256;
    int IMAGE_HEIGHT = 360;

    TextView tv_log, tv_version;
    TextView tv_message;
    EditText et_sn3;
    ImageView img_showFp;
    Button btn_findDevice;
    Button btn_openDevice;
    Button btn_readProductSN;
    Button btn_prepareCapture;
    Button btn_beginCapture;
    Button btn_endCapture;
    Button btn_closeDevice;
    Button btn_readFmVersion;
    Button btn_chooseImage;
    Button btn_generateTemplate;
    Button btn_get_template;
    Button btn_capture_template;
    Button btn_verify_template;
    Button btn_select_template;
    JG_FprDev myScanner;
    RecyclerView recyclerview;
    volatile boolean isCapture = false;
    volatile boolean isCaptureTemplate;
    boolean isWakeup = false;
    IntentFilter filter;

    // byte[] bmpData = new byte[256*360 + 1078]; //for getBMP
    byte[] bmpData;
    final byte[] rawData = new byte[IMAGE_WIDTH * IMAGE_HEIGHT];

    private static String PATH_KEY;
    // final int FILE_SELECT_CODE = 20170310;

    private LinearLayoutManager mLayoutManager;
    private RecyclerViewAdapter mAdapter;

    private ArrayList mFingerList;

    String choosepath = "";
    byte[] chooseBuffer;

    private static final int ENROLL_MSG = 100;
    private static final int AUTH_MSG = 101;
    private static final int SCORE_MSG = 102;
    private static final int GENRATE_TEMPLATE_MSG = 103;
    private static final int READ_TEMPLATE_MSG = 104;
    private static final int CAPTURE_TEMPLATE_MSG = 105;
    private static final int DOWNLOAD_AUTH_MSG = 106;
    private static final int SELECT_PATH_MSG = 107;
    private static final int DELETE_TEMPLATE_MSG = 108;

    private static  final  String  MATCH_RESULT = "match_result";
    private static  final  String  MATCH_SCORE = "match_score";

    ExecutorService exec;

    Handler msgHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {

            int result = -1;

            switch (msg.what) {
                case 0:
                    if (isCapture) {
                        Bitmap bitmap = ImageUtils.createBitmap(bmpData);
                        img_showFp.setImageBitmap(bitmap);//显示bitmap
                        String str_time = msg.getData().getString("time");
                        tv_log.setText(" cost time: " + str_time + " ms");
                    }
                    break;
                case ENROLL_MSG:
                    result = msg.arg1;
                    if (result == 0) {
                        tv_message.setText(getString(R.string.label_enroll_end));
                        img_showFp.setImageResource(R.drawable.finger_done);
                    }
                    /*for (int i = 0; i < 5; i++) {
                        mFingerList.add(i + 1);
                    }
                    mAdapter.notifyDataSetChanged();*/
                    break;
                case AUTH_MSG:
                    result = msg.arg1;
                    if (result >= 0) {
                        img_showFp.setImageResource(R.drawable.match);
                    } else {
                        img_showFp.setImageResource(R.drawable.non_match);
                    }
                    break;
                case SCORE_MSG:
                    result = msg.arg1;
                    tv_message.setText("score = " + result);
                    break;
                case GENRATE_TEMPLATE_MSG:
                    result = msg.arg1;
                    if (result == 0) {
                        tv_message.setText("generate template success");
                    } else {
                        tv_message.setText("generate template failed");
                    }
                    break;
                case READ_TEMPLATE_MSG:
                    img_showFp.setImageResource(R.drawable.finger_done);
                    result = msg.arg1;
                    if (result == 0) {
                        tv_message.setText("read back template success");
                    } else {
                        tv_message.setText("read back template failed");
                    }
                    break;
                case CAPTURE_TEMPLATE_MSG:
                    img_showFp.setImageResource(R.drawable.finger_done);
                    result = msg.arg1;
                    if (result == 0) {
                        tv_message.setText("capture template success");
                    } else if (result == ErrorCode.OXI_NO_TOUCH) {
                        tv_message.setText("no touch ,please touch again");
                    } else {
                        tv_message.setText("capture template failed");
                    }
                    break;
                case DOWNLOAD_AUTH_MSG:
                    Bundle b = msg.getData();
                    int score = 0;
                    result = b.getInt(MATCH_RESULT);
                    score = b.getInt(MATCH_SCORE);
                    Log.e("fenglei", "DOWNLOAD_AUTH_MSG result:" + result + " score:"+ score);
                    if(score >= 580){
                        tv_message.setText("score: "+score);
                    }else{
                        tv_message.setText("match score is less than 580");
                    }
                    if (result > 0) {
                        img_showFp.setImageResource(R.drawable.match);
                    } else {
                        if (result == -100) {
                            tv_message.setText(getString(R.string.text_pls_choose_template_file));
                        } else {
                            img_showFp.setImageResource(R.drawable.non_match);
                        }
                    }
                    break;
                case SELECT_PATH_MSG:
                    String str = "choose file is :" + choosepath;
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                    break;
                case DELETE_TEMPLATE_MSG:
                    tv_message.setText(getString(R.string.text_delete_template_success));
                    break;
                default:
                    OxiLog.Log("default case:" +msg.what);
                    break;
            }
        }

    };

    public class CaptureThread implements Runnable {
        @Override
        public void run() {
            while (isCapture) {

                //Log.e("fenglei", "capture start");
                int channel = myScanner.LIVESCAN_GetChannelCount();

                long begintTime = System.currentTimeMillis();
                long endTime = 0;
                int result = myScanner.LIVESCAN_GetFPRawData(channel, rawData);

                //Log.e("fenglei", "capture get data end");
                endTime = System.currentTimeMillis();

                if (result == OxiErrorCode.OXI_OK) {
                    ImageUtils.translate180(rawData, IMAGE_WIDTH, IMAGE_HEIGHT);
                    bmpData = ImageUtils.bufferToBmpbuffer(rawData, IMAGE_WIDTH, IMAGE_HEIGHT);
                    Message msg = new Message();
                    msg.what = 0;
                    Bundle bundle = new Bundle();
                    bundle.putString("time", "" + (endTime - begintTime));
                    msg.setData(bundle);
                    msgHandler.sendMessage(msg);
                }
                //Thread.yield();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myScanner = JG_FprDev.getInstance(getApplicationContext());
        mFingerList = new ArrayList();
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new RecyclerViewAdapter(this, mFingerList);
        findViews();

        //显示版本号
        try {
            tv_version.setText("version: " + (getPackageManager().getPackageInfo(getPackageName(), 0)).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        setDeviceDefaultStatus();

        // find device
        btn_findDevice.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int result = myScanner.LIVESCAN_Find();
                if (result == OxiErrorCode.OXI_OK) {

                    updateDeviceFoundStatus();
                }
                tv_log.setText("find result: " + result);
            }
        });

        // open device
        btn_openDevice.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                int result = myScanner.LIVESCAN_Init();
                if (result == OxiErrorCode.OXI_OK) {
                    updateDeviceOpendStatus();
                    updateDevicePrepareStatus();
                } else if (result == OxiErrorCode.OXI_NO_DEVICE) {

                } else if (result == OxiErrorCode.OXI_NO_PERMISSION) {
                    updateDevicesClosedStatus();
                }

                tv_log.setText("open result: " + result);
            }
        });

        // prepare capture
        btn_prepareCapture.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                tv_message.setText(getString(R.string.text_wakeup_state));
                int result = myScanner.LIVESCAN_PrepareCapture();
                if (result == OxiErrorCode.OXI_OK) {
                    updateDevicePrepareStatus();
                }
                tv_log.setText("prepare capture result: " + result);
            }
        });

        // begin capture
        btn_beginCapture.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(btn_beginCapture.getText().toString().equals("capture image")){
                    tv_message.setText(getString(R.string.text_capture_image_state));
                    img_showFp.setImageResource(R.drawable.finger01);
                    //wakeup device
                    int result = myScanner.LIVESCAN_PrepareCapture();
                    if (result == OxiErrorCode.OXI_OK) {
                        if (isCapture) {
                            isCapture = false;
                            exec.shutdownNow();
                            exec = null;
                        }
                        exec = Executors.newSingleThreadExecutor();
                        exec.execute(new CaptureThread());
                        isCapture = true;
                        isWakeup = true;

                        updateDevicesBeginStatus();
                        btn_beginCapture.setText("end capture");
                    }
                    tv_log.setText("begin capture result: " + result);
                }else if (btn_beginCapture.getText().toString().equals("end capture")){
                    tv_message.setText(getString(R.string.text_end_capture));
                    if (isCapture) {
                        isCapture = false;
                        exec.shutdownNow();
                        exec = null;
                        if (bmpData != null && bmpData.length != 0)
                            FileUtils.saveImage(MainActivity.this, bmpData, 1);
                    }
                    int result = myScanner.LIVESCAN_EndCapture();
                    if (result == OxiErrorCode.OXI_OK) {
                        isWakeup = false;
                        updateDevicesEndStatus();
                        btn_beginCapture.setText("capture image");
                    }
                    tv_log.setText("end capture result:" + result);
                }
            }
        });

        // end capture
        btn_endCapture.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                tv_message.setText(getString(R.string.text_end_capture));
                if (isCapture) {
                    isCapture = false;
                    exec.shutdownNow();
                    exec = null;
                    if (bmpData != null && bmpData.length != 0)
                        FileUtils.saveImage(MainActivity.this, bmpData, 1);
                }
                int result = myScanner.LIVESCAN_EndCapture();
                if (result == OxiErrorCode.OXI_OK) {
                    isWakeup = false;
                    updateDevicesEndStatus();
                }
                tv_log.setText("end capture result:" + result);
            }
        });

        btn_chooseImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_message.setText(getString(R.string.label_choose_image));
                img_showFp.setImageResource(R.drawable.finger01);
                showFileChooser(11);
            }
        });

        btn_generateTemplate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // wakeup device
                int result = myScanner.LIVESCAN_PrepareCapture();
                if (result == OxiErrorCode.OXI_OK) {
                    isWakeup = true;
                    updateDevicePrepareStatus();
                }
                // generate template
                tv_message.setText("generating template...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean isBmpFile = ImageUtils.isBmpFile(choosepath);
                        Log.e("fenglei", "path: " + choosepath + " is BmpFile whether or not:" + isBmpFile);
                        if (isBmpFile) {
                            byte[] buffer = ImageUtils.getBufferFromBmpBuffer(chooseBuffer);
                            int ret_gen = myScanner.LIVESCAN_GENERATETEMPLATEBYIMAGE(buffer);
                            if (ret_gen == 0){
                                byte[] template = new byte[498];
                                int ret_read = myScanner.LIVESCAN_READMPLATE(template);
                                if (ret_read == 0) {
                                    FileUtils.saveImage(MainActivity.this, template, 2);
                                }
                                Message message = msgHandler.obtainMessage();
                                message.what = READ_TEMPLATE_MSG;
                                message.arg1 = ret_read;
                                msgHandler.sendMessage(message);
                            }else{
                                Message message = msgHandler.obtainMessage();
                                message.what = GENRATE_TEMPLATE_MSG;
                                message.arg1 = ret_gen;
                                msgHandler.sendMessage(message);
                            }
                        }else{
                            tv_message.setText(getString(R.string.text_pls_choose_image_file));
                        }
                        //sleep device
                        int result = myScanner.LIVESCAN_EndCapture();
                        if (result == OxiErrorCode.OXI_OK) {
                            isWakeup = false;
                        }
                    }
                }).start();

            }
        });

        btn_get_template.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_message.setText("get template start");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] template = new byte[498];
                        int ret = myScanner.LIVESCAN_READMPLATE(template);
                        if (ret == 0) {
                            FileUtils.saveImage(MainActivity.this, template, 2);
                        }

                        Message message = msgHandler.obtainMessage();
                        message.what = READ_TEMPLATE_MSG;
                        message.arg1 = ret;
                        msgHandler.sendMessage(message);

                    }
                }).start();
            }
        });

        btn_capture_template.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // wakeup device
                int result = myScanner.LIVESCAN_PrepareCapture();
                if (result == OxiErrorCode.OXI_OK) {
                    isWakeup = true;
                    updateDevicePrepareStatus();
                }
                // capture template
                isCaptureTemplate = true;
                tv_message.setText(getString(R.string.text_capture_template_start));
                img_showFp.setImageResource(R.drawable.finger01);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isCaptureTemplate) {
                            byte[] template = new byte[498];
                            int ret = myScanner.LIVESCAN_CAPTUREMPLATE(template);
                            if (ret == 0) {
                                FileUtils.saveImage(MainActivity.this, template, 2);

                                Message message = msgHandler.obtainMessage();
                                message.what = CAPTURE_TEMPLATE_MSG;
                                message.arg1 = ret;
                                msgHandler.sendMessage(message);
                                isCaptureTemplate = false;
                            }
                        }
                        //sleep device
                        int result = myScanner.LIVESCAN_EndCapture();
                        if (result == OxiErrorCode.OXI_OK) {
                            isWakeup = false;
                        }
                    }
                }).start();
            }
        });

        btn_select_template.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_message.setText(getString(R.string.label_select_template));
                showFileChooser(1);
                //chooserFile(1);
            }
        });

        btn_verify_template.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // wakeup device
                int result = myScanner.LIVESCAN_PrepareCapture();
                if (result == OxiErrorCode.OXI_OK) {
                    isWakeup = true;
                    updateDevicePrepareStatus();
                }

                tv_message.setText("capturing image... ");
                img_showFp.setImageResource(R.drawable.finger01);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int result = 0;
                        int[] scores= new int[2];
                        boolean isIsoFile = ImageUtils.isIso(choosepath);

                        msgHandler.removeMessages(DOWNLOAD_AUTH_MSG);
                        Message message = msgHandler.obtainMessage();

                        if (isIsoFile) {
                            if (chooseBuffer.length >0) {
                                result = myScanner.LIVESCAN_VERIFYTEMPLATE(scores,chooseBuffer, chooseBuffer.length);
                            }else{
                                throw new RuntimeException("template buffer is wrong");
                            }
                        } else {
                            result = -100;
                        }

                        Bundle bundle=new Bundle();
                        bundle.putInt(MATCH_RESULT,result);
                        bundle.putInt(MATCH_SCORE,scores[0]);
                        message.setData(bundle);
                        message.what = DOWNLOAD_AUTH_MSG;
                        msgHandler.sendMessage(message);

                        //sleep device
                        result = myScanner.LIVESCAN_EndCapture();
                        if (result == OxiErrorCode.OXI_OK) {
                            isWakeup = false;
                        }
                    }
                }).start();
            }
        });

        // close device
        btn_closeDevice.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if (isCapture) {
                    isCapture = false;
                    exec.shutdownNow();
                    exec = null;

                }

                if (isWakeup) {
                    myScanner.LIVESCAN_EndCapture();
                    isWakeup = false;
                }

                myScanner.LIVESCAN_Close();
                tv_log.setText("close device");
                tv_message.setText("");
                updateDevicesClosedStatus();
            }
        });

        // read the product sn number
        btn_readProductSN.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                String serialNumber = null;
                byte[] sn3 = new byte[20];

                int result = myScanner.LIVESCAN_READ_ProductSN(sn3);
                serialNumber = new String(sn3);

                tv_log.setText("read product sn3 result " + result
                        + "\n length: " + sn3.length + "\n productSN : "
                        + serialNumber);

            }
        });


        //读固件版本
        btn_readFmVersion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] fmVersion = new byte[4];
                int result = myScanner.LIVESCAN_READ_FirmwareVersion(fmVersion);

                StringBuffer str_sn3 = new StringBuffer();
                for (int i = 0; i < 4; i++) {
                    str_sn3.append(fmVersion[i]);
                    str_sn3.append(" ");
                }

                tv_log.setText("read fmVersion result " + result
                        + "\n length: " + fmVersion.length + "\n fmVersion : "
                        + str_sn3);
            }
        });

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED);
        registerReceiver(mUsbReceiver, filter);

        checkPermissions();
    }

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
    };

    private void checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 100);
        }
    }

    void saveFile(int index, byte[] keyBuffer) {

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {// 优先保存到SD卡中
            PATH_KEY = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + "OXiKey";
            OxiLog.Log("OxiKey", " sd卡----------------");
        } else {// 如果SD卡不存在，就保存到本应用的目录下
            PATH_KEY = getApplicationContext().getFilesDir().getAbsolutePath()
                    + File.separator + "OXiKey";
            OxiLog.Log("OxiKey", "应用目录-----------");
        }

        File dir = new File(PATH_KEY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filename = "key" + index;

        OxiLog.Log("OxiKey", "filename:" + filename);

        BufferedOutputStream bos = null;
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(dir + File.separator + filename + ".key");
            bos = new BufferedOutputStream(fos);

            try {
                bos.write(keyBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();

                    Uri localUri = Uri.fromFile(new File(dir, filename + ".key"));
                    Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
                    sendBroadcast(localIntent);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    private byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);

            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
            byte[] b = new byte[4 * 1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    @Override
    protected void onResume() {
        super.onResume();
        OxiLog.Log("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        OxiLog.Log("onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        OxiLog.Log("onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        OxiLog.Log("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isCapture) {
            isCapture = false;
            exec.shutdownNow();
            exec = null;

            int result = myScanner.LIVESCAN_EndCapture();
            if (result == OxiErrorCode.OXI_OK) {
                isWakeup = false;
                updateDevicesEndStatus();
                tv_log.setText("end capture");
            }
        }

        myScanner.LIVESCAN_Close();

        unregisterReceiver(mUsbReceiver);
    }

    private void findViews() {
        // TODO Auto-generated method stub
        tv_log = (TextView) findViewById(R.id.tv_log);
        btn_findDevice = (Button) findViewById(R.id.btn_findDevice);
        btn_openDevice = (Button) findViewById(R.id.btn_openDevice);
        et_sn3 = (EditText) findViewById(R.id.et_sn3);
        btn_readProductSN = (Button) findViewById(R.id.btn_readProductSN);
        btn_readFmVersion = (Button) findViewById(R.id.btn_readFmVersion);
        btn_prepareCapture = (Button) findViewById(R.id.btn_prepareCapture);
        btn_beginCapture = (Button) findViewById(R.id.btn_beginCapture);
        img_showFp = (ImageView) findViewById(R.id.img_showFp);
        btn_endCapture = (Button) findViewById(R.id.btn_endCapture);
        btn_chooseImage = (Button) findViewById(R.id.btn_choose_image);
        btn_generateTemplate = (Button) findViewById(R.id.btn_generate_template);
        btn_get_template = (Button) findViewById(R.id.btn_get_template);
        btn_capture_template = (Button) findViewById(R.id.btn_capture_template);
        btn_select_template = (Button) findViewById(R.id.btn_select_template);
        btn_verify_template = (Button) findViewById(R.id.btn_verify_template);
        btn_closeDevice = (Button) findViewById(R.id.btn_closeDevice);
        tv_version = (TextView) findViewById(R.id.tv_version);
        tv_message = (TextView) findViewById(R.id.tv_message);
        recyclerview = (RecyclerView) findViewById(R.id.recycler_view);

        recyclerview.setLayoutManager(mLayoutManager);
        recyclerview.setAdapter(mAdapter);
    }

    private void setDeviceDefaultStatus() {
        btn_findDevice.setEnabled(true);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(false);
        btn_chooseImage.setEnabled(false);
        btn_generateTemplate.setEnabled(false);
        btn_get_template.setEnabled(false);
        btn_capture_template.setEnabled(false);
        btn_select_template.setEnabled(false);
        btn_verify_template.setEnabled(false);
    }

    private void updateDeviceFoundStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(true);
        btn_readFmVersion.setEnabled(false);
        btn_chooseImage.setEnabled(false);
        btn_generateTemplate.setEnabled(false);
        btn_get_template.setEnabled(false);
        btn_capture_template.setEnabled(false);
        btn_select_template.setEnabled(false);
        btn_verify_template.setEnabled(false);
    }

    private void updateDeviceOpendStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(true);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(true);
        btn_closeDevice.setEnabled(true);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(true);
        btn_chooseImage.setEnabled(false);
        btn_generateTemplate.setEnabled(false);
        btn_get_template.setEnabled(false);
        btn_capture_template.setEnabled(false);
        btn_select_template.setEnabled(false);
        btn_verify_template.setEnabled(false);
    }

    private void updateDevicePrepareStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(true);
        btn_endCapture.setEnabled(true);
        btn_readProductSN.setEnabled(true);
        btn_closeDevice.setEnabled(true);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(true);
        btn_chooseImage.setEnabled(true);
        btn_generateTemplate.setEnabled(true);
        btn_get_template.setEnabled(true);
        btn_capture_template.setEnabled(true);
        btn_select_template.setEnabled(true);
        btn_verify_template.setEnabled(true);
    }

    private void updateDevicesBeginStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(true);
        btn_endCapture.setEnabled(true);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(false);
        btn_chooseImage.setEnabled(false);
        btn_generateTemplate.setEnabled(false);
        btn_get_template.setEnabled(false);
        btn_capture_template.setEnabled(false);
        btn_select_template.setEnabled(false);
        btn_verify_template.setEnabled(false);
    }

    private void updateDevicesEnrollStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(false);
    }

    private void updateDevicesAuthStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(false);
    }

    private void updateDevicesEndStatus() {
        btn_findDevice.setEnabled(false);
        btn_prepareCapture.setEnabled(true);
        btn_beginCapture.setEnabled(true);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(true);
        btn_closeDevice.setEnabled(true);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(true);
        btn_chooseImage.setEnabled(true);
        btn_generateTemplate.setEnabled(true);
        btn_get_template.setEnabled(true);
        btn_capture_template.setEnabled(true);
        btn_select_template.setEnabled(true);
        btn_verify_template.setEnabled(true);
    }

    private void updateDevicesClosedStatus() {
        btn_findDevice.setEnabled(true);
        btn_prepareCapture.setEnabled(false);
        btn_beginCapture.setEnabled(false);
        btn_endCapture.setEnabled(false);
        btn_readProductSN.setEnabled(false);
        btn_closeDevice.setEnabled(false);
        btn_openDevice.setEnabled(false);
        btn_readFmVersion.setEnabled(false);
        btn_chooseImage.setEnabled(false);
        btn_generateTemplate.setEnabled(false);
        btn_get_template.setEnabled(false);
        btn_capture_template.setEnabled(false);
        btn_select_template.setEnabled(false);
        btn_verify_template.setEnabled(false);
    }


    private void showFileChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (requestCode == 11){
            intent.setType("image/*");
        }else{
            intent.setType("*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to write"), requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }

    }

    private void chooserFile(int requestCode) {
        String path = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + File.separator + "OXiUart";
        File file = new File(path);

        File parentFlie = new File(file.getParent());
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.fromFile(parentFlie), "*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to write"), requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Uri uri = data.getData();
            choosepath = FileUtils.getPathFromContentUri(this, uri);
            chooseBuffer = getBytes(choosepath);

            if (chooseBuffer != null) {
                Message message = msgHandler.obtainMessage();
                message.what = SELECT_PATH_MSG;
                msgHandler.sendMessage(message);
            }
            if (requestCode == 11) {
                btn_generateTemplate.performClick();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            OxiLog.Log("action: " + action);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                OxiLog.Log("usb permission: " + true);
            } else {
                OxiLog.Log("usb permission: " + false);
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    OxiLog.Log("usbreceiver", "attach ");
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    OxiLog.Log("usbreceiver", "dettach");

                    if (isCapture) {
                        isCapture = false;
                        exec.shutdownNow();
                        exec = null;

                        isWakeup = false;
                        tv_log.setText("usb detached");
                    }
                    myScanner.LIVESCAN_Close();
                    setDeviceDefaultStatus();
                }
            }
        }
    };

}
