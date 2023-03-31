FROM gradle:8.0.2 as builder
ARG BUILD_ARTIFACT \
	GITHUB_DEPLOY_USER \
	GITHUB_DEPLOY_TOKEN \
	GITHUB_DEPLOY_REPOSITORY

LABEL maintainer="ysenih@erpya.com" \
	description="ADempiere gRPC Middleware used as ADempiere backend as little core"

WORKDIR /opt/apps/server

COPY . . /opt/apps/server/

EXPOSE ${SERVER_PORT}

ENV GITHUB_DEPLOY_USER=$GITHUB_DEPLOY_USER \
	GITHUB_DEPLOY_TOKEN=$GITHUB_DEPLOY_TOKEN \ 
	GITHUB_DEPLOY_REPOSITORY=$GITHUB_DEPLOY_REPOSITORY

RUN gradle build && \
	unzip -d /opt/apps build/distributions/adempiere-middleware.zip && \
	mv docker/env.yaml /opt/apps/adempiere-middleware && \
	mv docker/start.sh /opt/apps/adempiere-middleware

FROM eclipse-temurin:11-jdk-alpine

# Init ENV with default values
ENV \
	SERVER_PORT="50059" \
	SERVER_LOG_LEVEL="WARNING" \
	SERVER_PRIVATE_KEY="" \
	DB_HOST="localhost" \
	DB_PORT="5432" \
	DB_NAME="adempiere" \
	DB_USER="adempiere" \
	DB_PASSWORD="adempiere" \
	DB_TYPE="PostgreSQL" \
	ADEMPIERE_APPS_TYPE=""

WORKDIR /opt/apps/server

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


COPY --from=builder /opt/apps/adempiere-middleware/ /opt/apps/server/

RUN addgroup adempiere && \
	adduser --disabled-password --gecos "" --ingroup adempiere --no-create-home adempiere && \
	chown -R adempiere /opt/apps/server/ && \
	chmod +x start.sh

USER adempiere

# Start app
ENTRYPOINT ["sh" , "start.sh"]

