package me.ele.idgen.client;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
//     public static void main(String[] args) {
//          int count = 200;
//          CyclicBarrier cyclicBarrier = new CyclicBarrier(count);
//          ExecutorService executorService = Executors.newFixedThreadPool(count);
//          for (int i = 0; i < count; i++)
//               executorService.execute(new Test().new Task(cyclicBarrier));
//
//          executorService.shutdown();
//          while (!executorService.isTerminated()) {
//               try {
//                    Thread.sleep(10);
//               } catch (InterruptedException e) {
//                    e.printStackTrace();
//               }
//          }
//     }
	
	public static void main(String[] args) {
		 MockIDGenerator g = new MockIDGenerator();
         MemIDPool p = new MemIDPool("order", "orderId",20, g);
         
         for(int i =0 ;i <100000 ; i++){
//        	  p.borrow();
         }
       
	}

     public class Task implements Runnable {
          private CyclicBarrier cyclicBarrier;

          public Task(CyclicBarrier cyclicBarrier) {
               this.cyclicBarrier = cyclicBarrier;
          }

          @Override
          public void run() {
               try {
                    // 等待所有任务准备就绪
                    cyclicBarrier.await();
                    // 测试内容
                    MockIDGenerator g = new MockIDGenerator();
                    MemIDPool p = new MemIDPool("order", "orderId",40, g);
                    p.borrow();
               } catch (Exception e) {
                    e.printStackTrace();
               }
          }
     }
}