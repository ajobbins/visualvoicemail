package au.com.wallaceit.voicemail.service;
/*
 * Copyright 2013 Michael Boyde Wallace (http://wallaceit.com.au)
 * This file is part of Visual Voicemail.
 *
 * Visual Voicemail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Visual Voicemail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Visual Voicemail (COPYING). If not, see <http://www.gnu.org/licenses/>.
 *
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import au.com.wallaceit.voicemail.Preferences;
import au.com.wallaceit.voicemail.VisualVoicemail;
import au.com.wallaceit.voicemail.activity.setup.AccountSettings;

public class MissedCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){

        SharedPreferences prefs =  Preferences.getPreferences(context).getPreferences();
        SharedPreferences.Editor editor = prefs.edit();

        // Get current phone state
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (VisualVoicemail.DEBUG)
            Log.i(VisualVoicemail.LOG_TAG, "Call state broadcast received: " + state);

        if(state==null)
            return;

        //phone is ringing
        if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            editor.putBoolean("phoneCallRinging", true);
        }

        //phone is received
        if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            editor.putBoolean("phoneCallReceived", true);
        }

        // phone is idle
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
            // detect missed call
            if(prefs.getBoolean("phoneCallRinging", false) && !prefs.getBoolean("phoneCallReceived", false)){
                if (VisualVoicemail.DEBUG)
                    Log.i(VisualVoicemail.LOG_TAG, "Missed call detected...");
                // TODO: If the broadcast receivers do what they are suppose to (ie activate/deactivate) this may be redundant.
                boolean isEnabled = Preferences.getPreferences(context).getAccounts().get(0).getAutomaticCheckMethod() == AccountSettings.PREFERENCE_AUTO_CHECK_MISSED_CALL;
                if (isEnabled) {
                    if (VisualVoicemail.DEBUG)
                        Log.i(VisualVoicemail.LOG_TAG, "Missed call check enabled, scheduling check");
                    PollTask pollTask = new PollTask(context);
                    Timer timer = new Timer();
                    timer.schedule(pollTask, 180000);
                }

                editor.putBoolean("phoneCallRinging", false);
                editor.putBoolean("phoneCallReceived", false);
            }
        }
        editor.commit();
    }

    class PollTask extends TimerTask {
        private Context mContext;

        public PollTask(Context context){
            mContext = context;
        }

        @Override
        public void run() {
            if (VisualVoicemail.DEBUG)
                Log.i(VisualVoicemail.LOG_TAG, "***** Missed Call Receiver *****: checking mail");
            MailService.actionCheck(mContext, null, true);
        }
    }
}