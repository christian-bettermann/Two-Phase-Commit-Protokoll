# Two-Phase-Commit Protokoll
Message-Format:
* public Message(int statusCode, InetAddress senderAddress, int senderPort, int bookingID, String statusMessage)

Status-codes:
* 0: Connection test
* 1: 
* 8: Testing
* 9: Error 
