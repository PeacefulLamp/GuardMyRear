from socket import *
import time
import math




for i in range(10000):
	time.sleep(.01)
	fake_data = 400*(math.sin(i*math.pi/1000)**2) + 100
	print fake_data

	cs = socket(AF_INET, SOCK_DGRAM)
	cs.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
	cs.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

	cs.sendto("{\"key1\":%s, \"key2\":%s, \"key3\":%s}" %(fake_data, fake_data, fake_data), ('255.255.255.255', 5005))
