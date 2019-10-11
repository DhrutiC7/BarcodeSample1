/*
 * Copyright (C) 2015-2018 Zebra Technologies Corp
 * All rights reserved.
 */
package com.symbol.barcodesample1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
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
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.pm.ActivityInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReReceive extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

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
    private String re_receive_supervisor_id = null;

    Context context = null;

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
    private String reason = null;
    private Spinner storageSelection = null;
    private Spinner regionSelection = null;
    private Spinner reasonSelection = null;
    private Spinner supervisorSelection = null;
    private double regionVolume = 0;
    private JSONArray storageData = null;
    RequestQueue requestQueue =null ;
    ArrayAdapter<String> adapter = null;
    private String storageList[] = null;
    private String regionList[] = null;
    private String reasonList[] = null;
    private JSONArray supervisorList = null;
    private Button reReceiveStart = null;
    private Button reReceiveStop = null;
    private Button cancel = null;
    private TextView remarkLabel =null;
    private EditText remarkData = null;

    //private Boolean scanValid =true;
    private Boolean reReceiveInProgress =false;
    ArrayList<JSONObject> distinctProduct = new ArrayList<JSONObject>();
    ArrayList<String> scannedCartons = new ArrayList<String>();
    Runnable delayRunnable =null;
    Handler handler = null;
    Double scannedCartonVolume = 0.0;
    private String TAG = "Class Name: ReReceive.java";
    private int requestTimeout = 300000;
    private String fetchStorageRegionsUrl = "https://prataap-api.eronkan.com/component/warehouse-operations/form-data/prataap_snacks_rereceive_form_api/getStorageRegions";
    private String reReceiveInItUrl = "https://prataap-api.eronkan.com/component/warehouse-operations/form-data/prataap_snacks_rereceive_form_api/getInitData";
    private String fetchCartonProductUrl = "https://prataap-api.eronkan.com/component/warehouse-operations/form-data/prataap_snacks_rereceive_form_api/getCartonProduct";
    private String maintainReReceiveUrl="https://prataap-api.eronkan.com/component/warehouse-operations/form-data/prataap_snacks_rereceive_form_api/maintainReReceive";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();
        cancel = (Button)findViewById(R.id.cancel);
        context = this;
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        productNameData = (TextView)findViewById(R.id.productNameData);
        productCodeData = (TextView)findViewById(R.id.productCodeData);
        allocatedRowData = (TextView)findViewById(R.id.allocatedRowData);
        dateData = (TextView)findViewById(R.id.dateData);
        shiftData = (TextView)findViewById(R.id.shiftData);
        storageSelection = (Spinner) findViewById(R.id.storageSelection);
        regionSelection = (Spinner) findViewById(R.id.regionSelection);
        reasonSelection = (Spinner) findViewById(R.id.reasonSelection);
        supervisorSelection = (Spinner) findViewById(R.id.supervisorSelection);
        reasonList = new String[]{"Select Reason", "Wrong Quantity Rejected by Client", "Wrong Material Rejected by Client", "Quality Rejected by Client", "Miscalculation in Warehouse"};
        floorId =  getIntent().getStringExtra("floor_id");
        reReceiveStart = (Button)findViewById(R.id.reReceiveStart);
        reReceiveStop = (Button)findViewById(R.id.reReceiveStop);
        remarkLabel = (TextView) findViewById(R.id.remarkLabel);
        remarkData = (EditText) findViewById(R.id.remarkData);

        handler = new android.os.Handler(Looper.getMainLooper());
        requestQueue = requestSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!",errorColor);
            return;
        }

        try{
            initReReceiveForm();
        }catch(Exception ex){
            System.out.print("exception in getting initials,function:create()"+ex);
        }

        try {
            getStorageRegions();
        }catch(Exception ex){
            System.out.print("exception in getting storageRegions,function:create()"+ex);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        storageSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {
                        storageChange(position);
                    }
                }catch(Exception ex){
                    System.out.println("error in storageSelection onStart: storageSelection listener");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });
        regionSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {

                    }
                }catch(Exception ex){
                    System.out.println("error in getting regionSelection data onStart: guardSelection listener");
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });
        reasonSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                try {
                    if (position != 0) {
                        reason = reasonList[position];
                    }
                    else reason = null;
                }catch(Exception ex){
                    System.out.println("error in getting reasonSelection data onStart: guardSelection listener");
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
                        re_receive_supervisor_id = (String) supervisorList.getJSONObject(position-1).get("id");
                    }
                    else re_receive_supervisor_id = null;
                }catch(Exception ex){
                    System.out.println("error in getting supervisorSelection data onStart: supervisorSelection listener");
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
                try {
                    System.out.println("DATA RECEIVED: " + data.getData());
                    postBarcodeData(data.getData());                                                                        //change this
                }catch(Exception ex){
                    System.out.println("exception function : onData "+ex);
                }
            }
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


    //scanner initialization
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

    private void setDefaultOrientation(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(width > height){
            setContentView(R.layout.activity_rereceive);
        } else {
            setContentView(R.layout.activity_rereceive);
        }
    }


    //form initialization
    public void initReReceiveForm() {
        Map<String, String> params = new HashMap();
        JSONObject parameters = new JSONObject(params);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, reReceiveInItUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            setInitialData(response);

                        }catch(Exception ex){
                            System.out.println("In catch block of request function: ReReceive.Java Init Form"+ex);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateStatus("System Error",errorColor);
                        Log.e(TAG, "In error response block" + error);
                        System.out.println("in error response block"+error.getMessage());
                    }
                });

        RetryPolicy policy = new DefaultRetryPolicy(requestTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue.
        System.out.println("before queue add");
        requestQueue.add(jsonObjectRequest);
    }

    public void setInitialData (JSONObject responseData) {
        try {
            supervisorList = (JSONArray) responseData.get("supervisors");
            String[] items = new String[supervisorList.length()+1];
            items[0] = "Select Supervisor";
            for (int i = 1; i < supervisorList.length() + 1; i++) {
                items[i] = (String) (supervisorList.getJSONObject(i-1)).get("name");
            }
            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items);
            supervisorSelection.setAdapter(adapter);

            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, reasonList);
            reasonSelection.setAdapter(adapter);

        }catch(Exception ex){
            System.out.println("exception in ReReceive.java : setIntialData()"+ex);
        }
    }

    private void getStorageRegions()throws org.json.JSONException{
        updateStatus("fetching regions! wait.....",okColor);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
            (Request.Method.POST, fetchStorageRegionsUrl, null, new Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
                try {
                    setStorage(response);
                    updateStatus("Regions Fetching completed",okColor);
                }catch(Exception ex){
                    updateStatus("error: some error occured",errorColor);
                    System.out.println("in catch block of request function"+ex);
                }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                updateStatus("System Error",errorColor);
                Log.e(TAG, "In error response block" + error);
                System.out.println("in error response block"+error.getMessage());
                }
            });
        RetryPolicy policy = new DefaultRetryPolicy(requestTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonArrayRequest.setRetryPolicy(policy);
        // Add the request to the RequestQueue.
        requestQueue.add(jsonArrayRequest);
    }

    private void setStorage(JSONArray responseStorageData)throws org.json.JSONException{
        storageData =responseStorageData;
        storageList = new String[storageData.length()+1];
        storageList[0]="Select Storage";
        for(int i=1;i<=storageData.length();i++){
            storageList[i]=storageData.getJSONObject(i-1).getString("floor_name");
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, storageList);
        storageSelection.setAdapter(adapter);
    }

    private void storageChange(int idx)throws org.json.JSONException{
        JSONArray regionData = storageData.getJSONObject(idx-1).getJSONArray("regions");
        regionList = new String[regionData.length()+1];
        regionList[0] = "Select Region";
        for(int i=1;i<=regionData.length();i++){
            regionList[i]=regionData.getJSONObject(i-1).getString("region_name");
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, regionList);
        regionSelection.setAdapter(adapter);
    }

    private String getSelectedRegionId() throws JSONException{

        int selected_storage_idx=storageSelection.getSelectedItemPosition()-1;
        int selected_region_idx=regionSelection.getSelectedItemPosition()-1;
        String selected_region_id = storageData.getJSONObject(selected_storage_idx).getJSONArray("regions").getJSONObject(selected_region_idx).getString("region_id");
        return selected_region_id;

    }

    private JSONObject getSelectedRegion() throws JSONException{

        int selected_storage_idx=storageSelection.getSelectedItemPosition()-1;
        int selected_region_idx=regionSelection.getSelectedItemPosition()-1;
        JSONObject selected_region = storageData.getJSONObject(selected_storage_idx).getJSONArray("regions").getJSONObject(selected_region_idx);
        return selected_region;

    }


    //start, stop functionality
    public void startReReceive(View view){
        if(checkTransferStatus()){
            lockSelections();
            updateStatus("Re-Receive Started",okColor);
            toggleStockButtons();
            reReceiveInProgress=true;
            scannedCartonVolume = 0.0;
        }
        else {
            updateStatus("Select All the Fields",errorColor);
        }
    }

    public void toggleStockButtons(){

        if(reReceiveStart.getVisibility() == View.VISIBLE){
            reReceiveStart.setVisibility(View.GONE);
        }
        else {
            reReceiveStart.setVisibility(View.VISIBLE);
        }

        if(reReceiveStop.getVisibility() == View.VISIBLE){
            reReceiveStop.setVisibility(View.GONE);
        }
        else {
            reReceiveStop.setVisibility(View.VISIBLE);
        }

        if(remarkLabel.getVisibility() == View.VISIBLE){
            remarkLabel.setVisibility(View.GONE);
        }
        else {
            remarkLabel.setVisibility(View.VISIBLE);
        }

        if(remarkData.getVisibility() == View.VISIBLE){
            remarkData.setText("");
            remarkData.setVisibility(View.GONE);
        }
        else {
            remarkData.setText("");
            remarkData.setVisibility(View.VISIBLE);
        }

        if(cancel.getVisibility() == View.VISIBLE){
            cancel.setVisibility(View.GONE);
        }
        else {
            cancel.setVisibility(View.VISIBLE);
        }
    }

    public void stopReReceive(View view)throws JSONException{
        String selected_region_id = getSelectedRegionId();
        final String remark = remarkData.getText().toString();
        JSONObject parameters = new JSONObject();
        parameters.put("region_id", selected_region_id);
        parameters.put("reason", reason);
        parameters.put("re_receive_supervisor_id", re_receive_supervisor_id);
        parameters.put("distinct_products", getJsonArrJSONObject(distinctProduct));         //converted because request in node is taking it as string
        parameters.put("scanned_cartons", getJsonArrString(scannedCartons));
        parameters.put("comment", remark);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, maintainReReceiveUrl, parameters, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    try {
                        refreshReReceiveForm();
                        updateStatus(response.getString("msg"), response.getString("msgColor"));

                    } catch (Exception ex) {
                        System.out.println("in catch block of request function" + ex);
                    }
                    reReceiveInProgress=false;
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    updateStatus("System Error", errorColor);
                    Log.e(TAG, "In error response block" + error);
                    reReceiveInProgress=false;
                    System.out.println("in error response block" + error.getMessage());
                    }
                });

        RetryPolicy policy = new DefaultRetryPolicy(requestTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);
        // Add the request to the RequestQueue.
        System.out.println("before queue add");
        requestQueue.add(jsonObjectRequest);

    }

    public void cancelReReceive(View view){
        reReceiveInProgress=false;
        refreshReReceiveForm();
    }


    //scanned barcode functionality
    private void delayMsg(final String msg, final String color){
        if(delayRunnable==null)handler.removeCallbacks(delayRunnable);

        delayRunnable = new Runnable() {
            public void run() {
                updateStatus(msg,color );
            }
        };
        handler.postDelayed(delayRunnable, 500);
    }

    public void postBarcodeData(final String bar_id)throws JSONException {
        if(!reReceiveInProgress){
            delayMsg("Press Start Re-Receive Button",errorColor);
        }
        else if(!checkTransferStatus()){
            delayMsg("Select All the Fields",errorColor);
        }
        else if(!checkDistinctCarton(bar_id)){
            delayMsg("Barcode Repeated",errorColor);
        }
        else {
            String selected_region_id = getSelectedRegionId();
            JSONObject parameters = new JSONObject();
            parameters.put("id", bar_id);
            parameters.put("region_id", selected_region_id);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, fetchCartonProductUrl, parameters, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                productNameData.setText(response.get("product_name").toString());
                                productCodeData.setText(response.get("product_code").toString());
                                if(response.get("msg").toString().equals("Barcode OK")) {
                                    delayMsg(response.get("msg").toString(), okColor);
                                    setDistinctProduct(response,bar_id);
                                }
                                else{
                                    delayMsg(response.get("msg").toString(), errorColor);
                                }


                            } catch (Exception ex) {
                                System.out.println("in catch block of request function" + ex);
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            updateStatus("System Error", errorColor);
                            Log.e(TAG, "In error response block" + error);
                            System.out.println("in error response block" + error.getMessage());
                        }
                    });
            RetryPolicy policy = new DefaultRetryPolicy(requestTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            jsonObjectRequest.setRetryPolicy(policy);
// Add the request to the RequestQueue.
            System.out.println("before queue add");
            requestQueue.add(jsonObjectRequest);
        }
    }

    private Boolean checkTransferStatus(){
        if(storageSelection.getSelectedItemPosition() > 0 && regionSelection.getSelectedItemPosition() > 0 && reasonSelection.getSelectedItemPosition() > 0 && supervisorSelection.getSelectedItemPosition() > 0)return true; //change this condition
        else return false;
    }

    private Boolean checkDistinctProduct(JSONObject product)throws JSONException{
        for(JSONObject data : distinctProduct){
            if(data.getString("product_id").equals(product.getString("product_id")))return false;
        }
        return true;
    }

    private Boolean checkDistinctCarton(String carton_id){
        for(String data : scannedCartons){
            if(data.equals(carton_id))return false;
        }
        return true;
    }

    private boolean setAndCheckVolume(JSONObject product)throws JSONException{
        double scannedVolume = scannedCartonVolume + product.getDouble("product_volume");
        double selectedRegionVolume = getSelectedRegion().getDouble("region_volume");

        System.out.println("Carton Volume ++++++++++++++++++++++++++++++++++++++++: " + scannedVolume);
        System.out.println("Region Volume: +++++++++++++++++++++++++++++++++++++++: " + selectedRegionVolume);

        if(scannedVolume > selectedRegionVolume){
            delayMsg("region capacity is less than scanned cartons volume",errorColor);
            return true;
        }
        else
        {
            scannedCartonVolume = scannedCartonVolume + product.getDouble("product_volume");
            return true;
        }
    }

    private void setDistinctProduct(JSONObject product, String carton_id)throws JSONException{
        if(checkDistinctProduct(product)){
            if(distinctProduct.size()==2){
                updateStatus("more than two products not allowed,carton not added in stock", errorColor);
            }
            else{
                distinctProduct.add(product);
                if(checkDistinctCarton(carton_id) && setAndCheckVolume(product))scannedCartons.add(carton_id);
            }
        }
        else{
            if(checkDistinctCarton(carton_id) && setAndCheckVolume(product))scannedCartons.add(carton_id);
        }
    }


    //refresh form
    public void refreshReReceiveForm(){
        releaseSelectionLocks();
        resetSelections();
        toggleStockButtons();
        productCodeData.setText("");
        productNameData.setText("");
        distinctProduct.clear();
        scannedCartons.clear();
    }

    private void resetSelections(){
        storageSelection.setSelection(0);
        regionSelection.setSelection(0);
        reasonSelection.setSelection(0);
        supervisorSelection.setSelection(0);
    }

    private void lockSelections(){
        storageSelection.setEnabled(false);
        regionSelection.setEnabled(false);
        reasonSelection.setEnabled(false);
        supervisorSelection.setEnabled(false);
    }

    private void releaseSelectionLocks(){
        storageSelection.setEnabled(true);
        regionSelection.setEnabled(true);
        reasonSelection.setEnabled(true);
        supervisorSelection.setEnabled(true);
    }


    public JSONArray getJsonArrString(ArrayList<String> arr)throws JSONException{
        int len = arr.size();
        JSONArray arr2 = new JSONArray();
        for(int i=0;i<len;i++){
            arr2.put(arr.get(i));
        }
        return arr2;
    }

    public JSONArray getJsonArrJSONObject(ArrayList<JSONObject> arr)throws JSONException{
        int len = arr.size();
        JSONArray arr2 = new JSONArray();
        for(int i=0;i<len;i++){
            arr2.put(arr.get(i));
        }
        return arr2;
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
//                    if(dataLength ++ > 100) { //Clear the cache after 100 scans
//                        textViewData.setText("");
//                        dataLength = 0;
//                    }
//                    textViewData.append(Html.fromHtml(result));
//                    textViewData.append("\n");
//                    ((View) findViewById(R.id.scrollViewData)).post(new Runnable()
//                    {
//                        public void run()
//                        {
//                            ((ScrollView) findViewById(R.id.scrollViewData)).fullScroll(View.FOCUS_DOWN);
//                        }
//                    });
                }
            }
        });
    }

}
