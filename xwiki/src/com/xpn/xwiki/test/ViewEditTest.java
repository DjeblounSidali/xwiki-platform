
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;

import junit.framework.TestCase;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 11 janv. 2004
 * Time: 12:08:01
 */

public class ViewEditTest extends ServletTestCase {

    private String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context = new XWikiContext();
    public XWiki xwiki;

    public void setUp() {
        flushCache();
    };

    public void cleanUp() {
    };

    public void clientSetUp(XWikiStoreInterface store) throws XWikiException {
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);
    }


    private void setUrl(WebRequest webRequest, String action, String docname) {
        setUrl(webRequest, action, docname, "");
    }

    private void setUrl(WebRequest webRequest, String action, String docname, String query) {
        webRequest.setURL("127.0.0.1:9080", "/xwiki" , "/testbin", "/" + action + "/Main/" + docname, query);
    }

    private void setVirtualUrl(WebRequest webRequest, String host, String appname, String action, String docname, String query) {
        webRequest.setURL(host + ":9080", "/" + appname , "/testbin", "/" + action + "/Main/" + docname, query);
    }

    public String getHibpath() {
        // Usefull in case we need to understand where we are
        String path = (new File(".")).getAbsolutePath();
        System.out.println("Current Directory is: " + path);

        File file = new File(hibpath);
        if (file.exists())
            return hibpath;

        file = new File("WEB-INF", hibpath);
        if (file.exists())
            return "./WEB-INF/" + hibpath;

        file = new File("test", hibpath);
        if (file.exists())
            return "./test/" + hibpath;

        if (config!=null)
        {
            ServletContext context = config.getServletContext();
            if (context!=null)
                return context.getRealPath("WEB-INF/" + hibpath);
        }

        return hibpath;
    }

    public void cleanSession(HttpSession session) {
        Vector names = new Vector();
        Enumeration enum = session.getAttributeNames();
        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            names.add(name);
        }

        for (int i=0;i<names.size();i++)
        {
            session.removeAttribute((String)names.get(i));
        }
    }

    public void flushCache() {
        // We need to flush the server cache before running our tests
        // because we are modifiying the database behind the scenes
        // so if we are running the tests twice we won't necessarly
        // get the same results..
        try {
            XWiki xwiki = (XWiki) config.getServletContext().getAttribute("xwikitest");
            xwiki.flushCache();
        } catch (Exception e) {
        }
    }

    public void beginViewNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        setUrl(webRequest, "view", "ViewNotOkTest");
    }

    public void endViewNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Page should have no version", result.indexOf("1.1")==-1);
    }

    public void testViewNotOk() throws IOException, Throwable, HibernateException {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "ViewOkTest", context);
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewOk() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }



     public void beginViewGetDocument(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "ViewGetDocumentContent", context);
        String content = Utils.content1;
        Utils.content1 = "test\n$xwiki.getDocument(\"Main.ViewGetDocumentContent\").getContent()\ntest\n";
        Utils.createDoc(hibstore, "Main", "ViewGetDocument", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewGetDocument");
    }

    public void endViewGetDocument(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find content in result:\n" + result, result.indexOf("Hello")!=-1);
    }

    public void testViewGetDocument() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }


    public void beginViewDocumentLink(WebRequest webRequest) throws HibernateException, XWikiException {
       XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
       StoreHibernateTest.cleanUp(hibstore, context);
       // Utils.createDoc(hibstore, "Main", "ViewDocumentTestLinkTest", context);
       String content = Utils.content1;
       Utils.content1 = "[Main.ViewDocumentLink]";
       Utils.createDoc(hibstore, "Main", "ViewDocumentLink", context);
       Utils.content1 = content;
       setUrl(webRequest, "view", "ViewDocumentLink");
   }

   public void endViewDocumentLink(WebResponse webResponse) {
       String result = webResponse.getText();
       assertTrue("Could not find content in result:\n" + result, result.indexOf("<a href")!=-1);
   }

   public void testViewDocumentLink() throws IOException, Throwable {
       try {
           ActionServlet servlet = new ActionServlet();
           servlet.init(config);
           servlet.service(request, response);
           cleanSession(session);
       } catch (ServletException e) {
           e.getRootCause().printStackTrace();
           throw e.getRootCause();
       }
   }



        public void beginEditOk(WebRequest webRequest) throws HibernateException, XWikiException {
            XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
            StoreHibernateTest.cleanUp(hibstore, context);
            Utils.createDoc(hibstore, "Main", "EditOkTest", context);
            setUrl(webRequest, "edit", "EditOkTest");
        }

        public void endEditOk(WebResponse webResponse) {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.NumberClass")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.StringClass")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.TextAreaClass")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.PasswordClass")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.BooleanClass")!=-1);
            assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.DBListClass")!=-1);
        }

        public void testEditOk() throws IOException, Throwable {
            try {
                ActionServlet servlet = new ActionServlet();
                servlet.init(config);
                servlet.service(request, response);
                cleanSession(session);
            } catch (ServletException e) {
                e.getRootCause().printStackTrace();
                throw e.getRootCause();
            }
        }

    public void beginViewRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "ViewRevOkTest", context);
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "ViewRevOkTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        hibstore.saveXWikiDoc(doc2, context);
        setUrl(webRequest, "view", "ViewRevOkTest", "rev=1.1");
    }

    public void endViewRevOk(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewRevOk() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }


    public void testSave() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginSave(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        setUrl(webRequest, "save", "SaveTest");
        webRequest.addParameter("content","Hello1Hello2Hello3");
        webRequest.addParameter("parent","Main.WebHome");
    }

    public void endSave(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "SaveTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        String content2 = doc2.getContent();
        assertEquals("Content is not indentical", "Hello1Hello2Hello3",content2);
        assertEquals("Parent is not identical", "Main.WebHome", doc2.getParent());
    }

    public void testAddProp(Class cclass) throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginAddProp(WebRequest webRequest, String name, Class cclass) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "PropAddTest", context);
        setUrl(webRequest, "propadd", "PropAddTest");
        webRequest.addParameter("propname", name);
        webRequest.addParameter("proptype", cclass.getName());
    }

    public void endAddProp(WebResponse webResponse, String name, Class cclass) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Property " + cclass.getName() + " returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseClass bclass = doc2.getxWikiClass();
        assertNotNull("Class does not exist for " + cclass.getName(), bclass);
        assertNotNull("Property of type " + cclass.getName() + " has not been added", bclass.safeget(name));
        assertEquals("Property type is not correct for " + cclass.getName(), bclass.safeget(name).getClass(), cclass);
    }

    public void testAddNumberProp() throws IOException, Throwable {
        testAddProp(NumberClass.class);
    }

    public void beginAddNumberProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "score", NumberClass.class);
    }

    public void endAddNumberProp(WebResponse response)  throws XWikiException {
        endAddProp(response, "score", NumberClass.class);
    }

    public void testAddStringProp() throws IOException, Throwable {
        testAddProp(StringClass.class);
    }

    public void beginAddStringProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "category", StringClass.class);
    }

    public void endAddStringProp(WebResponse response)  throws XWikiException {
        endAddProp(response, "category", StringClass.class);
    }

    public void testAddObject() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }

    public void beginAddObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareObject();
        Utils.createDoc(hibstore, "Main", "PropAddObjectClass", bobject, bobject.getxWikiClass(), context);
        Utils.createDoc(hibstore, "Main", "PropAddObject", context);
        setUrl(webRequest, "objectadd", "PropAddObject");
        webRequest.addParameter("classname", "Main.PropAddObjectClass");
    }

    public void endAddObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropAddObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Added Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Added Object does not have a wikiClass", bclass);

        assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
    }

    public void testAddSecondObject() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginAddSecondObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("PropAddSecondObjectClass");
        Utils.createDoc(hibstore, "Main", "PropAddSecondObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropAddSecondObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropAddSecondObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropAddSecondObject", null, null, bobjects, context);
        setUrl(webRequest, "objectadd", "PropAddSecondObject");
        webRequest.addParameter("classname", "Main.PropAddSecondObjectClass");
    }

    public void endAddSecondObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddSecondObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("First Object does not exist", bobject);
        bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 1); }
        catch (Exception e) {}
        assertNotNull("Second Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Added Object does not have a wikiClass", bclass);

        assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
    }

    public void testRemoveObject() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginRemoveObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("PropRemoveObjectClass");
        Utils.createDoc(hibstore, "Main", "PropRemoveObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropRemoveObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropRemoveObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropRemoveObject", null, null, bobjects, context);
        if (hibstore instanceof XWikiCacheInterface)
            ((XWikiCacheInterface) hibstore).flushCache();
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRemoveObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject2 = null;
        try {
            bobject2 = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Object does not exists", bobject2);

        setUrl(webRequest, "objectremove", "PropRemoveObject");
        webRequest.addParameter("classname", "Main.PropRemoveObjectClass");
        webRequest.addParameter("classid", "0");
    }

    public void endRemoveObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRemoveObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0); }
        catch (Exception e) {}
        assertNull("Object still exists", bobject);
    }


    public void testUpdateObjectProp() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }

    public void beginUpdateObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareObject("Main.PropUpdateObjectClass");
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("Main.PropUpdateObjectClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateObject", null, null, bobjects, context);

        setUrl(webRequest, "save", "PropUpdateObject");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateObjectClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_first_name", "john");
    }

    public void endUpdateObjectProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Updated Object does not have a wikiClass", bclass);

        assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
        assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

        assertNotNull("Updated Object should have age property", bobject.safeget("age"));
        assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Object name property value is incorrect", "john", name);
    }


     public void testUpdateClassProp() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }

    public void beginUpdateClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("Main.PropUpdateClassPropClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateClassPropClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateClassProp");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateClassPropClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateClassProp", null, null, bobjects, context);
        setUrl(webRequest, "propupdate", "PropUpdateClassPropClass");
        webRequest.addParameter("age_name", "age");
        webRequest.addParameter("age_size", "20");
        webRequest.addParameter("age_prettyName", "Age of person");
    }

    public void endUpdateClassProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Main", "PropUpdateClassPropClass");
        doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
        BaseClass bclass = doc.getxWikiClass();
        NumberClass ageclass = (NumberClass) bclass.safeget("age");
        assertNotNull("Updated Class wikiClass should have age property", ageclass);
        assertEquals("Updated Class age numberclass size is incorrect", 20, ageclass.getSize());
        assertEquals("Updated Class age numberclass pretty name is incorrect", "Age of person", ageclass.getPrettyName());
        assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));


        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateClassProp");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateClassPropClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Class does not exist", bobject);

        BaseClass bclass2 = bobject.getxWikiClass();
        assertNotNull("Updated Class object does not have a wikiClass", bclass2);

        NumberClass ageclass2 = (NumberClass) bclass2.safeget("age");
        assertNotNull("Updated Class wikiClass from object should have age property", ageclass2);
        assertEquals("Updated Class age numberclass from object size is incorrect", 20, ageclass2.getSize());
        assertEquals("Updated Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());
        assertNotNull("Updated Class wikiClass from object should have name property", bclass2.safeget("first_name"));

        assertNotNull("Updated Class should have age property", bobject.safeget("age"));
        assertNotNull("Updated Class should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Class age property value is incorrect", new Integer(33), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Class name property value is incorrect", "Ludovic", name);
    }



    public void testRenameClassProp() throws IOException, Throwable {
       try {
           ActionServlet servlet = new ActionServlet();
           servlet.init(config);
           servlet.service(request, response);
           cleanSession(session);
       } catch (ServletException e) {
           e.getRootCause().printStackTrace();
           throw e.getRootCause();
       }
   }

   public void beginRenameClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
       XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
       StoreHibernateTest.cleanUp(hibstore, context);
       BaseObject bobject = Utils.prepareObject();
       BaseClass bclass = bobject.getxWikiClass();
       bclass.setName("Main.PropRenameClassPropClass");
       Utils.createDoc(hibstore, "Main", "PropRenameClassPropClass", bobject, bclass, context);
       Map bobjects = new HashMap();
       Vector bobjlist = new Vector();
       bobject.setName("Main.PropRenameClassProp");
       bobjlist.add(bobject);
       bobjects.put("Main.PropRenameClassPropClass", bobjlist);
       Utils.createDoc(hibstore, "Main", "PropRenameClassProp", null, null, bobjects, context);
       setUrl(webRequest, "propupdate", "PropRenameClassPropClass");
       webRequest.addParameter("age_name", "age2");
       webRequest.addParameter("age_size", "40");
       webRequest.addParameter("age_prettyName", "Age of person");
   }

   public void endRenameClassProp(WebResponse webResponse) throws XWikiException {
       String result = webResponse.getText();
       // Verify return
       assertTrue("Rename Class returned exception", result.indexOf("Exception")==-1);
       XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
       XWikiSimpleDoc doc = new XWikiSimpleDoc("Main", "PropRenameClassPropClass");
       doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
       BaseClass bclass = doc.getxWikiClass();
       NumberClass ageclass2 = (NumberClass) bclass.safeget("age2");
       assertNotNull("Rename Class wikiClass should have age2 property", ageclass2);
       assertEquals("Rename Class age2 numberclass size is incorrect", 40, ageclass2.getSize());

       NumberClass ageclass = (NumberClass) bclass.safeget("age");
       assertNull("Rename Class wikiClass should not have age property", ageclass);

       assertEquals("Rename Class age2 numberclass pretty name is incorrect", "Age of person", ageclass2.getPrettyName());
       assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));

       // Check object in the Class
       BaseObject bobject = null;
       try { bobject = (BaseObject) doc.getObject("Main.PropRenameClassPropClass", 0); }
       catch (Exception e) {}
       assertNotNull("Rename Class object does not exist", bobject);

       BaseClass bclass2 = bobject.getxWikiClass();
       assertNotNull("Rename Class does not have a wikiClass", bclass2);

       ageclass2 = (NumberClass) bclass.safeget("age2");
       assertNotNull("Rename Class wikiClass from object should have age2 property", ageclass2);

       ageclass = (NumberClass) bclass.safeget("age");
       assertNull("Rename Class wikiClass from object should not have age property", ageclass);

       assertEquals("Rename Class age numberclass from object size is incorrect", 40, ageclass2.getSize());
       assertEquals("Rename Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());


       assertNotNull("Rename Class wikiClass should have name property", bclass2.safeget("first_name"));

       assertNull("Rename Class object should not have age property", bobject.safeget("age"));
       assertNotNull("Rename Class object should have age property", bobject.safeget("age2"));
       assertNotNull("Rename Class object should have name property", bobject.safeget("first_name"));

       Number age = (Number)((NumberProperty)bobject.safeget("age2")).getValue();
       assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
       String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
       assertEquals("Rename Class name property value is incorrect", "Ludovic", name);

       XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRenameClassProp");
       doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
       bobject = null;
       try { bobject = (BaseObject) doc2.getObject("Main.PropRenameClassPropClass", 0); }
       catch (Exception e) {}
       assertNotNull("Rename Class object does not exist", bobject);

       bclass2 = bobject.getxWikiClass();
       assertNotNull("Rename Class does not have a wikiClass", bclass2);

       ageclass2 = (NumberClass) bclass.safeget("age2");
       assertNotNull("Rename Class wikiClass from object should have age2 property", ageclass2);

       ageclass = (NumberClass) bclass.safeget("age");
       assertNull("Rename Class wikiClass from object should not have age property", ageclass);

       assertEquals("Rename Class age numberclass from object size is incorrect", 40, ageclass2.getSize());
       assertEquals("Rename Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());


       assertNotNull("Rename Class wikiClass should have name property", bclass2.safeget("first_name"));

       assertNull("Rename Class object should not have age property", bobject.safeget("age"));
       assertNotNull("Rename Class object should have age property", bobject.safeget("age2"));
       assertNotNull("Rename Class object should have name property", bobject.safeget("first_name"));

       age = (Number)((NumberProperty)bobject.safeget("age2")).getValue();
       assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
       name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
       assertEquals("Rename Class name property value is incorrect", "Ludovic", name);
   }



    public void testUpdateAdvancedObjectProp() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }

    public void beginUpdateAdvancedObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        BaseObject bobject = Utils.prepareAdvancedObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("PropUpdateAdvObjectClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateAdvObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateAdvObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateAdvObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateAdvObject", null, null, bobjects, context);
        setUrl(webRequest, "save", "PropUpdateAdvObject");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_first_name", "john");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category2", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category2", "3");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category3", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category3", "3");
    }

    public void endUpdateAdvancedObjectProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateAdvObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateAdvObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Updated Object does not have a wikiClass", bclass);

        assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
        assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

        assertNotNull("Updated Object should have age property", bobject.safeget("age"));
        assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Object name property value is incorrect", "john", name);

        String category = (String)((StringProperty)bobject.safeget("category")).getValue();
        assertEquals("Updated Object category property value is incorrect", "2", category);

        List category2 = (List)((ListProperty)bobject.safeget("category2")).getValue();
        assertEquals("Updated Object category2 property size is incorrect", 2, category2.size());
        assertEquals("Updated Object category2 property item 1 is incorrect", "2", category2.get(0));
        assertEquals("Updated Object category2 property item 2 is incorrect", "3", category2.get(1));

        List category3 = (List)((ListProperty)bobject.safeget("category3")).getValue();
        assertEquals("Updated Object category3 property size is incorrect", 2, category3.size());
        assertEquals("Updated Object category3 property item 1 is incorrect", "2", category3.get(0));
        assertEquals("Updated Object category3 property item 2 is incorrect", "3", category3.get(1));
    }



    public void sendMultipart(WebRequest webRequest, File file) throws IOException {
        Part part = new FilePart("application/octet-stream", file);
        Part[] parts = new Part[1];
        parts[0] = part;

        if (Part.getBoundary() != null) {
            webRequest.setContentType(
                    "multipart/form-data" + "; boundary=" + Part.getBoundary());
        }

        PipedInputStream pipedin = new PipedInputStream();
        PipedOutputStream pipedout = new PipedOutputStream(pipedin);
        MultipartSenderThread sender = new MultipartSenderThread(pipedout, parts);
        sender.start();
        webRequest.setUserData(pipedin);
    }


    public void testAttach() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }


    public void beginAttach(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttach(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        File fattach = new File(Utils.filename);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }


    public void testAttachUpdate() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }


    public void beginAttachUpdate(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);

        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "AttachTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        hibstore.saveXWikiDoc(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);

        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttachUpdate(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        File fattach = new File(Utils.filename);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment version is not correct", attachment.getVersion(), "1.2");
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }

    public static class MultipartSenderThread extends Thread  {

        private PipedOutputStream out;
        private Part[] parts;

        protected MultipartSenderThread(PipedOutputStream outs, Part[] source) {
            out = outs;
            parts = source;
        }

        public void run() {
            try {
                Part.sendParts(out, parts);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



    public void beginVirtualViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest", context);
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest", "view", "VirtualViewOkTest", "");
    }

    public void endVirtualViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testVirtualViewOk() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }


}
