package dea.monitor.checker;

import java.util.HashMap;

public interface MultiCheckI<T extends ChildCheckItemI> extends CheckItemI {
	HashMap<String, T> getChecks();
}
