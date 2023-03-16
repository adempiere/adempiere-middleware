FROM postgres:13

ADD https://github.com/adempiere/adempiere/releases/download/3.9.4/Adempiere_394LTS.tar.gz /tmp/Adempiere_394LTS.tar.gz

COPY --chown=1  initdb.sh /docker-entrypoint-initdb.d
RUN chmod +x /docker-entrypoint-initdb.d/initdb.sh && \
	tar -xvf /tmp/Adempiere_394LTS.tar.gz Adempiere/data/Adempiere_pg.dmp  -C /tmp/ && \
	rm /tmp/Adempiere_394LTS.tar.gz
