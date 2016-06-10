package org.ngrinder.sso;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ngrinder.extension.OnServletFilter;
import org.ngrinder.service.IConfig;
import org.springframework.beans.factory.annotation.Autowired;

import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

public class SiteMinderLogoutFilter extends Plugin {

	public SiteMinderLogoutFilter(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class SiteMinderLogoutFilterExtension implements OnServletFilter {

		private String[] cookiesString;
		private String[] cookiesDomain;

		@Autowired
		public void setSpringExtensionFactory(IConfig config) {
			this.cookiesString = config.getControllerProperties().getProperty(
				"ngrinder.sso.domain", "").split(",");
			this.cookiesDomain = config.getControllerProperties().getProperty(
				"ngrinder.sso.cookiename", "").split(",");
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(final ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			if (httpServletRequest.getRequestURI().endsWith("/logout")) {
				HttpServletResponse httpServletResponse = (HttpServletResponse) response;
				String domain = "";
				String serverName = request.getServerName();
				for (String each : this.cookiesDomain) {
					if (serverName.contains(each)) {
						domain = each;
						break;
					}
				}
				for (String each : this.cookiesString) {
					Cookie cookie = new Cookie(each, "");
					cookie.setDomain(domain);
					cookie.setMaxAge(0);
					cookie.setPath("/");
					httpServletResponse.addCookie(cookie);
				}
				Cookie switchUser = new Cookie("switchUser", "");
				switchUser.setMaxAge(0);
				switchUser.setPath("/");
				switchUser.setDomain(serverName);
				httpServletResponse.addCookie(switchUser);
			}
			chain.doFilter(request, response);

		}

		@Override
		public void destroy() {
		}

	}
}