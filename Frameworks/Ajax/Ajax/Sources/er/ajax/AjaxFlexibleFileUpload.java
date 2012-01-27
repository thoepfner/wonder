package er.ajax;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.ajax.AjaxFileUpload;
import er.ajax.AjaxUploadProgress;
import er.ajax.AjaxUtils;
import er.extensions.appserver.ERXRequest;
import er.extensions.appserver.ERXWOContext;
import er.extensions.components.ERXComponentUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * AjaxFlexibleFileUpload is an enhanced file upload component that uses a call to a hidden iFrame to handle a file
 * upload. It is based on the code in AjaxFileUpload but extends it by using Andrew Valums' ajaxupload.js (from 
 * http://valums.com/ajax-upload/). This dynamically creates the iFrame at the end of the current page content freeing
 * this component to be used in or out of a form. The component is fully styleable (including the upload button) and
 * supports upload progress and canceling.
 * 
 * @binding cancelLabel the label for for the cancel button (defaults to "Cancel")
 * @binding startingText the text to display when the progress is starting (defaults "Upload Starting...");
 * @binding selectFileLabel the label for the select file button (defaults to "Select File...")
 * @binding clearLabel the label for the button used to clear a selected file or uploaded file (defaults to "Clear")
 * @binding uploadLabel the label for the Upload button (defaults to "Upload")
 * @binding startedFunction the javascript function to execute when the progress is started
 * @binding canceledFunction the javascript function to execute when the upload is canceled
 * @binding succeededFunction the javascript function to execute when the upload succeeds
 * @binding clearedFunction the javascript function to execute when the clear button is clicked
 * @binding failedFunction the javascript function to execute when the upload fails
 * @binding finishedFunction the javascript function to execute when the upload finishes (succeeded, failed, or
 *          canceled)
 * @binding finishedAction the action to fire when the upload finishes (cancel, failed, or succeeded)
 * @binding canceledAction the action to fire when the upload is canceled
 * @binding succeededAction the action to fire when the upload succeeded
 * @binding clearedAction the action to fire when the clear button is clicked
 * @binding failedAction the action to fire when the upload fails
 * @binding data the NSData that will be bound with the contents of the upload
 * @binding inputStream will be bound to an input stream on the contents of the upload
 * @binding outputStream the output stream to write the contents of the upload to
 * @binding streamToFilePath the path to write the upload to, can be a directory
 * @binding finalFilePath the final file path of the upload (when streamToFilePath is set or keepTempFile = true)
 * @binding filePath the name of the uploaded file
 * @binding allowCancel if true, the cancel link is visible 
 * @binding refreshTime the number of milliseconds to wait between refreshes (defaults to 2000)
 * @binding keepTempFile if true, don't delete the temp file that AjaxFileUpload creates
 * @binding uploadFunctionName the upload button will instead be a function with the given name
 * @binding autoSubmit should the upload start immediately after a file is selected (defaults to true)
 * @binding injectDefaultCSS inject the default stylesheet from the Ajax framework (defaults to true);
 * @binding selectFileButtonClass class for the select file button (defaults to "Button ObjButton SelectFileObjButton");
 * @binding uploadButtonClass class for the select file button (defaults to "Button ObjButton UploadFileObjButton")
 * @binding cancelButtonClass class for the select file button (defaults to "Button ObjButton CancelUploadObjButton")
 * @binding clearButtonClass class for the select file button (defaults to "Button ObjButton ClearUploadObjButton")
 * @binding clearUploadProgressOnSuccess if true, displays the select file button instead of the uploaded file name on completion of a successful upload
 * @binding mimeType set from the content-type of the upload header if available
 * 
 * @author dleber
 * @author mschrag
 */
public class AjaxFlexibleFileUpload extends AjaxFileUpload {
	
	protected final Logger log = Logger.getLogger(getClass());
	
	public static interface Keys {
		public static final String name = "name";
		public static final String wosid = "wosid";
		public static final String selectFileLabel = "selectFileLabel";
		public static final String cancelLabel = "cancelLabel";
		public static final String clearLabel = "clearLabel";
		public static final String uploadLabel = "uploadLabel";
		public static final String cancelingText = "cancelingText";
		public static final String startingText = "startingText";
		public static final String failedText = "failedText";
		public static final String refreshTime = "refreshTime";
		public static final String autoSubmit = "autoSubmit";
		public static final String allowCancel = "allowCancel";
		public static final String startedFunction = "startedFunction";
		public static final String canceledFunction = "canceledFunction";
		public static final String finishedFunction = "finishedFunction";
		public static final String failedFunction = "failedFunction";
		public static final String succeededFunction = "succeededFunction";
		public static final String clearedFunction = "clearedFunction";
		public static final String selectFileButtonClass = "selectFileButtonClass";
		public static final String uploadButtonClass = "uploadButtonClass";
		public static final String cancelButtonClass = "cancelButtonClass";
		public static final String clearButtonClass = "clearButtonClass";
		public static final String injectDefaultCSS = "injectDefaultCSS";
		public static final String clearUploadProgressOnSuccess = "clearUploadProgressOnSuccess";
	}
	
	private String _refreshTime;
	private String _clearLabel;
	private String _cancelLabel;
	private String _uploadLabel;
	private String _selectFileLabel;
	private String _selectFileButtonClass;
	private String _uploadButtonClass;
	private String _cancelButtonClass;
	private String _clearButtonClass;

	private Boolean _autoSubmit;
	private Boolean _allowCancel;
	private Boolean _clearUploadProgressOnSuccess;

	public boolean testFlag = false;
	public enum UploadState { DORMANT, STARTED, INPROGRESS, CANCELED, FAILED, SUCCEEDED, FINISHED }
	public UploadState state = UploadState.DORMANT;
	
    public AjaxFlexibleFileUpload(WOContext context) {
        super(context);
    }
	
    @Override
    public void appendToResponse(WOResponse response, WOContext context) {
    	super.appendToResponse(response, context);
    	if (ERXComponentUtilities.booleanValueForBinding(this, Keys.injectDefaultCSS, true)) {
    		AjaxUtils.addStylesheetResourceInHead(context, response, "default_ajaxupload.css");
    	}
    	AjaxUtils.addScriptResourceInHead(context, response, "prototype.js");
    	AjaxUtils.addScriptResourceInHead(context, response, "effects.js");
    	AjaxUtils.addScriptResourceInHead(context, response, "wonder.js");
    	AjaxUtils.addScriptResourceInHead(context, response, "ajaxupload.js");
    }
    
    // AJAX UPLOAD INIT
    
    /**
     * Generates the script to initialize a new AjaxUpload JS object
     * 
     * @return script to initialize a new AjaxUpload JS object
     */
	public String ajaxUploadScript() {
		String result = "AUP.add('" + id() + "', " + ajaxProxyName() +", {" + ajaxUploadLabels() + "}, {" + options() + "}, {" + ajaxUploadOptions() + "});";
		if (log.isDebugEnabled()) log.debug("AFU Create Script: " + result);
		return result;
	}
	
	/**
	 * Builds the array of required additional AjaxUpload data items (wosid, id).
	 * 
	 * @return array of required additional AjaxUpload data items (wosid, id).
	 */
	protected NSArray<String> _ajaxUploadData() {
		NSMutableArray<String> _data = new NSMutableArray<String>("wosid:'" + this.session().sessionID() + "'");
		
		_data.addObject("id:'" + id() + "'");
		
		return _data.immutableClone();
	}
	
	/**
	 * Returns a comma separated string of AjaxUpload data items.
	 * 
	 * @return comma separated string of AjaxUpload data items.
	 */
	public String ajaxUploadData() {
		return _ajaxUploadData().componentsJoinedByString(", ");
	}
	
	/**
	 * Builds the array of AjaxUpload options
	 * 
	 * @return array of AjaxUpload options
	 */
    protected NSArray<String> _ajaxUploadOptions() {
    	NSMutableArray<String> _options = new NSMutableArray<String>("action:'" + uploadUrl() + "'");
    	
    	// add options
    	_options.addObject("data:{" + ajaxUploadData() + "}");
    	_options.addObject("name:'" + uploadName() + "'");
    	_options.add("iframeId:'"+ iframeId() +"'");
    	if ( !autoSubmit().booleanValue() ) {
    		_options.add("onChange:" + onChangeFunction());
    		_options.add("autoSubmit:false");
    	}
    	_options.add("onSubmit:" + onSubmitFunction());
    	return _options.immutableClone();
    }
    
    /**
     * Returns a comma separated string of AjaxUpload options.
     * 
     * @return comma separated string of AjaxUpload options.
     */
    public String ajaxUploadOptions() {
    	return _ajaxUploadOptions().componentsJoinedByString(",");
    }
    
    /**
     * Builds an array of localized label strings
     * 
     * @return array of label/text strings
     */
    protected NSArray<String> _ajaxUploadLabels() {
    	NSMutableArray<String> _labels = new NSMutableArray<String>();
    	_labels.addObject(String.format("upload_canceling:'%s'", cancelingText()));
    	_labels.addObject(String.format("upload_starting:'%s'", startingText()));
    	_labels.addObject(String.format("upload_failed:'%s'", localizedStringForBinding(Keys.failedText, "Upload Failed")));
    	return _labels;
    }
    
    /**
     * Returns a comma separated string of the localized label strings.
     * 
     * @return comma separated string of labels/text strings
     */
    public String ajaxUploadLabels() {
    	return _ajaxUploadLabels().componentsJoinedByString(",");
    }
    
    /**
     * Builds an array of AFU options
     * @return array of AFU options
     */
    protected NSArray<String> _options() {
    	NSMutableArray<String> _options = new NSMutableArray<String>(String.format("refreshtime:%s", refreshTime()));
    	_options.addObject("autosubmit:" + autoSubmit());
    	_options.addObject("allowcancel:" + valueForBinding(Keys.allowCancel));
      _options.add("clearUploadProgressOnSuccess:" + clearUploadProgressOnSuccess());

    	String startedFunction = (String)this.valueForBinding(Keys.startedFunction);
    	if (startedFunction != null) _options.addObject(String.format("startedFunction:%s", startedFunction));
    	
    	String finishedFunction = (String)this.valueForBinding(Keys.finishedFunction);
    	if (finishedFunction != null) _options.addObject(String.format("finishedFunction:%s", finishedFunction));
    	
    	String failedFunction = (String)this.valueForBinding(Keys.failedFunction);
    	if (failedFunction != null) _options.addObject(String.format("failedFunction:%s", failedFunction));
    	
    	String canceledFunction = (String)this.valueForBinding(Keys.canceledFunction);
    	if (canceledFunction != null) _options.addObject(String.format("canceledFunction:%s", canceledFunction));
    	
    	String succeededFunction = (String)this.valueForBinding(Keys.succeededFunction);
    	if (succeededFunction != null) _options.addObject(String.format("succeededFunction:%s", succeededFunction));
    	
      String clearedFunction = (String)this.valueForBinding(Keys.clearedFunction);
      if (clearedFunction != null) _options.addObject(String.format("clearedFunction:%s", clearedFunction));
      
    	return _options;
    }
    
    /**
     * Return a comma separated string of the AFU options
     * @return comma separated string of options
     */
    public String options() {
    	return _options().componentsJoinedByString(",");
    }
	
	// INLINE JS FUNCTIONS
		
	/**
	 * JS Function called when the AjaxUpload registers a change
	 * 
	 * @return string JS Function called when the AjaxUpload registers a change
	 */
	public String onChangeFunction() {
		String result = "function(file, extension) { AUP.prepare('"+ id() +"', file, extension); }";
		return result;
	}
	
	/**
	 * JS Function called when the AjaxUploader submits
	 * 
	 * @return string JS Function called when the AjaxUploader submits
	 */
	public String onSubmitFunction() {
		String result = "function(){ AUP.start('" + id() + "'); }";
		return result;
	}
	
	/**
	 * Generate a dictionary containing the current state of the upload.
	 * 
	 * @return a dictionary containing the current state of the upload.
	 */
	public NSDictionary<String, ?> uploadState() {
		NSMutableDictionary<String, ?> stateObj = new NSMutableDictionary<String, String>();
		AjaxUploadProgress progress = this.uploadProgress();
		if (progress != null) {
			stateObj.takeValueForKey(this.progressAmount(), "progress");
			stateObj.takeValueForKey(progress.fileName(), "filename");
		}
		this.refreshState();
		stateObj.takeValueForKey(new Integer(state.ordinal()), "state");
		if (state == UploadState.CANCELED) {
			stateObj.takeValueForKey(cancelUrl(), "cancelUrl");
		}
		if (log.isDebugEnabled()) log.debug("AjaxFlexibleFileUpload2.uploadState: " + stateObj);
		return stateObj.immutableClone();
	}
	
	/**
	 * Refresh the current state, call the finished handlers if we are succeeded, failed, or canceled
	 */
	private void refreshState() {
		state = UploadState.DORMANT;
		AjaxUploadProgress progress = this.uploadProgress();
		if (progress != null) {
			if (progress.isStarted()) {
				state = UploadState.INPROGRESS;
				if (progress.isDone()) {
					state = UploadState.FINISHED;
					if (progress.isSucceeded()) {
						state = UploadState.SUCCEEDED;
						uploadSucceeded();
					}
					if (progress.isFailed()) {
						state = UploadState.FAILED;
						uploadFailed();
					}
					if (progress.isCanceled()) {
						state = UploadState.CANCELED;
						uploadCanceled();
					}
				} 
			} else {
				state = UploadState.STARTED;
			}
		}
		if (log.isDebugEnabled()) log.debug("AjaxFlexibleFileUpload.refreshState: " + state);
	}
	
	/**
	 * JS function bound to the cancel button
	 * 
	 * @return JS function bound to the cancel button
	 */
	public String cancelUploadFunction() {
		return String.format("AUP.cancel('%s');", id());
	}
	
	/**
	 * JS function bound to the manual upload button
	 * 
	 * @return JS function bound to the manul upload button
	 */
	public String manualSubmitUploadFunction() {
		return String.format("AUP.submit('%s');", id());
	}
	
	/**
	 * JS function bound to the clear button
	 * 
	 * @return JS function bound ot the clear button
	 */
	public String clearUploadFunction() {
		return String.format("AUP.clear('%s');", id());
	}
	
	// AJAX IDS
	
	/**
	 * Element id for the cancel button
	 * 
	 * @return id for the cancel button
	 */
	public String cancelButtonId() {
		return String.format("AFUCancelButton%s", id());
	}
	
	/**
	 * Element id for the clear button
	 * 
	 * @return id for the clear button
	 */
	public String clearUploadButtonId() {
		return String.format("AFUClearButton%s", id());
	}
	
	/**
	 * Element id for the manual upload submit button
	 * 
	 * @return id for the upload button
	 */
	public String submitUploadButtonId() {
		return String.format("AFUSubmitUploadButton%s", id());
	}
	
	/**
	 * Element id for the select file button
	 * 
	 * @return id for the select file button
	 */
	public String selectFileButtonId() {
		return String.format("AFUSelectFileButton%s", id());
	}
	
	/**
	 * Element id for the select file button wrapper div
	 * 
	 * @return id for the select file button wrapper div
	 */
	public String selectFileButtonWrapperId() {
		return String.format("AFUSelectFileButtonWrapper%s", id());
	}
	
	/**
	 * Element id for the file object wrapper div
	 * 
	 * @return id for the file object wrapper div
	 */
	public String fileObjectId() {
		return String.format("AFUFileObject%s", id());
	}
	
	/**
	 * Element id for the progress bar wrapper div
	 * 
	 * @return id for the progress bar wrapper div
	 */
	public String progressWrapperId() {
		return String.format("AFUProgressBarWrapper%s", id());
	}
	
	/**
	 * Element id for the progress bar value inner div
	 * 
	 * @return id for the progress bar value inner div
	 */
	public String progressBarValueId() {
		return String.format("AFUProgressBarValue%s", id());
	}
	
	/**
	 * Unique identifier for the ajax proxy object for this upload component
	 * 
	 * @return identifier for the ajax proxy object for this upload component
	 */
	public String ajaxProxyName() {
		return "jsonrpc" + id();
	}
	
	/**
	 * Unique identifier for the inner update container
	 * 
	 * @return identifier for the inner update container
	 */
	public String innerUpdateContainerId() {
		return "AFUIC" + id();
	}
	
	/**
	 * Unique identifier for the outer update container 
	 * 
	 * @return identifier for the outer update container 
	 */
	public String outerUpdateContainerId() {
		return "AFUOC" + id();
	}
	
	/**
	 * Unique identifier for the select files button
	 * 
	 * @return identifier for the select files button
	 */
	public String uploadButtonId() {
		return "AFUUB" + id();
	}
	
	/**
	 * Unique identifier for the iframe generated by the AjaxUploader.js
	 * 
	 * @return identifier for the iframe generated by the AjaxUploader.js
	 */
	public String iframeId() {
		return "AFUIF" + id();
	}
	
	/**
	 * Unique identifier for the fileName container
	 * 
	 * @return identifier for the fileName container
	 */
	public String fileNameId() {
		return "AFUFileNameWrapper" + id();
	}
	
	/**
	 * Unique identifier for the upload name
	 * 
	 * @return identifier for the upload name
	 */
	public String uploadName() {
		return "AFUUN" + id();
	}
	
	// IFRAME URLS
	
	/**
	 * Returns a closeHTTPSession DA action URL passed to the iframe to cancel the client-side upload
	 * 
	 * @return url sent to the iframe to cancel
	 */
	public String cancelUrl() {
		NSDictionary<String,Boolean> queryParams = new NSDictionary<String,Boolean>(Boolean.FALSE, Keys.wosid);
		String url = ERXWOContext._directActionURL(context(), "ERXDirectAction/closeHTTPSession", queryParams, ERXRequest.isRequestSecure(context().request()));
		if (log.isDebugEnabled()) log.debug("URL: " + url);
		return url;
	}
	
	// ACTIONS
	
	/**
	 * Action called by the cancel upload button
	 */
	public void cancelUpload() {
		if (this.uploadProgress() != null) {
			this.uploadProgress().cancel();
		}
		state = UploadState.CANCELED;
	}
	
	/**
	 * Action called by the clear button, resets the uploader for a new file selection
	 * 
	 * @return results of action
	 */
	public WOActionResults clearFileResults() {
		clearUploadProgress();
		WOActionResults results = (WOActionResults) valueForBinding("clearedAction");
		return results;
	}
	
	/**
	 * Hook for add-in action called when an upload is canceled
	 * 
	 * @return results of action
	 */
	@Override
	public WOActionResults uploadCanceled() {
		clearUploadProgress();
		return super.uploadCanceled();
	}
	
	/**
	 * Hook for add-in action called when an upload fails
	 * 
	 * @return results of action
	 */
	@Override
	public WOActionResults uploadFailed() {
		clearUploadProgress();
		return super.uploadFailed();
	}
	
	/**
	 * Hook for add-in action called when an upload succeeds.
	 */
	public WOActionResults uploadSucceeded() {
		WOActionResults result = super.uploadSucceeded();
		clearUploadProgress();
		return result;
	}
	
	/**
	 * Helper to reset the uploader and unregister the AjaxProgress object
	 */
	public void clearUploadProgress() {
		if (_progress != null) {
			AjaxUploadProgress.unregisterProgress(session(), _progress);
		}
		_progress = null;
	}
	
	// ACCESSORS
	
	/**
	 * Returns the AjaxUploadProgress for this uploader
	 */
	@Override
	public AjaxUploadProgress uploadProgress() {
		if (_progress == null) {
			_progress = (AjaxUploadProgress)AjaxUploadProgress.progress(session(), id());
		}
		return _progress;
	}


	/**
	 * Boolean which determines whether the upload should occur automatically after a file is selected.
	 * 
	 * @return value for 'autoSubmit' binding
	 */
  public Boolean autoSubmit() {
    if (_autoSubmit == null) {
      _autoSubmit = ERXValueUtilities.BooleanValueWithDefault(valueForBinding(Keys.autoSubmit), Boolean.TRUE);
    }
    return _autoSubmit;
  }
  
  public Boolean clearUploadProgressOnSuccess() {
    if (_clearUploadProgressOnSuccess == null) {
      _clearUploadProgressOnSuccess = ERXValueUtilities.BooleanValueWithDefault(valueForBinding(Keys.clearUploadProgressOnSuccess), Boolean.FALSE);
      System.out.println("clearUploadProgressOnSuccess: " + _clearUploadProgressOnSuccess);
    }
    return _clearUploadProgressOnSuccess;
  }
  
	public Boolean allowCancel() {
		if (_allowCancel == null) {
			_allowCancel = ERXValueUtilities.BooleanValueWithDefault(valueForBinding(Keys.allowCancel), Boolean.FALSE);
		}
		return _allowCancel;
	}
	
	/**
	 * Calculate the current progress amount ( 0-100 )
	 * 
	 * @return current progress amount ( 0-100 )
	 */
	public Integer progressAmount() {
		Integer amount = null;
		AjaxUploadProgress progress = this.uploadProgress();
		if (progress != null) {
			if (!progress.isSucceeded()) {
				int percent = (int)(progress.percentage() * 100);
				amount = new Integer(percent);
			} else {
				amount = new Integer(100);
			}
		}
		return amount;
	}

	/**
	 * Returns the value for the binding 'refreshTime'
	 * 
	 * The binding takes milliseconds between refreshes, this returns seconds
	 * 
	 * @return value of the 'refreshTime' binding converted to seconds
	 */
	public String refreshTime() {
		if (_refreshTime == null) {
			double tempValue = ERXValueUtilities.intValueWithDefault(valueForBinding(Keys.refreshTime), 1000);
			_refreshTime = String.valueOf(tempValue);
		}
		return _refreshTime;
	}

	/**
	 * Utility to localize labels with current localizer
	 * @param value
	 * @return localized value
	 */
	private String localizedString(String value) {
		return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(value);
	}
	
	/**
	 * Utility to return string value for binding or default value if no value is found
	 * only really needed to support < WO5.4
	 * @param key
	 * @param defaultValue
	 * @return value for binding or defaultValue
	 */
	private String stringValueForBinding(String key, String defaultValue) {
		String result = (String) valueForBinding(key);
		if (result == null) {
			result = defaultValue;
		}
		return result;
	}
	
	/**
	 * Utility to return localized value from stringValueForBinding
	 * @param key
	 * @param defaultValue
	 * @return localized value of binding key or defaultValue
	 */
	private String localizedStringForBinding(String key, String defaultValue) {
		return localizedString(stringValueForBinding(key, defaultValue));
	}
	
	/**
	 * Label for the upload button
	 * 
	 * @return string value for 'uploadLabel' binding
	 */
	public String uploadLabel() {
		if (_uploadLabel == null) {
			_uploadLabel = localizedStringForBinding(Keys.uploadLabel, "Upload");
		}
		return _uploadLabel;
	}
	
	/**
	 * Label for the clear button
	 * 
	 * @return string value for 'clearLabel' binding
	 */
	public String clearLabel() {
		if (_clearLabel == null) {
			_clearLabel = localizedStringForBinding(Keys.clearLabel, "Clear");
		}
		return _clearLabel;
	}

	/**
	 * Label for the cancel button
	 * 
	 * @return string value for 'cancelLabel' binding
	 */
	public String cancelLabel() {
		if (_cancelLabel == null) {
			_cancelLabel = localizedStringForBinding(Keys.cancelLabel, "Cancel");
		}
		return _cancelLabel;
	}

	/**
	 * Label for the select file button
	 * 
	 * @return string value for 'selectFileLabel' binding
	 */
	public String selectFileLabel() {
		if (_selectFileLabel == null) {
			_selectFileLabel = localizedStringForBinding(Keys.selectFileLabel, "Select File...");
		}
		return _selectFileLabel;
	}
	
	/**
	 * CSS Class for the select file button
	 * 
	 * @return string value for 'selectFileButtonClass' binding
	 */
	public String selectFileButtonClass() {
		if (_selectFileButtonClass == null) {
			_selectFileButtonClass = stringValueForBinding(Keys.selectFileButtonClass, "Button ObjButton SelectFileObjButton");
		}
		return _selectFileButtonClass;
	}
	
	/**
	 * CSS Class for the upload file button
	 * 
	 * @return string value for 'uploadButtonClass' binding
	 */
	public String uploadButtonClass() {
		if (_uploadButtonClass == null) {
			_uploadButtonClass = stringValueForBinding(Keys.uploadButtonClass, "Button ObjButton UploadFileObjButton");
		}
		return _uploadButtonClass;
	}
	
	/**
	 * CSS Class for the cancel upload button
	 * 
	 * @return string value for 'cancelButtonClass' binding
	 */
	public String cancelButtonClass() {
		if (_cancelButtonClass == null) {
			_cancelButtonClass = stringValueForBinding(Keys.cancelButtonClass, "Button ObjButton CancelUploadObjButton");
		}
		return _cancelButtonClass;
	}
	
	/**
	 * CSS Class for the clear upload button
	 * 
	 * @return string value for 'clearButtonClass' binding
	 */
	public String clearButtonClass() {
		if (_clearButtonClass == null) {
			_clearButtonClass = stringValueForBinding(Keys.clearButtonClass, "Button ObjButton ClearUploadObjButton");
		}
		return _clearButtonClass;
	}
}