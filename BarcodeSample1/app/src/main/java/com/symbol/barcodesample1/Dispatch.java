/*
 * Copyright (C) 2015-2018 Zebra Technologies Corp
 * All rights reserved.
 */
package com.symbol.barcodesample1;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.google.gson.JsonObject;
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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.pm.ActivityInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Dispatch extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private TextView textViewData = null;
    private TextView textViewStatus = null;
    private TextView dateData = null;
    private TextView shiftData = null;
    private TextView dayCartonData = null;
    private TextView shiftCartonData = null;
    private TextView productNameData = null;
    private TextView productCodeData = null;
    private Spinner guardSelection = null;
    private Spinner supervisorSelection = null;
    private Spinner pickListSelection = null;
    private JSONArray guardList = null;
    private JSONArray supervisorList = null;
    private JSONArray pickList = null;
    private String []gateList = null;
    private Spinner gateSelection = null;
    private TextView driverName = null;
    private TextView mobileNo = null;
    private TextView vehicleNo = null;
    private Button dispatchStop = null;
    private Button dispatchStart = null;
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
    RequestQueue requestQueue =null ;
    Context context = null;
    private TextView clientData = null;
    private TextView scannedLabel = null;
    private LinearLayout scannedDataLayout =null;
    private String guard_id = null;
    private String dispatch_supervisor_id = null;
    private String pick_list_id = null;
    private String gate_no = null;
    private JSONObject current_picklist_data = null;
    private MediaPlayer mp = null;
    private TextView pickListData = null;
    private Boolean gateDispatchON =false;
    Runnable delayRunnable =null;
    Handler handler = null;
    private String warehouseOpUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/dispatchItem";
    private String dispatchInItUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_dispatch_form_api/getInitData";
    private String pickListUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_dispatch_form_api/getPickListData";
    private String  dispatchStartUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_dispatch_form_api/startDispatch";
    private String  dispatchStopUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_dispatch_form_api/stopDispatch";
    private String  fetchGatePickListUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_dispatch_form_api/fetchGatePickList";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        textViewData = (TextView)findViewById(R.id.textViewData);
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        productNameData = (TextView)findViewById(R.id.productNameData);
        productCodeData = (TextView)findViewById(R.id.productCodeData);
        dateData = (TextView)findViewById(R.id.dateData);
        shiftData = (TextView)findViewById(R.id.shiftData);
        dayCartonData = (TextView)findViewById(R.id.dayCartonData);
        shiftCartonData = (TextView)findViewById(R.id.shiftCartonData);
        guardSelection= (Spinner)findViewById(R.id.guardSelection);
        supervisorSelection = (Spinner)findViewById(R.id.supervisorSelection);
        pickListSelection = (Spinner)findViewById(R.id.pickListSelection);
        gateSelection = (Spinner)findViewById(R.id.gateSelection);
        requestQueue = requestSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        clientData = (TextView)findViewById(R.id.clientData);
        context = this;
        scannedLabel = (TextView)findViewById(R.id.scannedLabel);
        scannedDataLayout = (LinearLayout)findViewById(R.id.scannedDataLayout);
        gateList = new String[]{"select gate","1","2","3","4","5"};
        driverName = (TextView)findViewById(R.id.driverName);
        mobileNo = (TextView)findViewById(R.id.mobileNo);
        vehicleNo = (TextView)findViewById(R.id.vehicleNo);
        dispatchStop = (Button)findViewById(R.id.dispatchStop);
        dispatchStart = (Button)findViewById(R.id.dispatchStart);
        pickListData = (TextView)findViewById(R.id.pickListData);
        handler = new android.os.Handler(Looper.getMainLooper());
//        checkBoxEAN8 = (CheckBox)findViewById(R.id.checkBoxEAN8);
//        checkBoxEAN13 = (CheckBox)findViewById(R.id.checkBoxEAN13);
//        checkBoxCode39 = (CheckBox)findViewById(R.id.checkBoxCode39);
//        checkBoxCode128 = (CheckBox)findViewById(R.id.checkBoxCode128);
        spinnerScannerDevices = (Spinner)findViewById(R.id.spinnerScannerDevices);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!",errorColor);
            return;
        }

//        checkBoxEAN8.setOnCheckedChangeListener(this);
//        checkBoxEAN13.setOnCheckedChangeListener(this);
//        checkBoxCode39.setOnCheckedChangeListener(this);
//        checkBoxCode128.setOnCheckedChangeListener(this);

        //addSpinnerScannerDevicesListener();

        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());
        initDispatchForm();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        gateSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {
                        gate_no = gateList[position];
                        checkRunningDispatch(gate_no);
                    }
                    else gate_no = null;
                }catch(Exception ex){
                    System.out.println("error in getting guardlist data onStart: guardSelection listener");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });
        guardSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {
                        guard_id = (String) guardList.getJSONObject(position-1).get("id");
                    }
                    else guard_id = null;
                }catch(Exception ex){
                    System.out.println("error in getting guardlist data onStart: guardSelection listener");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });

        supervisorSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {
                        dispatch_supervisor_id = (String) supervisorList.getJSONObject(position-1).get("id");
                    }
                    else dispatch_supervisor_id = null;
                }catch(Exception ex){
                    System.out.println("error in getting supervisorlist data onStart: supervisorSelection listener");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        pickListSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here

                try {
                    if(position!=0) {
                        pick_list_id = (String) pickList.getJSONObject(position-1).get("id");
                        getPickListData((String) pickList.getJSONObject(position-1).get("id"), false);
                    }
                    else pick_list_id = null;


                }catch(Exception ex){
                    System.out.println("error in getting picklist data onStart: picklistSelection listener");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });


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

//    @Override
//    public void onStop () {
////        refreshFormData();
//        super.onStop();
//    }
    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            updateData("<font color='gray'>" + "Status" + "</font> : " + "OK");
            for(ScanData data : scanData) {
                System.out.println("scan data = " + data.getData());
                try {
                    postBarcodeData(data.getData());
                }catch(Exception ex){
                    System.out.println("Exception in posting barcode data function:onData");
                }
            }
        }
    }

    private void stopPlaying() {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
    }

    private void delayMsg(String msg, String color){
        if(delayRunnable==null)handler.removeCallbacks(delayRunnable);

        delayRunnable = new Runnable() {
            public void run() {
                updateStatus("dispatch not started","#EF3038" );
            }
        };
        handler.postDelayed(delayRunnable, 500);
    }

    public void postBarcodeData(String data) throws JSONException {
        System.out.println("in postBarcodeData function" );
        if(!gateDispatchON){
            delayMsg("dispatch not started","#EF3038");
        }
        else {
            JSONObject parameters = new JSONObject();
            parameters.put("id", data);
            parameters.put("pick_list_data", current_picklist_data);
            System.out.println("params to send = " + parameters);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, warehouseOpUrl, parameters, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if ((boolean) response.get("showBarcodeData")) {
                                    scannedLabel.setVisibility(View.VISIBLE);
                                    scannedDataLayout.setVisibility(View.VISIBLE);
                                    productNameData.setText(response.get("product_name").toString());
                                    productCodeData.setText(response.get("product_code").toString());
//                                    dateData.setText(response.get("date").toString());
//                                    shiftData.setText(response.get("shift").toString());
                                 }
                                updateStatus(response.get("msg").toString(), response.get("msgColor").toString());
                                getPickListData(current_picklist_data.getString("id"), true);
                                if (!response.get("msg").toString().equals("Barcode Ok") ) {
                                    try {
                                        stopPlaying();
                                        mp = MediaPlayer.create(context, R.raw.error_alert);
                                        mp.start();
                                    } catch (Exception ex) {
                                        System.out.print(ex);
                                    }
                                }
                            } catch (Exception ex) {
                                System.out.println("in catch block of request function" + ex);
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            updateStatus("System Error", errorColor);
                            System.out.println("in error response block" + error.getMessage());
                        }
                    });

// Add the request to the RequestQueue.
            System.out.println("before queue add");
            requestQueue.add(jsonObjectRequest);
            }
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

    /*private void addSpinnerScannerDevicesListener() {
        spinnerScannerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {
                if ((scannerIndex != position) || (scanner==null)) {
                    scannerIndex = position;
                    bSoftTriggerSelected = false;
                    bExtScannerDisconnected = false;
                    deInitScanner();
                    initScanner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }*/

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
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(Dispatch.this, android.R.layout.simple_spinner_item, friendlyNameList);
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
            setContentView(R.layout.activity_dispatch);
        } else {
            setContentView(R.layout.activity_dispatch);
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

    public void initDispatchForm() {
        Map<String, String> params = new HashMap();
//        params.put("id", data);
        JSONObject parameters = new JSONObject(params);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, dispatchInItUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            setInitialData(response);

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

    public void setInitialData (JSONObject responseData) {
        try {
            guardList = (JSONArray) responseData.get("guards");
            String[] items = new String[guardList.length()+1];
            items[0] = "select guard";
            for (int i = 1; i < guardList.length() + 1; i++) {
                items[i] = (String) (guardList.getJSONObject(i-1)).get("name");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items);
            guardSelection.setAdapter(adapter);

            supervisorList = (JSONArray) responseData.get("supervisors");
            items = new String[supervisorList.length()+1];
            items[0] = "select supervisor";
            for (int i = 1; i < supervisorList.length() + 1; i++) {
                items[i] = (String) (supervisorList.getJSONObject(i-1)).get("name");
            }
            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items);
            supervisorSelection.setAdapter(adapter);

            pickList = (JSONArray) responseData.get("pickLists");
            items = new String[pickList.length()+1];
            items[0] = "select pick list";
            for (int i = 1; i < pickList.length()+1; i++) {
                items[i] = (String) (pickList.getJSONObject(i-1)).get("pick_list_no");
            }
            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items);
            pickListSelection.setAdapter(adapter);

            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, gateList);
            gateSelection.setAdapter(adapter);

            dateData.setText(responseData.getString("date"));
            shiftData.setText(responseData.getString("shift"));

        }catch(Exception ex){
            System.out.println("exception in Dispatch.java : setIntialData()"+ex);
        }
    }

    public void getPickListData(String id, final boolean showActual) throws org.json.JSONException{
        JSONObject parameters = new JSONObject();
        parameters.put("pick_list_id",id);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, pickListUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println(response);
                            current_picklist_data = response;
                            pickListData.setText(current_picklist_data.getString("pick_list_no"));
                            showShippingOrderData(response,showActual);

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

    public void getCurrentPickListData(String id, final boolean showActual) throws org.json.JSONException{
        JSONObject parameters = new JSONObject();
        parameters.put("pick_list_id",id);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, pickListUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println(response);
                            current_picklist_data = response;
                            dispatchStart.setVisibility(View.GONE);
                            dispatchStop.setVisibility(View.VISIBLE);
                            pickListData.setText(current_picklist_data.getString("pick_list_no"));
                            pickListData.setVisibility(View.VISIBLE);
                            pickListSelection.setVisibility(View.GONE);
                            setCurrentPicklistData();
                            lockDispatchForm();
                            showShippingOrderData(response,showActual);

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

    public void showShippingOrderData(JSONObject responseData, boolean showActual) throws org.json.JSONException{
        JSONArray allShippingArr =(JSONArray)responseData.get("shippingOrders");
        JSONArray shippingArr  = null;
        JSONObject shippingObj = null;
        TextView tv = null;
        clientData.setText(allShippingArr.getJSONArray(0).getJSONObject(0).getString("client_name"));
        TableLayout ll = (TableLayout) findViewById(R.id.shippingOrderTable);
        ll.setStretchAllColumns(true);
        TableRow.LayoutParams tlp = null;
        ll.removeAllViews();

        for (int i = 0; i <allShippingArr.length(); i++) {
            shippingArr  = allShippingArr.getJSONArray(i);

            setTableHeaders(shippingArr, showActual);

            for (int j = 0; j <shippingArr.length(); j++) {
                TableRow row= new TableRow(context);
                if(showActual)tlp=getTableParam(0.25f);
                else tlp = getTableParam(0.33f);
                try {
                    shippingObj = shippingArr.getJSONObject(j);
                    makeAndSetColumn(shippingObj.getString("product_name"),row);
//                    makeAndSetColumn(shippingObj.getString("weight"),row);
                    makeAndSetColumn(shippingObj.getString("planned_quantity"),row);
                    if(showActual){
                        makeAndSetColumn(shippingObj.getString("actual_quantity"),row);
                        makeAndSetColumn(shippingObj.getString("row"),row);
                    }
                    ll.addView(row);
                }catch(Exception ex){
                    System.out.println(ex);
                }

            }

        }
    }

    public void setTableHeaders(JSONArray shippingArr, boolean showActual) throws org.json.JSONException{

        TableLayout ll = (TableLayout) findViewById(R.id.shippingOrderTable);
        TableRow row= new TableRow(context);
        TableRow.LayoutParams tlp = showActual ? getTableParam(0.25f) : getTableParam(0.33f);
        makeAndSetColumn("Sales Order No. : ", row);
        makeAndSetColumn(shippingArr.getJSONObject(0).getString("sales_order_no"),row);
        ll.addView(row);
        row= new TableRow(context);
        makeAndSetColumn("Product", row);
//        makeAndSetColumn("Weight", row);
        makeAndSetColumn("Planned Qty", row);
        if(showActual){
            makeAndSetColumn("Actual Qty", row);
            makeAndSetColumn("row", row);
        }
        ll.addView(row);

    }

    public void makeAndSetColumn(String data, TableRow row){

        TextView tv = new TextView(context);
        tv.setText(data);
        tv.setPadding(15,5,0,5);
        row.addView(tv);
//        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) tv.getLayoutParams();
//        tv.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
//        tv.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
//        tv.setLayoutParams(params);
    }
    public TableRow.LayoutParams getTableParam(float  weight){
        TableRow.LayoutParams tlp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, weight);
        return tlp;
    }
    public void startDispatch(View view) throws org.json.JSONException{
        if(!checkDispatchValidity()){
            updateStatus("Enter all fields",errorColor);
        }
        else {
//            guardSelection.setEnabled(false);
//            supervisorSelection.setEnabled(false);
//            pickListSelection.setEnabled(false);
//            mobileNo.setEnabled(false);
//            vehicleNo.setEnabled(false);
//            driverName.setEnabled(false);
            JSONObject parameters = new JSONObject();
            int idx = pickListSelection.getSelectedItemPosition();
            final String id = pickList.getJSONObject(idx-1).getString("id");
            Date currentTime = Calendar.getInstance().getTime();
            parameters.put("pick_list_id", id);
            parameters.put("dispatch_start_time", currentTime);
            parameters.put("dispatch_supervisor_id", dispatch_supervisor_id);
            parameters.put("driver_name", driverName.getText());
            parameters.put("driver_mobile_number", mobileNo.getText());
            parameters.put("vehicle_no", vehicleNo.getText());
            parameters.put("gate_no", gate_no);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, dispatchStartUrl, parameters, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                System.out.println(response);
                                getPickListData(id, true);
                                gateDispatchON = true;
                                toggleDispatchButtons();
                                toggleDropDowns();
                                lockDispatchForm();

                            } catch (Exception ex) {
                                System.out.println("in catch block of request function" + ex);
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            updateStatus("System Error", errorColor);
                            System.out.println("in error response block" + error.getMessage());
                        }
                    });

// Add the request to the RequestQueue.
            System.out.println("before queue add");
            requestQueue.add(jsonObjectRequest);
        }
    }

    public void stopDispatch(View view) throws org.json.JSONException{
        JSONObject parameters = new JSONObject();
        final String id = current_picklist_data.getString("id");
        Date currentTime = Calendar.getInstance().getTime();
        parameters.put("pick_list_id",id);
        parameters.put("dispatch_stop_time",currentTime);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, dispatchStopUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println(response);
                            refreshFormData();
                            toggleDispatchButtons();
                            toggleDropDowns();
                            releaseDispatchLock();

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

    public boolean checkDispatchValidity() {
        String a = driverName.getText().toString();
        String b = mobileNo.getText().toString();
        String c = vehicleNo.getText().toString();

        if(driverName.getText().toString().trim().equals("")){
            System.out.println("ok");
        }

        if(gate_no == null || pick_list_id == null || dispatch_supervisor_id == null || guard_id == null
                || driverName.getText().toString().trim().equals("") || mobileNo.getText().toString().trim().equals("") || vehicleNo.getText().toString().trim().equals("")){
            return false;
        }
        else return true;
    }

    public void toggleDispatchButtons(){

        if(dispatchStart.getVisibility() == View.VISIBLE){
            dispatchStart.setVisibility(View.GONE);
        }
        else {
            dispatchStart.setVisibility(View.VISIBLE);
        }

        if(dispatchStop.getVisibility() == View.VISIBLE){
            dispatchStop.setVisibility(View.GONE);
        }
        else {
            dispatchStop.setVisibility(View.VISIBLE);
        }
    }
    public void toggleDropDowns(){
        if(pickListSelection.getVisibility() == View.VISIBLE){
            pickListSelection.setVisibility(View.GONE);
        }
        else {
            pickListSelection.setVisibility(View.VISIBLE);
        }

        if(pickListData.getVisibility() == View.VISIBLE){
            pickListData.setVisibility(View.GONE);
        }
        else {
            pickListData.setVisibility(View.VISIBLE);
        }
    }

    public void refreshFormData(){
//      gateSelection.setSelection(0);
      pickListSelection.setSelection(0);
      supervisorSelection.setSelection(0);
      guardSelection.setSelection(0);
      driverName.setText("");
      mobileNo.setText("");
      vehicleNo.setText("");
      clientData.setText("");
//      toggleDropDowns();
      TableLayout ll = (TableLayout) findViewById(R.id.shippingOrderTable);
      ll.removeAllViews();
    }
    public void lockDispatchForm(){
        guardSelection.setEnabled(false);
        supervisorSelection.setEnabled(false);
        pickListSelection.setEnabled(false);
        mobileNo.setEnabled(false);
        vehicleNo.setEnabled(false);
        driverName.setEnabled(false);
    }

    public void releaseDispatchLock(){
        guardSelection.setEnabled(true);
        supervisorSelection.setEnabled(true);
        pickListSelection.setEnabled(true);
        mobileNo.setEnabled(true);
        vehicleNo.setEnabled(true);
        driverName.setEnabled(true);
    }
    public void checkRunningDispatch(String gateNo) throws org.json.JSONException{
       // refreshFormData();
        JSONObject parameters = new JSONObject();

        parameters.put("gate_no",gateNo);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, fetchGatePickListUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            System.out.println(response);
                            if(response.getString("id")!="null") {
                                getCurrentPickListData(response.getString("id"), true);
                                gateDispatchON=true;
                            }
                            else{
                                if(gateDispatchON){
                                    refreshFormData();
                                    releaseDispatchLock();
                                    toggleDropDowns();
                                    toggleDispatchButtons();
                                    initDispatchForm();
                                }
                                gateDispatchON =false;
                            }

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

    public int getIndexFromId(String id, JSONArray list) throws JSONException{

        for(int i=0;i<list.length();i++){
            if(list.getJSONObject(i).get("id") == id){
                return i;
            }
        }
        return 1;
    }

     public void setCurrentPicklistData() throws JSONException {
         int guard_idx = getIndexFromId(current_picklist_data.getString("guard_id"),guardList) + 1;
         int supervisor_idx = getIndexFromId(current_picklist_data.getString("dispatch_supervisor_id"), supervisorList) + 1;
         int gate_idx = Integer.parseInt(current_picklist_data.getString("gate_no"))+1;
         //int pickList_idx = getIndexFromId(current_picklist_data.getString("id"), pickList) + 1;
         guardSelection.setSelection(guard_idx);
         supervisorSelection.setSelection(supervisor_idx);
        // pickListSelection.setSelection(pickList_idx);
//         gateSelection.setSelection(gate_idx);
         vehicleNo.setText(current_picklist_data.getString("vehicle_no"));
         driverName.setText(current_picklist_data.getString("driver_name"));
         mobileNo.setText(current_picklist_data.getString("driver_mobile_number"));
         clientData.setText(current_picklist_data.getJSONArray("shippingOrders").getJSONArray(0).getJSONObject(0).getString("client_name"));

     }
}
