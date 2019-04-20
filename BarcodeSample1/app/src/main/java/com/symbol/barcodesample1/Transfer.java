/*
 * Copyright (C) 2015-2018 Zebra Technologies Corp
 * All rights reserved.
 */
package com.symbol.barcodesample1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.pm.ActivityInfo;

import org.json.JSONArray;
import org.json.JSONObject;

public class Transfer extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private TextView textViewData = null;
    private TextView textViewStatus = null;
    private TextView dateData = null;
    private TextView shiftData = null;
    private TextView productNameData = null;
    private TextView productCodeData = null;
    private TextView allocatedRowData = null;
    private CheckBox checkBoxEAN8 = null;
    private CheckBox checkBoxEAN13 = null;
    private CheckBox checkBoxCode39 = null;
    private CheckBox checkBoxCode128 = null;

    private Spinner spinnerScannerDevices = null;

    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private int dataLength = 0;
    private String statusString = "";

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();
    private String errorColor  = "#EF3038";
    private  String okColor = "#0865B3";
    private String floorId = "";
    private Spinner storageSelection = null;
    private Spinner regionSelection = null;
    private JSONArray storageData = null;
    RequestQueue requestQueue =null ;
    private String warehouseOpUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/receiveables";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        productNameData = (TextView)findViewById(R.id.productNameData);
        productCodeData = (TextView)findViewById(R.id.productCodeData);
        allocatedRowData = (TextView)findViewById(R.id.allocatedRowData);
        dateData = (TextView)findViewById(R.id.dateData);
        shiftData = (TextView)findViewById(R.id.shiftData);
        storageSelection = (Spinner) findViewById(R.id.storageSelection);
        regionSelection = (Spinner) findViewById(R.id.regionSelection);
        floorId =  getIntent().getStringExtra("floor_id");
        requestQueue = requestSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!",errorColor);
            return;
        }


        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());
//        initTransferForm();
    }
    @Override
    protected void onStart()
    {
        super.onStart();
//        storageSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
//                // your code here
//                try {
//                    if (position != 0) {
//                        getRegions();
//                    }
//                    else gate_no = null;
//                }catch(Exception ex){
//                    System.out.println("error in getting guardlist data onStart: guardSelection listener");
//                }
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView) {
//                // your code here
//
//            }
//
//        });
//        regionSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
//                // your code here
//                try {
//                    if (position != 0) {
//                        guard_id = (String) guardList.getJSONObject(position-1).get("id");
//                    }
//                    else guard_id = null;
//                }catch(Exception ex){
//                    System.out.println("error in getting guardlist data onStart: guardSelection listener");
//                }
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView) {
//                // your code here
//
//            }
//
//        });
    }
    @Override
    public void onOpened(EMDKManager emdkManager) {
        updateStatus(" wait!",okColor);//previously message was EMDK open success!
        this.emdkManager = emdkManager;
        // Acquire the barcode manager resources
        initBarcodeManager();
        // Enumerate scanner devices
        enumerateScannerDevices();
        // Set default scanner
        spinnerScannerDevices.setSelection(defaultIndex);
        // Initialize scanner
        initScanner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground
        if (emdkManager != null) {
            // Acquire the barcode manager resources
            initBarcodeManager();
            // Enumerate scanner devices
            enumerateScannerDevices();
            // Set selected scanner
            spinnerScannerDevices.setSelection(defaultIndex);
            // Initialize scanner
            initScanner();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background
        // Release the barcode manager resources
        deInitScanner();
        deInitBarcodeManager();
    }

    @Override
    public void onClosed() {
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.",errorColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            updateData("<font color='gray'>" + "Status" + "</font> : " + "OK");
            for(ScanData data : scanData) {
                System.out.println("scan data = " + data.getData());
                postBarcodeData(data.getData());
            }
        }
    }
    public void postBarcodeData(String data) {
        System.out.println("in postBarcodeData function" );
        Map<String, String> params = new HashMap();
        params.put("id", data);
        params.put("floor_id", floorId);
        JSONObject parameters = new JSONObject(params);
        System.out.println("params to send = " + parameters);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, warehouseOpUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if((boolean)response.get("showBarcodeData")) {
                                productNameData.setText(response.get("product_name").toString());
                                productCodeData.setText(response.get("product_code").toString());
                                allocatedRowData.setText(response.get("region").toString());
                            }

                            dateData.setText(response.get("date").toString());
                            shiftData.setText(response.get("shift").toString());
                            updateStatus(response.get("msg").toString(),response.get("msgColor").toString());
                        }catch(Exception ex){
                            System.out.println("in catch block of request function"+ex);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateStatus("System Error",errorColor);
                        System.out.println("in error response block"+error.getMessage());
                    }
                });

// Add the request to the RequestQueue.
        System.out.println("before queue add");
        requestQueue.add(jsonObjectRequest);
    }
    @Override
    public void onStatus(StatusData statusData) {
        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                updateStatus(statusString,okColor);
                // set trigger type
                if(bSoftTriggerSelected) {
                    scanner.triggerType = TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    scanner.triggerType = TriggerType.HARD;
                }
                // set decoders
                if(bDecoderSettingsChanged) {
                    setDecoders();
                    bDecoderSettingsChanged = false;
                }
                // submit read
                if(!scanner.isReadPending() && !bExtScannerDisconnected) {
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        updateStatus(e.getMessage(),errorColor);
                    }
                }
                break;
            case WAITING:
                statusString = "Ready";//previously message was Scanner is waiting for trigger press...
                updateStatus(statusString,okColor);
                break;
            case SCANNING:
                statusString = "Scanning...";
                updateStatus(statusString,okColor);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                updateStatus(statusString,errorColor);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                updateStatus(statusString,errorColor);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {
        String status;
        String scannerName = "";
        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();
        if (deviceList.size() != 0) {
            scannerName = deviceList.get(defaultIndex).getFriendlyName();
        }
        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {
            switch(connectionState) {
                case CONNECTED:
                    bSoftTriggerSelected = false;
                    synchronized (lock) {
                        initScanner();
                        bExtScannerDisconnected = false;
                    }
                    break;
                case DISCONNECTED:
                    bExtScannerDisconnected = true;
                    synchronized (lock) {
                        deInitScanner();
                    }
                    break;
            }
            status = scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status,okColor);
        }
        else {
            bExtScannerDisconnected = false;
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status,okColor);
        }
    }

    private void initScanner() {
        if (scanner == null) {
            if ((deviceList != null) && (deviceList.size() != 0)) {
                if (barcodeManager != null)
                    scanner = barcodeManager.getDevice(deviceList.get(defaultIndex));
            }
            else {
                updateStatus("Failed to get the specified scanner device! Please close and restart the application.",errorColor);
                return;
            }
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage(),errorColor);
                    deInitScanner();
                }
            }else{
                updateStatus("Failed to initialize the scanner device.",errorColor);
            }
        }
    }

    private void deInitScanner() {
        if (scanner != null) {
            try{
                scanner.disable();
                scanner.release();
            } catch (Exception e) {
                updateStatus(e.getMessage(),errorColor);
            }
            scanner = null;
        }
    }

    private void initBarcodeManager(){
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }
    }

    private void deInitBarcodeManager(){
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }

    private void enumerateScannerDevices() {
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();
            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.",errorColor);
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(Transfer.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }

    private void setDecoders() {
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                // Set EAN8
                config.decoderParams.ean8.enabled = true;
                // Set EAN13
                config.decoderParams.ean13.enabled = true;
                // Set Code39
                config.decoderParams.code39.enabled= true;
                //Set Code128
                config.decoderParams.code128.enabled = true;
                scanner.setConfig(config);
            } catch (ScannerException e) {
                updateStatus(e.getMessage(),errorColor);
            }
        }
    }

    public void softScan(View view) {
        bSoftTriggerSelected = true;
        cancelRead();
    }

    private void cancelRead(){
        if (scanner != null) {
            if (scanner.isReadPending()) {
                try {
                    scanner.cancelRead();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage(),errorColor);
                }
            }
        }
    }

    private void updateStatus(final String status,final String color){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText("" + status);
                textViewStatus.setBackgroundColor(Color.parseColor(color));
            }
        });
    }

    private void updateData(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result != null) {
                    if(dataLength ++ > 100) { //Clear the cache after 100 scans
                        textViewData.setText("");
                        dataLength = 0;
                    }
                    textViewData.append(Html.fromHtml(result));
                    textViewData.append("\n");
                    ((View) findViewById(R.id.scrollViewData)).post(new Runnable()
                    {
                        public void run()
                        {
                            ((ScrollView) findViewById(R.id.scrollViewData)).fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private void setDefaultOrientation(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(width > height){
            setContentView(R.layout.activity_recieve);
        } else {
            setContentView(R.layout.activity_recieve);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        bDecoderSettingsChanged = true;
        cancelRead();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initTransferForm()throws org.json.JSONException{
          storageData = new JSONArray();
          JSONObject jo = new JSONObject();
          jo.put("name","Storage 1");
          jo.put("id","");
          storageData.put(jo);

          jo = new JSONObject();
          jo.put("name","Storage 2");
          jo.put("id","");
          storageData.put(jo);

          jo = new JSONObject();
          jo.put("name","Storage 3");
          jo.put("id","");
          storageData.put(jo);
    }

}
