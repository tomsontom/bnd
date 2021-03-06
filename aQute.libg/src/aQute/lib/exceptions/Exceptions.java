package aQute.lib.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Exceptions {
	private Exceptions() {}
	public static RuntimeException duck(Throwable t) {
		Exceptions.<RuntimeException> throwsUnchecked(t);
		throw new AssertionError("unreachable");
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}

	public static String toString(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
