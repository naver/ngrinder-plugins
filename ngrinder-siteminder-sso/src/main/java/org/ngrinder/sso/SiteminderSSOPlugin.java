package org.ngrinder.sso;

import java.util.Map;

import net.grinder.util.NoOp;

import org.ngrinder.extension.OnLoginRunnable;
import org.ngrinder.model.Role;
import org.ngrinder.model.User;
import org.ngrinder.sso.SiteMinderFilter.SiteMinderFilterExtension;
import org.springframework.security.authentication.BadCredentialsException;

import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;

public class SiteminderSSOPlugin extends Plugin {

	public SiteminderSSOPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class SiteminderSSOPluginExtension implements OnLoginRunnable {

		@Override
		public User loadUser(final String userId) {
			Map<String, String> map = SiteMinderFilterExtension.threadStorage.get();
			User user = null;
			if (map != null) {
				user = new User();
				user.setUserId(userId);
				user.setUserName(getString(map, "name", ""));
				user.setEmail(getString(map, "email", ""));
				user.setMobilePhone(getString(map, "cellphone", ""));
				user.setAuthProviderClass(SiteminderSSOPluginExtension.this.getClass().getName());
				user.setEnabled(true);
				user.setExternal(true);
				user.setRole(Role.USER);
				SiteMinderFilterExtension.threadStorage.remove();
			}
			return user;
		}

		private String getString(Map<String, String> map, String key, String defaultValue) {
			String value = map.get(key);
			if (value == null) {
				return defaultValue;
			}
			return value;
		}

		@Override
		public boolean validateUser(String userId, String password, String encPass, Object encoder,
			Object salt) {
			throw new BadCredentialsException("no validation is permitted");
		}

		@Override
		public void saveUser(User user) {
			NoOp.noOp();
		}

	}
}
