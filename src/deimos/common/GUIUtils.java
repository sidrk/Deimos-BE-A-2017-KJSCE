package deimos.common;

import javafx.concurrent.Service;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * A collection of some common methods used in JavaFX
 * GUI applications.
 * 
 * @author Siddhesh Karekar
 * @author Bhushan Pathak
 */
public class GUIUtils {
	
	/**
	 * An alternative to the factory restart() method.
	 * Avoids the cancellation of the service if it never started.
	 * Functionally near-equivalent to restart().
	 * @param s The service to start again
	 */
	public static void startAgain(Service<?> s) {
		if(s.isRunning())
			s.restart();
		else {
			s.reset();
			s.start();
		}
	}
	
	/**
	 * A quick function to generate error alerts.
	 * @param message
	 * @param stage
	 * @return
	 */
	public static Alert generateErrorAlert(String message, Stage stage)
	{
		Alert alert = generateAlert(message, stage, AlertType.ERROR);
		alert.setTitle("Something's not right.");
		return alert;
	}
	
	/**
	 * A quick function to generate alerts.
	 * @param message
	 * @param stage
	 * @param alt What kind of alert to generate
	 * @return
	 */
	public static Alert generateAlert(String message, Stage stage, AlertType alt)
	{
		Alert alert = new Alert(alt);
		alert.initOwner(stage);
		alert.setContentText(message);
		alert.showAndWait();
		return alert;
	}

}
