from socket import *
import math
import RPi.GPIO as GPIO
import time
​
echoPin = 12 	#Pin 32 on Pi
trigPin = 6		#Pin 31 on Pi
​
#Initialize pins
GPIO.setmode(BCM)
GPIO.setup(echoPin, GPIO.IN)
GPIO.setup(trigPin, GPIO.out)
​
def sensorRead():
​
	GPIO.output (trigPin, false)
	time.sleep(0.2)
​
	#Send trigger signal (10 µs)
	GPIO.output(trigPin, true)
	time.sleep(10/1000000)
	GPIO.output(trigPin, false)
​
	while GPIO.input(echoPin) == 0:
		pass
​
	risingEdgeTime = time.time() 
​
	while GPIO.input(echoPin) == 1:
		pass
​
	fallingEdgeTime = time.time()
​
	deltaTime = fallingEdgeTime - risingEdgeTime
​
	distanceCenti = deltatime/(1000000.0 * 58)
​
	return distanceCenti

cs = socket(AF_INET, SOCK_DGRAM)
cs.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
cs.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

while True:
	time.sleep(.01)
	value_data = sensorRead()
	print value_data
	cs.sendto("{\"key1\":%s, \"key2\":%s, \"key3\":%s}" %(value_data, value_data, value_data), ('255.255.255.255', 5005))
