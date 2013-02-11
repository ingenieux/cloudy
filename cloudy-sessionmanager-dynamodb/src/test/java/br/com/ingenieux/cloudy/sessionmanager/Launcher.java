package br.com.ingenieux.cloudy.sessionmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

public class Launcher {
	private Tomcat tomcat;

	public Launcher() {
		this.tomcat = new Tomcat();
	}
	
	public static void main(String[] args) throws Exception {
		Launcher launcher = new Launcher();
		
		launcher.start();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		reader.readLine();
		
		launcher.stop();
	}

	protected void start() throws Exception {
		tomcat.setPort(8080);
		Context context = tomcat.addWebapp("/", new File("src/test/webapp").getAbsolutePath());
		
		DynamoDBManager manager = new DynamoDBManager();
		DynamoDBStore store = new DynamoDBStore();
		
		store.setTable("cloudy-test-sessions");
		
		manager.setStore(store);
		
		context.setManager(manager);
		
		tomcat.start();
	}
	
	protected void stop() throws Exception {
		tomcat.stop();
	}
}
