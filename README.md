# Two-Phase-Commit Protokoll
Message-Format:
* public Message(StatusTypes statusCode, InetAddress senderAddress, int senderPort, int bookingID, String statusMessage)

Implemented StatusTypes:
* BOOKING
* PREPARE
* READY
* ABORT
* COMMIT
* ROLLBACK
* ACKNOWLEDGMENT
* TESTING
* ERROR
* CONNECTIONTEST
