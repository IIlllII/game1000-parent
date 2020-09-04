import matplotlib.pyplot as plt
import numpy as np
import sys


arg1 = sys.argv[1]
arg2 = sys.argv[2]

print(arg1)
print(arg2)

a1 = np.loadtxt(arg1)
a2 = np.loadtxt(arg2)

colors = ['#ed0dd9', '#448ee4']

n, bins, patches = plt.hist(
	[a1,a2], 
	100, 
	range = (0,500)  , 
	histtype = 'stepfilled',
	color=colors, 
	log = False, 
	alpha = 0.35, 
	label = ['pure','mixed']
	) 

plt.xlabel('Distance from 1000')
plt.ylabel('Games')

plt.title("Pure only VS mixed strategy")

plt.legend()

plt.show()
