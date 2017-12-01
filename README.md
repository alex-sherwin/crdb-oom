# CockroachDB JDBC OutOfMemoryError on PgArray

1. Setup a testing CRDB Docker container + test data
    ```bash
    docker run -d \
      --name=test1 \
      --hostname=test1 \
      -p 26267:26257 -p 8888:8080  \
      -v test1-data:/cockroach/cockroach-data  \
      cockroachdb/cockroach:v1.1.3 start --insecure
    
    # create user
    docker exec test1 ./cockroach user set test1 --insecure
    # create db
    docker exec -it test1 ./cockroach sql --insecure -e 'CREATE DATABASE test1'
    # setup grants
    docker exec -it test1 ./cockroach sql --insecure -e 'GRANT ALL ON DATABASE test1 TO test1'
    
    # create tables
    docker exec -it test1 ./cockroach sql --insecure -e 'CREATE TABLE test1.sometable1 (id UUID PRIMARY KEY, name TEXT)'
    docker exec -it test1 ./cockroach sql --insecure -e 'CREATE TABLE IF NOT EXISTS test1.sometable2 ( id UUID PRIMARY KEY, dnsServers TEXT [] NULL, sometable1_id UUID NULL REFERENCES test1.sometable1 (id) )'
    
    # insert test data
    docker exec -it test1 ./cockroach sql --insecure -e "INSERT INTO test1.sometable1 (id, name) VALUES ('00000000-0000-0000-0000-000000000000', 'somename')"
    docker exec -it test1 ./cockroach sql --insecure -e "INSERT INTO test1.sometable2 (id, dnsServers, sometable1_id ) VALUES ( '00000000-0000-0000-0000-000000000000', ARRAY['server1.com', 'server2.com'], '00000000-0000-0000-0000-000000000000' )"
    ```
1. Run `crdboom.Main`
1. Observe OOM, example:
    ```
    Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
      at org.postgresql.jdbc.PgArray.readBinaryArray(PgArray.java:191)
      at org.postgresql.jdbc.PgArray.toString(PgArray.java:879)
      at java.lang.String.valueOf(String.java:2994)
      at java.lang.StringBuilder.append(StringBuilder.java:131)
      at crdboom.Main.main(Main.java:40)
    ```
    
# What's going on?

* For some reason, after *exactly* 5 iterations for some reason `org.postgresql.core.Field.setFormat()` is invoked with value `1` (which is binary mode) for column `dnsServers`
* The next time `ResultSet.getArray()` is invoked, during `PgArray.readBinaryArray` execution the array dimensions are read with `int dimensions = ByteConverter.int4(fieldBytes, 0);`, but since the mode was changed to binary, the value returned here seems obviously wrong, it is a huge number which is used to initialize an `int[]` array which causes the `OutOfMemoryError` to occur

# Why this weird project setup?

* I have no idea why, but if using a single connection, it doesn't seem to happen.  I've only observed it when using a pool like HikariCP
* If you simply select from `sometable2` and read the array, this doesn't occur.  It only seems happens if you first run a select which joins the table and then select from the table on the same `Connection`
* This is the smallest working proof of concept I could create based on the observed symptoms from a larger project
 