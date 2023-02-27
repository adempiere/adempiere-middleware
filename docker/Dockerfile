FROM registry.digitalocean.com/erpya/adempiere-cloud:latest as libs
FROM gradle:7.4.1 as bridge
ARG BUILD_ARTIFACT 

COPY --from=libs /opt/Adempiere/jboss/server/adempiere/deploy/adempiere.ear/  /opt/libDeploy/

LABEL	maintainer="ysenih@erpya.com; EdwinBetanc0urt@outlook.com" \
	description="ADempiere gRPC All In One Server used as ADempiere backend"

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	SERVICES_ENABLED="access; business; core; dashboarding; dictionary; enrollment; log; ui; workflow; store; pos;" \
	SERVER_LOG_LEVEL="WARNING" \
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
    BUILD_ARTIFACT=$BUILD_ARTIFACT



EXPOSE ${SERVER_PORT}

COPY $BUILD_ARTIFACT /opt/Apps/source

RUN cd /opt/libDeploy && \
        unzip -o adempiereRoot.war  && \
        unzip adempiereHome/AdempiereClient.zip && \
        mkdir /opt/Apps/source/dependences && \
        mv Adempiere/lib/ext/*.jar /opt/Apps/source/dependences/ && \
        mv Adempiere.jar /opt/Apps/source/dependences/ && \
        mv AdempiereSLib.jar /opt/Apps/source/dependences/ && \
        rm /opt/Apps/source/dependences/adempiere-gRPC-Server.jar && \
        rm -R /opt/libDeploy && \
        cd /opt/Apps/source && \
        gradle build && \
        unzip -d /opt/Apps /opt/Apps/source/build/distributions/adempiere-customer-backend.zip && \
        mv /opt/Apps/adempiere-customer-backend /opt/Apps/ADempiere-gRPC-Server && \
        cp /opt/Apps/source/build-docker/all_in_one_connection.yaml /opt/Apps/ADempiere-gRPC-Server/bin/ && \
        cp /opt/Apps/source/build-docker/start.sh /opt/Apps/ADempiere-gRPC-Server/bin/ && \
        cp /opt/Apps/source/build-docker/all_in_one_connection.yaml /opt/Apps/ADempiere-gRPC-Server/bin/

FROM openjdk:8-jdk-alpine

# timezone
ENV TZ America/Caracas

# Add operative system dependencies
RUN	rm -rf /var/cache/apk/* && \
	rm -rf /tmp/* && \
	apk update && \
	apk add --no-cache \
		bash \
	 	fontconfig \
		ttf-dejavu && \
		echo "Set Timezone..." && \
	 	echo $TZ > /etc/timezone && \
                apk add --no-cache \
		tzdata

COPY --from=bridge /opt/Apps/ADempiere-gRPC-Server /opt/Apps/ADempiere-gRPC-Server

WORKDIR /opt/Apps/ADempiere-gRPC-Server/bin/


# Start app
CMD	'sh' 'start.sh'
