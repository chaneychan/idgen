Introduction

   idgen is the  Global ID generation Platform ,thus like the twitter snowflake(https://github.com/twitter/snowflake).

   The use sample of idgen  is thus like the following :
    
   1)  	ApplicationContext context = new ClassPathXmlApplicationContext("classpath:client.xml");
	MemIDPool idPool = context.getBean(MemIDPool.class);
	String id = idPool.borrow();
	
   2)   client have to butt soa
   
   3)   examples are the following address: git@github.com:chaneychan/idgen-sample.git


架构概览：

![image](https://github.com/chaneychan/idgen/blob/master/doc/%E5%88%86%E5%B8%83%E5%BC%8F%E5%85%A8%E5%B1%80id--%E6%9E%B6%E6%9E%84%E6%A6%82%E8%A7%881.png)
