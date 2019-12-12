package com.example.WiFiPasswordSearcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppVersion
{
    private Context context;
    private Float ActualyVersion;
    private String WhatNews;
    private Boolean LoadSuccesses = false;
    private String wpsInternalDate = "2019-12-12 14:00:40";
    private Settings mSettings;

    public AppVersion(Context _context)
    {
        context = _context;
        ActualyVersion = 0f;
        WhatNews = "";

        mSettings = new Settings(context);
    }

    public void ShowUpdateDialog(Activity activity)
    {
        AlertDialog.Builder ad = new AlertDialog.Builder(activity);

        ad.setTitle("New version " + ActualyVersion + " available!");
        ad.setMessage(WhatNews);
        ad.setCancelable(false);
        ad.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                mSettings.Reload();
                String SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, context.getResources().getString(R.string.SERVER_URI_DEFAULT));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SERVER_URI + "/api/app.latest.apk"));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            }
        });

        ad.setNegativeButton("Ask later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });

        ad.show();
    }

    private void GetActualyVersion() throws IOException
    {
        String Args = "/api/ajax.php?Query=AppVersion";
        BufferedReader Reader = null;
        String ReadLine = "";
        String RawData = "";

        try {
            mSettings.Reload();
            String SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, context.getResources().getString(R.string.SERVER_URI_DEFAULT));
            URL Uri = new URL(SERVER_URI + Args);

            HttpURLConnection Connection = (HttpURLConnection) Uri.openConnection();
            Connection.setRequestMethod("GET");
            Connection.setReadTimeout(10 * 1000);
            Connection.connect();

            Reader = new BufferedReader(new InputStreamReader(Connection.getInputStream()));

            while ((ReadLine = Reader.readLine()) != null) {
                RawData += ReadLine;
            }
            try
            {
                JSONObject Json = new JSONObject(RawData);
                Boolean Successes = Json.getBoolean("Successes");
                LoadSuccesses = Successes;
                if (LoadSuccesses)
                {
                    ActualyVersion = (float)Json.getDouble("ActualyVersion");
                    WhatNews = Json.getString("WhatNews");
                    return;
                }

            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

        }
        finally
        {

        }
    }

    public Boolean isActualyVersion(Context context, Boolean showMessage)
    {
        if (showMessage) LoadSuccesses = false;

        if (!LoadSuccesses) try {
            GetActualyVersion();
        } catch (IOException e) {
            LoadSuccesses = false;
        }
        if (!LoadSuccesses)
        {
            if (showMessage)
            {
                Toast t = Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT);
                t.show();
            }
            return true;
        }
        Float CurVersion = Float.parseFloat(context.getResources().getString(R.string.app_version));
        if (CurVersion >= ActualyVersion)
        {
            if (showMessage)
            {
                Toast t = Toast.makeText(context, "Using latest version", Toast.LENGTH_SHORT);
                t.show();
            }
            return true;
        }
        return false;
    }

    private Boolean wpsCompanionExists()
    {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/wpspin.html");
        return file.exists();
    }

    public void wpsCompanionInit(Boolean force)
    {
        if (wpsCompanionExists() && !force)
            return;

        String filename = "wpspin.html";
        try
        {
            InputStream in = context.getAssets().open(filename);
            int size = in.available();
            byte[] data = new byte[size];
            in.read(data);
            in.close();
            String str = new String(data, "UTF-8");

            Date date;
            SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.DEFAULT_DATE_FORMAT), Locale.US);
            try {
                date = format.parse(wpsInternalDate);
            } catch (Exception e) {
                date = new Date();
            }
            wpsCompanionUpdate(str, date);
        }
        catch (Exception e) {}
    }

    public void wpsCompanionUpdate(String str, Date date)
    {
        File outFile = new File(context.getFilesDir().getAbsolutePath() + "/wpspin.html");

        try
        {
            OutputStream out = new FileOutputStream(outFile);
            str = str.replace("a.filter((n) => b.includes(n));", "a;");
            byte[] data = str.getBytes(Charset.forName("UTF-8"));
            out.write(data);
            out.close();
            outFile.setLastModified(date.getTime());
        }
        catch (Exception e) {}
    }

    public String wpsCompanionGetPath()
    {
        if (!wpsCompanionExists())
            return null;

        return context.getFilesDir().getAbsolutePath() + "/wpspin.html";
    }

    public Date wpsCompanionGetDate()
    {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/wpspin.html");
        Date date = new Date();
        date.setTime(file.lastModified());
        return date;
    }

    public long wpsCompanionGetSize()
    {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/wpspin.html");
        return file.length();
    }

    public Boolean wpsCompanionInternal()
    {
        Date date;
        SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.DEFAULT_DATE_FORMAT), Locale.US);
        try {
            date = format.parse(wpsInternalDate);
        } catch (Exception e) {
            date = new Date();
        }
        return date.compareTo(wpsCompanionGetDate()) == 0;
    }

    public String readableFileSize(long size)
    {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "KiB", "MiB", "GiB", "TiB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
