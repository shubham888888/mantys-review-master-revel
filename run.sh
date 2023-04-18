export JAVA_HOME=`/usr/libexec/java_home -v 19`
mvn -pl commons,dao,services -amd clean install
cd services
mvn spring-boot:run 
