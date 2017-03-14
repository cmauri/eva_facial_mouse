/*
* Enable Viacam for Android, a camera based mouse emulator
*
* Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.crea_si.eviacam.common;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TechInfoActivity extends Activity {

    /**
     * Read the last max_tail_lines from the logcat
     * @param max_tail_lines max lines or 0 if all
     * @return string with the logcat output
     */
    private String readLogcat(int max_tail_lines) {
        StringBuilder log= new StringBuilder();

        try {
            int skip_lines= 0;
            if (max_tail_lines> 0) {
                int line_count= 0;

                // Need to count lines first
                Process process = Runtime.getRuntime().exec("logcat -d");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                while (bufferedReader.readLine() != null) {
                    line_count++;
                }

                if (line_count> max_tail_lines) {
                    skip_lines= line_count - max_tail_lines;
                }
            }

            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (skip_lines> 0) {
                    skip_lines--;
                }
                else {
                    log.append(line).append("\n");
                }
            }
        }
        catch (IOException ignored) {
        }

        return log.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_info);

        TextView tv= (TextView) findViewById(R.id.tech_info_text);
        tv.setMovementMethod(new ScrollingMovementMethod());
        String info= "";

        info+= getResources().getText(R.string.app_name) + " " + BuildConfig.VERSION_NAME;
        info+= "\nVERSION_CODE: " + BuildConfig.VERSION_CODE;
        info+= "\nBUILD_TYPE: " + BuildConfig.BUILD_TYPE;
        //info+= "\nFLAVOR: " + BuildConfig.FLAVOR;
        //info+= "\nDEBUG: " + BuildConfig.DEBUG;
        info+= "\n";

        info+= "\nMANUFACTURER: " + Build.MANUFACTURER;
        info+= "\nMODEL: " + Build.MODEL;
        info+= "\nVERSION: " + Build.VERSION.RELEASE;
        info+= "\nAPI: " + Build.VERSION.SDK_INT;
        info+= "\nDEVICE: " + Build.DEVICE;
        info+= "\nID: " + Build.ID;
        info+= "\nFINGERPRINT: " + Build.FINGERPRINT;
        info+= "\nBRAND: " + Build.BRAND;
        info+= "\nHARDWARE: " + Build.HARDWARE;
        info+= "\nPRODUCT: " + Build.PRODUCT;
        info+= "\nBOARD: " + Build.BOARD;
        info+= "\nLOGCAT:\n" + readLogcat(150);

        //info+= "\nSERIAL: " + Build.SERIAL;
        //info+= "\nDISPLAY: " + Build.DISPLAY;
        //info+= "\nTAGS: " + Build.TAGS;
        //info+= "\nTYPE: " + Build.TYPE;
        //info+= "\nUSER: " + Build.USER;
        //info+= "\nTIME: " + Build.TIME;
        //info+= "\nHOST: " + Build.HOST;
        //info+= "\nBOOTLOADER: " + Build.BOOTLOADER;

        tv.setText(info);

        final String fInfo= info;

        /*
         * Send button
         */
        Button b= (Button) findViewById(R.id.send_button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                CharSequence[] recipients= new String[1];
                recipients[0]= "eva.facial.mouse@gmail.com";
                i.putExtra(Intent.EXTRA_EMAIL  , recipients);
                i.putExtra(Intent.EXTRA_SUBJECT, String.format("%1$s: device details",
                                                 getResources().getText(R.string.app_name)));
                i.putExtra(Intent.EXTRA_TEXT, fInfo);
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(TechInfoActivity.this, "There are no email clients installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
