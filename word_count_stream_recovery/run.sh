#!/bin/bash

../../spark-1.4.1-bin-hadoop2.6/bin/spark-submit --class "WordCountStream" --master spark://notebook:7077 --jars lib/guava-18.0.jar target/scala-2.10/wordcountstream_2.10-1.0.jar localhost 9999 ~/checkpoint/ ~/out
