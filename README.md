# Two-Phase-Commit Protokoll
Message-Format:
* public Message(StatusTypes statusCode, InetAddress senderAddress, int senderPort, int bookingID, String statusMessage)

Implemented StatusTYpes:
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
