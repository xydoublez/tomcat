/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.core;


import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityUtil;
import org.apache.tomcat.util.ExceptionUtils;


/**
 * Facade object which masks the internal <code>ApplicationContext</code>
 * object from the web application.
 *
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 * @version $Id$
 */

public final class ApplicationContextFacade
    implements ServletContext {
        
    // ---------------------------------------------------------- Attributes
    /**
     * Cache Class object used for reflection.
     */
    private HashMap<String,Class<?>[]> classCache;
    
    
    /**
     * Cache method object.
     */
    private HashMap<String,Method> objectCache;
    
    
    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new instance of this class, associated with the specified
     * Context instance.
     *
     * @param context The associated Context instance
     */
    public ApplicationContextFacade(ApplicationContext context) {
        super();
        this.context = context;
        
        classCache = new HashMap<String,Class<?>[]>();
        objectCache = new HashMap<String,Method>();
        initClassCache();
    }
    
    
    private void initClassCache(){
        Class<?>[] clazz = new Class[]{String.class};
        classCache.put("getContext", clazz);
        classCache.put("getMimeType", clazz);
        classCache.put("getResourcePaths", clazz);
        classCache.put("getResource", clazz);
        classCache.put("getResourceAsStream", clazz);
        classCache.put("getRequestDispatcher", clazz);
        classCache.put("getNamedDispatcher", clazz);
        classCache.put("getServlet", clazz);
        classCache.put("setInitParameter", new Class[]{String.class, String.class});
        classCache.put("createServlet", new Class[]{Class.class});
        classCache.put("addServlet", new Class[]{String.class, String.class});
        classCache.put("createFilter", new Class[]{Class.class});
        classCache.put("addFilter", new Class[]{String.class, String.class});
        classCache.put("createListener", new Class[]{Class.class});
        classCache.put("addListener", clazz);
        classCache.put("getFilterRegistration", clazz);
        classCache.put("getServletRegistration", clazz);
        classCache.put("getInitParameter", clazz);
        classCache.put("setAttribute", new Class[]{String.class, Object.class});
        classCache.put("removeAttribute", clazz);
        classCache.put("getRealPath", clazz);
        classCache.put("getAttribute", clazz);
        classCache.put("log", clazz);
        classCache.put("setSessionTrackingModes", new Class[]{EnumSet.class} );
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Wrapped application context.
     */
    private ApplicationContext context = null;


    // ------------------------------------------------- ServletContext Methods


    public ServletContext getContext(String uripath) {
        ServletContext theContext = null;
        if (SecurityUtil.isPackageProtectionEnabled()) {
            theContext = (ServletContext)
                doPrivileged("getContext", new Object[]{uripath});
        } else {
            theContext = context.getContext(uripath);
        }
        if ((theContext != null) &&
            (theContext instanceof ApplicationContext)){
            theContext = ((ApplicationContext)theContext).getFacade();
        }
        return (theContext);
    }


    public int getMajorVersion() {
        return context.getMajorVersion();
    }


    public int getMinorVersion() {
        return context.getMinorVersion();
    }


    public String getMimeType(String file) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String)doPrivileged("getMimeType", new Object[]{file});
        } else {
            return context.getMimeType(file);
        }
    }

    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<String> getResourcePaths(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()){
            return (Set<String>)doPrivileged("getResourcePaths",
                    new Object[]{path});
        } else {
            return context.getResourcePaths(path);
        }
    }


    public URL getResource(String path)
        throws MalformedURLException {
        if (Globals.IS_SECURITY_ENABLED) {
            try {
                return (URL) invokeMethod(context, "getResource", 
                                          new Object[]{path});
            } catch(Throwable t) {
                if (t instanceof MalformedURLException){
                    throw (MalformedURLException)t;
                }
                return null;
            }
        } else {
            return context.getResource(path);
        }
    }


    public InputStream getResourceAsStream(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (InputStream) doPrivileged("getResourceAsStream", 
                                              new Object[]{path});
        } else {
            return context.getResourceAsStream(path);
        }
    }


    public RequestDispatcher getRequestDispatcher(final String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (RequestDispatcher) doPrivileged("getRequestDispatcher", 
                                                    new Object[]{path});
        } else {
            return context.getRequestDispatcher(path);
        }
    }


    public RequestDispatcher getNamedDispatcher(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (RequestDispatcher) doPrivileged("getNamedDispatcher", 
                                                    new Object[]{name});
        } else {
            return context.getNamedDispatcher(name);
        }
    }


    /**
     * @deprecated
     */
    @Deprecated
    public Servlet getServlet(String name)
        throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            try {
                return (Servlet) invokeMethod(context, "getServlet", 
                                              new Object[]{name});
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                }
                return null;
            }
        } else {
            return context.getServlet(name);
        }
    }


    /**
     * @deprecated
     */
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<Servlet>) doPrivileged("getServlets", null);
        } else {
            return context.getServlets();
        }
    }


    /**
     * @deprecated
     */
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    @Deprecated
    public Enumeration<String> getServletNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged("getServletNames", null);
        } else {
            return context.getServletNames();
        }
   }


    public void log(String msg) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Object[]{msg} );
        } else {
            context.log(msg);
        }
    }


    /**
     * @deprecated
     */
    @Deprecated
    public void log(Exception exception, String msg) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Class[]{Exception.class, String.class}, 
                         new Object[]{exception,msg});
        } else {
            context.log(exception, msg);
        }
    }


    public void log(String message, Throwable throwable) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("log", new Class[]{String.class, Throwable.class}, 
                         new Object[]{message, throwable});
        } else {
            context.log(message, throwable);
        }
    }


    public String getRealPath(String path) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getRealPath", new Object[]{path});
        } else {
            return context.getRealPath(path);
        }
    }


    public String getServerInfo() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getServerInfo", null);
        } else {
            return context.getServerInfo();
        }
    }


    public String getInitParameter(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getInitParameter", 
                                         new Object[]{name});
        } else {
            return context.getInitParameter(name);
        }
    }


    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Enumeration<String> getInitParameterNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged(
                    "getInitParameterNames", null);
        } else {
            return context.getInitParameterNames();
        }
    }


    public Object getAttribute(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return doPrivileged("getAttribute", new Object[]{name});
        } else {
            return context.getAttribute(name);
        }
     }


    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Enumeration<String> getAttributeNames() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Enumeration<String>) doPrivileged(
                    "getAttributeNames", null);
        } else {
            return context.getAttributeNames();
        }
    }


    public void setAttribute(String name, Object object) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("setAttribute", new Object[]{name,object});
        } else {
            context.setAttribute(name, object);
        }
    }


    public void removeAttribute(String name) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("removeAttribute", new Object[]{name});
        } else {
            context.removeAttribute(name);
        }
    }


    public String getServletContextName() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getServletContextName", null);
        } else {
            return context.getServletContextName();
        }
    }

       
    public String getContextPath() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (String) doPrivileged("getContextPath", null);
        } else {
            return context.getContextPath();
        }
    }

       
    public FilterRegistration.Dynamic addFilter(String filterName,
            String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                    "addFilter", new Object[]{filterName, className});
        } else {
            return context.addFilter(filterName, className);
        }
    }


    public FilterRegistration.Dynamic addFilter(String filterName,
            Filter filter) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                    "addFilter", new Class[]{String.class, Filter.class}, new Object[]{filterName, filter});
        } else {
            return context.addFilter(filterName, filter);
        }
    }


    public FilterRegistration.Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration.Dynamic) doPrivileged(
                    "addFilter", new Object[]{filterName, filterClass.getName()});
        } else {
            return context.addFilter(filterName, filterClass);
        }
    }

    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends Filter> T createFilter(Class<T> c)
    throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            try {
                return (T) invokeMethod(context, "createFilter", 
                                              new Object[]{c});
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                }
                return null;
            }
        } else {
            return context.createFilter(c);
        }
    }


    public FilterRegistration getFilterRegistration(String filterName) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (FilterRegistration) doPrivileged(
                    "getFilterRegistration", new Object[]{filterName});
        } else {
            return context.getFilterRegistration(filterName);
        }
    }
    
    
    public ServletRegistration.Dynamic addServlet(String servletName,
            String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                    "addServlet", new Object[]{servletName, className});
        } else {
            return context.addServlet(servletName, className);
        }
    }


    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet servlet) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                    "addServlet", new Class[]{String.class, Servlet.class}, new Object[]{servletName, servlet});
        } else {
            return context.addServlet(servletName, servlet);
        }
    }


    public ServletRegistration.Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration.Dynamic) doPrivileged(
                    "addServlet", new Object[]{servletName, servletClass.getName()});
        } else {
            return context.addServlet(servletName, servletClass);
        }
    }


    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends Servlet> T createServlet(Class<T> c)
    throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            try {
                return (T) invokeMethod(context, "createServlet", 
                                              new Object[]{c});
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                }
                return null;
            }
        } else {
            return context.createServlet(c);
        }
    }

    
    public ServletRegistration getServletRegistration(String servletName) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ServletRegistration) doPrivileged(
                    "getServletRegistration", new Object[]{servletName});
        } else {
            return context.getServletRegistration(servletName);
        }
    }
    
    
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (EnumSet<SessionTrackingMode>)
                doPrivileged("getDefaultSessionTrackingModes", null);
        } else {
            return context.getDefaultSessionTrackingModes();
        }
    }

    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (EnumSet<SessionTrackingMode>)
                doPrivileged("getEffectiveSessionTrackingModes", null);
        } else {
            return context.getEffectiveSessionTrackingModes();
        }
    }


    public SessionCookieConfig getSessionCookieConfig() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (SessionCookieConfig)
                doPrivileged("getSessionCookieConfig", null);
        } else {
            return context.getSessionCookieConfig();
        }
    }


    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("setSessionTrackingModes",
                    new Object[]{sessionTrackingModes});
        } else {
            context.setSessionTrackingModes(sessionTrackingModes);
        }
    }


    public boolean setInitParameter(String name, String value) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return ((Boolean) doPrivileged("setInitParameter",
                    new Object[]{name, value})).booleanValue();
        } else {
            return context.setInitParameter(name, value);
        }
    }


    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Object[]{listenerClass.getName()});
        } else {
            context.addListener(listenerClass);
        }
    }


    @Override
    public void addListener(String className) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Object[]{className});
        } else {
            context.addListener(className);
        }
    }


    @Override
    public <T extends EventListener> void addListener(T t) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            doPrivileged("addListener",
                    new Object[]{t.getClass().getName()});
        } else {
            context.addListener(t);
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public <T extends EventListener> T createListener(Class<T> c)
            throws ServletException {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            try {
                return (T) invokeMethod(context, "createListener", 
                                              new Object[]{c});
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                if (t instanceof ServletException) {
                    throw (ServletException) t;
                }
                return null;
            }
        } else {
            return context.createListener(c);
        }
    }


    @Override
    public void declareRoles(String... roleNames) {
        if (SecurityUtil.isPackageProtectionEnabled()) {
//FIXME
            doPrivileged("declareRoles",
                    new Object[]{roleNames});
        } else {
            context.declareRoles(roleNames);
        }
    }


    @Override
    public ClassLoader getClassLoader() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (ClassLoader) doPrivileged("getClassLoader", null);
        } else {
            return context.getClassLoader();
        }
    }


    @Override
    public int getEffectiveMajorVersion() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return ((Integer) doPrivileged("getEffectiveMajorVersion",
                    null)).intValue();
        } else  {
            return context.getEffectiveMajorVersion();
        }
    }


    @Override
    public int getEffectiveMinorVersion() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return ((Integer) doPrivileged("getEffectiveMinorVersion",
                    null)).intValue();
        } else  {
            return context.getEffectiveMinorVersion();
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Map<String, ? extends FilterRegistration>) doPrivileged(
                    "getFilterRegistrations", null);
        } else {
            return context.getFilterRegistrations();
        }
    }


    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (JspConfigDescriptor) doPrivileged("getJspConfigDescriptor",
                    null);
        } else {
            return context.getJspConfigDescriptor();
        }
    }


    @Override
    @SuppressWarnings("unchecked") // doPrivileged() returns the correct type
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        if (SecurityUtil.isPackageProtectionEnabled()) {
            return (Map<String, ? extends ServletRegistration>) doPrivileged(
                    "getServletRegistrations", null);
        } else {
            return context.getServletRegistrations();
        }
    }

    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     * @param methodName The method to call.
     * @param params The arguments passed to the called method.
     */
    private Object doPrivileged(final String methodName, final Object[] params) {
        try{
            return invokeMethod(context, methodName, params);
        }catch(Throwable t){
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    
    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     * @param appContext The AppliationContext object on which the method
     *                   will be invoked
     * @param methodName The method to call.
     * @param params The arguments passed to the called method.
     */
    private Object invokeMethod(ApplicationContext appContext,
                                final String methodName, 
                                Object[] params) 
        throws Throwable{

        try{
            Method method = objectCache.get(methodName);
            if (method == null){
                method = appContext.getClass()
                    .getMethod(methodName, classCache.get(methodName));
                objectCache.put(methodName, method);
            }
            
            return executeMethod(method,appContext,params);
        } catch (Exception ex){
            handleException(ex);
            return null;
        } finally {
            params = null;
        }
    }
    
    /**
     * Use reflection to invoke the requested method. Cache the method object 
     * to speed up the process
     * @param methodName The method to invoke.
     * @param clazz The class where the method is.
     * @param params The arguments passed to the called method.
     */    
    private Object doPrivileged(final String methodName, 
                                final Class<?>[] clazz,
                                Object[] params) {

        try{
            Method method = context.getClass().getMethod(methodName, clazz);
            return executeMethod(method,context,params);
        } catch (Exception ex){
            try {
                handleException(ex);
            } catch (Throwable t){
                ExceptionUtils.handleThrowable(t);
                throw new RuntimeException(t.getMessage());
            }
            return null;
        } finally {
            params = null;
        }
    }
    
    
    /**
     * Executes the method of the specified <code>ApplicationContext</code>
     * @param method The method object to be invoked.
     * @param context The AppliationContext object on which the method
     *                   will be invoked
     * @param params The arguments passed to the called method.
     */
    private Object executeMethod(final Method method, 
                                 final ApplicationContext context,
                                 final Object[] params) 
            throws PrivilegedActionException, 
                   IllegalAccessException,
                   InvocationTargetException {
                                     
        if (SecurityUtil.isPackageProtectionEnabled()){
           return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>(){
                public Object run() throws IllegalAccessException, InvocationTargetException{
                    return method.invoke(context,  params);
                }
            });
        } else {
            return method.invoke(context, params);
        }        
    }

    
    /**
     *
     * Throw the real exception.
     * @param ex The current exception
     */
    private void handleException(Exception ex)
        throws Throwable {

        Throwable realException;
        
        if (ex instanceof PrivilegedActionException) {
            ex = ((PrivilegedActionException) ex).getException();
        }
        
        if (ex instanceof InvocationTargetException) {
            realException =
                ((InvocationTargetException) ex).getTargetException();
        } else {
            realException = ex;
        }   
        
        throw realException;
    }

}
