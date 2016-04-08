import RPi.GPIO as GPIO
import time
​
echoPin = 12 	#Pin 32 on Pi
trigPin = 6		#Pin 31 on Pi
​
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
	print distanceCenti
