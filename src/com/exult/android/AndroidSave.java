package com.exult.android;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

/*
 * A native Android save/restore screen.
 */
public class AndroidSave extends GameSingletons {
	private View myView, mainView;
	
	public AndroidSave(Activity exult) {
		myView = exult.findViewById(R.id.save_restore);
		mainView = exult.findViewById(R.id.main_layout);
		mainView.setVisibility(View.INVISIBLE);
		myView.setVisibility(View.VISIBLE);
		setButtonHandlers(exult);
	}
	private void setButtonHandlers(Activity exult) {
		Button button;
    	button = (Button) exult.findViewById(R.id.save_cancel_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { close();}
        });
	}
	private void close() {
		mainView.setVisibility(View.VISIBLE);
		myView.setVisibility(View.INVISIBLE);
	}
	
}
