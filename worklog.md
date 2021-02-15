# Worklog for TN5250

## Workplan
* √ named sessions
* √ Screen grab (`GET /sessions/:id/screen`)
* √ navigate in fields
* √ close sessions
* Document API
----
* Connect to TestoryLocal.




## TODOs long run
* Clean up all sessions method (e.g. `DELETE /sessions/`).
* work in server mode (current shows connect modal which blocks the UI)
* Java 14
* POM, maven


## API

* `GET /configs/` lists configurations that can be used for starting sessions.
* `GET /sessions/` lists active session names
* `PUT /sessions/:sname` Starts a new session named :sname. Payload is config name.
* `DELETE /sessions/:sname` deletes the session :sname.
* `PUT /sessions/:sname/keys` sends keys to session :sname. Payload is the keys.
* `GET /sessions/:sname/text` returns a text view of :sname's screen.
* `GET /sessions/:sname/image` returns a screen grab of :sname.
* `GET /sessions/:sname/fields/` list of all the fields in :sname.
* `GET /sessions/:sname:/fields/:id` Data for field :id in :sname. Includes attributes and text. :id is number or "first".
* `PUT /sessions/:sname/fields/go` move to prev/next field in :sname. Payload is "prev" or "next".