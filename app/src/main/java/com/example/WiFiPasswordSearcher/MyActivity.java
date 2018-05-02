package com.example.WiFiPasswordSearcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.ActionBar;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class APData
{
    public String BSSID;
    public ArrayList<String> Keys;
    public ArrayList<Boolean> Generated;
    public ArrayList<String> WPS;
}

class WiFiListSimpleAdapter extends SimpleAdapter
{
    private Context context;
    private List DataList;
    private Map<Integer, View> myViews = new HashMap<Integer, View>();
    private static HashMap<String, Drawable> SvgImageCache = new HashMap<String, Drawable>();

    private int choosePasswordId  = -1;
    private View choosingPasswordView = null;

    public WiFiListSimpleAdapter(Context _context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(_context, data, resource, from, to);
        context = _context;
        DataList = data;
    }

    private String DeleteInTextTags(String text) {

        if (text.length() > 2 && text.substring(0, 2).equals("*[")) {
            String stylePref = text.substring(2, text.indexOf("]*"));

            text = text.substring(stylePref.length() + 4);
        }
        return text;
    }

    private void ParseInTextTags(TextView txtView)
    {
        String text = ""+txtView.getText();

        if (text.length() > 2 && text.substring(0, 2).equals("*["))
        {
            String stylePref = text.substring(2, text.indexOf("]*"));
            txtView.setText(text.substring(stylePref.length() + 4));

            if (stylePref.indexOf(":") > 0)
            {
                String style = stylePref.substring(0, stylePref.indexOf(":"));
                String value = stylePref.substring(stylePref.indexOf(":")+1);

                if (style.equals("color"))
                {
                    switch (value)
                    {
                        case "red":
                            txtView.setTextColor(Color.rgb(153, 0, 0));
                            break;
                        case "green":
                            txtView.setTextColor(Color.GREEN);
                            break;
                        case "greendark":
                            txtView.setTextColor(Color.rgb(0, 153, 76));
                            break;
                        case "blue":
                            txtView.setTextColor(Color.BLUE);
                            break;
                        case "yellow":
                            txtView.setTextColor(Color.rgb(153, 153, 0));
                            break;
                        case "gray":
                            txtView.setTextColor(Color.rgb(105, 105, 105));
                            break;
                    }
                }
            }
        }
        else {
            txtView.setTextColor(Color.WHITE);
        }
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView imgSec = (ImageView) view.findViewById(R.id.imgSec);
        ImageView imgWPS = (ImageView) view.findViewById(R.id.imgWps);

        HashMap<String, String> ElemWiFi;
        ElemWiFi = (HashMap) DataList.get(position);
        String Capability = ElemWiFi.get("CAPABILITY");

        imgSec.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        imgWPS.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        SVG svgImg = null;

        if (Capability.indexOf("WPA2") > -1)
        {
            Drawable img = SvgImageCache.get("WPA2");
            if (img == null)
            {
                svgImg = SVGParser.getSVGFromResource(context.getResources(), R.raw.wpa2_ico);
                img = svgImg.createPictureDrawable();
                SvgImageCache.put("WPA2", img);
            }
            imgSec.setImageDrawable(img);
        }
        else if (Capability.indexOf("WPA") > -1)
        {
            Drawable img = SvgImageCache.get("WPA");
            if (img == null)
            {
                svgImg = SVGParser.getSVGFromResource(context.getResources(), R.raw.wpa_ico);
                img = svgImg.createPictureDrawable();
                SvgImageCache.put("WPA", img);
            }
            imgSec.setImageDrawable(img);
        }
        else if (Capability.indexOf("WEP") > -1)
        {
            Drawable img = SvgImageCache.get("WEP");
            if (img == null)
            {
                svgImg = SVGParser.getSVGFromResource(context.getResources(), R.raw.wep_ico);
                img = svgImg.createPictureDrawable();
                SvgImageCache.put("WEP", img);
            }
            imgSec.setImageDrawable(img);
        }
        else
        {
            Drawable img = SvgImageCache.get("OPEN");
            if (img == null)
            {
                svgImg = SVGParser.getSVGFromResource(context.getResources(), R.raw.open_ico);
                img = svgImg.createPictureDrawable();
                SvgImageCache.put("OPEN", img);
            }
            imgSec.setImageDrawable(img);
        }

        if (Capability.indexOf("WPS") > -1)
        {
            Drawable img = SvgImageCache.get("WPS");
            if (img == null)
            {
                svgImg = SVGParser.getSVGFromResource(context.getResources(), R.raw.wps_ico);
                img = svgImg.createPictureDrawable();
                SvgImageCache.put("WPS", img);
            }
            imgWPS.setImageDrawable(img);
        }
        else
        {
            imgWPS.setImageResource(android.R.color.transparent);
        }

        TextView txtKey = (TextView) view.findViewById(R.id.KEY);
        TextView txtSignal = (TextView) view.findViewById(R.id.txtSignal);
        TextView txtRowId = (TextView)view.findViewById(R.id.txtRowId);
        TextView txtKeysCount = (TextView)view.findViewById(R.id.txtKeysCount);
        TextView txtWPS = (TextView)view.findViewById(R.id.txtWPS);
        LinearLayout llKeys = (LinearLayout)view.findViewById(R.id.llKeys);

        llKeys.setOnClickListener(onKeyClick);

        ParseInTextTags(txtKey);
        ParseInTextTags(txtSignal);
        ParseInTextTags(txtKeysCount);
        ParseInTextTags(txtWPS);

        int keysCount = Integer.parseInt(txtKeysCount.getText().toString());
        llKeys.setClickable(keysCount > 1);

        txtRowId.setText(Integer.toString(position));

        return view;
    }

    private View.OnClickListener onKeyClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            if (MyActivity.WiFiKeys == null || MyActivity.WiFiKeys.size() == 0) return;

            LinearLayout llRow = (LinearLayout)v.getParent().getParent();
            TextView txtRowId = (TextView)llRow.findViewById(R.id.txtRowId);
            final int rowId = Integer.parseInt(txtRowId.getText().toString());

            ArrayList<String> keys = MyActivity.WiFiKeys.get(rowId).Keys;
            ArrayList<String> wpss = MyActivity.WiFiKeys.get(rowId).WPS;

            if (keys.size() <= 1) return;

            String[] keysList = new String[keys.size()];

            for (int i = 0; i < keysList.length; i++)
            {
                String WPS = wpss.get(i);
                if (WPS == "") {
                    keysList[i] = "Key: " + DeleteInTextTags(keys.get(i));
                }
                else {
                    keysList[i] = "Key: " + DeleteInTextTags(keys.get(i)) + " WPS: " + DeleteInTextTags(WPS);
                }
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle("Choose key:");
            dialogBuilder.setItems(keysList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    passwordChoose(rowId, item);
                }});
            choosePasswordId = rowId;
            dialogBuilder.show();
        }
    };

    private void passwordChoose(int rowID, int passId)
    {
        ArrayList<String> keys = MyActivity.WiFiKeys.get(rowID).Keys;
        ArrayList<Boolean> gen = MyActivity.WiFiKeys.get(rowID).Generated;
        ArrayList<String> wps = MyActivity.WiFiKeys.get(rowID).WPS;
        String choosedPassword = keys.get(passId);
        Boolean isGen = gen.get(passId);
        String curWPS = wps.get(passId);
        String KeyColor;

        keys.set(passId, keys.get(0));
        keys.set(0, choosedPassword);
        gen.set(passId, gen.get(0));
        gen.set(0, isGen);
        wps.set(passId, wps.get(0));
        wps.set(0, curWPS);

        View row = MyActivity.WiFiList.getChildAt(rowID);
        TextView txtKey = (TextView)row.findViewById(R.id.KEY);
        KeyColor = (isGen ? "*[color:red]*" : "*[color:green]*");
        txtKey.setText(KeyColor + choosedPassword);
        ParseInTextTags(txtKey);
        TextView txtWPS = (TextView)row.findViewById(R.id.txtWPS);
        txtWPS.setText(curWPS == "" ? "*[color:gray]*[unknown]" : "*[color:blue]*" + curWPS);
        ParseInTextTags(txtWPS);
    }
}


public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    private Settings mSettings;
    private UserManager User;

    public static String APP_VERSION = "";
    public static String SERVER_LOGIN = "";
    public static String SERVER_PASSWORD = "";
    public static String SERVER_URI = "";
    public static String API_READ_KEY = "";
    public static String API_WRITE_KEY = "";
    public static Boolean API_KEYS_VALID = false;


    public static ListView WiFiList = null;
    private Button btnRefresh = null;
    private Button btnCheckFromBase = null;
    private Button btnStartGPSLog = null;
    private ImageButton btnSettings = null;
    private static List<ScanResult> WiFiScanResult = null;
    public static ArrayList<APData> WiFiKeys = new ArrayList<APData>();

    private static boolean ScanInProcess = false;
    private static boolean WiFiListPrepared = false;
    private static BroadcastReceiver ScanWiFiReceiverIntent = null;

    private WifiManager WifiMgr = null;
    private LocationManager LocationMgr = null;

    private ClipboardManager sClipboard = null;
    protected LinearLayout lastWiFiClickItem = null;

    private static WiFiListSimpleAdapter adapter = null;

    private static final String[] listContextMenuItems = new String[]{
        "Network details",
        "Copy ESSID",
        "Copy BSSID",
        "Copy network key",
        "Store network profile",
        "Generate WPS PIN"
    };

    public void btnRefreshOnClick(View v)
    {
        if (ScanInProcess) return;

        if (WiFiKeys != null) WiFiKeys.clear();
        if (WiFiScanResult != null) WiFiScanResult.clear();

        Context context = getApplicationContext();
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        SimpleAdapter adapter = new SimpleAdapter(context, list, R.layout.row,
            new String[]{"ESSID", "BSSID"},
            new int[]{R.id.ESSID, R.id.BSSID});
        WiFiList.setAdapter(adapter);
        ScanAndShowWiFi();
    };

    private View.OnClickListener btnSettingsOnClick = new View.OnClickListener()
    {
        public void onClick(View v) {
            Intent intent = new Intent(MyActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener btnStartGPSLogOnClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            /*setContentView(R.layout.gpslogging);
            btnSettingsRevent = (ImageButton) findViewById(R.id.btnGPSLoggingRevent);
            btnSettingsRevent.setOnClickListener(btnSettingsReventOnClick);*/
        }
    };

    private View.OnClickListener btnCheckFromBaseOnClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            if (ScanInProcess) return;
            if (WiFiKeys != null) WiFiKeys.clear();

            final ProgressDialog dProccess = new ProgressDialog(MyActivity.this);
            dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dProccess.setMessage("Searching in 3WiFi...");
            dProccess.setCanceledOnTouchOutside(false);
            btnCheckFromBase.setEnabled(false);
            dProccess.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    CheckFromBase();
                    dProccess.dismiss();
                }
            }).start();
        }
    };

    public Activity getActivity()
    {
        return this;
    }

    private TextView GetDataRowsFromLinLay(LinearLayout LinLay, String Type)
    {
        switch (Type)
        {
            case "BSSID":
                return (TextView)LinLay.findViewById(R.id.BSSID);
            case "ESSID":
                return (TextView)LinLay.findViewById(R.id.ESSID);
            case "KEY":
                return (TextView)LinLay.findViewById(R.id.KEY);
        }
        return null;
    }


    public AdapterView.OnItemClickListener WiFiListOnClick = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View linearLayout, int position, final long id)
        {
            LinearLayout item = (LinearLayout)linearLayout;
            lastWiFiClickItem = item;

            TextView txtBSSID = GetDataRowsFromLinLay(item, "BSSID");
            TextView txtESSID = GetDataRowsFromLinLay(item, "ESSID");
            TextView txtKey = GetDataRowsFromLinLay(item, "KEY");

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MyActivity.this);

            View dialogView = MyActivity.this.getLayoutInflater().inflate(R.layout.listclick_contextmenu, null);

            dialogBuilder.setTitle(txtESSID.getText());
            final String ESSDWps = txtESSID.getText().toString();
            final String BSSDWps = txtBSSID.getText().toString();

            dialogBuilder.setItems(listContextMenuItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item)
                {
                    APData apdata;
                    Boolean NeedToast = false;

                    ScanResult scanResult = WiFiScanResult.get((int)id);

                    switch (item)
                    {
                        case 0:
                            Intent detailsActivityIntent = new Intent(MyActivity.this, WifiDetails.class);
                            HashMap<String, String> WifiInfo = new HashMap<String, String>();
                            WifiInfo.put("BSSID", scanResult.BSSID);
                            WifiInfo.put("SSID", scanResult.SSID);
                            WifiInfo.put("Freq", Integer.toString(scanResult.frequency));
                            WifiInfo.put("Signal", Integer.toString(scanResult.level));
                            WifiInfo.put("Capabilities", scanResult.capabilities);

                            detailsActivityIntent.putExtra("WifiInfo", WifiInfo);
                            startActivity(detailsActivityIntent);
                            break;
                        case 1:         // Copy
                            TextView txtBSSID = GetDataRowsFromLinLay(lastWiFiClickItem, "ESSID");
                            ClipData dataClip;
                            dataClip = ClipData.newPlainText("text", txtBSSID.getText());
                            sClipboard.setPrimaryClip(dataClip);
                            NeedToast = true;
                            break;
                        case 2:         // Copy BSSID
                            TextView txtESSID = GetDataRowsFromLinLay(lastWiFiClickItem, "BSSID");
                            dataClip = ClipData.newPlainText("text", txtESSID.getText());
                            sClipboard.setPrimaryClip(dataClip);
                            NeedToast = true;
                            break;
                        case 3:         // Copy Key
                            if (WiFiKeys.isEmpty())
                            {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "No data here! Click 3WiFi button to fetch some keys.",
                                        Toast.LENGTH_LONG);
                                toast.show();
                                break;
                            }

                            apdata = WiFiKeys.get((int)id);

                            if (apdata.Keys.size() < 1)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Key not found! Nothing to copy", Toast.LENGTH_SHORT);
                                toast.show();
                                return;
                            }

                            dataClip = ClipData.newPlainText("text", apdata.Keys.get(0));
                            sClipboard.setPrimaryClip(dataClip);
                            NeedToast = true;
                            break;
                        case 4:         // Add network
                            if (WiFiKeys.isEmpty())
                            {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "No data here! Click 3WiFi button to fetch some keys.",
                                        Toast.LENGTH_LONG);
                                toast.show();
                                break;
                            }

                            apdata = WiFiKeys.get((int)id);
                            if (apdata == null || apdata.Keys.size() < 1)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Key not found!", Toast.LENGTH_SHORT);
                                toast.show();
                                break;
                            }

                            if (!WifiMgr.isWifiEnabled())
                            {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Wi-Fi interface is disabled", Toast.LENGTH_SHORT);
                                toast.show();
                                break;
                            }
                            List<WifiConfiguration> list = WifiMgr.getConfiguredNetworks();
                            int cnt = 0;
                            for (WifiConfiguration wifi : list)
                            {
                                if (wifi.SSID != null && wifi.SSID.equals("\"" + scanResult.SSID + "\""))
                                    cnt++;
                            }
                            if (cnt > 0)
                            {
                                final ScanResult gScanResult = scanResult;
                                final APData gAPData = apdata;
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                addNetworkProfile(gScanResult, gAPData);
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                break;
                                        }
                                        dialog.dismiss();
                                    }
                                };
                                AlertDialog.Builder builder = new AlertDialog.Builder(MyActivity.this);
                                builder.setTitle(String.format("Network \"%s\" is already stored %d times.", scanResult.SSID, cnt))
                                    .setMessage("Do you still want to add this network?")
                                    .setPositiveButton("Yes", dialogClickListener)
                                    .setNegativeButton("No", dialogClickListener).show();
                            }
                            else
                                addNetworkProfile(scanResult, apdata);
                            break;
                        case 5:         // wps
                            Intent wpsActivityIntent = new Intent(MyActivity.this, WPSActivity.class);
                            wpsActivityIntent.putExtra("variable", ESSDWps);
                            wpsActivityIntent.putExtra("variable1", BSSDWps);
                            startActivity(wpsActivityIntent);
                            break;
                    }

                    if (NeedToast)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Copied to clipboard", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    dialog.dismiss();
                }
            });

            dialogBuilder.show();

        }

    };

    private void addNetworkProfile(ScanResult scanResult, APData apdata)
    {
        WifiConfiguration WifiCfg = new WifiConfiguration();
        WifiCfg.BSSID = scanResult.BSSID;
        WifiCfg.SSID = String.format("\"%s\"", scanResult.SSID);
        WifiCfg.hiddenSSID = false;
        WifiCfg.priority = 1000;

        if (scanResult.capabilities.indexOf("WEP") > -1)
        {
            WifiCfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

            WifiCfg.wepKeys[0] = String.format("\"%s\"", apdata.Keys.get(0));
            WifiCfg.wepTxKeyIndex = 0;
        }
        else
        {
            WifiCfg.preSharedKey = String.format("\"%s\"", apdata.Keys.get(0));
        }

        int netId = WifiMgr.addNetwork(WifiCfg);
        Toast toast;
        if (netId > -1)
        {
            toast = Toast.makeText(getApplicationContext(),
                    "Network profile added!", Toast.LENGTH_SHORT);
        }
        else
        {
            if (WifiMgr.isWifiEnabled())
            {
                toast = Toast.makeText(getApplicationContext(),
                        "Failed to add network profile", Toast.LENGTH_SHORT);
            }
            else
            {
                toast = Toast.makeText(getApplicationContext(),
                        "Wi-Fi interface is disabled", Toast.LENGTH_SHORT);
            }
        }
        toast.show();
    }

    private View.OnClickListener vKEYOnClick = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Copy...", Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

    }

    public void ApiDataTest()
    {
        if (!API_KEYS_VALID)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast t = Toast.makeText(getApplicationContext(), "Please enter credentials", Toast.LENGTH_SHORT);
                    t.show();
                }
            });
            Intent startActivity = new Intent(this, StartActivity.class);
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startActivity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActionBar actionBar = getActionBar();
        actionBar.hide();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        APP_VERSION = getResources().getString(R.string.app_version);

        mSettings = new Settings(getApplicationContext());
        User = new UserManager(getApplicationContext());

        SERVER_LOGIN = mSettings.AppSettings.getString(Settings.APP_SERVER_LOGIN, "");
        SERVER_PASSWORD = mSettings.AppSettings.getString(Settings.APP_SERVER_PASSWORD, "antichat");
        SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, "http://3wifi.stascorp.com");
        API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");
        API_WRITE_KEY = mSettings.AppSettings.getString(Settings.API_WRITE_KEY, "");
        API_KEYS_VALID = mSettings.AppSettings.getBoolean(Settings.API_KEYS_VALID, false);


        WiFiList = (ListView) findViewById(R.id.WiFiList);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnCheckFromBase = (Button) findViewById(R.id.btnCheckFromBase);
        btnSettings = (ImageButton) findViewById(R.id.btnSettings);
        btnStartGPSLog = (Button) findViewById(R.id.btnStartGPSLog);

        WifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        LocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        btnCheckFromBase.setOnClickListener(btnCheckFromBaseOnClick);
        btnStartGPSLog.setOnClickListener(btnStartGPSLogOnClick);
        btnSettings.setOnClickListener(btnSettingsOnClick);
        WiFiList.setOnItemClickListener(WiFiListOnClick);

        if (adapter != null)
        {
            WiFiList.setAdapter(adapter);
            btnCheckFromBase.setEnabled(true);
        }
        if (ScanInProcess)
        {
         //   if(ScanWiFiReceiverIntent != null) unregisterReceiver(ScanWiFiReceiverIntent);
            //ScanAndShowWiFi();
        }
        ScanAndShowWiFi();
    }

    public void ScanAndShowWiFi()
    {
        WiFiScanResult = null;
        adapter = null;
        if (!WifiMgr.isWifiEnabled())
        {
            Toast toast = Toast.makeText(this,
                    "Wi-Fi interface is disabled", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        final ProgressDialog dProccess = new ProgressDialog(MyActivity.this);
        dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dProccess.setMessage("Scanning networks...");
        dProccess.setCanceledOnTouchOutside(false);
        dProccess.show();

        ScanWiFiReceiverIntent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                List<ScanResult> results = WifiMgr.getScanResults();
                WiFiScanResult = results;

                ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
                HashMap<String, String> ElemWiFi;

                for (ScanResult result : results) {
                    ElemWiFi = new HashMap<String, String>();
                    ElemWiFi.put("ESSID", result.SSID);
                    ElemWiFi.put("BSSID", result.BSSID.toUpperCase());
                    ElemWiFi.put("KEY", "*[color:gray]*[no data]");
                    ElemWiFi.put("WPS", "*[color:gray]*[no data]");
                    ElemWiFi.put("SIGNAL", getStrSignal(result.level));
                    ElemWiFi.put("KEYSCOUNT", "*[color:gray]*0");
                    ElemWiFi.put("CAPABILITY", result.capabilities);

                    list.add(ElemWiFi);
                }

                adapter = new WiFiListSimpleAdapter(getActivity(), list, R.layout.row,
                        new String[]{"ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"},
                        new int[]{R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount});
                WiFiList.setAdapter(adapter);

                ScanInProcess = false;
                btnRefresh.setEnabled(true);
                btnCheckFromBase.setEnabled(true);

                Toast toast = Toast.makeText(getApplicationContext(),
                        "Scan is complete!", Toast.LENGTH_SHORT);
                toast.show();

                unregisterReceiver(this);
                ScanWiFiReceiverIntent = null;
                dProccess.dismiss();
            }
        };
        registerReceiver(ScanWiFiReceiverIntent, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        ScanInProcess = true;
        btnRefresh.setEnabled(false);
        btnCheckFromBase.setEnabled(false);
        WifiMgr.startScan();
    }

    private void CheckFromBase()
    {
        JSONObject bss = new JSONObject();
        BufferedReader Reader = null;
        String ReadLine = "";
        String RawData = "";

        try {
            JSONObject query = new JSONObject();
            query.put("key", API_READ_KEY);
            JSONArray bssids = new JSONArray();
            for (ScanResult result : WiFiScanResult) {
                bssids.put(result.BSSID);
            }
            query.put("bssid", bssids);

            URL Uri = new URL(SERVER_URI + "/api/apiquery");

            HttpURLConnection Connection = (HttpURLConnection) Uri.openConnection();
            Connection.setRequestMethod("POST");
            Connection.setDoOutput(true);
            Connection.setRequestProperty("Content-Type", "application/json");

            DataOutputStream writer = new DataOutputStream(
                    Connection.getOutputStream());
            writer.writeBytes(query.toString());

            Connection.setReadTimeout(10 * 1000);
            Connection.connect();

            Reader = new BufferedReader(new InputStreamReader(Connection.getInputStream()));

            while ((ReadLine = Reader.readLine()) != null) {
                RawData += ReadLine;
            }

            try
            {
                JSONObject json = new JSONObject(RawData);
                Boolean ret = json.getBoolean("result");

                if (!ret)
                {
                    // API failure
                    String error = json.getString("error");
                    final String errorDesc = User.GetErrorDesc(error);

                    if (error.equals("loginfail"))
                    {
                        mSettings.Editor.putBoolean(Settings.API_KEYS_VALID, false);
                        mSettings.Editor.commit();
                        API_KEYS_VALID = false;
                        ApiDataTest();
                        return;
                    }
                    if (error != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast t = Toast.makeText(getApplicationContext(), errorDesc, Toast.LENGTH_SHORT);
                                t.show();
                                btnCheckFromBase.setEnabled(true);
                            }
                        });
                    }
                    return;
                }
                if (!json.isNull("data"))
                {
                    bss = json.getJSONObject("data");
                }
            }
            catch (Exception e)
            {
                // JSON error
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast t = Toast.makeText(getApplicationContext(), "Database failure", Toast.LENGTH_SHORT);
                        t.show();
                        btnCheckFromBase.setEnabled(true);
                    }
                });
                return;
            }
        }
        catch (Exception e)
        {
            // Connection error
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast t = Toast.makeText(getApplicationContext(), "No internet connection", Toast.LENGTH_SHORT);
                    t.show();
                    btnCheckFromBase.setEnabled(true);
                }
            });
            return;
        }

        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> ElemWiFi;
        String KeyColor;
        int i = 0;
        for (ScanResult result : WiFiScanResult) {
            APData apdata = GetWiFiKeyByBSSID(bss, result.SSID, result.BSSID.toUpperCase());

            ElemWiFi = new HashMap<String, String>();
            ElemWiFi.put("ESSID", result.SSID);
            ElemWiFi.put("BSSID", result.BSSID.toUpperCase());
            ElemWiFi.put("SIGNAL", getStrSignal(result.level));


            if (apdata.Keys.size() < 1) {
                ElemWiFi.put("KEY", "*[color:gray]*[unknown]");
                ElemWiFi.put("KEYSCOUNT", "*[color:gray]*" + Integer.toString(apdata.Keys.size()));

            } else {
                KeyColor = (apdata.Generated.get(0) ? "*[color:red]*" : "*[color:green]*");
                ElemWiFi.put("KEY", KeyColor + apdata.Keys.get(0));
                ElemWiFi.put("KEYSCOUNT", "*[color:green]*" + Integer.toString(apdata.Keys.size()));
            }

            if (apdata.WPS.size() < 1 || apdata.WPS.get(0) == "")
            {
                ElemWiFi.put("WPS", "*[color:gray]*[unknown]");
            }
            else
            {
                ElemWiFi.put("WPS", "*[color:blue]*" + apdata.WPS.get(0));
            }


            ElemWiFi.put("CAPABILITY", result.capabilities);
            list.add(ElemWiFi);

            ArrayList nodeKey = new ArrayList();
            WiFiKeys.add(i, apdata);
            i++;
        }

        adapter = new WiFiListSimpleAdapter(getActivity(), list, R.layout.row,
                new String[]{"ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"},
                new int[]{R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount});

        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {

                WiFiList.setAdapter(adapter);
                btnCheckFromBase.setEnabled(true);
            }
        }
        ));
    }

    private String WiFiKeyDeleteServiceData(String Key)
    {
        if (Key.length() > 2 && Key.substring(0, 2).equals("*["))
        {
            String stylePref = Key.substring(2, Key.indexOf("]*"));
            Key  = Key.substring(stylePref.length() + 4);
        }
        if (Key == "[request error]" || Key == "[unknown]") return null;
        return Key;
    }

    public APData GetWiFiKeyByBSSID(JSONObject bss, String ESSID, String BSSID)
    {
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<Boolean> gen = new ArrayList<Boolean>();
        ArrayList<String> wpsPins = new ArrayList<String>();

        try {
            boolean Successes = !bss.isNull(BSSID);
            if (Successes)
            {
                JSONArray rows = bss.getJSONArray(BSSID);

                for (int i = 0; i < rows.length(); i++)
                {
                    JSONObject row = rows.getJSONObject(i);
                    keys.add(row.getString("key"));
                    gen.add(false);
                    String wps = row.getString("wps");
                    if (!wps.equals("")) wpsPins.add(wps);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (keys.size() == 0)
        {
            String PassiveKey = PassiveVulnerabilityTest(ESSID, BSSID);
            if (PassiveKey.length() != 0)
            {
                keys.add(PassiveKey);
                gen.add(true);
                wpsPins.add("");
            }
        }

        APData apdata = new APData();
        apdata.BSSID = BSSID;
        apdata.Keys = keys;
        apdata.Generated = gen;
        apdata.WPS = wpsPins;

        return apdata;
    }

    public String PassiveVulnerabilityTest(String ESSID, String BSSID)
    {
        String ret = "";
        if (ESSID.length() > 9)
        {
            if (ESSID.substring(0, 9).equals("MGTS_GPON"))
            {
                ret = BSSID.replace(":", "");
                ret = ret.substring(4, 12);
            }
        }
        return ret;
    }
    private String getStrSignal(int Signal) {
        String Color = "";
        Signal = (100 + Signal) * 2;
        Signal = Math.min(Math.max(Signal, 0), 100);

        if (Signal < 48) Color = "*[color:red]*";
        if (Signal >= 48 && Signal < 65) Color = "*[color:yellow]*";
        if (Signal >= 65) Color = "*[color:greendark]*";

        return Color + Integer.toString(Signal) + "%";
    }
}