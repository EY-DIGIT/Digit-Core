package org.egov.tracer.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentracing.noop.NoopTracerFactory;
import org.egov.tracer.http.RestTemplateLoggingInterceptor;
import org.egov.tracer.http.filters.TracerFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"org.egov.tracer"})
@PropertySource("classpath:tracer.properties")
@EnableConfigurationProperties({TracerProperties.class})
//@Import(OpenTracingConfiguration.class)
public class TracerConfiguration {

	@Bean
	public ObjectMapperFactory objectMapperFactory(TracerProperties tracerProperties, Environment environment) {
		return new ObjectMapperFactory(tracerProperties, environment);
	}

	@Bean(name = "logAwareRestTemplate")
	public RestTemplate logAwareRestTemplate(RestTemplateBuilder builder, TracerProperties tracerProperties) {
		return builder
				.requestFactory(() -> {
					SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
					factory.setOutputStreaming(false);
					return new BufferingClientHttpRequestFactory(factory);
				})
				.interceptors(new RestTemplateLoggingInterceptor(tracerProperties))
				.build();
	}

	/**
	 * Configure tracer filter with order one
	 *
	 * @param objectMapperFactory Object mapper
	 * @param tracerProperties    configuration for the filter
	 * @return Filter
	 */
	@Bean
	@ConditionalOnProperty(name = "tracer.filter.enabled",
			havingValue = "true", matchIfMissing = true)
	public FilterRegistrationBean tracerFilter(ObjectMapperFactory objectMapperFactory,
											   TracerProperties tracerProperties) {
		final TracerFilter tracerFilter = new TracerFilter(tracerProperties, objectMapperFactory);
		FilterRegistrationBean registration = new FilterRegistrationBean(tracerFilter);
		registration.addUrlPatterns("/*");
		registration.setName("TracerFilter");
		registration.setOrder(1);
		return registration;
	}

	/**
	 * Disable open tracing by injecting a Noop
	 *
	 * @return Noop tracer
	 */
	@Bean
	@ConditionalOnProperty(
			name = {"tracer.opentracing.enabled"},
			havingValue = "false",
			matchIfMissing = true
	)
	public io.opentracing.Tracer tracer() {
		return NoopTracerFactory.create();
	}

	@Bean
	public OtlpGrpcSpanExporter otlpHttpSpanExporter(@Value("${tracing.url}") String url) {
		return OtlpGrpcSpanExporter.builder().setEndpoint(url).build();
	}
}


