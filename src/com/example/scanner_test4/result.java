package com.example.scanner_test4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;

public class result extends Activity {
	
	ImageView result;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.result);

	    result = (ImageView)findViewById(R.id.result);
	    
	    Bundle extras = getIntent().getExtras();
	    String path = extras.getString("PATH");
	    
	    
	    try {
	        File f=new File(path, "profile.jpg");
	        Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
	        result.setImageBitmap(b);
	    } 
	    catch (FileNotFoundException e) 
	    {
	        e.printStackTrace();
	    }
	} 
	
	//@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK ) {
	        // do something on back.
	    	Intent i = new Intent(result.this, MainActivity.class);
	    	startActivity(i);
	        return true;
	    }

	    return super.onKeyDown(keyCode, event);
	}
}
