package dea.monitor.reset;

public interface ResetI {
	void doReset(String bundleName) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException;

	void usage();
}
