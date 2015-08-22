package org.gulla.service.gulla.threads;

public class ThreadImpl extends Thread {
	
	public ThreadImpl(){
		super();
	}
	
   public ThreadImpl(Runnable r){
	   super(r);
	   
   }
}
