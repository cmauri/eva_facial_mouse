package org.opencv.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.util.Log;

import com.crea_si.eviacam.R;

/**
 * Basic implementation of LoaderCallbackInterface.
 */
public abstract class MyBaseLoaderCallback implements LoaderCallbackInterface {

    public MyBaseLoaderCallback(Context AppContext) {
        mAppContext = AppContext;
    }

    public void onManagerConnected(int status)
    {
        Resources res= mAppContext.getResources();

        switch (status)
        {
            /** OpenCV initialization was successful. **/
            case LoaderCallbackInterface.SUCCESS:
            {
                /** Application must override this method to handle successful library initialization. **/
            } break;
            /** OpenCV loader can not start Google Play Market. **/
            case LoaderCallbackInterface.MARKET_ERROR:
            {
                Log.e(TAG, "Package installation failed!");
                AlertDialog MarketErrorMessage = new AlertDialog.Builder(mAppContext).create();
                MarketErrorMessage.setTitle("OpenCV Manager");
                MarketErrorMessage.setMessage(res.getText(R.string.opencv_package_installation_failed));
                MarketErrorMessage.setCancelable(false); // This blocks the 'BACK' button
                MarketErrorMessage.setButton(
                        AlertDialog.BUTTON_POSITIVE,
                        res.getText(android.R.string.ok), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                MarketErrorMessage.show();
            } break;
            /** Package installation has been canceled. **/
            case LoaderCallbackInterface.INSTALL_CANCELED:
            {
                Log.d(TAG, "OpenCV library instalation was canceled by user");
                finish();
            } break;
            /** Application is incompatible with this version of OpenCV Manager. Possibly, a service update is required. **/
            case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
            {
                Log.d(TAG, "OpenCV Manager Service is uncompatible with this app!");
                AlertDialog IncomatibilityMessage = new AlertDialog.Builder(mAppContext).create();
                IncomatibilityMessage.setTitle("OpenCV Manager");
                IncomatibilityMessage.setMessage(res.getText(R.string.opencv_incompatible));
                IncomatibilityMessage.setCancelable(false); // This blocks the 'BACK' button
                IncomatibilityMessage.setButton(AlertDialog.BUTTON_POSITIVE,
                        res.getText(android.R.string.ok), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                IncomatibilityMessage.show();
            } break;
            /** Other status, i.e. INIT_FAILED. **/
            default:
            {
                Log.e(TAG, "OpenCV loading failed!");
                AlertDialog InitFailedDialog = new AlertDialog.Builder(mAppContext).create();
                InitFailedDialog.setTitle("OpenCV error");
                InitFailedDialog.setMessage(res.getText(R.string.opencv_init_error));
                InitFailedDialog.setCancelable(false); // This blocks the 'BACK' button
                InitFailedDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                        res.getText(android.R.string.ok), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

                InitFailedDialog.show();
            } break;
        }
    }

    public void onPackageInstall(final int operation, final InstallCallbackInterface callback)
    {
        Resources res= mAppContext.getResources();

        switch (operation)
        {
            case InstallCallbackInterface.NEW_INSTALLATION:
            {
                AlertDialog InstallMessage = new AlertDialog.Builder(mAppContext).create();
                InstallMessage.setTitle(res.getText(R.string.opencv_installation));
                InstallMessage.setMessage(res.getText(R.string.opencv_installation_summary));
                InstallMessage.setCancelable(false); // This blocks the 'BACK' button
                InstallMessage.setButton(AlertDialog.BUTTON_POSITIVE,
                        res.getText(android.R.string.yes), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        callback.install();
                    }
                });

                InstallMessage.setButton(AlertDialog.BUTTON_NEGATIVE,
                        res.getText(android.R.string.no), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        callback.cancel();
                    }
                });

                InstallMessage.show();
            } break;
            case InstallCallbackInterface.INSTALLATION_PROGRESS:
            {
                AlertDialog WaitMessage = new AlertDialog.Builder(mAppContext).create();
                WaitMessage.setTitle(R.string.opencv_not_ready);
                WaitMessage.setMessage(res.getText(R.string.opencv_installation_in_progress));
                WaitMessage.setCancelable(false); // This blocks the 'BACK' button
                WaitMessage.setButton(AlertDialog.BUTTON_POSITIVE,
                        res.getText(R.string.opencv_wait), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callback.wait_install();
                    }
                });
                WaitMessage.setButton(AlertDialog.BUTTON_NEGATIVE,
                        res.getText(android.R.string.cancel), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        callback.cancel();
                    }
                });

                WaitMessage.show();
            } break;
        }
    }

    void finish()
    {
        ((Activity) mAppContext).finish();
    }

    protected Context mAppContext;
    private final static String TAG = "MyBaseLoaderCallback";
}
