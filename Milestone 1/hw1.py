# Write your code here :-)
from microbit import *

#Define variable to record steps
# steps = 0
# x = 0
# while True:
#     # Check to see if a step has been taken. If so, display a smile and increase the number of steps by 1
#     if accelerometer.was_gesture('shake'):
#         steps += 1
#         string_steps = str(steps)
#         display.show(string_steps)
#         sleep(500)
#         x = accelerometer.get_z()
#         display.clear()

#     if accelerometer.get_z() < x - 3:
#         display.show(Image.NO)

steps = 0
while True:
    x = accelerometer.get_x()
    y = accelerometer.get_y()
    z = accelerometer.get_z()
    sleep(200)
    xx = accelerometer.get_x()
    yy = accelerometer.get_y()
    zz = accelerometer.get_z()
    if (x != xx or y != yy or z != zz):
        steps += 1

    # Check to see if button A has been pressed. If so, display the number of steps taken
    if button_a.is_pressed():
        steps += 1
        string_steps = str(steps)
        display.show(string_steps)
        sleep(500)
        display.clear()