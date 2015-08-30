#!/usr/bin/python

import os
import sys
import json
import time
from pprint import pprint

if len(sys.argv) != 2:
    print "Usage: %s <number of measure>"
    quit()

repeat = int(sys.argv[1])

master = "localhost:4040"

values = []

def get_json():
    cmd = "wget " + master + "/metrics/json"
    os.system(cmd)

def parse_json():
    with open('json') as data_file:
        data = json.load(data_file)
        data1 = data["gauges"]
        for (k, v) in data1.items():
            if "totalDelay" in k:
                v = int(data1[k]["value"])
                print v
                values.append(v)
    cmd = "rm json"
    os.system(cmd)

for i in range(0, repeat):
    get_json()
    parse_json()
    time.sleep(1)

s = 0
for v in values:
    s = s + v
with open('measure', "w") as output_file:
    for v in values:
        output_file.write("%d\n" %(v))
    output_file.write("average %lf\n" %(1.0*s/len(values)))
