package br.com.ingenieux.cloudy.sessionmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.LifecycleSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Extends the <b>ManagerBase</b> class to provide cloud architecture awareness to Tomcat sessions.
 */
public class DynamoDBManager extends ManagerBase implements Lifecycle, LifecycleListener, PropertyChangeListener {

  /**
   * Info about this implementation of a <b>Manager</b>.
   */
  protected static final String info = "DynamoDBManager/1.0";
  /**
   * Name of this implementation.
   */
  protected static final String name = "DynamoDBManager";

  protected Logger log = LoggerFactory.getLogger(getClass());
  
  /**
   * <b>Store</b> object to manage user sessions.
   */
  protected DynamoDBStore store;
  
  /**
   * We don't actually worry about lifecycle events directly, we delegate this to an internal <b>LifecycleSupport</b>
   * object.
   */
  protected LifecycleSupport lifecycle = new LifecycleSupport(this);
  /**
   * I'm not actually using this yet, though I don't know that I should yank it out of the source quite yet.
   */
  protected PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);
  /**
   * Is this <b>Manager</b> in a started state?
   */
  protected AtomicBoolean started = new AtomicBoolean(false);
  /**
   * Not sure what this is supposed to be used for.
   */
  protected AtomicInteger rejectedSessions = new AtomicInteger(0);
  /**
   * The default inactivity timeout is 900 rather than the superclass's default of 60.
   */
  protected int maxInactiveInterval = 900;

  @Override
  public String getInfo() {
    return info;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Get the <b>DynamoDBStore</b> attached to this <b>Manager</b>.
   *
   * @return
   */
  public DynamoDBStore getStore() {
    return store;
  }

  /**
   * Set the <b>DynamoDBStore</b> this <b>Manager</b> should use to maintain user sessions. Note that this is not the
   * generic <b>Store</b> interface, but references the specific cloud-aware <b>Store</b>.
   *
   * @param store
   */
  public void setStore(Store store) {
    if (store instanceof DynamoDBStore) {
      this.store = (DynamoDBStore) store;
      store.setManager(this);
    }
  }

  @Override
  public void processExpires() {
    store.processExpires();
  }

  @Override
  public void add(Session session) {
    try {
      store.save(session);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public Session createSession(String sessionId) {
    MDC.put("method", "createSession()");

    Session session = createEmptySession();
    session.setNew(true);
    session.setValid(true);
    session.setCreationTime(System.currentTimeMillis());
    session.setMaxInactiveInterval(this.maxInactiveInterval);
    if (null == sessionId) {
      session.setId(generateSessionId());
      sessionCounter++;
    } else {
      session.setId(sessionId);
    }
    if (log.isDebugEnabled()) {
      log.debug("Created a new session: " + session.getId());
    }

    MDC.remove("method");

    return session;
  }

  @Override
  public Session createEmptySession() {
    return getNewSession();
  }

  @Override
  public Session findSession(String id) throws IOException {
    MDC.put("method", "findSession()");

    Session session = null;
    if (store.isValidSession(id)) {
      // Try to find it somewhere in the cloud
      if (log.isDebugEnabled()) {
        log.debug("Trying to load session " + id + " from store...");
      }
      try {
        session = store.load(id);
      } catch (ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      }
    } else {
      return null;
    }

    // This part heavily-influenced by Tomcat's PersistentManagerBase
    if (null != session) {
      if (!session.isValid()) {
        if (log.isDebugEnabled()) {
          log.debug("Session " + id + " invalid.");
        }
        session.expire();
        remove(session);
        return null;
      }
      session.setManager(this);
      ((StandardSession) session).tellNew();
      //add( session );
      session.endAccess();
    }

    if (null == session) {
      if (log.isDebugEnabled()) {
        log.debug("Session " + id + " not found.");
      }
    }

    MDC.remove("method");

    return session;
  }

  public void clearLocalSession(String id) {
    sessions.remove(id);
  }

  @Override
  public Session[] findSessions() {
    String[] ids = new String[0];
    try {
      ids = store.keys();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    List<Session> sessions = new ArrayList<Session>();
    for (String id : ids) {
      try {
        Session sess = store.load(id);
        if (null != sess) {
          sessions.add(sess);
        }
      } catch (ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    }
    return sessions.toArray(new Session[sessions.size()]);
  }

  @Override
  public void remove(Session session) {
    try {
      store.remove(session.getId());
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  protected StandardSession getNewSession() {
    return new DynamoDBSession(this);
  }

  @Override
  public String listSessionIds() {
    StringBuffer buff = new StringBuffer();
    boolean needsComma = false;
    try {
      for (String id : store.keys()) {
        if (needsComma) {
          buff.append(", ");
        } else {
          needsComma = true;
        }
        buff.append(id);
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return buff.toString();
  }

  @Override
  public String getSessionAttribute(String sessionId, String key) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      Object o = session.getSession().getAttribute(key);
      if (o instanceof String) {
        return (String) o;
      } else {
        return (null != o ? o.toString() : null);
      }
    }

    return null;
  }

  @Override
  public HashMap<String, String> getSession(String sessionId) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      HttpSession httpSession = session.getSession();
      HashMap<String, String> map = new HashMap<String, String>();
      String key = null;
      for (Enumeration<String> keys = httpSession.getAttributeNames(); keys.hasMoreElements(); key = keys.nextElement()
          .toString()) {
        map.put(key, "" + httpSession.getAttribute(key));
      }
      return map;
    }
    
    return new HashMap<String, String>();
  }

  @Override
  public void expireSession(String sessionId) {
    try {
      store.remove(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public long getLastAccessedTimestamp(String sessionId) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      return session.getLastAccessedTime();
    }
    return -1L;
  }

  @Override
  public String getLastAccessedTime(String sessionId) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      return String.valueOf(session.getLastAccessedTime());
    }
    return null;
  }

  @Override
  public String getCreationTime(String sessionId) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      return String.valueOf(session.getCreationTime());
    }
    return null;
  }

  @Override
  public long getCreationTimestamp(String sessionId) {
    Session session = null;
    try {
      session = findSession(sessionId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    if (null != session) {
      return session.getCreationTime();
    }
    return -1L;
  }

  /**
   * Delegate this to the internal <b>LifecycleSupport</b> object, which manages these things for us.
   *
   * @param lifecycleListener
   */
  public void addLifecycleListener(LifecycleListener lifecycleListener) {
    lifecycle.addLifecycleListener(lifecycleListener);
  }

  /**
   * Delegate this to the internal <b>LifecycleSupport</b> object, which manages these things for us.
   *
   * @return
   */
  public LifecycleListener[] findLifecycleListeners() {
    return lifecycle.findLifecycleListeners();
  }

  /**
   * Delegate this to the internal <b>LifecycleSupport</b> object, which manages these things for us.
   *
   * @param lifecycleListener
   */
  public void removeLifecycleListener(LifecycleListener lifecycleListener) {
    lifecycle.removeLifecycleListener(lifecycleListener);
  }

  /**
   * Delegate this to the internal <b>LifecycleSupport</b> object, which manages these things for us.
   *
   * @param event
   */
  public void lifecycleEvent(LifecycleEvent event) {
    log.debug(event.toString());
  }

  /**
   * We only care about changes to <code>sessionTimeout</code> properties at this point in time.
   *
   * @param propertyChangeEvent
   */
  public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
    if (!(propertyChangeEvent.getSource() instanceof Context)) {
      // Ignore these
      return;
    }

    if (propertyChangeEvent.getPropertyName().equals("sessionTimeout")) {
      try {
        setMaxInactiveInterval(Integer.parseInt(propertyChangeEvent.getNewValue().toString()) * 60);
      } catch (NumberFormatException nfe) {
        log.error(nfe.getMessage(), nfe);
      }
    }
  }

  /**
   * Not really sure what this is supposed to do? :)
   *
   * @return
   */
  public int getRejectedSessions() {
    return rejectedSessions.get();
  }

  /**
   * Not really sure what this is supposed to do? :)
   *
   * @param i
   */
  public void setRejectedSessions(int i) {
    rejectedSessions.set(i);
  }

  /**
   * These aren't used at the moment, but I suspect replication events will be triggered by these methods.
   *
   * @throws ClassNotFoundException
   * @throws IOException
   */
  public void load() throws ClassNotFoundException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("load()");
    }
  }

  /**
   * These aren't used at the moment, but I suspect replication events will be triggered by these methods.
   *
   * @throws IOException
   */
  public void unload() throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("unload()");
    }
  }
  
  @Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();
		
		setState(LifecycleState.STARTING);
	}
}