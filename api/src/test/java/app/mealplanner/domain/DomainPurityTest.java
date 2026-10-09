package app.mealplanner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the architecture rule: domain classes are plain Java. No Spring, no JPA, no Hibernate,
 * no access to the application layer. Data in, data out.
 */
class DomainPurityTest {

	private static final Path DOMAIN_SOURCES = Path.of("src/main/java/app/mealplanner/domain");
	private static final List<String> FORBIDDEN = List.of(
			"import org.springframework",
			"import jakarta.",
			"import javax.persistence",
			"import org.hibernate",
			"import app.mealplanner.persistence",
			"import app.mealplanner.web",
			"import app.mealplanner.application");

	@Test
	void domainSourcesExist() {
		assertThat(DOMAIN_SOURCES).isDirectory();
	}

	@Test
	void domainDependsOnNothingButTheJdk() throws IOException {
		try (Stream<Path> files = Files.walk(DOMAIN_SOURCES)) {
			List<String> violations = files
					.filter(path -> path.toString().endsWith(".java"))
					.flatMap(DomainPurityTest::violations)
					.toList();
			assertThat(violations).isEmpty();
		}
	}

	private static Stream<String> violations(Path file) {
		try {
			return Files.readAllLines(file).stream()
					.map(String::strip)
					.filter(line -> FORBIDDEN.stream().anyMatch(line::startsWith))
					.map(line -> file.getFileName() + ": " + line);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
