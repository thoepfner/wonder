package wowodc.background.rest.controllers;

import java.util.concurrent.Callable;

import wowodc.background.tasks.T10RestEOFTask;
import wowodc.background.utilities.Utilities;
import wowodc.eof.TaskInfo;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.appserver.ERXHttpStatusCodes;
import er.extensions.concurrency.ERXExecutorService;
import er.extensions.concurrency.ERXFutureTask;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXKeyFilter;
import er.rest.routes.ERXRouteController;
import er.rest.routes.ERXRouteResults;
import er.rest.routes.ERXRouteUrlUtils;

public class TaskInfoController extends ERXRouteController {

  public TaskInfoController(WORequest request) {
    super(request);
  }

  public WOActionResults createAction() throws Throwable {
    TaskInfo taskInfo = ERXEOControlUtilities.createAndInsertObject(editingContext(), TaskInfo.class);
    taskInfo.setDuration(new Long(15000));
    taskInfo.setStartNumber(Utilities.newStartNumber());
    taskInfo.setStartTime(new NSTimestamp());

    editingContext().saveChanges();
    
    T10RestEOFTask taskInfoGID = new T10RestEOFTask();
    taskInfoGID.setTaskGID(taskInfo.__globalID());

    ERXFutureTask<?> _future = new ERXFutureTask(taskInfoGID);
    ERXExecutorService.executorService().execute(_future);

    ERXRouteResults results = (ERXRouteResults)response(taskInfo, ERXKeyFilter.filterWithAttributesAndToOneRelationships());    
    WOResponse response = results.generateResponse();
    String location = hostName() + ERXRouteUrlUtils.actionUrlForRecord(_context, taskInfo, "show", this.format ().name(), new NSDictionary(), this.request().isSecure(), this.request().isSessionIDInRequest());
    response.setHeader(location, "Content-Location");
    response.setStatus(ERXHttpStatusCodes.ACCEPTED);
    return response;  
  }
  
  public WOActionResults showAction() throws Throwable {
    TaskInfo taskInfo = routeObjectForKey("taskInfo");
    if (TaskInfo.WORKFLOW_PRIME_CHECKED.equals(taskInfo.workflowState())) {
      ERXRouteResults results = (ERXRouteResults)response(taskInfo, ERXKeyFilter.filterWithNone());    
      WOResponse response = results.generateResponse();
      String location = hostName() + ERXRouteUrlUtils.actionUrlForRecord(_context, taskInfo, "results", this.format ().name(), new NSDictionary(), this.request().isSecure(), this.request().isSessionIDInRequest());
      response.setHeader(location, "Content-Location");
      response.setStatus(ERXHttpStatusCodes.SEE_OTHER);
      return response;
    }
    return response(taskInfo.workflowState(), ERXKeyFilter.filterWithAttributes());
  }
  
  public WOActionResults resultsAction() throws Throwable {
    TaskInfo taskInfo = routeObjectForKey("taskInfo");
    return response(taskInfo, ERXKeyFilter.filterWithAttributesAndToOneRelationships());
  }
  
  protected String hostName() {
    String host = this.request()._serverName(); 
    if (this.request().isSecure()) {
      host = "https://" + host; 
    } else {
      host = "http://" + host; 
      if (this.request()._serverPort() != null) {
        host = host + ":" + this.request()._serverPort();
        return host;
      }
    }
    return host;
  }
  
  
}
