# Design

## How it works

```
input file  →  Client  →  (TCP)  →  Server  →  output file (UTF-8 text)
                                           →  database (SQLite)
```

1. The client reads one frame at a time from the file and sends it.
2. The server reads the frame, converts the text from ISO-8859-2 to UTF-8, and writes it as a line to the output file.
3. If the text is in the form `type|id|amount|currency`, the server also saves it in the database, each part in its own column.

## Classes

| Class | What it does |
|---|---|
| `FrameReader`, `FrameWriter` | Read and write frames (2-byte length + text). Used by both client and server. |
| `MessageClient` | Reads the input file and sends the frames. |
| `MessageServer` | Receives the frames, writes the file, saves to the database. |
| `TransactionParser` | Splits `type\|id\|amount\|currency` into parts and checks them. |
| `DatabaseManager` | Creates the table and saves rows. |
| `ServerMain`, `ClientMain`, `QueryMain` | Start the server, start the client, show the database content. |
| `AppConfig` | Port and file names. |

## Decisions

* **Only the port is configurable.** The task asks for that; file names are constants.
* **One client at a time.** It is simpler and the messages stay in order. If a client is slow, the next one waits (a client that sends nothing for 30 seconds is dropped).
* **A bad frame closes that connection.** After a broken length we cannot tell where the next frame starts. What was received before is kept, and the server keeps running.
* **`END` / `KRAJ` are not saved in the database.** They are not transactions, but they are written to the file because the expected output contains them.
* **A message with a line break inside is rejected,** because the output file has one message per line.
* **Amount is saved as text** (`42.00`)

## Limits

* The file and the database are not saved as one step: if the database fails, the line can already be in the file.
* One database connection is opened per message. Fine for this size, slow for very large amounts of data.


