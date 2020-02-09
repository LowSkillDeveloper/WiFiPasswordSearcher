package com.example.WiFiPasswordSearcher;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by пк on 20.12.2015.
 */
public class UserManager {
    private Settings mSettings = null;
    private String APP_VERSION = "";
    private String API_READ_KEY = "";

    public String Login = "";
    public String NickName = "";
    public String RegDate = "";
    public Integer Level = -1;

    UserManager(Context context) {
        APP_VERSION = context.getResources().getString(R.string.app_version);
        mSettings = new Settings(context);
        API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");
        Login = mSettings.AppSettings.getString(Settings.APP_SERVER_LOGIN, "");
    }

    public void getFromSettings()
    {
        NickName = mSettings.AppSettings.getString(Settings.USER_NICK, "");
        RegDate = mSettings.AppSettings.getString(Settings.USER_REGDATE, "");
        Level = mSettings.AppSettings.getInt(Settings.USER_GROUP, -1);
    }

    public String GetGroup()
    {
        return GetTextGroup(Level);
    }

    public String GetTextGroup(Integer Level)
    {
        switch (Level)
        {
            case -2: return "Banned";
            case -1: return "No logged";
            case 0: return "Guest";
            case 1: return "User";
            case 2: return "Developer";
            case 3: return "Administrator";
        }
        return "";
    }

    public String GetErrorDesc(String error)
    {
        if (error.equals("database"))
            return "Database maintenance, try again later";
        if (error.equals("loginfail"))
            return "Username or password incorrect";
        if (error.equals("cooldown"))
            return "Сooldown, please wait 10 seconds";
        if (error.equals("form"))
            return "Please fill required form fields";
        return "Unknown Error: " + error;
    }

}
