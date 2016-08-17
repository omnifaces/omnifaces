package org.omnifaces.test.cdi;

import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getViewId;
import static org.omnifaces.util.Faces.hasContext;
import static org.omnifaces.util.Faces.setViewRoot;
import static org.omnifaces.util.Messages.addGlobalInfo;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ViewScopedITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static boolean unloaded;
	private static boolean destroyed;

	@PostConstruct
	public void init() {
		if (unloaded) {
			addGlobalInfo("unload");
			unloaded = false;
		}
		else if (destroyed) {
			addGlobalInfo("destroy");
			destroyed = false;
		}

		addGlobalInfo("init");
	}

	public void submit() {
		addGlobalInfo("submit");
	}

	public String navigate() {
		addGlobalInfo("navigate");
		return getViewId();
	}

	public void rebuild() {
		addGlobalInfo("rebuild");
		setViewRoot(getViewId());
	}

	@PreDestroy
	public void destroy() {
		if (hasContext() && isUnloadRequest(getContext())) {
			unloaded = true;
		}
		else {
			destroyed = true;
		}
	}

}