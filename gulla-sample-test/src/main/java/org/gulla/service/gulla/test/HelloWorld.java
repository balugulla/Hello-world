package org.gulla.service.gulla.test;

public class HelloWorld {

	public static void main(String[] args) {
		System.out.println("hello world guys");
        ThreadRunnable runnable = new ThreadRunnable();
        Thread t = new Thread(runnable);
        t.start();
	}

}
