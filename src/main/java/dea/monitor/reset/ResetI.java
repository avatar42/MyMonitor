package dea.monitor.reset;

/**
 * Interface to call to try and fix issue. Though in most cases it probably makes
 * more sense to send status to automation system and have it sort the issue
 * keeping the logic in one place. This can be used though if you do not have an
 * automation system to broadcast to.
 * 
 * @author avata
 *
 */
public interface ResetI {
	void doReset(String bundleName) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

	void usage();
}
