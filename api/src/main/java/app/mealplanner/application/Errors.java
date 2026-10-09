package app.mealplanner.application;

import java.util.List;

/** Application exceptions, translated to HTTP problem responses by the web layer. */
public final class Errors {

	private Errors() {
	}

	/** 404: the resource does not exist. */
	public static class NotFound extends RuntimeException {

		private final String code;

		public NotFound(String code, String message) {
			super(message);
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	/** 409: the request conflicts with the current state (e.g. editing a confirmed week). */
	public static class Conflict extends RuntimeException {

		private final String code;

		public Conflict(String code, String message) {
			super(message);
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	/** 422: the request is well-formed but breaks a business rule. */
	public static class RuleViolation extends RuntimeException {

		private final String code;
		private final List<String> reasons;

		public RuleViolation(String code, String message, List<String> reasons) {
			super(message);
			this.code = code;
			this.reasons = List.copyOf(reasons);
		}

		public String code() {
			return code;
		}

		public List<String> reasons() {
			return reasons;
		}
	}
}
