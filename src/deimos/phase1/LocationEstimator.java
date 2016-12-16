package deimos.phase1;

import java.io.File;
import java.io.IOException;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;

/**
 * Uses Maxmind's GeoIP API to find a location mapping to a given public IP
 * address.
 * Much of this code was provided by
 * https://www.mkyong.com/java/java-find-location-using-ip-address/
 * @author mykong
 * @author Siddhesh Karekar
 *
 */

public class LocationEstimator {

	// Get the location object for an IP using the default GeoLiteCity database
	public static ServerLocation getLocation(String ipAddress) {

		File file = new File(
				"libs/Maxmind/GeoLiteCity.dat");
		return getLocation(ipAddress, file);

	}
	
	// Get the location object for an IP using a provided database
	public static ServerLocation getLocation(String ipAddress, File file) {

		ServerLocation serverLocation = null;

		try {

			serverLocation = new ServerLocation();

			LookupService lookup = new LookupService(file,LookupService.GEOIP_MEMORY_CACHE);
			Location locationServices = lookup.getLocation(ipAddress);

			serverLocation.setCountryCode(locationServices.countryCode);
			serverLocation.setCountryName(locationServices.countryName);
			serverLocation.setRegion(locationServices.region);
			serverLocation.setRegionName(regionName.regionNameByCode(
					locationServices.countryCode, locationServices.region));
			serverLocation.setCity(locationServices.city);
			serverLocation.setPostalCode(locationServices.postalCode);
			serverLocation.setLatitude(String.valueOf(locationServices.latitude));
			serverLocation.setLongitude(String.valueOf(locationServices.longitude));

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		return serverLocation;

	}
	
	// Get the location information for an IP passed as a parameter
	public static String getLocationString(String ipAddress)
	{

		ServerLocation location = LocationEstimator.getLocation(ipAddress);
		return location.toString();

	}
	
	// Get the location information of the given 
	public static String getLocationString()
	{
		String ipAddress = ExportIP.retrievePublicIP();
		ServerLocation location = LocationEstimator.getLocation(ipAddress);
		return location.toString();
	}

	public static void main(String[] args) {
		
		System.out.println(getLocationString());

	}
}