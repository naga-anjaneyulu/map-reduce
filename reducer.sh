cd map-reduce
git pull origin main
cd reducer
mvn clean install
cd target
java -jar reducer-0.0.1-SNAPSHOT.jar