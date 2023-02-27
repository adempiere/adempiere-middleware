#!/usr/bin/env sh
# @autor Yamel Senih <ysenih@erpya.com>

# Set server values
sed -i "s|50059|$SERVER_PORT|g" env.yaml
sed -i "s|WARNING|$SERVER_LOG_LEVEL|g" env.yaml


# create array to iterate
SERVICES_LIST=$(echo $SERVICES_ENABLED | tr "; " "\n")

SERVICES_LIST_TO_SET=""
for SERVICE_ITEM in $SERVICES_LIST
do
    # Service to lower case
    SERVICE_LOWER_CASE=$(echo $SERVICE_ITEM | tr '[:upper:]' '[:lower:]')

    NEW_LINE="\n"
    PREFIX="        - "
    if [ -z "$SERVICES_LIST_TO_SET" ]
    then
        NEW_LINE=""
        PREFIX="- "
    fi

    # Add to the list of services
    SERVICES_LIST_TO_SET="${SERVICES_LIST_TO_SET}${NEW_LINE}${PREFIX}${SERVICE_LOWER_CASE}"
done

sed -i "s|- services_enabled|$SERVICES_LIST_TO_SET|g" env.yaml

export DEFAULT_JAVA_OPTIONS='"-Xms64M" "-Xmx1512M"'
# Set data base conection values
sed -i "s|localhost|$DB_HOST|g" env.yaml
sed -i "s|5432|$DB_PORT|g" env.yaml
sed -i "s|adempieredb|$DB_NAME|g" env.yaml
sed -i "s|adempiereuser|$DB_USER|g" env.yaml
sed -i "s|adempierepass|$DB_PASSWORD|g" env.yaml
sed -i "s|PostgreSQL|$DB_TYPE|g" env.yaml
sed -i "s|PostgreSQL|$DB_TYPE|g" env.yaml
sed -i "s|$DEFAULT_JAVA_OPTIONS|$GRPC_JAVA_OPTIONS|g" adempiere-middleware-server


# Run app
./adempiere-middleware-server ./env.yaml
