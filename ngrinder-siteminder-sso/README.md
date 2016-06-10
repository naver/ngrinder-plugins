ngrinder-siteminder-sso
=======================

ngrinder-siteminder-sso is the ngrinder plugin which allows users who passed siteminder served in a web server (like apache httpd) to login to ngrinder without additional login.

## How to build
	PF4J - Spring Framework integration
* clone the git repo.
** git clone https://github.com/naver/ngrinder-siteminder-sso
* build the plugin
** plugin-package
* copy the packaged plugin to ${NGRINDER_HOME}/plugins/

## How to configure sso plugin
* Open the system configuration editor in ngrinder
* Provide following configurations.
'''
ngrinder.sso.header.id=id_field_name_in_header_siteminder_provide
ngrinder.sso.header.name=name_field_name_in_header_siteminder_provide
ngrinder.sso.header.email=email_field_name_in_header_siteminder_provide
ngrinder.sso.header.cellphone=cellphone_field_name_in_header_siteminder_provide
ngrinder.sso.header.locale=locale_field_name_in_header_siteminder_provide
ngrinder.sso.header.timezone=timezone_field_name_in_header_siteminder_provide
ngrinder.sso.default.locale=default_locale_if_no_locale_is_provided
ngrinder.sso.default.timezone=default_timezone_if_no_timezone_is_provided
ngrinder.sso.domain=the_domain_name_of_ngrinder
ngrinder.sso.cookiename=the_cookiename_name_of_ngrinder
'''

This completly depends on the your siteminder configuration, please ask to your siteminder administrator.

## How to configure apache.
* Please ask your siteminder administrator to setup the apache.
  * You should ask him the provide the id, name, email, cellphone, timezone, locale in the headers to the backend WAS.
* Add the local login page.
