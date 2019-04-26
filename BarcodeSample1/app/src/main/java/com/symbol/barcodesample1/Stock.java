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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
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
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import org.json.JSONException;
import org.json.JSONObject;

public class Stock extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

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
    ArrayAdapter<String> adapter = null;
    private String storageList[] = null;
    private String regionList[] = null;
    private Button stockStart = null;
    private Button cancel = null;
    private Button stockStop = null;
    private Boolean scanValid =true;

    ArrayList<JSONObject> distinctProduct = new ArrayList<JSONObject>();
    ArrayList<String> scannedCartons = new ArrayList<String>();
    private String fetchStorageRegionsUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_stock_form_api/getStorageRegions";
    private String fetchCartonProductUrl = "https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_stock_form_api/getCartonProduct";
    private String maintainStockUrl="https://stageapi.eronkan.com:443/component/warehouse-operations/form-data/prataap_snacks_stock_form_api/maintainStock";
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
        stockStart = (Button)findViewById(R.id.stockStart);
        stockStop = (Button)findViewById(R.id.stockStop);
        cancel = (Button)findViewById(R.id.cancel);
        requestQueue = requestSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!",errorColor);
            return;
        }
        try {
            getStorageRegions();
        }catch(Exception ex){
            System.out.print("exception in getting storageRegions,function:create()"+ex);
        }
    }
    @Override
    protected void onStart()
    {
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
                    System.out.println("error in getting guardlist data onStart: guardSelection listener");
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
                    postBarcodeData(data.getData());
                }catch(Exception ex){
                    System.out.println("exception function : onData "+ex);
                }
            }
        }
    }

    public void postBarcodeData(final String bar_id)throws JSONException {
        if(!checkTransferStatus()){
            updateStatus("Select Both Storage and Region",errorColor);
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
                                    if(response.get("msg").toString().equals("Barcode Ok")) {
                                        updateStatus(response.get("msg").toString(), okColor);
                                        setDistinctProduct(response,bar_id);
                                    }
                                    else{
                                        updateStatus(response.get("msg").toString(), errorColor);
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
//            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(Transfer.this, android.R.layout.simple_spinner_item, friendlyNameList);
//            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            spinnerScannerDevices.setAdapter(spinnerAdapter);
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

    private void setDefaultOrientation(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(width > height){
            setContentView(R.layout.activity_stock);
        } else {
            setContentView(R.layout.activity_stock);
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
                        System.out.println("in error response block"+error.getMessage());
                    }
                });

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

    public void startStock(View view){
        if(checkTransferStatus()){
            lockSelections();
            updateStatus("Stock Started",okColor);
            toggleStockButtons();
        }
        else {
            updateStatus("Select Both Storage and Region",errorColor);
        }
    }

    public void toggleStockButtons(){

        if(stockStart.getVisibility() == View.VISIBLE){
            stockStart.setVisibility(View.GONE);
        }
        else {
            stockStart.setVisibility(View.VISIBLE);
        }

        if(stockStop.getVisibility() == View.VISIBLE){
            stockStop.setVisibility(View.GONE);
        }
        else {
            stockStop.setVisibility(View.VISIBLE);
        }

        if(cancel.getVisibility() == View.VISIBLE){
            cancel.setVisibility(View.GONE);
        }
        else {
            cancel.setVisibility(View.VISIBLE);
        }
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

    public void stopStock(View view)throws JSONException{

        if(!scanValid){
            updateStatus("Only two products allowed, Cancel and Restart Scan",errorColor);
        }
        else  {
            String selected_region_id = getSelectedRegionId();
            JSONObject parameters = new JSONObject();
            parameters.put("region_id", selected_region_id);
            parameters.put("distinct_products", getJsonArrJSONObject(distinctProduct));//converted beacuase request in node is taking it as string
            parameters.put("scanned_cartons", getJsonArrString(scannedCartons));
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, maintainStockUrl, parameters, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                refreshStockForm();
                                updateStatus(response.getString("msg"), response.getString("msgColor"));

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

    public void cancelStock(View view){
           refreshStockForm();
    }

    public void refreshStockForm(){
        releaseSelectionLocks();
        resetSelections();
        toggleStockButtons();
        distinctProduct.clear();
        scannedCartons.clear();
        scanValid=true;
    }

    private void resetSelections(){
        storageSelection.setSelection(0);
        regionSelection.setSelection(0);
    }

    private void lockSelections(){
        storageSelection.setEnabled(false);
        regionSelection.setEnabled(false);
    }

    private void releaseSelectionLocks(){
        storageSelection.setEnabled(true);
        regionSelection.setEnabled(true);
    }

    private Boolean checkTransferStatus(){
        if(storageSelection.getSelectedItemPosition() > 0 && regionSelection.getSelectedItemPosition() > 0)return true;
        else return false;
    }

    private String getSelectedRegionId() throws JSONException{

        int selected_storage_idx=storageSelection.getSelectedItemPosition()-1;
        int selected_region_idx=regionSelection.getSelectedItemPosition()-1;
        String selected_region_id = storageData.getJSONObject(selected_storage_idx).getJSONArray("regions").getJSONObject(selected_region_idx).getString("region_id");
        return selected_region_id;

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


    private void setDistinctProduct(JSONObject product, String carton_id)throws JSONException{
            if(checkDistinctProduct(product)){
                if(distinctProduct.size()>=2){
                    scanValid=false;
                    updateStatus("more than two products not allowed", errorColor);
                }
                else{
                    distinctProduct.add(product);
                    if(checkDistinctCarton(carton_id))scannedCartons.add(carton_id);
                }
            }
            else{
                if(checkDistinctCarton(carton_id))scannedCartons.add(carton_id);
            }
    }
}
