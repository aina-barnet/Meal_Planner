package app.mealplanner.web;

import app.mealplanner.application.Errors;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns failures into RFC 9457 problem details. Every problem carries a machine-readable
 * {@code code}; business rule violations also carry the human-readable {@code reasons}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Errors.NotFound.class)
	ProblemDetail notFound(Errors.NotFound e) {
		return problem(HttpStatus.NOT_FOUND, e.code(), e.getMessage());
	}

	@ExceptionHandler(Errors.Conflict.class)
	ProblemDetail conflict(Errors.Conflict e) {
		return problem(HttpStatus.CONFLICT, e.code(), e.getMessage());
	}

	@ExceptionHandler(Errors.RuleViolation.class)
	ProblemDetail ruleViolation(Errors.RuleViolation e) {
		ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, e.code(), e.getMessage());
		problem.setProperty("reasons", e.reasons());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Hi ha camps no vàlids.");
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(),
						"message", error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
				.toList();
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", code);
		return problem;
	}
}
