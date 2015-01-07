package org.omnifaces.taghandler;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

public class TagParameter extends TagHandler {
	
	private final TagAttribute name;
	
	public TagParameter(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name");
	}
	
	@Override
	public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
		
		checkAndMarkMapper(ctx);
		
		String nameStr = name.getValue(ctx);
		
		VariableMapper variableMapper = ctx.getVariableMapper();
				
		ValueExpression valueExpressionLocal = variableMapper.setVariable(nameStr, null);
		if (valueExpressionLocal == null) {
			ValueExpression valueExpressionParent = variableMapper.resolveVariable(nameStr);
			if (valueExpressionParent != null) {
				valueExpressionLocal = ctx.getExpressionFactory().createValueExpression(null, Object.class);
			}
		}
		
		variableMapper.setVariable(nameStr, valueExpressionLocal);
	}
	
	public void checkAndMarkMapper(FaceletContext ctx) {
		Integer marker = (Integer) ctx.getAttribute("OmniFaces-marker");
		if (marker != null && marker.equals(ctx.hashCode())) {
			// Marked and our own
			return;
		}
		
		VariableMapper variableMapper = ctx.getVariableMapper();
		
		// Our current parent marker
		ValueExpression valueExpressionParentMarker = variableMapper.resolveVariable("OmniFaces-marker");
		
		// Remove the variable locally
		// If we have our own mapper, this will not affect our parent mapper
		variableMapper.setVariable("OmniFaces-marker", null);
		
		// Obtain parent again
		ValueExpression valueExpressionParentMarkerCheck = variableMapper.resolveVariable("OmniFaces-marker");
		
		if (valueExpressionParentMarkerCheck == null || !valueExpressionParentMarkerCheck.equals(valueExpressionParentMarker)) {
			// We were able to remove our parent's mapper, so we share it
		}
	}
	
	

}
