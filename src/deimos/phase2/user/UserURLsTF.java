package deimos.phase2.user;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

import org.jsoup.HttpStatusException;

import deimos.common.DeimosConfig;
import deimos.common.StatisticsUtilsCSV;
import deimos.common.StringUtils;
import deimos.phase2.DBOperations;
import deimos.phase2.collection.PageFetcher;
import deimos.phase2.collection.StemmerApplier;
import deimos.phase2.collection.StopWordsRemoval;

/**
 * 
 * Populates user tables for URLs and TF.
 * Parses user history currently kept in text file.
 * 
 * References:
 * www.mkyong.com/java/how-do-calculate-elapsed-execute-time-in-java/
 * 
 * @author Bhushan Pathak
 * @author Siddhesh Karekar
 */
public class UserURLsTF
{
	private static int currentURLNumber;
	private static String currentURL;
	private static String currentTimestamp;
	private static String currentTitle;
	private static int currentVisitCount;
	private static int currentTypedCount;
	private static String currentURLText;
	
	public static int getCurrentURLNumber()
	{
		return currentURLNumber;
	}
	
	public static int getURLsSize() {
		if(urls == null) {
			return -1;
		}
		else
			return urls.size();
	}

	/** Store all URLs in user history. */
	private static List<String> urls = new ArrayList<String>();

	/** Store all timestamps in user history for respective URLs. */
	private static List<String> urlTimeStamps = new ArrayList<String>();

	private static List<String> urlTitles = new ArrayList<String>();
	private static List<Integer> urlVisitCounts = new ArrayList<Integer>();
	private static List<Integer> urlTypedCounts = new ArrayList<Integer>();

	private static List<String> allowedWebsites;
	public static final String FILE_URL_FILTER = "resources/shoppingWebsites.txt";

	private static Map<String, Integer> currentURLTermCounts;

	private static int noOfURLs = DeimosConfig.LIMIT_URLS_DOWNLOADED;
	
	private static DBOperations dbo;
	
	// Logging functions

	/** Output the time taken per URL to a *.csv file. */
	private static boolean OPTION_RECORD_STATS_URL_TIMES = true;
	
	/** If false, blank the csv file */
	private static final boolean OPTION_RECORD_STATS_APPEND = false;

	/** The name of the CSV file to append info to. */
	private static final String FILENAME_STATS_URL_TIMES = "stats_url_times.csv";
	
	private static StatisticsUtilsCSV csvStats;

	static
	{
		// Initialize everything
		currentURLNumber = -1;
		currentURLTermCounts = new HashMap<>();
		loadAllowedWebsiteFilter();

		try {
			dbo = new DBOperations();
		} catch (SQLException e) {

			e.printStackTrace();
		}

		// file initialization
		if(OPTION_RECORD_STATS_URL_TIMES) {
			
			System.out.println("Logging for UserURLsTF is enabled.");
			
			try {
				csvStats = new StatisticsUtilsCSV(FILENAME_STATS_URL_TIMES, OPTION_RECORD_STATS_APPEND);

				// Header row
				if(!OPTION_RECORD_STATS_APPEND)
					csvStats.printAsCSV("Sr. No",
							"URL",
							"Time to fetch (ms)",
							"Success?",
							"Exception");
			}
			catch (IOException fnfe) {
				System.out.println(fnfe);
				System.out.println("Logging will be disabled.");
				OPTION_RECORD_STATS_URL_TIMES = false;
			}
		}
	}
	
	/**
	 * Checks whether user with given user-ID exists in database
	 * returns true if exists
	 * @param id
	 * @return
	 */

	public static boolean doesUserIdExist(int id) {
		boolean userIdFound = false;
		String queryCheck = "SELECT count(*) from user_urls WHERE user_id = "+id;
		try {
			ResultSet rs = dbo.executeQuery(queryCheck);
			int count = rs.getInt(1);
			if(count > 0) {
				userIdFound = true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userIdFound;
	}
	
	private static boolean isAnAllowedWebsite(String url)
	{
		for(String site : allowedWebsites)
			if(url.contains(site))
				return true;

		return false;
	}

	private static void loadAllowedWebsiteFilter()
	{
		allowedWebsites = new ArrayList<String>();
		// Contains list of all filters
		File shoppingWebsitesFile = new File(FILE_URL_FILTER);
		try
		{
			FileReader fileReader = new FileReader(shoppingWebsitesFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// add filters from external file to set
			String line;
			while ((line = bufferedReader.readLine()) != null) {

				line = line.trim();
				if(line.length() > 0) {
					allowedWebsites.add(line);
				}
			}
			fileReader.close();

			System.out.println("(Site filters loaded.)");

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Prepare the List of urls in user browsing history;
	 * load the history text file and parse it.
	 * 
	 * @param historyFileName The location of the history text file.
	 */
	private static void prepareHistory(String historyFileName)
	{
		File historyFile = new File(historyFileName); 

		try {
			FileReader fileReader = new FileReader(historyFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				urls.add(line);
			}
			fileReader.close();

			// remove the first line that has the number of URLs
			urls.remove(0);

			// Convert the URLs into URL + Timestamp
			for (int i = 0; i < urls.size(); i++)
			{
				// Format
				String[] urlPieces;
				if(DeimosConfig.DELIM.equals("|"))
					urlPieces = urls.get(i).split("\\|");
				else
					urlPieces = urls.get(i).split(DeimosConfig.DELIM);

				// Second 'piece' after splitting is the URL
				if(isAnAllowedWebsite(urlPieces[1])) {
					// Update arrayLists
					urlTimeStamps.add(urlPieces[0]);
					urls.set(i, urlPieces[1]); // Replace entire text line with only URL
					urlTitles.add(urlPieces[2]);
					urlVisitCounts.add(Integer.parseInt(urlPieces[3]));
					urlTypedCounts.add(Integer.parseInt(urlPieces[4]));

					System.out.format("%6d: timeStamp = %s, url = %s, title = %s, visitCount = %s, typedCount = %s\n",
							i, urlPieces[0], urlPieces[1], urlPieces[2], urlPieces[3], urlPieces[4]);
				}
				else {
					urls.remove(i);
					i--;
				}


			}
			System.out.format("Finished parsing user history of %d URL(s).\n", urls.size());

		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Prepare the List of urls in user browsing history,
	 * by default, use the exported output text file.
	 * 
	 * @param user_id NOT USED NOW!!
	 */
	private static void prepareHistory(int user_id)
	{ 
		// TODO use the history file of that user id

		prepareHistory(DeimosConfig.FILE_OUTPUT_HISTORY);
	}

	private static void tfTableInsertion(int user_id)
	{
		try
		{			
			currentURLTermCounts = StemmerApplier.stemmedWordsAndCount(currentURLText);

			// Print currentURLTermCounts
			// System.out.println(Collections.singletonList(currentURLTermCounts));

			// Put this stuff into the table
			int termsAlreadyInTable = 0; // For better error catching
			int totalTerms = currentURLTermCounts.size();
			for (Map.Entry<String, Integer> entry : currentURLTermCounts.entrySet())
			{
				String term = entry.getKey();

				if(term.length() > 50)
					term = term.substring(0, 50);

				Integer tf = entry.getValue();
				String queryUserTF = String.format(
						"INSERT INTO user_tf (user_id, url, term, tf, weight) VALUES (%d, '%s', '%s', %d, null)",
						user_id,
						currentURL,
						term,
						tf
						);

				try
				{
					dbo.executeUpdate(queryUserTF);
				}
				catch (SQLIntegrityConstraintViolationException sicve) {
					termsAlreadyInTable++;
				}
			}
			if(termsAlreadyInTable > 0)
				System.out.format("%d/%d URL terms already in user_tf.\n", termsAlreadyInTable, totalTerms);
			else
				System.out.println("Inserted into user_tf.");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static void userURLAndTFTableInsertion(int user_id)
	{
		// USE WITH CAUTION!
		dbo.truncateAllUserTables(user_id);
			

		// Prepare the urls List.
		prepareHistory(user_id);

		System.out.println("\nFetching pages and populating user_urls and user_tf...");
		for (currentURLNumber = 0; (currentURLNumber < noOfURLs && currentURLNumber < urls.size()) ; currentURLNumber++)
		{
			currentTimestamp = urlTimeStamps.get(currentURLNumber);
			currentURL = urls.get(currentURLNumber);
			currentTitle = urlTitles.get(currentURLNumber);
			currentVisitCount = urlVisitCounts.get(currentURLNumber);
			currentTypedCount = urlTypedCounts.get(currentURLNumber);



			// Only for printing
			int displayLen = 40;
			String displayURL = StringUtils.truncateURL(currentURL, displayLen);

			// only for printing
			// String displayTitle = String.format("%20s", currentTitle).substring(0, 15);

			System.out.format("%6d | ", currentURLNumber);
			System.out.print(currentTimestamp+" | "+displayURL + " | ");
			// System.out.print(displayTitle + " | ");

			long lStartTime, lEndTime;

			// Log to CSV
			lStartTime = Instant.now().toEpochMilli();

			Exception caughtEx = null;
			try
			{
				
				// All these exceptions!
				currentURLText = "";
				currentURLText = PageFetcher.fetchHTML(currentURL);
				if(currentURLText.isEmpty())
					throw new Exception("Text is Empty");

				currentURLText = StopWordsRemoval.removeStopWordsFromString(currentURLText);

				// System.out.println("URL Text:"+currentURLText);

				String queryUserURL = String.format(
						"INSERT INTO user_urls (user_id, url_timestamp, url, title, visit_count, typed_count) "
								+ "VALUES (%d, TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS'), '%s', '%s', %d, %d)",
								user_id, currentTimestamp, currentURL, currentTitle, currentVisitCount, currentTypedCount);

				// System.out.println(query);
				try
				{
					dbo.executeUpdate(queryUserURL);
					System.out.print("Inserted into user_urls | ");
				}
				catch (SQLIntegrityConstraintViolationException sicve) {
					System.out.print("Already in user_urls | ");
				}

				tfTableInsertion(user_id);
			}
			catch (SQLException sqle) {
				sqle.printStackTrace();
				
				caughtEx = sqle;
				System.out.println(caughtEx);
				
			}
			catch (IllegalArgumentException ile) {				
				caughtEx = ile;
				System.out.println(caughtEx+ ", Invalid URL - missed a protocol?");
			}
			catch (SocketException se) {				
				caughtEx = se;
				System.out.println(caughtEx);
			}
			catch (SocketTimeoutException ste) {
				caughtEx = ste;
				System.out.println(caughtEx);
			}
			catch (HttpStatusException hse) {
				caughtEx = hse;
				System.out.println(caughtEx);
			}
			catch (UnknownHostException uhe) {
				caughtEx = uhe;
				System.out.println(caughtEx);
			}
			catch (SSLHandshakeException sshe) {
				System.err.println(sshe);
				caughtEx = sshe;
			}
			catch (SSLProtocolException spe) {
				caughtEx = spe;
				System.out.println(caughtEx);
			}
			catch (SSLException sslxe){				
				/* General_Merchandise/D
				Current URL: http://www.davidmorgan.com/
				javax.net.ssl.SSLException: java.lang.RuntimeException: Could not generate DH keypair
				
				Caused by: java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64,
				and can only range from 512 to 2048 (inclusive)
				*/
				
				caughtEx = sslxe;
				System.out.println(caughtEx);
			}
			catch (Exception ex) {
				caughtEx = ex;
				System.out.println(caughtEx);
			}
			finally
			{
				if(OPTION_RECORD_STATS_URL_TIMES)
				{
					lEndTime = Instant.now().toEpochMilli();
					String ifFetchSuccess = (currentURLText.isEmpty())?"false":"true";
					String exceptString = (caughtEx == null)?"":caughtEx.toString();
					csvStats.printAsCSV(
							String.valueOf(currentURLNumber),
							currentURL,
							String.valueOf(lEndTime-lStartTime),
							ifFetchSuccess,
							exceptString);

				}
			}
		}

		System.out.println();
		System.out.println("Finished fetching pages and populating user_urls and user_tf!");

		if(OPTION_RECORD_STATS_URL_TIMES) {
			csvStats.closeOutputStream();
			System.out.println("Logging for UserURLsTF is finished.");
		}
	}

	public static void main(String[] args) {
		userURLAndTFTableInsertion(1);
	}
}
