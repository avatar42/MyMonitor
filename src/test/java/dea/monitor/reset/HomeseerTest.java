/**
 * 
 */
package dea.monitor.reset;

import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dea.monitor.broadcast.Homeseer;
import dea.monitor.db.Props;
import dea.monitor.tools.Props2DB;

/**
 * @author avata
 *
 */
public class HomeseerTest {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	private static final String BUNDLE_NAME = "HomeseerTest";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		try {
			// setup to turn the desk light off and on as a reset test.
			// creating the values if needed.
			Props2DB p2d = new Props2DB();
			Props p = new Props(BUNDLE_NAME, p2d.getDbi(), null);
			p.updateProp("resetObjID", 3072);
			p.updateProp("wait", 1000);
			p.updateProp("onValue", 255f);
			p.updateProp("offValue", 0f);

		} catch (SQLException e1) {
			log.error("Failed connectiong to DB", e1);
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link dea.monitor.broadcast.Homeseer#doReset(java.lang.String)}.
	 */
	@Test
	public void testDoReset() {
		try {
			Homeseer hs = new Homeseer();
			hs.doReset(BUNDLE_NAME);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
