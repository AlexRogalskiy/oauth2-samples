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
package samples.oauth2.nimbus.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.config.ClientConfiguration;
import org.springframework.security.oauth2.client.config.ClientConfigurationRepository;
import org.springframework.security.oauth2.client.config.InMemoryClientConfigurationRepository;

import java.util.List;

import static org.springframework.security.oauth2.client.config.annotation.web.configurers.AuthorizationCodeGrantFilterConfigurer.authorizationCodeGrant;
import static org.springframework.security.oauth2.client.config.annotation.web.configurers.AuthorizationRequestRedirectFilterConfigurer.authorizationRedirector;

/**
 *
 * @author Joe Grandja
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	// @formatter:off
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests()
					.anyRequest().fullyAuthenticated()
					.and()
				.apply(authorizationRedirector())
					.and()
				.apply(authorizationCodeGrant());
	}
	// @formatter:on

	@ConfigurationProperties(prefix = "security.oauth2.client.google")
	@Bean
	public ClientConfiguration googleClientConfiguration() {
		return new ClientConfiguration();
	}

	@ConfigurationProperties(prefix = "security.oauth2.client.github")
	@Bean
	public ClientConfiguration githubClientConfiguration() {
		return new ClientConfiguration();
	}

	@Bean
	public ClientConfigurationRepository clientConfigurationRepository(List<ClientConfiguration> clientConfigurations) {
		return new InMemoryClientConfigurationRepository(clientConfigurations);
	}
}