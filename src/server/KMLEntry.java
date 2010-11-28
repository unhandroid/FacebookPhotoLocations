package server;

/**
 * This class represents an entry into a KML file.
 * 
 * @author Matthew Robinson
 *
 */
public class KMLEntry
{
    double latitude;
    double longitude;
    int altitude;
    
    KMLEntry( double lat, double lon, double alt )
    {
    	latitude = lat;
    	longitude = lon;
    	altitude = (int)alt;
    }
    
    /**
	 * Returns latitude, longitude, altitude (optional) string that will be
	 * used on the Server to update the KML file.
	 * 
	 * NOTE: No spaces allowed between commas and values!
	 * 
	 * @return A line containing info for the KML file.
	 */
    public String getKMLCoordinatesString()
    {

		StringBuilder sb = new StringBuilder();
		
		sb.append( "kml:" );
		sb.append( longitude ).append( "," ).append( latitude );
		
		if( altitude != -1 )
		{
			sb.append( "," ).append( altitude ); 
		}
		
		return sb.toString();
    }
    
    /**
     * Returns a String representation of this KML Entry.
     */
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( "Latitude: " ).append( latitude );
    	sb.append( "\nLongitude: " ).append( longitude );
    	sb.append( "\nAltitude: " ).append( altitude );
    	
    	return sb.toString();
    }
}
