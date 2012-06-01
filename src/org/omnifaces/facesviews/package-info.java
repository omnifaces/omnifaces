/**
 * <h2>This package contains the classes for the OmniFaces' FacesViews feature.</h2>
 * <p>
 * FacesViews is a feature where a special dedicated directory (<code>/WEB-INF/faces-views</code>) can be used
 * to store Facelets source files. All files found in this directory are automatically mapped as Facelets files
 * and made available using both their original extension as well as without an extension (extensionless).
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
 * <b>Example:</b><br>
 *
 * Consider the following file structure:
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
 * JSF links in which logical view ids are used will be rendered as either extensionless or with an extension based on whether
 * the request in which this rendering is done was extensionless or not. E.g. consider the following link on
 * <code>/WEB-INF/faces-views/index.xhtml</code>:
 * <pre>
 * &lt;h:link value="Add user" outcome="/users/add" /&gt;
 * </pre>
 * This will render as <code>/users/add</code> if the request was to <code>/index</code> and as <code>/users/add.xhtml</code> if the
 * request was to <code>/index.xhtml</code>.
 *
 * <p>
 * <h3>Configuration</h3>
 * The following context parameters are available.
 * <table>
 * <tr>
 * <td nowrap><code>{@value org.omnifaces.facesviews.FacesViewsInitializerListener#FACES_VIEWS_ENABLED_PARAM_NAME}</code></td>
 * <td>Used to completely switch scanning off. Allowed values: {<code>true</code>,<code>false</code>} Default: <code>true</code>
 * (note that if no <code>/WEB-INF/faces-views</code> directory is present, no scanning will be done either)</td>
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