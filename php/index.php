
<?php

// Home page of the Facebook Photo Location Map
// This page retrieves the logged in user's ID then dynamically displays photos they've taken
// on a Google map.

include_once "facebook.php";

define('FACEBOOK_APP_ID', '178069788885403');
define('FACEBOOK_SECRET', '63d1c90f5cfea92abdf189113f2c61cb');

function get_facebook_cookie($app_id, $application_secret) {
  $args = array();
  parse_str(trim($_COOKIE['fbs_' . $app_id], '\\"'), $args);
  ksort($args);
  $payload = '';
  foreach ($args as $key => $value) {
    if ($key != 'sig') {
      $payload .= $key . '=' . $value;
    }
  }
  if (md5($payload . $application_secret) != $args['sig']) {
    return null;
  }
  return $args;
}

/**
 * Returns a String consisting of the necessities for a new KML file.
 */
function getKMLOpeningTags($name)
{
    $tags = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	$tags = $tags."<kml xmlns=\"http://earth.google.com/kml/2.0\">\n";
	$tags = $tags."<Document>";
	$tags = $tags."<name>".$name."'s Facebook Photo Locations</name>\n";
	$tags = $tags."<description>".$name."'s Facebook Photo Locations</description>\n";

	return $tags;
}

/**
 * Returns a String consisting of the necessary placemark tags using the
 * KML coordinates and placemark name passed.
 * 
 * @param kmlEntry
 * @param placemarkName
 * @return
 */
function getPlacemarkTags( $kmlEntries, $PHOTOSDIR, $name )
{	
	// XML Strings needed to create a KML file
	$PLACEMARK_OPENING_TAG = "<Placemark>";
	$PLACEMARK_CLOSING_TAG = "</Placemark>";
	$NAME_OPENING_TAG = "<name>";
	$NAME_CLOSING_TAG = "</name>";
	$POINT_OPENING_TAG = "<Point>";
	$POINT_CLOSING_TAG = "</Point>";
	$COORDINATES_OPENING_TAG = "<coordinates>";
	$COORDINATES_CLOSING_TAG = "</coordinates>";
	$STYLE_URL_OPENING_TAG = "<styleUrl>";
	$STYLE_URL_CLOSING_TAG = "</styleUrl>";
	
	// LOOP through each kml entry to create appropriate kml file.
	$i=1;
	foreach( $kmlEntries as $entry )
	{
		// remove "kml:" from string
		$entry = str_replace( "kml:", "", $entry );
		
		// Get tokens
		$tokens = explode( ",", $entry );
		$img = $tokens[ 0 ];
	    $id = explode( ".", $img );
	    $id = $id[ 0 ];
		$id_notimestamp = explode( "_", $id );
		$id_notimestamp = $id_notimestamp[ 0 ];
		$longitude = $tokens[ 2 ];
		$latitude  = $tokens[ 1 ];
		$altitude  = $tokens[ 3 ];
		
		$coordinates = $longitude.",".$latitude.",".$altitude;
		$coordinates = str_replace( "\n", "", $coordinates );
		
		$PHOTOS_URL = "http://w2-25239.unh.edu/~mattrobinson/PhotoLocationMap/photos/".$id_notimestamp."/";
		
		// get style url to display picture as the icon
		$style = "\n<Style id=\"".$id."\">\n";
		$style = $style."  <IconStyle>\n";
		$style = $style."    <scale>1.2</scale>\n    <Icon>\n";
		$style = $style."        <href>".$PHOTOS_URL.$img."</href>\n";
		
		$style = $style."    </Icon>\n  </IconStyle>\n";
		$style = $style."  <BalloonStyle>\n  <bgColor>ffffffbb</bgColor>\n";
		$style = $style."    <text><![CDATA[<font color=\"#CC0000\" size=\"-3\">Latitude: ".$latitude."<br>Longitude: ".$longitude."<br>";
		$style = $style."Altitude: ".$altitude."M<br><a href=\"".$PHOTOS_URL.$img."\"target=\"_blank\"><img src = \"".$PHOTOS_URL.$img."\" width=\"450\" height=\"200\">";
		$style = $style."</a><br></font>]]></text>\n";
		$style = $style."  </BalloonStyle>\n";
		$style = $style."</Style>";
		
		// make the actual placemark for this photo
	    $tags = $style."\n";
		$tags = $tags.$PLACEMARK_OPENING_TAG."\n".$NAME_OPENING_TAG;
		$tags = $tags.$name.$i;
		$tags = $tags.$NAME_CLOSING_TAG."\n";
		$tags = $tags.$STYLE_URL_OPENING_TAG."#".$id.$STYLE_URL_CLOSING_TAG."\n";
		$tags = $tags.$POINT_OPENING_TAG.$COORDINATES_OPENING_TAG;
		$tags = $tags.$coordinates;
		$tags = $tags.$COORDINATES_CLOSING_TAG.$POINT_CLOSING_TAG."\n";
		$tags = $tags.$PLACEMARK_CLOSING_TAG."\n";
		
		$placemarkTags = $placemarkTags.$tags;	// accumulator of all tags
		$i = $i + 1;
	}
	
	return $placemarkTags;
}

/**
 * Provides the end tags for the KML file.
 */
function getKMLClosingTags()
{	
	$tags = "</Document>\n";
	$tags = $tags."</kml>";
	
	return $tags;
}
	
$cookie = get_facebook_cookie(FACEBOOK_APP_ID, FACEBOOK_SECRET);
$facebook = new Facebook(array('appId'=>'178069788885403','secret'=>'63d1c90f5cfea92abdf189113f2c61cb','cookie'=>true ));
$session = $facebook->getSession();

?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:fb="http://www.facebook.com/2008/fbml">
  <body>
  <?php 
  if ($cookie) 
  { 
	// Get the Facebook ID of the current user.
    $me = $facebook->api('/me');
 	$name = $me['name'];
 	$id = $me['id'];		// Matt Robinson's ID: 11013226
	
	// All pertinent files are based on the user id running this application
	// ./ is public_html
	//$STORAGE = "../LocationPhotoMap/";		// this is what should be used when it's on the Zeno server..
	$STORAGE = "./";
	$PHOTOSDIR = $STORAGE."photos/".$id;
	$KMLFILE = $STORAGE."kml/".$id."_kmlentries.txt";
	
 	// DEBUG
 	echo "<strong>Hi ".$name."!</strong><br>";
	echo "<br><em>* Click once on a cluster to expand to a set of pictures.";
	echo "<br>* Click again on the specific picture to view the actual image itself.</em>";
	//echo "<br>Your ID is ".$id."<br>";

 	// Fetch the user's friends
 	// In the future, can display friend's pictures as well...                                      
 	//$friends = $facebook->api('/me/friends'); // array of this user's friends
    //var_dump($friends);
		
	// Dynamically create KMZ file by obtaining the necessary KML files and photos using the
	// currently logged-in Facebook user's ID.
	
	// Get opening KML file tags
	$contents = getKMLOpeningTags($name);
	
	// Retrieve all necessary information concerning this user's photos and kml entries
	$kmlEntries = file($KMLFILE);
	
	// Construct the KML placemark tags using the kml entries obtained
	$placemarks = getPlacemarkTags( $kmlEntries, $PHOTOSDIR, $name );
	$contents = $contents.$placemarks;
	
	// Get ending KML tags and close out the file
	$contents = $contents.getKMLClosingTags();
	
	// Create KML File
	$kmlFile = "doc.kml";
	$fileWriter = fopen( $kmlFile, 'w' );
	fwrite( $fileWriter, $contents );
	fclose( $fileWriter );
	
	// *** Need to create a unique doc.kml file for each individual user logged in and have the correct gadget script (with the unique kml) run below
	
	?>
<script src="http://www.gmodules.com/ig/ifr?url=http://code.google.com/apis/kml/embed/embedkmlgadget.xml&amp;up_kml_url=http%3A%2F%2Fw2-25239.unh.edu%2F~mattrobinson%2FPhotoLocationMap%2Fdoc.kml&amp;up_view_mode=earth&amp;up_earth_2d_fallback=1&amp;up_earth_fly_from_space=1&amp;up_earth_show_nav_controls=1&amp;up_earth_show_buildings=1&amp;up_earth_show_terrain=1&amp;up_earth_show_roads=1&amp;up_earth_show_borders=1&amp;up_earth_sphere=earth&amp;up_maps_zoom_out=0&amp;up_maps_default_type=map&amp;synd=open&amp;w=600&amp;h=350&amp;title=Photo+Locations&amp;border=%23ffffff%7C1px%2C1px+solid+black%7C1px%2C1px+solid+black%7C0px%2C1px+black&amp;output=js"></script>
  
 <?php
  }
   else 
   { ?> Please click here to allow this application access: <fb:login-button></fb:login-button>
      
    <?php } ?>

    <div id="fb-root"></div>
    <script src="http://connect.facebook.net/en_US/all.js"></script>
    <script>
      FB.init({appId: '<?= FACEBOOK_APP_ID ?>', status: true,
               cookie: true, xfbml: true});
      FB.Event.subscribe('auth.login', function(response) {
        window.location.reload();
      });
    </script>
  </body>
</html>
