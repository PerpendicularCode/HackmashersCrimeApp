import sys
import csv
import getopt

def makeSortable( num ):
	if len(num) == 1:
		return '0' + num
	else:
		return num

def main():
#	try:
#		opts, args = getopt.getopt(argv,"hi:o:",["ifile=","ofile="])
#	except getopt.GetoptError:
#		print 'test.py -i <inputfile> -o <outputfile>'
#		sys.exit(2)
	with open('example.csv') as csvfile:
		reader = csv.reader(csvfile)
		typedict = {'VEHICLE BREAK-IN/THEFT' : '0'}
		firstRow = True
		for row in reader:
			if firstRow:
				firstRow = False
				continue
			columnNum = 0
			date = row[1]
			time = row[7]
			type = row[10]
			lat = row[21]
			lon = row[22]
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
