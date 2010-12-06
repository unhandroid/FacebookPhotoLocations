package android.facebook;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * This class retrieves location information using GPS or Network info.
 * 
 * @author Matthew Robinson
 *
 */
public class PhotoLocation 
{
	// Constants
	static final String TAG = FacebookPhotoLocationMap.TAG;
	
	LocationManager locationManager = null;
	Location location = null;
	
	PhotoLocation( LocationManager manager )
	{
	    // Set appropriate location-based objects
	    locationManager = manager;
	    
	    // Create location listener. Used for determining location updates
        LocationListener locationListener = new LocationListener() 
        {
            public void onLocationChanged( Location location ) 
            {
               setLocation( location );
            }

            public void onStatusChanged( String provider, int status, Bundle extras )
            {}

            public void onProviderEnabled(String provider)
            {}

            public void onProviderDisabled(String provider) 
            {}
          };
        
       // Register the listener with the Location Manager to receive location updates
       // from both available Network and GPS providers.
       // The third parameter is the minimum change in distance before receiving 
       // new information. 
          
       // Set to request location update every 50 meters
       locationManager.requestLocationUpdates
          (LocationManager.NETWORK_PROVIDER, 0, 50, locationListener);
       locationManager.requestLocationUpdates
          (LocationManager.GPS_PROVIDER, 0, 50, locationListener);
	}
	
	/**
	 * Sets the current location object in this class.
	 * 
	 * @param location 
	 */
	protected void setLocation( Location location )
	{
		this.location = location;
	}
	
	/**
	 * Gets the current location object set in this class.
	 * 
	 * @return The current location object associated with this class.
	 */
	protected Location getLocation()
	{
		return location;	
	}

	/**
	 * Creates a KML line for the current location set
	 * 
	 * The line looks this: "kml:<lon>,<lat>,<alt>"
	 */
	protected String getKMLString()
	{
		StringBuilder sb = new StringBuilder();
		
		if( location != null )
		{
		    double lat = location.getLatitude();
		    double lon = location.getLongitude();
		    double alt = location.getAltitude();
		    
		    sb.append( "kmlString:" ).append( lon ).append( "," );
		    sb.append( lat ).append( "," ).append( alt );		    
		}
		else
		{
			Log.e( TAG, "No location information set yet, exiting..." );
			System.exit( 1 );
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns
	 */
}
