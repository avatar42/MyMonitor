package dea.monitor.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class EncodeString {
	public Set<String> vaildTypes = new HashSet<String>();

	public EncodeString() {
		vaildTypes.add("MD5");
		vaildTypes.add("SHA1");
	}

	public String genResponse(String encodeType, String txt) throws NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance(encodeType);
		byte[] arr = txt.getBytes();
		arr = md.digest(arr);
		StringBuilder sb = new StringBuilder();
		for (byte b : arr) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public void usage() {
		System.err.println("Usage: EncodeString encodeType textToEncode");
		System.err.println("Where encodeType is one of:");
		for (String encodeType : vaildTypes) {
			System.err.println(encodeType);
		}
	}

	public static void main(String[] args) {
		EncodeString item = new EncodeString();
		try {
			if (args.length < 2)
				item.usage();
			else if (!item.vaildTypes.contains(args[0]))
				item.usage();
			else {
				System.out.println(item.genResponse(args[0], args[1]));
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			item.usage();
		}
	}

}
