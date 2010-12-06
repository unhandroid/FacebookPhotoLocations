package server;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
	String lastKML = null;
	
	/**
	 * Creates a KML Entry object using the String passed.
	 * Strings should be in one of these forms: 
	 * 	"latitude,longitude" or "latitude,longitude,altitude".
	 */
	private static KMLEntry createKMLEntry( String line )
	{
		// In case line contains a "kml:" part, take it out
        line = line.replace( "kml:", "" );
        
	    String[] tokens = line.split( "," );
	    
	    double latitude = Double.parseDouble( tokens[ 0 ] );
	    double longitude = Double.parseDouble( tokens[ 1 ] );
	    double altitude = Double.parseDouble( tokens[ 2 ] );

	    
	    return new KMLEntry( latitude, longitude, altitude );	    
	}
	
    public static void main(String[] args) throws IOException 
    {

        ServerSocket serverSocket = null;
    	
        try 
        {
            serverSocket = new ServerSocket(8877);
            System.out.println( "Starting server on port 8877..." );
        } 
        catch (IOException e) 
        {
            System.err.println("Could not listen on port: 8877");
            System.exit(1);
        }

        Socket clientSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        while ( true ) 
        {
            try
            {
                clientSocket = serverSocket.accept();
                System.out.println( "Server accepted a client..." );
            }
            catch( IOException e )
            {
                System.err.println( "Accepting client failed, exiting..." );
                System.exit( 1 );
            }

            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader( new InputStreamReader(
                                          clientSocket.getInputStream()));
            
            System.out.println( "Reading in picture information..." );
            String inputLine;
            StringBuilder pictureBuffer = new StringBuilder();
            String kmlLine = null;
            String facebookId = null;
            KMLEntry kmlEntry = null;
            while( ( inputLine = in.readLine() ) != null )
            {
            	if( inputLine.startsWith( "kmlString:" ) )
            	{
            		kmlLine = inputLine.replace( "kmlString:", "" );
            		kmlEntry = createKMLEntry( kmlLine ); 
            	}
                else if( inputLine.startsWith( "facebookid:") )
                {
                        facebookId = inputLine.replace( "facebookid:", "" ); 
                }
            	else
                {
            		pictureBuffer.append( inputLine );
                }
            }
            
            String pictureString = pictureBuffer.toString();
                
            System.out.println( "Decoding String into byte array..." );
            byte[] picBytes = Base64.decode( pictureString );
            
            System.out.println( "Converting byteArray to jpg..." );
            FileOutputStream f = new FileOutputStream( "testPic.jpg" );
        	f.write( picBytes );
        	
        	// Close streams
        	f.close();
            out.close();
            in.close();
            clientSocket.close();
            
            // DEBUG
            System.out.println( "Picture received with the following KML entry:" );
            System.out.println( kmlEntry.toString() );
            System.out.println( "And the following facebook id: " + facebookId );
        }
    }
}
