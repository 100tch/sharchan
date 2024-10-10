#!/bin/bash

for i in {1..100}; do
	echo $i > "file$i.txt"
	curl -X POST localhost:3000 -F "file=@file$i.txt"
	rm "file$i.txt"
done
