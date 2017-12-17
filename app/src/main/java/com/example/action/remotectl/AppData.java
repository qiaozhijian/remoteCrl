package com.example.action.remotectl;

import android.content.Context;
import android.util.Log;

/**
 * Created by Summer on 2017/10/27.
 */

public class AppData {
    private String KEY_SETTING_STEER = "key_setting_STEER";
    private String KEY_SETTING_PITCH = "key_setting_PITCH";
    private String KEY_SETTING_GAS = "key_setting_GAS";

    //在这里进行扩展，加入键，然后自动生成getter和setter函数，就达到了扩展的目的
    public AppData(Context context) {
        SharedPreferencesHelper.init(context);
    }

    public void setSettingSTEER(float bFlag) {
        SharedPreferencesHelper.getInstance().saveData(
                KEY_SETTING_STEER, bFlag);
    }

    public float getSettingSTEER() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_STEER, 1.2f);
    }

    public void setSettingPITCH(float bFlag) {

        SharedPreferencesHelper.getInstance()
                .saveData(KEY_SETTING_PITCH, bFlag);
    }

    public float getSettingPITCH() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_PITCH, 1.2f);
    }

    public void setSettingGAS(float bFlag) {

        SharedPreferencesHelper.getInstance()
                .saveData(KEY_SETTING_GAS, bFlag);
    }

    public float getSettingGAS() {
        return (float) SharedPreferencesHelper.getInstance().getData(
                KEY_SETTING_GAS, 1.2f);
    }

}