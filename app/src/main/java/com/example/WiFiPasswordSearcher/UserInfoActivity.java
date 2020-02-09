package com.example.WiFiPasswordSearcher;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by пк on 20.12.2015.
 */
public class UserInfoActivity extends Activity {

    public TextView txtLogin = null;
    public TextView txtRegDate = null;
    public TextView txtGroup = null;
    private String info;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user);

        Settings mSettings = new Settings(getApplicationContext());

        try {
            info = getIntent().getExtras().getString("showInfo");
        }
        catch (Exception e) {
            info = "user";
        }

        txtLogin = (TextView) findViewById(R.id.txtLogin);
        txtRegDate = (TextView) findViewById(R.id.txtRegDate);
        txtGroup = (TextView) findViewById(R.id.txtGroup);

        String Nick, Group;
        Date date;

        if (info != null && info.equals("wpspin"))
        {
            AppVersion updater = new AppVersion(getApplicationContext());
            updater.wpsCompanionInit(false);

            LinearLayout lButtons = (LinearLayout) findViewById(R.id.buttonsLayout);
            lButtons.setVisibility(LinearLayout.VISIBLE);
            Button btnRevert = (Button) findViewById(R.id.btnRevert);
            btnRevert.setEnabled(!updater.wpsCompanionInternal());

            TextView lReg = (TextView) findViewById(R.id.labRegDate);
            TextView lGroup = (TextView) findViewById(R.id.labGroup);

            Nick = "WPS PIN Generator";
            lReg.setText("Last Updated");
            date = updater.wpsCompanionGetDate();
            lGroup.setText("File Size");
            long size = updater.wpsCompanionGetSize();
            Group = updater.readableFileSize(size);
        }
        else
        {
            UserManager User = new UserManager(getApplicationContext());
            User.getFromSettings();

            LinearLayout lButtons = (LinearLayout) findViewById(R.id.buttonsLayout);
            lButtons.setVisibility(LinearLayout.GONE);

            SimpleDateFormat format = new SimpleDateFormat(getResources().getString(R.string.DEFAULT_DATE_FORMAT), Locale.US);
            try {
                date = format.parse(User.RegDate);
            } catch (Exception e) {
                date = new Date();
            }

            Nick = User.NickName;
            Group = User.GetGroup();
        }

        txtLogin.setText(Nick);
        txtRegDate.setText(DateFormat.getDateTimeInstance().format(date));
        txtGroup.setText(Group);
    }

    private class AsyncWpsUpdater extends AsyncTask<String, Void, String>
    {
        private ProgressDialog pd;
        private Toast toast;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            pd = new ProgressDialog(UserInfoActivity.this);
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setMessage("Updating component...");
            pd.setCanceledOnTouchOutside(false);
            pd.show();
        }

        protected String doInBackground(String[] input)
        {
            DefaultHttpClient hc = new DefaultHttpClient();
            ResponseHandler<String> res = new BasicResponseHandler();

            Settings mSettings = new Settings(getApplicationContext());
            mSettings.Reload();
            String SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, getResources().getString(R.string.SERVER_URI_DEFAULT));
            HttpGet http = new HttpGet(SERVER_URI + "/wpspin");
            String str = "";
            try
            {
                str = hc.execute(http, res);
            }
            catch (Exception e) {}

            return str;
        }

        @Override
        protected void onPostExecute(String str)
        {
            String msg;
            pd.dismiss();

            if (str.contains("initAlgos();"))
            {
                AppVersion updater = new AppVersion(getApplicationContext());
                updater.wpsCompanionUpdate(str, new Date());
                txtRegDate.setText(DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate()));
                txtGroup.setText(updater.readableFileSize(updater.wpsCompanionGetSize()));
                Button btnRevert = (Button) findViewById(R.id.btnRevert);
                btnRevert.setEnabled(!updater.wpsCompanionInternal());
                msg = "Update successful!";
            }
            else if (str.length() == 0)
            {
                msg = "No internet connection";
            }
            else
            {
                msg = "Update failed";
            }
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void btnUpdateOnClick(View v)
    {
        if (info != null && info.equals("wpspin"))
        {
            new AsyncWpsUpdater().execute();
        }
    }

    public void btnRevertOnClick(View v)
    {
        if (info != null && info.equals("wpspin"))
        {
            AppVersion updater = new AppVersion(getApplicationContext());
            updater.wpsCompanionInit(true);
            txtRegDate.setText(DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate()));
            txtGroup.setText(updater.readableFileSize(updater.wpsCompanionGetSize()));
            v.setEnabled(!updater.wpsCompanionInternal());
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Reverted to initial state", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
