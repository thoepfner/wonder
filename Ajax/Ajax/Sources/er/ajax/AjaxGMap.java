package er.ajax;

// Generated by the WOLips Templateengine Plug-in at Dec 18, 2006 10:27:08 AM

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;

import er.extensions.ERXWOContext;
import er.extensions.ERXProperties;

/**
 * Instantiates a Google Map (an object of GMap2 javascript class) at the given location (specified by lat&lng or address) with given properties. IMPORTANT: your GMaps api key must be specified in your properties file as <code>ajax.google.maps.apiKey</code>.
 * 
 * @author eric robinson
 * @binding id the id of the div that the map is rendered into. If none is given, a unique id will be generated. This is also the name of the map javascript object, which can be accessed after the map has been instantiated.
 * @binding address The address that the map will be centered on at load. 
 * @binding lng longitude for map center (must be paired with lat, cannot coexist with address)
 * @binding lat latitude for map center (must be paired with lng, cannot coexist with address)
 * @binding width the width of the map (does not have to be specified here, but must be specified somewhere [ie. css])
 * @binding height the height of the map (does not have to be specified here, but must be specified somewhere [ie. css])
 * @control control control type name. Will automatically append a 'G' before and 'Control' after the name given, possible values here: http://www.google.com/apis/maps/documentation/reference.html#GControlImpl
 * @binding zoomLevel zoom level of map (will default to 13 is not specified). Higher is closer.
 * @property ajax.google.maps.apiKey an api key you can get from http://www.google.com/apis/maps/ . If your app runs on http://ip:port/cgi-bin/WebObjects/GoogleMaps.woa, register the key for http://ip:port/cgi-bin/WebObjects/ . Using a fixed WO port is recommended (unless you want to get a new api key everytime you restart your server). AjaxGMaps will not work without an Api Key. 
 */
public class AjaxGMap extends AjaxComponent {
	private String _id;

	public AjaxGMap(WOContext context) {
		super(context);

	}

	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	protected void addRequiredWebResources(WOResponse response) {
		String apiKey = ERXProperties.stringForKey("ajax.google.maps.apiKey");
		String mapApiJsFilename = "http://maps.google.com/maps?file=api&amp;v=2&amp;key=" + apiKey;
		addScriptResourceInHead(response, mapApiJsFilename);
		addScriptResourceInHead(response, "WonderGMapsHelpers.js");
	}

	public String id() {
		if (_id == null) {
			_id = (String) valueForBinding("id");
			if (_id == null) {
				_id = ERXWOContext.safeIdentifierName(context(), true);
			}
		}
		return _id;
	}

	public WOActionResults handleRequest(WORequest request, WOContext context) {
		return null;
	}

	public String mapContainerStyle() {
		Object width = valueForBinding("width");
		Object height = valueForBinding("height");

		if (width instanceof Integer) {
			width = width + "px";
		}

		if (height instanceof Integer) {
			height = height + "px";
		}

		if (width == null || height == null) {
			return null;
		}
		return "width: " + width + "; height: " + height;
	}
}