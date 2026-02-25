# Project Maintainer

*Project Maintainer* is a browser based to tool for maintaining applications managed in git repositories. Though being
used via a browser, it is as of now only intended to be run locally on your machine. Running the application on a
server, though technically possible, is strongly discouraged as multiple users are not supported.

## Disclaimers

1. This application stores credentials on your machine only, and does not send them to any external location
   apart from when they are needed for authentication purposes (e.g. when accessing Git repositories or APIs such as
   Github).
2. Your credentials will be encrypted automatically before stored on your disk. This encryption requires you to setup an
   *encryption master key*, See bellow on how to create custom encryption master key.
3. This application does not send any cloned repository content anywhere. Working copies and their content remain on
   your machine.
4. This application does not **automatically** perform any manipulation on source code, nor does it
   push any changes to the cloned repositories unless the user explicitly consents to it.
5. This application runs entirely on your machine, and does not facilitate any external logic on external servers or
   cloud services and alike, which are part of this project or related to it. APIs for source code management are exempt
   from this rule. These include:
    * Github API (when explicitly used by you)
    * Bitbucket Cloud API (when explicitly used by you)
    * AWS CodeCommit API (when explicitly used by you)
6. Please use this software at your own risk.

## Configuration

The application needs some setup work to be done, before it can be properly used.

### Encryption master key

To setup up an custom encryption master key you can:

1. Start the application with either an spring-profile-based YAML file (e.g. ``application-never-commit.yml``)
   containing:
   ```yaml
   project-maintainer:
     encryption-master-key: 'your-16-characters-wide-master-key'
   ```
   Never commit this file.
2. Start the application with the environment variable:
   ``PROJECTMAINTAINER_ENCRYPTIONMASTERKEY=your-16-characters-wide-master-key``

### Definition of project/repository locations

1. Open the [Workspace page](http://localhost:8080/workspace)
2. Create a workspace and open it
3. Create a project connection for a git provider (e.g. Bitbucket)
4. Save the workspace
5. Press "Discover projects" (If discovery fails, check your credential, and make sure they have the appropriate
   permissions.)
6. Go to the projects view in the menu to on the left side
7. Select the projects you are interested in, and press *attach*.

## FAQ

### Where are the clone repositories stored?

All data is stored in your home directory located at `~/.project-maintainer/`. Each workspace has its own
subdirectory containing various subfolders depending on the use case. This data is automatically cleaned up when
projects are detached. Manual operations in this directory are discouraged.

### Where are my credentials stored?

Credentials are stored for later reuse for when interacting with Bitbucket, Github and alike. These credentials belong
to a created workspace, and are located in an `~/.project-maintainer/workspace/{workspaceId}/workspace.yml` file. The
credentials are encrypted at rest using an encryption master key (see above on how to set one up).

At some point in time, the encryption of the credentials may change, causing the previously stored credentials
becoming unreadable. When this situation eventually arises, they will need be be re-configured via the ui, in order to
resume usage. It is therefore well advised, to store the credentials in some password manager, in order to keep track
of them and have them ready, when need be. **Project-maintainer is not replacement for a genuine password manager**.

## License

This software uses [custom Source Available License (SAL)](/LICENSE-1.0.md).

## Used external Assets

The following assets are used within the application, and included the source code. The above-mentioned license
does not apply to them.

* Github Logo: https://en.m.wikipedia.org/wiki/File:Octicons-mark-github.svg
* AWS CodeCommit Logo: https://aws.amazon.com/de/architecture/icons/
* Bitbucket Logo: https://atlassian.design/resources/logo-library#atlassian-product-logos