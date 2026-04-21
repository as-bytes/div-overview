package com.asbytes.helpers

import com.asbytes.Main

class Exceptions {

	static Object assertTrue(Object assertion, String message) {
		if (assertion == false) {
			throw new ValidationException(null, message)
		}
		return assertion
	}

	static Object assertNotNull(Object assertion, String message) {
		def exception = null
		if (assertion == null) {
			exception = new ValidationException(null, message)
		}
		if (assertion instanceof Double && Double.isNaN(assertion)) {
			exception = new ValidationException(null, message)
		}
		if (exception) {
			Main.printout("-" * 80)
			Main.printout(message + ("-" * 80))
			throw exception
		}
		return assertion
	}

	static void throwOnFalse(boolean assertion, String message) {
		if (!assertion) {
			throw new FrameworkException(null, message)
		}
	}

	class ValidationException extends Exception {
		ValidationException(String message) {
			super(message)
		}
	}

	class FrameworkException extends Exception {
		FrameworkException(String message) {
			super(message)
		}
	}
}
