package dea.monitor.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTester {
	public static void main(String[] args) {

		String[] response = {
				"Station reported <span id=\"update_time\">11 seconds ago</span></span></h2>",
				"Station reported <span id=\"update_time\">1 minute ago</span></span></h2>",
				"Station reported <span id=\"update_time\">2 minutes ago</span></span></h2>" };
		Pattern pattern = null;
		String regexString = "seconds ago</span></span>|>[1-9] minute[s]{0,1} ago</span></span>";
		if (regexString != null) {
			pattern = Pattern.compile(regexString);
		}
		for (int i = 0; i < response.length; i++) {

			Matcher matcher = pattern.matcher(response[i]);
			System.out.println("found:" + matcher.find());
		}

	}
}
