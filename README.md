Introduction

   idgen is the  Global ID generation Platform ,thus like the twitter snowflake(https://github.com/twitter/snowflake).

   The use sample of idgen  is thus like the following :
    
   1)  	ApplicationContext context = new ClassPathXmlApplicationContext("classpath:client.xml");
	   	MemIDPool idPool = context.getBean(MemIDPool.class);
	  	String id = idPool.borrow();
   
   2)   client have to butt soa
   
   3)   examples are the following address: git@github.com:chaneychan/idgen-sample.git
