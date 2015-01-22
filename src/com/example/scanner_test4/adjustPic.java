package com.example.scanner_test4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class adjustPic extends Activity implements OnTouchListener{
	
	private final String TAG = "BZCardImageAdjustmentActivity";
	
	public static String CURRENT_CARD_ID = 
			"com.whova.bzcard.adjustment.BZCardImageAdjustmentActivity.current_card_id";
	
	public static String RETAKE_IMAGE_FLAG = 
			"com.whova.bzcard.adjustment.BZCardImageAdjustmentActivity.retake_image_flag";
	
	private String mCurrentCardID = null;
	private boolean mRetakeImageFlag = false;
	
	int dst_width = 1000;
	int dst_height = 600;
	 
	ImageView confirm, rotateLeft, rotateRight;
	ImageView zoom;
	ImageView LT, LB, RT, RB;
	ImageView imageView, imageViewResult;
	ImageView blackLayer;// whiteLayer;
	ProgressBar pb;
	private Point leftTop = new Point(20,20);
	private Point leftBot = new Point(20,580);
	private Point rightTop = new Point(980,20);
	private Point rightBot = new Point(980,580);
	private Point new_leftTop = new Point(0,0);
	private Point new_leftBot = new Point(0,0);
	private Point new_rightTop = new Point(0,0);
	private Point new_rightBot = new Point(0,0);
	Bitmap processBitmap, resultBitmap;
	
	double rHeight, rWidth;
	
	double screenWidth=0, screenHeight=0, pictureRatio=0.6;
	float x,y = 0.0f;
	boolean moving=false;
	RelativeLayout rl;
    LinearLayout componentControl;
    Display display;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	        
	    setContentView(R.layout.adjust_pic);
	    
	    /*
	    ActionBar actionBar = getActionBar();
	    actionBar.setTitle("Crop card");
	    actionBar.setDisplayHomeAsUpEnabled(true);
	    actionBar.setDisplayShowHomeEnabled(true);
	    */ 
	    try{
	    	//mCurrentCardID = getIntent().getExtras().getString(CURRENT_CARD_ID, null);
	    	mRetakeImageFlag = getIntent().getExtras().getBoolean(RETAKE_IMAGE_FLAG, false);
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
	    
	    display = getWindowManager().getDefaultDisplay();

	    
	    componentControl = (LinearLayout)findViewById(R.id.component_control);
	    
	    rHeight = display.getHeight();
	    rWidth = display.getWidth()-componentControl.getWidth();
	    
	    if(rHeight/rWidth > pictureRatio){
	    	screenWidth = rWidth;
	    	screenHeight = screenWidth * pictureRatio;
	    }else{
	    	screenHeight = rHeight;
	    	screenWidth = screenHeight / pictureRatio;
	    }
	    
	    
	    
	    rl = (RelativeLayout) findViewById(R.id.relative_layout);
	    confirm = (ImageView) findViewById(R.id.image_done);
	    rotateLeft = (ImageView) findViewById(R.id.image_left_rotate);
	    rotateRight = (ImageView) findViewById(R.id.image_right_rotate);
	    
	    imageView = (ImageView) findViewById(R.id.image);
	    imageViewResult = (ImageView) findViewById(R.id.image_result);
		imageView.getLayoutParams().height = (int) screenHeight;
		imageView.getLayoutParams().width = (int) screenWidth;
		
		zoom = (ImageView)findViewById(R.id.image_zoom);
		
	    pb = (ProgressBar) findViewById(R.id.progressBar);
	    LT = (ImageView) findViewById(R.id.left_up);
	    LB = (ImageView) findViewById(R.id.left_down);
	    RT = (ImageView) findViewById(R.id.right_up);
	    RB = (ImageView) findViewById(R.id.right_down);
	    LT.setOnTouchListener(this);
	    LB.setOnTouchListener(this);
	    RT.setOnTouchListener(this);
	    RB.setOnTouchListener(this);
	    
	    MyAsyncTaskHelper async = new MyAsyncTaskHelper(getApplicationContext());
	    async.execute();
	    
	    confirm.setOnClickListener(new ConfirmListerner());
	    rotateLeft.setOnClickListener(new RotateLeftListerner());
	    rotateRight.setOnClickListener(new RotateRightListerner());
	    
	    
	    blackLayer = (ImageView) findViewById(R.id.black_layer);
	    blackLayer.getLayoutParams().height = imageView.getLayoutParams().height;
	    blackLayer.getLayoutParams().width = imageView.getLayoutParams().width;
	    /*
	    whiteLayer = (ImageView) findViewById(R.id.white_layer);
	    whiteLayer.getLayoutParams().height = imageView.getLayoutParams().height;
	    whiteLayer.getLayoutParams().width = imageView.getLayoutParams().width;
	    */
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	private class ConfirmListerner implements OnClickListener{
		@Override
		public void onClick(View v) {
			
	        Bitmap resultBitmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
	        		+"/scanner_test4_pic.jpg");	
	        
	        double realWidth = resultBitmap.getWidth()/2;
	        double realHeight = resultBitmap.getHeight()/2;
	        double widthRatio = realWidth/dst_width;
	        double heightRatio = realHeight/dst_height;
	        
	        resultBitmap = getResizedBitmap(resultBitmap,(int)realHeight,(int)realWidth);
	        
			new_leftTop = new Point(((LT.getX()+LT.getLayoutParams().width/2f)/(imageView.getWidth())*dst_width)*widthRatio,
					((LT.getY()+LT.getLayoutParams().height/2f)/(imageView.getHeight())*dst_height)*heightRatio);
			new_leftBot = new Point(((LB.getX()+LB.getLayoutParams().width/2f)/(imageView.getWidth())*dst_width)*widthRatio,
					((LB.getY()+LB.getLayoutParams().height/2f)/(imageView.getHeight())*dst_height)*heightRatio);
			new_rightTop = new Point(((RT.getX()+RT.getLayoutParams().width/2f)/(imageView.getWidth())*dst_width)*widthRatio,
					((RT.getY()+RT.getLayoutParams().height/2f)/(imageView.getHeight())*dst_height)*heightRatio);
			new_rightBot = new Point(((RB.getX()+RB.getLayoutParams().width/2f)/(imageView.getWidth())*dst_width)*widthRatio,
					((RB.getY()+RB.getLayoutParams().height/2f)/(imageView.getHeight())*dst_height)*heightRatio);

			
			double resultWidth = 1000;
			double resultHeight = 630.60748;
			
	        Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
	        Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);

            

            
	        if(distance(new_leftTop,new_rightTop)>distance(new_leftTop,new_leftBot)){
	            src_mat.put(0,0,new_leftTop.x,new_leftTop.y,
	            		new_rightTop.x, new_rightTop.y, 
	            		new_leftBot.x, new_leftBot.y, 
	            		new_rightBot.x, new_rightBot.y );
	        }else{
	            src_mat.put(0,0,new_rightTop.x,new_rightTop.y,
	            		new_rightBot.x, new_rightBot.y, 
	            		new_leftTop.x, new_leftTop.y, 
	            		new_leftBot.x, new_leftBot.y);
	        }
	        
	        
	        Mat rgbMat = new Mat();
	        rgbMat = Utils.bitmapToMat(processBitmap);
	        
	        Imgproc.resize(rgbMat, rgbMat, new Size(realWidth, realHeight));
	        //resultBitmap = Bitmap.createBitmap((int)dst_width,(int)dst_height, Config.RGB_565);
	        
	        dst_mat.put(0,0, 0,0, resultWidth,0, 0,resultHeight, resultWidth,resultHeight);
	        Mat tempMat = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
	        Mat dstMat=rgbMat.clone();
	        Imgproc.warpPerspective(rgbMat, dstMat, tempMat, new Size(resultWidth,resultHeight));
	        
	        resultBitmap = getResizedBitmap(resultBitmap,(int)resultHeight,(int)resultWidth);
	        Utils.matToBitmap(dstMat, resultBitmap);
	        
	        
	        //store result into cell phone
	        ContextWrapper cw = new ContextWrapper(getApplicationContext());
	         // path to /data/data/yourapp/app_data/imageDir
	        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
	        // Create imageDir
	        File mypath=new File(directory,"profile.jpg");

	        FileOutputStream fos = null;
	        try {           
	            fos = new FileOutputStream(mypath);
	       // Use the compress method on the BitMap object to write image to the OutputStream
	            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
	            fos.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        
	        // done with storing
	        
	        
	        Intent intent = new Intent(adjustPic.this, result.class);
	        intent.putExtra("PATH", directory.getAbsolutePath());
	        startActivity(intent);

		}
		
	}
	private class RotateLeftListerner implements OnClickListener{
	
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			resultBitmap = RotateBitmap(resultBitmap,-90);
			if(resultBitmap.getWidth()>resultBitmap.getHeight()){
				imageViewResult.getLayoutParams().height = (int) screenHeight;
				imageViewResult.getLayoutParams().width = (int) screenWidth;
			}else{
				imageViewResult.getLayoutParams().width = (int) screenHeight;
				imageViewResult.getLayoutParams().height = (int) screenWidth;
			}
			imageViewResult.setImageBitmap(resultBitmap);
		}
		
	}
	private class RotateRightListerner implements OnClickListener{
	
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			resultBitmap = RotateBitmap(resultBitmap,90);
			if(resultBitmap.getWidth()>resultBitmap.getHeight()){
				imageViewResult.getLayoutParams().height = (int) screenHeight;
				imageViewResult.getLayoutParams().width = (int) screenWidth;
			}else{
				imageViewResult.getLayoutParams().width = (int) screenHeight;
				imageViewResult.getLayoutParams().height = (int) screenWidth;
			}
			imageViewResult.setImageBitmap(resultBitmap);
		}
		
		
	}
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Bitmap temp = processBitmap.copy(processBitmap.getConfig(), true);
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			moving = true;		
			// handle zoom in functionality
			zoom.setVisibility(View.VISIBLE);
			Bitmap zoomPic = findZoomInPic((v.getX()+v.getLayoutParams().width/2)/(imageView.getWidth())*dst_width+v.getLayoutParams().width/2, 
					(v.getY()+v.getLayoutParams().height/2)/(imageView.getHeight())*dst_height, temp);
			zoom.setImageBitmap(zoomPic);
			
			break;
		case MotionEvent.ACTION_MOVE:
			if(moving){
				x=event.getRawX() - v.getLayoutParams().width/2;
				y=event.getRawY() - v.getLayoutParams().height/2;
				if(x<=imageView.getWidth()-v.getLayoutParams().width/2 && y<=imageView.getHeight()-v.getLayoutParams().height/2){
					if(x<0-v.getLayoutParams().width/2){
						v.setX(0-v.getLayoutParams().width/2);
					}else{
						v.setX(x);
					}
					if(y<0-v.getLayoutParams().height/2){
						v.setY(0-v.getLayoutParams().height/2);
					}else{
						v.setY(y);	
					}	
				}else{
					if(x>imageView.getWidth()-v.getLayoutParams().width/2){
						v.setX((float) (imageView.getWidth() - v.getLayoutParams().width/2));
					}else{
						v.setX(x);
					}
					if(y>imageView.getHeight()-v.getLayoutParams().height/2){
						v.setY((float) (imageView.getHeight() - v.getLayoutParams().height/2));		
					}else{
						v.setY(y);
					}
	
				}
				imageView.setImageBitmap(processBitmap);
				temp = processBitmap.copy(processBitmap.getConfig(), true);
				Canvas canvas = new Canvas(temp);
			    Paint paint = new Paint();
			    paint.setColor(Color.rgb(102, 204, 255));
			    paint.setStrokeWidth(2);
			    canvas.drawLine((LT.getX()+LT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(LT.getY()+LT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		(RT.getX()+RT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(RT.getY()+RT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		paint);
			    canvas.drawLine((RT.getX()+RT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(RT.getY()+RT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		(RB.getX()+RB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(RB.getY()+RB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		paint);
			    canvas.drawLine((RB.getX()+RB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(RB.getY()+RB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		(LB.getX()+LB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(LB.getY()+LB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		paint);
			    canvas.drawLine((LB.getX()+LB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(LB.getY()+LB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		(LT.getX()+LT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
			    		(LT.getY()+LT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
			    		paint);
			    
			    imageView.setImageBitmap(temp);
		   
			    
				zoom.setVisibility(View.VISIBLE);
				zoomPic = findZoomInPic((v.getX()+v.getLayoutParams().width/2)/(imageView.getWidth())*dst_width, 
						(v.getY()+v.getLayoutParams().height/2)/(imageView.getHeight())*dst_height,
						temp);
				zoom.setImageBitmap(zoomPic);	
				
			    //drwing black part
			    blackLayer.setBackgroundColor(Color.rgb(32,32,32));
			    blackLayer.buildDrawingCache();
			    Bitmap temp2 = imageView.getDrawingCache();
			    temp2 = Bitmap.createBitmap(temp.getWidth(),temp.getHeight(), Config.ARGB_8888);
			    Canvas canvas2 = new Canvas(temp2);
			    Path path = new Path();
			    final Rect rect = new Rect(0, 0, temp.getWidth(),temp.getHeight());
			    
			    Point point1_draw = new Point((LT.getX()+LT.getLayoutParams().width/2f)/((int)imageView.getWidth()*1.0)*dst_width,
			    		(LT.getY()+LT.getLayoutParams().height/2f)/((int)imageView.getHeight()*1.0)*dst_height);
	            Point point2_draw = new Point((RT.getX()+RT.getLayoutParams().width/2f)/((int)imageView.getWidth()*1.0)*dst_width,
	            		(RT.getY()+RT.getLayoutParams().height/2f)/((int)imageView.getHeight()*1.0)*dst_height);
	            Point point3_draw = new Point((RB.getX()+RB.getLayoutParams().width/2f)/((int)imageView.getWidth()*1.0)*dst_width, 
	            		(RB.getY()+RB.getLayoutParams().height/2f)/((int)imageView.getHeight()*1.0)*dst_height);
	            Point point4_draw = new Point((LB.getX()+LB.getLayoutParams().width/2f)/((int)imageView.getWidth()*1.0)*dst_width, 
	            		(LB.getY()+LB.getLayoutParams().height/2f)/((int)imageView.getHeight()*1.0)*dst_height);
	            
	            path.moveTo((float)point1_draw.x, (float)point1_draw.y);
	            path.lineTo((float)point2_draw.x, (float)point2_draw.y);
	            path.lineTo((float)point3_draw.x, (float)point3_draw.y);
	            path.lineTo((float)point4_draw.x, (float)point4_draw.y);
	            path.lineTo((float)point1_draw.x, (float)point1_draw.y);
	            
	            path.close();
	            canvas2.drawARGB(0, 0, 0, 0);
	            paint.setColor(Color.WHITE);
	            canvas2.drawPath(path, paint);
	            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	            canvas2.drawBitmap(temp2, 0, 0, paint);
	            blackLayer.setImageBitmap(temp2);
	            // end of drawing part
				
			}
			break;
		case MotionEvent.ACTION_UP:
			moving = false;
			zoom.setVisibility(View.GONE);
			break;
		}
		return true;
	}
	
	// find zoom in picture
	private Bitmap findZoomInPic(double x, double y, Bitmap processBitmap2) {
		double x_t = x/1.5;
		double y_t = y/1.5;
		Bitmap originBm = getResizedBitmap(processBitmap2, 400, 667);
		
		Mat origin = new Mat();
		origin = Utils.bitmapToMat(originBm);
		
        Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
        Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
        
        src_mat.put(0,0, x_t-20,y_t-20, x_t+20,y_t-20, x_t-20,y_t+20, x_t+20,y_t+20); 
        dst_mat.put(0,0, 0,0,667,0, 0,400, 667,400);
        
        Mat tempMat = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        
        Mat dstMat=origin.clone();
        Imgproc.warpPerspective(origin, dstMat, tempMat, new Size(667,400));
        
        double middle_x = 667/2;
        double middle_y = 400/2;
        
        Core.line(dstMat, new Point(middle_x,middle_y+120), new Point(middle_x,middle_y-120), new Scalar(255,255,255), 5);
        Core.line(dstMat, new Point(middle_x+120,middle_y), new Point(middle_x-120,middle_y), new Scalar(255,255,255), 5);
        Utils.matToBitmap(dstMat, originBm);
        
		return originBm;
	}	

		private class MyAsyncTaskHelper extends AsyncTask<Void, Void, Void>{
	
		private Context context;
		
		MyAsyncTaskHelper(Context c){
			context = c;
		}
		
		protected void onPreExecute(){
			pb.setVisibility(View.VISIBLE);
		}
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			findCard();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v){
	        pb.setVisibility(View.GONE);
	        confirm.setVisibility(View.VISIBLE);
	        imageView.setVisibility(View.VISIBLE);
			imageView.setImageBitmap(processBitmap);
			
		    if(rHeight/rWidth > pictureRatio){
		    	imageView.getLayoutParams().width = (int) (imageView.getMeasuredHeight() / pictureRatio);
		    }else{
		    	imageView.getLayoutParams().height = (int) (imageView.getMeasuredWidth() * pictureRatio); 
		    }
		    blackLayer.getLayoutParams().height = imageView.getLayoutParams().height;
		    blackLayer.getLayoutParams().width = imageView.getLayoutParams().width;
			
			
			LT.setX((float) (leftTop.x/(dst_width*1.0)*imageView.getWidth() - LT.getLayoutParams().width/2f));
			LT.setY((float) (leftTop.y/(dst_height*1.0)*imageView.getLayoutParams().height) - LT.getLayoutParams().width/2f);
			LT.setVisibility(View.VISIBLE);
			
			LB.setX((float) (leftBot.x/(dst_width*1.0)*imageView.getWidth() - LB.getLayoutParams().width/2f));
			LB.setY((float) (leftBot.y/(dst_height*1.0)*imageView.getLayoutParams().height) - LB.getLayoutParams().width/2f);
			LB.setVisibility(View.VISIBLE);
			
			RT.setX((float) (rightTop.x/(dst_width*1.0)*imageView.getWidth() - RT.getLayoutParams().width/2f));
			RT.setY((float) (rightTop.y/(dst_height*1.0)*imageView.getLayoutParams().height) - RT.getLayoutParams().width/2f);
			RT.setVisibility(View.VISIBLE);
			
			RB.setX((float) (rightBot.x/(dst_width*1.0)*imageView.getWidth() - RB.getLayoutParams().width/2f));
			RB.setY((float) (rightBot.y/(dst_height*1.0)*imageView.getLayoutParams().height) - RB.getLayoutParams().width/2f);
			RB.setVisibility(View.VISIBLE);
			
			
	        
	        //draw the first four lines
			Bitmap temp = processBitmap.copy(processBitmap.getConfig(), true);;
			Canvas canvas = new Canvas(temp);
		    Paint paint = new Paint();
		    paint.setColor(Color.rgb(102, 204, 255));
		    paint.setStrokeWidth(2);
		    
		    
		    canvas.drawLine((LT.getX()+LT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(LT.getY()+LT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		(RT.getX()+RT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(RT.getY()+RT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		paint);
		    canvas.drawLine((RT.getX()+RT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(RT.getY()+RT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		(RB.getX()+RB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(RB.getY()+RB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		paint);
		    canvas.drawLine((RB.getX()+RB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(RB.getY()+RB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		(LB.getX()+LB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(LB.getY()+LB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		paint);
		    canvas.drawLine((LB.getX()+LB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(LB.getY()+LB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		(LT.getX()+LT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
		    		(LT.getY()+LT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height, 
		    		paint);

		    		
		    imageView.setImageBitmap(temp);
		    
		    
		    //drwing black part
			blackLayer.setX(0);
			blackLayer.setY(0);
		    blackLayer.setBackgroundColor(Color.rgb(32,32,32));
		    blackLayer.buildDrawingCache();
		    Bitmap temp2 = imageView.getDrawingCache();
		    temp2 = Bitmap.createBitmap(temp.getWidth(),temp.getHeight(), Config.ARGB_8888);
		    Canvas canvas2 = new Canvas(temp2);
		    Path path = new Path();
		    
		    Point point1_draw = new Point((LT.getX()+LT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width,
		    		(LT.getY()+LT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height);
            Point point2_draw = new Point((RT.getX()+RT.getLayoutParams().width/2f)/imageView.getWidth()*dst_width,
            		(RT.getY()+RT.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height);
            Point point3_draw = new Point((RB.getX()+RB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
            		(RB.getY()+RB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height);
            Point point4_draw = new Point((LB.getX()+LB.getLayoutParams().width/2f)/imageView.getWidth()*dst_width, 
            		(LB.getY()+LB.getLayoutParams().height/2f)/imageView.getLayoutParams().height*dst_height);
            
            path.moveTo((float)point1_draw.x, (float)point1_draw.y);
            path.lineTo((float)point2_draw.x, (float)point2_draw.y);
            path.lineTo((float)point3_draw.x, (float)point3_draw.y);
            path.lineTo((float)point4_draw.x, (float)point4_draw.y);
            path.lineTo((float)point1_draw.x, (float)point1_draw.y);
            
            path.close();
            canvas2.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.WHITE);
            canvas2.drawPath(path, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas2.drawBitmap(temp2, 0, 0, paint);
            blackLayer.setImageBitmap(temp2);
            // end of drawing part
             
             
		}
		
		private void findCard() {
			Mat rgbMat = new Mat(); 
			Bitmap srcBitmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
	        		+"/scanner_test4_pic.jpg");	
			processBitmap = getResizedBitmap(srcBitmap, dst_height, dst_width);
			
			
			if(srcBitmap.getWidth() < srcBitmap.getHeight()){
				Bitmap tempBitmap = RotateBitmap(srcBitmap,90);
				srcBitmap = tempBitmap.copy(tempBitmap.getConfig(), true);
			}
			rgbMat = Utils.bitmapToMat(srcBitmap);
			Imgproc.resize(rgbMat, rgbMat, new Size(dst_width, dst_height));
	        //processBitmap = Bitmap.createBitmap((int)dst_width,(int)dst_height, Config.RGB_565);
	         
	        ArrayList<List<Point>> squares = new ArrayList<List<Point>>();
	        ArrayList<List<Point>> largest_square = new ArrayList<List<Point>>();
	        squares = (ArrayList<List<Point>>) find_squares(rgbMat).clone();
	        largest_square = (ArrayList<List<Point>>) find_largest_square(squares).clone();    
	         
	        if(largest_square.size()==0){
	        	//Utils.matToBitmap(rgbMat, processBitmap);
	        }else{
	        	double maxSum=Double.NEGATIVE_INFINITY, 
	        			minSum = Double.POSITIVE_INFINITY,
	        			maxDiff = Double.NEGATIVE_INFINITY, 
	        			minDiff = Double.POSITIVE_INFINITY;
	        	
	        	for(int i=0; i<largest_square.get(0).size();i++){
	              	if(largest_square.get(0).get(i).x+largest_square.get(0).get(i).y <= minSum){
	              		minSum = largest_square.get(0).get(i).x+largest_square.get(0).get(i).y;
	              		leftTop.x = largest_square.get(0).get(i).x+5;
	              		leftTop.y = largest_square.get(0).get(i).y+5;
	              	}
	              	if(largest_square.get(0).get(i).x+largest_square.get(0).get(i).y >= maxSum){
	              		maxSum = largest_square.get(0).get(i).x+largest_square.get(0).get(i).y;
	              		rightBot.x = largest_square.get(0).get(i).x-5;
	              		rightBot.y = largest_square.get(0).get(i).y+5;
	              	}
	              	if(largest_square.get(0).get(i).x-largest_square.get(0).get(i).y <= minDiff){
	              		minDiff = largest_square.get(0).get(i).x-largest_square.get(0).get(i).y;
	              		leftBot.x = largest_square.get(0).get(i).x+5;
	              		leftBot.y = largest_square.get(0).get(i).y-5;
	              	}
	              	if(largest_square.get(0).get(i).x-largest_square.get(0).get(i).y >= maxDiff){
	              		maxDiff = largest_square.get(0).get(i).x-largest_square.get(0).get(i).y;
	              		rightTop.x = largest_square.get(0).get(i).x-5;
	              		rightTop.y = largest_square.get(0).get(i).y-5;
	              	}
	        	}        	
	        	/*
	        	Core.line(rgbMat, leftBot, rightBot, new Scalar(255,0,0), 1);
	        	Core.line(rgbMat, rightBot, rightTop, new Scalar(255,0,0), 1);
	        	Core.line(rgbMat, rightTop, leftTop, new Scalar(255,0,0), 1);
	        	Core.line(rgbMat, leftTop, leftBot, new Scalar(255,0,0), 1);
	        	*/
	            //Utils.matToBitmap(rgbMat, processBitmap);
	            
	        }
	        
	        
		}
		
		private ArrayList<List<Point>> find_largest_square(ArrayList<List<Point>> squares) {
			// TODO Auto-generated method stub
			ArrayList<List<Point>> largest_squares = new ArrayList<List<Point>>();
		    if (squares.size()==0)
		    {
		        // no squares detected
		        return largest_squares;
		    }else{
		        int max_width = 0;
		        int max_height = 0;
		        int max_square_idx = 0;
		        for (int i = 0; i < squares.size(); i++){
		        	
		        	List<Point> list = new Vector<Point>();
		        	list.add(squares.get(i).get(0));
		        	list.add(squares.get(i).get(1));
		        	list.add(squares.get(i).get(2));
		        	list.add(squares.get(i).get(3));
					Rect rectangle = Imgproc.boundingRect(list);
					
					if ((rectangle.width >= max_width) && (rectangle.height >= max_height))
			        {
			            max_width = rectangle.width;
			            max_height = rectangle.height;
			            max_square_idx = i;
			        }
		        }
		        largest_squares.add(squares.get(max_square_idx));
				return largest_squares;
		    }
		}
	
		private ArrayList<List<Point>> find_squares(Mat rgbMat) {
			// TODO Auto-generated method stub
			ArrayList<List<Point>> squares = new ArrayList<List<Point>>();
			
			Mat blurred_1 = new Mat(); 
			Mat gray_0 = new Mat();
			Mat gray = new Mat();
			Imgproc.medianBlur(rgbMat, blurred_1, 9);
			Imgproc.cvtColor(blurred_1, gray_0, Imgproc.COLOR_RGB2GRAY);
			
			List<Mat> blurred=new ArrayList<Mat>();
			List<Mat> gray0=new ArrayList<Mat>();
			blurred.add(blurred_1);
			gray0.add(gray_0);
			List<Mat> contours = new Vector<Mat>();
			for (int c = 0; c < 3; c++){
				List<Integer> temp = new Vector<Integer>();
				temp.add(c);
				temp.add(0);
				Core.mixChannels(blurred, gray0, temp);
			}
			  
			Imgproc.Canny(gray0.get(0), gray, 10, 20, 3, false);
			Imgproc.dilate(gray, gray, new Mat(), new Point(-1,-1),1);
			
			Mat hierarchy = new Mat();
			Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
			
			//List<Point> approx =  new Vector<Point>();
			for (int i = 0; i < contours.size(); i++){
				List<Point> tempList = new Vector<Point>();;
				Mat tempMat = new Mat();
				List<Point> tempList2 = new Vector<Point>();;
				Converters.Mat_to_vector_Point(contours.get(i), tempList);
				Imgproc.approxPolyDP(contours.get(i), tempMat, Imgproc.arcLength(tempList, true)*0.02,
						true);
				Converters.Mat_to_vector_Point(tempMat, tempList2);
				if(tempList2.size()==4 && 
						Math.abs(Imgproc.contourArea(tempMat)) > 1000 &&
						Imgproc.isContourConvex(tempList2)){
					double maxCosine = 0;
					
					for (int j = 2; j < 5; j++){
						double cosine = Math.abs(angle(tempList2.get(j%4), tempList2.get(j-2), tempList2.get(j-1)));
						maxCosine = Math.max(maxCosine, cosine);
					}
					if (maxCosine < 0.4){
                        squares.add(tempList2);
					}
				}
			}
			
			
			return squares;
		}
		    
		double angle( Point pt1, Point pt2, Point pt0 ) {
		    double dx1 = pt1.x - pt0.x;
		    double dy1 = pt1.y - pt0.y;
		    double dx2 = pt2.x - pt0.x;
		    double dy2 = pt2.y - pt0.y;
		    return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
		}
		
		double distance(Point pt1, Point pt2){
			return (Math.sqrt((pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y)));
		}
	}
	
	public static float convertDpToPixel(float dp, Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi / 160f);
	    return px;
	}
	public static float convertPixelsToDp(float px, Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;
	} 
	double distance(Point pt1, Point pt2){
		return (Math.sqrt((pt1.x-pt2.x)*(pt1.x-pt2.x)+(pt1.y-pt2.y)*(pt1.y-pt2.y)));
	}
	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
	      Matrix matrix = new Matrix();
	      matrix.postRotate(angle);
	      return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}
	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
	    int width = bm.getWidth();
	    int height = bm.getHeight();
	    float scaleWidth = ((float) newWidth) / width;
	    float scaleHeight = ((float) newHeight) / height;
	    // CREATE A MATRIX FOR THE MANIPULATION
	    Matrix matrix = new Matrix();
	    // RESIZE THE BIT MAP
	    matrix.postScale(scaleWidth, scaleHeight);

	    // "RECREATE" THE NEW BITMAP
	    Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
	    return resizedBitmap;
	}
}
