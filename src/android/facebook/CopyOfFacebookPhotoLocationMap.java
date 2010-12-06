package android.facebook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.R;
import com.facebook.android.Facebook.DialogListener;


/**
 * This class is the main class for the FacebookPhotoLocations app. It provides
 * the necessary code for using the Camera.
 * 
 * Most Camera Code borrowed from: 
 * - http://snippets.dzone.com/posts/show/8683 and
 * - http://www.developer.com/ws/other/article.php/3748281/Working-with-Images-in-Googles-Android.htm
 * 
 * Converting and sending byte arrays, help from:
 * - http://www.herongyang.com/encoding/Base64-Sun-Java-Implementation.html
 * - http://www.hcilab.org/documents/tutorials/PictureTransmissionOverHTTP/index.html
 */
public class CopyOfFacebookPhotoLocationMap extends Activity implements SurfaceHolder.Callback, OnClickListener
{
	static final int MODE = 0;
	public static final String TAG = "FacebookPhotoLocations";
	private static final String photosLocation = "http://cisunix.unh.edu/~mbv4/photos";
	private static final String photoFileName = "FBPhotoLocationTest.jpg";
	Camera camera;
	boolean isPreviewRunning = false;
	private Context context = this;
	
	// Facebook stuff
	public static final String APP_ID = "178069788885403";
	private static final String[] PERMISSIONS =
        new String[] { " " };
	private static Facebook facebook;
	private AsyncFacebookRunner asyncRunner;
	public static String facebookUserId;	// currently a global variable, not the best practice...

	
	// Provides location information to be sent with the pictures
	PhotoLocation location = null;
	
	/**
	 * Called when the application is started.
	 */
	public void onCreate( Bundle bundle ) 
	{
		super.onCreate( bundle );

		Log.i( TAG, "Reaches onCreate()..." );

		Bundle extras = getIntent().getExtras();
		
		// Set up Facebook stuff
		setupFacebook();
		
		// The following is needed to get location information for the photos
		LocationManager manager = (LocationManager)getSystemService( Context.LOCATION_SERVICE );
		location = new PhotoLocation( manager );
		
		getWindow().setFormat( PixelFormat.TRANSLUCENT );
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN );
		setContentView( R.layout.camera_surface);
		mSurfaceView = (SurfaceView) findViewById( R.id.surface_camera );
		mSurfaceView.setOnClickListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	protected void setupFacebook()
	{
		// DEBUG
		Log.i( TAG, "Reaches setupFacebook()..." );
		
		if (APP_ID == null) 
		{
            Builder alertBuilder = new Builder(this);
            alertBuilder.setTitle("Warning");
            alertBuilder.setMessage("A Facebook Applicaton ID must be " +
                    "specified before running this example: " +
                    "see FacebookPhotoLocationMap.java");
            alertBuilder.create().show();
        }
		
		Facebook facebook = new Facebook( APP_ID );
		facebook.authorize( this, PERMISSIONS, Facebook.FORCE_DIALOG_AUTH, new AuthorizeListener() ); 
	}
	
	// Currently should not be called because not doing single sign-on...
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	    Log.i( TAG, "Reaches onActivityResult()..." );
	    
	    super.onActivityResult(requestCode, resultCode, data);
	    facebook.authorizeCallback(requestCode, resultCode, data);   
	}

	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	Camera.PictureCallback pictureCallback = new Camera.PictureCallback() 
	{
		public void onPictureTaken(byte[] imageData, Camera c) 
		{
			Log.i( TAG, "reaches onPictureTaken()..." );

			// Get FacebookID
			try
		    {
		        String accessToken = facebook.getAccessToken();	
		        Log.i( TAG, "Access Token: " + accessToken );
		    }
		    catch( Exception e )
		    {
		    	Log.e( TAG, "Error requesting Facebook information..." );
		    	Log.e( TAG, e.toString() );
		    	System.exit( 1 );
		    }
		    
			if ( imageData != null ) 
			{
				Intent intent = new Intent();

				sendPhotoToServer( imageData );
									
				camera.startPreview();	// go back to camera view

				setResult( MODE, intent );
				
				// DETERMINE HERE IF YOU WANT TO KEEP TAKING PICS OR NOT
				// FOR NOW, JUST KEEP TAKING THEM and manually end program on phone.
				
				//finish();			
			}
		}
	};
	
	/**
	 * Stores the bitmap image as a JPG in a directory, then upload it
	 * to the server with location and Facebook information for further
	 * work to be done on it concerning the KML maps and Facebook info.
	 * 
	 * @param bitmap - The bitmap image to convert and upload.
	 */
	protected void sendPhotoToServer( byte[] picBytes )
	{
		Log.i( TAG, "reaches sendPhotoToServer()..." );
        
        try 
        {
        	// encode the byte array in a string
        	String imageString = Base64.encode( picBytes );
        	
        	// send the string to the server
        	OutputStream out;
        	PrintWriter stringOut;
        	Socket socket = new Socket( "agate.cs.unh.edu", 8877 );      
            out = socket.getOutputStream();
            
            		
    		// Wait for server to say something to us so we know we're connected.
            if( socket.isConnected() )
            {   
            	Log.i( TAG, "Sending image bytes to server..." );
            	out.write( imageString.getBytes() );
            	out.flush();

            	Log.i( TAG, "Sending this KML information to server..." );
            	Log.i( TAG, location.getKMLString() );

            	out.write( "\n".getBytes() );	// new line necessary for proper parsing
            	out.write( location.getKMLString().getBytes() );
            	out.flush();

            	out.close();
            	socket.close();
            	
            	Log.i( TAG, "Photo and KML sent to server..." );
            	
            	// DECIDE HERE IF YOU WANT TO TAKE MORE PICTURES OR NOT
            	// FOR NOW, JUST KEEP TAKING THEM...
            	camera.startPreview();
        		isPreviewRunning = true;
            }
            else
            {
            	Log.e(TAG, "Not connected to the server, exiting..." );
            	System.exit(1);
            }
   
        }
        catch( Exception e ) 
        {
        	Log.e( TAG, "Error in sendPhotoToServer()..." );
            e.printStackTrace();                	
        }

	}

	protected void onResume() {
		Log.i( TAG, "Reaches onResume()..." );
		super.onResume();
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	protected void onStop() 
	{
		Log.i(TAG, " Reaches onStop()..." );
		super.onStop();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		camera = Camera.open();
	
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) 
	{
		Log.i(TAG, "surfaceChanged");

		// XXX stopPreview() will crash if preview is not running
		if (isPreviewRunning) {
			camera.stopPreview();
		}

		Camera.Parameters p = camera.getParameters();
		
		// DEBUG
		// Check out supported sizes...
		/*for( Size s : p.getSupportedPreviewSizes() )
		{
		    Log.i( TAG, "width: " + s.width + ", height: " + s.height );	
		}*/
		
		
		try 
		{
			p.setPreviewSize(h, w);			// switched h and w from example...
			camera.setParameters(p);
			camera.setPreviewDisplay(holder);
		} 
		catch ( IOException e ) 
		{
			// TODO Auto-generated catch block
			Log.e( TAG, "Error setting the preview size, exiting..." );
			e.printStackTrace();
			System.exit( 1 );
		}
		camera.startPreview();
		isPreviewRunning = true;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e(TAG, "surfaceDestroyed");
		camera.stopPreview();
		isPreviewRunning = false;
		camera.release();
	}

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	public void onClick(View arg0) {

		camera.takePicture(null, pictureCallback, pictureCallback);

	}
	
	// -------------------------------------------------------------------------
	// Facebook classes
	class AuthorizeListener implements DialogListener 
	{
		  public void onComplete(Bundle values) 
		  {
		      //  Handle a successful login
			  Log.i( TAG, "reaches AuthorizeListener.onComplete().." );
		  }

		public void onCancel() 
		{
			Log.e( TAG, "Reaches AuthorizeListener.onCancel()..." );			
		}

		public void onError(DialogError e) 
		{
			
			Log.e( TAG, "AuthorizeListener.onError(): " + e );
		}

		public void onFacebookError(FacebookError e)
		{
			Log.e( TAG, "AuthorizeListener.onFacebookError()..." + e );
			
		}
	}
   


	 
}