package fr.xtof54.jsgo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import fr.xtof54.dragonGoApp.R;

public class GUI {
	private GUI() {}
	private static GUI gui = new GUI();
    private static WaitDialogFragment waitdialog;
    private static boolean isWaitingDialogShown = false;

	public static GUI getGUI() {
		return gui;
	}

	//	public void showMain() {
	//		GoJsActivity.main.setContentView(R.layout.activity_main);
	//	}

	public void showHome() {
		GoJsActivity.main.initGUI();
	}
	
	public static synchronized void hideWaitingWin() {
	    if (isWaitingDialogShown) {
	        waitdialog.dismiss();
	        isWaitingDialogShown=false;
	    }
	}
	public static synchronized void showWaitingWin() {
        try {
            if (!isWaitingDialogShown) {
                waitdialog = new WaitDialogFragment();
                waitdialog.show(GoJsActivity.main.getSupportFragmentManager(),"waiting");
                isWaitingDialogShown=true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    private static class WaitDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.waiting, null))
            // Add action buttons
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    WaitDialogFragment.this.getDialog().cancel();
                    // TODO stop thread
                }
            });
            return builder.create();
        }
    }
}
