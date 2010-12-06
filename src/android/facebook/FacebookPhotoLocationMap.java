package android.facebook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
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
import com.facebook.android.Util;
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
public class FacebookPhotoLocationMap extends Activity implements SurfaceHolder.Callback, OnClickListener
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
	private static String facebookUserId;
	private Handler handler;

	
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
		asyncRunner = new AsyncFacebookRunner( facebook );
		
		if( facebook.isSessionValid() )
		{
			Log.i(TAG, "Session Key Valid." );
			SessionEvents.onLogoutBegin();
            asyncRunner.logout( context, new LogoutRequestListener());
		}
		else 
		{
			Log.i(TAG, "Session Key Invalid..." );
			facebook.authorize( this, PERMISSIONS, Facebook.FORCE_DIALOG_AUTH, new LoginDialogListener() ); 
		}
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

            	Log.i(TAG, "Sending this Facebook ID to server: " + facebookUserId );
            	out.write( "\n".getBytes() );
            	String idString = "facebookid:" + facebookUserId;
            	out.write( idString.getBytes() );
            	out.flush();
            	
            	// Close streams
            	out.close();
            	socket.close();
            	
            	Log.i( TAG, "Photo, KML, and Facebook ID sent to server..." );
            	
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
	public class IdRequestListener extends BaseRequestListener 
    {
        public void onComplete(final String response) 
        {
        	Log.i( TAG, "Reaches IdRequestListener.onComplete()..." );
        	Log.i( TAG, "Response: " + response );
            try 
            {
                // process the response here: executed in background thread
                Log.d("Facebook-Example", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
                final String userId = json.getString("id");
                
                // Set the facebook user id variable to send to the server
                // along with the picture and kml entry.
                facebookUserId = userId;
                Log.i( TAG, "User Id: " + userId );
                
            } 
            catch (JSONException e)
            {
                Log.w("Facebook-Example", "JSON Error in response");
            }
            catch (FacebookError e) 
            {
                Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
            }
            
            Log.i(TAG, "Leaving IdRequestListener.onComplete()..." );
        }
    }
	
	private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
            SessionEvents.onLoginSuccess();
            Log.i( TAG, "Successfully logged in..." );
    		
    		// Set user id
    		asyncRunner.request( "me", new IdRequestListener() );
        }

        public void onFacebookError(FacebookError error) {
            SessionEvents.onLoginError(error.getMessage());
            Log.i( TAG, "Error logging in: " + error.getMessage() );
        }
        
        public void onError(DialogError error) {
            SessionEvents.onLoginError(error.getMessage());
        }

        public void onCancel() {
            SessionEvents.onLoginError("Action Canceled");
        }
    }
	
	private class LogoutRequestListener extends BaseRequestListener {
        public void onComplete(String response) 
        {
        	Log.i(TAG, "Reaches LogoutRequestListener.onComplete()..." );
        	
            // callback should be run in the original thread, 
            // not the background thread
            handler.post(new Runnable() {
                public void run() {
                    SessionEvents.onLogoutFinish();
                }
            });
        }
    }


	 
}