# Project Maintainer

## Disclaimers

1. This application does not store any credentials, nor does it send them to any external location
   apart from when they are needed for authentication purposes (e.g. when accessing Github).
2. This application does not send any cloned repository content anywhere. Everything is stored on
   the machine where the application is executed.
3. This application does not **automatically** perform any manipulation on source code, nor does it
   push any changes to the attached repositories unless the user explicitly consents to it.

## Configuration

### Repository locations

1. Define a `src/main/resources/application-local.yml` profile
   ```yaml
   project-maintainer:
     projects:
       discovery:
         bitbucket:
           users:
             - your-atlassian-email-adress@domain.org
   ```

### Credentials

1. Create a file `./credentials/passwords.properties`
2. Add the credentials from the `application-local.yml` file.
   ```
   bitbucket.your-atlassian-email-adress@domain.org=your-credential-in-plain-text
   ```

## Assets

* vue.js logo: https://github.com/vuejs/art/blob/master/logo.svg

* react: https://en.m.wikipedia.org/wiki/File:React-icon.svg
  This file is licensed under the Creative Commons Attribution-Share Alike 1.0 Generic license.


* nestjs: https://commons.wikimedia.org/wiki/File:NestJS.svg
  This logo image consists only of simple geometric shapes or text. It does not meet the threshold
  of originality needed for copyright protection, and is therefore in the public domain. Although it
  is free of copyright restrictions, this image may still be subject to other restrictions. See WP:
  PD § Fonts and typefaces or Template talk:PD-textlogo for more information.

* nuxtjs: https://en.wikipedia.org/wiki/File:Nuxt_logo.svg
  This file is licensed under the Expat License, sometimes known as the MIT License:

* angular: https://angular.io/presskit
  CC BY 4.0

* nextjs: https://en.wikipedia.org/wiki/File:Nextjs-logo.svg
  This file is licensed under the Creative Commons Attribution-Share Alike 4.0 International
  license.
  adjusted color to white

* unity: https://unity.com/de/legal/branding-trademarks

* wasm: https://de.wikipedia.org/wiki/Datei:WebAssembly_Logo.svg
  Diese Datei wird unter der Creative-Commons-Lizenz „CC0 1.0 Verzicht auf das Copyright“ zur
  Verfügung gestellt.
  https://github.com/carlosbaraza/web-assembly-logo/blob/master/dist/icon/web-assembly-icon.svg

* webpack: https://github.com/webpack/media/blob/master/logo/icon-square-big.svg
  public domain https://en.m.wikipedia.org/wiki/File:Webpack.svg

* github: https://en.m.wikipedia.org/wiki/File:Octicons-mark-github.svg
  MIT


* aws codecommit: https://aws.amazon.com/de/architecture/icons/

* bitbucket: https://atlassian.design/resources/logo-library#atlassian-product-logos