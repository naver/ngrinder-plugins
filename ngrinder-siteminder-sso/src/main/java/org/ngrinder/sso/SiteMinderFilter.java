package org.ngrinder.sso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import net.grinder.util.NoOp;

import org.apache.commons.lang.StringUtils;
import org.ngrinder.common.util.PropertiesWrapper;
import org.ngrinder.extension.OnPreAuthServletFilter;
import org.ngrinder.service.IConfig;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class SiteMinderFilter extends Plugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(SiteMinderFilter.class);

	public SiteMinderFilter(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class SiteMinderFilterExtension implements OnPreAuthServletFilter {

		public static final ThreadLocal<Map<String, String>> threadStorage = new ThreadLocal<Map<String, String>>();

		@Autowired
		private AuthenticationManager authenticationManager;

		private String userIdHeader;
		private String userNameHeader;
		private String userEmailHeader;
		private String userCellPhoneHeader;
		private String userLocaleHeader;
		private String userTimezoneHeader;
		private String defaultLocale;
		private String defaultTimezone;

		@Autowired
		public void setSiteMinderFilterExtension(IConfig config) {
			PropertiesWrapper systemProperties = config.getSystemProperties();
			userIdHeader = systemProperties.getProperty("plugin.siteminder.header.id", "id");
			userNameHeader = systemProperties.getProperty("plugin.siteminder.header.name", "name");
			userEmailHeader = systemProperties.getProperty("plugin.siteminder.header.mail", "mail");
			userCellPhoneHeader = systemProperties.getProperty("plugin.siteminder.header.cellphone", "mail");
			userLocaleHeader = systemProperties.getProperty("plugin.siteminder.header.locale", "locale");
			userTimezoneHeader = systemProperties.getProperty("plugin.siteminder.header.timezone", "timezone");
			defaultLocale = systemProperties.getProperty("plugin.siteminder.header.default.locale", "en");
			defaultTimezone = systemProperties.getProperty("plugin.siteminder.header.default.timezone", "Asia/Seoul");
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(final ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			String userId = httpServletRequest.getHeader(userIdHeader);
			if (authentication == null && StringUtils.isNotEmpty(userId)) {
				userId = userId.toLowerCase();
				LOGGER.info("[NOTICE][SSO] {} is accessing through SSO", userId);
				PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(
					userId, "");
				token.setDetails(createDetails(httpServletRequest, userId));
				threadStorage.set(createEmpInfoFrom(httpServletRequest));
				Authentication authenticate = authenticationManager.authenticate(token);
				SecurityContextHolder.getContext().setAuthentication(authenticate);
			}
			chain.doFilter(request, response);
		}

		private Map<String, String> createDetails(final HttpServletRequest httpServletRequest,
			final String userName) {
			Map<String, String> details = new HashMap<String, String>();
			String locale = StringUtils.defaultIfBlank(
				httpServletRequest.getHeader(userLocaleHeader), defaultLocale);
			if (StringUtils.containsIgnoreCase(locale, "cn")) {
				locale = "cn";
			} else if (StringUtils.containsIgnoreCase(locale, "kr")) {
				locale = "kr";
			} else {
				// When the others.. return en
				locale = "en";
			}
			LOGGER.debug("{}'s locale is {}", userName, locale);
			details.put("user_language", locale);

			String timeZone = StringUtils.defaultIfBlank(
				httpServletRequest.getHeader(userTimezoneHeader), defaultTimezone);
			String[] split = StringUtils.split(timeZone, ":");
			if (split.length < 1 || StringUtils.isEmpty(split[0])) {
				timeZone = defaultTimezone;
			} else {
				timeZone = split[0];
			}
			LOGGER.debug("{}'s timeZone is {}", userName, timeZone);
			details.put("user_timezone", timeZone);
			return details;
		}

		Map<String, String> createEmpInfoFrom(HttpServletRequest request) {
			HashMap<String, String> map = new HashMap<String, String>();
			String name = request.getHeader(userNameHeader);
			if (name != null) {
				try {
					name = new String(name.getBytes("8859_1"), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					NoOp.noOp();
				}
			}
			map.put("name", name);
			map.put("email", request.getHeader(userEmailHeader));
			map.put("cellphone", request.getHeader(userCellPhoneHeader));
			return map;
		}

		@Override
		public void destroy() {
		}

	}

}
