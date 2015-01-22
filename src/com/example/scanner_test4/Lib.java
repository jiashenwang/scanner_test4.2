package com.example.scanner_test4;

public class Lib {
	public  native String showstring(String astr);
	static{
	System.loadLibrary("libopencv_core");
	System.loadLibrary("libopencv_imgproc");
	}
}
