import sys
import csv
import getopt
import random

def main():
    with open('incidents-5y_no_empty.csv') as csvfile:
        lines = [line for line in csvfile]
        randomRows = random.sample(lines, 5)
        for row in randomRows:
            print(row.replace('\n', ''))
if __name__ == "__main__": main()    
