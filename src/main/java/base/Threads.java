package base;

public class Threads {
	public static void main(String[] args) {
		
		Thread t = new Thread(new Th());
		t.setName("A");
		Thread t2 = new Thread(t);
		t2.setName("B");
		System.out.println(Thread.currentThread().getName());
		System.err.println(t.getName());
		t2.start();
	}

	static class Th extends Thread{
		@Override
		public void run() {
			System.err.println(Thread.currentThread().getName());
			System.out.println(this.getName());
		}
	}
}
