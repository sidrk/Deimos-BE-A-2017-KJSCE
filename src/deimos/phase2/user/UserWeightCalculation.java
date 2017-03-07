package deimos.phase2.user;

import java.sql.SQLException;

import deimos.phase2.DBOperations;

/**
 * Computes the weights as a product of TF and IDF
 * only for user databases.
 * 
 * @author Amogh Bhabal
 * @author Siddhesh Karekar
 */
public class UserWeightCalculation {

	static DBOperations dbo;

	static
	{
		try
		{
			dbo = new DBOperations();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * In case the IDF table has changed, you can
	 * optionally clear the weight column before you
	 * re-compute all the weights.
	 */
	public static void nullAllWeights(int user_id)
	{
		try
		{
			String query = String.format("UPDATE user_tf SET weight = null WHERE user_id = "+user_id);
			dbo.executeUpdate(query);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For each term in IDF table,
	 * Find all rows in user_tf of that term,
	 * And update their weight as tf*idf.
	 */
	public static void updateWeights(int user_id)
	{
		try
		{
			System.out.println("Weight calculation and updation for user_tf started.");

			// nullAllWeights();

			// Update weights
			String queryUpdate = "UPDATE user_tf SET user_tf.weight = user_tf.tf * ( SELECT idf FROM user_idf WHERE user_idf.term = user_tf.term ) WHERE user_id = "+user_id;
			dbo.executeUpdate(queryUpdate);

			System.out.println("Weight calculation and updation finished for user_tf!");
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		// TODO userid dont hardcode!! lols
		updateWeights(1);

	}

}