package dea.monitor.reset;

import java.io.IOException;

/**
 * Interface to call to try and fix issue. Though in most cases it probably
 * makes more sense to send status to automation system and have it sort the
 * issue keeping the logic in one place. This can be used though if you do not
 * have an automation system to broadcast to or just want to remove a hop. As in
 * calling the Etekcity or Wemo API directly instead of asking the hub to do it.
 * 
 * @author avata
 *
 */
public interface ResetI {
	/**
	 * Call this method to trigger the reset of a device linked to the status object
	 * referenced by bundleName
	 * 
	 * @param bundleName
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	void doReset(String bundleName) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedOperationException, IOException;

	void usage();
}
