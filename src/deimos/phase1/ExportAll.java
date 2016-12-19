package deimos.phase1;

import java.net.UnknownHostException;

import org.sqlite.SQLiteException;

/**
 * Combines all 4 export functions.
 * 
 * @author Bhushan Pathak
 * @author Amogh Bhabal
 * @author Siddhesh Karekar
 *
 */

public class ExportAll {

	public static void main(String[] args) {
		
		ExportBookmarks.retreiveBookmarksAsFile("export-bookmarks.txt");
		
		try {
			ExportCookies.retreiveCookiesAsFile("export-cookies.txt");
		} catch (SQLiteException e) {
			
			e.printStackTrace();
		}
		
		try {
			ExportHistory.retreiveHistoryAsFile("export-history.txt");
		}
		catch (SQLiteException sle) {
			
			sle.printStackTrace();
		}
		
		try {
			ExportIP.retrievePublicIPAsFile("export-publicIP.txt");
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		
		ExportUserInfo.retrieveUserInfoAsFile("John", "Doe",
				"male", 1995, "export-userInfo.txt");

	}

}
