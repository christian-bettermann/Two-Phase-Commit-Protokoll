# Two-Phase-Commit Protokoll
Message-Format:
* public Message(StatusTypes statusCode, InetAddress senderAddress, int senderPort, int bookingID, String statusMessage)

Implemented StatusTypes:
* INFO
* INFOCARS
* INFOROOMS
* INITIALIZED
* BOOKING
* PREPARE
* READY
* ABORT
* COMMIT
* ROLLBACK
* ACKNOWLEDGMENT
* ERROR
* CONNECTIONTEST
* INQUIRE
* THROWAWAY
