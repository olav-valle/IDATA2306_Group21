FROM openjdk:11


RUN apt-get update && apt-get install -y maven
RUN mkdir /project
#VOLUME /home/mort/git/spring_rest_sandbox/:/project
COPY . /project
#WORKDIR /project
#RUN chmod +x entrypoint.sh
RUN cd /project && mvn package && cp /project/target/IDATA2306_Group21-1.0-SNAPSHOT-jar-with-dependencies.jar /project/app.jar
#EXPOSE 8080
WORKDIR /project

#ENTRYPOINT ["java", "-jar", "/project/target/IDATA2306_Group21-1.0-SNAPSHOT-jar-with-dependencies.jar"]
ENTRYPOINT ["java", "-jar", "/project/app.jar"]

#ENTRYPOINT ["/bin/bash"]