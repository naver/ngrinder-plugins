# ngrinder-siteminder-sso

ngrinder-siteminder-sso is the ngrinder plugin which allows users who passed siteminder served in a web server (like apache httpd) to login to ngrinder without additional login.

## How to build
You can build ngrinder siteminder-sso plugin with [Plugin Framework for Java (PF4J)](https://github.com/pf4j/pf4j).
Check with wiki documentation.

- en : [How-to-develop-plugin](https://github.com/naver/ngrinder/wiki/How-to-develop-plugin)
- kr : [How-to-develop-plugin-in-kr](https://github.com/naver/ngrinder/wiki/How-to-develop-plugin-in-kr)

## How to configure sso plugin
* Open the system configuration editor in ngrinder
* Provide following configurations.

```
plugin.siteminder.header.id=id_field_name_in_header_siteminder_provide
plugin.siteminder.header.name=name_field_name_in_header_siteminder_provide
plugin.siteminder.header.mail=email_field_name_in_header_siteminder_provide
plugin.siteminder.header.locale=locale_field_name_in_header_siteminder_provide
plugin.siteminder.header.timezone=timezone_field_name_in_header_siteminder_provide
plugin.siteminder.default.locale=default_locale_if_no_locale_is_provided
plugin.siteminder.default.timezone=default_timezone_if_no_timezone_is_provided
plugin.siteminder.cookie.domain=the_domain_name_of_ngrinder
plugin.siteminder.cookie.logout=the_cookiename_name_of_ngrinder
```

This completly depends on the your siteminder configuration, please ask to your siteminder administrator.

## How to configure apache.
* Please ask your siteminder administrator to setup the apache.
  * You should ask him the provide the id, name, email, timezone, locale in the headers to the backend WAS.
* Add the local login page.
