package com.oriahulrich.perusalwithspritz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.devspark.robototextview.widget.RobotoTextView;

public class DialogAboutFragment extends DialogFragment {

    private static final String TAG = "ABOUT DIALOG: ";

    public DialogAboutFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.fragment_dialog_about, null);
        builder.setView(rootView);

        RobotoTextView titleAndVersionView = (RobotoTextView) rootView.findViewById(R.id.about_title);
        String version;
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            version = "1.0";
        }
        titleAndVersionView.setText(getString(R.string.about_app_name) + ", " + version );

        TextView aboutBodyView = (TextView) rootView.findViewById(R.id.about_body);
        aboutBodyView.setText(Html.fromHtml(getString(R.string.about_body)));
        aboutBodyView.setMovementMethod(new LinkMovementMethod());

        builder.setPositiveButton(R.string.about_dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }
}
