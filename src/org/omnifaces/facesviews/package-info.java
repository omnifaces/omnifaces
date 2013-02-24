/**
 * <h2>This package contains the classes for the OmniFaces' FacesViews feature.</h2>
 * <p>
 * FacesViews is a feature where a special dedicated directory (<code>/WEB-INF/faces-views</code>), or optionally
 * one or more user specified directories, can be used to store Facelets source files. <br>
 * 
 * All files found in these directory are automatically mapped as Facelets files
 * and made available using both their original extension as well as without an extension (extensionless). Optionally scanning
 * can be restricted to include only certain extensions.
 *
 * <p>
 * With FacesViews, there is thus no need to list all Facelets views that should be accessed without an extension in
 * some configuration file. Additionally, it thus automatically maps Facelets files to their original file extension, which
 * prevents exposing the source code of those Facelets that happens with the default JSF mapping.
 *
 * <p>
 * In Servlet 3.0 containers, scanning is done automatically and no further configuration is needed. The feature
 * is compatible with applications that don't have <code>web.xml</code> or <code>faces-config.xml</code> configuration
 * files. As such, it can be used as an alternative to declaring the {@link javax.faces.webapp.FacesServlet} in <code>web.xml</code>
 * for the <code>.xhtml</code> to <code>.xhtml</code> mapping.
 *
 * <p>
 * <b>Example 1:</b><br>
 *
 * Consider the following file structure and assume no further configuration has been done:
 * <pre>
 *    /WEB-INF/faces-views/index.xhtml
 *    /WEB-INF/faces-views/users/add.xhtml
 *    /normal.xhtml
 * </pre>
 * This will make the Facelets available via the following URLs (given a root deployment on domain <code>example.com</code>):
 * <pre>
 *    example.com/index
 *    example.com/users/add
 *    example.com/index.xhtml
 *    example.com/users/add.xhtml
 *    example.com/normal.xhtml
 * </pre>
 *
 * Note that although the directory outside <code>/WEB-INF/faces-views</code> is not scanned, the {@link javax.faces.webapp.FacesServlet}
 * <em>is</em> mapped on all extensions found in <code>/WEB-INF/faces-views</code>, so this will also affect files outside
 * this directory. In the above example <code>normal.xhtml</code> is thus also available via the <code>.xhtml</code> extension, since
 * the whole FacesServlet is mapped on this.
 * 
 * <p>
 * <b>Example 2:</b><br>
 * 
 * Consider the following configuration and file structure: <br><br>
 * 
 * <small><b>web.xml</b></small>
 * <pre>
 *     &lt;context-param&gt;
 *       &lt;param-name&gt;org.omnifaces.FACES_VIEWS_SCAN_PATHS&lt;/param-name&gt;
 *       &lt;param-value&gt;/*.xhtml&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 * </pre>
 * 
 * <small><b>File structure</b></small>
 * <pre>
 *    /page1.xhtml
 *    /foo/page2.xhtml
 *    /WEB-INF/resources/template.xhtml
 *    /script.js
 * </pre>
 * This will make the Facelets available via the following URLs (given a root deployment on domain <code>example.com</code>):
 * <pre>
 *     example.com/page1
 *     example.com/foo/page2
 *     example.com/page1.xhtml
 *     example.com/foo/page2.xhtml
 * </pre>
 * 
 * Note that in the above example, <code>WEB-INF</code> was NOT scanned and thus <code>template.xhtml</code> is not made publicly available. Likewise
 * <code>script.js</code> was also not scanned since it doesn't have the configured extension (.xhtml). Finally, although a web.xml was used, there
 * does not need to be a mapping for the <code>FacesServlet</code> in it when using a Servlet 3.0 container. 
 * 
 * <p>
 * JSF links in which logical view ids are used will be rendered as either extensionless or with an extension based on whether
 * the request in which this rendering is done was extensionless or not. E.g. consider the following link on
 * <code>/WEB-INF/faces-views/index.xhtml</code>:
 * <pre>
 * &lt;h:link value="Add user" outcome="/users/add" /&gt;
 * </pre>
 * This will render as <code>/users/add</code> if the request was to <code>/index</code> and as <code>/users/add.xhtml</code> if the
 * request was to <code>/index.xhtml</code>. This behavior can be changed so that such links are always rendered as the extensionless
 * version using a configuration parameter (see below).
 *
 * <p>
 * <h3>Configuration</h3>
 * The following context parameters are available.
 * <table>
 * <tr>
 * <td nowrap><code>{@value org.omnifaces.facesviews.FacesViews#FACES_VIEWS_ENABLED_PARAM_NAME}</code></td>
 * <td>Used to completely switch scanning off. Allowed values: {<code>true</code>,<code>false</code>} Default: <code>true</code>
 * (note that if no <code>/WEB-INF/faces-views</code> directory is present, no scanning will be done either)</td>
 * </tr>
 * <tr>
 * <td nowrap><code>{@value org.omnifaces.facesviews.FacesViews#FACES_VIEWS_SCAN_PATHS_PARAM_NAME}</code></td>
 * <td>A comma separated list of paths that are to be scanned in addition to <code>/WEB-INF/faces-views</code>. Allowed values: any path relative
 * to the web root, including the root path (<code>/</code>) and <code>/WEB-INF</code>. A wildcard can be added to the path, which will cause
 * only files with the given extension te be scanned. Example: Scan all files in both folder1 and folder2 <code>/folder1, /folder2</code>, Scan only
 * .xhtml files in the root <code>/*.xhtml</code>. Note that when the root path is given, all its sub paths are also scanned EXCEPT WEB-INF and META-INF.
 * If those have to be scanned as well, they can be added to the list of paths explicitly. Default: <code>/WEB-INF/faces-views</code> (note when this value
 * is set, those paths will be in addition to the default <code>/WEB-INF/faces-views</code>)
 * </td>
 * </tr>
 * <tr>
 * <td nowrap><code>{@value org.omnifaces.facesviews.FacesViews#FACES_VIEWS_SCANNED_VIEWS_EXTENSIONLESS_PARAM_NAME}</code></td>
 * <td>Used to set that scanned views should always be rendered extensionless when used in JSF controlled links. Without this setting
 *  (or it being set to false), it depends on whether the request URI uses an extension or not. If it doesn't, links are also rendered without one,
 *  otherwise they are rendered with an extension. Default: <code>false</code>
 * </td>
 * </tr>
 * </table>
 * 
 * <p>
 * <h3>Servlet 2.5 configuration</h3>
 * Servlet 2.5 users will have to install the {@link org.omnifaces.facesviews.FacesViewsForwardingFilter} and
 * {@link org.omnifaces.facesviews.FacesViewsViewHandler} manually:
 * <br/>
 *
 * <p>
 * <b>web.xml</b>
 * <pre>
 * &lt;filter>
 *     &lt;filter-name>FacesViewsForwardingFilter&lt;/filter-name>
 *     &lt;filter-class>org.omnifaces.facesviews.FacesViewsForwardingFilter&lt;/filter-class>
 * &lt;/filter>
 * &lt;filter-mapping>
 *     &lt;filter-name>FacesViewsForwardingFilter&lt;/filter-name>
 *     &lt;url-pattern>/*&lt;/url-pattern>
 * &lt;/filter-mapping>
 * </pre>
 *
 * <b>faces-config.xml</b>
 * <pre>
 * &lt;application>
 *     &lt;view-handler>org.omnifaces.facesviews.FacesViewsViewHandler&lt;/view-handler>
 * &lt;/application>
 * </pre>
 *
 * <em>(at the moment Servlet 2.5 compatibility has not been tested)</em>
 *
 * @author Arjan Tijms
 *
 */
package org.omnifaces.facesviews;