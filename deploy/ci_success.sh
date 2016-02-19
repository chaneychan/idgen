#!/usr/bin/env bash
mkdir -p release/ 
wget -P release/ http://192.168.65.11/deploy_dependencies/commons/tomcat.tar.gz
tar -xzf release/tomcat.tar.gz -C release/
rm -rf release/tomcat.tar.gz
mkdir -p release/tomcat/webapps/ROOT
unzip idgen-service/target/idgen-service.war -d release/tomcat/webapps/ROOT
cp deploy/start.sh release/
cp deploy/appspec.yml release/
cp deploy/validate.sh release/

cd release/
appid=me.ele.idgen
tomcat_jmx_port=10826
tomcat_shutdown_port=8005
tomcat_bind_port=9080
tomcat_redirect_port=8443
tomcat_ajp_port=8009
tomcat_ajp_redirect_port=8443
echo $appid
echo $tomcat_jmx_port
echo $tomcat_shutdown_port
echo $tomcat_bind_port
echo $tomcat_redirect_port
echo $tomcat_ajp_port
echo $tomcat_ajp_redirect_port

sed -i 's/appid/'"$appid"'/g' tomcat/bin/setenv.sh.etpl
sed -i 's/jmxport/'"$tomcat_jmx_port"'/g' tomcat/bin/setenv.sh.etpl

sed -i 's/appid/'"$appid"'/g' tomcat/conf/logging.properties

sed -i 's/tomcatport/'"$tomcat_shutdown_port"'/g' tomcat/conf/server.xml
sed -i 's/bindport/'"$tomcat_bind_port"'/g' tomcat/conf/server.xml
sed -i 's/bindredirect/'"$tomct_redirect_port"'/g' tomcat/conf/server.xml
sed -i 's/ajpport/'"$tomcat_ajp_port"'/g' tomcat/conf/server.xml
sed -i 's/ajpredirect/'"$tomcat_ajp_redirect_port"'/g' tomcat/conf/server.xml
sed -i 's/appid/'"$appid"'/g' tomcat/conf/server.xml