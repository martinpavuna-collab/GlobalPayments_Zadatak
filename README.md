# Developers Notes
I had no previous experience with client/server implementations, 
so I did not know all the details that need attention
(for example reading exactly the right number of bytes from a TCP stream, handling cut-off or invalid frames, 
or keeping the server running after a bad client). Because of that, I used AI assistance for these parts.

I set up the foundation of the application myself: the interfaces and the responsibilities of the classes (reader, writer, client, server, database manager). The AI helped me to complete the client/server logic, to find and fix bugs, and to write tests.
Also AI was used to write the documentation (README and DESIGN), because I did not see a point in writing it by hand.

Added a little extra class QueryMain that contains selects on the database table.
I would very much appreciate any kind of feedback even if i did not suffice in the quality of the task.

# Framed Message Client/Server

A client reads messages from a binary file and sends them over TCP to a server. The server converts
the text from ISO-8859-2 to UTF-8, writes each message as one line to a text file, and saves the
parsed data in a SQLite database.

**Message format:** each message is sent as a frame: 2 bytes with the length (big-endian), then the
text (1–4096 bytes). The text looks like `PAYMENT|12345|100.50|HRK` (type, id, amount, currency).

## Requirements

JDK 11 or newer and Maven. Nothing else (SQLite is included, the database file is created automatically).

## Try it (one command)

```
./run-demo.sh      # Linux / macOS / Git Bash
run-demo.bat       # Windows
```

It builds the project, starts the server, sends both sample files and prints the results. Everything
is created in `target/demo/`, your own files are not touched.

## Run it yourself

Start the server first, then the client in a second terminal:

```
mvn compile exec:java@server        # server, default port 5000
mvn compile exec:java@client        # client, sends input/sample-input-hr.bin
mvn compile exec:java@query         # shows what is stored in the database
```

Files used (relative to the folder you start from): input `input/sample-input-hr.bin`, output
`output/messages.txt`, database `messages.db`. To send the other sample, change `INPUT_FILE` in
`config/AppConfig.java`. Stop the server with Ctrl+C.

## Configuration

**Only the TCP port is configurable.** It is the first argument of the server and of the client
(both must use the same one). Default: `5000`.

```
mvn compile exec:java@server -Dexec.args="6000"
mvn compile exec:java@client -Dexec.args="6000"
```

Everything else (file names, host `localhost`) is a constant in `config/AppConfig.java`.

## Database

SQLite file `messages.db`, one table. It is created by the server when it starts:

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_type TEXT NOT NULL,
    transaction_id   TEXT NOT NULL,
    amount           TEXT NOT NULL,   -- exact decimal, e.g. '42.00'
    currency         TEXT NOT NULL CHECK (length(currency) = 3),
    received_at      TEXT NOT NULL    -- UTC time
);
```

## What happens with bad data

* Every valid message is written to the output file.
* Only messages in the form `type|id|amount|currency` go to the database. `END` and `KRAJ` (end markers) and messages in another form are only written to the file.
* A bad frame (length 0 or above 4096, cut-off data) is reported with its number. The server closes that connection, keeps what it already received, and waits for the next client.
* The client reports a bad input file and exits with an error. Messages before the problem are still sent.

## Tests

```
mvn test
```

Covers the frame reading/writing, message parsing, the database, and a full client → server run with real sockets.
