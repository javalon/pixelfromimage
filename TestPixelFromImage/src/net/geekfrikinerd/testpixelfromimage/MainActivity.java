package net.geekfrikinerd.testpixelfromimage;

import net.geekfrikinerd.pixelfromimage.PickerColorFromImage;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {
	
	Button btnLauch;
	TextView txtColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        txtColor = (TextView) findViewById(R.id.lblColor);
        btnLauch = (Button)findViewById(R.id.btnLaunch);
        btnLauch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(),PickerColorFromImage.class);
				startActivityForResult(intent, 1);
			}
		});
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	 if (resultCode != RESULT_OK) {
             return;
         }
    	   Bundle bundle= data.getExtras();
           txtColor.setText(Integer.toHexString(bundle.getInt("color")));
    }

    
}
