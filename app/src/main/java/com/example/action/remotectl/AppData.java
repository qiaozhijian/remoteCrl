package com.example.action.remotectl;

import android.content.Context;
import android.util.Log;

/**
 * Created by Summer on 2017/10/27.
 */

public class AppData {
    private String KEY_SETTING_SPEED = "key_setting_SPEED";
    private String KEY_SETTING_ANGLE = "key_setting_ANGLE";
    private String KEY_SETTING_CIRCLE = "key_setting_CIRCLE";
    private String KEY_SETTING_TESTENV = "key_setting_testenv";

    //在这里进行扩展，加入键，然后自动生成getter和setter函数，就达到了扩展的目的
    public AppData(Context context) {
        SharedPreferencesHelper.init(context);
    }

    public void setSettingSPEED(float bFlag) {
        SharedPreferencesHelper.getInstance().saveData(
                KEY_SETTING_SPEED, bFlag);
    }

    public float getSettingSPEED() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_SPEED, 1.2f);
    }

    public void setSettingANGLE(float bFlag) {

        SharedPreferencesHelper.getInstance()
                .saveData(KEY_SETTING_ANGLE, bFlag);
    }

    public float getSettingANGLE() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_ANGLE, 1.2f);
    }


    public void setSettingCIRCLE(float bFlag) {

        SharedPreferencesHelper.getInstance().saveData(KEY_SETTING_CIRCLE,
                bFlag);
    }

    public float getSettingCIRCLE() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_CIRCLE, 1.2f);
    }

    public void setTestEnvSetting(boolean bFlag) {
        Log.d("imdata", "set testenv:" + bFlag);
        SharedPreferencesHelper.getInstance().saveData(KEY_SETTING_TESTENV,
                bFlag);
    }

    public boolean getTestEnvSetting() {
        return (Boolean) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_TESTENV, false);
    }
}