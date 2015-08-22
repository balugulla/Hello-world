package org.gulla.service.gulla.test;

import org.gulla.service.gulla.threads.ThreadImpl;
import org.gulla.service.gulla.threads.ThreadRunnable;

public class HelloWorld {

	public static void main(String[] args) {
		System.out.println("hello world guys");
        ThreadRunnable runnable = new ThreadRunnable();
        ThreadImpl t = new ThreadImpl(runnable);
        t.start();
	}

}
