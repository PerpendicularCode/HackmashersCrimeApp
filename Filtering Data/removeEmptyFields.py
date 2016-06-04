import sys
import csv
import getopt
import random

def makeSortable( num ):
    if len(num) == 1:
        return '0' + num
    else:
        return num

def main():
    with open('incidents-5y.csv') as csvfile:
        lines = [line for line in csvfile]
        typedict = {'"VEHICLE BREAK-IN/THEFT"' : '0'}
        firstRow = True
        for line in lines:
            if firstRow:
                firstRow = False
                continue
            row = line.replace('\n', '').split(',')
            lat = ""
            lon = ""
            for c in row:
                if ("32." in c or "33." in c):
                    lat = c
                if ("-116." in c or "-117." in c):
                    lon = c
            if (lat and lon):
                date = row[1]
                time = row[7]
                type = row[10]
                if not(type in typedict):
                    typedict[type] = str(len(typedict))
                typeNum = makeSortable(typedict[type])
                print(date + ', ' + time + ', ' + typeNum + ', ' + lat + ', ' + lon)
                    
        typelist = []
        for types,num in typedict.items():
            typelist.append(makeSortable(num) + ': ' + types)
        with open('types.txt', 'w') as f:
            for row in sorted(typelist):
                f.write(row + '\n') 
if __name__ == "__main__": main()    
