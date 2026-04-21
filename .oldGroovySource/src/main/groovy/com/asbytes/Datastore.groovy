package com.asbytes

import groovy.sql.GroovyRowResult

class Datastore implements Closeable {
    String file
    String driver
    String table
    groovy.sql.Sql sql = null

    Datastore(String file = "./transactions.sqlite", String driver = "org.sqlite.JDBC", String table = "transactions") {
        this.file = file
        this.driver = driver
        this.table = table
    }

    public void checkConnection() {
        if (!this.sql.connection) {
            this.openConnection();
        }
    }

    public Datastore openConnection() {
        try {
            def dbPath = new File(this.file).absolutePath
            print "Connecting to DB: ${dbPath}..."
            sql = groovy.sql.Sql.newInstance("jdbc:sqlite:${dbPath}", driver)
            println " connected"
            return this
        } catch(err) {
            println " failed"
            println err
            return null
        }
    }

    public boolean closeConnection() {
        def dbPath = new File(this.file).absolutePath
        print "Disconnecting from DB: ${dbPath}..."
        try {
            sql?.close()
            println "disconnected"
            return true
        } catch(err) {
            println " failed\\$err"
            return false
        }
    }

//    public void executeAction(Closure<Datastore> actions = null, Object arg = null) {
//        try {
//            this.openConnection().ensureTableExists()
//
//            if (actions) {
//                print "Executing actions... "
//                def result = actions.call(this, arg)
//                println ((result) ? "executed" : "failed to execute")
//            }
//        } catch(err) {
//            println " failed\n${err}\n${err.stackTrace.join('\n')}"
//        } finally {
//            this.closeConnection()
//        }
//    }

//    public static void save(Datastore datastore, Transaction transaction) {
//        datastore.saveTransaction(transaction)
//    }

    public List<GroovyRowResult> loadTransactions() {
        this.checkConnection()

        def query = """
            SELECT * FROM ${this.table};
        """

        return this.sql.rows(query.toString().trim())
    }

    public int saveTransaction(Transaction transaction) {
        this.checkConnection()

        def checkSum = transaction.file.bytes.digest('MD5')
        def dataVersion = transaction.sqlDataVersion
        def relFilePath = transaction.getRelativePath()
        def fileSize = transaction.file.size()
        def fileDate = transaction.file.lastModified()
        def dataBlob = transaction.toBlob()

        def query = """
            INSERT OR REPLACE INTO ${this.table} (md5, version, source, date, size, data)
            VALUES (?, ?, ?, ?, ?, ?)
        """

        def values = [checkSum, dataVersion, relFilePath, fileDate, fileSize, dataBlob]

        def saved = 0
        try {
            saved = this.sql.executeInsert(query.toString().trim(), values).size()
        } catch(err) {
            println " failed to insert\n${err}\n${err.stackTrace.join('\n')}"
        }
        return saved
    }

//    public static List<GroovyRowResult> executeQuery(Datastore datastore, String sql) {
//        return datastore.executeQuery(sql)
//    }

//    public List<GroovyRowResult> executeQuery(String sql) {
//        return [this.sql.firstRow(sql.toString().trim())]
//    }

    def Datastore ensureTableExists() {
        this.checkConnection()

        if (this.table) {
            try {
                def query = """
                    CREATE TABLE IF NOT EXISTS ${this.table} (
                        "md5" TEXT NOT NULL PRIMARY KEY,
                        "version" INTEGER NOT NULL,
                        "source" TEXT NOT NULL,
                        "date" INTEGER NOT NULL,
                        "size" INTEGER NOT NULL,
                        "data" BLOB NOT NULL
                    );
                """
                this.sql.execute(query.toString().trim())
                return this
            } catch(err) {
                println "Failed to ensure table exists"
                println err
                println err.stackTrace.join('\n')
                return null
            }
        }
    }

    def Transaction matchTransaction(int sqlDataVersion, List<GroovyRowResult> transactions, File source) {
        return transactions.findResult {data ->
            def size = data['size']
            def date = data['date']
            def version = data['version']

            if (version == sqlDataVersion) {
                if (size == source.size()) {
                    if (date == source.lastModified()) {
    //                  def checkSum = source.bytes.digest('MD5')
                        def storedTransaction = Transaction.fromBlob(data['data'])
                        return storedTransaction
                    }
                }
            }
        }
    }

    def Transaction findTransaction(int sqlDataVersion, File source) {
        this.checkConnection()

        def query = """
            SELECT data
            FROM ${this.table}
            WHERE source = "${Transaction.getRelativePath(source)}"
            AND size = "${source.size()}"
            AND date = "${source.lastModified()}"
            AND version = "${sqlDataVersion}"
        """

        def data = this.sql.firstRow(query.toString().trim())
        if (data) {
            def storedTransaction = Transaction.fromBlob(data['data'])
            return storedTransaction
//            return this.matchTransaction(sqlDataVersion, [data], source)
        }
        return null
    }

    @Override
    void close() throws IOException {
        this.closeConnection()
    }
}
