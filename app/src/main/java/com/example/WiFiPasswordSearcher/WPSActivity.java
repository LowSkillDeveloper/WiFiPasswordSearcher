package com.example.WiFiPasswordSearcher;

import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.*;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WpsCallback;
import android.net.wifi.WpsInfo;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.json.*;


class WPSPin
{
    public int mode;
    public String name;
    public String pin;
    public Boolean sugg;
}

public class WPSActivity extends Activity
{
    private WebView mWebView;
    private WifiManager WifiMgr;

    ArrayList<ItemWps> data = new ArrayList<ItemWps>();
    ArrayList<WPSPin> pins = new ArrayList<WPSPin>();
    ProgressDialog pd = null;
    private Settings mSettings;
    private WpsCallback wpsCallback;
    private Boolean wpsConnecting = false;
    private String wpsLastPin = "";
    public static String API_READ_KEY = "";

    ArrayList<String> wpsPin = new ArrayList<String>();
    ArrayList<String> wpsMet = new ArrayList<String>();
    ArrayList<String> wpsScore = new ArrayList<String>();
    ArrayList<String> wpsDb = new ArrayList<String>();

    private DatabaseHelper mDBHelper;
    private SQLiteDatabase mDb;
    private volatile boolean wpsReady = false;
    private String cachedPins = "";

    private static final String[] listContextMenuItems = new String[]{
            "Connect using WPS... (without root)",
            "Copy this WPS PIN"
    };

    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wps);

        mDBHelper = new DatabaseHelper(this);
        try
        {
            mDBHelper.updateDataBase();
        }
        catch (IOException mIOException)
        {
            throw new Error("UnableToUpdateDatabase");
        }

        try
        {
            mDb = mDBHelper.getWritableDatabase();
        }
        catch (SQLException mSQLException)
        {
            throw mSQLException;
        }

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mSettings = new Settings(getApplicationContext());

        API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        TextView ESSDWpsText = (TextView)findViewById(R.id.ESSDWpsTextView);
        String ESSDWps = getIntent().getExtras().getString("variable");
        ESSDWpsText.setText(ESSDWps); // ESSID
        TextView BSSDWpsText = (TextView)findViewById(R.id.BSSDWpsTextView);
        final String BSSDWps = getIntent().getExtras().getString("variable1");
        BSSDWpsText.setText(BSSDWps); // BSSID

        WifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wpsCallback = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            wpsCallback = new WpsCallback()
            {
                @Override
                public void onStarted(String pin) {
                    wpsConnecting = true;
                    pd = new ProgressDialog(WPSActivity.this);
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pd.setMessage("Connecting to the network...");
                    pd.setCanceledOnTouchOutside(false);
                    pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            {
                                WifiMgr.cancelWps(wpsCallback);
                            }
                            wpsConnecting = false;
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Connection cancelled", Toast.LENGTH_SHORT).show();
                        }
                    });
                    pd.show();
                }

                @Override
                public void onSucceeded() {
                    if (!wpsConnecting)
                        return;
                    wpsConnecting = false;
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(), "Connected successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(int reason) {
                    if (!wpsConnecting && reason > 2)
                        return;
                    wpsConnecting = false;
                    pd.dismiss();
                    String title = "An error occurred";
                    String errorMessage;
                    switch (reason) {
                        case 0: // Generic failure
                            if (wpsLastPin.isEmpty()) {
                                title = "WPS connection failed";
                                errorMessage = "Your device does not support entering <empty> pin via WPS. You could try again, or use root mode.";
                            }
                            else {
                                errorMessage = "Generic failure - something went wrong. Try turning your Wi-Fi interface off and on, then try again. If this doesn't help, try restarting your device.";
                            }
                            break;
                        case 1: // In progress
                            errorMessage = "Operation currently in progress.";
                            break;
                        case 2: // Busy
                            errorMessage = "Wi-Fi interface is busy.";
                            break;
                        case WifiManager.WPS_OVERLAP_ERROR:
                            errorMessage = "Another WPS transaction is in progress.";
                            break;
                        case WifiManager.WPS_WEP_PROHIBITED:
                            errorMessage = "WEP encryption prohibited.";
                            break;
                        case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                            errorMessage = "TKIP-only encryption prohibited.";
                            break;
                        case WifiManager.WPS_AUTH_FAILURE:
                            errorMessage = "Selected WPS PIN is not correct.";
                            break;
                        case WifiManager.WPS_TIMED_OUT:
                            title = "WPS connection timeout";
                            errorMessage = "The network did not respond " +
                                    "due to low signal or some other reasons. " +
                                    "You may try again.";
                            break;
                        default:
                            title = "OH SHI*";
                            errorMessage = "Unexpected error " + reason;
                            break;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(WPSActivity.this);
                    builder.setTitle(title)
                            .setMessage(errorMessage)
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            };
        }

        ListView wpslist = (ListView)findViewById(R.id.WPSlist);
        wpslist.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id)
            {
                ShowMenu(BSSDWps, wpsPin.get(position));
            }
        });

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.addJavascriptInterface(new myJavascriptInterface(), "JavaHandler");
        mWebView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                final String BSSDWps = getIntent().getExtras().getString("variable1");
                mWebView.loadUrl("javascript:initAlgos();window.JavaHandler.initAlgos(JSON.stringify(algos),'" + BSSDWps + "');");
            }
        });
        mWebView.getSettings().setJavaScriptEnabled(true);
        AppVersion wpspin = new AppVersion(getApplicationContext());
        wpspin.wpsCompanionInit(false);
        String path = wpspin.wpsCompanionGetPath();
        if (path == null)
            path = "/android_asset/wpspin.html";
        mWebView.loadUrl("file://" + path);

        new AsyncInitActivity().execute(BSSDWps);
    }

    private void ShowMenu(final String BSSID, final String pin)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(WPSActivity.this);
        String spin = pin;
        if (spin.length() == 0)
            spin = "<empty>";
        dialogBuilder.setTitle("Selected pin: " + spin);

        dialogBuilder.setItems(listContextMenuItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        if (!WifiMgr.isWifiEnabled())
                        {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Wi-Fi interface is disabled", Toast.LENGTH_SHORT);
                            toast.show();
                            break;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        {
                            WpsInfo wpsInfo = new WpsInfo();
                            wpsInfo.BSSID = BSSID;
                            wpsInfo.pin = pin;
                            wpsInfo.setup = WpsInfo.KEYPAD;

                            wpsLastPin = pin;
                            WifiMgr.startWps(wpsInfo, wpsCallback);
                        }
                        else
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(WPSActivity.this);
                            builder.setTitle("Unsupported Android version")
                                    .setMessage("This function requires Android 5.0 (Lollipop) with API 21 or higher. Please upgrade your system.")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "Pin \"" + pin + "\" copied", Toast.LENGTH_SHORT).show();
                        try
                        {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData dataClip = ClipData.newPlainText("text", pin);
                            clipboard.setPrimaryClip(dataClip);
                        }
                        catch (Exception e) {}
                        break;
                }
            }
        });

        dialogBuilder.show();
    }

    private class AsyncInitActivity extends AsyncTask <String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            pd = ProgressDialog.show(WPSActivity.this, "Please wait...", "Initializing...");
        }

        protected String doInBackground(String[] BSSDWps)
        {
            // get MAC manufacturer
            String BSSID = BSSDWps[0];
            String response2;
            DefaultHttpClient hc2 = new DefaultHttpClient();
            ResponseHandler<String> res2 = new BasicResponseHandler();

            HttpPost http2 = new HttpPost("http://wpsfinder.com/ethernet-wifi-brand-lookup/MAC:" + BSSID);
            try
            {
                response2 = hc2.execute(http2, res2);
                response2 = response2.substring(response2.indexOf("muted'><center>") + 15, response2.indexOf("<center></h4><h6"));
            }
            catch (Exception e)
            {
                response2 = "N/A";
            }

            int wait = 8000;
            while (!wpsReady && wait > 0)
            {
                try
                {
                    Thread.sleep(100);
                    wait -= 100;
                }
                catch (Exception e) {}
            }

            return response2;
        }

        @Override
        protected void onPostExecute(String response2)
        {
            int src = mSettings.AppSettings.getInt(Settings.WPS_SOURCE, 1);
            if (src != 1)
                pd.dismiss();
            TextView VendorWpsText = (TextView)findViewById(R.id.VendorWpsTextView);
            if (response2.length() > 50)
            {
                response2 = "unknown vendor";
            }
            VendorWpsText.setText(response2);

            switch (src)
            {
                case 1:
                    btnwpsbaseclick(null);
                    break;
                case 2:
                    btnGenerate(null);
                    break;
                case 3:
                    btnLocalClick(null);
                    break;
            }
        }
    }

    private class GetPinsFromBase extends AsyncTask <String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            String msg = "Getting pins...";

            if (pd.isShowing())
            {
                pd.setMessage(msg);
            }
            else
            {
                pd = ProgressDialog.show(WPSActivity.this, "Please wait...", msg);
            }
        }

        protected String doInBackground(String[] BSSDWps)
        {
            String BSSID = BSSDWps[0];
            String response;
            data.clear();
            wpsScore.clear();
            wpsDb.clear();
            wpsPin.clear();
            wpsMet.clear();
            DefaultHttpClient hc = new DefaultHttpClient();
            ResponseHandler<String> res = new BasicResponseHandler();

            mSettings.Reload();
            String SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, getResources().getString(R.string.SERVER_URI_DEFAULT));
            HttpGet http = new HttpGet(SERVER_URI + "/api/apiwps?key=" + API_READ_KEY + "&bssid=" + BSSID);
            try
            {
                if (cachedPins.isEmpty())
                    response = hc.execute(http, res);
                else
                    response = cachedPins;

                try
                {
                    JSONObject jObject = new JSONObject(response);
                    Boolean result = jObject.getBoolean("result");

                    if (result)
                    {
                        cachedPins = response;
                        try
                        {
                            jObject = jObject.getJSONObject("data");
                            jObject = jObject.getJSONObject(BSSID);

                            JSONArray array = jObject.optJSONArray("scores");
                            for (int i = 0; i < array.length(); i++)
                            {
                                jObject = array.getJSONObject(i);
                                wpsPin.add(jObject.getString("value"));
                                wpsMet.add(jObject.getString("name"));
                                wpsScore.add(jObject.getString("score"));
                                wpsDb.add(jObject.getBoolean("fromdb") ? "✔" : "");
                                Integer score = Math.round(Float.parseFloat(wpsScore.get(i)) * 100);
                                wpsScore.set(i, Integer.toString(score) + "%");

                                data.add(new ItemWps(wpsPin.get(i), wpsMet.get(i), wpsScore.get(i), wpsDb.get(i)));
                            }
                        }
                        catch (Exception e) {}
                    }
                    else
                    {
                        String error = jObject.getString("error");

                        if (error.equals("loginfail"))
                        {
                            mSettings.Editor.putBoolean(Settings.API_KEYS_VALID, false);
                            mSettings.Editor.commit();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast t = Toast.makeText(getApplicationContext(), "Please enter credentials", Toast.LENGTH_SHORT);
                                    t.show();
                                }
                            });
                            Intent startActivity = new Intent(getApplicationContext(), StartActivity.class);
                            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(startActivity);
                        }
                        response = "api_error";
                    }
                }
                catch (JSONException e)
                {
                    response = "json_error";
                }
            }
            catch (Exception e)
            {
                response = "http_error";
            }
            return response;
        }

        @Override
        protected void onPostExecute(String str)
        {
            pd.dismiss();
            ListView wpslist = (ListView)findViewById(R.id.WPSlist);
            String msg = "";
            Boolean toast = true;
            if (str.equals("http_error"))
            {
                msg = "No internet connection";
                toast = false;
            }
            else if (str.equals("json_error"))
            {
                msg = "Connection failure";
                toast = false;
            }
            else if (str.equals("api_error"))
            {
                msg = "Database failure";
                toast = false;
            }
            else if (data.isEmpty())
            {
                msg = "No pins found";
            }
            if (msg.length() > 0)
            {
                data.add(new ItemWps(null, msg, null, null));
            }
            wpslist.setEnabled(msg.length() == 0);

            wpslist.setAdapter(new MyAdapterWps(WPSActivity.this, data));
            if (toast) toastMessage("Selected source: 3WiFi Online WPS PIN");
        }
    }

    public void btnwpsbaseclick(View view)
    { //пины из базы
        findViewById(R.id.baseButton).getBackground().setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY);
        findViewById(R.id.wpsButton1).getBackground().clearColorFilter();
        findViewById(R.id.wpsButton2).getBackground().clearColorFilter();
        mSettings.Editor.putInt(Settings.WPS_SOURCE, 1);
        mSettings.Editor.commit();
        String BSSDWps = getIntent().getExtras().getString("variable1");
        new GetPinsFromBase().execute(BSSDWps);
    }

    private class myJavascriptInterface
    {
        @JavascriptInterface
        public void initAlgos(String json, String bssid)
        {
            pins.clear();
            try
            {
                JSONArray arr = new JSONArray(json);

                for (int i = 0; i < arr.length(); i++)
                {
                    JSONObject obj = arr.getJSONObject(i);

                    WPSPin pin = new WPSPin();
                    pin.mode = obj.getInt("mode");
                    pin.name = obj.getString("name");
                    pins.add(pin);
                }
                mWebView.loadUrl("javascript:window.JavaHandler.getPins(1,JSON.stringify(pinSuggestAPI(true,'" + bssid + "',null)), '" + bssid + "');");
            }
            catch (JSONException e)
            {
                wpsReady = true;
            }
        }

        @JavascriptInterface
        public void getPins(int all, String json, String bssid)
        {
            try
            {
                JSONArray arr = new JSONArray(json);

                for (int i = 0; i < arr.length(); i++)
                {
                    JSONObject obj = arr.getJSONObject(i);
                    if (all > 0)
                    {
                        WPSPin pin = pins.get(i);
                        pin.pin = obj.getString("pin");
                        pin.sugg = false;
                    }
                    else
                    {
                        WPSPin pin = pins.get(obj.getInt("algo"));
                        pin.sugg = true;
                    }
                }
                if (all > 0)
                    mWebView.loadUrl("javascript:window.JavaHandler.getPins(0,JSON.stringify(pinSuggestAPI(false,'" + bssid + "',null)), '');");
                else
                    wpsReady = true;
            }
            catch (JSONException e)
            {
                pins.clear();
                wpsReady = true;
            }
        }
    }

    public void btnGenerate(View view)
    { //генераторpppppp
        findViewById(R.id.wpsButton1).getBackground().setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY);
        findViewById(R.id.baseButton).getBackground().clearColorFilter();
        findViewById(R.id.wpsButton2).getBackground().clearColorFilter();
        mSettings.Editor.putInt(Settings.WPS_SOURCE, 2);
        mSettings.Editor.commit();
        ListView wpslist = (ListView) findViewById(R.id.WPSlist);
        wpslist.setAdapter(null);
        wpsPin.clear();
        wpsMet.clear();
        data.clear();

        for (WPSPin pin : pins)
        {
            if (!pin.sugg) continue;
            wpsPin.add(pin.pin);
            wpsMet.add(pin.name);
            data.add(new ItemWps(
                    pin.pin.equals("") ? "<empty>" : pin.pin,
                    pin.name,
                    pin.mode == 3 ? "STA" : "",
                    "✔"
            ));
        }
        for (WPSPin pin : pins)
        {
            if (pin.sugg) continue;
            wpsPin.add(pin.pin);
            wpsMet.add(pin.name);
            data.add(new ItemWps(
                    pin.pin.equals("") ? "<empty>" : pin.pin,
                    pin.name,
                    pin.mode == 3 ? "STA" : "",
                    ""
            ));
        }
        wpslist.setEnabled(pins.size() > 0);
        wpslist.setAdapter(new MyAdapterWps(WPSActivity.this, data));
        toastMessage("Selected source: WPS PIN Generator");
    }

    private int findAlgoByPin(String pin)
    {
        int i = 0;
        for (WPSPin p : pins)
        {
            if (pin.equals(p.pin))
                return i;
            i++;
        }
        return -1;
    }

    private int findAlgoByName(String name)
    {
        int i = 0;
        for (WPSPin p : pins)
        {
            if (name.equals(p.name))
                return i;
            i++;
        }
        return -1;
    }

    public void btnLocalClick(View view)
    { //локальная база
        findViewById(R.id.wpsButton2).getBackground().setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY);
        findViewById(R.id.wpsButton1).getBackground().clearColorFilter();
        findViewById(R.id.baseButton).getBackground().clearColorFilter();
        mSettings.Editor.putInt(Settings.WPS_SOURCE, 3);
        mSettings.Editor.commit();
        ListView wpslist = (ListView) findViewById(R.id.WPSlist);
        wpslist.setAdapter(null);
        final String BSSDWps = getIntent().getExtras().getString("variable1");

        try
        {
            data.clear();
            wpsPin.clear();
            Cursor cursor = mDb.rawQuery("SELECT * FROM pins WHERE mac='" + BSSDWps.substring(0, 8) + "'", null);
            cursor.moveToFirst();

            do {
                String p = cursor.getString(0);
                if (p.equals("vacante"))
                    p = ""; // empty pin
                int idx = findAlgoByPin(p);

                if (idx == -1)
                {
                    if (p.equals("airocon"))
                        idx = findAlgoByName("Airocon Realtek");
                    else if (p.equals("arcady"))
                        idx = findAlgoByName("Livebox Arcadyan");
                    else if (p.equals("asus"))
                        idx = findAlgoByName("ASUS PIN");
                    else if (p.equals("dlink"))
                        idx = findAlgoByName("D-Link PIN");
                    else if (p.equals("dlink1"))
                        idx = findAlgoByName("D-Link PIN +1");
                    else if (p.equals("thirtytwo"))
                        idx = findAlgoByName("32-bit PIN");
                    //else if (p.equals("trend"))
                    //    idx = findAlgoByName(""); // unknown
                    else if (p.equals("twentyeight"))
                        idx = findAlgoByName("28-bit PIN");
                    else if (p.equals("zhao"))
                        idx = findAlgoByName("24-bit PIN");

                    if (idx > -1)
                    {
                        WPSPin algo = pins.get(idx);
                        p = algo.pin;
                    }
                }

                if (idx > -1)
                {
                    WPSPin algo = pins.get(idx);
                    data.add(new ItemWps(
                        p.equals("") ? "<empty>" : p,
                        algo.name,
                        algo.mode == 3 ? "STA" : "",
                        ""
                    ));
                }
                else
                {
                    data.add(new ItemWps(
                            p.equals("") ? "<empty>" : p,
                            "Unknown",
                            p.matches("[0-9]+") ? "STA" : "",
                            ""
                    ));
                }
                wpsPin.add(p);
            }
            while(cursor.moveToNext());
            cursor.close();
            wpslist.setEnabled(true);
        }
        catch (Exception e)
        {
            data.add(new ItemWps(null, "No pins found", null, null));
            wpslist.setEnabled(false);
        }
        wpslist.setAdapter(new MyAdapterWps(WPSActivity.this, data));

        toastMessage("Selected source: WPA WPS TESTER");
    }

    public void btnCustomPin(View view)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Enter your WPS PIN:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String BSSDWps = getIntent().getExtras().getString("variable1");
                String pin = input.getText().toString();
                ShowMenu(BSSDWps, pin);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        alert.show();
    }

    //Toast
    public void toastMessage(String text)
    {
        Toast toast = Toast.makeText(getApplicationContext(),
                                     text, Toast.LENGTH_LONG);
        toast.show();
    }
}
