/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.client.config.annotation.web.configurers;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.config.ClientConfiguration;
import org.springframework.security.oauth2.client.config.ClientConfigurationRepository;
import org.springframework.security.oauth2.client.config.InMemoryClientConfigurationRepository;
import org.springframework.security.oauth2.client.filter.AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.filter.AuthorizationRequestUriBuilder;
import org.springframework.security.oauth2.client.filter.nimbus.NimbusAuthorizationRequestUriBuilder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Joe Grandja
 */
public final class AuthorizationRequestRedirectFilterConfigurer<B extends HttpSecurityBuilder<B>> extends
		AbstractHttpConfigurer<AuthorizationRequestRedirectFilterConfigurer<B>, B> {

	private String authorizationProcessingBaseUri;

	private AuthorizationRequestUriBuilder authorizationUriBuilder;

	public AuthorizationRequestRedirectFilterConfigurer<B> authorizationProcessingBaseUri(String authorizationProcessingBaseUri) {
		this.authorizationProcessingBaseUri = authorizationProcessingBaseUri;
		return this;
	}

	public AuthorizationRequestRedirectFilterConfigurer<B> clientConfigurationRepository(ClientConfigurationRepository clientConfigurationRepository) {
		this.getBuilder().setSharedObject(ClientConfigurationRepository.class, clientConfigurationRepository);
		return this;
	}

	public AuthorizationRequestRedirectFilterConfigurer<B> authorizationUriBuilder(AuthorizationRequestUriBuilder authorizationUriBuilder) {
		this.authorizationUriBuilder = authorizationUriBuilder;
		return this;
	}

	@Override
	public void configure(B http) throws Exception {
		AuthorizationRequestRedirectFilter filter = new AuthorizationRequestRedirectFilter(
				this.getAuthorizationProcessingBaseUri(),
				this.getClientConfigurationRepository(),
				this.getAuthorizationUriBuilder());

		// TODO Temporary workaround
		// 		Remove this after we add an order in FilterComparator for AuthorizationRequestRedirectFilter
		this.addObjectPostProcessor(new OrderedFilterWrappingPostProcessor());

		http.addFilter(this.postProcess(filter));
	}

	private String getAuthorizationProcessingBaseUri() {
		return (this.authorizationProcessingBaseUri != null ?
				this.authorizationProcessingBaseUri : AuthorizationRequestRedirectFilter.DEFAULT_FILTER_PROCESSING_BASE_URI);
	}

	private ClientConfigurationRepository getClientConfigurationRepository() {
		ClientConfigurationRepository clientConfigurationRepository = this.getBuilder().getSharedObject(ClientConfigurationRepository.class);
		if (clientConfigurationRepository == null) {
			ApplicationContext context = this.getBuilder().getSharedObject(ApplicationContext.class);
			Map<String, ClientConfiguration> clientConfigurations = context.getBeansOfType(ClientConfiguration.class);
			Assert.state(!CollectionUtils.isEmpty(clientConfigurations),
					"There must be at least 1 bean configured of type " + ClientConfiguration.class.getName());
			clientConfigurationRepository = new InMemoryClientConfigurationRepository(
					clientConfigurations.values().stream().collect(Collectors.toList()));
			this.getBuilder().setSharedObject(ClientConfigurationRepository.class, clientConfigurationRepository);
		}
		return clientConfigurationRepository;
	}

	private AuthorizationRequestUriBuilder getAuthorizationUriBuilder() {
		if (this.authorizationUriBuilder == null) {
			this.authorizationUriBuilder = new NimbusAuthorizationRequestUriBuilder();
		}
		return this.authorizationUriBuilder;
	}

	public static AuthorizationRequestRedirectFilterConfigurer<HttpSecurity> authorizationRedirector() {
		AuthorizationRequestRedirectFilterConfigurer<HttpSecurity> configurer = new AuthorizationRequestRedirectFilterConfigurer<>();

		return configurer;
	}

	// TODO Temporary workaround
	// 		Remove this after we add an order in FilterComparator for AuthorizationRequestRedirectFilter
	private final class OrderedFilterWrappingPostProcessor implements ObjectPostProcessor<Object> {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object postProcess(final Object delegateFilter) {
			AbstractPreAuthenticatedProcessingFilter orderedFilter = new AbstractPreAuthenticatedProcessingFilter() {

				@Override
				public void doFilter(ServletRequest request, ServletResponse response,
									 FilterChain chain) throws IOException, ServletException {

					((AuthorizationRequestRedirectFilter)delegateFilter).doFilter(request, response, chain);
				}

				@Override
				protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
					return null;
				}

				@Override
				protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
					return null;
				}
			};
			return orderedFilter;
		}
	}
}