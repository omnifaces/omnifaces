##OmniFaces - to make JSF life easier


###What is OmniFaces?

Tired of reinventing `JSFUtils` or `FacesUtils` utility classes for every JSF web application and/or homebrewing custom components, taghandlers, etc to workaround or enhance some general shortcomings in JSF? OmniFaces may be what you're looking for!

OmniFaces is a **utility library** for JSF 2 that focusses on utilities that ease everyday tasks with the standard JSF API. OmniFaces is a response to frequently recurring problems encountered during ages of professional JSF development of the [JDevelopment](http://jdevelopment.nl) team and from questions being asked on [Stack Overflow](http://stackoverflow.com).

An important design goal will be to have as few dependencies as possible and to be minimally invasive. So far, it only requires Java 1.6, JSF 2.0, EL 2.1 and Servlet 2.5 APIs ([Java EE 6 details](https://github.com/omnifaces/omnifaces/wiki/Java-EE-5-and-6-compatibility)) which is already minimally available in a fairly modern servlet container serving a JSF 2 web application. As such, OmniFaces should principally integrate perfectly well with most other JSF component libraries. Even more, the [OmniFaces showcase application](http://showcase.omnifaces.org) uses PrimeFaces. Note that OmniFaces is **not** designed for portlets.

Contrary to some of the other excellent JSF 2 libraries out there (like [PrimeFaces](http://primefaces.org), or [RichFaces](http://jboss.org/richfaces)), OmniFaces does not contain much if any of the beautiful visually oriented components that those other libraries are already known and loved for. As such, OmniFaces does not and probably will never contain things like rich table or datagrid components. OmniFaces is more geared toward "utilities" that solve everyday practical problems and workarounds for small shortcomings in the JSF API. Such utilities and workarounds can be based on components, but OmniFaces does not necessarily strive to be a "component library" perse.

Besides utility classes for working with the JSF API from Java code, such as [Faces](http://showcase.omnifaces.org/utils/Faces) and [Messages](http://showcase.omnifaces.org/utils/Messages), and utility and enhanced components, such as [o:highlight](http://showcase.omnifaces.org/components/highlight) and [o:viewParam](http://showcase.omnifaces.org/components/viewParam), OmniFaces will include various general converters, validators and Facelets tag handlers. These will range from ["all-or-none" validators](http://showcase.omnifaces.org/validators/validateAllOrNone) to [converters which automatically convert Java models](http://showcase.omnifaces.org/converters/SelectItemsConverter) for usage in f:selectItem(s). There are also specialized handlers, such as a [full ajax exception handler](http://showcase.omnifaces.org/exceptionhandlers/FullAjaxExceptionHandler) which will automatically handle all ajax exceptions with the default web.xml error-page mechanisms and a [combined resource handler](http://showcase.omnifaces.org/resourcehandlers/CombinedResourceHandler) which will automatically combine all separate CSS/JS resources into a single resource. Since version 1.6, CDI specific features are available such as transparent support for injection in [@FacesConverter](http://showcase.omnifaces.org/cdi/FacesConverter) and [@FacesValidator](http://showcase.omnifaces.org/cdi/FacesValidator). For a full overview of what's all available in OmniFaces and several live examples, look at the [showcase](http://showcase.omnifaces.org).


###Installation

It is a matter of dropping the [OmniFaces 1.7 JAR file](http://repo1.maven.org/maven2/org/omnifaces/omnifaces/1.7/) in `/WEB-INF/lib`. If you like to play around with the newest of the newest, hereby accepting the risk that new components/tags/classes/methods may be moved/renamed without notice, then grab the [OmniFaces 1.8 SNAPSHOT JAR file](https://oss.sonatype.org/content/repositories/snapshots/org/omnifaces/omnifaces/1.8-SNAPSHOT/) instead.

Maven users can add OmniFaces by adding the following Maven coordinates to pom.xml:

```XML
<dependency>
    <groupId>org.omnifaces</groupId>
    <artifactId>omnifaces</artifactId>
    <version>1.7</version> <!-- Or 1.8-SNAPSHOT -->
</dependency>
```

The OmniFaces UI components/taghandlers and EL functions are available under the following XML namespaces:

```XML
xmlns:o="http://omnifaces.org/ui"
xmlns:of="http://omnifaces.org/functions"
```

###Documentation

 * [OmniFaces 1.7 API documentation](http://wiki.omnifaces.googlecode.com/hg/javadoc/1.7/index.html)
 * [OmniFaces 1.7 VDL documentation](http://wiki.omnifaces.googlecode.com/hg/vdldoc/1.7/index.html)
 * [Known issues of OmniFaces in combination with specific JSF impls, JSF libs and appservers](https://github.com/omnifaces/omnifaces/wiki/Known-issues-of-OmniFaces-in-combination-with-specific-JSF-implementations,-JSF-component-libraries-and-application-servers)

###Demo

 * [OmniFaces 1.7 showcase](http://showcase.omnifaces.org)
 * [OmniFaces SNAPSHOT showcase](http://snapshot.omnifaces.org)
 * [Showcase source project](https://github.com/omnifaces/showcase)

###Support

If you have specific programming problems or questions related to the OmniFaces library, feel free to post a question on [Stack Overflow](http://stackoverflow.com) using at least the [`jsf`](http://stackoverflow.com/questions/tagged/jsf) and [`omnifaces`](http://stackoverflow.com/questions/tagged/omnifaces) tags.

If you have found bugs or have new ideas, feel free to open a [new issue](https://github.com/omnifaces/omnifaces/issues).

For general feedback that's not either a question, bug report or feature request, or a review/rating please feel free to leave it at [Devrates](http://devrates.com/project/show/95941/Omnifaces) or [Ohloh](https://ohloh.net/p/omnifaces)

###OmniFaces in the worldwide news

 * [How to cache component rendering in JSF example](http://byteslounge.com/tutorials/how-to-cache-component-rendering-in-jsf-example)
 * [Why JSF 2.0 Hides Exceptions When Using AJAX (about FullAjaxExceptionHandlerFactory)](http://beyondjava.net/blog/jsf-2-0-hides-exceptions-ajax)
 * [Omnifaces: una librería de utilidades para JSF2](http://adictosaltrabajo.com/tutoriales/tutoriales.php?pagina=omnifacesJSF2UtilityLibrary) (Spanish)
 * [JSFCentral - Arjan Tijms and Bauke Scholtz (BalusC) Talk about OmniFaces and Building zeef.com](http://content.jsfcentral.com/c/journal_articles/view_article_content?groupId=35702&articleId=91827&version=1.7)
 * [JSF Performance Tuning (with CombinedResourceHandler)](http://blog.oio.de/2013/05/06/jsf-performance-tuning/)
 * [JSFでPDFファイルを開いたりダウンロードしたりしてみる (download PDF files in JSF)](http://kikutaro777.hatenablog.com/entry/2013/04/09/181002) (Japanese)
 * [OSChina - OmniFaces 1.4 发布，JSF2 工具库](http://oschina.net/news/38546/omnifaces-1-4) (Chinese)
 * [JAXenter - JSF-Bibliothek OmniFaces vereinfacht HTML Messages](http://jaxenter.de/news/JSF-Bibliothek-OmniFaces-vereinfacht-HTML-Messages) (German)
 * [JAXenter - Besser spät als nie: JSF-Bibliothek OmniFaces 1.4 mit überarbeiteten FacesViews](http://it-republik.de/jaxenter/news/Besser-spaet-als-nie-JSF-Bibliothek-OmniFaces-1.4-mit-ueberarbeiteten-FacesViews-066860.html) (German)
 * [JAXenter - Nie wieder "View Expired": JSF-Bibliothek OmniFaces 1.3 erschienen](http://it-republik.de/jaxenter/news/Nie-wieder-View-Expired-JSF-Bibliothek-OmniFaces-1.3-erschienen-066319.html) (German)
 * [Entwicklertagebuch - OmniFaces - Das Schweizer Taschenmescher für JSF-Entwickler](http://entwicklertagebuch.com/blog/2012/10/omnifaces-das-schweizer-taschenmesser-fur-jsf-entwickler-2/) (German)
 * [OmniFaces: librería de utilidad para JSF](http://unpocodejava.wordpress.com/2012/07/26/omnifaces-libreria-de-utilidad-para-jsf) (Spanish)
 * [InfoQ - OmniFaces: uma biblioteca de utilitários para JSF](http://www.infoq.com/br/news/2012/09/jsf-omnifaces) (the Brazilian-Portuguese translation of previous English article)
 * [InfoQ - OmniFaces: A Utility Library for Java Server Faces](http://www.infoq.com/news/2012/07/omnifaces-1)
