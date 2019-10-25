package dev.xframe.http.service.config;

import dev.xframe.http.service.Request;
import dev.xframe.http.service.Response;

public interface ErrorHandler {
    
    public Response handle(Request req, Throwable e);
    
}
