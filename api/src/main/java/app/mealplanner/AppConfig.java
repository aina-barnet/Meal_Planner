package app.mealplanner;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
// Repositories are declared as nested interfaces of persistence.Repositories.
@EnableJpaRepositories(basePackages = "app.mealplanner.persistence", considerNestedRepositories = true)
public class AppConfig {

	/** Injected wherever "today" matters, so tests can pin it. */
	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("Planificador d'àpats — API")
				.version("1.0")
				.description("""
						Local, single-household meal planner. No authentication: the app runs on the \
						family's own computer. All quantities are in each ingredient's canonical unit \
						(g or ml). Error responses are RFC 9457 problem details with a machine-readable \
						`code` and, for business rule violations, a list of `reasons`."""));
	}
}
